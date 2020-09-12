package free.rm.skytube.businessobjects.db.Tasks;

import free.rm.skytube.businessobjects.AsyncTaskParallel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.db.SubscriptionsDb;

/**
 * An Asynctask class that unsubscribes user from all the channels at once.
 */
public class UnsubscribeFromAllChannelsTask extends AsyncTaskParallel<YouTubeChannel, Void, Void> {

	@Override
	protected Void doInBackground(YouTubeChannel... youTubeChannels) {
		SubscriptionsDb.getSubscriptionsDb().unsubscribeFromAllChannels();
		return null;
	}

}
