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

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.UUID;

import free.rm.skytube.businessobjects.Logger;

/**
 * Utility methods used by the download engine.
 *
 * <p>This is a Java port of the helper methods used by NewPipe's downloader
 * ({@code us.shandian.giga.util.Utility}), adapted to SkyTube's plain
 * {@link File} based storage.</p>
 */
public final class DownloadUtils {

    /**
     * User agent used for the download requests. YouTube rejects some requests
     * coming from the default Android user agent, so a desktop Firefox agent is
     * used, exactly like NewPipe does.
     */
    public static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:153.0) Gecko/20100101 Firefox/153.0";

    private static final Charset UTF_8 = Charset.forName("UTF-8");

    private DownloadUtils() {
    }

    public static String formatBytes(long bytes) {
        Locale locale = Locale.getDefault();
        if (bytes < 1024) {
            return String.format(locale, "%d B", bytes);
        } else if (bytes < 1024 * 1024) {
            return String.format(locale, "%.2f kB", bytes / 1024d);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format(locale, "%.2f MB", bytes / 1024d / 1024d);
        } else {
            return String.format(locale, "%.2f GB", bytes / 1024d / 1024d / 1024d);
        }
    }

    public static void writeToFile(@NonNull File file, @NonNull Serializable serializable) {
        try (ObjectOutputStream objectOutputStream =
                     new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
            objectOutputStream.writeObject(serializable);
        } catch (Exception e) {
            Logger.e(DownloadUtils.class, "Unable to persist download metadata", e);
        }
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static <T> T readFromFile(@Nullable File file) {
        if (file == null || !file.isFile()) {
            return null;
        }

        try (ObjectInputStream objectInputStream =
                     new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            return (T) objectInputStream.readObject();
        } catch (Exception e) {
            Logger.e(DownloadUtils.class, "Failed to deserialize the download metadata", e);
            return null;
        }
    }

    @Nullable
    public static String getFileExt(String url) {
        int index;
        if ((index = url.indexOf("?")) > -1) {
            url = url.substring(0, index);
        }
        if ((index = url.indexOf("#")) > -1) {
            url = url.substring(0, index);
        }

        index = url.lastIndexOf("/");
        String fileName = index == -1 ? url : url.substring(index + 1);

        index = fileName.lastIndexOf(".");
        if (index == -1) {
            return null;
        }

        String ext = fileName.substring(index);
        if ((index = ext.indexOf("%")) > -1) {
            ext = ext.substring(0, index);
        }
        return ext.toLowerCase(Locale.ROOT);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static boolean mkdir(File dir, boolean allDirs) {
        if (dir.exists()) {
            return true;
        }

        if (allDirs) {
            dir.mkdirs();
        } else {
            dir.mkdir();
        }

        return dir.exists();
    }

    public static long getContentLength(HttpURLConnection connection) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return connection.getContentLengthLong();
        }

        try {
            return Long.parseLong(connection.getHeaderField("Content-Length"));
        } catch (Exception err) {
            return -1;
        }
    }

    /**
     * Get the content length of the entire file even if the HTTP response is
     * partial (response code 206).
     */
    public static long getTotalContentLength(HttpURLConnection connection) {
        try {
            if (connection.getResponseCode() == 206) {
                final String rangeStr = connection.getHeaderField("Content-Range");
                final String bytesStr = rangeStr.split("/", 2)[1];
                return Long.parseLong(bytesStr);
            } else {
                return getContentLength(connection);
            }
        } catch (Exception err) {
            return -1;
        }
    }

    /**
     * Creates a deterministic metadata file name for the given target file.
     * Using the target path guarantees that two attempts to download the same
     * file share the same resume state.
     */
    public static File getMetadataFile(File pendingDir, File targetFile) {
        UUID uuid = UUID.nameUUIDFromBytes(targetFile.getAbsolutePath().getBytes(UTF_8));
        return new File(pendingDir, uuid.toString() + ".meta");
    }

    public static void deleteQuietly(@Nullable File file) {
        if (file != null && file.exists() && !file.delete()) {
            Logger.w(DownloadUtils.class, "Could not delete file: %s", file);
        }
    }

    /**
     * Renames the completed temporary file to its final name.
     */
    public static boolean moveCompletedFile(@NonNull File source, @NonNull File target) {
        if (!source.isFile()) {
            return false;
        }

        if (target.exists() && !target.delete()) {
            Logger.e(DownloadUtils.class, "Unable to replace existing file: %s", target);
            return false;
        }

        if (source.renameTo(target)) {
            return true;
        }

        // Some file systems refuse to rename between directories. Fall back to
        // a copy + delete, which is slower but reliable.
        try (FileInputStream in = new FileInputStream(source);
             FileOutputStream out = new FileOutputStream(target)) {
            byte[] buffer = new byte[128 * 1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            out.getFD().sync();
        } catch (IOException e) {
            Logger.e(DownloadUtils.class, "Unable to move completed download", e);
            return false;
        }

        return source.delete();
    }
}
