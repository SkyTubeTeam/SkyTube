package free.rm.skytube.businessobjects.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import free.rm.skytube.app.SkyTubeApp;

/**
 * A database (DB) that stores user's searches (for use in Search Suggestions).
 */

public class SearchHistoryDb extends SQLiteOpenHelperEx {

	private static volatile SearchHistoryDb searchHistoryDb = null;

	private static final int DATABASE_VERSION = 1;
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
		}
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
		// if the user input "'" it was causing the app to crash, hence replace "'" with "\''"
		String searchClause = exactTextSearch
				? String.format(" = '%s'", searchText.replaceAll("'", "\''"))
				: String.format(" LIKE '%s%%'", searchText.replaceAll("'", "\''"));

		return getReadableDatabase().query(SearchHistoryTable.TABLE_NAME,
				new String[] {SearchHistoryTable.COL_SEARCH_ID, SearchHistoryTable.COL_SEARCH_TEXT},
				SearchHistoryTable.COL_SEARCH_TEXT + searchClause,
				null,
				null,
				null,
				SearchHistoryTable.COL_SEARCH_TEXT + " ASC");
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
