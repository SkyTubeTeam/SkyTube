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

package free.rm.skytube.gui.businessobjects;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.common.collect.Lists;

import java.text.NumberFormat;
import java.util.List;

import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.YouTube.VideoBlocker;
import free.rm.skytube.databinding.BlockedVideoItemBinding;
import free.rm.skytube.gui.activities.PreferencesActivity;
import free.rm.skytube.gui.fragments.preferences.VideoBlockerPreferenceFragment;

/**
 * A dialog that displays a list of blocked videos.
 */
public class BlockedVideosDialog extends SkyTubeMaterialDialog {

	private final BlockedVideosDialogListener listener;


	public BlockedVideosDialog(@NonNull final Context context, final BlockedVideosDialogListener blockedVideosDialogListener, final List<VideoBlocker.BlockedVideo> blockedVideos) {
		super(context);

		if (blockedVideos.isEmpty()) {
			// if no videos have been blocked, then ask the user if they want to configure the
			// preferences of the video blocker...
			title(R.string.pref_video_blocker_category);
			content(R.string.no_videos_blocked);

			this.listener = null;
		} else {
			// display a list of blocked videos
			title(R.string.blocked_videos);
			adapter(new BlockedVideosAdapter(context, Lists.reverse(blockedVideos)), null);     // invert the list of blocked videos
			neutralText(R.string.clear);

			this.listener = blockedVideosDialogListener;

			onNeutral((dialog, which) -> {
				if (listener != null) {
					listener.onClearBlockedVideos();
				}
			});

		}

		positiveText(R.string.configure);
		onPositive((dialog, which) -> {
			// display the PreferenceActivity where the Videos Blocker tab is selected/opened
			// by default
			final Intent i = new Intent(context, PreferencesActivity.class);
			i.putExtra(PreferencesActivity.START_FRAGMENT, VideoBlockerPreferenceFragment.class.getName());
			context.startActivity(i);
		});
	}

	////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Adapter that displays a list of blocked videos.
	 */
	public static class BlockedVideosAdapter extends RecyclerView.Adapter<ViewHolder> {

		private final List<VideoBlocker.BlockedVideo> blockedVideos;
		private final int[] rowColors = new int[] {SkyTubeApp.getColorEx(R.color.dialog_row_0), SkyTubeApp.getColorEx(R.color.dialog_row_1)};
		private Context context;


		public BlockedVideosAdapter(Context context, final List<VideoBlocker.BlockedVideo> blockedVideos) {
			this.context = context;
			this.blockedVideos = blockedVideos;
		}

		@NonNull
		@Override
		public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			BlockedVideoItemBinding binding = BlockedVideoItemBinding.inflate(
					LayoutInflater.from(parent.getContext()), parent, false);
			return new ViewHolder(binding);
		}

		@Override
		public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
			final VideoBlocker.BlockedVideo blockedVideo = blockedVideos.get(position);

			// alternate the row colors
			holder.binding.blockedVideoRow.setBackgroundColor(rowColors[position % 2]);
			holder.binding.blockedVideoRow.setOnClickListener(v -> {
				// play the video
				YouTubePlayer.launch(blockedVideo.getVideo(), context);
			});

			// update view holder's data
			holder.binding.idTextView.setText(NumberFormat.getNumberInstance().format(getItemCount() - position)); // since the list of blocked videos is inverted, we need to get the original item position
			holder.binding.videoTitleTextView.setText(blockedVideo.getVideo().getTitle());
			holder.binding.filterTextView.setText(blockedVideo.getFilteringType().toString());
			holder.binding.reasonTextView.setText(blockedVideo.getReason());
		}

		@Override
		public int getItemCount() {
			return blockedVideos.size();
		}

	}


	////////////////////////////////////////////////////////////////////////////////////////////////

	private static class ViewHolder extends RecyclerView.ViewHolder {
		BlockedVideoItemBinding binding;

		ViewHolder(BlockedVideoItemBinding binding) {
			super(binding.getRoot());
			this.binding = binding;
		}
	}

	////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * {@link BlockedVideosDialog} listener.
	 */
	public interface BlockedVideosDialogListener {

		/**
		 * Called when the user wants to clear the history of blocked videos.
		 */
		void onClearBlockedVideos();

	}

}