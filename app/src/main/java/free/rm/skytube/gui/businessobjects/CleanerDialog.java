package free.rm.skytube.gui.businessobjects;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.Logger;
import free.rm.skytube.businessobjects.db.BookmarksDb;
import free.rm.skytube.businessobjects.db.DownloadedVideosDb;
import free.rm.skytube.businessobjects.db.PlaybackStatusDb;
import free.rm.skytube.databinding.DialogCleanDownloadsBinding;
import free.rm.skytube.gui.activities.MainActivity;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * Dialog for tool, that helps clean your storage from possibly now-unwanted videos
 */
public class CleanerDialog extends SkyTubeMaterialDialog {

    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    public CleanerDialog(@NonNull MainActivity context) {
        super(context);

        final DialogCleanDownloadsBinding binding = DialogCleanDownloadsBinding.inflate(LayoutInflater.from(context));
        title(R.string.cleaner);
        // Set Layout for Dialog
        customView(binding.getRoot(),true);

        positiveText(R.string.cleaner_confirm);
        onPositive((dialog, action) -> {
            boolean cleanWatchedDownloads = binding.cleanWatchedDownloads.isChecked();
            boolean cleanWatchedBookmarks = binding.cleanWatchedBookmarks.isChecked();
            List<Single<Pair<Integer,Long>>> tasks = new ArrayList<>();
            if (cleanWatchedDownloads) {
                tasks.add(cleanWatchedDownloads(context));
            }
            if (cleanWatchedBookmarks) {
                tasks.add(cleanWatchedBookmarks(context));
            }
            // MainFragment#refreshTabs
            compositeDisposable.add(Single.merge(tasks)
                            .subscribeOn(Schedulers.io())
                    .reduce(Pair.create(0, 0L), (a, b) -> Pair.create(a.first + b.first, a.second + b.second))
                    .doOnSuccess(pair -> {
                        final double usedMB = ((double) pair.second) / 1024 / 1024;
                        final String formatted = new DecimalFormat("#.###").format(usedMB);
                        Toast.makeText(context, String.format(
                                SkyTubeApp.getStr(R.string.cleaner_toast_finish), pair.first, formatted), Toast.LENGTH_LONG).show();
                    })
                    .doOnError(error -> SkyTubeApp.notifyUserOnError(context, error))
                    .subscribe());
        });

        cancelListener = dialog -> clearBackgroundTasks();
    }

    public void clearBackgroundTasks() { compositeDisposable.clear(); }

    /**
     * Cleans watched Videos, that are downloaded by the user
     *
     * @param context The Context for executing database actions and showing Toasts
     */
    private Single<Pair<Integer, Long>> cleanWatchedDownloads(@NonNull Context context) {
        return Single.fromCallable(() -> DownloadedVideosDb.getVideoDownloadsDb().getDownloadedVideosStatuses())
                .flattenAsFlowable(statuses -> statuses)
                .filter(status -> PlaybackStatusDb.getPlaybackStatusDb().getVideoWatchedStatus(status.getVideoId().getId()).isFullyWatched())
                .flatMapSingle(status -> {
                    // We need to save the file sizes...
                    long size = status.getLocalSize();
                    return DownloadedVideosDb.getVideoDownloadsDb().removeDownload(context, status.getVideoId()).map(deleted -> size);
                })
                .reduce(Pair.create(0, 0L), (a, size) -> Pair.create(a.first + 1, a.second + size))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(error -> SkyTubeApp.notifyUserOnError(context, error))
                .doOnSuccess(msg -> Logger.i(this, "Downloads removed: %s - size : %s", msg.first, msg.second));
    }

    /**
     * Undo bookmark for watched videos, that are not necessary anymore
     *
     * @param context The Context for showing Toasts
     */
    private Single<Pair<Integer, Long>> cleanWatchedBookmarks(@NonNull Context context) {
        return Single.fromCallable(() -> BookmarksDb.getBookmarksDb().getAllBookmarkedVideoIds())
                .flattenAsFlowable(ids -> ids)
                .filter(videoId -> PlaybackStatusDb.getPlaybackStatusDb().getVideoWatchedStatus(videoId.getId()).isFullyWatched()) // Filter only watched videos
                .flatMapSingle(videoId -> BookmarksDb.getBookmarksDb().unbookmarkAsync(videoId))
                .reduce(0, (counter, dbResult) -> counter + 1)
                .map(counter -> Pair.create(counter, 0L))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(error -> SkyTubeApp.notifyUserOnError(context, error))
                .doOnSuccess(msg -> Logger.i(this, "Bookmarks removed: %s", msg));
    }
}
