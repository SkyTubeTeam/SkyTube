package free.rm.skytube.gui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.schabi.newpipe.extractor.StreamingService;

import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.GetVideoDetailsTask;
import free.rm.skytube.businessobjects.YouTube.newpipe.ContentId;

/**
 * Activity that receives an intent from other apps in order to bookmark a video from another app.
 * This Activity uses a transparent theme, and finishes right away, so as not to take focus from the sharing app.s
 */
public class ShareBookmarkActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(getIntent() != null) {
            String text_data = getIntent().getStringExtra(Intent.EXTRA_TEXT);
            ContentId content = SkyTubeApp.parseUrl(this, text_data, false);
            if (content != null && content.getType() == StreamingService.LinkType.STREAM) {
                new GetVideoDetailsTask(this, content, (videoUrl, video) -> {
                    if (video != null) {
                        video.bookmarkVideo(ShareBookmarkActivity.this);
                    } else {
                        invalidUrlError();
                    }
                }).setFinishCallback(this::finish).executeInParallel();
            } else {
                SkyTubeApp.openUrl(this, text_data, false);
                finish();
            }
        }
    }

    private void invalidUrlError() {
        Toast.makeText(this, R.string.bookmark_share_invalid_url, Toast.LENGTH_LONG).show();
        finish();
    }
}
