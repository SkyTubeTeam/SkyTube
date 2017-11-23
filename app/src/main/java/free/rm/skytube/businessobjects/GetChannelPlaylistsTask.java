package free.rm.skytube.businessobjects;

import java.util.List;

import free.rm.skytube.gui.businessobjects.PlaylistsGridAdapter;

/**
 * An asynchronous task that will retrieve YouTube playlists for a specific channel and displays them in the supplied Adapter.
 */
public class GetChannelPlaylistsTask extends AsyncTaskParallel<Void, Void, List<YouTubePlaylist>> {
	// Used to retrieve the playlists
	private GetChannelPlaylists getChannelPlaylists = new GetChannelPlaylists();

	// The adapter where the playlists will be displayed
	private PlaylistsGridAdapter playlistsGridAdapter;

	// Runnable to run after playlists are retrieved
	private Runnable onFinished;

	public GetChannelPlaylistsTask(GetChannelPlaylists getChannelPlaylists, PlaylistsGridAdapter playlistsGridAdapter) {
		this.playlistsGridAdapter = playlistsGridAdapter;
		this.getChannelPlaylists = getChannelPlaylists;
	}

	public GetChannelPlaylistsTask(GetChannelPlaylists getChannelPlaylists, PlaylistsGridAdapter playlistsGridAdapter, Runnable onFinished) {
			this.playlistsGridAdapter = playlistsGridAdapter;
			this.getChannelPlaylists = getChannelPlaylists;
			this.onFinished = onFinished;
			getChannelPlaylists.reset();
			playlistsGridAdapter.clearList();
	}

	@Override
	protected List<YouTubePlaylist> doInBackground(Void... voids) {
		List<YouTubePlaylist> playlists = null;

		if (!isCancelled()) {
			playlists = getChannelPlaylists.getNextPlaylists();
		}

		return playlists;
	}

	@Override
	protected void onPostExecute(List<YouTubePlaylist> youTubePlaylists) {
		playlistsGridAdapter.appendList(youTubePlaylists);
		if(onFinished != null)
			onFinished.run();
	}
}
