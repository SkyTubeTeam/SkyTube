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
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.app.Utils;
import free.rm.skytube.businessobjects.Logger;
import free.rm.skytube.businessobjects.YouTube.POJOs.CardData;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;
import free.rm.skytube.businessobjects.YouTube.newpipe.VideoId;
import free.rm.skytube.businessobjects.interfaces.OrderableDatabase;

/**
 * A database (DB) that stores user's bookmarked videos.
 */
public class BookmarksDb extends SQLiteOpenHelperEx implements OrderableDatabase {
	private static volatile BookmarksDb bookmarksDb = null;
	private static boolean hasUpdated = false;

	private static final int DATABASE_VERSION = 1;
	private static final String DATABASE_NAME = "bookmarks.db";

	private final Set<BookmarksDbListener> listeners = new HashSet<>();


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
	public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
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
	public DatabaseResult add(YouTubeVideo video) {
		Gson gson = new Gson();
		ContentValues values = new ContentValues();
		values.put(BookmarksTable.COL_YOUTUBE_VIDEO_ID, video.getId());
		values.put(BookmarksTable.COL_YOUTUBE_VIDEO, gson.toJson(video).getBytes());

		int order = getMaximumOrderNumber();
		order++;
		values.put(BookmarksTable.COL_ORDER, order);

		try {
			long result = getWritableDatabase().insertWithOnConflict(BookmarksTable.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_IGNORE);
			Logger.i(this, "Result for adding "+ video+ " IS "+ result);
			if (result >= 1) {
				onBookmarkAdded(video);
				return DatabaseResult.SUCCESS;
			}
			if (isBookmarked(video.getId())) {
				return DatabaseResult.NOT_MODIFIED;
			} else {
				return DatabaseResult.ERROR;
			}
		} catch (SQLException e) {
			Logger.e(this, "Unexpected error in bookmark creation :"+ video+" - error:"+e.getMessage(), e);
			return DatabaseResult.ERROR;
		}
	}

	/**
	 * Remove the specified video from the list of bookmarked videos.
	 *
	 * @param video Video to remove.
	 *
	 * @return True if the video has been unbookmarked; false otherwise.
	 */
	public DatabaseResult remove(VideoId video) {
		try {
			int rowsDeleted = getWritableDatabase().delete(BookmarksTable.TABLE_NAME,
					BookmarksTable.COL_YOUTUBE_VIDEO_ID + " = ?",
					new String[]{video.getId()});

			if (rowsDeleted > 0) {
				// Since we've removed a video, we will need to update the order column for all the videos.
				int order = 1;
				Cursor cursor = getReadableDatabase().query(
						BookmarksTable.TABLE_NAME,
						new String[]{BookmarksTable.COL_YOUTUBE_VIDEO, BookmarksTable.COL_ORDER},
						null,
						null, null, null, BookmarksTable.COL_ORDER + " ASC");
				if (cursor.moveToNext()) {
					Gson gson = new Gson();
					do {
						byte[] blob = cursor.getBlob(cursor.getColumnIndex(BookmarksTable.COL_YOUTUBE_VIDEO));
						YouTubeVideo uvideo = gson.fromJson(new String(blob), YouTubeVideo.class).updatePublishTimestampFromDate();
						ContentValues contentValues = new ContentValues();
						contentValues.put(BookmarksTable.COL_ORDER, order++);

						getWritableDatabase().update(BookmarksTable.TABLE_NAME, contentValues, BookmarksTable.COL_YOUTUBE_VIDEO_ID + " = ?",
								new String[]{uvideo.getId()});
					} while (cursor.moveToNext());
				}

				cursor.close();

				onBookmarkDeleted(video);
				return DatabaseResult.SUCCESS;
			}
				return DatabaseResult.NOT_MODIFIED;
		} catch (SQLException e) {
			Logger.e(this, "Database error: " + e.getMessage(), e);
			return DatabaseResult.ERROR;
		}
	}


	/**
	 * When a Video in the Bookmarks tab is drag & dropped to a new position, this will be
	 * called with the new updated list of videos. Since the videos are displayed in descending order,
	 * the first video in the list will have the highest number.
	 *
	 * @param videos List of Videos to update their order.
	 */
	@Override
	public void updateOrder(List<CardData> videos) {
		int order = getMaximumOrderNumber();

		for(CardData video : videos) {
			ContentValues cv = new ContentValues();
			cv.put(BookmarksTable.COL_ORDER, order--);
			getWritableDatabase().update(BookmarksTable.TABLE_NAME, cv, BookmarksTable.COL_YOUTUBE_VIDEO_ID + " = ?", new String[]{video.getId()});
		}
	}


	/**
	 * Check if the specified Video has been bookmarked.
	 *
	 * @param videoId Video to check
	 *
	 * @return True if it has been bookmarked, false if not.
	 */
	public boolean isBookmarked(String videoId) {
		Cursor cursor = getReadableDatabase().rawQuery("SELECT EXISTS(SELECT "+ BookmarksTable.COL_YOUTUBE_VIDEO_ID +" FROM "+BookmarksTable.TABLE_NAME+" WHERE "+BookmarksTable.COL_YOUTUBE_VIDEO_ID+" =?)",new String[]{videoId});
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
	 * @return The maximum of the order number - which could be different from the number of bookmarked videos, in case some of them are deleted.
	 */
	public int getMaximumOrderNumber() {
		Cursor	cursor = getReadableDatabase().rawQuery(BookmarksTable.MAXIMUM_ORDER_QUERY, null);
		int		maxBookmarkOrder = 0;

		if (cursor.moveToFirst()) {
			maxBookmarkOrder = cursor.getInt(0);
		}

		cursor.close();
		return maxBookmarkOrder;
	}


	/**
	 * Get the list of Videos that have been bookmarked.
	 *
	 * @return List of Videos
	 */
	public @NonNull Pair<List<YouTubeVideo>, Integer> getBookmarkedVideos(int limit, Integer maxOrderLimit) {
        //Logger.i(this, "getBookmarkedVideos " + limit + ',' + maxOrderLimit +
        //        " - " + (maxOrderLimit != null ? BookmarksTable.PAGED_QUERY : BookmarksTable.PAGED_QUERY_UNBOUNDED));

        SQLiteDatabase db = getReadableDatabase();
        Cursor	cursor = maxOrderLimit != null ?
                db.rawQuery(
                        BookmarksTable.PAGED_QUERY, new String[] { String.valueOf(maxOrderLimit), String.valueOf(limit)}) :
                db.rawQuery(
                        BookmarksTable.PAGED_QUERY_UNBOUNDED, new String[] { String.valueOf(limit)});

		List<YouTubeVideo> videos = new ArrayList<>();

		final Gson gson = new Gson();
		Integer minOrder = null;
		if(cursor.moveToNext()) {
			final int colOrder = cursor.getColumnIndex(BookmarksTable.COL_ORDER);
			final int colVideo = cursor.getColumnIndex(BookmarksTable.COL_YOUTUBE_VIDEO);
			do {
				final byte[] blob = cursor.getBlob(colVideo);
				final int currentOrder = cursor.getInt(colOrder);

                minOrder = Utils.min(currentOrder, minOrder);

				final String videoJson = new String(blob);

				// convert JSON into YouTubeVideo
				YouTubeVideo video = gson.fromJson(videoJson, YouTubeVideo.class).updatePublishTimestampFromDate();

                // Logger.i(this, "Order "+cursor.getInt(colOrder)+ ", id="+video.getId()+","+video.getTitle());

                // due to upgrade to YouTubeVideo (by changing channel{Id,Name} to YouTubeChannel)
				// from version 2.82 to 2.90
				if (video.getChannel() == null) {
					try {
						JSONObject videoJsonObj = new JSONObject(videoJson);
						final String channelId   = videoJsonObj.get("channelId").toString();
						final String channelName = videoJsonObj.get("channelName").toString();
						video.setChannel(new YouTubeChannel(channelId, channelName));
					} catch (JSONException e) {
						Logger.e(this, "Error occurred while extracting channel{Id,Name} from JSON", e);
					}
				}

				// regenerate the video's PublishDatePretty (e.g. 5 hours ago)
				//video.forceRefreshPublishDatePretty();
				// add the video to the list
				videos.add(video);
			} while(cursor.moveToNext());
		}

		cursor.close();
		return Pair.create(videos, minOrder);
	}


	/**
	 * Add a Listener that will be notified when a Video is added or removed from Bookmarked Videos. This will
	 * allow the Video Grid to be redrawn in order to remove the video from display.
	 *
	 * @param listener The Listener (which implements BookmarksDbListener) to add.
	 */
	public void addListener(@NonNull BookmarksDbListener listener) {
		listeners.add(listener);
	}

	/**
	 * Remove the Listener
	 *
	 * @param listener The Listener (which implements BookmarksDbListener) to remove.
	 */
	public void removeListener(@NonNull BookmarksDbListener listener) {
		listeners.remove(listener);
	}

	/**
	 * Called when the Bookmarks DB is updated by a bookmark insertion.
	 */
	private void onBookmarkAdded(YouTubeVideo video) {
		for (BookmarksDbListener listener : listeners) {
			listener.onBookmarkAdded(video);
		}
	}

	/**
	 * Called when the Bookmarks DB is updated by deletion.
	 */
	private void onBookmarkDeleted(VideoId video) {
		for (BookmarksDbListener listener : listeners) {
			listener.onBookmarkDeleted(video);
		}
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
		 * Will be called once the bookmarks DB is updated - by a bookmark insertion.
		 */
		void onBookmarkAdded(YouTubeVideo video);

		/**
		 * Will be called once the bookmarks DB is updated - by a bookmark deletion.
		 */
		void onBookmarkDeleted(VideoId videoId);
	}

}
