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

/**
 * Bookmarked Videos Table
 */
public class BookmarksTable {
	public static final String TABLE_NAME = "Bookmarks";
	public static final String COL_YOUTUBE_VIDEO_ID = "YouTube_Video_Id";
	public static final String COL_YOUTUBE_VIDEO = "YouTube_Video";
	public static final String COL_ORDER = "Order_Index";


	static final String COUNT_ALL_BOOKMARKS = String.format("SELECT COUNT(*) FROM %s", BookmarksTable.TABLE_NAME);
	static final String MAXIMUM_ORDER_QUERY = String.format("SELECT MAX(%s) FROM %s", COL_ORDER, TABLE_NAME);
	static final String PAGED_QUERY = String.format("SELECT %1$s,%2$s FROM %3$s WHERE %2$s < ? ORDER BY %2$s DESC LIMIT ?", COL_YOUTUBE_VIDEO, COL_ORDER, TABLE_NAME);
	static final String PAGED_QUERY_UNBOUNDED = String.format("SELECT %1$s,%2$s FROM %3$s ORDER BY %2$s DESC LIMIT ?", COL_YOUTUBE_VIDEO, COL_ORDER, TABLE_NAME);
	static final String IS_BOOKMARKED_QUERY = String.format("SELECT 1 FROM %s WHERE %s =?", TABLE_NAME, COL_YOUTUBE_VIDEO_ID);


	public static String getCreateStatement() {
		return "CREATE TABLE " + TABLE_NAME + " (" +
						COL_YOUTUBE_VIDEO_ID + " TEXT PRIMARY KEY NOT NULL, " +
						COL_YOUTUBE_VIDEO + " BLOB, " +
						COL_ORDER + " INTEGER " +
						" )";
	}

}
