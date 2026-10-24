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
    public String id = UUID.randomUUID().toString();

    /** Source url of the resource. */
    public String source;

    /** Length of the current resource. */
    public long length;

    /** Creation timestamp (also used to order missions). */
    public long timestamp;

    /** Pre-defined content type: 'v' video, 'a' audio, 's' subtitle, '?' other. */
    public char kind = '?';

    /** The final downloaded file. */
    public File targetFile;

    public long getTimestamp() {
        return timestamp;
    }

    /**
     * The file that is being written while the download is in progress. Once
     * the download finishes it is renamed to {@link #targetFile}.
     */
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
