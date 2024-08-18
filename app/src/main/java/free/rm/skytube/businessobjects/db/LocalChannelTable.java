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

import androidx.annotation.NonNull;

import com.github.skytube.components.utils.Column;
import com.github.skytube.components.utils.SQLiteHelper;
import com.google.common.base.Joiner;

import free.rm.skytube.businessobjects.YouTube.POJOs.PersistentChannel;
import free.rm.skytube.businessobjects.YouTube.newpipe.ChannelId;
import free.rm.skytube.businessobjects.model.Status;

public class LocalChannelTable {

    public static final String TABLE_NAME = "Channel";
    public static final String COL_CHANNEL_ID_name = "Channel_Id";
    public static final String COL_LAST_VIDEO_TS = "Last_Video_TS";
    public static final String COL_LAST_CHECK_TS = "Last_Check_TS";
    public static final String COL_TITLE = "Title";
    public static final String COL_DESCRIPTION = "Description";
    public static final String COL_THUMBNAIL_NORMAL_URL = "Thumbnail_Normal_Url";
    public static final String COL_BANNER_URL = "Banner_Url";
    public static final String COL_SUBSCRIBER_COUNT = "Subscriber_Count";
    public static final Column COL_ID = new Column("_id", "integer", " primary key");
    public static final Column COL_CHANNEL_ID = new Column(COL_CHANNEL_ID_name, "text", "UNIQUE NOT NULL");
    public static final Column COL_STATE = new Column("state", "integer", "default 0");

    static final String GET_ID_AND_CHANNEL_ID = String.format("SELECT %s, %s FROM %s", LocalChannelTable.COL_ID.name(), LocalChannelTable.COL_CHANNEL_ID.name(), LocalChannelTable.TABLE_NAME);

    private static final String[] ALL_COLUMNS = new String[]{
            LocalChannelTable.COL_CHANNEL_ID.name(),
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
                COL_CHANNEL_ID.format() + ", " +
                COL_STATE.format() + ", " +
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
        final String allRowNames = Joiner.on(',').join(ALL_COLUMNS);
        SQLiteHelper.updateTableSchema(db, TABLE_NAME, getCreateStatement(true),
                "insert into " + TABLE_NAME + " (_id," + allRowNames + ") select rowid," + allRowNames);
    }

    public static void updateLatestVideoTimestamp(SQLiteDatabase db, PersistentChannel persistentChannel, long latestPublishTimestamp) {
        db.execSQL("update " + TABLE_NAME + " set " + COL_LAST_VIDEO_TS + " = max(?, coalesce(" + COL_LAST_VIDEO_TS + ",0)) where " + COL_ID.name() + " = ?", new Object[]{
                latestPublishTimestamp, persistentChannel.subscriptionPk()});
    }

    public static void updateChannelStatus(SQLiteDatabase db, @NonNull ChannelId channelId, @NonNull Status status) {
        db.execSQL("update " + TABLE_NAME + " set " + COL_STATE.name() + " = ? where " + COL_CHANNEL_ID.name() + " = ?",
                new Object[] { status.code, channelId.getRawId() });
    }

    public static void addChannelIdIndex(SQLiteDatabase db) {
        SQLiteHelper.createIndex(db, "IDX_channel_channelId", TABLE_NAME, COL_CHANNEL_ID);
    }

    public static void addStateColumn(SQLiteDatabase db) {
        SQLiteHelper.addColumn(db, TABLE_NAME, COL_STATE);
    }
}
