package free.rm.skytube.gui.businessobjects;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;

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
            AtomicInteger removedVideosCnt = new AtomicInteger();
            AtomicLong removedMB = new AtomicLong();
            compositeDisposable.add(Single.merge(tasks).doFinally(() -> Toast.makeText(context, String.format(
                            SkyTubeApp.getStr(R.string.cleaner_toast_finish), removedVideosCnt, removedMB), Toast.LENGTH_LONG).show())
                    .subscribe(pair -> {
                        removedVideosCnt.addAndGet(pair.first);
                        removedMB.addAndGet(pair.second);
                    }));


        });

        cancelListener = dialog -> clearBackgroundTasks();
    }

    public void clearBackgroundTasks() { compositeDisposable.clear(); }

    /**
     * Cleans watched Videos, that are downloaded by the user
     *
     * @param context The Context for executing database actions and showing Toasts
     */
    private Single<Pair<Integer,Long>> cleanWatchedDownloads(@NonNull Context context) {
        return Single.fromCallable(() -> {
                    // Stats
                    AtomicInteger removedVideosCnt = new AtomicInteger();
                    AtomicLong removedB = new AtomicLong();
                    // Maybe rewrite this with reactivex, so that it's parallel
                    DownloadedVideosDb.getVideoDownloadsDb().getDownloadedVideosStatuses().stream()
                            .filter((status) -> PlaybackStatusDb.getPlaybackStatusDb().getVideoWatchedStatus(status.getVideoId().getId()).isFullyWatched()) // Filter only watched videos
                            .forEach((status) -> {
                                // Try to remove download
                                DownloadedVideosDb.getVideoDownloadsDb().removeDownload(context, status.getVideoId()).subscribe();
                                // Stats
                                removedVideosCnt.getAndIncrement();
                                removedB.addAndGet(status.getLocalVideoFile().length());
                                removedB.addAndGet(status.getLocalAudioFile() != null ? status.getLocalAudioFile().length() : 0);
                            });
                    // Format everything in a nice string to output
                    return new Pair<>(removedVideosCnt.intValue(), removedB.longValue() / 1000 / 1000 /* in MB */);
                }).subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(error -> SkyTubeApp.notifyUserOnError(context, error)
                ).doOnSuccess(msg -> Logger.i(this, "(Downloads)" + SkyTubeApp.getStr(R.string.cleaner_toast_finish),
                        msg.first, msg.second));
    }

    /**
     * Undo bookmark for watched videos, that are not necessary anymore
     *
     * @param context The Context for showing Toasts
     */
    private Single<Pair<Integer,Long>> cleanWatchedBookmarks(@NonNull Context context) {
        return Single.fromCallable(() -> {
                    // Stats
                    AtomicInteger removedVideosCnt = new AtomicInteger();
                    AtomicLong removedB = new AtomicLong();
                    // Maybe rewrite this with reactivex, so that it's parallel
                    BookmarksDb.getBookmarksDb().getBookmarkedVideos(BookmarksDb.getBookmarksDb().getTotalBookmarkCount().blockingGet(), null).first.stream()
                            .filter((video) -> PlaybackStatusDb.getPlaybackStatusDb().getVideoWatchedStatus(video.getId()).isFullyWatched()) // Filter only watched videos
                            .forEach((video) -> {
                                // Try to remove bookmark
                                BookmarksDb.getBookmarksDb().unbookmarkAsync(video.getVideoId()).subscribe();
                                // Stats
                                removedVideosCnt.getAndIncrement();
                                removedB.addAndGet(4930); // Raw estimate what one bookmark takes up in Bytes
                            });
                    // Format everything in a nice string to output
                    return new Pair<>(removedVideosCnt.intValue(), removedB.longValue() / 1000 / 1000);
                }).subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(error -> SkyTubeApp.notifyUserOnError(context, error)
                ).doOnSuccess(msg -> Logger.i(this, "(Bookmarks)" + SkyTubeApp.getStr(R.string.cleaner_toast_finish),
                        msg.first, msg.second));
    }
}
