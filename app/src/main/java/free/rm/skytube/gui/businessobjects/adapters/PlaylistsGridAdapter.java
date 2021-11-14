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

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.List;

import free.rm.skytube.R;
import free.rm.skytube.businessobjects.YouTube.GetChannelPlaylists;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubePlaylist;
import free.rm.skytube.businessobjects.YouTube.YouTubeTasks;
import free.rm.skytube.databinding.VideoCellBinding;
import free.rm.skytube.gui.businessobjects.PlaylistClickListener;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.functions.Consumer;

/**
 * An adapter that will display playlists in a {@link android.widget.GridView}.
 */
public class PlaylistsGridAdapter extends RecyclerViewAdapterEx<YouTubePlaylist, PlaylistViewHolder> {
	private static final String TAG = PlaylistsGridAdapter.class.getSimpleName();

	private GetChannelPlaylists getChannelPlaylists;
	private final PlaylistClickListener playlistClickListener;
	private final CompositeDisposable compositeDisposable = new CompositeDisposable();

	public PlaylistsGridAdapter(Context context, PlaylistClickListener playlistClickListener) {
		super(context);
		getChannelPlaylists = null;
		this.playlistClickListener = playlistClickListener;
	}

	@NonNull
	@Override
	public PlaylistViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		VideoCellBinding binding = VideoCellBinding.inflate(LayoutInflater.from(parent.getContext()),
				parent, false);
		return new PlaylistViewHolder(binding, playlistClickListener);
	}

	@Override
	public void onBindViewHolder(@NonNull PlaylistViewHolder viewHolder, int position) {
		viewHolder.setPlaylist(get(position));

		// if it reached the bottom of the list, then try to get the next page of videos
		if (position >= getItemCount() - 1) {
			Log.w(TAG, "BOTTOM REACHED!!!");
			if(getChannelPlaylists != null)
				compositeDisposable.add(YouTubeTasks.getChannelPlaylists(getContext(), getChannelPlaylists, this, false)
						.subscribe());
		}
	}

	public void clearBackgroundTasks() {
		compositeDisposable.clear();
	}

	public void setYouTubeChannel(YouTubeChannel youTubeChannel) {
		try {
			clearList();
			getChannelPlaylists = new GetChannelPlaylists();
			getChannelPlaylists.setYouTubeChannel(youTubeChannel);
			compositeDisposable.add(YouTubeTasks.getChannelPlaylists(getContext(), getChannelPlaylists, this, false)
					.subscribe());
		} catch (IOException e) {
			e.printStackTrace();
			Toast.makeText(getContext(),
							String.format(getContext().getString(R.string.could_not_get_videos), youTubeChannel.getTitle()),
							Toast.LENGTH_LONG).show();
		}
	}

	public void refresh(@NonNull Consumer<List<YouTubePlaylist>> onFinished) {
		if(getChannelPlaylists != null)
			compositeDisposable.add(YouTubeTasks.getChannelPlaylists(getContext(), getChannelPlaylists, this, false)
					.subscribe(onFinished));
	}
}
