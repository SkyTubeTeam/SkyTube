/*
 * SkyTube
 * Copyright (C) 2016  Ramon Mifsud
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation (version 3 of the License).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package free.rm.skytube.gui.activities;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.Logger;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubePlaylist;
import free.rm.skytube.businessobjects.interfaces.YouTubePlayerActivityListener;
import free.rm.skytube.businessobjects.interfaces.YouTubePlayerFragmentInterface;
import free.rm.skytube.gui.businessobjects.YoutubePlayerMediaSession;
import free.rm.skytube.gui.businessobjects.fragments.FragmentEx;
import free.rm.skytube.gui.fragments.YouTubePlayerTutorialFragment;
import free.rm.skytube.gui.fragments.YouTubePlayerV1Fragment;
import free.rm.skytube.gui.fragments.YouTubePlayerV2Fragment;

/**
 * An {@link Activity} that contains an instance of either {@link YouTubePlayerV2Fragment} or
 * {@link YouTubePlayerV1Fragment}.
 */
public class YouTubePlayerActivity extends BaseActivity implements YouTubePlayerActivityListener {
	public static final String YOUTUBE_VIDEO = "YouTubePlayerActivity.YouTubeVideo";
	public static final String YOUTUBE_VIDEO_POSITION = "YouTubePlayerActivity.YouTubeVideoPosition";
	public static final int YOUTUBE_PLAYER_RESUME_RESULT = 2931;

	private FragmentEx videoPlayerFragment;
	private YouTubePlayerFragmentInterface fragmentListener;
	private YoutubePlayerMediaSession mediaSession = null;

	public static final String YOUTUBE_VIDEO_OBJ  = "YouTubePlayerActivity.video_object";


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		final boolean useDefaultPlayer = useDefaultPlayer();
		// MediaSession isn't available on SDKs before 21
		if (Build.VERSION.SDK_INT >= 21) {
			mediaSession = new YoutubePlayerMediaSession(this);
		}

		// if the user wants to use the default player, then ensure that the activity does not
		// have a toolbar (actionbar) -- this is as the fragment is taking care of the toolbar
		if (useDefaultPlayer) {
			setTheme(R.style.NoActionBarActivityTheme);
		}

		super.onCreate(savedInstanceState);
        setContentView(binding.getRoot());

		// if the tutorial was previously displayed, the just "install" the video player fragment
		if (SkyTubeApp.getSettings().wasTutorialDisplayedBefore()) {
			installNewVideoPlayerFragment(useDefaultPlayer);
		} else {
			// display the tutorial
			FragmentEx tutorialFragment = new YouTubePlayerTutorialFragment().setListener(() -> installNewVideoPlayerFragment(useDefaultPlayer));
			installFragment(tutorialFragment);
		}

		registerPlaybackPauseReceiver(true);
	}


	private boolean receiverRegistered = false;
	private final BroadcastReceiver playbackPauseReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (fragmentListener != null && fragmentListener.isPlaying()) {
				fragmentListener.pause();
			}
		}
	};

	private synchronized void registerPlaybackPauseReceiver(boolean register) {
		if (register != receiverRegistered) {
			if (register) {
				// when audio is "becoming noisy", the device is switching audio to speaker from headphones
				IntentFilter filter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
				registerReceiver(playbackPauseReceiver, filter);
			} else {
				unregisterReceiver(playbackPauseReceiver);
			}
			this.receiverRegistered = register;
		}
	}

	/**
	 * @return True if the user wants to use SkyTube's default video player;  false if the user wants
	 * to use the legacy player.
	 */
	private boolean useDefaultPlayer() {
		if (Build.VERSION.SDK_INT < 16) {
			Logger.i(this, "Android version is old, ExoPlayer probably wont work: "+ Build.VERSION.SDK_INT);
			//return false;
		}
		final String defaultPlayerValue = getString(R.string.pref_default_player_value);
		final String str = SkyTubeApp.getPreferenceManager().getString(getString(R.string.pref_key_choose_player), defaultPlayerValue);

		return str.equals(defaultPlayerValue);
	}

	// If the back button in the toolbar is hit, save the video's progress (if playback history is not disabled)
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// close this activity when the user clicks on the back button (action bar)
		if (item.getItemId() == android.R.id.home) {
			fragmentListener.videoPlaybackStopped();
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onOptionsMenuCreated(Menu menu) {
		super.onOptionsMenuCreated(menu);
	}

	@Override
	protected boolean isLocalPlayer() {
		return true;
	}


	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// media events are handled by MediaSession instead of being passed
		// as keyDown events starting from SDK v21
		if (Build.VERSION.SDK_INT < 21 && handleMediaKeyDown(keyCode)) {
			return true;
		}

		return super.onKeyDown(keyCode, event);
	}


	/**
	 * Executes appropriate media action for media button key codes.
	 * @return boolean - whether media action was executed
	 */
	public boolean handleMediaKeyDown(int keyCode) {
		switch (keyCode) {
			case KeyEvent.KEYCODE_MEDIA_PLAY:
				fragmentListener.play();
				return true;
            case KeyEvent.KEYCODE_MEDIA_STOP:
			case KeyEvent.KEYCODE_MEDIA_PAUSE:
				fragmentListener.pause();
				return true;
			case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
				if (fragmentListener.isPlaying()) {
					fragmentListener.pause();
				} else {
					fragmentListener.play();
				}
				return true;
			default:
				return false;
		}
	}


	/**
	 * "Installs" the video player fragment.
	 *
	 * @param useDefaultPlayer  True to use the default player; false to use the legacy one.
	 */
	private void installNewVideoPlayerFragment(boolean useDefaultPlayer) {
		videoPlayerFragment = useDefaultPlayer ? new YouTubePlayerV2Fragment() : new YouTubePlayerV1Fragment();

		try {
			fragmentListener = (YouTubePlayerFragmentInterface) videoPlayerFragment;
		} catch(ClassCastException e) {
			throw new ClassCastException(videoPlayerFragment.toString()
					+ " must implement YouTubePlayerFragmentInterface");
		}

		if (mediaSession != null) {
			mediaSession.bindToPlayer(fragmentListener);
		}
		installFragment(videoPlayerFragment);
	}


	/**
	 * "Installs" a fragment inside the {@link FragmentManager}.
	 *
	 * @param fragment  Fragment to install and that is going to be displayed to the user.
	 */
	private void installFragment(FragmentEx fragment) {
		// either use the SkyTube's default video player or the legacy one
		FragmentManager fragmentManager = getSupportFragmentManager();
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

		fragmentTransaction.add(R.id.fragment_container, fragment);
		fragmentTransaction.commit();
	}


	@Override
	protected void onStart() {
		if (mediaSession != null) {
			mediaSession.setActive(true);
		}
		super.onStart();

		// set the video player's orientation as what the user wants
		String  str = SkyTubeApp.getPreferenceManager().getString(getString(R.string.pref_key_screen_orientation), getString(R.string.pref_screen_auto_value));
		int     orientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;

		if (str.equals(getString(R.string.pref_screen_landscape_value)))
			orientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
		if (str.equals(getString(R.string.pref_screen_portrait_value)))
			orientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT;
		if (str.equals(getString(R.string.pref_screen_sensor_value)))
			orientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR;

		setRequestedOrientation(orientation);
	}


	@Override
	protected void onStop() {
		if (mediaSession != null) {
			mediaSession.setActive(false);
		}

		super.onStop();
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
	}


	@Override
	public void onPanelClosed(int featureId, Menu menu) {
		super.onPanelClosed(featureId, menu);

		// notify the player that the menu is no longer visible
		if (videoPlayerFragment instanceof YouTubePlayerV2Fragment) {
			((YouTubePlayerV2Fragment) videoPlayerFragment).onMenuClosed();
		}
	}


	@Override
	public void onBackPressed() {
		if (fragmentListener != null) {
			fragmentListener.videoPlaybackStopped();
		}
		super.onBackPressed();
	}

	@Override
	public void onSessionStarting() {
		fragmentListener.pause();
	}

	// This is called when connecting to a Chromecast from this activity. It will tell BaseActivity
	// to launch the video that was playing on the Chromecast.
	@Override
	protected void returnToMainAndResume() {
		Bundle bundle = new Bundle();
		bundle.putSerializable(YOUTUBE_VIDEO, fragmentListener.getYouTubeVideo());
		bundle.putInt(YOUTUBE_VIDEO_POSITION, fragmentListener.getCurrentVideoPosition());

		if(getIntent() != null && getIntent().getAction() != null && getIntent().getAction().equals(Intent.ACTION_VIEW)) {
			Intent intent = new Intent(YouTubePlayerActivity.this, MainActivity.class);
			intent.putExtras(bundle);
			intent.setData(Uri.parse(fragmentListener.getYouTubeVideo().getVideoUrl()));
			intent.setAction(Intent.ACTION_VIEW);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME);
			startActivity(intent);
			finish();
		} else {
			Intent intent = new Intent();
			intent.putExtras(bundle);
			setResult(RESULT_OK, intent);
			finish();
		}
	}


	@Override
	public void finish() {
		if (mediaSession != null) {
			mediaSession.release();
		}
		registerPlaybackPauseReceiver(false);
		super.finish();
	}


    @Override
    public void onPlaylistClick(YouTubePlaylist playlist) {}

    /**
     * No-op method. Since this class needs to extend BaseActivity, in order to be able to connect to a Chromecast from
     * this activity, it needs to implement this method, but doesn't need to do anything, since it doesn't use
     * SubscriptionsFeedFragment.
     */
    @Override
    public void refreshSubscriptionsFeedVideos() {}

}
