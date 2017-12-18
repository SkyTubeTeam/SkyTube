package free.rm.skytube.gui.businessobjects;

import java.io.IOException;
import java.util.List;

import free.rm.skytube.businessobjects.GetYouTubeVideos;
import free.rm.skytube.businessobjects.YouTubeVideo;
import free.rm.skytube.businessobjects.db.DownloadedVideosDb;

/**
 * Get Downloaded Videos
 */
public class GetDownloadedVideos extends GetYouTubeVideos {
	@Override
	public void init() throws IOException {
		noMoreVideoPages = false;
	}

	@Override
	public List<YouTubeVideo> getNextVideos() {
		if (!noMoreVideoPages()) {
			noMoreVideoPages = true;
			return DownloadedVideosDb.getVideoDownloadsDb().getDownloadedVideos();
		}

		return null;
	}

	@Override
	public boolean noMoreVideoPages() {
		return noMoreVideoPages;
	}
}
