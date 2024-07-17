/*
 * SkyTube
 * Copyright (C) 2019  Zsombor Gegesy
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

import android.database.sqlite.SQLiteDatabase;

import com.google.common.base.Joiner;

import free.rm.skytube.businessobjects.YouTube.POJOs.PersistentChannel;

public class LocalChannelTable {
    public static final String TABLE_NAME = "Channel";
    public static final String COL_CHANNEL_ID = "Channel_Id";
    public static final String COL_LAST_VIDEO_TS = "Last_Video_TS";
    public static final String COL_LAST_CHECK_TS = "Last_Check_TS";
    public static final String COL_TITLE = "Title";
    public static final String COL_DESCRIPTION = "Description";
    public static final String COL_THUMBNAIL_NORMAL_URL = "Thumbnail_Normal_Url";
    public static final String COL_BANNER_URL = "Banner_Url";
    public static final String COL_SUBSCRIBER_COUNT = "Subscriber_Count";
    public static final Column COL_ID = new Column("_id", "integer", " primary key");

    static final String GET_ID_AND_CHANNEL_ID = String.format("SELECT %s, %s FROM %s", LocalChannelTable.COL_ID.name, LocalChannelTable.COL_CHANNEL_ID, LocalChannelTable.TABLE_NAME);

    public static final String[] ALL_COLUMNS = new String[]{
            LocalChannelTable.COL_CHANNEL_ID,
            LocalChannelTable.COL_TITLE,
            LocalChannelTable.COL_DESCRIPTION,
            LocalChannelTable.COL_BANNER_URL,
            LocalChannelTable.COL_THUMBNAIL_NORMAL_URL,
            LocalChannelTable.COL_SUBSCRIBER_COUNT,
            LocalChannelTable.COL_LAST_VIDEO_TS,
            LocalChannelTable.COL_LAST_CHECK_TS};

    public static String getCreateStatement(boolean withPk) {
        return "CREATE TABLE " + TABLE_NAME + " (" +
                (withPk ? COL_ID.format() + "," : "") +
                COL_CHANNEL_ID + " TEXT UNIQUE NOT NULL, " +
                COL_TITLE      + " TEXT, " +
                COL_DESCRIPTION     	+ " TEXT, " +
                COL_THUMBNAIL_NORMAL_URL+ " TEXT, " +
                COL_BANNER_URL      	+ " TEXT, " +
                COL_SUBSCRIBER_COUNT	+ " INTEGER, " +
                COL_LAST_VIDEO_TS 	+ " INTEGER, " +
                COL_LAST_CHECK_TS 	+ " INTEGER " +
                " )";
    }

    public static final void addIdColumn(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE " + TABLE_NAME + " RENAME TO old_" + TABLE_NAME);
        db.execSQL(getCreateStatement(true));
        String allRowNames = Joiner.on(',').join(ALL_COLUMNS);
        db.execSQL("insert into " + TABLE_NAME + " (_id," + allRowNames + ") select rowid," + allRowNames + " from old_" + TABLE_NAME);
        db.execSQL("DROP TABLE old_" + TABLE_NAME);
    }

    public static void updateLatestVideoTimestamp(SQLiteDatabase db, PersistentChannel persistentChannel, long latestPublishTimestamp) {
        db.execSQL("update " + TABLE_NAME + " set " + COL_LAST_VIDEO_TS + " = max(?, "+COL_LAST_VIDEO_TS+") where " + COL_ID.name + " = ?", new Object[] {
                latestPublishTimestamp, persistentChannel.subscriptionPk() });
    }
}
