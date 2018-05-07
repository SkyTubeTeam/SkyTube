package free.rm.skytube.businessobjects.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.gui.businessobjects.MultiSelectListPreferenceItem;

/**
 * A database (DB) that stores user's blocked channels.
 */
public class ChannelFilteringDb extends SQLiteOpenHelperEx {

	private static volatile ChannelFilteringDb channelFilteringDb = null;

	private static final int DATABASE_VERSION = 1;
	private static final String DATABASE_NAME = "channelFiltering.db";


	private ChannelFilteringDb(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}


	public static synchronized ChannelFilteringDb getChannelFilteringDb() {
		if (channelFilteringDb == null) {
			channelFilteringDb = new ChannelFilteringDb(SkyTubeApp.getContext());
		}
		return channelFilteringDb;
	}


	@Override
	protected void clearDatabaseInstance() {
		channelFilteringDb = null;
	}


	@Override
	public void onCreate(SQLiteDatabase sqLiteDatabase) {
		sqLiteDatabase.execSQL(ChannelBlacklistTable.getCreateStatement());
		sqLiteDatabase.execSQL(ChannelWhitelistTable.getCreateStatement());
	}


	@Override
	public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
	}


	////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// Channel blacklisting methods.
	//


	/**
	 * Blacklisted a channel.
	 *
	 * @param channelId     Channel ID.
	 * @param channelName   Channel name.
	 *
	 * @return True if the channel was successfully saved/blocked to the DB.
	 */
	public boolean blacklist(String channelId, String channelName) {
		ContentValues contentValues = new ContentValues();
		contentValues.put(ChannelBlacklistTable.COL_CHANNEL_ID, channelId);
		contentValues.put(ChannelBlacklistTable.COL_CHANNEL_NAME, channelName);
	  
		return getWritableDatabase().insert(ChannelBlacklistTable.TABLE_NAME, null, contentValues) != -1;
	}


	/**
	 * Remove the specified channel from the list of blacklisted channels.
	 *
	 * @param channelName channel to unblacklist.
	 *
	 * @return True if the channel has been unblocked; false otherwise.
	 */
	public boolean unblacklist(String channelName) {
		int rowsDeleted = getWritableDatabase().delete(ChannelBlacklistTable.TABLE_NAME,
				ChannelBlacklistTable.COL_CHANNEL_NAME + " = ?",
				new String[]{String.valueOf(channelName)});

		return rowsDeleted > 0;
	}
	

	/**
	 * Method for getting blacklisted channels' IDs as list.
	 *
	 * @return List of blacklisted channel IDs.
	 */
	public List<String> getBlacklistedChannelIdsList() {
		List<String>    channelIdsList = new ArrayList<>();
		Cursor          cursor = getReadableDatabase().query(ChannelBlacklistTable.TABLE_NAME,
										new String[]{ChannelBlacklistTable.COL_CHANNEL_ID},
										null, null,
										null, null, null);

		if (cursor.moveToFirst()) {
			do {
				String channelId = cursor.getString(0);
				channelIdsList.add(channelId);
			} while (cursor.moveToNext());
		}
		cursor.close();

		return channelIdsList;
	}


	/**
	 * Method for getting blocked channels' names as list.
	 *
	 * @return list of blocked channel names.
	 */
	public List<String> getBlacklistedChannelNamesList() {
		List<String>    channelNames = new ArrayList<>();
		Cursor          cursor = getReadableDatabase().query(ChannelBlacklistTable.TABLE_NAME,
										new String[]{ChannelBlacklistTable.COL_CHANNEL_NAME},
										null, null,
										null, null,
										ChannelBlacklistTable.COL_CHANNEL_NAME + " ASC");

		if (cursor.moveToFirst()) {
			do {
				String channelId = cursor.getString(0);
				channelNames.add(channelId);
			} while (cursor.moveToNext());
		}
		cursor.close();

		return channelNames;
	}



	////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// Channel whitelisting methods.
	//


	/**
	 * Whitelist a channel.
	 *
	 * @param channelId     Channel ID.
	 * @param channelName   Channel name.
	 *
	 * @return  True if successful.
	 */
	public boolean whitelist(String channelId, String channelName) {
		ContentValues values = new ContentValues();
		values.put(ChannelWhitelistTable.COL_CHANNEL_ID, channelId);
		values.put(ChannelWhitelistTable.COL_CHANNEL_NAME, channelName);

		return getWritableDatabase().insert(ChannelWhitelistTable.TABLE_NAME, null, values) != -1;
	}


	/**
	 * Unwhitelist the given channels.
	 *
	 * @param channels  Channels to unwhitelist.
	 *
	 * @return  True if successful.
	 */
	public boolean unwhitelist(final List<MultiSelectListPreferenceItem> channels) {
		String[] channelsIds = new String[channels.size()];

		for (int i = 0;  i < channelsIds.length;  i++) {
			channelsIds[i]= "'" + channels.get(i).id + "'";
		}

		final String  channelIdsCsv = TextUtils.join(", ", channelsIds);
		final int     rowsDeleted;

		rowsDeleted = getWritableDatabase()
				.delete(ChannelWhitelistTable.TABLE_NAME,
						ChannelWhitelistTable.COL_CHANNEL_ID + " IN (" + channelIdsCsv + ")",
						null);

		return (rowsDeleted > 0);
	}


	/**
	 * Unwhitelist the given channel.
	 *
	 * @param channelId Channel ID.
	 *
	 * @return  True if successful.
	 */
	public boolean unwhitelist(final String channelId) {
		return (getWritableDatabase().delete(ChannelWhitelistTable.TABLE_NAME,
						ChannelWhitelistTable.COL_CHANNEL_ID + " = ?",
						new String[]{channelId}) > 0);
	}


	/**
	 * @return List of whitelisted channels.
	 */
	public List<MultiSelectListPreferenceItem> getWhitelistedChannels() {
		List<MultiSelectListPreferenceItem> whitelistedChannels = new ArrayList<>();
		String                              channelId;
		String                              channelName;
		Cursor                              cursor;

		cursor = getReadableDatabase().query(
				ChannelWhitelistTable.TABLE_NAME,
				new String[]{ChannelWhitelistTable.COL_CHANNEL_ID, ChannelWhitelistTable.COL_CHANNEL_NAME},
				null, null,
				null, null, null);

		if (cursor.moveToFirst()) {
			do {
				channelId = cursor.getString(0);
				channelName = cursor.getString(1);
				whitelistedChannels.add(new MultiSelectListPreferenceItem(channelId, channelName, false));
			} while (cursor.moveToNext());
		}
		cursor.close();

		return whitelistedChannels;
	}


	/**
	 * @return List of whitelisted channel IDs.
	 */
	public List<String> getWhitelistedChannelsIdsList() {
		List<String>    whitelistedChannels = new ArrayList<>();
		String          channelId;
		Cursor          cursor;

		cursor = getReadableDatabase().query(
				ChannelWhitelistTable.TABLE_NAME,
				new String[]{ChannelWhitelistTable.COL_CHANNEL_ID},
				null, null,
				null, null, null);

		if (cursor.moveToFirst()) {
			do {
				channelId = cursor.getString(0);
				whitelistedChannels.add(channelId);
			} while (cursor.moveToNext());
		}
		cursor.close();

		return whitelistedChannels;
	}

}