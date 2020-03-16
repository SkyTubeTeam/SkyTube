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
import android.util.Log;

import androidx.core.util.Consumer;

import java.util.ArrayList;
import java.util.List;

import free.rm.skytube.businessobjects.AsyncTaskParallel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.db.SubscriptionsDb;
import free.rm.skytube.gui.businessobjects.adapters.SubsAdapter;

/**
 * Gets a list of channels (from the DB) that the user is subscribed to and then passes the channels
 * list to the given {@link SubsAdapter}.
 */
public class GetSubscribedChannelsTask extends AsyncTaskParallel<Void, Void, List<YouTubeChannel>> {


	private static final String TAG = GetSubscribedChannelsTask.class.getSimpleName();
	private final Context context;
	private final Consumer<List<YouTubeChannel>> consumer;

	public GetSubscribedChannelsTask(Context context, Consumer<List<YouTubeChannel>> consumer) {
		this.context = context;
		this.consumer = consumer;
	}


	@Override
	protected List<YouTubeChannel> doInBackground(Void... params) {
		List<YouTubeChannel> subbedChannelsList = new ArrayList<>();
		GetChannelInfo channelInfo = new GetChannelInfo(context,true);
		try {
			List<String> channelIds = SubscriptionsDb.getSubscriptionsDb().getSubscribedChannelIds();

			// TODO, add bookmark and downloaded videos channel id too...

			for (String id : channelIds) {
				YouTubeChannel channel = channelInfo.getChannelInfoSync(id);
				// This shouldn't be null, but could happen in rare scenarios, where the app is offline,
				// and the info previously not saved
				if (channel != null) {
					subbedChannelsList.add(channel);
				}
			}

		} catch (Throwable tr) {
			Log.e(TAG, "An error has occurred while refreshing channels", tr);
		}

		return subbedChannelsList;
	}

	@Override
	protected void onPostExecute(List<YouTubeChannel> subbedChannelsList) {
		if (consumer != null) {
			consumer.accept(subbedChannelsList);
		}
	}

}
