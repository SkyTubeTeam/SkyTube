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

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import free.rm.skytube.businessobjects.Logger;
import free.rm.skytube.businessobjects.YouTube.POJOs.CardData;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;
import free.rm.skytube.businessobjects.YouTube.Tasks.GetYouTubeVideosTask;

/**
 * Returns a list of YouTube videos.
 *
 * <p>Do not run this directly, but rather use {@link GetYouTubeVideosTask}.</p>
 */
public abstract class GetYouTubeVideos {
	protected String nextPageToken = null;
	protected boolean noMoreVideoPages = false;
	private IOException lastException;

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
	public abstract List<CardData> getNextVideos();


	/**
	 * @return True if YouTube states that there will be no more video pages; false otherwise.
	 */
	public boolean noMoreVideoPages() {
		return noMoreVideoPages;
	}


	/**
	 * Reset the fetching of videos. This will be called when a swipe to refresh is done.
	 */
	public void reset() {
		nextPageToken = null;
		noMoreVideoPages = false;
		lastException = null;
	}

	public IOException getLastException() {
		return lastException;
	}

	protected void setLastException(IOException lastException) {
		this.lastException = lastException;
	}



	/**
	 * <p>Hence, we need to submit the video IDs to YouTube to retrieve more information about the
	 * given video list.</p>
	 *
	 * @param videoIds Search results
	 * @return List of {@link YouTubeVideo}s.
	 * @throws IOException
	 */
	protected List<CardData> getVideoListFromIds(List<String> videoIds) throws IOException {
		if (videoIds == null || videoIds.isEmpty()) {
			return Collections.emptyList();
		}
		StringBuilder videoIdsStr = new StringBuilder();

		// append the video IDs into a strings (CSV)
		for (String id : videoIds) {
			videoIdsStr.append(id);
			videoIdsStr.append(',');
		}

		if (videoIdsStr.length() > 0) {
			videoIdsStr.setLength(videoIdsStr.length() - 1);
		}
		// get video details by supplying the videos IDs
		GetVideosDetailsByIDs getVideo = new GetVideosDetailsByIDs();
		getVideo.init(videoIdsStr.toString());
		Logger.i(this, "getVideList light from %s id, video ids: %s", videoIds.size(), videoIdsStr);

		return getVideo.getNextVideos();
	}

}
