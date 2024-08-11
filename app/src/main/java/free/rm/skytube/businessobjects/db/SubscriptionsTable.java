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

import android.database.sqlite.SQLiteDatabase;

import com.github.skytube.components.utils.Column;
import com.github.skytube.components.utils.SQLiteHelper;

import free.rm.skytube.businessobjects.YouTube.POJOs.PersistentChannel;

/**
 * YouTube channels subscriptions table.
 */
public class SubscriptionsTable {

    public static final String TABLE_NAME = "Subs";
    public static final String COL_ID = "_id";
    public static final String COL_CHANNEL_ID = "Channel_Id";
    public static final String COL_LAST_VISIT_TIME = "Last_Visit_Time";
    public static final String COL_LAST_CHECK_TIME = "Last_Check_Time";
    private static final String COL_TITLE = "Title";
    private static final String COL_DESCRIPTION = "Description";
    private static final String COL_THUMBNAIL_NORMAL_URL = "Thumbnail_Normal_Url";
    private static final String COL_BANNER_URL = "Banner_Url";
    private static final String COL_SUBSCRIBER_COUNT = "Subscriber_Count";
    public static final Column COL_CATEGORY_ID = new Column("category_id", "INTEGER");
    public static final String COL_LAST_VIDEO_FETCH = "last_video_fetch_time";
    public static final Column COL_CHANNEL_PK = new Column("channel_pk", "integer");

    private static final String ADD_COLUMN = "ALTER TABLE " + TABLE_NAME + " ADD COLUMN ";

    static final String GET_ID_AND_CHANNEL_ID = String.format("SELECT %s, %s FROM %s", SubscriptionsTable.COL_ID, SubscriptionsTable.COL_CHANNEL_ID, SubscriptionsTable.TABLE_NAME);

    public static String getCreateStatement() {
        return "CREATE TABLE " + TABLE_NAME + " (" +
                COL_ID + " INTEGER PRIMARY KEY ASC, " +
                COL_CHANNEL_ID + " TEXT UNIQUE NOT NULL, " +
                COL_CHANNEL_PK.format() + ", " +
                COL_CATEGORY_ID.format() + ", " +
                COL_LAST_VISIT_TIME + " TIMESTAMP DEFAULT (strftime('%s', 'now')), " +
                COL_LAST_VIDEO_FETCH + " INTEGER " +
                " )";
    }

    public static String[] getAddColumns() {
        return new String[]{
                ADD_COLUMN + COL_TITLE + " TEXT",
                ADD_COLUMN + COL_DESCRIPTION + " TEXT",
                ADD_COLUMN + COL_THUMBNAIL_NORMAL_URL + " TEXT",
                ADD_COLUMN + COL_BANNER_URL + " TEXT",
                ADD_COLUMN + COL_SUBSCRIBER_COUNT + " INTEGER"
        };
    }

    public static void addCategoryColumn(SQLiteDatabase db) {
        SQLiteHelper.addColumn(db, TABLE_NAME, COL_CATEGORY_ID);
    }

    public static String[] getLastCheckTimeColumn() {
        return new String[]{ADD_COLUMN + COL_LAST_CHECK_TIME + " INTEGER "};
    }

    public static void cleanupTable(SQLiteDatabase db) {
        SQLiteHelper.updateTableSchema(db, TABLE_NAME, getCreateStatement(),
                "insert into " + TABLE_NAME + " (_id," + COL_CHANNEL_ID + "," + COL_CATEGORY_ID.name() + "," + COL_LAST_VISIT_TIME + "," + COL_LAST_VIDEO_FETCH +
                ") select _id," + COL_CHANNEL_ID + "," + COL_CATEGORY_ID.name() + "," + COL_LAST_VISIT_TIME + "," + COL_LAST_CHECK_TIME );
    }

    public static void updateLastVideoFetchTimestamps(SQLiteDatabase db, PersistentChannel persistentChannel) {
        if (persistentChannel.isSubscribed()) {
            db.execSQL("update " + TABLE_NAME + " set " + COL_LAST_VIDEO_FETCH + " = ? where " + COL_ID + " = ?", new Object[] {
                    System.currentTimeMillis(), persistentChannel.subscriptionPk() });
        }
    }

    public static void addChannelIdColumn(SQLiteDatabase db) {
        SQLiteHelper.addColumn(db, TABLE_NAME, COL_CHANNEL_PK);
        db.execSQL("update " + TABLE_NAME +
                " set " + COL_CHANNEL_PK.name() +
                    " = (select " + LocalChannelTable.COL_ID.name() + " from " + LocalChannelTable.TABLE_NAME + " c where c." + LocalChannelTable.COL_CHANNEL_ID_name + " = " + TABLE_NAME + '.' + COL_CHANNEL_ID + ")");
    }
}
