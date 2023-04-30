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

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.io.IOException;
import java.util.List;

import free.rm.skytube.businessobjects.YouTube.POJOs.CardData;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;
import free.rm.skytube.gui.businessobjects.adapters.VideoGridAdapter;

/**
 * Returns a list of YouTube videos.
 *
 * <p>Do not run this directly, but rather use {@link YouTubeTasks#getYouTubeVideos(GetYouTubeVideos,
 * VideoGridAdapter, SwipeRefreshLayout, boolean)}.</p>
 */
public abstract class GetYouTubeVideos {
	protected String nextPageToken = null;
	protected boolean noMoreVideoPages = false;
	private Exception lastException;

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


	public void resetKey() {
	}

	/**
	 * Reset the fetching of videos. This will be called when a swipe to refresh is done.
	 */
	public void reset() {
		nextPageToken = null;
		noMoreVideoPages = false;
		lastException = null;
	}

	public Exception getLastException() {
		return lastException;
	}

	protected void setLastException(Exception lastException) {
		this.lastException = lastException;
	}

}
