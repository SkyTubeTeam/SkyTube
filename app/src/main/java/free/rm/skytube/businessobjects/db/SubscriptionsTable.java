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

/**
 * YouTube channels subscriptions table.
 */
public class SubscriptionsTable {

	public static final String TABLE_NAME = "Subs";
	public static final String COL_ID  = "_id";
	public static final String COL_CHANNEL_ID = "Channel_Id";
	public static final String COL_LAST_VISIT_TIME = "Last_Visit_Time";
	public static final String COL_LAST_CHECK_TIME = "Last_Check_Time";
	public static final String COL_TITLE = "Title";
	public static final String COL_DESCRIPTION = "Description";
	public static final String COL_THUMBNAIL_NORMAL_URL = "Thumbnail_Normal_Url";
	public static final String COL_BANNER_URL = "Banner_Url";
	public static final String COL_SUBSCRIBER_COUNT = "Subscriber_Count";
	public static final Column COL_CATEGORY_ID = new Column("category_id", "INTEGER");
	public static final String[] ALL_COLUMNS = new String[]{
			SubscriptionsTable.COL_CHANNEL_ID,
			SubscriptionsTable.COL_TITLE,
			SubscriptionsTable.COL_DESCRIPTION,
			SubscriptionsTable.COL_BANNER_URL,
			SubscriptionsTable.COL_THUMBNAIL_NORMAL_URL,
			SubscriptionsTable.COL_SUBSCRIBER_COUNT,
			SubscriptionsTable.COL_CATEGORY_ID.name,
			SubscriptionsTable.COL_LAST_VISIT_TIME,
			SubscriptionsTable.COL_LAST_CHECK_TIME};

	private static final String ADD_COLUMN = "ALTER TABLE " + TABLE_NAME + " ADD COLUMN ";

	public static String getCreateStatement() {
		return "CREATE TABLE " + TABLE_NAME + " (" +
				COL_ID         + " INTEGER PRIMARY KEY ASC, " +
				COL_CHANNEL_ID + " TEXT UNIQUE NOT NULL, " +
				COL_TITLE      + " TEXT, " +
				COL_DESCRIPTION     	+ " TEXT, " +
				COL_THUMBNAIL_NORMAL_URL+ " TEXT, " +
				COL_BANNER_URL      	+ " TEXT, " +
				COL_SUBSCRIBER_COUNT	+ " INTEGER, " +
				COL_CATEGORY_ID.format() + ", " +
				COL_LAST_VISIT_TIME 	+ " TIMESTAMP DEFAULT (strftime('%s', 'now')), " +
				COL_LAST_CHECK_TIME 	+ " INTEGER " +
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
        SQLiteOpenHelperEx.addColumn(db, TABLE_NAME, COL_CATEGORY_ID);
    }

	public static String[] getLastCheckTimeColumn() {
		return new String[] { ADD_COLUMN + COL_LAST_CHECK_TIME + " INTEGER "};
	}
}
