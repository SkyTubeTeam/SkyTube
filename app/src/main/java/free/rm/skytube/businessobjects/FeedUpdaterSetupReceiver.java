package free.rm.skytube.businessobjects;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import androidx.core.content.ContextCompat;

import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;

/**
 * A BroadcastReceiver that will receive a Broadcast when the device boots up. It will check if the user has set the app to
 * automatically check for new videos from subscribed channels, and set the repeating alarm if they have.
 */
public class FeedUpdaterSetupReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		int feedUpdaterInterval = SkyTubeApp.getSettings().getFeedUpdaterInterval();

		Intent i = new Intent(context, FeedUpdaterReceiver.class);
		PendingIntent intentExecuted = PendingIntent.getBroadcast(context, 0, i,
						PendingIntent.FLAG_CANCEL_CURRENT);
		if(feedUpdaterInterval > 0) {
			ContextCompat.getSystemService(context, AlarmManager.class)
					.setRepeating(AlarmManager.ELAPSED_REALTIME,
							SystemClock.elapsedRealtime()+feedUpdaterInterval, feedUpdaterInterval, intentExecuted);
		}
	}
}
