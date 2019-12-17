package free.rm.skytube.gui.activities;


import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import free.rm.skytube.R;
import free.rm.skytube.businessobjects.GetVideoDetailsTask;
import free.rm.skytube.gui.businessobjects.views.ClickableLinksTextView;

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
            new GetVideoDetailsTask(text_data, (videoUrl, video) -> {
                if (video != null) {
                    boolean bookmarked = video.bookmarkVideo(ShareBookmarkActivity.this);
                    Toast.makeText(ShareBookmarkActivity.this,
                            bookmarked ? R.string.video_bookmarked : R.string.video_bookmarked_error,
                            Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    Toast.makeText(ShareBookmarkActivity.this, R.string.bookmark_share_invalid_url, Toast.LENGTH_LONG).show();
                    finish();
                }
            }).executeInParallel();
        }
    }
}
