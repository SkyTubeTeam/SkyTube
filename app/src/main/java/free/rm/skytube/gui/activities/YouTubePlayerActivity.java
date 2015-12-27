package free.rm.skytube.gui.activities;

import android.app.Activity;
import android.os.Bundle;

import free.rm.skytube.R;

/**
 * An {@link Activity} that contains an instance of
 * {@link free.rm.skytube.gui.fragments.YouTubePlayerFragment}.
 */
public class YouTubePlayerActivity extends Activity {

	public static final String VIDEO_ID = "video_id";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_video_player);
	}

}
