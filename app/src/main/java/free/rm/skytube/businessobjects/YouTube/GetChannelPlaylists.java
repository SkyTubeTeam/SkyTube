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
import com.google.api.services.youtube.model.Playlist;
import com.google.api.services.youtube.model.PlaylistListResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeAPI;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeAPIKey;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubePlaylist;
import free.rm.skytube.gui.businessobjects.adapters.PlaylistsGridAdapter;


/**
 * Returns a list of YouTube playlists for a specific channel.
 *
 * <p>Do not run this directly, but rather use
 * {@link YouTubeTasks#getChannelPlaylists(GetChannelPlaylists, PlaylistsGridAdapter, boolean)}.</p>
 */
public class GetChannelPlaylists {
	protected YouTube.Playlists.List playlistList = null;
	protected static final Long	MAX_RESULTS = 45L;

	protected String nextPageToken = null;
	protected boolean noMorePlaylistPages = false;

	private YouTubeChannel channel;

	public GetChannelPlaylists() throws IOException {
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

	public List<YouTubePlaylist> getNextPlaylists() throws IOException {

		SkyTubeApp.nonUiThread();

		if (!noMorePlaylistPages()) {
			List<Playlist> playlistList = null;
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
			return toYouTubePlaylistList(playlistList);
		}
		return Collections.emptyList();

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
		playlistList.setKey(YouTubeAPIKey.get().getYouTubeAPIKey());
	}
}
