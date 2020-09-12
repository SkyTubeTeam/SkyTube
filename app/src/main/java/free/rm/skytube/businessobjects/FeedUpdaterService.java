package free.rm.skytube.businessobjects;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.YouTube.Tasks.GetSubscriptionVideosTask;
import free.rm.skytube.businessobjects.YouTube.Tasks.GetSubscriptionVideosTaskListener;
import free.rm.skytube.gui.activities.MainActivity;

/**
 * A Service to automatically refresh the Subscriptions Database for Subscribed Channels. If any new videos have been found,
 * the Service will create a notification.
 */
public class FeedUpdaterService extends Service implements GetSubscriptionVideosTaskListener {
	private GetSubscriptionVideosTask getSubscriptionVideosTask;
	private int newVideosFetched;

	public static final String NEW_SUBSCRIPTION_VIDEOS_FOUND = "FeedUpdaterService.NEW_SUBSCRIPTION_VIDEOS_FOUND";

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// Need to instantiate the task here since you can only run a task once.
		int feedUpdaterInterval = Integer.parseInt(SkyTubeApp.getPreferenceManager().getString(SkyTubeApp.getStr(R.string.pref_key_feed_notification), "0"));
		if(feedUpdaterInterval > 0) {
			newVideosFetched = 0;
			getSubscriptionVideosTask = new GetSubscriptionVideosTask(this, null);
			getSubscriptionVideosTask.executeInParallel();
		}
		return START_STICKY;

	}

	@Override
	public void onChannelVideosFetched(String channelId, int videosFetched, boolean videosDeleted) {
		newVideosFetched += videosFetched;
	}

	@Override
	public void onAllChannelVideosFetched(boolean changed) {
		if(changed) {
			Intent clickIntent = new Intent(this, MainActivity.class);
			clickIntent.setAction(MainActivity.ACTION_VIEW_FEED);

			PendingIntent clickPendingIntent = PendingIntent.getActivity(this, 0, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT);

			Notification notification = new NotificationCompat.Builder(this, SkyTubeApp.NEW_VIDEOS_NOTIFICATION_CHANNEL)
							.setSmallIcon(R.drawable.ic_notification_icon)
							.setContentTitle(getString(R.string.app_name))
							.setContentText(String.format(getString(R.string.notification_new_videos_found), newVideosFetched))
							.setContentIntent(clickPendingIntent)
							.setAutoCancel(true)
							.build();

			NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
			notificationManager.notify(SkyTubeApp.NEW_VIDEOS_NOTIFICATION_CHANNEL_ID, notification);

			// Send a broadcast that new subscription videos have been found. The feed tab will receive the broadcast and
			// refresh its video grid to show the new videos.
			Intent feedTabIntent = new Intent(NEW_SUBSCRIPTION_VIDEOS_FOUND);
			sendBroadcast(feedTabIntent);
		}
	}
}