package free.rm.skytube.businessobjects.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.Logger;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * A database (DB) that stores user's searches (for use in Search Suggestions).
 */

public class SearchHistoryDb extends SQLiteOpenHelperEx {

	public static final String[] SEARCH_HISTORY_COLUMNS = {SearchHistoryTable.COL_SEARCH_ID, SearchHistoryTable.COL_SEARCH_TEXT};
	private static volatile SearchHistoryDb searchHistoryDb = null;

	private static final int DATABASE_VERSION = 2;
	private static final String DATABASE_NAME = "searchHistory.db";

	private static final String UPDATE_SEARCH_TEXT_TIMESTAMP = String.format("UPDATE %s SET %s = datetime('now','localtime') WHERE %s = ?", SearchHistoryTable.TABLE_NAME, SearchHistoryTable.COL_SEARCH_DATE, SearchHistoryTable.COL_SEARCH_TEXT);


	private SearchHistoryDb(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}


	public static synchronized SearchHistoryDb getSearchHistoryDb() {
		if (searchHistoryDb == null) {
			searchHistoryDb = new SearchHistoryDb(SkyTubeApp.getContext());
		}

		return searchHistoryDb;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(SearchHistoryTable.getCreateStatement());
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	    // Version 2 introduced the date the search term was searched for. If the user already has version 1,
        // grab all the values from the database, recreate it with the new column, and add the search terms back in.
        // The current timestamp will be used.
		if(oldVersion == 1 && newVersion == 2) {
			Cursor cursor = db.query(SearchHistoryTable.TABLE_NAME,
					new String[] {SearchHistoryTable.COL_SEARCH_ID, SearchHistoryTable.COL_SEARCH_TEXT},
					null,
					null,
					null,
					null,
					SearchHistoryTable.COL_SEARCH_ID + " ASC");
			List<Map<Integer, String>> history = new ArrayList<>();
			if(cursor.moveToFirst()) {
				do {
					int id = cursor.getInt(cursor.getColumnIndex(SearchHistoryTable.COL_SEARCH_ID));
					String text = cursor.getString(cursor.getColumnIndex(SearchHistoryTable.COL_SEARCH_TEXT));
					history.add(new HashMap<Integer, String>(){{put(id, text); }});
				} while (cursor.moveToNext());
				db.execSQL("DROP TABLE " + SearchHistoryTable.TABLE_NAME);
				onCreate(db);
				for(Map<Integer, String> entry : history) {
					for(Map.Entry<Integer, String> e : entry.entrySet()) {
						String text = e.getValue();
						ContentValues values = new ContentValues();
						values.put(SearchHistoryTable.COL_SEARCH_ID, e.getKey());
						values.put(SearchHistoryTable.COL_SEARCH_TEXT, text);
						db.insert(SearchHistoryTable.TABLE_NAME, null, values);
					}
				}
			}
		}
	}

	/**
	 * Delete all search history
	 */
	public void deleteAllSearchHistory() {
		getWritableDatabase().delete(SearchHistoryTable.TABLE_NAME, null, null);
	}


    /**
     * Save a search text into the DB.
     *
     * @param text  Text the user just searched for.
     */
    public Completable insertSearchText(String text) {
        return Completable.fromRunnable(()  -> {
            if (!isSearchTextAlreadyStored(text)) {
                ContentValues values = new ContentValues();
                values.put(SearchHistoryTable.COL_SEARCH_TEXT, text);
                getWritableDatabase().insert(SearchHistoryTable.TABLE_NAME, null, values);
            } else {
                updateSearchTextTimestamp(text);
            }
        }).subscribeOn(Schedulers.io());
    }

	/**
	 * Update the datetime field to the current date/time for the passed search text string
	 *
	 * @param text   Text the user searched for.
	 */
	public Completable updateSearchTextTimestamp(String text) {
		return Completable.fromRunnable(() -> {
			try (Cursor cursor = getWritableDatabase().rawQuery(UPDATE_SEARCH_TEXT_TIMESTAMP, new String[]{ text })) {
				cursor.moveToFirst();
			}
		}).subscribeOn(Schedulers.io());
	}


	/**
	 * Return true if the given search text has already been stored in the database.
	 *
	 * @param text  Search text
	 * @return True if the given search text has already been stored in the database; false otherwise.
	 */
	private boolean isSearchTextAlreadyStored(String text) {
		try(Cursor cursor =  getSearchCursor(text, true)) {
			return cursor.moveToNext();
		}
	}

    /**
     * Given a search string, it will return a cursor contain text strings which start as the given
     * searchText.
     *
     * @param searchText    Text the user has typed.
     * @return              A cursor containing texts which starts with the contents of searchText.
     */
    public Cursor executeSearch(String searchText){
         SkyTubeApp.nonUiThread();
         if (SkyTubeApp.getSettings().isDisableSearchHistory()) {
             return new MatrixCursor(SEARCH_HISTORY_COLUMNS, 0);
         } else {
            return convert(getSearchCursor(searchText, false));
         }
    }

    /**
     * Given a search string, it will return a cursor contain text strings which start as the given
     * searchText.
     *
     * @param searchText        Text the user has typed.
     * @param exactTextSearch   If set to true, it will only return items whose COL_SEARCH_TEXT's
     *                          data is identical to the given searchText.
     *
     * @return                  A cursor containing texts which starts with the contents of searchText.
     */
    private Cursor getSearchCursor(String searchText, boolean exactTextSearch) {
        if(searchText.length() <= 1) {
            Logger.i(this, "showing all the recent searches for: %s", searchText);
            return getReadableDatabase().query(SearchHistoryTable.TABLE_NAME,
                    SEARCH_HISTORY_COLUMNS,
                    null,
                    null,
                    null,
                    null,
                    SearchHistoryTable.COL_SEARCH_DATE + " DESC, " + SearchHistoryTable.COL_SEARCH_ID + " DESC",
                    "10");
        } else {
            // if the user input "'" it was causing the app to crash, hence replace "'" with "\''"
            String searchClause = exactTextSearch
                    ? " = ?"
                    : " LIKE ?";
            String params = exactTextSearch ? searchText : "%" + searchText + '%';
            Logger.i(this, "showing search queries matching : %s", params);

            return getReadableDatabase().query(SearchHistoryTable.TABLE_NAME,
                    SEARCH_HISTORY_COLUMNS,
                    SearchHistoryTable.COL_SEARCH_TEXT + searchClause,
                    new String[] {params},
                    null,
                    null,
                    SearchHistoryTable.COL_SEARCH_TEXT + " ASC");
        }
    }

    /**
     * Delete a previously searched text, and re-run the given query, and the result will be delivered as a Cursor
     *
     * @param textToDelete  A previous searched text to delete
     */
    public @NonNull Single<Cursor> deleteAndSearchAgain(String textToDelete, String search) {
        return Single.fromSupplier(() -> {
            deleteSearchText(textToDelete);
            return convert(getSearchCursor(search, false));
        }).subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * Convert the cursor into a MatrixCursor
     * @param cursor
     * @return
     */
    private Cursor convert(Cursor cursor) {
        try {
            MatrixCursor newCursor = new MatrixCursor(SEARCH_HISTORY_COLUMNS, cursor.getCount());
            while (cursor.moveToNext()) {
                newCursor.addRow(new Object[]{cursor.getInt(0), cursor.getString(1)});
            }
            return newCursor;
        } finally {
            cursor.close();
        }
    }

    /**
     * Delete a previously searched text.
     *
     * @param text  A previous searched text.
     */
    private void deleteSearchText(String text) {
        getWritableDatabase().delete(SearchHistoryTable.TABLE_NAME,
                SearchHistoryTable.COL_SEARCH_TEXT + " = ?",
                new String[]{text});
    }

}
