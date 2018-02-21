package free.rm.skytube.businessobjects;

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Playlist;
import com.google.api.services.youtube.model.PlaylistListResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import free.rm.skytube.businessobjects.YouTube.GetChannelsDetails;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeAPI;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeAPIKey;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubePlaylist;

/**
 * Returns a YouTubePlaylist for the specified Playlist URL.
 *
 * <p>Do not run this directly, but rather use {@link GetPlaylistTask}.</p>
 */
public class GetPlaylist {
	protected YouTube.Playlists.List playlistList = null;

	protected static final Long	MAX_RESULTS = 45L;

	protected String nextPageToken = null;
	protected boolean noMorePlaylistPages = false;

	public void init(String playlistId) throws IOException {
		playlistList = YouTubeAPI.create().playlists().list("id, snippet, contentDetails");
		playlistList.setKey(YouTubeAPIKey.get().getYouTubeAPIKey());
		playlistList.setFields("items(id, snippet/title, snippet/description, snippet/thumbnails, snippet/publishedAt, contentDetails/itemCount, snippet/channelId)," +
						"nextPageToken");
		playlistList.setMaxResults(MAX_RESULTS);
		playlistList.setId(playlistId);

		nextPageToken = null;
	}

	public List<YouTubePlaylist> getNextPlaylists() {
		List<Playlist> playlistList = null;

		if (!noMorePlaylistPages()) {
			try {
				// set the page token/id to retrieve
				this.playlistList.setPageToken(nextPageToken);

				// communicate with YouTube
				PlaylistListResponse listResponse = this.playlistList.execute();

				// get playlists
				playlistList = listResponse.getItems();

				// set the next page token
				nextPageToken = listResponse.getNextPageToken();

				// if nextPageToken is null, it means that there are no more videos
				if (nextPageToken == null)
					noMorePlaylistPages = true;
			} catch (IOException ex) {
				Logger.d(this, ex.getLocalizedMessage());
			}
		}

		return toYouTubePlaylist(playlistList);
	}

	public boolean noMorePlaylistPages() {
		return noMorePlaylistPages;
	}

	protected List<YouTubePlaylist> toYouTubePlaylist(List<Playlist> playlistList) {
		final List<YouTubePlaylist> youTubePlaylists = new ArrayList<>();

		if(playlistList != null) {
			for (final Playlist playlist : playlistList) {
				final YouTubePlaylist youTubePlaylist = new YouTubePlaylist(playlist);
				// YouTubePlaylist object is now available, but we need to set its channel. We only have the channel ID, so init a new
				// YouTubeChannel object with the id, and set the playlist's channel
				YouTubeChannel channel;
				try {
					channel = new GetChannelsDetails().getYouTubeChannel(youTubePlaylist.getChannelId());
					if (channel != null) {
						youTubePlaylist.setChannel(channel);
					}
				} catch (IOException e) {
					Logger.d(this, "Unable to get channel info: %s", youTubePlaylist.getChannelId());
				}


				youTubePlaylists.add(youTubePlaylist);
			}
		}
		return youTubePlaylists;
	}
}
