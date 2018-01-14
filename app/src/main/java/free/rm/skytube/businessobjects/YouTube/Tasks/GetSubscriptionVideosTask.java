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

package free.rm.skytube.businessobjects.YouTube.Tasks;

import android.content.SharedPreferences;

import com.google.api.client.util.DateTime;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.AsyncTaskParallel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;
import free.rm.skytube.businessobjects.db.SubscriptionsDb;
import free.rm.skytube.gui.businessobjects.adapters.SubsAdapter;

/**
 * A task that returns the videos of channel the user has subscribed too.  Used to detect if new
 * videos have been published since last time the user used the app.
 */
public class GetSubscriptionVideosTask extends AsyncTaskParallel<Void, Void, Void> {
	private List<GetChannelVideosTask> tasks = new ArrayList<>();
	private GetSubscriptionVideosTaskListener listener;
	private int numTasksLeft = 0;
	private int numTasksFinished = 0;
	boolean forceRefresh = false;
	private List<YouTubeChannel> overriddenChannels;

	public GetSubscriptionVideosTask(GetSubscriptionVideosTaskListener listener) {
		this.listener = listener;
	}

	public GetSubscriptionVideosTask setForceRefresh(boolean forceRefresh) {
		this.forceRefresh = forceRefresh;
		return this;
	}

	public GetSubscriptionVideosTask setChannelsToRefresh(List<YouTubeChannel> channels) {
		overriddenChannels = channels;
		return this;
	}


	/**
	 * @return The last time we updated the subscriptions videos feed.  Will return null if the
	 * last refresh time is set to -1.
	 */
	private DateTime getFeedsLastUpdateTime() {
		long l = SkyTubeApp.getPreferenceManager().getLong(SkyTubeApp.KEY_SUBSCRIPTIONS_LAST_UPDATED, -1);
		return (l != -1)  ?  new DateTime(l)  :  null;
	}


	/**
	 * Update the feeds' last update time to the current time.
	 */
	private void updateFeedsLastUpdateTime() {
		updateFeedsLastUpdateTime(new DateTime(new Date()));
	}


	/**
	 * Update the feed's last update time to dateTime.
	 *
	 * @param dateTime  The feed's last update time.  If it is set to null, then -1 will be stored
	 *                  to indicate that the app needs to force refresh the feeds...
	 */
	public static void updateFeedsLastUpdateTime(DateTime dateTime) {
		SharedPreferences.Editor editor = SkyTubeApp.getPreferenceManager().edit();
		editor.putLong(SkyTubeApp.KEY_SUBSCRIPTIONS_LAST_UPDATED, dateTime != null  ?  dateTime.getValue()  :  -1);
		editor.commit();
	}


	@Override
	protected Void doInBackground(Void... voids) {
		List<YouTubeChannel> channels = overriddenChannels != null ? overriddenChannels : SubsAdapter.get(null).getSubsLists();

		/*
		 * Get the last time all subscriptions were updated, and only fetch videos that were published after this.
		 * Any new channels that have been subscribed to since the last time this refresh was done will have any
		 * videos published after the last published time stored in the database, so we don't need to worry about missing
		 * any.
		 */

		// If forceRefresh is set to true, then set publishedAfter to null... this will force
		// the app to update the feeds.  Otherwise, set the publishedAfter date to the last
		// time we updated the feeds.
		DateTime publishedAfter = forceRefresh ? null : getFeedsLastUpdateTime();

		for(final YouTubeChannel channel : channels) {
			tasks.add(new GetChannelVideosTask(channel)
				.setPublishedAfter(publishedAfter)
 				.setGetChannelVideosTaskInterface(new GetChannelVideosTaskInterface() {
					@Override
					public void onGetVideos(List<YouTubeVideo> videos) {
						numTasksFinished++;
						boolean videosDeleted = false;
						if(numTasksFinished < numTasksLeft) {
							if(tasks.size() > 0) {
								// More channels to fetch videos from
								tasks.get(0).executeInParallel();
								tasks.remove(0);
							}
							if(listener != null)
								listener.onChannelVideosFetched(channel, videos != null ? videos : new ArrayList<YouTubeVideo>(), videosDeleted);
						} else {
							videosDeleted = SubscriptionsDb.getSubscriptionsDb().trimSubscriptionVideos();

							// All channels have finished querying. Update the last time this refresh was done.
							updateFeedsLastUpdateTime();

							if (listener != null) {
								listener.onChannelVideosFetched(channel, videos != null ? videos : new ArrayList<YouTubeVideo>(), videosDeleted);
								listener.onAllChannelVideosFetched();
							}
						}
					}
				})
			);
		}

		numTasksLeft = tasks.size();

		int numToStart = tasks.size() >= 4 ? 4 : tasks.size();

		// Start fetching videos for up to 4 channels simultaneously.
		for(int i=0;i<numToStart;i++) {
			tasks.get(0).executeInParallel();
			tasks.remove(0);
		}

		return null;
	}




}
