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
import com.google.api.services.youtube.model.PlaylistItem;
import com.google.api.services.youtube.model.PlaylistItemListResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import free.rm.skytube.businessobjects.Logger;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeAPI;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeAPIKey;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;

/**
 * Returns a list of videos for a specific playlist.
 */
public class GetPlaylistVideos extends GetYouTubeVideos {
	// The list of playlist videos returned by the YouTube Data API
	private YouTube.PlaylistItems.List playlistItemsList;

	// Number of videos to return each time
	private static final long MAX_RESULTS = 45L;

	@Override
	public void init() throws IOException {
		playlistItemsList = YouTubeAPI.create().playlistItems().list("contentDetails");
		playlistItemsList.setFields("items(contentDetails/videoId), nextPageToken");
		playlistItemsList.setKey(YouTubeAPIKey.get().getYouTubeAPIKey());
		playlistItemsList.setMaxResults(MAX_RESULTS);
		nextPageToken = null;
	}

	@Override
	public List<YouTubeVideo> getNextVideos() {
		List<YouTubeVideo> videoList = new ArrayList<>();

		if (!noMoreVideoPages()) {
			try {
				playlistItemsList.setPageToken(nextPageToken);

				PlaylistItemListResponse response = playlistItemsList.execute();

				StringBuilder videoIds = new StringBuilder();

				List<PlaylistItem> items = response.getItems();
				if(items != null) {
					for(PlaylistItem item : items) {
						videoIds.append(item.getContentDetails().getVideoId());
						videoIds.append(',');
					}
				}

				// get video details by supplying the videos IDs
				GetVideosDetailsByIDs getVideo = new GetVideosDetailsByIDs();
				getVideo.init(videoIds.toString());

				videoList = getVideo.getNextVideos();

				nextPageToken = response.getNextPageToken();

				if(nextPageToken == null)
					noMoreVideoPages = true;
			} catch (IOException e) {
				Logger.e(this, "Error has occurred while getting playlist's videos", e);
			}
		}

		return videoList;
	}

	/**
	 * Set the Playlist ID to search for
	 * @param query Playlist ID
	 */
	@Override
	public void setQuery(String query) {
		playlistItemsList.setPlaylistId(query);
	}

	@Override
	public boolean noMoreVideoPages() {
		return noMoreVideoPages;
	}
}
