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
import java.util.Date;
import java.util.TimeZone;

import free.rm.skytube.businessobjects.db.BookmarksDb;
import free.rm.skytube.businessobjects.db.SubscriptionsDb;

/**
 * A class that handles subscriptions and bookmarks databases backups.
 */
public class BackupDatabases {

	private static final File   EXPORT_DIR  = Environment.getExternalStorageDirectory();
	private static final String BACKUPS_EXT = ".skytube";

	/**
	 * Backs up the subscriptions and bookmarks databases to external storage.
	 *
	 * @return The path of the archive file generated (containing the backup).
	 * @throws IOException
	 */
	public String backupDbsToSdCard() throws IOException {
		SubscriptionsDb subscriptionsDb = SubscriptionsDb.getSubscriptionsDb();
		BookmarksDb     bookmarksDb = BookmarksDb.getBookmarksDb();
		final File      backupPath = new File(EXPORT_DIR, generateFileName());

		// close the databases
		subscriptionsDb.close();
		bookmarksDb.close();

		// backup the databases inside a zip file
		ZipFile databasesZip = new ZipFile(backupPath);
		databasesZip.zip(subscriptionsDb.getDatabasePath(), bookmarksDb.getDatabasePath());

		return backupPath.getPath();
	}



	public void importBackupDb(String backupFilePath) throws IOException {
		SubscriptionsDb subscriptionsDb = SubscriptionsDb.getSubscriptionsDb();
		BookmarksDb     bookmarksDb = BookmarksDb.getBookmarksDb();
		File            databasesDirectory = subscriptionsDb.getDatabaseDirectory();

		// close the databases
		subscriptionsDb.close();
		bookmarksDb.close();

		// backup the databases inside a zip file
		ZipFile databasesZip = new ZipFile(new File(backupFilePath));
		databasesZip.unzip(databasesDirectory);
	}



	private String generateFileName() {
		TimeZone tz = TimeZone.getTimeZone("UTC");
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd-HHmmss");
		df.setTimeZone(tz);

		return "skytube-" + df.format(new Date()) + BACKUPS_EXT;
	}

}
