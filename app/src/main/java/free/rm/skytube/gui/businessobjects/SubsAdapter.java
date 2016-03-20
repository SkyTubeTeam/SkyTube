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
import android.widget.TextView;

import free.rm.skytube.R;
import free.rm.skytube.businessobjects.YouTubeChannel;
import free.rm.skytube.businessobjects.db.GetSubscribedChannelsTask;
import free.rm.skytube.gui.activities.ChannelBrowserActivity;

/**
 * Channel subscriptions adapter.
 */
public class SubsAdapter extends BaseAdapterEx<YouTubeChannel> {

	private static SubsAdapter subsAdapter = null;
	private static final String TAG = SubsAdapter.class.getSimpleName();

	private SubsAdapter(Context context) {
		super(context);
		new GetSubscribedChannelsTask(this).execute();
	}


	public static SubsAdapter get(Context context) {
		if (subsAdapter == null) {
			subsAdapter = new SubsAdapter(context);
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

		return rowView;
	}


	/////////////////////

	private class SubChannelViewHolder {

		private InternetImageView	thumbnailImageView;
		private TextView			channelNameTextView;
		private YouTubeChannel		channel = null;

		public SubChannelViewHolder(View rowView) {
			thumbnailImageView  = (InternetImageView) rowView.findViewById(R.id.sub_channel_thumbnail_image_view);
			channelNameTextView = (TextView) rowView.findViewById(R.id.sub_channel_name_text_view);
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
			this.channel = channel;
		}

	}

}
