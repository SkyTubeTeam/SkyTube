package free.rm.skytube.gui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.schabi.newpipe.extractor.StreamingService;

import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.YouTube.YouTubeTasks;
import free.rm.skytube.businessobjects.YouTube.newpipe.ContentId;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

/**
 * Activity that receives an intent from other apps in order to bookmark a video from another app.
 * This Activity uses a transparent theme, and finishes right away, so as not to take focus from the sharing app.s
 */
public class ShareBookmarkActivity extends AppCompatActivity {
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(getIntent() != null) {
            String textData = getIntent().getStringExtra(Intent.EXTRA_TEXT);
            ContentId contentId = SkyTubeApp.parseUrl(this, textData, false);
            if (contentId.getType() == StreamingService.LinkType.STREAM) {
                compositeDisposable.add(YouTubeTasks.getVideoDetails(this, contentId)
                        .subscribe(video -> {
                            if (video != null) {
                                video.bookmarkVideo(ShareBookmarkActivity.this);
                            } else {
                                invalidUrlError();
                            }
                            finish();
                        }));
            } else {
                SkyTubeApp.openUrl(this, textData, false);
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        compositeDisposable.clear();
        super.onDestroy();
    }

    private void invalidUrlError() {
        Toast.makeText(this, R.string.bookmark_share_invalid_url, Toast.LENGTH_LONG).show();
        finish();
    }
}
