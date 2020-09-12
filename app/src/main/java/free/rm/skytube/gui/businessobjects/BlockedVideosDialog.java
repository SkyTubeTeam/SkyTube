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
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.common.collect.Lists;

import java.text.NumberFormat;
import java.util.List;

import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.YouTube.VideoBlocker;
import free.rm.skytube.gui.activities.PreferencesActivity;
import free.rm.skytube.gui.fragments.preferences.VideoBlockerPreferenceFragment;

import static android.preference.PreferenceActivity.EXTRA_SHOW_FRAGMENT;

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
			i.putExtra(EXTRA_SHOW_FRAGMENT, VideoBlockerPreferenceFragment.class.getName());
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
			View itemLayoutView = LayoutInflater.from(parent.getContext()).inflate(
					R.layout.blocked_video_item, parent, false);
			return new ViewHolder(itemLayoutView);
		}


		@Override
		public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
			final VideoBlocker.BlockedVideo blockedVideo = blockedVideos.get(position);

			// alternate the row colors
			holder.row.setBackgroundColor(rowColors[position % 2]);
			holder.row.setOnClickListener(v -> {
				// play the video
				YouTubePlayer.launch(blockedVideo.getVideo(), context);
			});

			// update view holder's data
			holder.id.setText(NumberFormat.getNumberInstance().format(getItemCount() - position)); // since the list of blocked videos is inverted, we need to get the original item position
			holder.videoBlocked.setText(blockedVideo.getVideo().getTitle());
			holder.filter.setText(blockedVideo.getFilteringType().toString());
			holder.reason.setText(blockedVideo.getReason());
		}


		@Override
		public int getItemCount() {
			return blockedVideos.size();
		}

	}


	////////////////////////////////////////////////////////////////////////////////////////////////

	private static class ViewHolder extends RecyclerView.ViewHolder {
		View row;
		TextView    id;
		TextView    videoBlocked;
		TextView    filter;
		TextView    reason;

		ViewHolder(View itemView) {
			super(itemView);

			row = itemView.findViewById(R.id.blocked_video_row);
			id = itemView.findViewById(R.id.id_text_view);
			videoBlocked = itemView.findViewById(R.id.video_title_text_view);
			filter = itemView.findViewById(R.id.filter_text_view);
			reason = itemView.findViewById(R.id.reason_text_view);
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