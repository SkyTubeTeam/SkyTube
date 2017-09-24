package free.rm.skytube.businessobjects;

import java.util.List;

/**
 * Interface used by {@link GetChannelVideosTask} to return the videos belonging to a channel.
 */
public interface GetChannelVideosTaskInterface {
	void onGetVideos(List<YouTubeVideo> videos);
}
