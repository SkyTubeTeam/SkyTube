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

package free.rm.skytube.businessobjects.db.Tasks;

import android.content.Context;
import android.widget.Toast;

import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.AsyncTaskParallel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.db.DatabaseResult;
import free.rm.skytube.businessobjects.db.SubscriptionsDb;
import free.rm.skytube.gui.businessobjects.adapters.SubsAdapter;
import free.rm.skytube.gui.businessobjects.views.SubscribeButton;
import free.rm.skytube.gui.fragments.SubscriptionsFeedFragment;

/**
 * A task that subscribes / unsubscribes to a YouTube channel.
 */
public class SubscribeToChannelTask extends AsyncTaskParallel<Void, Void, DatabaseResult> {

	/** Set to true if the user wants to subscribe to a youtube channel;  false if the user wants to
	 *  unsubscribe. */
	private boolean			subscribeToChannel;
	private SubscribeButton subscribeButton;
	private Context         context;
	private YouTubeChannel	channel;
	private boolean         displayToastMessage = true;


	/**
	 * Constructor.
	 *
	 * @param subscribeButton	The subscribe button that the user has just clicked.
	 * @param channel			The channel the user wants to subscribe / unsubscribe.
	 */
	public SubscribeToChannelTask(SubscribeButton subscribeButton, YouTubeChannel channel) {
		this.subscribeToChannel = !subscribeButton.isUserSubscribed();
		this.subscribeButton = subscribeButton;
		this.context = subscribeButton.getContext();
		this.channel = channel;
	}


	/**
	 * Constructor.  Will unsubscribe the given channel.  No toast messages will be displayed.
	 *
	 * @param channel   Channel the user wants to unsubscribe.
	 */
	public SubscribeToChannelTask(YouTubeChannel channel) {
		this.subscribeToChannel = false;
		this.subscribeButton = null;
		this.context = SkyTubeApp.getContext();
		this.channel = channel;
		displayToastMessage = false;
	}


	@Override
	protected DatabaseResult doInBackground(Void... params) {
		if (subscribeToChannel) {
			return SubscriptionsDb.getSubscriptionsDb().subscribe(channel);
		} else {
			return SubscriptionsDb.getSubscriptionsDb().unsubscribe(channel.getId());
		}
	}


	@Override
	protected void onPostExecute(DatabaseResult databaseResult) {
		if (databaseResult == DatabaseResult.SUCCESS) {
			SubsAdapter adapter = SubsAdapter.get(context);

			// we need to refresh the Feed tab so it shows videos from the newly subscribed (or
			// unsubscribed) channels
			SubscriptionsFeedFragment.refreshSubsFeedFromCache();

			if (subscribeToChannel) {
				// change the state of the button
				if (subscribeButton != null)
					subscribeButton.setUnsubscribeState();
				// Also change the subscription state of the channel
				channel.setUserSubscribed(true);

				// update the SubsAdapter (i.e. the channels subscriptions list/drawer)
				adapter.refreshSubsList();

				if (displayToastMessage) {
					Toast.makeText(context, R.string.subscribed, Toast.LENGTH_LONG).show();
				}
			} else {
				// change the state of the button
				if (subscribeButton != null)
					subscribeButton.setSubscribeState();
				// Also change the subscription state of the channel
				channel.setUserSubscribed(false);
				
				// remove the channel from the SubsAdapter (i.e. the channels subscriptions list/drawer)
				adapter.removeChannel(channel.getId());

				if (displayToastMessage) {
					Toast.makeText(context, R.string.unsubscribed, Toast.LENGTH_LONG).show();
				}
			}
		} else if (databaseResult == DatabaseResult.NOT_MODIFIED) {
			if (subscribeToChannel) {
				Toast.makeText(context, R.string.channel_already_subscribed, Toast.LENGTH_LONG).show();
			}
		} else {
			String err = String.format(SkyTubeApp.getStr(R.string.error_unable_to_subscribe), channel.getId());
			Toast.makeText(context, err, Toast.LENGTH_LONG).show();
		}

		this.subscribeButton = null;
		this.context = null;
	}

}
