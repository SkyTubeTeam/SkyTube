package free.rm.skytube.businessobjects.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;
import free.rm.skytube.businessobjects.interfaces.VideoPlayStatusUpdateListener;

/**
 * A database (DB) that stores video playback history
 */
public class PlaybackStatusDb extends SQLiteOpenHelperEx {
	private static volatile PlaybackStatusDb playbackStatusDb = null;
	private static HashMap<String, VideoWatchedStatus> playbackHistoryMap = null;

	private static final int DATABASE_VERSION = 1;
	private static boolean hasUpdated = false;
	private static final String DATABASE_NAME = "playbackhistory.db";

	private List<VideoPlayStatusUpdateListener> listeners = new ArrayList<>();

	public static synchronized PlaybackStatusDb getVideoDownloadsDb() {
		if (playbackStatusDb == null) {
			playbackStatusDb = new PlaybackStatusDb(SkyTubeApp.getContext());
		}

		return playbackStatusDb;
	}

	private PlaybackStatusDb(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	protected void clearDatabaseInstance() {

	}

	public void deleteAllPlaybackHistory() {
		getWritableDatabase().delete(PlaybackStatusTable.TABLE_NAME, null, null);
		playbackHistoryMap = null;
		hasUpdated = true;
		onUpdated();
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(PlaybackStatusTable.getCreateStatement());
	}

	@Override
	public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

	}

	/**
	 * Get the watched status of the passed {@link YouTubeVideo}. Instead of always querying the database, a HashMap
	 * is constructed that stores the watch status of all videos (that have a status). Subsequent calls to this method
	 * will return the watch status for the passed video from this HashMap (which also gets updated by calls to setWatchedStatus().
	 *
	 * @param video {@link YouTubeVideo}
	 * @return {@link VideoWatchedStatus} of the passed video, which contains the position (in ms) and whether or not the video
	 * 					has been (completely) watched.
	 */
	public VideoWatchedStatus getVideoWatchedStatus(YouTubeVideo video) {
		if(playbackHistoryMap == null) {
			Cursor cursor = getReadableDatabase().query(
							PlaybackStatusTable.TABLE_NAME,
							new String[]{PlaybackStatusTable.COL_YOUTUBE_VIDEO_ID, PlaybackStatusTable.COL_YOUTUBE_VIDEO_POSITION, PlaybackStatusTable.COL_YOUTUBE_VIDEO_WATCHED},
							null,
							null, null, null, null);
			playbackHistoryMap = new HashMap<>();
			if(cursor.moveToFirst()) {
				do {
					String video_id = cursor.getString(cursor.getColumnIndex(PlaybackStatusTable.COL_YOUTUBE_VIDEO_ID));
					int position = cursor.getInt(cursor.getColumnIndex(PlaybackStatusTable.COL_YOUTUBE_VIDEO_POSITION));
					int finished = cursor.getInt(cursor.getColumnIndex(PlaybackStatusTable.COL_YOUTUBE_VIDEO_WATCHED));
					VideoWatchedStatus status = new VideoWatchedStatus(position, finished == 1);
					playbackHistoryMap.put(video_id, status);
				} while (cursor.moveToNext());
			}
			cursor.close();
		}
		if(playbackHistoryMap.get(video.getId()) == null) {
			// Requested video has no entry in the database, so create one in the Map. No need to create it in the Database yet - if needed,
			// that will happen when video position is set
			VideoWatchedStatus status = new VideoWatchedStatus();
			playbackHistoryMap.put(video.getId(), status);
		}
		return playbackHistoryMap.get(video.getId());
	}

	/**
	 * Set the position (in ms) of the passed {@link YouTubeVideo}. If the position is less than 5 seconds,
	 * don't do anything. If the position is greater than or equal to 90% of the duration of the video, set
	 * the position to 0 and mark the video as watched.
	 *
	 * @param video {@link YouTubeVideo}
	 * @param position Number of milliseconds
	 * @return boolean on whether the database was updated successfully.
	 */
	public boolean setVideoPosition(YouTubeVideo video, long position) {
		// Don't record the position if it's < 5 seconds
		if(position < 5000)
			return false;

		int watched = 0;
		// If the user has stopped watching the video and the position is greater than 90% of the duration, mark the video as watched and reset position
		if((float)position / (video.getDurationInSeconds()*1000) >= 0.9) {
			watched = 1;
			position = 0;
		}

		ContentValues values = new ContentValues();
		values.put(PlaybackStatusTable.COL_YOUTUBE_VIDEO_ID, video.getId());
		values.put(PlaybackStatusTable.COL_YOUTUBE_VIDEO_POSITION, (int)position);
		values.put(PlaybackStatusTable.COL_YOUTUBE_VIDEO_WATCHED, watched);

		playbackHistoryMap.get(video.getId()).position = position;
		playbackHistoryMap.get(video.getId()).watched = watched == 1;

		boolean addSuccessful = getWritableDatabase().insertWithOnConflict(PlaybackStatusTable.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE) != -1;
		if(addSuccessful)
				hasUpdated = true;

		onUpdated();

		return addSuccessful;
	}

	/**
	 * Set the watched status of the passed {@link YouTubeVideo}. Regardless of watched status, set the
	 * position to 0.
	 *
	 * @param video {@link YouTubeVideo}
	 * @param watched boolean on whether or not the passed video has been watched
	 * @return boolean on whether the database was updated successfully.
	 */
	public boolean setVideoWatchedStatus(YouTubeVideo video, boolean watched) {
		ContentValues values = new ContentValues();
		values.put(PlaybackStatusTable.COL_YOUTUBE_VIDEO_ID, video.getId());
		values.put(PlaybackStatusTable.COL_YOUTUBE_VIDEO_POSITION, 0);
		values.put(PlaybackStatusTable.COL_YOUTUBE_VIDEO_WATCHED, watched ? 1 : 0);

		playbackHistoryMap.get(video.getId()).watched = watched;
		playbackHistoryMap.get(video.getId()).position = 0;

		boolean success = getWritableDatabase().insertWithOnConflict(PlaybackStatusTable.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE) != -1;
		if(success)
			hasUpdated = true;

		onUpdated();

		return success;
	}

	private void onUpdated() {
		for(VideoPlayStatusUpdateListener listener : listeners) {
			listener.onVideoStatusUpdated();
		}
	}

	/**
	 * Class that contains the position and watched status of a video.
	 */
	public class VideoWatchedStatus {
		public VideoWatchedStatus() {}
		public VideoWatchedStatus(long position, boolean watched) {
			this.position = position;
			this.watched = watched;
		}

		@Override
		public String toString() {
			return String.format("Position: %d\nWatched: %s\n", position, watched);
		}

		private long position = 0;
		private boolean watched = false;

		public boolean isFullyWatched() {
			return watched;
		}

		public boolean isWatched() {
			return position > 0 || watched;
		}

		public long getPosition() {
			return position;
		}
	}

	/**
	 * Whether or not the database has been updated. If it has, the VideoGrid will refresh.
	 *
	 * @return boolean
	 */
	public static boolean isHasUpdated() {
		return hasUpdated;
	}

	public static void setHasUpdated(boolean hasUpdated) {
		PlaybackStatusDb.hasUpdated = hasUpdated;
	}

	public void addListener(VideoPlayStatusUpdateListener listener) {
		if(listeners.indexOf(listener) == -1) {
			listeners.add(listener);
		}
	}

	public void removeListener(VideoPlayStatusUpdateListener listener) {
		listeners.remove(listener);
	}
}
