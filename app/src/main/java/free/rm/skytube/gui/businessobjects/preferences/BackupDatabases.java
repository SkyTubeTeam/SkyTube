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

import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
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
		BackupDataDb        backupDataDb = BackupDataDb.getBackupDataDbDb();
		final File          backupPath = new File(EXPORT_DIR, generateFileName());


		// close the databases
		subscriptionsDb.close();
		bookmarksDb.close();
		playbackDb.close();
		channelFilteringDb.close();
		searchHistoryDb.close();
		backupDataDb.close();

		// backup the databases inside a zip file
		ZipFile databasesZip = new ZipFile(backupPath);
		String[] pathArray = new String[]{subscriptionsDb.getDatabasePath(),bookmarksDb.getDatabasePath(),playbackDb.getDatabasePath(),channelFilteringDb.getDatabasePath(),searchHistoryDb.getDatabasePath(),backupDataDb.getDatabasePath()};
		for (String s: pathArray) {
			File file = new File(s);
			if (file.exists()){
				databasesZip.zip(s);
			} else {
				System.out.println("no file");
			}
		}
		/*databasesZip.zip(subscriptionsDb.getDatabasePath(),
				bookmarksDb.getDatabasePath(),
				playbackDb.getDatabasePath(),
				channelFilteringDb.getDatabasePath(),
				searchHistoryDb.getDatabasePath(),
                backupDataDb.getDatabasePath());*/

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
		BackupDataDb        backupDataDb = BackupDataDb.getBackupDataDbDb();

        //File                databasesDirectory = subscriptionsDb.getDatabaseDirectory();
        File[] dbFiles = new File[]{
                subscriptionsDb.getDatabaseDirectory(),
                bookmarksDb.getDatabaseDirectory(),
                playbackDb.getDatabaseDirectory(),
                channelFilteringDb.getDatabaseDirectory(),
                searchHistoryDb.getDatabaseDirectory(),
                backupDataDb.getDatabaseDirectory()};

        // extract the databases from the backup zip file
        ZipFile databasesZip = new ZipFile(new File(backupFilePath));
        for (File f: dbFiles) {
            databasesZip.unzip(f);
        }

		// close the databases
		subscriptionsDb.close();
		bookmarksDb.close();
		playbackDb.close();
		channelFilteringDb.close();
		searchHistoryDb.close();
		backupDataDb.close();

		SkyTubeApp.getPreferenceManager().edit().putString(SkyTubeApp.getStr(R.string.pref_youtube_api_key),backupDataDb.getBackupData().get(BackupDataTable.COL_YOUTUBE_API_KEY).getAsString()).apply();
		System.out.println("hiddenTabsDB " + Arrays.toString(backupDataDb.getBackupData().get(BackupDataTable.COL_HIDE_TABS).getAsString().split(",")));
		Set<String> hiddenTabsSet = new HashSet<>(Arrays.asList(backupDataDb.getBackupData().get(BackupDataTable.COL_HIDE_TABS).getAsString().split(",")));
		SkyTubeApp.getPreferenceManager().edit().putStringSet(SkyTubeApp.getStr(R.string.pref_key_hide_tabs), hiddenTabsSet).apply();
		SkyTubeApp.getPreferenceManager().edit().putString(SkyTubeApp.getStr(R.string.pref_key_default_tab_name),backupDataDb.getBackupData().get(BackupDataTable.COL_DEFAULT_TAB_NAME).getAsString()).apply();
		SkyTubeApp.getPreferenceManager().edit().putBoolean(SkyTubeApp.getStr(R.string.pref_key_subscriptions_alphabetical_order),backupDataDb.getBackupData().get(BackupDataTable.COL_SORT_CHANNELS).getAsBoolean()).apply();
		SkyTubeApp.getPreferenceManager().edit().putBoolean(SkyTubeApp.getStr(R.string.pref_use_newpipe_backend),backupDataDb.getBackupData().get(BackupDataTable.COL_USE_NEWPIPE_BACKEND).getAsBoolean()).apply();




	}



	private String generateFileName() {
		TimeZone tz = TimeZone.getTimeZone("UTC");
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd-HHmmss");
		df.setTimeZone(tz);

		return "skytube-" + df.format(new Date()) + BACKUPS_EXT;
	}

}
