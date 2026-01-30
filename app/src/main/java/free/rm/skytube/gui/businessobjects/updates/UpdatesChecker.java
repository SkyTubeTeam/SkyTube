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

    private URL latestApkUrl;
    private String latestApkVersion;
    private String releaseNotes;
    private final boolean fetchReleaseNotes;
    private final String currentVersionNumber;
    private boolean updatesAvailable;
    private Throwable failure;

    private final static String TAG = UpdatesChecker.class.getSimpleName();

    UpdatesChecker(boolean fetchReleaseNotes, String currentVersionNumber) {
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

        try {
            if (oss) {
                // Check F-Droid API for OSS builds (including snapshot OSS builds)
                checkFDroidForUpdates();
            } else if (!snapshot || fetchReleaseNotes) {
                // Check GitHub releases for non-OSS builds, or snapshot builds when fetchReleaseNotes is true
                JsonObject json = NewPipeService.getHttpDownloader().getJSONObject(BuildConfig.SKYTUBE_UPDATES_URL);

                latestApkVersion = getLatestVersionNumber(json);
                releaseNotes = getReleaseNotes(json);

                Log.d(TAG, "CURRENT_VER: " + currentVersionNumber);
                Log.d(TAG, "REMOTE_VER: " + latestApkVersion);

                if (!Objects.equals(currentVersionNumber, latestApkVersion)) {
                    this.latestApkUrl = getLatestApkUrl(json);
                    updatesAvailable = latestApkUrl != null;
                    Log.d(TAG, "Update available.  APK_URL: " + latestApkUrl);
                } else {
                    Log.d(TAG, "Not updating.");
                }
            } else {
                Log.d(TAG, "Snapshot version - build by Github - will not be checking for updates.");
            }
        } catch (Throwable e) {
            Log.e(TAG, "An error has occurred while checking for updates", e);
            failure = e;
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

    public Throwable getFailure() {
        return failure;
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
     * Check F-Droid API for OSS builds.
     */
    private void checkFDroidForUpdates() {
        String fdroidApiUrl = "https://f-droid.org/api/v1/packages/" + BuildConfig.APPLICATION_ID;
        Log.d(TAG, "Checking F-Droid API: " + fdroidApiUrl);

        try {
            JsonObject json = NewPipeService.getHttpDownloader().getJSONObject(fdroidApiUrl);

            // Get suggested version code from F-Droid
            int suggestedVersionCode = json.getInt("suggestedVersionCode");
            int currentVersionCode = BuildConfig.VERSION_CODE;

            Log.d(TAG, "CURRENT_VERSION_CODE: " + currentVersionCode);
            Log.d(TAG, "SUGGESTED_VERSION_CODE: " + suggestedVersionCode);

            if (suggestedVersionCode > currentVersionCode) {
                // Update available
                updatesAvailable = true;
                latestApkVersion = getVersion(json, suggestedVersionCode);
                // For OSS builds, we don't have a direct APK URL - F-Droid handles installation
                latestApkUrl = null;
                Log.d(TAG, "Update available via F-Droid. Version: " + latestApkVersion);
            } else {
                Log.d(TAG, "Not updating.");
            }
        } catch (Throwable e) {
            Log.e(TAG, "Error checking F-Droid for updates", e);
            failure = e;
        }
    }

    String getVersion(JsonObject json, int suggestedVersionCode) {
        JsonArray packages = json.getArray("packages");
        for (var i = 0; i < packages.size(); i++) {
            JsonObject pkg = packages.getObject(i);
            int versionCode = pkg.getInt("versionCode", -1);
            if (versionCode == suggestedVersionCode) {
                return pkg.getString("versionName");
            }
        }
        return null;
    }

    /**
     * Extracts from json the APK's URL of the latest version.
     *
     */
    private URL getLatestApkUrl(JsonObject json) throws MalformedURLException {
        JsonArray assets = json.getArray("assets");
        for (int i = 0; i < assets.size(); i++) {
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
