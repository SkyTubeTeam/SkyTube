package free.rm.skytube.businessobjects.interfaces;

import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;

/**
 * Interface used by {@link free.rm.skytube.gui.fragments.YouTubePlayerV1Fragment} & {@link free.rm.skytube.gui.fragments.YouTubePlayerV2Fragment}
 * Also, when a video is playing on a Chromecast, and the user disconnects from the Chromecast, that video will begin playing on the device.
 */

public interface YouTubePlayerFragmentInterface {
	void videoPlaybackStopped();

	/**
	 * Return the video being played in this fragment
	 * @return {@link YouTubeVideo}
	 */
	YouTubeVideo getYouTubeVideo();

	/**
	 * Return the current video position.
	 * @return
	 */
	int getCurrentVideoPosition();

	/**
	 * Whether the current video is playing.
	 * @return boolean
	 */
	boolean isPlaying();

	/**
	 * Pause the currently playing video.
	 */
	void pause();

	/**
	 * Start playback of current video.
	 */
	void play();

	void setPlaybackStateListener(PlaybackStateListener listener);
}
