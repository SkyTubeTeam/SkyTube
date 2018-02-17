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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;
import free.rm.skytube.businessobjects.interfaces.OrderableDatabase;

/**
 * A database (DB) that stores user's bookmarked videos.
 */
public class BookmarksDb extends SQLiteOpenHelperEx implements OrderableDatabase {
	private static volatile BookmarksDb bookmarksDb = null;
	private static boolean hasUpdated = false;

	private static final int DATABASE_VERSION = 1;
	private static final String DATABASE_NAME = "bookmarks.db";

	private List<BookmarksDbListener> listeners = new ArrayList<>();


	private BookmarksDb(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}


	public static synchronized BookmarksDb getBookmarksDb() {
		if (bookmarksDb == null) {
			bookmarksDb = new BookmarksDb(SkyTubeApp.getContext());
		}

		return bookmarksDb;
	}


	@Override
	protected void clearDatabaseInstance() {
		bookmarksDb = null;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(BookmarksTable.getCreateStatement());
	}


	@Override
	public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

	}


	/**
	 * Add the specified video to the list of bookmarked videos. The video will appear at the
	 * top of the list (when displayed in the grid, videos will be ordered by the Order
	 * field, descending.
	 *
	 * @param video Video to add
	 *
	 * @return True if the video was successfully saved/bookmarked to the DB.
	 */
	public boolean add(YouTubeVideo video) {
		Gson gson = new Gson();
		ContentValues values = new ContentValues();
		values.put(BookmarksTable.COL_YOUTUBE_VIDEO_ID, video.getId());
		values.put(BookmarksTable.COL_YOUTUBE_VIDEO, gson.toJson(video).getBytes());

		int order = getTotalBookmarks();
		order++;
		values.put(BookmarksTable.COL_ORDER, order);

		boolean addSuccessful = getWritableDatabase().insert(BookmarksTable.TABLE_NAME, null, values) != -1;
		onUpdated();

		return addSuccessful;
	}


	/**
	 * Remove the specified video from the list of bookmarked videos.
	 *
	 * @param video Video to remove.
	 *
	 * @return True if the video has been unbookmarked; false otherwise.
	 */
	public boolean remove(YouTubeVideo video) {
		int rowsDeleted = getWritableDatabase().delete(BookmarksTable.TABLE_NAME,
						BookmarksTable.COL_YOUTUBE_VIDEO_ID + " = ?",
						new String[]{video.getId()});
		boolean successful = false;

		if(rowsDeleted >= 0) {
			// Since we've removed a video, we will need to update the order column for all the videos.
			int order = 1;
			Cursor	cursor = getReadableDatabase().query(
							BookmarksTable.TABLE_NAME,
							new String[]{BookmarksTable.COL_YOUTUBE_VIDEO, BookmarksTable.COL_ORDER},
							null,
							null, null, null, BookmarksTable.COL_ORDER + " ASC");
			if(cursor.moveToNext()) {
				do {
					byte[] blob = cursor.getBlob(cursor.getColumnIndex(BookmarksTable.COL_YOUTUBE_VIDEO));
					YouTubeVideo uvideo = new Gson().fromJson(new String(blob), new TypeToken<YouTubeVideo>(){}.getType());
					ContentValues contentValues = new ContentValues();
					contentValues.put(BookmarksTable.COL_ORDER, order++);

					getWritableDatabase().update(BookmarksTable.TABLE_NAME, contentValues, BookmarksTable.COL_YOUTUBE_VIDEO_ID + " = ?",
									new String[]{uvideo.getId()});
				} while(cursor.moveToNext());
			}

			cursor.close();

			onUpdated();
			successful = true;
		}

		return successful;
	}


	/**
	 * When a Video in the Bookmarks tab is drag & dropped to a new position, this will be
	 * called with the new updated list of videos. Since the videos are displayed in descending order,
	 * the first video in the list will have the highest number.
	 *
	 * @param videos List of Videos to update their order.
	 */
	@Override
	public void updateOrder(List<YouTubeVideo> videos) {
		int order = videos.size();

		for(YouTubeVideo video : videos) {
			ContentValues cv = new ContentValues();
			cv.put(BookmarksTable.COL_ORDER, order--);
			getWritableDatabase().update(BookmarksTable.TABLE_NAME, cv, BookmarksTable.COL_YOUTUBE_VIDEO_ID + " = ?", new String[]{video.getId()});
		}
	}


	/**
	 * Check if the specified Video has been bookmarked.
	 *
	 * @param video Video to check
	 *
	 * @return True if it has been bookmarked, false if not.
	 */
	public boolean isBookmarked(YouTubeVideo video) {
		Cursor cursor = getReadableDatabase().query(
						BookmarksTable.TABLE_NAME,
						new String[]{BookmarksTable.COL_YOUTUBE_VIDEO_ID},
						BookmarksTable.COL_YOUTUBE_VIDEO_ID + " = ?",
						new String[]{video.getId()}, null, null, null);
		boolean	hasVideo = cursor.moveToNext();

		cursor.close();
		return hasVideo;
	}


	/**
	 * @return The total number of bookmarked videos.
	 */
	public int getTotalBookmarks() {
		String	query = String.format("SELECT COUNT(*) FROM %s", BookmarksTable.TABLE_NAME);
		Cursor	cursor = BookmarksDb.getBookmarksDb().getReadableDatabase().rawQuery(query, null);
		int		totalBookmarks = 0;

		if (cursor.moveToFirst()) {
			totalBookmarks = cursor.getInt(0);
		}

		cursor.close();
		return totalBookmarks;
	}


	/**
	 * Get the list of Videos that have been bookmarked.
	 *
	 * @return List of Videos
	 */
	public List<YouTubeVideo> getBookmarkedVideos() {
		Cursor	cursor = getReadableDatabase().query(
						BookmarksTable.TABLE_NAME,
						new String[]{BookmarksTable.COL_YOUTUBE_VIDEO, BookmarksTable.COL_ORDER},
						null,
						null, null, null, BookmarksTable.COL_ORDER + " DESC");
		List<YouTubeVideo> videos = new ArrayList<>();

		if(cursor.moveToNext()) {
			do {
				byte[] blob = cursor.getBlob(cursor.getColumnIndex(BookmarksTable.COL_YOUTUBE_VIDEO));

				// convert JSON into YouTubeVideo
				YouTubeVideo video = new Gson().fromJson(new String(blob), new TypeToken<YouTubeVideo>(){}.getType());
				// regenerate the video's PublishDatePretty (e.g. 5 hours ago)
				video.forceRefreshPublishDatePretty();
				// add the video to the list
				videos.add(video);
			} while(cursor.moveToNext());
		}

		cursor.close();
		return videos;
	}


	/**
	 * Add a Listener that will be notified when a Video is added or removed from Bookmarked Videos. This will
	 * allow the Video Grid to be redrawn in order to remove the video from display.
	 *
	 * @param listener The Listener (which implements BookmarksDbListener) to add.
	 */
	public void addListener(BookmarksDbListener listener) {
		if(!listeners.contains(listener))
			listeners.add(listener);
	}


	/**
	 * Called when the Bookmarks DB is updated by either a bookmark insertion or deletion.
	 */
	private void onUpdated() {
		hasUpdated = true;

		for (BookmarksDbListener listener : listeners)
			listener.onBookmarksDbUpdated();
	}

	public static boolean isHasUpdated() {
		return hasUpdated;
	}

	public static void setHasUpdated(boolean hasUpdated) {
		BookmarksDb.hasUpdated = hasUpdated;
	}



	////////////////////////////////////////////////////////////////////////////////////////////////


	public interface BookmarksDbListener {
		/**
		 * Will be called once the bookmarks DB is updated (by either a bookmark insertion or
		 * deletion).
		 */
		void onBookmarksDbUpdated();
	}

}
