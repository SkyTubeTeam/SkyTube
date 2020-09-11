/*
 * SkyTube
 * Copyright (C) 2015  Ramon Mifsud
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

package free.rm.skytube.gui.businessobjects;

import android.app.Activity;
import android.view.KeyEvent;
import android.widget.MediaController;
import android.widget.VideoView;

import free.rm.skytube.businessobjects.interfaces.YouTubePlayerFragmentInterface;

/**
 * A {@link MediaController} that handles the back button events.
 *
 * <p>When a user has pressed the back button (of the Navigation Bar), then this controller will
 * stop the video and closes the calling activity.</p>
 */
public class MediaControllerEx extends MediaController {

	private boolean     hideController = false;
	private YouTubePlayerFragmentInterface fragmentInterface;


	/**
	 * Initialises this object.  It also attaches the supplied VideoView with this MediaController.
	 *
	 * @param activity	Activity where this controller will run on.
	 * @param videoView	VideoView that this controller will control.
	 */
	public MediaControllerEx(Activity activity, VideoView videoView, YouTubePlayerFragmentInterface fragmentInterface) {
		super(activity);
		videoView.setMediaController(this);
		this.fragmentInterface = fragmentInterface;
	}


	/**
	 * Ignore hide() call.  This is as there is a bug in Android 5.0+ in which hide() is internally
	 * called by {@link MediaController} even if show(0) is explicitly called (i.e. Android was not
	 * honoring show(0)).
	 *
	 * <p>{@link #hide()} will be ignored until we call {@link #hideController()} </p>
	 *
	 * <p>Call {@link #hideController()} is you want to hide the controller instead of {@link #hide()}.
	 */
	@Override
	public void hide() {
		if (!hideController)
			super.show(0);
	}


	/**
	 * Hides the controller from screen.
	 */
	public void hideController() {
		super.hide();
		hideController = true;
	}


	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		// if the user has pressed the BACK button (of the Navigation Bar), then ...
		if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
			if (event.getAction() == KeyEvent.ACTION_DOWN) {
				// Tell the Fragment to stop playback, and record the playing video's position if needed
				fragmentInterface.videoPlaybackStopped();

				// ask the activity to finish
				((Activity) getContext()).finish();
			}

			// true means we handles the event
			return true;
		}

		return super.dispatchKeyEvent(event);
	}

}
