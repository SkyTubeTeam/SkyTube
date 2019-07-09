package free.rm.skytube.gui.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Activity that handles clicking on the Chromecast notification that appears when the app is casting to a Chromecast
 * and has been minimized. Clicking on the notification will resume the app.
 */
public class NotificationClickActivity extends AppCompatActivity {
	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		launchMain();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		launchMain();
	}

	private void launchMain() {
		Intent launchIntent = new Intent(this, MainActivity.class);
		launchIntent.setAction(BaseActivity.ACTION_NOTIFICATION_CLICK);
		startActivity(launchIntent);
		finish();
	}
}
