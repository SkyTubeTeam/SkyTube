/*
 * SkyTube
 * Copyright (C) 2026  SkyTube contributors
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

package free.rm.skytube.businessobjects.download;

import androidx.annotation.NonNull;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * A seekable file stream backed by {@link RandomAccessFile}.
 *
 * <p>Ported from NewPipe's {@code us.shandian.giga.io.FileStream}, without the
 * {@code SharpStream} abstraction since SkyTube only needs plain files.</p>
 */
public class FileStream implements Closeable {

    private RandomAccessFile source;

    public FileStream(@NonNull File target) throws FileNotFoundException {
        this.source = new RandomAccessFile(target, "rw");
    }

    public FileStream(@NonNull String path) throws FileNotFoundException {
        this.source = new RandomAccessFile(path, "rw");
    }

    public int read() throws IOException {
        return source.read();
    }

    public int read(byte[] b) throws IOException {
        return source.read(b);
    }

    public int read(byte[] b, int off, int len) throws IOException {
        return source.read(b, off, len);
    }

    public long skip(long pos) throws IOException {
        return source.skipBytes((int) pos);
    }

    public long available() {
        try {
            return source.length() - source.getFilePointer();
        } catch (IOException e) {
            return 0;
        }
    }

    @Override
    public void close() {
        if (source == null) {
            return;
        }
        try {
            source.close();
        } catch (IOException err) {
            // nothing to do
        }
        source = null;
    }

    public boolean isClosed() {
        return source == null;
    }

    public void rewind() throws IOException {
        source.seek(0);
    }

    public void write(byte value) throws IOException {
        source.write(value);
    }

    public void write(byte[] buffer) throws IOException {
        source.write(buffer);
    }

    public void write(byte[] buffer, int offset, int count) throws IOException {
        source.write(buffer, offset, count);
    }

    public void setLength(long length) throws IOException {
        source.setLength(length);
    }

    public void seek(long offset) throws IOException {
        source.seek(offset);
    }

    public long length() throws IOException {
        return source.length();
    }
}
