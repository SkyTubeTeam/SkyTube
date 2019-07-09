package free.rm.skytube.businessobjects;

import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;

/**
 * Chromecast Listener interface. {@link free.rm.skytube.gui.activities.BaseActivity} implements this interface in order
 * for other classes to communicate with it, for Chromecast specific functionality.
 */
public interface ChromecastListener {
	/**
	 * Play the passed video on Chromecast at the passed position.
	 *
	 * @param video
	 * @param position
	 */
	void playVideoOnChromecast(YouTubeVideo video, int position);

	/**
	 * When starting to play a video on Chromecast, show a spinner.
	 */
	void showLoadingSpinner();

	/**
	 * This method gets called when a video starts playing on Chromecast.
	 */
	void onPlayStarted();

	/**
	 * This method gets called when a video stops playing on Chromecast.
	 */
	void onPlayStopped();

	/**
	 * When returning to {@link free.rm.skytube.gui.fragments.MainFragment} from a fragment that uses
	 * CoordinatorLayout, redraw the Sliding Panel. This fixes an apparent bug in CoordinatorLayout that
	 * causes the panel to be positioned improperly (the bottom half of the panel ends up below the screen)
	 */
	void redrawPanel();

	/**
	 * When a connection to a Chromecast has started, this method will be called.
	 * This is used when a user is watching a video on the device, and connects to a Chromecast while the video
	 * is playing. {@link free.rm.skytube.gui.activities.YouTubePlayerActivity} will use this method to pause the currently playing
	 * video while the connection to the Chromecast is being set up.
	 */
	void onSessionStarting();
}
