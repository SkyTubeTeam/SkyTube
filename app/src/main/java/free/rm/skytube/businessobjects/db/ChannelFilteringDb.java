/*
 * SkyTube
 * Copyright (C) 2018  Ramon Mifsud
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation (version 3 of the License).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
 * A database (DB) that stores user's blacklist/whitelist channels.
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
	public void onCreate(SQLiteDatabase sqLiteDatabase) {
		sqLiteDatabase.execSQL(ChannelListTable.BLACKLIST.getCreateStatement());
		sqLiteDatabase.execSQL(ChannelListTable.WHITELIST.getCreateStatement());
	}


	@Override
	public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
	}


	////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// Channel blacklisting methods.
	//

	/**
	 * Blacklist a channel.
	 *
	 * @param channelId     Channel ID.
	 * @param channelName   Channel name.
	 *
	 * @return  True if successful.
	 */
	public boolean blacklist(String channelId, String channelName) {
		return addChannel(ChannelListTable.BLACKLIST, channelId, channelName);
	}


	/**
	 * Unblacklist the given channels.
	 *
	 * @param channels  Channels to unblacklist.
	 *
	 * @return  True if successful.
	 */
	public boolean unblacklist(final List<MultiSelectListPreferenceItem> channels) {
		return removeChannels(ChannelListTable.BLACKLIST, channels);
	}


	/**
	 * @return List of blacklisted channels.
	 */
	public List<MultiSelectListPreferenceItem> getBlacklistedChannels() {
		return getChannels(ChannelListTable.BLACKLIST);
	}


	/**
	 * @return List of blacklisted channel IDs.
	 */
	public List<String> getBlacklistedChannelsIdsList() {
		return getChannelsIdsList(ChannelListTable.BLACKLIST);
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
		return addChannel(ChannelListTable.WHITELIST, channelId, channelName);
	}


	/**
	 * Unwhitelist the given channel.
	 *
	 * @param channelId Channel ID.
	 *
	 * @return  True if successful.
	 */
	public boolean unwhitelist(final String channelId) {
		return removeChannels(ChannelListTable.WHITELIST, channelId);
	}


	/**
	 * @return List of whitelisted channels.
	 */
	public List<MultiSelectListPreferenceItem> getWhitelistedChannels() {
		return getChannels(ChannelListTable.WHITELIST);
	}


	/**
	 * @return List of whitelisted channel IDs.
	 */
	public List<String> getWhitelistedChannelsIdsList() {
		return getChannelsIdsList(ChannelListTable.WHITELIST);
	}


	///////////////////////////

	/**
	 * Add the given channel to the blacklist/whitelist.
	 *
	 * @param channelListTable  Blacklist/Whitelist table.
	 * @param channelId         Channel ID.
	 * @param channelName       Channel name.
	 *
	 * @return True if successful.
	 */
	private boolean addChannel(ChannelListTable channelListTable, String channelId, String channelName) {
		ContentValues values = new ContentValues();
		values.put(ChannelListTable.COL_CHANNEL_ID, channelId);
		values.put(ChannelListTable.COL_CHANNEL_NAME, channelName);

		return getWritableDatabase().insert(channelListTable.getTableName(), null, values) != -1;
	}


	/**
	 * Remove the given channels from the blacklist/whitelist.
	 *
	 * @param channelListTable  Blacklist/Whitelist table.
	 * @param channels          Channels to blacklist/whitelist.
	 *
	 * @return  True if successful.
	 */
	private boolean removeChannels(ChannelListTable channelListTable, final List<MultiSelectListPreferenceItem> channels) {
		String[] channelsIds = new String[channels.size()];

		for (int i = 0;  i < channelsIds.length;  i++) {
			channelsIds[i]= "'" + channels.get(i).id + "'";
		}

		final String  channelIdsCsv = TextUtils.join(", ", channelsIds);
		final int     rowsDeleted;

		rowsDeleted = getWritableDatabase()
				.delete(channelListTable.getTableName(),
						ChannelListTable.COL_CHANNEL_ID + " IN (" + channelIdsCsv + ")",
						null);

		return (rowsDeleted > 0);
	}


	/**
	 * Remove the given channel from blacklist/whitelist.
	 *
	 * @param channelListTable  Blacklist/Whitelist table.
	 * @param channelId         Channel ID.
	 *
	 * @return  True if successful.
	 */
	private boolean removeChannels(ChannelListTable channelListTable, final String channelId) {
		return (getWritableDatabase().delete(channelListTable.getTableName(),
				ChannelListTable.COL_CHANNEL_ID + " = ?",
				new String[]{channelId}) > 0);
	}


	/**
	 * @param channelListTable  Blacklist/Whitelist table.
	 *
	 * @return List of blacklisted/whitelisted channels.
	 */
	private List<MultiSelectListPreferenceItem> getChannels(ChannelListTable channelListTable) {
		List<MultiSelectListPreferenceItem> whitelistedChannels = new ArrayList<>();
		String                              channelId;
		String                              channelName;
		Cursor                              cursor;

		cursor = getReadableDatabase().query(
				channelListTable.getTableName(),
				new String[]{ChannelListTable.COL_CHANNEL_ID, ChannelListTable.COL_CHANNEL_NAME},
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
	 * @param channelListTable  Blacklist/Whitelist table.
	 *
	 * @return List of blacklisted/whitelisted channel IDs.
	 */
	private List<String> getChannelsIdsList(ChannelListTable channelListTable) {
		List<String>    whitelistedChannels = new ArrayList<>();
		String          channelId;
		Cursor          cursor;

		cursor = getReadableDatabase().query(
				channelListTable.getTableName(),
				new String[]{ChannelListTable.COL_CHANNEL_ID},
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