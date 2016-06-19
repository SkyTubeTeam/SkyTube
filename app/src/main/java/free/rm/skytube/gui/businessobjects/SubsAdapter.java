/*
 * SkyTube
 * Copyright (C) 2016  Ramon Mifsud
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
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Iterator;

import free.rm.skytube.R;
import free.rm.skytube.businessobjects.YouTubeChannel;
import free.rm.skytube.businessobjects.db.GetSubscribedChannelsTask;
import free.rm.skytube.gui.activities.ChannelBrowserActivity;

/**
 * Channel subscriptions adapter.
 */
public class SubsAdapter extends BaseAdapterEx<YouTubeChannel> {

	private ViewGroup listView = null;
	private static SubsAdapter subsAdapter = null;
	private static final String TAG = SubsAdapter.class.getSimpleName();

	private SubsAdapter(Context context, View progressBar) {
		super(context);
		new GetSubscribedChannelsTask(this, progressBar).executeInParallel();
	}


	public static SubsAdapter get(Context context) {
		return get(context, null);
	}


	public static SubsAdapter get(Context context, View progressBar) {
		if (subsAdapter == null) {
			subsAdapter = new SubsAdapter(context, progressBar);
		}

		return subsAdapter;
	}


	/**
	 * Append channel to this adapter.
	 *
	 * @param channel Channel to append.
	 */
	public void appendChannel(YouTubeChannel channel) {
		append(channel);
	}


	/**
	 * Remove channel from this adapter.
	 *
	 * @param channel Channel to remove.
	 */
	public void removeChannel(YouTubeChannel channel) {
		removeChannel(channel.getId());
	}


	/**
	 * Remove channel from this adapter.
	 *
	 * @param channelId Channel to remove.
	 */
	public void removeChannel(String channelId) {
		int size = getCount();

		for (int i = 0;  i < size;  i++) {
			if (get(i).getId().equalsIgnoreCase(channelId)) {
				remove(i);
				return;
			}
		}

		Log.e(TAG, "Channel not removed from adapter:  id="+channelId);
	}



	/**
	 * Changes the channel's 'new videos' status.  The channel's view is then refreshed.
	 *
	 * @param channelId	Channel ID.
	 * @param newVideos	'New videos' status (true = new videos have been added since user's last
	 *                  visit;  false = no new videos)
	 * @return	True if the operations have been successful; false otherwise.
	 */
	public boolean changeChannelNewVideosStatus(String channelId, boolean newVideos) {
		YouTubeChannel channel;
		int position = 0;

		for (Iterator<YouTubeChannel> i = getIterator();  i.hasNext(); position++) {
			channel = i.next();

			if (channel.getId().equals(channelId)) {
				// change the 'new videos' status
				channel.setNewVideosSinceLastVisit(newVideos);
				// we now need to notify the SubsAdapter to remove the new videos notification (near the channel name)
				updateView(position);
				return true;
			}
		}

		return false;
	}



	/**
	 * Update the contents of a view (i.e. refreshes the given view).
	 *
	 * @param viewPosition The position of the view that we want to update.
	 */
	private void updateView(int viewPosition) {
		int visiblePosition = ((ListView) this.listView).getFirstVisiblePosition();
		View view = listView.getChildAt(viewPosition - visiblePosition);
		getView(viewPosition, view, listView);
	}



	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View rowView;
		SubChannelViewHolder viewHolder;

		if (convertView == null) {
			rowView = getLayoutInflater().inflate(R.layout.sub_channel, parent, false);
			viewHolder = new SubChannelViewHolder(rowView);
			rowView.setTag(viewHolder);
		} else {
			rowView = convertView;
			viewHolder = (SubChannelViewHolder) rowView.getTag();
		}

		if (viewHolder != null) {
			viewHolder.updateInfo(get(position));
		}

		this.listView = parent;
		return rowView;
	}


	/////////////////////

	private class SubChannelViewHolder {

		private InternetImageView	thumbnailImageView;
		private TextView			channelNameTextView;
		private View				newVideosNotificationView;
		private YouTubeChannel		channel = null;

		public SubChannelViewHolder(View rowView) {
			thumbnailImageView  = (InternetImageView) rowView.findViewById(R.id.sub_channel_thumbnail_image_view);
			channelNameTextView = (TextView) rowView.findViewById(R.id.sub_channel_name_text_view);
			newVideosNotificationView = rowView.findViewById(R.id.sub_channel_new_videos_notification);
			channel = null;

			rowView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent i = new Intent(getContext(), ChannelBrowserActivity.class);
					i.putExtra(ChannelBrowserActivity.CHANNEL_OBJ, channel);
					getContext().startActivity(i);
				}
			});
		}

		public void updateInfo(YouTubeChannel channel) {
			thumbnailImageView.setImageAsync(channel.getThumbnailNormalUrl());
			channelNameTextView.setText(channel.getTitle());
			newVideosNotificationView.setVisibility(channel.newVideosSinceLastVisit() ? View.VISIBLE : View.INVISIBLE);
			this.channel = channel;
		}

	}

}
