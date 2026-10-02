/*
 * SkyTube
 * Copyright (C) 2026 SkyTube Team
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

package free.rm.skytube.gui.businessobjects.updates;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import free.rm.skytube.BuildConfig;
import free.rm.skytube.R;
import free.rm.skytube.businessobjects.AsyncTaskParallel;

/**
 * Task that opens F-Droid app or store page for SkyTube updates.
 * This is used for OSS variant updates that should be installed via F-Droid.
 */
public class OpenFDroidTask extends AsyncTaskParallel<Void, Void, Boolean> {

    private static final String TAG = OpenFDroidTask.class.getSimpleName();
    private static final String FDROID_PACKAGE = "org.fdroid.fdroid";
    private static final String FDROID_WEB_URL = "https://f-droid.org/packages/" + BuildConfig.APPLICATION_ID;
    
    private final Context context;

    public OpenFDroidTask(Context context) {
        this.context = context;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        return isFdroidInstalled();
    }

    @Override
    protected void onPostExecute(Boolean fdroidInstalled) {
        if (fdroidInstalled) {
            openFdroidApp();
        } else {
            openFdroidWebPage();
        }
    }

    /**
     * Check if F-Droid is installed on the device.
     */
    private Boolean isFdroidInstalled() {
        try {
            context.getPackageManager().getPackageInfo(FDROID_PACKAGE, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /**
     * Open F-Droid app to the SkyTube page.
     */
    private void openFdroidApp() {
        try {
            @SuppressLint("UnsafeImplicitIntentLaunch")
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("fdroidapp://package/" + BuildConfig.APPLICATION_ID));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            Log.d(TAG, "Opened F-Droid app for SkyTube update");
        } catch (Exception e) {
            Log.e(TAG, "Failed to open F-Droid app", e);
            // Fallback to web page
            openFdroidWebPage();
        }
    }

    /**
     * Open F-Droid web page in browser.
     */
    private void openFdroidWebPage() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(FDROID_WEB_URL));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            Log.d(TAG, "Opened F-Droid web page for SkyTube");
            
            // Show toast to inform user about manual installation
            showToastMessage(R.string.fdroid_web_page_opened);
        } catch (Exception e) {
            Log.e(TAG, "Failed to open F-Droid web page", e);
            showToastMessage(R.string.fdroid_open_failed);
        }
    }

    /**
     * Show toast message - extracted for better testability.
     */
    private void showToastMessage(int messageId) {
        try {
            Toast.makeText(context, messageId, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.w(TAG, "Could not show toast message", e);
        }
    }
}