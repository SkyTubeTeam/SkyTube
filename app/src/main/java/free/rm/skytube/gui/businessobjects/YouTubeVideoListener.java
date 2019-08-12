package free.rm.skytube.gui.businessobjects;


import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;

/**
 * Interface to return a YouTubeVideo via an url to the video. Returns the url as well as the video, if one was found.
 */
public interface YouTubeVideoListener {
	void onYouTubeVideo(String videoUrl, YouTubeVideo video);
}
