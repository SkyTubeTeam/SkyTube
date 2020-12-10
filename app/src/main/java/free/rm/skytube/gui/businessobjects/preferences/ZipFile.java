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

import org.apache.commons.codec.Charsets;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * A class that represents a zip file.  Used for database backups.
 */
public class ZipFile {

	static class JsonFile {
		String name;
		String content;

		public JsonFile(String name, String content) {
			this.name = name;
			this.content = content;
		}
	}
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
	 * Unzips the given zip file to the specified extraction path.
	 *
	 * @param extractionDirectory   The directory where the files (inside the zip) will be extracted to.
	 */
	public Map<String, JsonFile> unzip(File extractionDirectory) throws IOException {
		try (FileInputStream fin = new FileInputStream(zipFilePath);
			 ZipInputStream zipInputStream = new ZipInputStream(fin)) {
			ZipEntry zipEntry;

			Map<String, JsonFile> result = new HashMap<>();
			while ((zipEntry = zipInputStream.getNextEntry()) != null) {
				Log.v(TAG, "Unzipping " + zipEntry.getName());

				if (zipEntry.isDirectory()) {
					throw new IllegalStateException("The zip file should not contain any directories.");
				} else {
					if (zipEntry.getName().toLowerCase().endsWith(".json")) {
						if (zipEntry.getSize() < 100_000) {
							ByteArrayOutputStream sw = new ByteArrayOutputStream();
							copyStream(zipInputStream, sw);
							result.put(
									zipEntry.getName().toLowerCase(),
									new JsonFile(zipEntry.getName(), new String(sw.toByteArray(), Charsets.UTF_8)));
						}
					} else {
						FileOutputStream fout = new FileOutputStream(new File(extractionDirectory, zipEntry.getName()));
						copyStream(zipInputStream, fout);
					}
				}
			}
			return result;
		}
	}

	private void copyStream(ZipInputStream zipInputStream, OutputStream fout) throws IOException {
		byte[]              buffer = new byte[BUFFER_SIZE];
		int                 count;

		while ((count = zipInputStream.read(buffer)) != -1) {
			fout.write(buffer, 0, count);
		}

		zipInputStream.closeEntry();
		fout.close();
	}


}
