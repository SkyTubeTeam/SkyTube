/*
 * SkyTube
 * Copyright (C) 2015  Ramon Mifsud
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

package free.rm.skytube.app;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.multidex.MultiDexApplication;
import android.support.v4.content.res.ResourcesCompat;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import free.rm.skytube.R;
import free.rm.skytube.businessobjects.FeedUpdaterReceiver;

/**
 * SkyTube application.
 */
public class SkyTubeApp extends MultiDexApplication {

	/** SkyTube Application databaseInstance. */
	protected static SkyTubeApp skyTubeApp = null;

	public static final String KEY_SUBSCRIPTIONS_LAST_UPDATED = "SkyTubeApp.KEY_SUBSCRIPTIONS_LAST_UPDATED";
	public static final String NEW_VIDEOS_NOTIFICATION_CHANNEL = "free.rm.skytube.NEW_VIDEOS_NOTIFICATION_CHANNEL";
	public static final int NEW_VIDEOS_NOTIFICATION_CHANNEL_ID = 1;

	public boolean connectedToChromecast = false;
	public boolean connectingToChromecast = false;
	public Map<String, String> chromecastDevices = new HashMap<>();

	@Override
	public void onCreate() {
		super.onCreate();
		skyTubeApp = this;
		initChannels(this);
	}

	public static SkyTubeApp getInstance() {
		return skyTubeApp;
	}

	/**
	 * Returns a localised string.
	 *
	 * @param  stringResId	String resource ID (e.g. R.string.my_string)
	 * @return Localised string, from the strings XML file.
	 */
	public static String getStr(int stringResId) {
		return skyTubeApp.getString(stringResId);
	}


	/**
	 * Given a string array resource ID, it returns an array of strings.
	 *
	 * @param stringArrayResId String array resource ID (e.g. R.string.my_array_string)
	 * @return Array of String.
	 */
	public static String[] getStringArray(int stringArrayResId) {
		return skyTubeApp.getResources().getStringArray(stringArrayResId);
	}


	/**
	 * Given a string array resource ID, it returns an list of strings.
	 *
	 * @param stringArrayResId String array resource ID (e.g. R.string.my_array_string)
	 * @return List of String.
	 */
	public static List<String> getStringArrayAsList(int stringArrayResId) {
		return Arrays.asList(getStringArray(stringArrayResId));
	}


	/**
	 * Returns the App's {@link SharedPreferences}.
	 *
	 * @return {@link SharedPreferences}
	 */
	public static SharedPreferences getPreferenceManager() {
		return PreferenceManager.getDefaultSharedPreferences(skyTubeApp);
	}


	/**
	 * Returns the dimension value that is specified in R.dimens.*.  This value is NOT converted into
	 * pixels, but rather it is kept as it was originally written (e.g. dp).
	 *
	 * @return The dimension value.
	 */
	public static float getDimension(int dimensionId) {
		return skyTubeApp.getResources().getDimension(dimensionId);
	}


	/**
	 * @param colorId   Color resource ID (e.g. R.color.green).
	 *
	 * @return The color for the given color resource id.
	 */
	public static int getColorEx(int colorId) {
		return ResourcesCompat.getColor(skyTubeApp.getResources(), colorId, null);
	}


	/**
	 * @return {@link Context}.
	 */
	public static Context getContext() {
		return skyTubeApp.getBaseContext();
	}


	/**
	 * Restart the app.
	 */
	public static void restartApp() {
		Context context = getContext();
		PackageManager packageManager = context.getPackageManager();
		Intent intent = packageManager.getLaunchIntentForPackage(context.getPackageName());
		ComponentName componentName = intent.getComponent();
		Intent mainIntent = Intent.makeRestartActivityTask(componentName);
		context.startActivity(mainIntent);
		System.exit(0);
	}


	/**
	 * @return  True if the device is a tablet; false otherwise.
	 */
	public static boolean isTablet() {
		return getContext().getResources().getBoolean(R.bool.is_tablet);
	}


	/**
	 * @return True if the device is connected via mobile network such as 4G.
	 */
	public static boolean isConnectedToMobile() {
		final ConnectivityManager connMgr = (ConnectivityManager)
				getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
		final android.net.NetworkInfo mobile = connMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
		return mobile != null && mobile.isConnectedOrConnecting();
	}


	/*
	 * Initialize Notification Channels (for Android OREO)
	 * @param context
	 */
	@TargetApi(26)
	private void initChannels(Context context) {

		if(Build.VERSION.SDK_INT < 26) {
			return;
		}
		NotificationManager notificationManager =
						(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		String channelId = NEW_VIDEOS_NOTIFICATION_CHANNEL;
		CharSequence channelName = context.getString(R.string.notification_channel_feed_title);
		int importance = NotificationManager.IMPORTANCE_LOW;
		NotificationChannel notificationChannel = new NotificationChannel(channelId, channelName, importance);
		notificationChannel.enableLights(true);
		notificationChannel.setLightColor(Color.RED);
		notificationChannel.enableVibration(false);
		notificationManager.createNotificationChannel(notificationChannel);
	}

	/**
	 * Get the stored interval (in milliseconds) to pass to the below method.
	 */
	public static void setFeedUpdateInterval() {
		int feedUpdaterInterval = Integer.parseInt(SkyTubeApp.getPreferenceManager().getString(SkyTubeApp.getStr(R.string.pref_key_feed_notification), "0"));
		setFeedUpdateInterval(feedUpdaterInterval);
	}

	/**
	 * Setup the Feed Updater Service. First, cancel the Alarm that will trigger the next fetch (if there is one), then set the
	 * Alarm with the passed interval, if it's greater than 0. 
	 * @param interval The number of milliseconds between each time new videos for subscribed channels should be fetched.
	 */
	public static void setFeedUpdateInterval(int interval) {
		Intent alarm = new Intent(getContext(), FeedUpdaterReceiver.class);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(getContext(), 0, alarm, PendingIntent.FLAG_CANCEL_CURRENT);
		AlarmManager alarmManager = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);

		// Feed Auto Updater has been cancelled. If the selected interval is greater than 0, set the new alarm to call FeedUpdaterService
		if(interval > 0) {
			alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime()+interval, interval, pendingIntent);
		}
	}

}
