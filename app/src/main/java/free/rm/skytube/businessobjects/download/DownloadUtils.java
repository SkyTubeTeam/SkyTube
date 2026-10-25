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
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
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

    /** Magic number ("SKYT") that identifies a binary mission metadata file. */
    private static final int MISSION_MAGIC = 0x534B5954;

    /** Version of the binary mission metadata format. */
    private static final int MISSION_VERSION = 1;

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

    /**
     * Persists the resumable state of the given mission to {@code file}.
     *
     * <p>The mission is written with a compact binary format instead of Java
     * object serialization.  Only the fields needed to resume a download are
     * stored; the transient runtime state (threads, listeners, handler, ...) is
     * recreated when the mission is read back.</p>
     */
    static void writeToFile(@NonNull File file, @NonNull DownloadMission mission) {
        try (DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(file)))) {
            out.writeInt(MISSION_MAGIC);
            out.writeInt(MISSION_VERSION);
            writeMission(out, mission);
        } catch (Exception e) {
            Logger.e(DownloadUtils.class, "Unable to persist download metadata", e);
        }
    }

    /**
     * Reads a mission previously stored by
     * {@link #writeToFile(File, DownloadMission)}.
     *
     * @return the restored mission, or {@code null} if the file is missing, has
     *         an unknown format, or cannot be read
     */
    @Nullable
    static DownloadMission readFromFile(@Nullable File file) {
        if (file == null || !file.isFile()) {
            return null;
        }

        try (DataInputStream in = new DataInputStream(
                new BufferedInputStream(new FileInputStream(file)))) {
            if (in.readInt() != MISSION_MAGIC) {
                Logger.e(DownloadUtils.class, "Not a mission metadata file: %s", file);
                return null;
            }

            int version = in.readInt();
            if (version != MISSION_VERSION) {
                Logger.e(DownloadUtils.class, "Unsupported mission metadata version: %d", version);
                return null;
            }

            return readMission(in);
        } catch (Exception e) {
            Logger.e(DownloadUtils.class, "Failed to read the download metadata", e);
            return null;
        }
    }

    private static void writeMission(DataOutputStream out, DownloadMission mission) throws IOException {
        // Mission
        writeString(out, mission.getId());
        writeString(out, mission.getSource());
        out.writeLong(mission.getLength());
        out.writeLong(mission.getTimestamp());
        out.writeChar(mission.getKind());
        writeString(out, mission.getTargetFile() == null ? null : mission.getTargetFile().getPath());

        // DownloadMission
        writeStringArray(out, mission.getUrls());
        out.writeLong(mission.getDone());
        out.writeBoolean(mission.getUnknownLength());
        writeLongArray(out, mission.getOffsets());
        out.writeInt(mission.getCurrent());
        out.writeLong(mission.getNearLength());
        writeIntArray(out, mission.getBlocks());
        out.writeLong(mission.getFallbackResumeOffset());
        out.writeInt(mission.getThreadCount());
        writeString(out, mission.getTitle());
        writeString(out, mission.getNotificationGroupId());
        writeString(out, mission.getNotificationGroupTitle());
        out.writeInt(mission.getNotificationGroupSize());
        out.writeBoolean(mission.getAllowedOverRoaming());
        out.writeInt(mission.getAllowedNetworkTypes());
        out.writeBoolean(mission.getEnqueued());
        // only the error code is persisted: errObject is runtime-only state
        out.writeInt(mission.getErrCode());
    }

    private static DownloadMission readMission(DataInputStream in) throws IOException {
        // Mission
        String id = readString(in);
        String source = readString(in);
        long length = in.readLong();
        long timestamp = in.readLong();
        char kind = in.readChar();
        String targetPath = readString(in);
        File targetFile = targetPath == null ? null : new File(targetPath);

        // DownloadMission
        String[] urls = readStringArray(in);
        long done = in.readLong();
        boolean unknownLength = in.readBoolean();
        long[] offsets = readLongArray(in);
        int current = in.readInt();
        long nearLength = in.readLong();
        int[] blocks = readIntArray(in);
        long fallbackResumeOffset = in.readLong();
        int threadCount = in.readInt();
        String title = readString(in);
        String notificationGroupId = readString(in);
        String notificationGroupTitle = readString(in);
        int notificationGroupSize = in.readInt();
        boolean allowedOverRoaming = in.readBoolean();
        int allowedNetworkTypes = in.readInt();
        boolean enqueued = in.readBoolean();
        int errCode = in.readInt();

        return new DownloadMission(id, source, length, timestamp, kind, targetFile, urls, done,
                unknownLength, offsets, current, nearLength, blocks, fallbackResumeOffset,
                threadCount, title, notificationGroupId, notificationGroupTitle,
                notificationGroupSize, allowedOverRoaming, allowedNetworkTypes, enqueued, errCode);
    }

    /**
     * Writes a nullable string: a negative length denotes {@code null}, causing
     * the value to be encoded as UTF-8 bytes so it is not limited by the
     * 64&nbsp;KiB cap of {@link DataOutputStream#writeUTF(String)}.
     */
    private static void writeString(DataOutputStream out, @Nullable String value) throws IOException {
        if (value == null) {
            out.writeInt(-1);
            return;
        }

        byte[] bytes = value.getBytes(UTF_8);
        out.writeInt(bytes.length);
        out.write(bytes);
    }

    @Nullable
    private static String readString(DataInputStream in) throws IOException {
        int length = in.readInt();
        if (length < 0) {
            return null;
        }

        byte[] bytes = new byte[length];
        in.readFully(bytes);
        return new String(bytes, UTF_8);
    }

    private static void writeStringArray(DataOutputStream out, @Nullable String[] values) throws IOException {
        if (values == null) {
            out.writeInt(-1);
            return;
        }

        out.writeInt(values.length);
        for (String value : values) {
            writeString(out, value);
        }
    }

    @Nullable
    private static String[] readStringArray(DataInputStream in) throws IOException {
        int length = in.readInt();
        if (length < 0) {
            return null;
        }

        String[] values = new String[length];
        for (int i = 0; i < length; i++) {
            values[i] = readString(in);
        }
        return values;
    }

    private static void writeLongArray(DataOutputStream out, @Nullable long[] values) throws IOException {
        if (values == null) {
            out.writeInt(-1);
            return;
        }

        out.writeInt(values.length);
        for (long value : values) {
            out.writeLong(value);
        }
    }

    @Nullable
    private static long[] readLongArray(DataInputStream in) throws IOException {
        int length = in.readInt();
        if (length < 0) {
            return null;
        }

        long[] values = new long[length];
        for (int i = 0; i < length; i++) {
            values[i] = in.readLong();
        }
        return values;
    }

    private static void writeIntArray(DataOutputStream out, @Nullable int[] values) throws IOException {
        if (values == null) {
            out.writeInt(-1);
            return;
        }

        out.writeInt(values.length);
        for (int value : values) {
            out.writeInt(value);
        }
    }

    @Nullable
    private static int[] readIntArray(DataInputStream in) throws IOException {
        int length = in.readInt();
        if (length < 0) {
            return null;
        }

        int[] values = new int[length];
        for (int i = 0; i < length; i++) {
            values[i] = in.readInt();
        }
        return values;
    }

    @Nullable
    static String getFileExt(String url) {
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
    static boolean mkdir(File dir, boolean allDirs) {
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

    static long getContentLength(HttpURLConnection connection) {
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
    static long getTotalContentLength(HttpURLConnection connection) {
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
    static File getMetadataFile(File pendingDir, File targetFile) {
        UUID uuid = UUID.nameUUIDFromBytes(targetFile.getAbsolutePath().getBytes(UTF_8));
        return new File(pendingDir, uuid.toString() + ".meta");
    }

    static void deleteQuietly(@Nullable File file) {
        if (file != null && file.exists() && !file.delete()) {
            Logger.w(DownloadUtils.class, "Could not delete file: %s", file);
        }
    }

    /**
     * Renames the completed temporary file to its final name.
     */
    static boolean moveCompletedFile(@NonNull File source, @NonNull File target) {
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
