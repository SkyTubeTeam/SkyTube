package free.rm.skytube.gui.activities;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import free.rm.skytube.R;

/**
 * An {@link Activity} that contains an instance of
 * {@link free.rm.skytube.gui.fragments.YouTubePlayerFragment}.
 */
public class YouTubePlayerActivity extends AppCompatActivity {

	public static final String YOUTUBE_VIDEO_OBJ = "YouTubePlayerActivity.yt_video_obj";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_video_player);

		// enable back button (action bar)
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null)
			actionBar.setDisplayHomeAsUpEnabled(true);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			// close this activity when the user clicks on the back button (action bar)
			case android.R.id.home:
				finish();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
}
