package free.rm.skytube.businessobjects.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.AsyncTaskParallel;
import free.rm.skytube.businessobjects.Logger;
import free.rm.skytube.businessobjects.YouTube.POJOs.CardData;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;
import free.rm.skytube.businessobjects.YouTube.newpipe.VideoId;
import free.rm.skytube.businessobjects.interfaces.OrderableDatabase;

/**
 * A database (DB) that stores user's downloaded videos.
 */
public class DownloadedVideosDb extends SQLiteOpenHelperEx implements OrderableDatabase {
	private final static String TAG = "DownloadedVideosDb";

	public static class Status {
		final Uri uri;
		final Uri audioUri;
		final boolean disapeared;
		public Status(Uri uri, Uri audioUri, boolean disapeared) {
			this.uri = uri;
			this.audioUri = audioUri;
			this.disapeared = disapeared;
		}

		public Uri getUri() {
			return uri;
		}

		public File getLocalVideoFile() {
			if (uri != null) {
				return new File(uri.getPath());
			}
			return null;
		}

		public File getLocalAudioFile() {
			if (audioUri != null) {
				return new File(audioUri.getPath());
			}
			return null;
		}

		public File getParentFolder() {
			File localFile = getLocalVideoFile();
			if (localFile == null) {
				localFile = getLocalAudioFile();
			}
			return localFile != null ? localFile.getParentFile() : null;
		}

		public Uri getAudioUri() {
			return audioUri;
		}

		public boolean isDisapeared() {
			return disapeared;
		}

		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder("Status{");
			if (uri != null) {
				sb.append("uri=").append(uri);
			}
			if (audioUri != null) {
				sb.append(", audioUri=").append(audioUri);
			}
			sb.append(", disapeared=").append(disapeared);
			sb.append('}');
			return sb.toString();
		}
	}

	public interface DownloadedVideosListener {
		void onDownloadedVideosUpdated();
	}

	private static volatile DownloadedVideosDb downloadsDb = null;
	private static boolean hasUpdated = false;

	private static final int DATABASE_VERSION = 2;
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
		if(oldVersion == 1 && newVersion >= 2) {
			db.execSQL(DownloadedVideosTable.getAddAudioUriColumn());
		}

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

	public boolean add(YouTubeVideo video, Uri fileUri, Uri audioUri) {
		Gson gson = new Gson();
		ContentValues values = new ContentValues();
		values.put(DownloadedVideosTable.COL_YOUTUBE_VIDEO_ID, video.getId());
		values.put(DownloadedVideosTable.COL_YOUTUBE_VIDEO, gson.toJson(video).getBytes());
		if (fileUri != null) {
			values.put(DownloadedVideosTable.COL_FILE_URI, fileUri.toString());
		}
		if (audioUri != null) {
			values.put(DownloadedVideosTable.COL_AUDIO_FILE_URI, audioUri.toString());
		}

		int order = getMaximumOrderNumber();
		order++;
		values.put(DownloadedVideosTable.COL_ORDER, order);

		boolean addSuccessful = getWritableDatabase().replace(DownloadedVideosTable.TABLE_NAME, null, values) != -1;
		onUpdated();
		return addSuccessful;
	}

	/**
	 * Remove the filenames of the downloaded video from the database
	 * @param videoId
	 * @return
	 */
	private boolean remove(String videoId) {
		int rowsDeleted = getWritableDatabase().delete(DownloadedVideosTable.TABLE_NAME,
						DownloadedVideosTable.COL_YOUTUBE_VIDEO_ID + " = ?",
						new String[]{videoId});

		onUpdated();
		return (rowsDeleted >= 0);
	}

	public boolean remove(YouTubeVideo video) {
		return remove(video.getId());
	}

	/**
	 * Remove local copy of this video, and delete it from the VideoDownloads DB.
	 */
	public void removeDownload(VideoId videoId) {
		Status status = getVideoFileStatus(videoId);
		Log.i(TAG, "removeDownload for " + videoId + " -> " + status);
		if (status != null) {
			deleteIfExists(status.getLocalAudioFile());
			deleteIfExists(status.getLocalVideoFile());
			remove(videoId.getId());
			if (SkyTubeApp.getSettings().isDownloadToSeparateFolders()) {
				removeParentFolderIfEmpty(status);
			}
		}
	}

	private void removeParentFolderIfEmpty(Status file) {
		File parentFile = file.getParentFolder();
		Log.i(TAG, "removeParentFolderIfEmpty " + parentFile.getAbsolutePath() + " " + parentFile.exists() + " " + parentFile.isDirectory());
		if (parentFile.exists() && parentFile.isDirectory()) {
			String[] fileList = parentFile.list();
			Log.i(TAG, "file list is " + Arrays.asList(fileList));
			if (fileList != null) {
				if (fileList.length == 0) {
					// that was the last file in the directory, remove it
					Log.i(TAG, "now delete it:" + parentFile);
					parentFile.delete();
				}
			}
		}
		Log.i(TAG, "exit removeParentFolderIfEmpty");
	}

	public boolean isVideoDownloaded(VideoId video) {
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


	private Status getVideoFileStatus(VideoId videoId) {
		try (Cursor cursor = getReadableDatabase().query(
				DownloadedVideosTable.TABLE_NAME,
				new String[]{DownloadedVideosTable.COL_FILE_URI, DownloadedVideosTable.COL_AUDIO_FILE_URI},
				DownloadedVideosTable.COL_YOUTUBE_VIDEO_ID + " = ?",
				new String[]{videoId.getId()}, null, null, null)) {

			if (cursor.moveToNext()) {
				return new Status(
						getUri(cursor, cursor.getColumnIndex(DownloadedVideosTable.COL_FILE_URI)),
						getUri(cursor, cursor.getColumnIndex(DownloadedVideosTable.COL_AUDIO_FILE_URI)),
						false);
			}
			return null;
		}
	}

	private Uri getUri(Cursor cursor, int columnIndex) {
		String uri = cursor.getString(columnIndex);
		if (uri != null) {
			return Uri.parse(uri);
		} else {
			return null;
		}
	}
	/**
	 * Return a locally saved file URI for the given video, the call ensures, that the file exists currently
	 * @param videoId the id of the video
	 * @return the status, never null
	 */
	public Status getVideoFileUriAndValidate(VideoId videoId) {
		Status downloadStatus = getVideoFileStatus(videoId);
		if (downloadStatus != null) {
			File localVideo = downloadStatus.getLocalVideoFile();
			if (localVideo != null) {
				if (!localVideo.exists()) {
					deleteIfExists(downloadStatus.getLocalAudioFile());
					remove(videoId.getId());
					return new Status(null, null, true);
				}
			}
			File localAudio = downloadStatus.getLocalAudioFile();
			if (localAudio != null) {
				if (!localAudio.exists()) {
					deleteIfExists(downloadStatus.getLocalVideoFile());
					remove(videoId.getId());
					return new Status(null, null, true);
				}
			}
			return downloadStatus;
		}
		return new Status(null,null, false);
	}

	private void deleteIfExists(File file) {
		if (file != null && file.exists()) {
			Log.i(TAG, "File exists " + file.getAbsolutePath());
			file.delete();
		}
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
