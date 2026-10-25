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

import java.io.File;
import java.io.Serializable;
import java.util.Calendar;
import java.util.UUID;

/**
 * Base class for a download mission.
 *
 * <p>Ported from NewPipe's {@code us.shandian.giga.get.Mission}.</p>
 */
public abstract class Mission implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Unique id of this mission (also used to route completion callbacks). */
    private String id;

    /** Source url of the resource. */
    private String source;

    /** Length of the current resource. */
    private long length;

    /** Creation timestamp (also used to order missions). */
    private long timestamp;

    /** Pre-defined content type: 'v' video, 'a' audio, 's' subtitle, '?' other. */
    private char kind;

    /** The final downloaded file. */
    private File targetFile;

    protected Mission() {
        this.id = UUID.randomUUID().toString();
        this.kind = '?';
    }

    protected Mission(String id, String source, long length, long timestamp, char kind, File targetFile) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.source = source;
        this.length = length;
        this.timestamp = timestamp;
        this.kind = kind;
        this.targetFile = targetFile;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public long getLength() {
        return length;
    }

    public void setLength(long length) {
        this.length = length;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public char getKind() {
        return kind;
    }

    public void setKind(char kind) {
        this.kind = kind;
    }

    public File getTargetFile() {
        return targetFile;
    }

    public void setTargetFile(File targetFile) {
        this.targetFile = targetFile;
    }

    @NonNull
    public File getPartFile() {
        File parent = targetFile.getParentFile();
        return new File(parent, targetFile.getName() + ".part");
    }

    /**
     * Delete the downloaded file(s).
     *
     * @return {@code true} if and only if the files are successfully deleted.
     */
    public boolean delete() {
        DownloadUtils.deleteQuietly(getPartFile());
        DownloadUtils.deleteQuietly(targetFile);
        return true;
    }

    @NonNull
    @Override
    public String toString() {
        final Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        return "[" + calendar.getTime() + "] " + targetFile;
    }
}