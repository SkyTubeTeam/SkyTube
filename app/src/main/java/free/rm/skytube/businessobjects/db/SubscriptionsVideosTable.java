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
 * A table that caches metadata about videos published by subbed channels.
 */
public class SubscriptionsVideosTable {
	public static final String TABLE_NAME = "SubsVideos";
	public static final String COL_CHANNEL_ID = "Channel_Id";
	public static final String COL_YOUTUBE_VIDEO_ID = "YouTube_Video_Id";
	public static final String COL_YOUTUBE_VIDEO = "YouTube_Video";
	public static final String COL_YOUTUBE_VIDEO_DATE = "YouTube_Video_Date";

	public static String getCreateStatement() {
		return "CREATE TABLE " + TABLE_NAME + " (" +
						COL_YOUTUBE_VIDEO_ID + " TEXT PRIMARY KEY NOT NULL, " +
						COL_CHANNEL_ID + " TEXT NOT NULL, " +
						COL_YOUTUBE_VIDEO + " BLOB, " +
						COL_YOUTUBE_VIDEO_DATE + " TIMESTAMP DEFAULT (strftime('%s', 'now')) " +
						" )";
	}
}
