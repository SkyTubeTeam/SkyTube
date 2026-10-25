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

import android.app.DownloadManager;
import android.system.ErrnoException;
import android.system.OsConstants;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.channels.ClosedByInterruptException;
import java.util.Objects;

import javax.net.ssl.SSLException;

import free.rm.skytube.businessobjects.Logger;

/**
 * A resumable, multi-threaded download mission.
 *
 * <p>Ported from NewPipe's {@code us.shandian.giga.get.DownloadMission}. The
 * storage and post-processing abstractions were replaced with plain
 * {@link File} handling, as SkyTube stores the audio and video streams as
 * separate files and lets ExoPlayer play them together.</p>
 */
public class DownloadMission extends Mission {
    private static final long serialVersionUID = 1L;

    static final int BUFFER_SIZE = 64 * 1024;
    static final int BLOCK_SIZE = 512 * 1024;

    private static final String TAG = "DownloadMission";

    public static final int ERROR_NOTHING = -1;
    public static final int ERROR_PATH_CREATION = 1000;
    public static final int ERROR_FILE_CREATION = 1001;
    public static final int ERROR_UNKNOWN_EXCEPTION = 1002;
    public static final int ERROR_PERMISSION_DENIED = 1003;
    public static final int ERROR_SSL_EXCEPTION = 1004;
    public static final int ERROR_UNKNOWN_HOST = 1005;
    public static final int ERROR_CONNECT_HOST = 1006;
    public static final int ERROR_INSUFFICIENT_STORAGE = 1010;
    public static final int ERROR_PROGRESS_LOST = 1011;
    public static final int ERROR_TIMEOUT = 1012;
    public static final int ERROR_RESOURCE_GONE = 1013;
    public static final int ERROR_HTTP_NO_CONTENT = 204;
    static final int ERROR_HTTP_FORBIDDEN = 403;

    /** The urls of the file(s) to download. */
    public String[] urls;

    /** Number of bytes downloaded and written. */
    public volatile long done;

    /** Indicates a file generated dynamically on the web server. */
    public boolean unknownLength;

    /** Offset in the file where the data should be written. */
    public long[] offsets;

    /** The current resource to download, {@code urls[current]} and {@code offsets[current]}. */
    public int current;

    /** Metadata where the mission state is saved. */
    public transient File metadata;

    /** Maximum attempts. */
    public transient int maxRetry = 3;

    /** Approximated final length, this represents the sum of all resources sizes. */
    public long nearLength;

    /**
     * Download blocks, the size is a multiple of {@link #BLOCK_SIZE}. Every
     * entry (block) holds an offset used to resume the download. A block offset
     * can be -1 if the block was downloaded successfully.
     */
    int[] blocks;

    /** Download/File resume offset in fallback mode. */
    volatile long fallbackResumeOffset;

    /** Maximum number of download threads running, chosen by the user. */
    public int threadCount = 3;

    /** Human readable title used by notifications. */
    public String title;

    /**
     * Identifier of the logical download that this mission belongs to.
     * Missions that share an identifier are aggregated in one notification.
     */
    public String notificationGroupId;

    /** Human readable title shared by all missions of the same notification group. */
    public String notificationGroupTitle;

    /** Number of missions that make up the notification group. */
    public int notificationGroupSize;

    /** Whether the download may continue while the device is roaming. */
    public boolean allowedOverRoaming = true;

    /** Network types allowed by the caller (DownloadManager.Request.NETWORK_*). */
    public int allowedNetworkTypes =
            DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE;

    private transient int finishCount;
    public transient volatile boolean running;
    public boolean enqueued;

    public int errCode = ERROR_NOTHING;
    public Exception errObject = null;

    public transient HandlerThreadFactory handler;
    private transient boolean[] blockAcquired;

    private transient long writingToFileNext;
    private transient volatile boolean writingToFile;

    final Object LOCK = new Lock();

    @NonNull
    public transient Thread[] threads = new Thread[0];
    public transient Thread init = null;

    private transient Listener listener;

    public DownloadMission(String[] urls, File targetFile) {
        if (Objects.requireNonNull(urls).length < 1) {
            throw new IllegalArgumentException("urls array is empty");
        }
        this.urls = urls;
        this.offsets = new long[urls.length];
        this.enqueued = true;
        this.targetFile = Objects.requireNonNull(targetFile, "targetFile");
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Creates an empty mission whose persisted state is filled in by the
     * binary metadata reader ({@link DownloadUtils#readFromFile(File)}).  The
     * transient runtime state is left at its default value.
     */
    DownloadMission() {
    }

    /**
     * Restores the transient runtime state that is not carried through Java
     * serialization.  This is used when a mission is handed to the download
     * service through an {@link android.content.Intent}; without it, fields
     * such as {@link #threads} remain {@code null} and crash the mission on
     * resume.
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        metadata = null;
        maxRetry = 3;
        running = false;
        handler = null;
        blockAcquired = null;
        writingToFileNext = 0;
        writingToFile = false;
        threads = new Thread[0];
        init = null;
        listener = null;
    }

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }

    @Nullable
    public Listener getListener() {
        return listener;
    }

    /**
     * Acquire a block.
     *
     * @return the block or {@code null} if no more blocks are left
     */
    @Nullable
    Block acquireBlock() {
        synchronized (LOCK) {
            for (int i = 0; i < blockAcquired.length; i++) {
                if (!blockAcquired[i] && blocks[i] >= 0) {
                    Block block = new Block();
                    block.position = i;
                    block.done = blocks[i];

                    blockAcquired[i] = true;
                    return block;
                }
            }
        }

        return null;
    }

    /**
     * Release a block.
     *
     * @param position the index of the block
     * @param done     amount of bytes downloaded
     */
    void releaseBlock(int position, int done) {
        synchronized (LOCK) {
            blockAcquired[position] = false;
            blocks[position] = done;
        }
    }

    HttpURLConnection openConnection(boolean headRequest, long rangeStart, long rangeEnd) throws IOException {
        return openConnection(urls[current], headRequest, rangeStart, rangeEnd);
    }

    HttpURLConnection openConnection(String url, boolean headRequest, long rangeStart, long rangeEnd) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setInstanceFollowRedirects(true);
        conn.setRequestProperty("User-Agent", DownloadUtils.USER_AGENT);
        conn.setRequestProperty("Accept", "*/*");
        conn.setRequestProperty("Accept-Encoding", "*");

        if (headRequest) {
            conn.setRequestMethod("HEAD");
        }

        // Workaround: switching between networks can freeze the download forever
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(60000);

        if (rangeStart >= 0) {
            String req = "bytes=" + rangeStart + "-";
            if (rangeEnd > 0) {
                req += rangeEnd;
            }

            conn.setRequestProperty("Range", req);
        }

        return conn;
    }

    /**
     * @param threadId id of the calling thread
     * @param conn     opened connection
     * @throws IOException if an error occurred connecting to the server.
     * @throws HttpError   if the HTTP Status-Code is not satisfiable
     */
    void establishConnection(int threadId, HttpURLConnection conn) throws IOException, HttpError {
        int statusCode = conn.getResponseCode();

        switch (statusCode) {
            case 204:
            case 205:
            case 207:
                throw new HttpError(statusCode);
            case 416:
                return; // let the download thread handle this error
            default:
                if (statusCode < 200 || statusCode > 299) {
                    throw new HttpError(statusCode);
                }
        }
    }

    private void notify(int what) {
        if (handler != null) {
            handler.sendMessage(what, this);
        }
    }

    synchronized void notifyProgress(long deltaLen) {
        if (unknownLength) {
            length += deltaLen; // Update length before proceeding
        }

        done += deltaLen;

        if (listener != null) {
            listener.onProgress(this);
        }

        if (metadata == null) {
            return;
        }

        if (!writingToFile && (done > writingToFileNext || deltaLen < 0)) {
            writingToFile = true;
            writingToFileNext = done + BLOCK_SIZE;
            writeThisToFileAsync();
        }
    }

    synchronized void notifyError(Exception err) {
        Logger.e(TAG, "notifyError()", err);

        if (err instanceof FileNotFoundException) {
            notifyError(ERROR_FILE_CREATION, null);
        } else if (err instanceof SSLException) {
            notifyError(ERROR_SSL_EXCEPTION, null);
        } else if (err instanceof HttpError) {
            notifyError(((HttpError) err).statusCode, null);
        } else if (err instanceof ConnectException) {
            notifyError(ERROR_CONNECT_HOST, null);
        } else if (err instanceof UnknownHostException) {
            notifyError(ERROR_UNKNOWN_HOST, null);
        } else if (err instanceof SocketTimeoutException) {
            notifyError(ERROR_TIMEOUT, null);
        } else {
            notifyError(ERROR_UNKNOWN_EXCEPTION, err);
        }
    }

    public synchronized void notifyError(int code, Exception err) {
        Logger.e(TAG, "notifyError() code = " + code, err);
        if (err != null && err.getCause() instanceof ErrnoException) {
            int errno = ((ErrnoException) err.getCause()).errno;
            if (errno == OsConstants.ENOSPC) {
                code = ERROR_INSUFFICIENT_STORAGE;
                err = null;
            } else if (errno == OsConstants.EACCES) {
                code = ERROR_PERMISSION_DENIED;
                err = null;
            }
        }

        if (err instanceof IOException) {
            String message = err.getMessage();
            if (message != null && message.contains("Permission denied")) {
                code = ERROR_PERMISSION_DENIED;
                err = null;
            } else if (message != null && message.contains("ENOSPC")) {
                code = ERROR_INSUFFICIENT_STORAGE;
                err = null;
            } else if (targetFile == null || targetFile.getParentFile() == null
                    || !targetFile.getParentFile().canWrite()) {
                code = ERROR_FILE_CREATION;
                err = null;
            }
        }

        errCode = code;
        errObject = err;

        switch (code) {
            case ERROR_SSL_EXCEPTION:
            case ERROR_UNKNOWN_HOST:
            case ERROR_CONNECT_HOST:
            case ERROR_TIMEOUT:
                // do not change the queue flag for network errors, can be
                // recovered silently without the user interaction
                break;
            default:
                // also checks for server errors
                if (code < 500 || code > 599) {
                    enqueued = false;
                }
        }

        notify(DownloadService.MESSAGE_ERROR);

        if (running) {
            pauseThreads();
        }
    }

    synchronized void notifyFinished() {
        if (current < urls.length) {
            if (++finishCount < threads.length) {
                return;
            }

            current++;
            if (current < urls.length) {
                // prepare next sub-mission
                offsets[current] = offsets[current - 1] + length;
                initializer();
                return;
            }
        }

        // this mission is fully finished
        unknownLength = false;
        enqueued = false;
        running = false;

        if (!DownloadUtils.moveCompletedFile(getPartFile(), targetFile)) {
            notifyError(ERROR_FILE_CREATION, new IOException("Cannot move the downloaded file to " + targetFile));
            return;
        }

        deleteThisFromFile();
        notify(DownloadService.MESSAGE_FINISHED);
    }

    /**
     * Start downloading with multiple threads.
     */
    public void start() {
        if (running || isFinished() || urls.length < 1) {
            return;
        }

        // ensure that the previous state is completely paused.
        joinForThreads(10000);

        running = true;
        errCode = ERROR_NOTHING;

        if (hasInvalidStorage()) {
            notifyError(ERROR_FILE_CREATION, null);
            return;
        }

        if (current >= urls.length) {
            notifyFinished();
            return;
        }

        notify(DownloadService.MESSAGE_RUNNING);

        if (urls[current] == null) {
            notifyError(ERROR_RESOURCE_GONE, null);
            return;
        }

        if (blocks == null) {
            initializer();
            return;
        }

        init = null;
        finishCount = 0;
        blockAcquired = new boolean[blocks.length];

        if (blocks.length < 1) {
            threads = new Thread[]{runAsync(1, new DownloadRunnableFallback(this))};
        } else {
            int remainingBlocks = 0;
            for (int block : blocks) {
                if (block >= 0) {
                    remainingBlocks++;
                }
            }

            if (remainingBlocks < 1) {
                notifyFinished();
                return;
            }

            threads = new Thread[Math.min(threadCount, remainingBlocks)];

            for (int i = 0; i < threads.length; i++) {
                threads[i] = runAsync(i + 1, new DownloadRunnable(this, i));
            }
        }
    }

    /**
     * Pause the mission.
     */
    public void pause() {
        if (!running) {
            return;
        }

        running = false;
        notify(DownloadService.MESSAGE_PAUSED);

        if (init != null && init.isAlive()) {
            init.interrupt();
            synchronized (LOCK) {
                resetState(false, true, ERROR_NOTHING);
            }
            return;
        }

        init = null;
        pauseThreads();
    }

    private void pauseThreads() {
        running = false;
        joinForThreads(-1);
        writeThisToFile();
    }

    /**
     * Removes the downloaded file and the metadata file.
     */
    @Override
    public boolean delete() {
        notify(DownloadService.MESSAGE_DELETED);

        boolean res = deleteThisFromFile();

        return super.delete() && res;
    }

    /**
     * Resets the mission state.
     *
     * @param rollback       {@code true} to forget all progress, otherwise {@code false}
     * @param persistChanges {@code true} to commit changes to the metadata file
     */
    public void resetState(boolean rollback, boolean persistChanges, int errorCode) {
        length = 0;
        current = 0;
        finishCount = 0;
        errCode = errorCode;
        errObject = null;
        unknownLength = false;
        threads = new Thread[0];
        fallbackResumeOffset = 0;
        blocks = null;
        blockAcquired = null;

        if (rollback) {
            current = 0;
            done = 0;
            writingToFileNext = 0;
        }
        if (persistChanges) {
            writeThisToFile();
        }
    }

    private void initializer() {
        init = runAsync(DownloadInitializer.mId, new DownloadInitializer(this));
    }

    private void writeThisToFileAsync() {
        runAsync(-2, this::writeThisToFile);
    }

    /**
     * Write this {@link DownloadMission} to the metadata file.
     */
    void writeThisToFile() {
        synchronized (LOCK) {
            if (metadata == null) {
                return;
            }
            DownloadUtils.writeToFile(metadata, this);
            writingToFile = false;
        }
    }

    /**
     * @return true if the download is fully finished.
     */
    public boolean isFinished() {
        if (current < urls.length) {
            return false;
        }
        // once every sub-mission finished the file was (or should have been)
        // renamed to its final name; a missing target file means the
        // completion was lost (e.g. the process died) and the mission can be
        // restarted
        return targetFile != null && targetFile.exists();
    }

    /**
     * @return true if the mission has been initialized.
     */
    public boolean isInitialized() {
        return blocks != null;
    }

    /**
     * Gets the approximated final length of the file.
     */
    public long getLength() {
        if (length <= 0) {
            return nearLength;
        }

        long calculated = offsets[current < offsets.length ? current : (offsets.length - 1)] + length;
        calculated -= offsets[0];

        return Math.max(calculated, nearLength);
    }

    /**
     * @return the current progress in percent (0-100), or -1 if unknown.
     */
    public int getProgress() {
        long total = getLength();
        if (total <= 0) {
            return -1;
        }
        long downloaded = done;
        if (downloaded <= 0) {
            return 0;
        }
        if (downloaded >= total) {
            return 100;
        }
        return (int) ((downloaded * 100L) / total);
    }

    /**
     * Set this mission state on the queue.
     */
    public void setEnqueued(boolean queue) {
        enqueued = queue;
        writeThisToFileAsync();
    }

    /**
     * @return true if the backing storage is invalid and cannot be used.
     */
    public boolean hasInvalidStorage() {
        if (targetFile == null || targetFile.getParentFile() == null) {
            return true;
        }
        File parent = targetFile.getParentFile();
        if (!DownloadUtils.mkdir(parent, true)) {
            return true;
        }
        return !parent.canWrite();
    }

    /**
     * @return true if this mission is "healthy", otherwise false.
     */
    public boolean isCorrupt() {
        if (urls.length < 1) {
            return false;
        }
        return isFinished();
    }

    private boolean deleteThisFromFile() {
        synchronized (LOCK) {
            if (metadata == null) {
                return true;
            }
            boolean res = metadata.delete();
            metadata = null;
            return res;
        }
    }

    private Thread runAsync(int id, Runnable who) {
        return runAsync(id, new Thread(who));
    }

    private Thread runAsync(int id, Thread who) {
        if (who.getName().startsWith("Thread-")) {
            who.setName(String.format("%s[%s] %s", TAG, id, targetFile.getName()));
        }

        who.start();

        return who;
    }

    /**
     * Waits at most {@code millis} milliseconds for the threads to die.
     *
     * @param millis the time to wait in milliseconds, or a negative value to
     *               wait until the threads are interrupted
     */
    private void joinForThreads(int millis) {
        final Thread currentThread = Thread.currentThread();

        if (init != null && init != currentThread && init.isAlive()) {
            init.interrupt();

            if (millis > 0) {
                try {
                    init.join(millis);
                } catch (InterruptedException e) {
                    Logger.w(TAG, "Initializer thread is still running", e);
                    return;
                }
            }
        }

        final Thread[] downloadThreads = threads;
        if (downloadThreads == null) {
            return;
        }

        for (Thread thread : downloadThreads) {
            if (!thread.isAlive() || thread == currentThread) {
                continue;
            }
            thread.interrupt();
        }

        try {
            for (Thread thread : downloadThreads) {
                if (!thread.isAlive()) {
                    continue;
                }
                if (millis > 0) {
                    thread.join(millis);
                }
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("A download thread is still running", e);
        }
    }

    static class HttpError extends Exception {
        final int statusCode;

        HttpError(int statusCode) {
            this.statusCode = statusCode;
        }

        @Override
        public String getMessage() {
            return "HTTP " + statusCode;
        }
    }

    public static class Block {
        public int position;
        public int done;
    }

    private static class Lock implements Serializable {
        // java.lang.Object cannot be used because it is not serializable
    }

    /**
     * Receives the lifecycle events of a mission.
     */
    public interface Listener {
        void onProgress(DownloadMission mission);

        void onFinished(DownloadMission mission);

        void onError(DownloadMission mission);
    }

    /**
     * Small indirection that lets the service post mission events to its
     * handler without coupling the mission to Android's Handler.
     */
    public interface HandlerThreadFactory {
        void sendMessage(int what, DownloadMission mission);
    }
}
