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
import android.database.sqlite.SQLiteOpenHelper;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

import free.rm.skytube.businessobjects.YouTubeVideo;
import free.rm.skytube.gui.app.SkyTubeApp;

public class SavedVideosDb extends SQLiteOpenHelper {
	private static volatile SavedVideosDb savedVideosDb = null;
	private static boolean hasUpdated = false;

	private static final int DATABASE_VERSION = 2;
	private static final String DATABASE_NAME = "savedvideos.db";

	private List<SavedVideosDbListener> listeners = new ArrayList<>();

	public interface SavedVideosDbListener {
		void onUpdated();
	}

	private SavedVideosDb(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	public static synchronized SavedVideosDb getSavedVideosDb() {
		if (savedVideosDb == null) {
			savedVideosDb = new SavedVideosDb(SkyTubeApp.getContext());
		}

		return savedVideosDb;
	}
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(SavedVideosTable.getCreateStatement());
	}

	@Override
	public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

	}

	/**
	 * Add the specified video to the list of Saved Videos. The video will appear at the
	 * top of the list (when displayed in the grid, videos will be ordered by the Order
	 * field, descending.
	 *
	 * @param video Video to add
	 */
	public void add(YouTubeVideo video) {
		Gson gson = new Gson();
		ContentValues values = new ContentValues();
		values.put(SavedVideosTable.COL_YOUTUBE_VIDEO_ID, video.getId());
		values.put(SavedVideosTable.COL_YOUTUBE_VIDEO, gson.toJson(video).getBytes());
		int order = getNumVideos();
		order++;
		values.put(SavedVideosTable.COL_ORDER, order);
		getWritableDatabase().insert(SavedVideosTable.TABLE_NAME, null, values);
		onUpdated();
	}

	/**
	 * Remove the specified video from the list of Saved Videos.
	 *
	 * @param video Video to remove
	 * @return
	 */
	public boolean remove(YouTubeVideo video) {
		getWritableDatabase().delete(SavedVideosTable.TABLE_NAME,
						SavedVideosTable.COL_YOUTUBE_VIDEO_ID + " = ?",
						new String[]{video.getId()});

		int rowsDeleted = getWritableDatabase().delete(SavedVideosTable.TABLE_NAME,
						SavedVideosTable.COL_YOUTUBE_VIDEO_ID + " = ?",
						new String[]{video.getId()});

		if(rowsDeleted >= 0) {
			// Since we've removed a video, we will need to update the order column for all the videos.
			int order = 1;
			Cursor	cursor = getReadableDatabase().query(
							SavedVideosTable.TABLE_NAME,
							new String[]{SavedVideosTable.COL_YOUTUBE_VIDEO, SavedVideosTable.COL_ORDER},
							null,
							null, null, null, SavedVideosTable.COL_ORDER + " ASC");
			if(cursor.moveToNext()) {
				do {
					byte[] blob = cursor.getBlob(cursor.getColumnIndex(SavedVideosTable.COL_YOUTUBE_VIDEO));
					YouTubeVideo uvideo = new Gson().fromJson(new String(blob), new TypeToken<YouTubeVideo>(){}.getType());
					ContentValues contentValues = new ContentValues();
					contentValues.put(SavedVideosTable.COL_ORDER, order++);

					getWritableDatabase().update(SavedVideosTable.TABLE_NAME, contentValues, SavedVideosTable.COL_YOUTUBE_VIDEO_ID + " = ?",
									new String[]{uvideo.getId()});
				} while(cursor.moveToNext());
			}
			onUpdated();
		}
		return (rowsDeleted >= 0);
	}

	/**
	 * When a Video in the Saved Videos tab is drag & dropped to a new position, this will be
	 * called with the new updated list of videos. Since the videos are displayed in descending order,
	 * the first video in the list will have the highest number.
	 *
	 * @param videos List of Videos to update their order.
	 */
	public void updateOrder(List<YouTubeVideo> videos) {
		int order = videos.size();
		for(YouTubeVideo video : videos) {
			ContentValues cv = new ContentValues();
			cv.put(SavedVideosTable.COL_ORDER, order--);
			getWritableDatabase().update(SavedVideosTable.TABLE_NAME, cv, SavedVideosTable.COL_YOUTUBE_VIDEO_ID + " = ?", new String[]{video.getId()});
		}
	}

	/**
	 * Check if the specified Video has been added to Saved Videos.
	 *
	 * @param video Video to check
	 * @return True if it has been added, false if not.
	 */
	public boolean hasVideo(YouTubeVideo video) {
		Cursor cursor = getReadableDatabase().query(
						SavedVideosTable.TABLE_NAME,
						new String[]{SavedVideosTable.COL_YOUTUBE_VIDEO_ID},
						SavedVideosTable.COL_YOUTUBE_VIDEO_ID + " = ?",
						new String[]{video.getId()}, null, null, null);
		boolean	hasVideo = cursor.moveToNext();
		return hasVideo;
	}

	/**
	 * Get the number of videos that have been saved.
	 *
	 * @return int, the number of videos
	 */
	public int getNumVideos() {
		String query = String.format("SELECT COUNT(*) FROM %s", SavedVideosTable.TABLE_NAME);
		Cursor cursor = SavedVideosDb.getSavedVideosDb().getReadableDatabase().rawQuery(query, null);

		if(cursor.moveToFirst()) {
			return cursor.getInt(0);
		}
		return 0;
	}

	/**
	 * Get the list of Videos that have been saved.
	 *
	 * @return List of Videos
	 */
	public List<YouTubeVideo> getSavedVideos() {
		Cursor	cursor = getReadableDatabase().query(
						SavedVideosTable.TABLE_NAME,
						new String[]{SavedVideosTable.COL_YOUTUBE_VIDEO, SavedVideosTable.COL_ORDER},
						null,
						null, null, null, SavedVideosTable.COL_ORDER + " DESC");
		List<YouTubeVideo> videos = new ArrayList<>();
		if(cursor.moveToNext()) {
			do {
				byte[] blob = cursor.getBlob(cursor.getColumnIndex(SavedVideosTable.COL_YOUTUBE_VIDEO));
				YouTubeVideo video = new Gson().fromJson(new String(blob), new TypeToken<YouTubeVideo>(){}.getType());
				videos.add(video);
			} while(cursor.moveToNext());
		}
		return videos;
	}

	/**
	 * Add a Listener that will be notified when a Video is added or removed from Saved Videos. This will
	 * allow the Video Grid to be redrawn in order to remove the video from display.
	 *
	 * @param listener The Listener (which implements SavedVideosDbListener) to add.
	 */
	public void addListener(SavedVideosDbListener listener) {
		if(!listeners.contains(listener))
			listeners.add(listener);
	}

	private void onUpdated() {
		hasUpdated = true;
		for(SavedVideosDbListener listener : listeners) {
			listener.onUpdated();
		}
	}

	public static boolean isHasUpdated() {
		return hasUpdated;
	}

	public static void setHasUpdated(boolean hasUpdated) {
		SavedVideosDb.hasUpdated = hasUpdated;
	}
}
