package free.rm.skytube.gui.activities;


import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import free.rm.skytube.R;
import free.rm.skytube.businessobjects.GetVideoDetailsTask;

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
                    video.bookmarkVideo(ShareBookmarkActivity.this);
                    finish();
                } else {
                    Toast.makeText(ShareBookmarkActivity.this, R.string.bookmark_share_invalid_url, Toast.LENGTH_LONG).show();
                    finish();
                }
            }).executeInParallel();
        }
    }
}
