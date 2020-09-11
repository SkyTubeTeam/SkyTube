package free.rm.skytube.businessobjects.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.AsyncTaskParallel;
import free.rm.skytube.businessobjects.Logger;
import free.rm.skytube.businessobjects.YouTube.POJOs.CardData;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;
import free.rm.skytube.businessobjects.interfaces.OrderableDatabase;

/**
 * A database (DB) that stores user's downloaded videos.
 */
public class DownloadedVideosDb extends SQLiteOpenHelperEx implements OrderableDatabase {

	public static class Status {
		final Uri uri;
		final boolean disapeared;
		public Status(Uri uri, boolean disapeared) {
			this.uri = uri;
			this.disapeared = disapeared;
		}

		public Uri getUri() {
			return uri;
		}

		public boolean isDisapeared() {
			return disapeared;
		}
	}

	public interface DownloadedVideosListener {
		void onDownloadedVideosUpdated();
	}

	private static volatile DownloadedVideosDb downloadsDb = null;
	private static boolean hasUpdated = false;

	private static final int DATABASE_VERSION = 1;
	private static final String DATABASE_NAME = "videodownloads.db";

	private final Set<DownloadedVideosListener> listeners = new HashSet<>();

	public static synchronized DownloadedVideosDb getVideoDownloadsDb() {
		if (downloadsDb == null) {
			downloadsDb = new DownloadedVideosDb(SkyTubeApp.getContext());
		}

		return downloadsDb;
	}

	private DownloadedVideosDb(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	protected void clearDatabaseInstance() {

	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(DownloadedVideosTable.getCreateStatement());
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	}

	/**
	 * Get the list of Videos that have been downloaded.
	 *
	 * @return List of Videos
	 */
	public List<YouTubeVideo> getDownloadedVideos() {
		return getDownloadedVideos(DownloadedVideosTable.COL_ORDER + " DESC");
	}

	/**
	 * Get the list of Videos that have been downloaded in the given order.
	 *
	 * @return List of Videos
	 */
	private List<YouTubeVideo> getDownloadedVideos(String ordering) {
		Cursor	cursor = getReadableDatabase().query(
						DownloadedVideosTable.TABLE_NAME,
						new String[]{DownloadedVideosTable.COL_YOUTUBE_VIDEO, DownloadedVideosTable.COL_FILE_URI},
						null,
						null, null, null, ordering);
		List<YouTubeVideo> videos = new ArrayList<>();

		Gson gson = new Gson();
		if(cursor.moveToNext()) {
			do {
				final byte[] blob = cursor.getBlob(cursor.getColumnIndex(DownloadedVideosTable.COL_YOUTUBE_VIDEO));
				final String videoJson = new String(blob);

				// convert JSON into YouTubeVideo
				YouTubeVideo video = gson.fromJson(videoJson, YouTubeVideo.class).updatePublishTimestampFromDate();

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
				video.forceRefreshPublishDatePretty();
				videos.add(video);
			} while(cursor.moveToNext());
		}
		cursor.close();

		return videos;
	}

	public boolean add(YouTubeVideo video, String fileUri) {
		Gson gson = new Gson();
		ContentValues values = new ContentValues();
		values.put(DownloadedVideosTable.COL_YOUTUBE_VIDEO_ID, video.getId());
		values.put(DownloadedVideosTable.COL_YOUTUBE_VIDEO, gson.toJson(video).getBytes());
		values.put(DownloadedVideosTable.COL_FILE_URI, fileUri);

		int order = getMaximumOrderNumber();
		order++;
		values.put(DownloadedVideosTable.COL_ORDER, order);

		boolean addSuccessful = getWritableDatabase().replace(DownloadedVideosTable.TABLE_NAME, null, values) != -1;
		onUpdated();
		return addSuccessful;
	}

	public boolean remove(String videoId) {
		int rowsDeleted = getWritableDatabase().delete(DownloadedVideosTable.TABLE_NAME,
						DownloadedVideosTable.COL_YOUTUBE_VIDEO_ID + " = ?",
						new String[]{videoId});

		onUpdated();
		return (rowsDeleted >= 0);
	}

	public boolean remove(YouTubeVideo video) {
		return remove(video.getId());
	}

	public boolean isVideoDownloaded(YouTubeVideo video) {
		Cursor cursor = getReadableDatabase().query(
						DownloadedVideosTable.TABLE_NAME,
						new String[]{DownloadedVideosTable.COL_FILE_URI},
						DownloadedVideosTable.COL_YOUTUBE_VIDEO_ID + " = ?",
						new String[]{video.getId()}, null, null, null);

		boolean isDownloaded = false;
		if (cursor.moveToNext()) {
			String uri = cursor.getString(cursor.getColumnIndex(DownloadedVideosTable.COL_FILE_URI));
			isDownloaded = uri != null;
		}
		cursor.close();
		return isDownloaded;
	}

	public Uri getVideoFileUri(YouTubeVideo video) {
		return getVideoFileUri(video.getId());
	}

	public Uri getVideoFileUri(String videoId) {
		try (Cursor cursor = getReadableDatabase().query(
				DownloadedVideosTable.TABLE_NAME,
				new String[]{DownloadedVideosTable.COL_FILE_URI},
				DownloadedVideosTable.COL_YOUTUBE_VIDEO_ID + " = ?",
				new String[]{videoId}, null, null, null)) {

			if (cursor.moveToNext()) {
				String uri = cursor.getString(cursor.getColumnIndex(DownloadedVideosTable.COL_FILE_URI));
				return Uri.parse(uri);
			}
			return null;
		}
	}

	/**
	 * Return a locally saved file URI for the given video, the call ensures, that the file exists currently
	 * @param videoId the id of the video
	 * @return file URI
	 */
	public Status getVideoFileUriAndValidate(String videoId) {
		Uri uri = getVideoFileUri(videoId);
		if (uri != null) {
			File file = new File(uri.getPath());
			if (!file.exists()) {
				remove(videoId);
				return new Status(null, true);
			}
			return new Status(uri, false);
		}
		return new Status(null, false);
	}

	private synchronized void onUpdated() {
		hasUpdated = true;
		if(listeners != null) {
			for (DownloadedVideosListener listener: listeners) {
				listener.onDownloadedVideosUpdated();
			}
		}
	}

	/**
	 * Add a Listener that will be notified when a video is added or removed from the Downloaded Videos. This will
	 * allow the Video Grid to be redrawn in order to remove the video from display.
	 *
	 * @param listener The Listener (which implements DownloadedVideosListener) to add.
	 */
	public synchronized void addListener(@NonNull DownloadedVideosListener listener) {
		this.listeners.add(listener);
	}

	/**
	 * Remove the Listener
	 *
	 * @param listener The Listener (which implements BookmarksDbListener) to remove.
	 */
	public synchronized void removeListener(@NonNull DownloadedVideosListener listener) {
		this.listeners.remove(listener);
	}

	/**
	 * When a Video in the Downloads tab is drag & dropped to a new position, this will be
	 * called with the new updated list of videos. Since the videos are displayed in descending order,
	 * the first video in the list will have the highest number.
	 *
	 * @param videos List of Videos to update their order.
	 */
	@Override
	public void updateOrder(List<CardData> videos) {
		int order = videos.size();

		for(CardData video : videos) {
			ContentValues cv = new ContentValues();
			cv.put(DownloadedVideosTable.COL_ORDER, order--);
			getWritableDatabase().update(DownloadedVideosTable.TABLE_NAME, cv, DownloadedVideosTable.COL_YOUTUBE_VIDEO_ID + " = ?", new String[]{video.getId()});
		}
	}

	/**
	 * @return The maximum of the order number - which could be different from the number of downloaded files, in case some of them are deleted.
	 */
	public int getMaximumOrderNumber() {
		Cursor	cursor = getReadableDatabase().rawQuery(DownloadedVideosTable.MAXIMUM_ORDER_QUERY, null);
		int		totalDownloads = 0;

		if (cursor.moveToFirst()) {
			totalDownloads = cursor.getInt(0);
		}

		cursor.close();
		return totalDownloads;
	}

	public static boolean isHasUpdated() {
		return hasUpdated;
	}

	public static void setHasUpdated(boolean hasUpdated) {
		DownloadedVideosDb.hasUpdated = hasUpdated;
	}

	/**
	 * AsyncTask to remove any videos from the Database whose local files have gone missing.
	 */
	public static class RemoveMissingVideosTask extends AsyncTaskParallel<Void, Void, Void> {
		@Override
		protected Void doInBackground(Void... voids) {
			Cursor	cursor = getVideoDownloadsDb().getReadableDatabase().query(
							DownloadedVideosTable.TABLE_NAME,
							new String[]{DownloadedVideosTable.COL_YOUTUBE_VIDEO_ID, DownloadedVideosTable.COL_FILE_URI},
							null,
							null, null, null, null);

			if(cursor.moveToNext()) {
				do {
					String videoId = cursor.getString(cursor.getColumnIndex(DownloadedVideosTable.COL_YOUTUBE_VIDEO_ID));
					Uri uri = Uri.parse(cursor.getString(cursor.getColumnIndex(DownloadedVideosTable.COL_FILE_URI)));
					File file = new File(uri.getPath());
					if(!file.exists()) {
						getVideoDownloadsDb().remove(videoId);
					}
				} while(cursor.moveToNext());
			}
			cursor.close();
			return null;
		}
	}

}
