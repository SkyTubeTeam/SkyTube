/*
 * SkyTube
 * Copyright (C) 2020  Zsombor Gegesy
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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipOutput implements Closeable {
    private static final String TAG = ZipOutput.class.getSimpleName();

    private static final int BUFFER_SIZE = 2048;
    private final FileOutputStream dest;
    private final ZipOutputStream outputZipStream;

    /**
     * Constructor.
     *
     * @param zipFilePath   The zip file.
     */
    public ZipOutput(File zipFilePath) throws FileNotFoundException {
        this.dest            = new FileOutputStream(zipFilePath);
        this.outputZipStream = new ZipOutputStream(new BufferedOutputStream(dest));
    }

    public void addFile(String path) throws IOException {
        try (FileInputStream fi = new FileInputStream(path);
             BufferedInputStream origin = new BufferedInputStream(fi, BUFFER_SIZE)) {
            ZipEntry entry = new ZipEntry(path.substring(path.lastIndexOf("/") + 1));

            outputZipStream.putNextEntry(entry);

            final byte[] buffer = new byte[BUFFER_SIZE];
            int count;
            while ((count = origin.read(buffer, 0, BUFFER_SIZE)) != -1) {
                outputZipStream.write(buffer, 0, count);
            }
            outputZipStream.flush();
        }

        Log.d(TAG, "Added: " + path);
    }

    public void addContent(String name, String content) throws IOException {
        ZipEntry entry = new ZipEntry(name);
        outputZipStream.putNextEntry(entry);
        // TODO: After Android 4.4, we can use StandardCharsets
        outputZipStream.write(content.getBytes(Charsets.UTF_8));

        Log.d(TAG, "Added: " + name);
    }

    @Override
    public void close() throws IOException {
        outputZipStream.close();
        dest.close();
    }
}
