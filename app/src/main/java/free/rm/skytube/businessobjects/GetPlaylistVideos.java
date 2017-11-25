package free.rm.skytube.businessobjects;

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.PlaylistItem;
import com.google.api.services.youtube.model.PlaylistItemListResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import free.rm.skytube.gui.businessobjects.Logger;

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
