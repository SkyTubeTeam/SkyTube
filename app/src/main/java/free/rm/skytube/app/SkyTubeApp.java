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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Looper;
import android.os.StrictMode;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.ArrayRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.net.ConnectivityManagerCompat;
import androidx.multidex.MultiDexApplication;
import androidx.preference.PreferenceManager;

import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;

import org.ocpsoft.prettytime.PrettyTime;
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException;

import java.io.IOException;
import java.net.SocketException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import free.rm.skytube.BuildConfig;
import free.rm.skytube.R;
import free.rm.skytube.businessobjects.FeedUpdaterReceiver;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubePlaylist;
import free.rm.skytube.businessobjects.YouTube.YouTubeTasks;
import free.rm.skytube.businessobjects.YouTube.newpipe.ContentId;
import free.rm.skytube.businessobjects.YouTube.newpipe.NewPipeService;
import free.rm.skytube.businessobjects.db.DatabaseTasks;
import free.rm.skytube.gui.activities.MainActivity;
import free.rm.skytube.gui.businessobjects.YouTubePlayer;
import free.rm.skytube.gui.fragments.ChannelBrowserFragment;
import free.rm.skytube.gui.fragments.FragmentNames;
import free.rm.skytube.gui.fragments.PlaylistVideosFragment;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.exceptions.UndeliverableException;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * SkyTube application.
 */
public class SkyTubeApp extends MultiDexApplication {

	/** SkyTube Application databaseInstance. */
	private static SkyTubeApp skyTubeApp = null;
	private Settings settings;
	private FragmentNames names;

	private static final String TAG = "SkyTubeApp";

	public static final String KEY_SUBSCRIPTIONS_LAST_UPDATED = "SkyTubeApp.KEY_SUBSCRIPTIONS_LAST_UPDATED";
	public static final String NEW_VIDEOS_NOTIFICATION_CHANNEL = "free.rm.skytube.NEW_VIDEOS_NOTIFICATION_CHANNEL";
	public static final int NEW_VIDEOS_NOTIFICATION_CHANNEL_ID = 1;

	@Override
	public void onCreate() {
		super.onCreate();
		this.settings = new Settings(this);
		this.settings.migrate();
		this.names = new FragmentNames(this);
		skyTubeApp = this;
		setupRxJava();
		preloadPrettyTime();
		if (BuildConfig.DEBUG) {
			StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
					.detectDiskReads()
					.detectDiskWrites()
					.detectCustomSlowCalls()
					.detectNetwork()   // or .detectAll() for all detectable problems
					.penaltyLog()
					.build());
			StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
					.detectLeakedSqlLiteObjects()
					.detectLeakedClosableObjects()
					.penaltyLog()
					//.penaltyDeath()
					.build());
		}
		initChannels();
	}

	private void setupRxJava() {
		RxJavaPlugins.setErrorHandler(e -> {
			if (e instanceof UndeliverableException) {
				e = e.getCause();
			}
			if ((e instanceof IOException) || (e instanceof SocketException)) {
				// fine, irrelevant network problem or API that throws on cancellation
				return;
			}
			if (e instanceof InterruptedException) {
				// fine, some blocking code was interrupted by a dispose call
				return;
			}
			if ((e instanceof NullPointerException) || (e instanceof IllegalArgumentException)) {
				// that's likely a bug in the application
				Thread.currentThread().getUncaughtExceptionHandler()
						.uncaughtException(Thread.currentThread(), e);
				return;
			}
			if (e instanceof IllegalStateException) {
				// that's a bug in RxJava or in a custom operator
				Thread.currentThread().getUncaughtExceptionHandler()
						.uncaughtException(Thread.currentThread(), e);
				return;
			}
			Log.e(TAG, "Undeliverable exception received, not sure what to do" + e.getMessage(), e);
		});
	}

	private static void preloadPrettyTime() {
		Completable.fromAction(() -> new PrettyTime().format(LocalDate.of(2021, 2, 23)))
				.subscribeOn(Schedulers.io())
				.onErrorReturn(exc -> {
					Log.e(TAG, "Unable to initialize PrettyTime, because: " + exc.getMessage(), exc);
					return "";
				})
				.subscribe();
	}

	@RequiresApi(api = Build.VERSION_CODES.M)
	private static void uiThreadImpl() {
		if (!Looper.getMainLooper().isCurrentThread()) {
			throw new RuntimeException("Expected to be executing in UI!");
		}
	}

	public static void uiThread() {
		if (BuildConfig.DEBUG) {
			if (Build.VERSION.SDK_INT >= 29) {
				uiThreadImpl();
			} else {
				Log.i(TAG, "Expected to be UI thread : " + Thread.currentThread().getName() + " [" + Build.VERSION.SDK_INT + ']');
			}
		}
	}

	@RequiresApi(api = Build.VERSION_CODES.M)
	private static void nonUiThreadImpl() {
		if (Looper.getMainLooper().isCurrentThread()) {
			throw new RuntimeException("Expected to be NOT blocking the UI!");
		}
	}

	public static void nonUiThread() {
		if (BuildConfig.DEBUG) {
			if (Build.VERSION.SDK_INT >= 29) {
				nonUiThreadImpl();
			} else {
				Log.i(TAG, "Expected to be non-UI thread : " + Thread.currentThread().getName() + " [" + Build.VERSION.SDK_INT + ']');
			}
		}
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
	public static String[] getStringArray(@ArrayRes int stringArrayResId) {
		return skyTubeApp.getStrArray(stringArrayResId);
	}

	/**
	 * Given a string array resource ID, it returns an array of strings.
	 *
	 * @param stringArrayResId String array resource ID (e.g. R.string.my_array_string)
	 * @return Array of String.
	 */
	public String[] getStrArray(@ArrayRes int stringArrayResId) {
		return getResources().getStringArray(stringArrayResId);
	}

	/**
	 * Given a string array resource ID, it returns an list of strings.
	 *
	 * @param stringArrayResId String array resource ID (e.g. R.string.my_array_string)
	 * @return List of String.
	 */
	public static List<String> getStringArrayAsList(@ArrayRes int stringArrayResId) {
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

	public static FragmentNames getFragmentNames() {
		return skyTubeApp.names;
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
	 * @return True if the device is connected to a metered network.
	 */
	public static boolean isActiveNetworkMetered() {
		return ConnectivityManagerCompat.isActiveNetworkMetered(ContextCompat.getSystemService(skyTubeApp,
				ConnectivityManager.class));
	}

	/**
	 * Get the network info
	 * @param context
	 * @return
	 */
	public static NetworkInfo getNetworkInfo(@NonNull Context context){
		return ContextCompat.getSystemService(context, ConnectivityManager.class).getActiveNetworkInfo();
	}

	/**
	 * Check if there is any connectivity
	 * @param context
	 * @return
	 */
	public static boolean isConnected(@NonNull Context context){
		NetworkInfo info = getNetworkInfo(context);
		return (info != null && info.isConnected());
	}

	/*
	 * Initialize Notification Channels (for Android OREO)
	 */
	private void initChannels() {
		final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
		final NotificationChannelCompat notificationChannel = new NotificationChannelCompat
				.Builder(NEW_VIDEOS_NOTIFICATION_CHANNEL, NotificationManagerCompat.IMPORTANCE_LOW)
				.setName(getString(R.string.notification_channel_feed_title))
				.setLightsEnabled(true)
				.setLightColor(ColorUtils.compositeColors(0xFFFF0000, 0xFFFF0000))
				.setVibrationEnabled(true)
				.build();
		notificationManager.createNotificationChannel(notificationChannel);
	}

	/**
	 * Setup the Feed Updater Service. First, cancel the Alarm that will trigger the next fetch (if there is one), then set the
	 * Alarm with the passed interval, if it's greater than 0.
	 * @param interval The number of milliseconds between each time new videos for subscribed channels should be fetched.
	 */
	public static void setFeedUpdateInterval(int interval) {
		Intent alarm = new Intent(getContext(), FeedUpdaterReceiver.class);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(getContext(), 0, alarm, PendingIntent.FLAG_CANCEL_CURRENT);
		AlarmManager alarmManager = ContextCompat.getSystemService(getContext(), AlarmManager.class);

		// Feed Auto Updater has been cancelled. If the selected interval is greater than 0, set the new alarm to call FeedUpdaterService
		if(interval > 0) {
			alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime()+interval, interval, pendingIntent);
		}
	}

	public static Settings getSettings() {
		return skyTubeApp.settings;
	}

	public Settings getAppSettings() {
		return settings;
	}

	public static void notifyUserOnError(@NonNull Context ctx, @Nullable Throwable throwable) {
		if (throwable == null) {
			return;
		}
        if (throwable instanceof ReCaptchaException) {
            handleRecaptchaException(ctx, (ReCaptchaException) throwable);
            return;
        }
		final String message;
		if (throwable instanceof GoogleJsonResponseException) {
			GoogleJsonResponseException exception = (GoogleJsonResponseException) throwable;
			List<GoogleJsonError.ErrorInfo> errors = exception.getDetails().getErrors();
			if (errors != null && !errors.isEmpty()) {
				message =  "Server error:" + errors.get(0).getMessage()+ ", reason: "+ errors.get(0).getReason();
			} else {
				message = exception.getDetails().getMessage();
			}
		} else {
			message = throwable.getMessage();
		}
		if (message != null) {
			Log.e(TAG, "Error: "+message);

			String toastText = message;
			if(message.contains("resolve host")) {
				toastText = "No internet connection available";
			}
			if(message.contains("JavaScript player")) {
				return; // Error from Player when watching downloaded videos offline
			}

			Toast.makeText(ctx, toastText, Toast.LENGTH_LONG).show();
		}
	}

    private static void handleRecaptchaException(Context context, ReCaptchaException reCaptchaException) {
        // remove "pbj=1" parameter from YouYube urls, as it makes the page JSON and not HTML
        String url = reCaptchaException.getUrl().replace("&pbj=1", "").replace("pbj=1&", "").replace("?pbj=1", "");

        Log.e(TAG, "Error: " + reCaptchaException.getMessage() + " url: " + url, reCaptchaException);
        Toast.makeText(context, R.string.recaptcha_challenge_requested, Toast.LENGTH_LONG).show();
        viewInBrowser(url, context);
        return;
    }

	public static void shareUrl(@NonNull Context context, String url) {
		Intent intent = new Intent(android.content.Intent.ACTION_SEND);
		intent.setType("text/plain");
		intent.putExtra(android.content.Intent.EXTRA_TEXT, url);
		context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_via)));
	}

	public static void copyUrl(@NonNull Context context, String text, String url) {
		ClipData clip = ClipData.newPlainText(text, url);
		ContextCompat.getSystemService(context, ClipboardManager.class).setPrimaryClip(clip);
		Toast.makeText(context, R.string.url_copied_to_clipboard, Toast.LENGTH_SHORT).show();
	}

	/**
	 * The video URL is passed to SkyTube via another Android app (i.e. via an intent).
	 *
	 * @return The URL of the YouTube video the user wants to play.
	 */
	public static ContentId getUrlFromIntent(@NonNull final Context ctx, final Intent intent) {
		if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
			return parseUrl(ctx, intent.getData().toString(), true);
		}
		return null;
	}

	public static ContentId parseUrl(@NonNull Context context, String url, boolean showErrorIfNotValid) {
		try {
			ContentId id = NewPipeService.get().getContentId(url);
			if (id == null && showErrorIfNotValid) {
				showInvalidUrlToast(context, url);
			}
			return id;
		} catch (RuntimeException e) {
			SkyTubeApp.notifyUserOnError(context, e);
			return null;
		}

	}

	private static void showInvalidUrlToast(@NonNull Context context, String url) {
		String message = String.format(context.getString(R.string.error_invalid_url), url);
		Toast.makeText(context, message, Toast.LENGTH_LONG).show();
	}

	/**
	 * Open the url - internally, or externally if useExternalBrowser is switched on.
	 * @param ctx
	 * @param url
	 * @param useExternalBrowser
	 */
	public static void openUrl(Context ctx, String url, boolean useExternalBrowser) {
		// Show error message only if we don't want to use external browser, so we expect that the URL
		// can be handled internally, and if the URL is invalid, it's an error.
		ContentId content = parseUrl(ctx, url, !useExternalBrowser);

		if (openContent(ctx, content)) {
			return;
		} else {
			if (useExternalBrowser) {
				viewInBrowser(url, ctx);
			}
		}
	}

	/**
	 * Open the content in the appropriate viewer Activity, return true if it found one.
	 * @param ctx
	 * @param content
	 * @return
	 */
	public static boolean openContent(Context ctx, ContentId content) {
		if (content == null) {
			return false;
		}
		switch (content.getType()) {
			case STREAM: {
				YouTubePlayer.launch(content, ctx);
				break;
			}
			case CHANNEL: {
				SkyTubeApp.launchChannel(content.getId(), ctx);
				break;
			}
			case PLAYLIST: {
				YouTubeTasks.getPlaylist(ctx, content.getId())
						.subscribe(playlist -> launchPlaylist(playlist, ctx));
				break;
			}
			default:
				return false;
		}
		return true;
	}


	/**
	 * Launches the channel view, so the user can see all the videos from a channel.
	 *
	 * @param channelId the channel to be displayed.
	 */
	public static void launchChannel(String channelId, Context context) {
		if (channelId != null) {
			DatabaseTasks.getChannelInfo(context, channelId, true)
					.subscribe(youTubeChannel -> launchChannel(youTubeChannel, context));
		}
	}

	/**
	 * Launches the channel view, so the user can see all the videos from a channel.
	 *
	 * @param youTubeChannel the channel to be displayed.
	 */
	public static void launchChannel(YouTubeChannel youTubeChannel, Context context) {
		Intent i = new Intent(context, MainActivity.class);
		i.setAction(MainActivity.ACTION_VIEW_CHANNEL);
		i.putExtra(ChannelBrowserFragment.CHANNEL_OBJ, youTubeChannel);
		context.startActivity(i);
	}

	/**
	 * Launch the {@link PlaylistVideosFragment}
	 * @param playlist the playlist to display
	 */
	public static void launchPlaylist(final YouTubePlaylist playlist, final Context context) {
		Intent playlistIntent = new Intent(context, MainActivity.class);
		playlistIntent.setAction(MainActivity.ACTION_VIEW_PLAYLIST);
		playlistIntent.putExtra(PlaylistVideosFragment.PLAYLIST_OBJ, playlist);
		context.startActivity(playlistIntent);
	}

	/**
	 * Launch an external activity to actually open the given URL
	 * @param url
	 */
	public static void viewInBrowser(String url, final Context context) {
		Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
		browserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		try {
			context.startActivity(browserIntent);
		} catch (ActivityNotFoundException e) {
			showInvalidUrlToast(context, url);
			Log.e(TAG, "Activity not found for " + url + ", error:" + e.getMessage(), e);
		}
	}

}
