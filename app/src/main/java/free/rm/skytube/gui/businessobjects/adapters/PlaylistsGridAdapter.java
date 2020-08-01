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

package free.rm.skytube.gui.businessobjects.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.io.IOException;

import free.rm.skytube.R;
import free.rm.skytube.businessobjects.YouTube.GetChannelPlaylists;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubePlaylist;
import free.rm.skytube.businessobjects.YouTube.Tasks.GetChannelPlaylistsTask;
import free.rm.skytube.gui.businessobjects.PlaylistClickListener;

/**
 * An adapter that will display playlists in a {@link android.widget.GridView}.
 */
public class PlaylistsGridAdapter extends RecyclerViewAdapterEx<YouTubePlaylist, PlaylistViewHolder> {
	private GetChannelPlaylists getChannelPlaylists;
	private static final String TAG = PlaylistsGridAdapter.class.getSimpleName();
	private PlaylistClickListener playlistClickListener;

	public PlaylistsGridAdapter(Context context, PlaylistClickListener playlistClickListener) {
		super(context);
		getChannelPlaylists = null;
		this.playlistClickListener = playlistClickListener;
	}

	@Override
	public PlaylistViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.video_cell, parent, false);
		return new PlaylistViewHolder(v, playlistClickListener);
	}

	@Override
	public void onBindViewHolder(PlaylistViewHolder viewHolder, int position) {
		if (viewHolder != null) {
			viewHolder.setPlaylist(get(position));
		}

		// if it reached the bottom of the list, then try to get the next page of videos
		if (position >= getItemCount() - 1) {
			Log.w(TAG, "BOTTOM REACHED!!!");
			if(getChannelPlaylists != null)
				new GetChannelPlaylistsTask(getChannelPlaylists, this).executeInParallel();
		}
	}

	public void setYouTubeChannel(YouTubeChannel youTubeChannel) {
		try {
			clearList();
			getChannelPlaylists = new GetChannelPlaylists();
			getChannelPlaylists.init();
			getChannelPlaylists.setYouTubeChannel(youTubeChannel);
			new GetChannelPlaylistsTask(getChannelPlaylists, this).executeInParallel();
		} catch (IOException e) {
			e.printStackTrace();
			Toast.makeText(getContext(),
							String.format(getContext().getString(R.string.could_not_get_videos), youTubeChannel.getTitle()),
							Toast.LENGTH_LONG).show();
		}
	}

	public void refresh(Runnable onFinished) {
		if(getChannelPlaylists != null)
			new GetChannelPlaylistsTask(getChannelPlaylists, this, onFinished).executeInParallel();
	}
}
