package free.rm.skytube.gui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

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
