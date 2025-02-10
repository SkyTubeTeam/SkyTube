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

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

import free.rm.skytube.BuildConfig;
import free.rm.skytube.businessobjects.YouTube.newpipe.NewPipeService;

/**
 * Checks for app updates.
 */
public class UpdatesChecker {

	private URL		latestApkUrl;
	private String	latestApkVersion;
	private String	releaseNotes;
	private final boolean	fetchReleaseNotes;
	private final String	currentVersionNumber;
	private boolean updatesAvailable;

	private final static String TAG = UpdatesChecker.class.getSimpleName();

	UpdatesChecker(boolean fetchReleaseNotes, String	currentVersionNumber) {
		this.fetchReleaseNotes = fetchReleaseNotes;
		this.currentVersionNumber = currentVersionNumber;
	}

	/**
	 * Check for app updates.  If an update is available, {@link this#latestApkUrl} and {@link this#latestApkVersion}
	 * will be set.
	 *
	 */
	public void checkForUpdates() {
		updatesAvailable = false;
		boolean oss = BuildConfig.FLAVOR.equalsIgnoreCase("oss");
		boolean snapshot = BuildConfig.BUILD_TYPE.equalsIgnoreCase("snapshot");

		if ((oss || snapshot) && !fetchReleaseNotes) {
			if (oss) {
				// OSS version update checker is the responsibility of FDROID
				Log.d(TAG, "OSS version - will not be checking for updates.");
			} else if (snapshot) {
				Log.d(TAG, "Snapshot version - build by Github - will not be checking for updates.");
			}
		} else {
			try {
				JsonObject json = NewPipeService.getHttpDownloader().getJSONObject(BuildConfig.SKYTUBE_UPDATES_URL);

				latestApkVersion = getLatestVersionNumber(json);
				releaseNotes = getReleaseNotes(json);

				Log.d(TAG, "CURRENT_VER: " + currentVersionNumber);
				Log.d(TAG, "REMOTE_VER: " + latestApkVersion);

				if (!oss) {
					if (!Objects.equals(currentVersionNumber, latestApkVersion)) {
						this.latestApkUrl = getLatestApkUrl(json);
						updatesAvailable = latestApkUrl != null;
						Log.d(TAG, "Update available.  APK_URL: " + latestApkUrl);
					} else {
						Log.d(TAG, "Not updating.");
					}
				}
			} catch (Throwable e) {
				Log.e(TAG, "An error has occurred while checking for updates", e);
			}
		}
	}


	public URL getLatestApkUrl() {
		return latestApkUrl;
	}

	public String getLatestApkVersion() {
		return latestApkVersion;
	}

	public String getReleaseNotes() {
		return releaseNotes;
	}

	public boolean isUpdateAvailable() {
		return updatesAvailable;
	}

	/**
	 * Extracts from json the latest APP's version.
	 *
	 * @param json
	 * @return
	 */
	private String getLatestVersionNumber(JsonObject json) {
		return json.getString("tag_name").substring(1);
	}


	private String getReleaseNotes(JsonObject json) {
		return json.getString("body");
	}

	/**
	 * Extracts from json the APK's URL of the latest version.
	 *
	 * @param json
	 * @return
	 * @throws JSONException
	 */
	private URL getLatestApkUrl(JsonObject json) throws MalformedURLException {
		JsonArray assets = json.getArray("assets");
		for (int i=0; i < assets.size();i ++) {
            JsonObject asset = assets.getObject(i);
			String name = asset.getString("name");
			if (name != null) {
				if (name.toLowerCase().startsWith("skytube-" + BuildConfig.FLAVOR + "-")) {
					return new URL(asset.getString("browser_download_url"));
				}
			}
		}
		return null;
	}
}
