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

import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.util.List;

import free.rm.skytube.R;
import free.rm.skytube.businessobjects.AsyncTaskParallel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.db.SubscriptionsDb;
import free.rm.skytube.gui.businessobjects.adapters.SubsAdapter;

/**
 * Gets a list of channels (from the DB) that the user is subscribed to and then passes the channels
 * list to the given {@link SubsAdapter}.
 */
public class GetSubscribedChannelsTask extends AsyncTaskParallel<Void, Void, List<YouTubeChannel>> {

	private SubsAdapter adapter;
	private View progressBar;

	private static final String TAG = GetSubscribedChannelsTask.class.getSimpleName();


	public GetSubscribedChannelsTask(SubsAdapter adapter, View progressBar) {
		this.adapter = adapter;
		this.progressBar = progressBar;
	}


	@Override
	protected void onPreExecute() {
		if (progressBar != null)
			progressBar.setVisibility(View.VISIBLE);
	}


	@Override
	protected List<YouTubeChannel> doInBackground(Void... params) {
		List<YouTubeChannel> subbedChannelsList = null;

		try {
			subbedChannelsList = SubscriptionsDb.getSubscriptionsDb().getSubscribedChannels();
		} catch (Throwable tr) {
			Log.e(TAG, "An error has occurred while getting subbed channels", tr);
		}

		return subbedChannelsList;
	}


	@Override
	protected void onPostExecute(List<YouTubeChannel> subbedChannelsList) {
		if (progressBar != null) {
			progressBar.setVisibility(View.INVISIBLE);
			progressBar = null;
		}

		if (subbedChannelsList == null) {
			Toast.makeText(adapter.getContext(), R.string.error_get_subbed_channels, Toast.LENGTH_LONG).show();
		} else {
			adapter.appendList(subbedChannelsList);
		}

		// Notify the SubsAdapter that the subbed channel list has been retrieved and populated.  If
		// there is an error we still need to notify the adapter that the task has been completed
		// from this end...
		adapter.subsListRetrieved();
	}

}
