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

package free.rm.skytube.businessobjects.db;

import android.os.AsyncTask;
import android.widget.Toast;

import free.rm.skytube.R;
import free.rm.skytube.businessobjects.YouTubeChannel;
import free.rm.skytube.gui.app.SkyTubeApp;
import free.rm.skytube.gui.businessobjects.SubsAdapter;
import free.rm.skytube.gui.businessobjects.SubscribeButton;

/**
 * A task that subscribes / unsubscribes to a YouTube channel.
 */
public class SubscribeToChannelTask extends AsyncTask<Void, Void, Boolean> {

	/** Set to true if the user wants to subscribe to a youtube channel;  false if the user wants to
	 *  unsubscribe. */
	private boolean			subscribeToChannel;
	private SubscribeButton subscribeButton;
	private YouTubeChannel	channel;

	private static String TAG = SubscribeToChannelTask.class.getSimpleName();


	/**
	 * Constructor.
	 *
	 * @param subscribeButton	The subscribe button that the user has just clicked.
	 * @param channel			The channel the user wants to subscribe / unsubscribe.
	 */
	public SubscribeToChannelTask(SubscribeButton subscribeButton, YouTubeChannel channel) {
		this.subscribeToChannel = !subscribeButton.isUserSubscribed();
		this.subscribeButton = subscribeButton;
		this.channel = channel;
	}


	@Override
	protected Boolean doInBackground(Void... params) {
		if (subscribeToChannel) {
			return SubscriptionsDb.getSubscriptionsDb().subscribe(channel);
		} else {
			return SubscriptionsDb.getSubscriptionsDb().unsubscribe(channel);
		}
	}


	@Override
	protected void onPostExecute(Boolean success) {
		if (success) {
			SubsAdapter adapter = SubsAdapter.get(subscribeButton.getContext());

			if (subscribeToChannel) {
				// change the state of the button
				subscribeButton.setUnsubscribeState();
				// Also change the subscription state of the channel
				channel.setUserSubscribed(true);

				// append the channel to the SubsAdapter (i.e. the channels subscriptions list/drawer)
				adapter.appendChannel(channel);

				Toast.makeText(subscribeButton.getContext(), R.string.subscribed, Toast.LENGTH_LONG).show();
			} else {
				// change the state of the button
				subscribeButton.setSubscribeState();
				// Also change the subscription state of the channel
				channel.setUserSubscribed(false);
				
				// remove the channel from the SubsAdapter (i.e. the channels subscriptions list/drawer)
				adapter.removeChannel(channel);

				Toast.makeText(subscribeButton.getContext(), R.string.unsubscribed, Toast.LENGTH_LONG).show();
			}
		} else {
			String err = String.format(SkyTubeApp.getStr(R.string.error_unable_to_subscribe), channel.getId());
			Toast.makeText(subscribeButton.getContext(), err, Toast.LENGTH_LONG).show();
		}
	}

}
