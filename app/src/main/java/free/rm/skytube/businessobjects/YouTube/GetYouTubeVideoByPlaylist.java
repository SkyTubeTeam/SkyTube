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
import com.google.api.services.youtube.model.PlaylistItem;
import com.google.api.services.youtube.model.PlaylistItemListResponse;
import com.google.api.services.youtube.model.SearchResult;

import java.io.IOException;
import java.util.List;

import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeAPI;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeAPIKey;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;

/**
 * Get videos corresponding to the user's query (refer to {@link #setQuery(String)}).
 */
public class GetYouTubeVideoByPlaylist extends GetYouTubeVideos {

	protected YouTube.PlaylistItems.List videosList = null;

	private static final String	TAG = GetYouTubeVideoByPlaylist.class.getSimpleName();
	protected static final Long	MAX_RESULTS = 45L;

	private String playlistId;

	public GetYouTubeVideoByPlaylist(String playlistId) {
		this.playlistId = playlistId;
	}


	@Override
	public void init() throws IOException {
		videosList = YouTubeAPI.create().playlistItems().list("snippet,id");
		videosList.set("playlistId",playlistId);
		videosList.setFields("items(snippet/resourceId/videoId), nextPageToken");
		videosList.setKey(YouTubeAPIKey.get().getYouTubeAPIKey());
		//videosList.setType("video");
		//videosList.setRegionCode(getPreferredRegion());	// there is a bug in V3 API, so this does not work:  https://code.google.com/p/gdata-issues/issues/detail?id=6383 and https://code.google.com/p/gdata-issues/issues/detail?id=6913
		//videosList.setSafeSearch("none");
		videosList.setMaxResults(MAX_RESULTS);
		nextPageToken = null;
	}




	@Override
	public List<YouTubeVideo> getNextVideos() {
		List<YouTubeVideo> videosList = null;

		if (!noMoreVideoPages()) {
			try {
				// set the page token/id to retrieve
				this.videosList.setPageToken(nextPageToken);

				// communicate with YouTube
				PlaylistItemListResponse searchResponse = this.videosList.execute();

				// get videos
				List<PlaylistItem> searchResultList = searchResponse.getItems();
				if (searchResultList != null) {
					videosList = getVideosList(searchResultList);
				}

				// set the next page token
				nextPageToken = searchResponse.getNextPageToken();

				// if nextPageToken is null, it means that there are no more videos
				if (nextPageToken == null)
					noMoreVideoPages = true;
			} catch (IOException ex) {
				Log.e(TAG, ex.getLocalizedMessage());
			}
		}

		return videosList;
	}


	/**
	 * YouTube's search functionality (i.e. {@link SearchResult} does not return enough information
	 * about the YouTube videos.
	 *
	 * <p>Hence, we need to submit the video IDs to YouTube to retrieve more information about the
	 * given video list.</p>
	 *
	 * @param searchResultList Search results
	 * @return List of {@link YouTubeVideo}s.
	 * @throws IOException
	 */
	private List<YouTubeVideo> getVideosList(List<PlaylistItem> searchResultList) throws IOException {
		StringBuilder videoIds = new StringBuilder();

		// append the video IDs into a strings (CSV)
		for (PlaylistItem res : searchResultList) {
			videoIds.append(res.getSnippet().getResourceId().getVideoId());
			videoIds.append(',');
		}

		// get video details by supplying the videos IDs
		GetVideosDetailsByIDs getVideo = new GetVideosDetailsByIDs();
		getVideo.init(videoIds.toString());

		return getVideo.getNextVideos();
	}


	@Override
	public boolean noMoreVideoPages() {
		return noMoreVideoPages;
	}

}
