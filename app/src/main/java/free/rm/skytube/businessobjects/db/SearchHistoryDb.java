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

	public int deleteAllSuggestions() {
		return getWritableDatabase().delete(SearchHistoryTable.TABLE_NAME, null, null);
	}

	public long insertSuggestion(String text) {
		ContentValues values = new ContentValues();
		values.put(SearchHistoryTable.COL_SEARCH_TEXT, text);
		return getWritableDatabase().insert(SearchHistoryTable.TABLE_NAME, null, values);
	}


	public Cursor getSuggestions(String text) {
		return getReadableDatabase().query(SearchHistoryTable.TABLE_NAME, new String[] {SearchHistoryTable.COL_SEARCH_ID, SearchHistoryTable.COL_SEARCH_TEXT},
						SearchHistoryTable.COL_SEARCH_TEXT+" LIKE '"+ text +"%'", null, null, null, null);
	}

	public long deleteSuggestion(String text) {
		return getWritableDatabase().delete(SearchHistoryTable.TABLE_NAME, SearchHistoryTable.COL_SEARCH_TEXT + " = ?",
						new String[]{text});
	}

}
