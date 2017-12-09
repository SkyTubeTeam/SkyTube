package free.rm.skytube.businessobjects;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/*
 * A BroadcastReceiver to receive a message from the AlarmManager, to start the FeedUpdaterService,
 * which checks subscribed channels for new videos.
 */
public class FeedUpdaterReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		Intent feedUpdaterService = new Intent(context, FeedUpdaterService.class);
		context.startService(feedUpdaterService);
	}
}
