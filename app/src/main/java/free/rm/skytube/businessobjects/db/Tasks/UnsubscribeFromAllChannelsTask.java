package free.rm.skytube.businessobjects.db.Tasks;

import android.os.Handler;

import java.io.IOException;
import java.util.List;

import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.AsyncTaskParallel;
import free.rm.skytube.businessobjects.Logger;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.db.SubscriptionsDb;
import free.rm.skytube.gui.businessobjects.adapters.SubsAdapter;

/**
 * An Asynctask class that unsubscribes user from all the channels at once.
 */
public class UnsubscribeFromAllChannelsTask extends AsyncTaskParallel<YouTubeChannel, Void, Void> {

	private Handler handler = new Handler();

	@Override
	protected Void doInBackground(YouTubeChannel... youTubeChannels) {
		List<String> channelList = SubscriptionsDb.getSubscriptionsDb().getSubscribedChannelIds();

		for (final String channelId : channelList) {
			SubscriptionsDb.getSubscriptionsDb().unsubscribe(channelId);

			handler.post(() -> SubsAdapter.get(SkyTubeApp.getContext()).removeChannel(channelId));
		}

		return null;
	}

}
