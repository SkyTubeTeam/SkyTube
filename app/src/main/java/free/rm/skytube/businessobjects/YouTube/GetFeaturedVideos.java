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

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.Logger;
import free.rm.skytube.businessobjects.YouTube.POJOs.CardData;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeAPI;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeAPIKey;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;

/**
 * Get today's featured YouTube videos.
 */
public class GetFeaturedVideos extends GetYouTubeVideos {

	protected YouTube.Videos.List videosList = null;


	@Override
	public void init() throws IOException {
		videosList = YouTubeAPI.create().videos().list("snippet, statistics, contentDetails");
		videosList.setFields("items(id, snippet/defaultAudioLanguage, snippet/defaultLanguage, snippet/publishedAt, snippet/title, snippet/channelId, snippet/channelTitle," +
				"snippet/thumbnails, contentDetails/duration, statistics)," +
				"nextPageToken");
		videosList.setKey(YouTubeAPIKey.get().getYouTubeAPIKey());
		videosList.setChart("mostPopular");
		videosList.setMaxResults(getMaxResults());
		nextPageToken = null;
	}

	@Override
	public void resetKey() {
		videosList.setKey(YouTubeAPIKey.get().getYouTubeAPIKey());
	}

	@Override
	public List<CardData> getNextVideos() {
		setLastException(null);

		if (!noMoreVideoPages()) {
			try {
				// set the page token/id to retrieve
				videosList.setPageToken(nextPageToken);

				// set the preferred region (placed here to reflect any changes performed at runtime)
				videosList.setRegionCode(getPreferredRegion());

				// communicate with YouTube
				VideoListResponse response = videosList.execute();

				// get videos
				List<Video> searchResultList = response.getItems();

				// set the next page token
				nextPageToken = response.getNextPageToken();

				// if nextPageToken is null, it means that there are no more videos
				if (nextPageToken == null)
					noMoreVideoPages = true;
				return toYouTubeVideoList(searchResultList);
			} catch (IOException e) {
				setLastException(e);
				Logger.e(this, "Error has occurred while getting Featured Videos:" + e.getMessage(), e);
			}
		}
		return Collections.emptyList();
	}


	/**
	 * Converts {@link List} of {@link Video} to {@link List} of {@link YouTubeVideo}.
	 *
	 * @param videoList {@link List} of {@link Video}.
	 * @return {@link List} of {@link YouTubeVideo}.
	 */
	private List<CardData> toYouTubeVideoList(List<Video> videoList) {
		List<CardData> youTubeVideoList = new ArrayList<>();

		if (videoList != null) {
			for (Video video : videoList) {
				try {
					YouTubeVideo yv = new YouTubeVideo(video);
					youTubeVideoList.add(yv);
				} catch (Exception ex) {
					Logger.e(this, "Unable to convert video: " + video + ", error:" + ex.getMessage(), ex);
				}
			}
		}

		return youTubeVideoList;
	}


	private String getPreferredRegion() {
		String region = SkyTubeApp.getPreferenceManager()
				.getString(SkyTubeApp.getStr(R.string.pref_key_preferred_region), "").trim();
		return (region.isEmpty() ? null : region);
	}


	/**
	 * @return The maximum number of items that should be retrieved per YouTube query.
	 */
	protected Long getMaxResults() {
		return 50L;
	}

}
