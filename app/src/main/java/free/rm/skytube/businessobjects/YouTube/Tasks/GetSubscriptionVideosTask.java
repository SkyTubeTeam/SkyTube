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

import android.util.Log;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.AsyncTaskParallel;
import free.rm.skytube.businessobjects.db.SubscriptionsDb;

/**
 * A task that returns the videos of channel the user has subscribed too.  Used to detect if new
 * videos have been published since last time the user used the app.
 */
public class GetSubscriptionVideosTask extends AsyncTaskParallel<Void, Void, Boolean> {
	private GetSubscriptionVideosTaskListener listener;
	private List<String> channelIds;
	public GetSubscriptionVideosTask(GetSubscriptionVideosTaskListener listener, List<String> channelIds) {
		this.listener = listener;
		this.channelIds = channelIds;
	}



	@Override
	protected Boolean doInBackground(Void... voids) {
		if (channelIds == null) {
			channelIds = SubscriptionsDb.getSubscriptionsDb().getSubscribedChannelIds();
		}
		/*
		 * Get the last time all subscriptions were updated, and only fetch videos that were published after this.
		 * Any new channels that have been subscribed to since the last time this refresh was done will have any
		 * videos published after the last published time stored in the database, so we don't need to worry about missing
		 * any.
		 */

		Long publishedAfter =  SkyTubeApp.getSettings().getFeedsLastUpdateTime();
		AtomicBoolean changed = new AtomicBoolean(false);
		CountDownLatch countDown = new CountDownLatch(channelIds.size());

		Semaphore semaphore = new Semaphore(4);


		for(final String channelId: channelIds) {
			try {
				semaphore.acquire();
				new GetChannelVideosTask(channelId, publishedAfter, true, videos -> {
					semaphore.release();
					int numberOfVideos = videos != null ? videos.size() : 0;
					if (numberOfVideos > 0) {
						changed.compareAndSet(false, true);
					}
					if (listener != null) {
						listener.onChannelVideosFetched(channelId, numberOfVideos, false);
					}
					countDown.countDown();
				}).executeInParallel();
			} catch (InterruptedException e) {
				Log.e(GetSubscriptionVideosTask.class.getName(), "Interrupt in semaphore.acquire:"+ e.getMessage(), e);
			}
		}

		try {
			countDown.await();
		} catch (InterruptedException e) {
			Log.e(GetSubscriptionVideosTask.class.getName(), "Interrupt in countDown.await:"+ e.getMessage(), e);
		}

		return changed.get();
	}


	@Override
	protected void onPostExecute(Boolean changed) {
		SkyTubeApp.getSettings().updateFeedsLastUpdateTime();
		if (listener != null) {
			listener.onAllChannelVideosFetched(changed);
		}

	}
}
