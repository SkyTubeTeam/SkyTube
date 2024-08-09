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

import com.google.common.base.Joiner;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A table that caches metadata about videos published by subbed channels.
 */
public class SubscriptionsVideosTable {
    /**
     * @deprecated This table is not used anymore
     */
    @Deprecated
    public static final String TABLE_NAME = "SubsVideos";
    public static final String COL_CHANNEL_ID = "Channel_Id";
    public static final String COL_YOUTUBE_VIDEO_ID = "YouTube_Video_Id";
    public static final String COL_YOUTUBE_VIDEO = "YouTube_Video";
    public static final String COL_YOUTUBE_VIDEO_DATE = "YouTube_Video_Date";
    public static final String COL_RETRIEVAL_TS = "Retrieval_Timestamp";
    public static final String COL_PUBLISH_TS = "Publish_Timestamp";
    public static final Column COL_CATEGORY_ID = new Column("category_id", "INTEGER");

    public static final String TABLE_NAME_V2 = "subscription_videos";
    public static final Column COL_CHANNEL_ID_V2 = new Column(COL_CHANNEL_ID, "text", "not null");
    public static final Column COL_YOUTUBE_VIDEO_ID_V2 = new Column(COL_YOUTUBE_VIDEO_ID, "text", "primary key not null");
    public static final Column COL_CHANNEL_TITLE = new Column("channel_title", "text");
    public static final Column COL_TITLE = new Column("title", "text");
    public static final Column COL_DESCRIPTION = new Column("description", "text");
    public static final Column COL_LIKES = new Column("like_count", "integer", "not null default 0");
    public static final Column COL_DISLIKES = new Column("dislike_count", "integer", "not null default 0");
    public static final Column COL_VIEWS = new Column("view_count", "integer", "not null default 0");
    public static final Column COL_PUBLISH_TIME_EXACT = new Column("publish_time_exact", "integer", "not null default 0");
    public static final Column COL_DURATION = new Column("duration", "integer", "not null default 0");
    public static final Column COL_PUBLISH_TIME = new Column("publish_time", "integer", "not null default 0");
    public static final Column COL_THUMBNAIL_URL = new Column("thumbnail_url", "text");
    public static final Column COL_SUBS_ID = new Column("subs_id", "integer");
    public static final Column COL_CHANNEL_PK = new Column("channel_pk", "integer");

	public static final String COL_YOUTUBE_VIDEO_ID_EQUALS_TO = SubscriptionsVideosTable.COL_YOUTUBE_VIDEO_ID + " = ?";

	private static final String IDX_PUBLISH_TS = "IDX_SubsVideo_Publish";
    private static final String IDX_PUBLISH_TS_V2 = "IDX_subscription_videos_Publish";
    private static final String IDX_PUBLISH_TIMESTAMP = "IDX_subscription_videos_PublishTime";

    static final String[] ALL_COLUMNS_FOR_EXTRACT = new String[] {
            COL_CHANNEL_ID_V2.name,
            COL_YOUTUBE_VIDEO_ID_V2.name,
            COL_CATEGORY_ID.name,
            COL_TITLE.name,
            COL_DESCRIPTION.name,
            COL_THUMBNAIL_URL.name,
            COL_LIKES.name,
            COL_DISLIKES.name,
            COL_VIEWS.name,
            COL_DURATION.name,
            COL_PUBLISH_TIME.name,
            COL_PUBLISH_TIME_EXACT.name,
    };

    static final String BASE_QUERY;
    static {
        StringBuilder s = new StringBuilder("select c.Title channel_title");
        for (String col : ALL_COLUMNS_FOR_EXTRACT) {
            s.append(",s.").append(col);
        }
        s.append(" from subscription_videos s left join Channel c on s.channel_pk = c._id ");
        BASE_QUERY = s.toString();
    }


    private static final String ADD_COLUMN = "ALTER TABLE " + TABLE_NAME + " ADD COLUMN ";

    public static String getCreateStatement() {
        return "CREATE TABLE " + TABLE_NAME + " (" +
                        COL_YOUTUBE_VIDEO_ID + " TEXT PRIMARY KEY NOT NULL, " +
                        COL_CHANNEL_ID + " TEXT NOT NULL, " +
                        COL_TITLE.format() + ',' +
                        COL_DESCRIPTION.format() + ',' +
                        COL_LIKES.format() + ',' +
                        COL_DISLIKES.format() + ',' +
                        COL_VIEWS.format() + ',' +
                        COL_DURATION.format() + ',' +
                        COL_THUMBNAIL_URL.format() + ',' +
                        COL_YOUTUBE_VIDEO + " BLOB, " +
                        COL_YOUTUBE_VIDEO_DATE + " TIMESTAMP DEFAULT (strftime('%s', 'now')), " +
                        COL_CATEGORY_ID.format() + ',' +
                        COL_RETRIEVAL_TS + " INTEGER, " +
                        COL_PUBLISH_TS + " INTEGER " +
                        " )";
    }

    static String getDropTableStatement() {
        return "DROP TABLE "  + TABLE_NAME;
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
    private static Column[] getAllColumns(boolean withChannelTitle) {
        return new Column[] {
                COL_CHANNEL_PK,
                COL_SUBS_ID,
                COL_CHANNEL_ID_V2,
                COL_YOUTUBE_VIDEO_ID_V2,
                COL_CATEGORY_ID,
                withChannelTitle ? COL_CHANNEL_TITLE : null,
                COL_TITLE,
                COL_DESCRIPTION,
                COL_THUMBNAIL_URL,
                COL_LIKES,
                COL_DISLIKES,
                COL_VIEWS,
                COL_DURATION,
                COL_PUBLISH_TIME,
                COL_PUBLISH_TIME_EXACT
        };
    }

    public static void addNewFlatTable(SQLiteDatabase db, boolean withChannelTitle) {
        db.execSQL(SQLiteOpenHelperEx.getCreateTableCommand(TABLE_NAME_V2, getAllColumns(withChannelTitle)));
        SQLiteOpenHelperEx.createIndex(db, IDX_PUBLISH_TS_V2, TABLE_NAME_V2, COL_CATEGORY_ID);
    }

    static void addSubsIdColumn(SQLiteDatabase db) {
        SQLiteOpenHelperEx.addColumn(db, TABLE_NAME_V2, COL_SUBS_ID);
    }

    static void addChannelPkColumn(SQLiteDatabase db) {
        SQLiteOpenHelperEx.addColumn(db, TABLE_NAME_V2, COL_CHANNEL_PK);
    }

    static void removeChannelTitle(SQLiteDatabase db) {
        final Column[] allColumns = getAllColumns(false);

        String columnList = Stream.of(allColumns).filter(it -> it != null).map(Column::name).collect(Collectors.joining(","));

        SQLiteOpenHelperEx.updateTableSchema(db, TABLE_NAME_V2, SQLiteOpenHelperEx.getCreateTableCommand(TABLE_NAME_V2, allColumns),
                "insert into " + TABLE_NAME_V2 + " (" + columnList +
                        ") select " + columnList
        );
    }

    static void addPublishTimeIndex(SQLiteDatabase db) {
        SQLiteOpenHelperEx.createIndex(db, IDX_PUBLISH_TIMESTAMP, TABLE_NAME_V2, COL_PUBLISH_TIME);
    }
}
