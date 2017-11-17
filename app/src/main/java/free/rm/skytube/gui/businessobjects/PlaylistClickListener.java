package free.rm.skytube.gui.businessobjects;

import free.rm.skytube.businessobjects.YouTubePlaylist;

/**
 * Interface for an object that will respond to a Playlist being clicked on
 */
public interface PlaylistClickListener {
	void onClickPlaylist(YouTubePlaylist playlist);
}
