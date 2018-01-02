/*
 * SkyTube
 * Copyright (C) 2018  Ramon Mifsud
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

import android.util.Log;

import org.json.JSONObject;

import java.net.URL;

import free.rm.skytube.BuildConfig;

/**
 * Checks for app updates.
 */
public class UpdatesChecker {

	private URL		latestApkUrl = null;
	private float	latestApkVersion = 0;

	private static String TAG = UpdatesChecker.class.getSimpleName();


	/**
	 * Check for app updates.  If an update is available, {@link this#latestApkUrl} and {@link this#latestApkVersion}
	 * will be set.
	 *
	 * @return True if if an update is available;  false otherwise.
	 */
	public boolean checkForUpdates() {
		boolean updatesAvailable = false;

		if (BuildConfig.FLAVOR.equalsIgnoreCase("oss")) {
			// OSS version update checker is the responsibility of FDROID
			Log.d(TAG, "OSS version - will not be checking for updates.");
		}
		else {
			try {
				WebStream   webStream = new WebStream(BuildConfig.SKYTUBE_UPDATES_URL);
				String      updatesJSONStr = webStream.downloadRemoteTextFile();
				webStream.close();

				JSONObject  json = new JSONObject(updatesJSONStr);
				float remoteVersionNumber = getLatestVersionNumber(json);
				float currentVersionNumber = getCurrentVerNumber();

				Log.d(TAG, "CURRENT_VER: " + currentVersionNumber);
				Log.d(TAG, "REMOTE_VER: " + remoteVersionNumber);

				if (currentVersionNumber < remoteVersionNumber) {
					this.latestApkUrl = getLatestApkUrl(json);
					this.latestApkVersion = remoteVersionNumber;
					updatesAvailable = true;
					Log.d(TAG, "Update available.  APK_URL: " + latestApkUrl);
				} else {
					Log.d(TAG, "Not updating.");
				}
			} catch (Throwable e) {
				Log.e(TAG, "An error has occurred while checking for updates", e);
			}
		}

		return updatesAvailable;
	}


	public URL getLatestApkUrl() {
		return latestApkUrl;
	}

	public float getLatestApkVersion() {
		return latestApkVersion;
	}


	/**
	 * Extracts from json the latest APP's version.
	 *
	 * @param json
	 * @return
	 * @throws Exception
	 */
	private float getLatestVersionNumber(JSONObject json) throws Exception {
		String  versionNumberStr = json.getString("tag_name").substring(1);  // tag_name = "v2.0" --> so we are going to delete the 'v' character
		return Float.parseFloat(versionNumberStr);
	}


	/**
	 * Extracts from json the APK's URL of the latest version.
	 *
	 * @param json
	 * @return
	 * @throws Exception
	 */
	private URL getLatestApkUrl(JSONObject json) throws Exception {
		String apkUrl = json.getJSONArray("assets").getJSONObject(0).getString("browser_download_url");
		return new URL(apkUrl);
	}


	/**
	 * @return The current app's version number.
	 */
	private float getCurrentVerNumber() {
		String currentAppVersionStr = BuildConfig.VERSION_NAME;

		if (BuildConfig.FLAVOR.equalsIgnoreCase("extra")) {
			String[] ver = BuildConfig.VERSION_NAME.split("\\s+");
			currentAppVersionStr = ver[0];
		}

		return Float.parseFloat(currentAppVersionStr);
	}

}
