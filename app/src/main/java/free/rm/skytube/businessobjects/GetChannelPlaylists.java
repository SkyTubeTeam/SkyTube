package free.rm.skytube.businessobjects;

import android.util.Log;

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Playlist;
import com.google.api.services.youtube.model.PlaylistListResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * Returns a list of YouTube playlists for a specific channel.
 *
 * <p>Do not run this directly, but rather use {@link GetChannelPlaylistsTask}.</p>
 */
public class GetChannelPlaylists {
	protected YouTube.Playlists.List playlistList = null;
	protected static final Long	MAX_RESULTS = 45L;

	protected String nextPageToken = null;
	protected boolean noMorePlaylistPages = false;

	private YouTubeChannel channel;

	private static final String	TAG = GetChannelPlaylists.class.getSimpleName();

	public void init() throws IOException {
		playlistList = YouTubeAPI.create().playlists().list("id, snippet, contentDetails");
		playlistList.setKey(YouTubeAPIKey.get().getYouTubeAPIKey());
		playlistList.setFields("items(id, snippet/title, snippet/description, snippet/thumbnails, snippet/publishedAt, contentDetails/itemCount)," +
						"nextPageToken");
		playlistList.setMaxResults(MAX_RESULTS);
		nextPageToken = null;
	}

	public void setYouTubeChannel(YouTubeChannel channel) {
		this.channel = channel;
		if (playlistList != null)
			playlistList.setChannelId(channel.getId());
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
				Log.e(TAG, ex.getLocalizedMessage());
			}
		}

		return toYouTubePlaylistList(playlistList);
	}

	public boolean noMorePlaylistPages() {
		return noMorePlaylistPages;
	}

	private List<YouTubePlaylist> toYouTubePlaylistList(List<Playlist> playlistList) {
		List<YouTubePlaylist> youTubePlaylists = new ArrayList<>();

		if(playlistList != null) {
			YouTubePlaylist youTubePlaylist;

			for (Playlist playlist : playlistList) {
				youTubePlaylist = new YouTubePlaylist(playlist, channel);
				youTubePlaylists.add(youTubePlaylist);
			}
		}
		return youTubePlaylists;
	}

	/**
	 * Reset the fetching of playlists. This will be called when a swipe to refresh is done.
	 */
	public void reset() {
		nextPageToken = null;
		noMorePlaylistPages = false;
	}
}
