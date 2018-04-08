package free.rm.skytube.businessobjects.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;

/**
 * A database (DB) that stores user's blocked channels.
 */
public class BlockedChannelsDb extends SQLiteOpenHelperEx {

	private static volatile BlockedChannelsDb blockedChannelsDb = null;

	private static final int DATABASE_VERSION = 1;
	private static final String DATABASE_NAME = "blockedchannels.db";


	private BlockedChannelsDb(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}


	public static synchronized BlockedChannelsDb getBlockedChannelsDb() {
		if (blockedChannelsDb == null) {
			blockedChannelsDb = new BlockedChannelsDb(SkyTubeApp.getContext());
		}
		return blockedChannelsDb;
	}


	@Override
	protected void clearDatabaseInstance() {
		blockedChannelsDb = null;
	}


	@Override
	public void onCreate(SQLiteDatabase sqLiteDatabase) {
		sqLiteDatabase.execSQL(BlockedChannelsTable.getCreateStatement());
	}


	@Override
	public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
	}


	/**
	 * Add the specified channel to the list of blocked channels.
	 *
	 * @param video channel to add
	 * @return True if the channel was successfully saved/blocked to the DB.
	 */
	public boolean add(YouTubeVideo video) {
		ContentValues contentValues = new ContentValues();
		contentValues.put(BlockedChannelsTable.COL_CHANNEL_ID, video.getChannelId());
		contentValues.put(BlockedChannelsTable.COL_YOUTUBE_CHANNEL_NAME, video.getChannelName());
	  
		return getWritableDatabase().insert(BlockedChannelsTable.TABLE_NAME, null, contentValues) != -1;
	}


	/**
	 * Remove the specified channel from the list of blocked channels.
	 *
	 * @param channelName channel to remove.
	 *
	 * @return True if the channel has been unblocked; false otherwise.
	 */
	public boolean remove(String channelName) {
		int rowsDeleted = getWritableDatabase().delete(BlockedChannelsTable.TABLE_NAME,
				BlockedChannelsTable.COL_YOUTUBE_CHANNEL_NAME + " = ?",
				new String[]{String.valueOf(channelName)});

		return rowsDeleted >= 0;
	}
	

	/**
	 * Method for getting blocked channels' IDs as list.
	 *
	 * @return list of blocked channel IDs.
	 */
	public List<String> getBlockedChannelsListId() {
		List<String> videos = new ArrayList<>();
		String youtubeChannelId = "_Id";
		String query = "SELECT "+ youtubeChannelId +" FROM " + BlockedChannelsTable.TABLE_NAME;
		SQLiteDatabase sqLiteDatabase = getReadableDatabase();
		Cursor cursor = sqLiteDatabase.rawQuery(query, null);
		if (cursor.moveToFirst()) {
			do {
				String channelId = cursor.getString(0);
				videos.add(channelId);
			} while (cursor.moveToNext());
		}
		cursor.close();
		return videos;
	}


	/**
	 * Method for getting blocked channels' names as list.
	 *
	 * @return list of blocked channel names.
	 */
	public List<String> getBlockedChannelsListName() {
		List<String> videos = new ArrayList<>();
		String youtubeChannelName = "Youtube_Channel_Name";
		String query = "SELECT "+youtubeChannelName+" FROM " + BlockedChannelsTable.TABLE_NAME;
		SQLiteDatabase sqLiteDatabase = getReadableDatabase();
		Cursor cursor = sqLiteDatabase.rawQuery(query, null);
		if (cursor.moveToFirst()) {
			do {
				String channelId = cursor.getString(0);
				videos.add(channelId);
			} while (cursor.moveToNext());
		}
		cursor.close();
		return videos;
	}

}