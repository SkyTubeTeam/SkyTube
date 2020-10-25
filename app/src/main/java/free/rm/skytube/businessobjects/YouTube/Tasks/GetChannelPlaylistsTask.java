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

package free.rm.skytube.businessobjects.YouTube.Tasks;

import android.util.Log;

import java.util.List;

import free.rm.skytube.businessobjects.AsyncTaskParallel;
import free.rm.skytube.businessobjects.YouTube.GetChannelPlaylists;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubePlaylist;
import free.rm.skytube.gui.businessobjects.adapters.PlaylistsGridAdapter;

/**
 * An asynchronous task that will retrieve YouTube playlists for a specific channel and displays them in the supplied Adapter.
 */
public class GetChannelPlaylistsTask extends AsyncTaskParallel<Void, Void, List<YouTubePlaylist>> {
	// Used to retrieve the playlists
	private final GetChannelPlaylists getChannelPlaylists;

	// The adapter where the playlists will be displayed
	private PlaylistsGridAdapter playlistsGridAdapter;

	public GetChannelPlaylistsTask(GetChannelPlaylists getChannelPlaylists, PlaylistsGridAdapter playlistsGridAdapter) {
		this.playlistsGridAdapter = playlistsGridAdapter;
		this.getChannelPlaylists = getChannelPlaylists;
	}

	public GetChannelPlaylistsTask(GetChannelPlaylists getChannelPlaylists, PlaylistsGridAdapter playlistsGridAdapter, Runnable onFinished) {
		setFinishCallback(onFinished);
		this.playlistsGridAdapter = playlistsGridAdapter;
		this.getChannelPlaylists = getChannelPlaylists;
		getChannelPlaylists.reset();
		playlistsGridAdapter.clearList();
	}

	@Override
	protected List<YouTubePlaylist> doInBackground(Void... voids) {
		List<YouTubePlaylist> playlists = null;

		try {
			if (!isCancelled()) {
				playlists = getChannelPlaylists.getNextPlaylists();
			}
		} catch (Exception e) {
			Log.e(GetChannelPlaylistsTask.class.getSimpleName(), "Error:" + e.getLocalizedMessage(), e);
			lastException = e;
		}
		return playlists;
	}

	@Override
	protected void onPostExecute(List<YouTubePlaylist> youTubePlaylists) {
		if(youTubePlaylists != null) {
			playlistsGridAdapter.appendList(youTubePlaylists);
		}
		super.onPostExecute(youTubePlaylists);
	}
}
