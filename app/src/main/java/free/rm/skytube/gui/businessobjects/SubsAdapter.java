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
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import free.rm.skytube.R;
import free.rm.skytube.businessobjects.YouTubeChannel;
import free.rm.skytube.businessobjects.db.GetSubscribedChannelsTask;

/**
 *
 */
public class SubsAdapter extends BaseAdapterEx<YouTubeChannel> {

	public SubsAdapter(Context context) {
		super(context);
		new GetSubscribedChannelsTask(this).execute();
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

		public SubChannelViewHolder(View rowView) {
			thumbnailImageView  = (InternetImageView) rowView.findViewById(R.id.sub_channel_thumbnail_image_view);
			channelNameTextView = (TextView) rowView.findViewById(R.id.sub_channel_name_text_view);
		}

		public void updateInfo(YouTubeChannel channel) {
			thumbnailImageView.setImageAsync(channel.getThumbnailNormalUrl());
			channelNameTextView.setText(channel.getTitle());
		}

	}

}
