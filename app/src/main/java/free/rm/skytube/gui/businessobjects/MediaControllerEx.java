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

/**
 * A {@link MediaController} that handles the back button events.
 *
 * <p>When a user has pressed the back button (of the Navigation Bar), then this controller will
 * stop the video and closes the calling activity.</p>
 */
public class MediaControllerEx extends MediaController {

	private VideoView videoView;


	/**
	 * Initialises this object.  It also attaches the supplied VideoView with this MediaController.
	 *
	 * @param activity	Activity where this controller will run on.
	 * @param videoView	VideoView that this controller will control.
	 */
	public MediaControllerEx(Activity activity, VideoView videoView) {
		super(activity);
		this.videoView = videoView;
		this.videoView.setMediaController(this);
	}


	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		// if the user has pressed the BACK button (of the Navigation Bar), then ...
		if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
			if (event.getAction() == KeyEvent.ACTION_DOWN) {
				// stop playing the video
				videoView.stopPlayback();

				// ask the activity to finish
				((Activity) getContext()).finish();
			}

			// true means we handles the event
			return true;
		}

		return super.dispatchKeyEvent(event);
	}

}
