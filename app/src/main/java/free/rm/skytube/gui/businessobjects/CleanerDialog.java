package free.rm.skytube.gui.businessobjects;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.Toast;

import androidx.annotation.NonNull;

import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.Logger;
import free.rm.skytube.businessobjects.db.BookmarksDb;
import free.rm.skytube.businessobjects.db.PlaybackStatusDb;
import free.rm.skytube.databinding.DialogCleanDownloadsBinding;
import free.rm.skytube.gui.activities.MainActivity;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * Dialog for tool that helps clean watched videos from bookmarks
 */
public class CleanerDialog extends SkyTubeMaterialDialog {

    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    public CleanerDialog(@NonNull MainActivity context) {
        super(context);

        final DialogCleanDownloadsBinding binding = DialogCleanDownloadsBinding.inflate(LayoutInflater.from(context));
        title(R.string.cleaner);
        // Set Layout for Dialog
        customView(binding.getRoot(), true);

        positiveText(R.string.cleaner_confirm);
        onPositive((dialog, action) -> {
            boolean cleanWatchedBookmarks = binding.cleanWatchedBookmarks.isChecked();
            if (cleanWatchedBookmarks) {
                compositeDisposable.add(cleanWatchedBookmarks(context)
                        .subscribe(
                                count -> Toast.makeText(context, String.format(
                                        SkyTubeApp.getStr(R.string.cleaner_toast_finish), count), Toast.LENGTH_LONG).show(),
                                error -> SkyTubeApp.notifyUserOnError(context, error)
                        ));
            }
        });

        cancelListener = dialog -> clearBackgroundTasks();
    }

    public void clearBackgroundTasks() { compositeDisposable.clear(); }

    /**
     * Undo bookmark for watched videos, that are not necessary anymore
     *
     * @param context The Context for showing Toasts
     */
    private Single<Integer> cleanWatchedBookmarks(@NonNull Context context) {
        return Single.fromCallable(() -> BookmarksDb.getBookmarksDb().getAllBookmarkedVideoIds())
                .flattenAsFlowable(ids -> ids)
                .filter(videoId -> PlaybackStatusDb.getPlaybackStatusDb().getVideoWatchedStatus(videoId.getId()).isFullyWatched()) // Filter only watched videos
                .flatMapSingle(videoId -> BookmarksDb.getBookmarksDb().unbookmarkAsync(videoId))
                .reduce(0, (counter, dbResult) -> counter + 1)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(error -> SkyTubeApp.notifyUserOnError(context, error))
                .doOnSuccess(msg -> Logger.i(this, "Bookmarks removed: %s", msg));
    }
}
