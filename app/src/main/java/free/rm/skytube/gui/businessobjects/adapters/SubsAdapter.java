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
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.Iterator;
import java.util.List;

import free.rm.skytube.R;
import free.rm.skytube.businessobjects.Logger;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.db.Tasks.GetSubscribedChannelsTask;
import free.rm.skytube.gui.businessobjects.MainActivityListener;

/**
 * Channel subscriptions adapter: Contains a list of channels (that the user subscribed to) together
 * with a notification whether the channel has new videos since last visit to the channel or not.
 */
public class SubsAdapter extends RecyclerViewAdapterEx<YouTubeChannel, SubsAdapter.SubChannelViewHolder> {

	private static final String TAG = SubsAdapter.class.getSimpleName();
	private static SubsAdapter subsAdapter = null;
	/**
	 * Set to true if the users' subscriptions channels list has been fully retrieved and populated
	 * by querying the local database and YouTube servers...
	 */
	private final Bool isSubsListRetrieved = new Bool(false);
	private MainActivityListener listener;


	private SubsAdapter(Context context, View progressBar) {
		super(context);

		// populate this adapter with user's subscribed channels
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


	public void setListener(MainActivityListener listener) {
		this.listener = listener;
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
	private void removeChannel(String channelId) {
		int size = getItemCount();

		for (int i = 0; i < size; i++) {
			if (get(i).getId().equalsIgnoreCase(channelId)) {
				remove(i);
				return;
			}
		}

		Log.e(TAG, "Channel not removed from adapter:  id=" + channelId);
	}


	/**
	 * Changes the channel's 'new videos' status.  The channel's view is then refreshed.
	 *
	 * @param channelId Channel ID.
	 * @param newVideos 'New videos' status (true = new videos have been added since user's last
	 *                  visit;  false = no new videos)
	 * @return True if the operations have been successful; false otherwise.
	 */
	public boolean changeChannelNewVideosStatus(String channelId, boolean newVideos) {
		YouTubeChannel channel;
		int position = 0;

		for (Iterator<YouTubeChannel> i = getIterator(); i.hasNext(); position++) {
			channel = i.next();

			if (channel.getId() != null && channel.getId().equals(channelId)) {
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
		notifyItemChanged(viewPosition);
	}


	@Override
	public SubChannelViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.sub_channel, parent, false);
		return new SubChannelViewHolder(v);
	}


	@Override
	public void onBindViewHolder(SubChannelViewHolder viewHolder, int position) {
		viewHolder.updateInfo(get(position));
	}


	public void refreshSubsList() {
		synchronized (isSubsListRetrieved) {
			isSubsListRetrieved.value = false;
		}
		clearList();
		new GetSubscribedChannelsTask(this, null).executeInParallel();
	}


	/**
	 * @return True if the subscriptions channel list has been fully retrieved and populated.
	 */
	private Bool isSubsListRetrieved() {
		synchronized (isSubsListRetrieved) {
			return isSubsListRetrieved;
		}
	}


	/**
	 * Returns the list of channels that the user is subscribed to.
	 * <p>
	 * <p>If currently the Subs List is being retrieved by the {@link SubsAdapter} then wait until the
	 * {@link SubsAdapter} retrieves the list.</p>
	 *
	 * @return List of YouTube channels the user is subscribed to.
	 */
	public List<YouTubeChannel> getSubsLists() {
		SubsAdapter.Bool isSubsListRetrieved = isSubsListRetrieved();

		synchronized (isSubsListRetrieved) {
			// if the SubsAdapter is still retrieving the channels...
			if (!isSubsListRetrieved.value) {
				try {
					// ...then we have to wait...
					isSubsListRetrieved.wait();
				} catch (InterruptedException e) {
					Logger.e(this, "Something went wrong when waiting for the Subs Lists...", e);
				}
			}
		}

		// the list has now been retrieved; return it pls
		return getList();
	}


	/**
	 * Method used to notify {@link SubsAdapter} that the subscriptions channels list has been
	 * fully retrieved and populated.
	 */
	public void subsListRetrieved() {
		synchronized (isSubsListRetrieved) {
			isSubsListRetrieved.value = true;
			isSubsListRetrieved.notify();
		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Boolean class that is mutable.
	 * <p>
	 * Required in order to perform Bool.wait() and Bool.notify().
	 */
	public static class Bool {
		public boolean value = false;

		public Bool(boolean value) {
			this.value = value;
		}
	}

	class SubChannelViewHolder extends RecyclerView.ViewHolder {

		private ImageView thumbnailImageView;
		private TextView channelNameTextView;
		private View newVideosNotificationView;
		private YouTubeChannel channel = null;

		SubChannelViewHolder(View rowView) {
			super(rowView);
			thumbnailImageView = rowView.findViewById(R.id.sub_channel_thumbnail_image_view);
			channelNameTextView = rowView.findViewById(R.id.sub_channel_name_text_view);
			newVideosNotificationView = rowView.findViewById(R.id.sub_channel_new_videos_notification);
			channel = null;

			rowView.setOnClickListener(v -> {
				if (listener instanceof MainActivityListener)
					listener.onChannelClick(channel);
			});
		}

		void updateInfo(YouTubeChannel channel) {
			Glide.with(getContext().getApplicationContext())
					.load(channel.getThumbnailNormalUrl())
					.apply(new RequestOptions().placeholder(R.drawable.channel_thumbnail_default))
					.into(thumbnailImageView);

			channelNameTextView.setText(channel.getTitle());
			newVideosNotificationView.setVisibility(channel.newVideosSinceLastVisit() ? View.VISIBLE : View.INVISIBLE);
			this.channel = channel;
		}

	}

}
