package free.rm.skytube.gui.activities;

import android.app.Activity;
import android.os.Bundle;

import free.rm.skytube.R;

public class PlayYouTubeVideoActivity extends Activity {

	public static final String VIDEO_ID = "video_id";
	private static final String TAG = PlayYouTubeVideoActivity.class.getSimpleName();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_view_video);

		//Toast.makeText(this, getIntent().getExtras().getString(VIDEO_ID), Toast.LENGTH_SHORT).show();
		//Log.i(TAG, getIntent().getExtras().getString(VIDEO_ID));
	}

}
