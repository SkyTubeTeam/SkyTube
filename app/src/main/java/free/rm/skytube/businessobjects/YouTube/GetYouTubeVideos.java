/*
 * SkyTube
 * Copyright (C) 2018  Ramon Mifsud
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

package free.rm.skytube.businessobjects.YouTube;

import com.google.api.services.youtube.model.Video;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.YouTube.Tasks.GetYouTubeVideosTask;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;

/**
 * Returns a list of YouTube videos.
 *
 * <p>Do not run this directly, but rather use {@link GetYouTubeVideosTask}.</p>
 */
public abstract class GetYouTubeVideos {
	protected String nextPageToken = null;
	protected boolean noMoreVideoPages = false;

	/**
	 * Initialise this object.
	 *
	 * @throws IOException
	 */
	public abstract void init() throws IOException;


	/**
	 * Sets user's query. [optional]
	 */
	public void setQuery(String query) {
	}


	/**
	 * Gets the next page of videos.
	 *
	 * @return List of {@link YouTubeVideo}s.
	 */
	public abstract List<YouTubeVideo> getNextVideos();


	/**
	 * @return True if YouTube states that there will be no more video pages; false otherwise.
	 */
	public abstract boolean noMoreVideoPages();


	/**
	 * Converts {@link List} of {@link Video} to {@link List} of {@link YouTubeVideo}.
	 *
	 * @param videoList {@link List} of {@link Video}.
	 * @return {@link List} of {@link YouTubeVideo}.
	 */
	protected List<YouTubeVideo> toYouTubeVideoList(List<Video> videoList) {
		List<YouTubeVideo> youTubeVideoList = new ArrayList<>();

		if (videoList != null) {
			YouTubeVideo youTubeVideo;

			for (Video video : videoList) {
				youTubeVideo = new YouTubeVideo(video);
				if (!youTubeVideo.filterVideoByLanguage())
					youTubeVideoList.add(youTubeVideo);
			}
		}

		return youTubeVideoList;
	}


	protected String getPreferredRegion() {
		String region = SkyTubeApp.getPreferenceManager()
				.getString(SkyTubeApp.getStr(R.string.pref_key_preferred_region), "").trim();
		return (region.isEmpty() ? null : region);
	}

	/**
	 * Reset the fetching of videos. This will be called when a swipe to refresh is done.
	 */
	public void reset() {
		nextPageToken = null;
		noMoreVideoPages = false;
	}
}
