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
 * A table that caches metadata about videos published by subbed channels.
 */
public class SubscriptionsVideosTable {
	public static final String TABLE_NAME = "SubsVideos";
	public static final String COL_CHANNEL_ID = "Channel_Id";
	public static final String COL_YOUTUBE_VIDEO_ID = "YouTube_Video_Id";
	public static final String COL_YOUTUBE_VIDEO = "YouTube_Video";
	public static final String COL_YOUTUBE_VIDEO_DATE = "YouTube_Video_Date";
	public static final String COL_RETRIEVAL_TS = "Retrieval_Timestamp";
	public static final String COL_PUBLISH_TS = "Publish_Timestamp";
	public static final Column COL_CATEGORY_ID = new Column("category_id", "INTEGER");

	public static final Column COL_TITLE = new Column("title", "text");
	public static final Column COL_DESCRIPTION = new Column("description", "text");
	public static final Column COL_LIKES = new Column("like_count", "integer");
	public static final Column COL_DISLIKES = new Column("dislike_count", "integer");
	public static final Column COL_VIEWS = new Column("view_count", "integer");

	public static final String COL_YOUTUBE_VIDEO_ID_EQUALS_TO = SubscriptionsVideosTable.COL_YOUTUBE_VIDEO_ID + " = ?";

	private static final String IDX_PUBLISH_TS = "IDX_SubsVideo_Publish";

	static final String[] ALL_COLUMNS_FOR_EXTRACT = new String[] {
			COL_CHANNEL_ID,
			COL_YOUTUBE_VIDEO_ID,
			COL_YOUTUBE_VIDEO,
			COL_CATEGORY_ID.name,
			COL_RETRIEVAL_TS,
			COL_PUBLISH_TS
	};

	private static final String ADD_COLUMN = "ALTER TABLE " + TABLE_NAME + " ADD COLUMN ";

    public static String getCreateStatement() {
        return "CREATE TABLE " + TABLE_NAME + " (" +
                        COL_YOUTUBE_VIDEO_ID + " TEXT PRIMARY KEY NOT NULL, " +
                        COL_CHANNEL_ID + " TEXT NOT NULL, " +
                        COL_TITLE.format() +
                        COL_DESCRIPTION.format() +
                        COL_LIKES.format() +
                        COL_DISLIKES.format() +
                        COL_VIEWS.format() +
                        COL_YOUTUBE_VIDEO + " BLOB, " +
                        COL_YOUTUBE_VIDEO_DATE + " TIMESTAMP DEFAULT (strftime('%s', 'now')), " +
                        COL_CATEGORY_ID.format() +
                        COL_RETRIEVAL_TS + " INTEGER, " +
                        COL_PUBLISH_TS + " INTEGER " +
                        " )";
    }

	public static String[] getAddTimestampColumns() {
		return new String[]{
				ADD_COLUMN + COL_RETRIEVAL_TS + " INTEGER",
				ADD_COLUMN + COL_PUBLISH_TS + " INTEGER",
		};
	}

    public static String getIndexOnVideos() {
        return "CREATE INDEX " + IDX_PUBLISH_TS + " ON " + TABLE_NAME + "(" + COL_PUBLISH_TS + ")";
    }

    public static void addCategoryColumn(SQLiteDatabase db) {
        SQLiteOpenHelperEx.addColumn(db, TABLE_NAME, COL_CATEGORY_ID);
    }

    public static void addVideoInformationColumns(SQLiteDatabase db) {
        SQLiteOpenHelperEx.addColumn(db, TABLE_NAME, COL_TITLE);
        SQLiteOpenHelperEx.addColumn(db, TABLE_NAME, COL_DESCRIPTION);
        SQLiteOpenHelperEx.addColumn(db, TABLE_NAME, COL_LIKES);
        SQLiteOpenHelperEx.addColumn(db, TABLE_NAME, COL_DISLIKES);
        SQLiteOpenHelperEx.addColumn(db, TABLE_NAME, COL_VIEWS);
    }
}
