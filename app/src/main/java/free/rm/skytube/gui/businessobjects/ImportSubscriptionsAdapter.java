package free.rm.skytube.gui.businessobjects;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.List;

import free.rm.skytube.R;

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
