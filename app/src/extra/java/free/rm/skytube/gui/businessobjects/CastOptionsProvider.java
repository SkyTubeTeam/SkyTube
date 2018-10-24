package free.rm.skytube.gui.businessobjects;

import android.content.Context;
import android.text.format.DateUtils;

import com.google.android.gms.cast.framework.CastOptions;
import com.google.android.gms.cast.framework.OptionsProvider;
import com.google.android.gms.cast.framework.SessionProvider;
import com.google.android.gms.cast.framework.media.CastMediaOptions;
import com.google.android.gms.cast.framework.media.MediaIntentReceiver;
import com.google.android.gms.cast.framework.media.NotificationOptions;

import java.util.ArrayList;
import java.util.List;

import free.rm.skytube.BuildConfig;
import free.rm.skytube.gui.activities.NotificationClickActivity;

/**
 * Class that implements {@link com.google.android.gms.cast.framework.OptionsProvider}, which is required for Chromecast support.
 * See https://developers.google.com/android/reference/com/google/android/gms/cast/framework/OptionsProvider
 * Even though Android Studio will show that this class is not used anywhere, it is! Do not delete!
 */
public class CastOptionsProvider implements OptionsProvider {
	@Override
	public CastOptions getCastOptions(Context context) {
		List<String> buttonActions = new ArrayList<>();
		buttonActions.add(MediaIntentReceiver.ACTION_REWIND);
		buttonActions.add(MediaIntentReceiver.ACTION_TOGGLE_PLAYBACK);
		buttonActions.add(MediaIntentReceiver.ACTION_FORWARD);
		buttonActions.add(MediaIntentReceiver.ACTION_STOP_CASTING);
		// Showing "play/pause" and "stop casting" in the compat view of the notification.
		int[] compatButtonActionsIndicies = new int[]{ 1, 3 };
		// Builds a notification with the above actions. Each tap on the "rewind" and
		// "forward" buttons skips 30 seconds.
		// Tapping on the notification opens an Activity with class MainActivity.
		NotificationOptions notificationOptions = new NotificationOptions.Builder()
						.setActions(buttonActions, compatButtonActionsIndicies)
						.setSkipStepMs(30 * DateUtils.SECOND_IN_MILLIS)
						.setTargetActivityClassName(NotificationClickActivity.class.getName())
						.build();

		CastMediaOptions mediaOptions = new CastMediaOptions.Builder()
						.setNotificationOptions(notificationOptions)
						.build();



		return new CastOptions.Builder()
						.setReceiverApplicationId(BuildConfig.CHROMECAST_APP_ID)
						.setCastMediaOptions(mediaOptions)
						.build();
	}

	@Override
	public List<SessionProvider> getAdditionalSessionProviders(Context context) {
		return null;
	}

}
