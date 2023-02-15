package free.rm.skytube.businessobjects.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import free.rm.skytube.R;
import free.rm.skytube.app.Settings;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.AsyncTaskParallel;
import free.rm.skytube.businessobjects.Logger;
import free.rm.skytube.businessobjects.Sponsorblock.SBTasks;
import free.rm.skytube.businessobjects.Sponsorblock.SBVideoInfo;
import free.rm.skytube.businessobjects.YouTube.POJOs.CardData;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;
import free.rm.skytube.businessobjects.YouTube.newpipe.VideoId;
import free.rm.skytube.businessobjects.interfaces.OrderableDatabase;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * A database (DB) that stores user's downloaded videos.
 */
public class DownloadedVideosDb extends CardEventEmitterDatabase implements OrderableDatabase {
    private final static String TAG = "DownloadedVideosDb";

    public static class Status {
        final Uri uri;
        final Uri audioUri;
        final boolean disappeared;
        final VideoId videoId;

        public Status(VideoId videoId,Uri uri, Uri audioUri, boolean disappeared) {
            this.uri = uri;
            this.audioUri = audioUri;
            this.disappeared = disappeared;
            this.videoId = videoId;
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

        public boolean isDisappeared() {
            return disappeared;
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
            sb.append(", disapeared=").append(disappeared);
            sb.append('}');
            return sb.toString();
        }

        public VideoId getVideoId() {
            return videoId;
        }
    }

    public static class FileDeletionFailed extends Exception {
        String path;

        FileDeletionFailed(String path) {
            super("File deletion failed for " + path);
            this.path = path;
        }

        public String getPath() {
            return path;
        }
    }

    private static volatile DownloadedVideosDb downloadsDb = null;
    private static boolean hasUpdated = false;

    private static final int DATABASE_VERSION = 3;
    private static final String DATABASE_NAME = "videodownloads.db";

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
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DownloadedVideosTable.getCreateStatement());
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion == 1 && newVersion >= 2) {
            db.execSQL(DownloadedVideosTable.getAddAudioUriColumn());
        }
        if (oldVersion == 2 && newVersion >= 3) {
            db.execSQL(DownloadedVideosTable.getAddSponsorBlockColumn());
        }
    }

    /**
     * Get the list of Videos that have been downloaded.
     *
     * @return List of Videos
     */
    public List<YouTubeVideo> getDownloadedVideos() {
        SkyTubeApp.nonUiThread();
        return getDownloadedVideos(DownloadedVideosTable.COL_ORDER + " DESC");
    }

    /**
     * Get the list Statuses of Videos that have been downloaded.
     *
     * @return List of Status
     */
    public List<Status> getDownloadedVideosStatuses() {
        try (Cursor cursor = getReadableDatabase().query(
                DownloadedVideosTable.TABLE_NAME,
                new String[]{DownloadedVideosTable.COL_YOUTUBE_VIDEO, DownloadedVideosTable.COL_FILE_URI, DownloadedVideosTable.COL_AUDIO_FILE_URI},
                null,
                null, null, null, null)) {
            List<Status> statuses = new ArrayList<>();

            while (cursor.moveToNext()) {
                String id = cursor.getString(cursor.getColumnIndex(DownloadedVideosTable.COL_YOUTUBE_VIDEO_ID));
                String url = String.format("https://youtu.be/%s", id);
                statuses.add(new Status(new VideoId(id, url),
                        getUri(cursor, cursor.getColumnIndex(DownloadedVideosTable.COL_FILE_URI)),
                        getUri(cursor, cursor.getColumnIndex(DownloadedVideosTable.COL_AUDIO_FILE_URI)),
                        false));
            }
            return statuses;
        }
    }

    public SBVideoInfo getDownloadedVideoSponsorblock(String videoId) {
        SkyTubeApp.nonUiThread();
        try (Cursor cursor = getReadableDatabase().query(
                DownloadedVideosTable.TABLE_NAME,
                new String[]{DownloadedVideosTable.COL_SB},
                DownloadedVideosTable.COL_YOUTUBE_VIDEO_ID + " = ?",
                new String[]{videoId}, null, null, null)) {

            SBVideoInfo result = null;
            if (cursor.moveToNext()) {
                Gson gson = new Gson();
                do {
                    final byte[] sbBlob = cursor.getBlob(cursor.getColumnIndex(DownloadedVideosTable.COL_SB));
                    final String sbJson = new String(sbBlob);
                    result = gson.fromJson(sbJson, SBVideoInfo.class);
                } while (cursor.moveToNext());
            }

            return result;
        }
    }

    /**
     * Get the list of Videos that have been downloaded in the given order.
     *
     * @return List of Videos
     */
    private List<YouTubeVideo> getDownloadedVideos(String ordering) {
        try (Cursor cursor = getReadableDatabase().query(
                DownloadedVideosTable.TABLE_NAME,
                new String[]{DownloadedVideosTable.COL_YOUTUBE_VIDEO, DownloadedVideosTable.COL_FILE_URI},
                null,
                null, null, null, ordering)) {
            List<YouTubeVideo> videos = new ArrayList<>();

            if (cursor.moveToNext()) {
                Gson gson = new Gson();
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
                            final String channelId = videoJsonObj.get("channelId").toString();
                            final String channelName = videoJsonObj.get("channelName").toString();
                            video.setChannel(new YouTubeChannel(channelId, channelName));
                        } catch (JSONException e) {
                            Logger.e(this, "Error occurred while extracting channel{Id,Name} from JSON", e);
                        }
                    }
                    video.forceRefreshPublishDatePretty();
                    videos.add(video);
                } while (cursor.moveToNext());
            }
            return videos;
        }
    }

    public Single<Boolean> add(YouTubeVideo video, Uri fileUri, Uri audioUri) {
        return Single.fromCallable(() -> {

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
                    if (SkyTubeApp.getSettings().isSponsorblockEnabled()) {
                        SBVideoInfo sbInfo = SBTasks.retrieveSponsorblockSegmentsBk(video.getVideoId());
                        values.put(DownloadedVideosTable.COL_SB, gson.toJson(sbInfo).getBytes());
                    }

                    int order = getMaximumOrderNumber();
                    order++;
                    values.put(DownloadedVideosTable.COL_ORDER, order);

                    return getWritableDatabase().replace(DownloadedVideosTable.TABLE_NAME, null, values) != -1;
                }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess(success -> {
                    if (success) {
                        notifyCardAdded(video);
                    }
                });
    }

    /**
     * Remove the filenames of the downloaded video from the database
     *
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

    /**
     * Remove local copy of this video, and delete it from the VideoDownloads DB.
     *
     * @return
     */
    public @NonNull
    Single<Status> removeDownload(Context ctx, VideoId videoId) {
        return Single.fromCallable(() -> {
                    SkyTubeApp.nonUiThread();
                    Status status = getVideoFileStatus(videoId);
                    Log.i(TAG, "removeDownload for " + videoId + " -> " + status);
                    if (status != null) {
                        deleteIfExists(status.getLocalAudioFile());
                        deleteIfExists(status.getLocalVideoFile());
                        remove(videoId.getId());
                        final Settings settings = SkyTubeApp.getSettings();
                        if (settings.isDownloadToSeparateFolders()) {
                            removeParentFolderIfEmpty(status, settings.getDownloadParentFolder());
                        }
                    }
                    return status;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess(status -> {
                    SkyTubeApp.uiThread();
                    notifyCardDeleted(videoId);
                })
                .doOnError(exception -> {
                    displayGenericError(ctx, exception);
                });
    }

    private void removeParentFolderIfEmpty(Status file, File downloadParentFolder) {
        File parentFile = file.getParentFolder();
        Log.i(TAG, "removeParentFolderIfEmpty " + parentFile.getAbsolutePath() + " " + parentFile.exists() + " " + parentFile.isDirectory());
        if (parentFile.exists() && parentFile.isDirectory()) {
            if (parentFile.getParentFile().getAbsolutePath().equals(downloadParentFolder.getAbsolutePath())) {
                String[] fileList = parentFile.list();
                Log.i(TAG, "file list is " + Arrays.asList(fileList) + " under " + parentFile.getAbsolutePath());
                if (fileList != null) {
                    if (fileList.length == 0) {
                        // that was the last file in the directory, remove it
                        Log.i(TAG, "now delete it:" + parentFile);
                        parentFile.delete();
                    }
                }
            } else {
                Log.w(TAG, "Download parent folder is " + downloadParentFolder.getAbsolutePath() +
                        " but the file is stored under " + parentFile.getParentFile().getAbsolutePath());
            }
        }
        Log.i(TAG, "exit removeParentFolderIfEmpty");
    }

    /**
     * Returns whether or not the video has been downloaded.
     *
     * @return True if the video was previously saved by the user.
     */
    public Single<Boolean> isVideoDownloaded(@NonNull YouTubeVideo video) {
        return isVideoDownloaded(video.getVideoId());
    }

    /**
     * Returns whether or not the video with the given ID has been downloaded.
     *
     * @return True if the video was previously saved by the user.
     */
    public Single<Boolean> isVideoDownloaded(VideoId video) {
        return Single.fromCallable(() -> {
                    SkyTubeApp.nonUiThread();
                    try (Cursor cursor = getReadableDatabase().query(
                            DownloadedVideosTable.TABLE_NAME,
                            new String[]{DownloadedVideosTable.COL_FILE_URI},
                            DownloadedVideosTable.COL_YOUTUBE_VIDEO_ID + " = ?",
                            new String[]{video.getId()}, null, null, null)) {

                        boolean isDownloaded = false;
                        if (cursor.moveToNext()) {
                            String uri = cursor.getString(cursor.getColumnIndex(DownloadedVideosTable.COL_FILE_URI));
                            isDownloaded = uri != null;
                        }
                        return isDownloaded;
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    private Status getVideoFileStatus(VideoId videoId) {
        try (Cursor cursor = getReadableDatabase().query(
                DownloadedVideosTable.TABLE_NAME,
                new String[]{DownloadedVideosTable.COL_FILE_URI, DownloadedVideosTable.COL_AUDIO_FILE_URI},
                DownloadedVideosTable.COL_YOUTUBE_VIDEO_ID + " = ?",
                new String[]{videoId.getId()}, null, null, null)) {

            if (cursor.moveToNext()) {
                return new Status(videoId,
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
     *
     * @param videoId the id of the video
     * @return the status, never null
     */
    private @NonNull
    Single<Status> getVideoFileUriAndValidate(@NonNull VideoId videoId) {
        return Single.fromCallable(() -> {
                    Status downloadStatus = getVideoFileStatus(videoId);
                    if (downloadStatus != null) {
                        File localVideo = downloadStatus.getLocalVideoFile();
                        if (localVideo != null) {
                            if (!localVideo.exists()) {
                                deleteIfExists(downloadStatus.getLocalAudioFile());
                                remove(videoId.getId());
                                return new Status(videoId, null, null, true);
                            }
                        }
                        File localAudio = downloadStatus.getLocalAudioFile();
                        if (localAudio != null) {
                            if (!localAudio.exists()) {
                                deleteIfExists(downloadStatus.getLocalVideoFile());
                                remove(videoId.getId());
                                return new Status(videoId, null, null, true);
                            }
                        }
                        return downloadStatus;
                    }
                    return new Status(videoId, null, null, false);
                }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * Get the Uri for the local copy of this Video, if the file is missing, or incorrect state, it tries to cleanup,
     * and notifies the user about that error
     *
     * @return Status object - never null
     */
    public @NonNull
    Single<Status> getDownloadedFileStatus(Context context, @NonNull VideoId videoId) {
        return getVideoFileUriAndValidate(videoId).onErrorReturn(error -> {
            displayGenericError(context, error);
            return new Status(videoId, null, null, true);
        });
    }

    private void displayGenericError(Context context, Throwable exception) {
        if (exception instanceof FileDeletionFailed) {
            displayError(context, (FileDeletionFailed) exception);
        } else {
            Log.e(TAG, "Exception : " + exception.getMessage(), exception);
        }
    }

    private void displayError(Context context, DownloadedVideosDb.FileDeletionFailed fileDeletionFailed) {
        Logger.e(this, "Unable to delete file : %s", fileDeletionFailed.getPath());
        Toast.makeText(context, context.getString(R.string.unable_to_delete_file, fileDeletionFailed.getPath()), Toast.LENGTH_LONG).show();
    }

    private void deleteIfExists(File file) throws FileDeletionFailed {
        if (file != null && file.exists()) {
            Log.i(TAG, "File exists " + file.getAbsolutePath());
            if (!file.delete()) {
                throw new FileDeletionFailed(file.getAbsolutePath());
            }
        }
    }

    private synchronized void onUpdated() {
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

        for (CardData video : videos) {
            ContentValues cv = new ContentValues();
            cv.put(DownloadedVideosTable.COL_ORDER, order--);
            getWritableDatabase().update(DownloadedVideosTable.TABLE_NAME, cv, DownloadedVideosTable.COL_YOUTUBE_VIDEO_ID + " = ?", new String[]{video.getId()});
        }
    }

    /**
     * @return The total number of bookmarked videos.
     */
    public Single<Integer> getTotalCount() {
        return Single.fromCallable(() ->
                executeQueryForInteger(DownloadedVideosTable.COUNT_ALL, 0)
        ).subscribeOn(Schedulers.io());
    }

    /**
     * @return The maximum of the order number - which could be different from the number of downloaded files, in case some of them are deleted.
     */
    private int getMaximumOrderNumber() {
        try (Cursor cursor = getReadableDatabase().rawQuery(DownloadedVideosTable.MAXIMUM_ORDER_QUERY, null)) {
            int totalDownloads = 0;

            if (cursor.moveToFirst()) {
                totalDownloads = cursor.getInt(0);
            }

            return totalDownloads;
        }
    }

    /**
     * AsyncTask to remove any videos from the Database whose local files have gone missing.
     */
    public static class RemoveMissingVideosTask extends AsyncTaskParallel<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            try (Cursor cursor = getVideoDownloadsDb().getReadableDatabase().query(
                    DownloadedVideosTable.TABLE_NAME,
                    new String[]{DownloadedVideosTable.COL_YOUTUBE_VIDEO_ID, DownloadedVideosTable.COL_FILE_URI},
                    null,
                    null, null, null, null)) {

                if (cursor.moveToNext()) {
                    do {
                        String videoId = cursor.getString(cursor.getColumnIndex(DownloadedVideosTable.COL_YOUTUBE_VIDEO_ID));
                        Uri uri = Uri.parse(cursor.getString(cursor.getColumnIndex(DownloadedVideosTable.COL_FILE_URI)));
                        File file = new File(uri.getPath());
                        if (!file.exists()) {
                            getVideoDownloadsDb().remove(videoId);
                        }
                    } while (cursor.moveToNext());
                }
                return null;
            }
        }
    }

}
