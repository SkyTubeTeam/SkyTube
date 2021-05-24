package free.rm.skytube.businessobjects.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.Logger;
import free.rm.skytube.businessobjects.YouTube.POJOs.CardData;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;
import free.rm.skytube.businessobjects.interfaces.VideoPlayStatusUpdateListener;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * A database (DB) that stores video playback history
 */
public class PlaybackStatusDb extends SQLiteOpenHelperEx {
	private static volatile PlaybackStatusDb playbackStatusDb = null;
	private static HashMap<String, VideoWatchedStatus> playbackHistoryMap = null;

	private static final int DATABASE_VERSION = 1;
	private int updateCounter = 0;
	private static final String DATABASE_NAME = "playbackhistory.db";

	private final Set<VideoPlayStatusUpdateListener> listeners = new HashSet<>();

	public static synchronized PlaybackStatusDb getPlaybackStatusDb() {
		if (playbackStatusDb == null) {
			playbackStatusDb = new PlaybackStatusDb(SkyTubeApp.getContext());
		}

		return playbackStatusDb;
	}

	private PlaybackStatusDb(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	public void deleteAllPlaybackHistory() {
		getWritableDatabase().delete(PlaybackStatusTable.TABLE_NAME, null, null);
		playbackHistoryMap = null;
		updateCounter++;
		onUpdated(null);
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
	 * @param videoId {@link YouTubeVideo}
	 * @return {@link VideoWatchedStatus} of the passed video, which contains the position (in ms) and whether or not the video
	 * 					has been (completely) watched.
	 */
	public synchronized VideoWatchedStatus getVideoWatchedStatus(@NonNull String videoId) {
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
		if(playbackHistoryMap.get(videoId) == null) {
			// Requested video has no entry in the database, so create one in the Map. No need to create it in the Database yet - if needed,
			// that will happen when video position is set
			VideoWatchedStatus status = new VideoWatchedStatus();
			playbackHistoryMap.put(videoId, status);
		}
		return playbackHistoryMap.get(videoId);
	}

    public Maybe<VideoWatchedStatus> getVideoWatchedStatusAsync(@NonNull String videoId) {
        return Maybe.fromCallable(() -> getVideoWatchedStatus(videoId))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

	/**
	 * Set the position (in ms) of the passed {@link YouTubeVideo}. If the position is less than 5 seconds,
	 * don't do anything. If the position is greater than or equal to 90% of the duration of the video, set
	 * the position to 0 and mark the video as watched.
	 *
	 * @param video {@link YouTubeVideo}
	 * @param position Number of milliseconds
	 * @return Disposable which contains the background task.
	 */
	public Disposable setVideoPositionInBackground(YouTubeVideo video, long position) {
		// Don't record the position if it's < 5 seconds
		if (SkyTubeApp.getSettings().isPlaybackStatusEnabled() && position >= 5000) {
			boolean watched = false;
			// If the user has stopped watching the video and the position is greater than 90% of the duration, mark the video as watched and reset position
			if((float)position / (video.getDurationInSeconds()*1000) >= 0.9) {
				watched = true;
				position = 0;
			}
			final long positionValue = position;
			final boolean watchedValue = watched;
			return Single.fromCallable(() -> saveVideoWatchStatus(video.getId(), positionValue, watchedValue))
					.subscribeOn(Schedulers.io())
					.observeOn(AndroidSchedulers.mainThread())
					.subscribe((success) -> {
						Logger.i(this, "onUpdated " + success);

						if (success) {
							onUpdated(video);
						}
					});
		} else {
			return Disposable.empty();
		}

	}

	/**
	 * Set the watched status of the passed {@link YouTubeVideo}. Regardless of watched status, set the
	 * position to 0.
	 *
	 * @param video {@link YouTubeVideo}
	 * @param watched boolean on whether or not the passed video has been watched
	 * @return boolean on whether the database was updated successfully.
	 */
	public Maybe<Boolean> setVideoWatchedStatusInBackground(YouTubeVideo video, boolean watched) {
		if (SkyTubeApp.getSettings().isPlaybackStatusEnabled()) {
			return Maybe.fromCallable(() -> saveVideoWatchStatus(video.getId(), 0, watched))
					.subscribeOn(Schedulers.io())
					.observeOn(AndroidSchedulers.mainThread())
					.doOnSuccess((success) -> {
						Logger.i(this, "onUpdated " + success);

                        if (success) {
                            onUpdated(video);
                        }
					});
		} else {
			return Maybe.empty();
		}
	}

	private boolean saveVideoWatchStatus(String videoId, long position, boolean watched) {
		ContentValues values = new ContentValues();
		values.put(PlaybackStatusTable.COL_YOUTUBE_VIDEO_ID, videoId);
		values.put(PlaybackStatusTable.COL_YOUTUBE_VIDEO_POSITION, (int)position);
		values.put(PlaybackStatusTable.COL_YOUTUBE_VIDEO_WATCHED, watched ? 1 : 0);

		boolean addSuccessful = getWritableDatabase().insertWithOnConflict(PlaybackStatusTable.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE) != -1;
		if(addSuccessful) {
			updateCounter++;
		}

		VideoWatchedStatus status = playbackHistoryMap.get(videoId);
		if (status == null) {
			status = new VideoWatchedStatus();
			playbackHistoryMap.put(videoId, status);
		}
		status.position = position;
		status.watched = watched;


		return addSuccessful;
	}

    private void onUpdated(CardData cardData) {
        for(VideoPlayStatusUpdateListener listener : listeners) {
            listener.onVideoStatusUpdated(cardData);
        }
    }

	/**
	 * Class that contains the position and watched status of a video.
	 */
	public static class VideoWatchedStatus {
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
	 * Return the number of updates happened to the playback status
	 * If it different than the VideoGrid has, it needs to be refreshed.
	 *
	 * @return int updateCounter
	 */
	public int getUpdateCounter() {
		return updateCounter;
	}

	public void addListener(VideoPlayStatusUpdateListener listener) {
		listeners.add(listener);
	}

	public void removeListener(VideoPlayStatusUpdateListener listener) {
		listeners.remove(listener);
	}
}
