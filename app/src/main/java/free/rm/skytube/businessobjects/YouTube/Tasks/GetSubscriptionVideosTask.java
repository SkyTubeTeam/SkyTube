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

import java.util.ArrayList;
import java.util.List;

import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.AsyncTaskParallel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
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

	public GetSubscriptionVideosTask(GetSubscriptionVideosTaskListener listener) {
		this.listener = listener;
	}

	/**
	 * @return The last time we updated the subscriptions videos feed.  Will return null if the
	 * last refresh time is set to -1.
	 */
	private Long getFeedsLastUpdateTime() {
		long l = SkyTubeApp.getPreferenceManager().getLong(SkyTubeApp.KEY_SUBSCRIPTIONS_LAST_UPDATED, -1);
		return (l != -1)  ?  l  :  null;
	}


	/**
	 * Update the feeds' last update time to the current time.
	 */
	private void updateFeedsLastUpdateTime() {
		updateFeedsLastUpdateTime(System.currentTimeMillis());
	}


	/**
	 * Update the feed's last update time to dateTime.
	 *
	 * @param dateTimeInMs  The feed's last update time.  If it is set to null, then -1 will be stored
	 *                  to indicate that the app needs to force refresh the feeds...
	 */
	public static void updateFeedsLastUpdateTime(Long dateTimeInMs) {
		SharedPreferences.Editor editor = SkyTubeApp.getPreferenceManager().edit();
		editor.putLong(SkyTubeApp.KEY_SUBSCRIPTIONS_LAST_UPDATED, dateTimeInMs != null  ?  dateTimeInMs  :  -1);
		editor.commit();
	}


	@Override
	protected Void doInBackground(Void... voids) {
		List<YouTubeChannel> channels = SubsAdapter.get(null).getSubsLists();

		/*
		 * Get the last time all subscriptions were updated, and only fetch videos that were published after this.
		 * Any new channels that have been subscribed to since the last time this refresh was done will have any
		 * videos published after the last published time stored in the database, so we don't need to worry about missing
		 * any.
		 */

		// If forceRefresh is set to true, then set publishedAfter to null... this will force
		// the app to update the feeds.  Otherwise, set the publishedAfter date to the last
		// time we updated the feeds.
		Long publishedAfter = forceRefresh ? null : getFeedsLastUpdateTime();

		for(final YouTubeChannel channel : channels) {
			tasks.add(new GetChannelVideosTask(channel)
				.setPublishedAfter(publishedAfter)
 				.setGetChannelVideosTaskInterface(videos -> {
					 numTasksFinished++;
					 boolean videosDeleted = false;
					 int numberOfVideos = videos != null ? videos.size() : 0;
					 if(numTasksFinished < numTasksLeft) {
						 if(tasks.size() > 0) {
							 // More channels to fetch videos from
							 tasks.get(0).executeInParallel();
							 tasks.remove(0);
						 }
						 if(listener != null)
							 listener.onChannelVideosFetched(channel, numberOfVideos, videosDeleted);
					 } else {
						 videosDeleted = SubscriptionsDb.getSubscriptionsDb().trimSubscriptionVideos();

						 // All channels have finished querying. Update the last time this refresh was done.
						 updateFeedsLastUpdateTime();

						 if (listener != null) {
							 listener.onChannelVideosFetched(channel, numberOfVideos, videosDeleted);
							 listener.onAllChannelVideosFetched();
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
