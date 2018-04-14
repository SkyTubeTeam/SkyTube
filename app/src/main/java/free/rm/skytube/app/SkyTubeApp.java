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
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.multidex.MultiDexApplication;
import android.support.v7.app.AlertDialog;
import android.widget.TextView;
import android.widget.Toast;

import com.klinker.android.link_builder.Link;
import com.klinker.android.link_builder.LinkBuilder;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import free.rm.skytube.R;
import free.rm.skytube.businessobjects.FeedUpdaterReceiver;
import free.rm.skytube.businessobjects.GetPlaylistTask;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannelInterface;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubePlaylist;
import free.rm.skytube.businessobjects.YouTube.Tasks.GetYouTubeChannelInfoTask;
import free.rm.skytube.gui.activities.MainActivity;
import free.rm.skytube.gui.businessobjects.PlaylistClickListener;
import free.rm.skytube.gui.businessobjects.YouTubePlayer;
import free.rm.skytube.gui.businessobjects.YouTubePlaylistListener;
import free.rm.skytube.gui.fragments.ChannelBrowserFragment;
import free.rm.skytube.gui.fragments.PlaylistVideosFragment;

/**
 * SkyTube application.
 */
public class SkyTubeApp extends MultiDexApplication {

	/** SkyTube Application databaseInstance. */
	private static SkyTubeApp skyTubeApp = null;

	public static final String KEY_SUBSCRIPTIONS_LAST_UPDATED = "SkyTubeApp.KEY_SUBSCRIPTIONS_LAST_UPDATED";
	public static final String NEW_VIDEOS_NOTIFICATION_CHANNEL = "free.rm.skytube.NEW_VIDEOS_NOTIFICATION_CHANNEL";
	public static final int NEW_VIDEOS_NOTIFICATION_CHANNEL_ID = 1;

	@Override
	public void onCreate() {
		super.onCreate();
		skyTubeApp = this;
		initChannels(this);
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
	 * @return boolean determining if the device is connected via WiFi
	 */
	public static boolean isConnectedToWiFi() {
		final ConnectivityManager connMgr = (ConnectivityManager)
						getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
		final android.net.NetworkInfo wifi = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		return wifi != null && wifi.isConnectedOrConnecting();
	}

	/**
	 * @return boolean determining if the device is connected via Mobile
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

	/**
	 * Linkify the text inside the passed TextView, but intercept certain kinds of urls, to instead open them
	 * from within the app itself (i.e. YouTube video urls, playlist urls, etc). Also, capture all long clicks
	 * to show a menu to open, copy, or share the url.
	 * @param context The Activity context (this is needed instead of getContext(), in order to display the long click menu.
	 * @param textView The TextView whose contents will be modified
	 * @param youTubePlaylistListener	A Listener to handle a clicked link to a YouTube Playlist
	 */
	public static void interceptYouTubeLinks(final Context context, TextView textView, final YouTubePlaylistListener youTubePlaylistListener) {
		Link link = new Link(android.util.Patterns.WEB_URL);
		final Pattern videoPattern = Pattern.compile("http(?:s?):\\/\\/(?:www\\.)?youtu(?:be\\.com\\/watch\\?v=|\\.be\\/)([\\w\\-\\_]*)(&(amp;)?[\\w\\?=\\.]*)?");
		final Pattern playlistPattern = Pattern.compile("^.*(youtu.be\\/|list=)([^#\\&\\?]*).*");
		final Pattern channelPattern = Pattern.compile("(?:https|http)\\:\\/\\/(?:[\\w]+\\.)?youtube\\.com\\/(?:c\\/|channel\\/|user\\/)?([a-zA-Z0-9\\-]{1,})");
		link.setOnClickListener(new Link.OnClickListener() {
			@Override
			public void onClick(String clickedText) {
				final Matcher playlistMatcher = playlistPattern.matcher(clickedText);
				final Matcher channelMatcher = channelPattern.matcher(clickedText);
				if(videoPattern.matcher(clickedText).matches()) {
					YouTubePlayer.launch(clickedText, context);
				} else if(playlistMatcher.matches()) {
					String playlistId = playlistMatcher.group(2);
					// Retrieve the playlist from the playlist ID that was in the url the user clicked on
					new GetPlaylistTask(playlistId, new PlaylistClickListener() {
						@Override
						public void onClickPlaylist(YouTubePlaylist playlist) {
							if (youTubePlaylistListener != null) {
								youTubePlaylistListener.onYouTubePlaylist(playlist);
							} else {
								// Pass the clicked playlist to PlaylistVideosFragment.
								Intent playlistIntent = new Intent(context, MainActivity.class);
								playlistIntent.setAction(MainActivity.ACTION_VIEW_PLAYLIST);
								playlistIntent.putExtra(PlaylistVideosFragment.PLAYLIST_OBJ, playlist);
								context.startActivity(playlistIntent);
							}
						}
					}).executeInParallel();
				} else if(channelMatcher.matches()) {
					String username = channelMatcher.group(1);
					new GetYouTubeChannelInfoTask(getContext(), new YouTubeChannelInterface() {
						@Override
						public void onGetYouTubeChannel(YouTubeChannel youTubeChannel) {
							Intent channelIntent = new Intent(context, MainActivity.class);
							channelIntent.setAction(MainActivity.ACTION_VIEW_CHANNEL);
							channelIntent.putExtra(ChannelBrowserFragment.CHANNEL_OBJ, youTubeChannel);
							context.startActivity(channelIntent);
						}
					}).setUsingUsername().executeInParallel(username);
				} else {
					Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(clickedText));
					browserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					context.startActivity(browserIntent);
				}
			}
		});

		// Handle long click by showing a dialog allowing the user to open the link in a browser, copy the url, or share it.
		link.setOnLongClickListener(new Link.OnLongClickListener() {
			@Override
			public void onLongClick(final String clickedText) {
				AlertDialog.Builder builder = new AlertDialog.Builder(context)
								.setTitle(clickedText)
								.setItems(new CharSequence[]
										{getStr(R.string.open_in_browser), getStr(R.string.copy_url), getStr(R.string.share_via)},
										new DialogInterface.OnClickListener() {
											@Override
											public void onClick(DialogInterface dialog, int which) {
												switch (which) {
													case 0:
														Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(clickedText));
														browserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
														context.startActivity(browserIntent);
														break;
													case 1:
														ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
														ClipData clip = ClipData.newPlainText("URL", clickedText);
														clipboard.setPrimaryClip(clip);
														Toast.makeText(context, R.string.url_copied_to_clipboard, Toast.LENGTH_SHORT).show();
														break;
													case 2:
														Intent intent = new Intent(android.content.Intent.ACTION_SEND);
														intent.setType("text/plain");
														intent.putExtra(android.content.Intent.EXTRA_TEXT, clickedText);
														context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_via)));
														break;
												}
											}
										});
								builder.create().show();
			}
		});
		LinkBuilder.on(textView)
			.addLink(link)
			.build();
	}

	public static void interceptYouTubeLinks(Context context, TextView textView) {
		interceptYouTubeLinks(context, textView, null);
	}

}
