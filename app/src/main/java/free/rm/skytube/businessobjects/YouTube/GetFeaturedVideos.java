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

import android.util.Log;

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;

import java.io.IOException;
import java.util.List;

import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeAPI;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeAPIKey;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;

/**
 * Get today's featured YouTube videos.
 */
public class GetFeaturedVideos extends GetYouTubeVideos {

	protected YouTube.Videos.List videosList = null;

	private static final String	TAG = GetFeaturedVideos.class.getSimpleName();


	@Override
	public void init() throws IOException {
		videosList = YouTubeAPI.create().videos().list("snippet, statistics, contentDetails");
		videosList.setFields("items(id, snippet/defaultAudioLanguage, snippet/defaultLanguage, snippet/publishedAt, snippet/title, snippet/channelId, snippet/channelTitle," +
				"snippet/thumbnails, contentDetails/duration, statistics)," +
				"nextPageToken");
		videosList.setKey(YouTubeAPIKey.get().getYouTubeAPIKey());
		videosList.setChart("mostPopular");
		videosList.setRegionCode(getPreferredRegion());
		videosList.setMaxResults(getMaxResults());
		nextPageToken = null;
	}

	@Override
	public List<YouTubeVideo> getNextVideos() {
		List<Video> searchResultList = null;

		if (!noMoreVideoPages()) {
			try {
				// set the page token/id to retrieve
				videosList.setPageToken(nextPageToken);

				// communicate with YouTube
				VideoListResponse response = videosList.execute();

				// get videos
				searchResultList = response.getItems();

				// set the next page token
				nextPageToken = response.getNextPageToken();

				// if nextPageToken is null, it means that there are no more videos
				if (nextPageToken == null)
					noMoreVideoPages = true;
			} catch (IOException e) {
				Log.e(TAG, "Error has occurred while getting Featured Videos.", e);
			}
		}

		return toYouTubeVideoList(searchResultList);
	}


	@Override
	public boolean noMoreVideoPages() {
		return noMoreVideoPages;
	}


	/**
	 * @return The maximum number of items that should be retrieved per YouTube query.
	 */
	protected Long getMaxResults() {
		return 50L;
	}

}
