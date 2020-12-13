/*
 * SkyTube
 * Copyright (C) 2017  Ramon Mifsud
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
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import free.rm.skytube.R;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubePlaylist;
import free.rm.skytube.databinding.VideoCellBinding;
import free.rm.skytube.gui.businessobjects.PlaylistClickListener;

/**
 * A ViewHolder for the playlists grid view.
 */
class PlaylistViewHolder extends RecyclerView.ViewHolder {
	private final VideoCellBinding binding;
	private final PlaylistClickListener playlistClickListener;

	PlaylistViewHolder(@NonNull VideoCellBinding binding,
					   @NonNull PlaylistClickListener playlistClickListener) {
		super(binding.getRoot());
		this.binding = binding;

		binding.thumbsUpTextView.setVisibility(View.GONE);
		binding.videoDurationTextView.setVisibility(View.GONE);
		binding.channelTextView.setVisibility(View.GONE);
		binding.optionsButton.setVisibility(View.GONE);

		this.playlistClickListener = playlistClickListener;
	}

	void setPlaylist(final YouTubePlaylist playlist) {
		Context context = itemView.getContext();
		Glide.with(context)
						.load(playlist.getThumbnailUrl())
						.apply(new RequestOptions().placeholder(R.drawable.thumbnail_default))
						.into(binding.thumbnailImageView);
		binding.titleTextView.setText(playlist.getTitle());
		binding.publishDateTextView.setText(playlist.getPublishDatePretty());
		binding.viewsTextView.setText(String.format(context.getString(R.string.num_videos), playlist.getVideoCount()));
		binding.thumbnailImageView.setOnClickListener(view -> playlistClickListener.onClickPlaylist(playlist));
	}
}
