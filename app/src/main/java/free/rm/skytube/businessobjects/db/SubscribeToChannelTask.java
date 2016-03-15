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
import free.rm.skytube.gui.app.SkyTubeApp;
import free.rm.skytube.gui.businessobjects.SubscribeButton;

/**
 * A task that subscribes / Unsubscribes to a YouTube channel.
 */
public class SubscribeToChannelTask extends AsyncTask<Void, Void, Boolean> {

	private boolean			subscribeToChannel;
	private SubscribeButton subscribeButton;
	private String			channelId;

	private static String TAG = CheckIfUserSubbedToChannelTask.class.getSimpleName();


	/**
	 * Constructor.
	 *
	 * @param subscribeButton	The subscribe button that the user has just clicked.
	 * @param channelId			The channel ID the user wants to subscribe / unsubscribe.
	 */
	public SubscribeToChannelTask(SubscribeButton subscribeButton, String channelId) {
		this.subscribeToChannel = !subscribeButton.isUserSubscribed();
		this.subscribeButton = subscribeButton;
		this.channelId = channelId;
	}


	@Override
	protected Boolean doInBackground(Void... params) {
		if (subscribeToChannel) {
			return SkyTubeApp.getSubscriptionsDb().subscribe(channelId);
		} else {
			return SkyTubeApp.getSubscriptionsDb().unsubscribe(channelId);
		}
	}

	@Override
	protected void onPostExecute(Boolean success) {
		if (success) {
			if (subscribeToChannel) {
				subscribeButton.setUnsubscribeState();
				Toast.makeText(subscribeButton.getContext(), R.string.subscribed, Toast.LENGTH_LONG).show();
			} else {
				subscribeButton.setSubscribeState();
				Toast.makeText(subscribeButton.getContext(), R.string.unsubscribed, Toast.LENGTH_LONG).show();
			}
		} else {
			String err = String.format(SkyTubeApp.getStr(R.string.error_unable_to_subscribe), channelId);
			Toast.makeText(subscribeButton.getContext(), err, Toast.LENGTH_LONG).show();
		}
	}

}
