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

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.List;

import free.rm.skytube.R;
import free.rm.skytube.gui.businessobjects.ImportSubscriptionsChannel;

/**
 * Subclass of RecyclerView.Adapter to list the channels that have been imported from a YouTube XML Export.
 */
public class ImportSubscriptionsAdapter extends RecyclerView.Adapter<ImportSubscriptionsAdapter.ViewHolder> {
	private List<ImportSubscriptionsChannel> channels;

	public ImportSubscriptionsAdapter(List<ImportSubscriptionsChannel> channels) {
		this.channels = channels;
	}

	@Override
	public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		View itemLayoutView = LayoutInflater.from(parent.getContext()).inflate(
						R.layout.subs_youtube_import_channel, null);
		ViewHolder viewHolder = new ViewHolder(itemLayoutView);
		return viewHolder;
	}

	@Override
	public void onBindViewHolder(ViewHolder holder, int position) {
		final int pos = position;
		ImportSubscriptionsChannel channel = channels.get(position);
		holder.channelName.setText(channel.channelName);
		holder.checkBox.setChecked(channel.isChecked);
		holder.checkBox.setTag(channel);
		holder.checkBox.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				CheckBox cb = (CheckBox)view;
				ImportSubscriptionsChannel ch = (ImportSubscriptionsChannel)cb.getTag();
				ch.isChecked = cb.isChecked();
				channels.get(pos).isChecked = cb.isChecked();
			}
		});
	}

	@Override
	public int getItemCount() {
		return channels.size();
	}

	public static class ViewHolder extends RecyclerView.ViewHolder {
		public TextView channelName;
		public CheckBox checkBox;

		public ViewHolder(View itemView) {
			super(itemView);

			channelName = itemView.findViewById(R.id.channel_name);
			checkBox = itemView.findViewById(R.id.check_box);

		}
	}

	public List<ImportSubscriptionsChannel> getChannels() {
		return channels;
	}

	public void selectAll() {
		for(ImportSubscriptionsChannel channel : channels) {
			channel.isChecked = true;
		}
		notifyDataSetChanged();
	}

	public void selectNone() {
		for(ImportSubscriptionsChannel channel : channels) {
			channel.isChecked = false;
		}
		notifyDataSetChanged();
	}
}
