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

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.google.api.client.util.IOUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.db.BackupDataDb;
import free.rm.skytube.businessobjects.db.BackupDataTable;
import free.rm.skytube.businessobjects.db.BookmarksDb;
import free.rm.skytube.businessobjects.db.ChannelFilteringDb;
import free.rm.skytube.businessobjects.db.PlaybackStatusDb;
import free.rm.skytube.businessobjects.db.SearchHistoryDb;
import free.rm.skytube.businessobjects.db.SubscriptionsDb;
import free.rm.skytube.gui.fragments.MainFragment;

import static free.rm.skytube.gui.fragments.MainFragment.BOOKMARKS_FRAGMENT;
import static free.rm.skytube.gui.fragments.MainFragment.DOWNLOADED_VIDEOS_FRAGMENT;
import static free.rm.skytube.gui.fragments.MainFragment.MOST_POPULAR_VIDEOS_FRAGMENT;
import static free.rm.skytube.gui.fragments.MainFragment.SUBSCRIPTIONS_FEED_FRAGMENT;

/**
 * A class that handles subscriptions and bookmarks databases backups.
 */
public class BackupDatabases {

	private static final File   EXPORT_DIR  = Environment.getExternalStorageDirectory();
	private static final String BACKUPS_EXT = ".skytube";

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
		//BackupDataDb        backupDataDb = BackupDataDb.getBackupDataDbDb();

		final File          backupPath = new File(EXPORT_DIR, generateFileName());
		Map<String, ?> allPreferences = SkyTubeApp.getPreferenceManager().getAll();
		/*for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
			Log.d("map values", entry.getKey() + ": " + entry.getValue().toString());
		} */

		/*Gson gson = new Gson();
		SkyTubeApp.getPreferenceManager().getAll()
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty(SkyTubeApp.getStr(R.string.pref_key_subscriptions_alphabetical_order),SkyTubeApp.getPreferenceManager().getBoolean(SkyTubeApp.getStr(R.string.pref_key_subscriptions_alphabetical_order), false));
		jsonObject.addProperty(SkyTubeApp.getStr(R.string.pref_use_newpipe_backend),SkyTubeApp.getPreferenceManager().getBoolean(SkyTubeApp.getStr(R.string.pref_use_newpipe_backend), false));
		jsonObject.addProperty(SkyTubeApp.getStr(R.string.pref_youtube_api_key),SkyTubeApp.getPreferenceManager().getString(SkyTubeApp.getStr(R.string.pref_youtube_api_key), ""));
		jsonObject.addProperty(SkyTubeApp.getStr(R.string.pref_key_default_tab_name),SkyTubeApp.getPreferenceManager().getString(SkyTubeApp.getStr(R.string.pref_key_default_tab_name), ""));
		Set<String> hiddenFragments = SkyTubeApp.getPreferenceManager().getStringSet(SkyTubeApp.getStr(R.string.pref_key_hide_tabs), new HashSet<>());
		String[] hiddenFragmentsArray = new String[5];

		if(hiddenFragments.contains(MainFragment.FEATURED_VIDEOS_FRAGMENT)){
			hiddenFragmentsArray[0] = MainFragment.FEATURED_VIDEOS_FRAGMENT;
		}
		if(hiddenFragments.contains(MOST_POPULAR_VIDEOS_FRAGMENT)){
			hiddenFragmentsArray[1] = MainFragment.MOST_POPULAR_VIDEOS_FRAGMENT;
		}

		if(hiddenFragments.contains(SUBSCRIPTIONS_FEED_FRAGMENT)){
			hiddenFragmentsArray[2] = MainFragment.SUBSCRIPTIONS_FEED_FRAGMENT;
		}

		if(hiddenFragments.contains(BOOKMARKS_FRAGMENT)){
			hiddenFragmentsArray[3] = MainFragment.BOOKMARKS_FRAGMENT;
		}

		if(hiddenFragments.contains(DOWNLOADED_VIDEOS_FRAGMENT)){
			hiddenFragmentsArray[4] = MainFragment.DOWNLOADED_VIDEOS_FRAGMENT;
		}
		jsonObject.addProperty(SkyTubeApp.getStr(R.string.pref_key_hide_tabs),Arrays.toString(hiddenFragmentsArray));*/

		// close the databases
		subscriptionsDb.close();
		bookmarksDb.close();
		playbackDb.close();
		channelFilteringDb.close();
		searchHistoryDb.close();
		//backupDataDb.close();

		ZipFile databasesZip = new ZipFile(backupPath);
		File prefFile = null;
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
			prefFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),"pref-backup.txt");
		} else {
			prefFile = new File(SkyTubeApp.getContext().getFilesDir().getPath(),"pref-backup.txt");
		}
		FileOutputStream fos = new FileOutputStream(prefFile);
		fos.write(new GsonBuilder().create().toJson(allPreferences).getBytes());
		fos.close();

		// backup the databases inside a zip file
		databasesZip.zip(subscriptionsDb.getDatabasePath(),
				bookmarksDb.getDatabasePath(),
				playbackDb.getDatabasePath(),
				channelFilteringDb.getDatabasePath(),
				searchHistoryDb.getDatabasePath(),
                //backupDataDb.getDatabasePath(),
				prefFile.getAbsolutePath());
		prefFile.delete();

		return backupPath.getPath();
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
		//BackupDataDb        backupDataDb = BackupDataDb.getBackupDataDbDb();

        File                databasesDirectory = subscriptionsDb.getDatabaseDirectory();

		// close the databases
		subscriptionsDb.close();
		bookmarksDb.close();
		playbackDb.close();
		channelFilteringDb.close();
		searchHistoryDb.close();
		//backupDataDb.close();

        // extract the databases from the backup zip file
        ZipFile databasesZip = new ZipFile(new File(backupFilePath));
        databasesZip.unzip(databasesDirectory);


        String dataFromFile = getStringFromFile(databasesDirectory.getPath()+"/pref-backup.txt");
		try {
			JSONObject jsonObject = new JSONObject(dataFromFile);

			System.out.println("jsonObj " + jsonObject.toString());
		} catch (JSONException e) {
			e.printStackTrace();
		}
		File tempFile = new File(databasesDirectory.getPath()+"/pref-backup.txt");
		tempFile.delete();
		/*JsonObject backupObject = backupDataDb.getBackupData();

		SkyTubeApp.getPreferenceManager().edit().putString(SkyTubeApp.getStr(R.string.pref_youtube_api_key),backupObject.get(BackupDataTable.COL_YOUTUBE_API_KEY).getAsString()).apply();
		String tabString = backupObject.get(BackupDataTable.COL_HIDE_TABS).getAsString();
		String[] hiddenTabs = tabString.substring(1,tabString.length()-1).split(",");
		//here we need to remove empty space before tab names before otherwise it imports wrong and hidden tabs dont' work
		hiddenTabs[1] = hiddenTabs[1].substring(1);
		hiddenTabs[2] = hiddenTabs[2].substring(1);
		hiddenTabs[3] = hiddenTabs[3].substring(1);
		hiddenTabs[4] = hiddenTabs[4].substring(1);
		
		Set<String> hiddenTabsSet = new HashSet<>();

		if(Arrays.asList(hiddenTabs).contains(MainFragment.FEATURED_VIDEOS_FRAGMENT)){
			hiddenTabsSet.add(MainFragment.FEATURED_VIDEOS_FRAGMENT);
		}
		if(Arrays.asList(hiddenTabs).contains(MOST_POPULAR_VIDEOS_FRAGMENT)){
			hiddenTabsSet.add(MainFragment.MOST_POPULAR_VIDEOS_FRAGMENT);
		}

		if(Arrays.asList(hiddenTabs).contains(SUBSCRIPTIONS_FEED_FRAGMENT)){
			hiddenTabsSet.add(MainFragment.SUBSCRIPTIONS_FEED_FRAGMENT);
		}

		if(Arrays.asList(hiddenTabs).contains(BOOKMARKS_FRAGMENT)){
			hiddenTabsSet.add(MainFragment.BOOKMARKS_FRAGMENT);
		}

		if(Arrays.asList(hiddenTabs).contains(DOWNLOADED_VIDEOS_FRAGMENT)){
			hiddenTabsSet.add(MainFragment.DOWNLOADED_VIDEOS_FRAGMENT);
		}
		SkyTubeApp.getPreferenceManager().edit().putStringSet(SkyTubeApp.getStr(R.string.pref_key_hide_tabs), hiddenTabsSet).apply();
		SkyTubeApp.getPreferenceManager().edit().putString(SkyTubeApp.getStr(R.string.pref_key_default_tab_name),backupObject.get(BackupDataTable.COL_DEFAULT_TAB_NAME).getAsString()).apply();
		SkyTubeApp.getPreferenceManager().edit().putBoolean(SkyTubeApp.getStr(R.string.pref_key_subscriptions_alphabetical_order),backupObject.get(BackupDataTable.COL_SORT_CHANNELS).getAsBoolean()).apply();
		SkyTubeApp.getPreferenceManager().edit().putBoolean(SkyTubeApp.getStr(R.string.pref_use_newpipe_backend),backupObject.get(BackupDataTable.COL_USE_NEWPIPE_BACKEND).getAsBoolean()).apply();*/
	}

	public static String convertStreamToString(InputStream is) throws IOException {
		// http://www.java2s.com/Code/Java/File-Input-Output/ConvertInputStreamtoString.htm
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		StringBuilder sb = new StringBuilder();
		String line = null;
		Boolean firstLine = true;
		while ((line = reader.readLine()) != null) {
			if(firstLine){
				sb.append(line);
				firstLine = false;
			} else {
				sb.append("\n").append(line);
			}
		}
		reader.close();
		return sb.toString();
	}

	public static String getStringFromFile (String filePath) throws IOException {
		File fl = new File(filePath);
		FileInputStream fin = new FileInputStream(fl);
		String ret = convertStreamToString(fin);
		//Make sure you close all streams.
		fin.close();
		return ret;
	}

	private String generateFileName() {
		TimeZone tz = TimeZone.getTimeZone("UTC");
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd-HHmmss");
		df.setTimeZone(tz);

		return "skytube-" + df.format(new Date()) + BACKUPS_EXT;
	}

}
