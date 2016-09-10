/*
 * SkyTube
 * Copyright (C) 2016  Ramon Mifsud
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
import android.database.sqlite.SQLiteOpenHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import free.rm.skytube.businessobjects.YouTubeChannel;
import free.rm.skytube.gui.app.SkyTubeApp;

/**
 * A database (DB) that stores user subscriptions (with respect to YouTube channels).
 */
public class SubscriptionsDb extends SQLiteOpenHelper {

	private static volatile SubscriptionsDb subscriptionsDb = null;

	private static final int DATABASE_VERSION = 1;
	private static final String DATABASE_NAME = "subs.db";


	private SubscriptionsDb(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}


	public static synchronized SubscriptionsDb getSubscriptionsDb() {
		if (subscriptionsDb == null) {
			subscriptionsDb = new SubscriptionsDb(SkyTubeApp.getContext());
		}

		return subscriptionsDb;
	}


	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(SubscriptionsTable.getCreateStatement());
	}


	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	}


	public boolean subscribe(String channelId) {
		ContentValues values = new ContentValues();
		values.put(SubscriptionsTable.COL_CHANNEL_ID, channelId);
		values.put(SubscriptionsTable.COL_LAST_VISIT_TIME, System.currentTimeMillis());

		return getWritableDatabase().insert(SubscriptionsTable.TABLE_NAME, null, values) != -1;
	}


	public boolean unsubscribe(String channelId) {
		int rowsDeleted = getWritableDatabase().delete(SubscriptionsTable.TABLE_NAME,
				SubscriptionsTable.COL_CHANNEL_ID + " = ?",
				new String[]{channelId});

		return (rowsDeleted >= 0);
	}


	public List<YouTubeChannel> getSubscribedChannels() throws IOException {
		ArrayList<YouTubeChannel> subsChannels = new ArrayList<>();
		Cursor cursor = getReadableDatabase().query(SubscriptionsTable.TABLE_NAME, new String[]{SubscriptionsTable.COL_CHANNEL_ID}, null, null, null, null, SubscriptionsTable.COL_ID + " ASC");

		if (cursor.moveToNext()) {
			int colChannelIdNum = cursor.getColumnIndexOrThrow(SubscriptionsTable.COL_CHANNEL_ID);
			String channelId = null;
			YouTubeChannel channel = null;

			do {
				channelId = cursor.getString(colChannelIdNum);
				channel = new YouTubeChannel();
				channel.init(channelId, true /* = user is subscribed to this channel*/);
				subsChannels.add(channel);
			} while (cursor.moveToNext());
		}
		cursor.close();

		return subsChannels;
	}


	/**
	 * Checks if the user is subscribed to the given channel.
	 *
	 * @param channelId	Channel ID
	 * @return True if the user is subscribed; false otherwise.
	 * @throws IOException
	 */
	public boolean isUserSubscribedToChannel(String channelId) throws IOException {
		Cursor cursor = getReadableDatabase().query(
				SubscriptionsTable.TABLE_NAME,
				new String[]{SubscriptionsTable.COL_ID},
				SubscriptionsTable.COL_CHANNEL_ID + " = ?",
				new String[]{channelId}, null, null, null);
		boolean	isUserSubbed = cursor.moveToNext();

		cursor.close();
		return isUserSubbed;
	}


	/**
	 * Updates the given channel's last visit time.
	 *
	 * @param channelId	Channel ID
	 *
	 * @return	last visit time, if the update was successful;  -1 otherwise.
	 */
	public long updateLastVisitTime(String channelId) {
		SQLiteDatabase	db = getWritableDatabase();
		long			currentTime = System.currentTimeMillis();

		ContentValues values = new ContentValues();
		values.put(SubscriptionsTable.COL_LAST_VISIT_TIME, currentTime);

		int count = db.update(
				SubscriptionsTable.TABLE_NAME,
				values,
				SubscriptionsTable.COL_CHANNEL_ID + " = ?",
				new String[]{channelId});

		return (count > 0 ? currentTime : -1);
	}


	/**
	 * Returns the last time the user has visited this channel.
	 *
	 * @param channelId
	 *
	 * @return	last visit time, if the update was successful;  -1 otherwise.
	 * @throws IOException
	 */
	public long getLastVisitTime(String channelId) {
		Cursor	cursor = getReadableDatabase().query(
							SubscriptionsTable.TABLE_NAME,
							new String[]{SubscriptionsTable.COL_LAST_VISIT_TIME},
							SubscriptionsTable.COL_CHANNEL_ID + " = ?",
							new String[]{channelId}, null, null, null);
		long	lastVisitTime = -1;

		if (cursor.moveToNext()) {
			int colLastVisitTIme = cursor.getColumnIndexOrThrow(SubscriptionsTable.COL_LAST_VISIT_TIME);
			lastVisitTime = cursor.getLong(colLastVisitTIme);
		}

		cursor.close();
		return lastVisitTime;
	}

}
