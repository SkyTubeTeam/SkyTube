/*
 * SkyTube
 * Copyright (C) 2017  Ramon Mifsud
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

package free.rm.skytube.gui.businessobjects.preferences;

import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;

import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.db.BookmarksDb;
import free.rm.skytube.businessobjects.db.ChannelFilteringDb;
import free.rm.skytube.businessobjects.db.PlaybackStatusDb;
import free.rm.skytube.businessobjects.db.SearchHistoryDb;
import free.rm.skytube.businessobjects.db.SubscriptionsDb;

/**
 * A class that handles subscriptions and bookmarks databases backups.
 */
public class BackupDatabases {
	private static final String TAG = BackupDatabases.class.getSimpleName();

	private static final File   EXPORT_DIR  = Environment.getExternalStorageDirectory();
	private static final String BACKUPS_EXT = ".skytube";
	public static final String PREFERENCES_JSON = "preferences.json";
	private static final int[] KEY_IDS = {
			R.string.pref_use_default_newpipe_backend,
			R.string.pref_key_subscriptions_alphabetical_order,
			R.string.pref_youtube_api_key,
			R.string.pref_key_default_content_country,
			R.string.pref_key_default_tab_name,
			R.string.pref_key_switch_volume_and_brightness,
			R.string.pref_key_disable_screen_gestures,
			R.string.pref_key_mobile_network_usage_policy,
			R.string.pref_key_download_to_separate_directories,
			R.string.pref_key_video_download_folder,
			R.string.pref_key_minimum_res,
			R.string.pref_key_maximum_res,
			R.string.pref_key_video_quality,
			R.string.pref_key_minimum_res_mobile,
			R.string.pref_key_maximum_res_mobile,
			R.string.pref_key_video_quality_on_mobile,
			R.string.pref_key_video_download_minimum_resolution,
			R.string.pref_key_video_download_maximum_resolution,
			R.string.pref_key_video_quality_for_downloads,
			R.string.pref_key_hide_tabs
	};

	/**
	 * Backs up the databases to external storage.
	 *
	 * @return The path of the archive file generated (containing the backup).
	 * @throws IOException
	 */
	public String backupDbsToSdCard() throws IOException {
		SubscriptionsDb     subscriptionsDb = SubscriptionsDb.getSubscriptionsDb();
		BookmarksDb         bookmarksDb = BookmarksDb.getBookmarksDb();
		PlaybackStatusDb    playbackDb = PlaybackStatusDb.getPlaybackStatusDb();
		ChannelFilteringDb  channelFilteringDb = ChannelFilteringDb.getChannelFilteringDb();
		SearchHistoryDb     searchHistoryDb = SearchHistoryDb.getSearchHistoryDb();

		final File          backupPath = new File(EXPORT_DIR, generateFileName());

		Gson gson = new Gson();

		// close the databases
		subscriptionsDb.close();
		bookmarksDb.close();
		playbackDb.close();
		channelFilteringDb.close();
		searchHistoryDb.close();

		try (ZipOutput databasesZip = new ZipOutput(backupPath)) {
			// backup the databases inside a zip file
			databasesZip.addFile(subscriptionsDb.getDatabasePath());
			databasesZip.addFile(bookmarksDb.getDatabasePath());
			databasesZip.addFile(playbackDb.getDatabasePath());
			databasesZip.addFile(channelFilteringDb.getDatabasePath());
			databasesZip.addFile(searchHistoryDb.getDatabasePath());

			databasesZip.addContent(PREFERENCES_JSON, gson.toJson(getImportantKeys()));
		}
		return backupPath.getPath();
	}


	private static Map<String, Object> getImportantKeys() {
		Map<String, ?> allPreferences = SkyTubeApp.getPreferenceManager().getAll();
		Map<String, Object> result = new HashMap<>();
		for (int key : KEY_IDS) {
			String keyStr = SkyTubeApp.getStr(key);
			Object value = allPreferences.get(keyStr);
			Log.i(TAG, String.format("Saving %s as %s", keyStr, value));
			result.put(keyStr, value);
		}
		return result;
	}

	/**
	 * Imports the backed-up databases.
	 *
	 * @param backupFilePath    Path to the backup file (*.skytube)
	 * @throws IOException
	 */
	public void importBackupDb(String backupFilePath) throws IOException {

		SubscriptionsDb     subscriptionsDb = SubscriptionsDb.getSubscriptionsDb();
		BookmarksDb         bookmarksDb = BookmarksDb.getBookmarksDb();
		PlaybackStatusDb    playbackDb = PlaybackStatusDb.getPlaybackStatusDb();
		ChannelFilteringDb  channelFilteringDb = ChannelFilteringDb.getChannelFilteringDb();
		SearchHistoryDb     searchHistoryDb = SearchHistoryDb.getSearchHistoryDb();

        File                databasesDirectory = subscriptionsDb.getDatabaseDirectory();

		// close the databases
		subscriptionsDb.close();
		bookmarksDb.close();
		playbackDb.close();
		channelFilteringDb.close();
		searchHistoryDb.close();

        // extract the databases from the backup zip file
        ZipFile databasesZip = new ZipFile(new File(backupFilePath));
        Map<String, ZipFile.JsonFile> result = databasesZip.unzip(databasesDirectory);
		loadPreferencesFromJson(result);
	}

	private void loadPreferencesFromJson(Map<String, ZipFile.JsonFile> result) {
		ZipFile.JsonFile jsonFile = result.get(PREFERENCES_JSON);
		if (jsonFile != null) {
			Map<String, Object> preferences = new Gson().fromJson(jsonFile.content, Map.class);

			SharedPreferences allPreferences = SkyTubeApp.getPreferenceManager();
			SharedPreferences.Editor editor = allPreferences.edit();
			for (int key : KEY_IDS) {
				String keyStr = SkyTubeApp.getStr(key);
				Object newValue = preferences.get(keyStr);
				if (newValue != null) {
					Log.i(TAG, String.format("Setting %s to %s", keyStr, newValue));
					if (newValue instanceof String) {
						editor.putString(keyStr, (String) newValue);
					} else if (newValue instanceof Long) {
						editor.putLong(keyStr, (Long) newValue);
					} else if (newValue instanceof Collection) {
						Collection values = (Collection) newValue;
						Set<String> asSet = new HashSet<>();
						for (Object value: values) {
							if (value instanceof String) {
								asSet.add((String) value);
							}
						}
						editor.putStringSet(keyStr, asSet);
					} else if (newValue instanceof Boolean) {
						editor.putBoolean(keyStr, (Boolean) newValue);
					} else {
						Log.e(TAG, String.format("Failed to set preference: %s from %s (type=%s)", keyStr, newValue, newValue.getClass()));
					}
				} else {
					Log.i(TAG, "Removing "+ keyStr );
					editor.remove(keyStr);
				}
			}
			editor.commit();
		}
	}

	private String generateFileName() {
		TimeZone tz = TimeZone.getTimeZone("UTC");
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd-HHmmss");
		df.setTimeZone(tz);

		return "skytube-" + df.format(new Date()) + BACKUPS_EXT;
	}

}
