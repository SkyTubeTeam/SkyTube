package free.rm.skytube.businessobjects.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import free.rm.skytube.app.SkyTubeApp;

/**
 * A database (DB) that stores user's searches (for use in Search Suggestions).
 */

public class SearchHistoryDb extends SQLiteOpenHelperEx {

	private static volatile SearchHistoryDb searchHistoryDb = null;

	private static final int DATABASE_VERSION = 2;
	private static final String DATABASE_NAME = "searchHistory.db";


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
	protected void clearDatabaseInstance() {
		searchHistoryDb = null;
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
					for(Integer id : entry.keySet()) {
						String text = entry.get(id);
						ContentValues values = new ContentValues();
						values.put(SearchHistoryTable.COL_SEARCH_ID, id);
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
	public void insertSearchText(String text) {
		if (!isSearchTextAlreadyStored(text)) {
			ContentValues values = new ContentValues();
			values.put(SearchHistoryTable.COL_SEARCH_TEXT, text);
			getWritableDatabase().insert(SearchHistoryTable.TABLE_NAME, null, values);
		} else {
			updateSearchTextTimestamp(text);
		}
	}

	/**
	 * Update the datetime field to the current date/time for the passed search text string
	 *
	 * @param text   Text the user searched for.
	 */
	public void updateSearchTextTimestamp(String text) {
		getWritableDatabase().execSQL(String.format("UPDATE %s SET %s = datetime('now','localtime') WHERE %s = '%s'", SearchHistoryTable.TABLE_NAME, SearchHistoryTable.COL_SEARCH_DATE, SearchHistoryTable.COL_SEARCH_TEXT, text));
	}


	/**
	 * Return true if the given search text has already been stored in the database.
	 *
	 * @param text  Search text
	 * @return True if the given search text has already been stored in the database; false otherwise.
	 */
	private boolean isSearchTextAlreadyStored(String text) {
		Cursor  cursor = getSearchCursor(text, true);
		boolean	isSearchTextAlreadyStored = cursor.moveToNext();
		cursor.close();
		return isSearchTextAlreadyStored;
	}


	/**
	 * Given a search string, it will return a cursor contain text strings which start as the given
	 * searchText.
	 *
	 * @param searchText    Text the user has typed.
	 * @return              A cursor containing texts which starts with the contents of searchText.
	 */
	public Cursor getSearchCursor(String searchText) {
		return getSearchCursor(searchText, false);
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
			return getReadableDatabase().query(SearchHistoryTable.TABLE_NAME,
					new String[] {SearchHistoryTable.COL_SEARCH_ID, SearchHistoryTable.COL_SEARCH_TEXT},
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

			return getReadableDatabase().query(SearchHistoryTable.TABLE_NAME,
					new String[]{SearchHistoryTable.COL_SEARCH_ID, SearchHistoryTable.COL_SEARCH_TEXT},
					SearchHistoryTable.COL_SEARCH_TEXT + searchClause,
					new String[] {params},
					null,
					null,
					SearchHistoryTable.COL_SEARCH_TEXT + " ASC");
		}
	}

	/**
	 * Delete a previously searched text.
	 *
	 * @param text  A previous searched text.
	 */
	public void deleteSearchText(String text) {
		getWritableDatabase().delete(SearchHistoryTable.TABLE_NAME,
				SearchHistoryTable.COL_SEARCH_TEXT + " = ?",
				new String[]{text});
	}

}
