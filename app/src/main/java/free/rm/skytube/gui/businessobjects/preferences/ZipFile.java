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

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * A class that represents a zip file.  Used for database backups.
 */
public class ZipFile {

	private final File zipFilePath;

	private static final int BUFFER_SIZE = 2048;
	private static final String TAG = ZipFile.class.getSimpleName();


	/**
	 * Constructor.
	 *
	 * @param zipFilePath   The zip file.
	 */
	public ZipFile(File zipFilePath) {
		this.zipFilePath = zipFilePath;
	}


	/**
	 * Zips/Compresses the given files.
	 *
	 * @param files Files to compress.
	 */
	public void zip(String... files) throws IOException {
		FileOutputStream    dest            = new FileOutputStream(zipFilePath);
		ZipOutputStream     outputZipStream = new ZipOutputStream(new BufferedOutputStream(dest));
		byte[]              buffer            = new byte[BUFFER_SIZE];

		for (String file : files) {
			FileInputStream     fi = new FileInputStream(file);
			BufferedInputStream origin = new BufferedInputStream(fi, BUFFER_SIZE);
			ZipEntry            entry = new ZipEntry(file.substring(file.lastIndexOf("/") + 1));

			outputZipStream.putNextEntry(entry);

			int count;
			while ((count = origin.read(buffer, 0, BUFFER_SIZE)) != -1) {
				outputZipStream.write(buffer, 0, count);
			}
			origin.close();
			fi.close();

			Log.d(TAG, "Added: " + file);
		}

		outputZipStream.close();
		dest.close();
	}


	/**
	 * Unzips the given zip file to the specified extraction path.
	 *
	 * @param extractionDirectory   The directory where the files (inside the zip) will be extracted to.
	 */
	public void unzip(File extractionDirectory) throws IOException {
		FileInputStream fin             = new FileInputStream(zipFilePath);
		ZipInputStream  zipInputStream  = new ZipInputStream(fin);
		ZipEntry        zipEntry;

		while ((zipEntry = zipInputStream.getNextEntry()) != null) {
			Log.v(TAG, "Unzipping " + zipEntry.getName());

			if (zipEntry.isDirectory()) {
				throw new IllegalStateException("The zip file should not contain any directories.");
			} else {
				FileOutputStream    fout = new FileOutputStream(new File(extractionDirectory, zipEntry.getName()));
				byte[]              buffer = new byte[BUFFER_SIZE];
				int                 count;

				while ((count = zipInputStream.read(buffer)) != -1) {
					fout.write(buffer, 0, count);
				}

				zipInputStream.closeEntry();
				fout.close();
			}

		}

		zipInputStream.close();
		fin.close();
	}

}
