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
import android.os.Build;
import android.system.ErrnoException;
import android.system.OsConstants;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Objects;
import java.util.UUID;

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
    private String[] urls;

    /** Number of bytes downloaded and written. */
    private volatile long done;

    /** Indicates a file generated dynamically on the web server. */
    private boolean unknownLength;

    /** Offset in the file where the data should be written. */
    private long[] offsets;

    /** The current resource to download, {@code urls[current]} and {@code offsets[current]}. */
    private int current;

    /** Metadata where the mission state is saved. */
    private transient File metadata;

    /** Maximum attempts. */
    private transient int maxRetry;

    /** Approximated final length, this represents the sum of all resources sizes. */
    private long nearLength;

    /**
     * Download blocks, the size is a multiple of {@link #BLOCK_SIZE}. Every
     * entry (block) holds an offset used to resume the download. A block offset
     * can be -1 if the block was downloaded successfully.
     */
    private int[] blocks;

    /** Download/File resume offset in fallback mode. */
    private volatile long fallbackResumeOffset;

    /** Maximum number of download threads running, chosen by the user. */
    private int threadCount;

    /** Human readable title used by notifications. */
    private String title;

    /**
     * Identifier of the logical download that this mission belongs to.
     * Missions that share an identifier are aggregated in one notification.
     */
    private String notificationGroupId;

    /** Human readable title shared by all missions of the same notification group. */
    private String notificationGroupTitle;

    /** Number of missions that make up the notification group. */
    private int notificationGroupSize;

    /** Whether the download may continue while the device is roaming. */
    private boolean allowedOverRoaming;

    /** Network types allowed by the caller (DownloadManager.Request.NETWORK_*). */
    private int allowedNetworkTypes;

    private transient int finishCount;
    private transient volatile boolean running;
    private boolean enqueued;

    private int errCode;
    private Exception errObject;

    private transient HandlerThreadFactory handler;
    private transient boolean[] blockAcquired;

    private transient long writingToFileNext;
    private transient volatile boolean writingToFile;

    @NonNull
    private transient Thread[] threads;
    private transient Thread init;

    private transient Listener listener;

    public DownloadMission(String[] urls, File targetFile) {
        if (Objects.requireNonNull(urls).length < 1) {
            throw new IllegalArgumentException("urls array is empty");
        }
        this.urls = urls;
        this.offsets = new long[urls.length];
        this.enqueued = true;
        setTargetFile(Objects.requireNonNull(targetFile, "targetFile"));
        setTimestamp(System.currentTimeMillis());
        setId(UUID.randomUUID().toString());
        setKind('?');
        this.maxRetry = 3;
        this.threadCount = 3;
        this.allowedOverRoaming = true;
        this.allowedNetworkTypes =
                DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE;
        this.errCode = ERROR_NOTHING;
        this.threads = new Thread[0];
        this.running = false;
    }

    /**
     * Convenience constructor used by {@link free.rm.skytube.businessobjects.FileDownloader}
     * so that a mission is fully initialized at creation time instead of being
     * configured through setters afterwards.
     */
    public DownloadMission(String[] urls, File targetFile, String title, char kind,
                          String notificationGroupId, String notificationGroupTitle,
                          int notificationGroupSize, int threadCount,
                          boolean allowedOverRoaming, int allowedNetworkTypes) {
        this(urls, targetFile);
        this.title = title;
        setKind(kind);
        this.notificationGroupId = notificationGroupId;
        this.notificationGroupTitle = notificationGroupTitle;
        this.notificationGroupSize = notificationGroupSize;
        this.threadCount = threadCount;
        this.allowedOverRoaming = allowedOverRoaming;
        this.allowedNetworkTypes = allowedNetworkTypes;
    }

    /**
     * Creates a mission whose state is fully supplied (used when restoring a
     * persisted mission from its binary metadata file).
     */
    public DownloadMission(String id, String source, long length, long timestamp, char kind,
                           File targetFile, String[] urls, long done, boolean unknownLength,
                           long[] offsets, int current, long nearLength, int[] blocks,
                           long fallbackResumeOffset, int threadCount, String title,
                           String notificationGroupId, String notificationGroupTitle,
                           int notificationGroupSize, boolean allowedOverRoaming,
                           int allowedNetworkTypes, boolean enqueued, int errCode) {
        super(id, source, length, timestamp, kind, targetFile);
        this.urls = urls;
        this.done = done;
        this.unknownLength = unknownLength;
        this.offsets = offsets;
        this.current = current;
        this.nearLength = nearLength;
        this.blocks = blocks;
        this.fallbackResumeOffset = fallbackResumeOffset;
        this.threadCount = threadCount;
        this.title = title;
        this.notificationGroupId = notificationGroupId;
        this.notificationGroupTitle = notificationGroupTitle;
        this.notificationGroupSize = notificationGroupSize;
        this.allowedOverRoaming = allowedOverRoaming;
        this.allowedNetworkTypes = allowedNetworkTypes;
        this.enqueued = enqueued;
        this.errCode = errCode;
        this.maxRetry = 3;
        this.threads = new Thread[0];
        this.running = false;
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

    // Getters and setters

    public String[] getUrls() {
        return urls;
    }

    public void setUrls(String[] urls) {
        this.urls = urls;
    }

    public long getDone() {
        return done;
    }

    public void setDone(long done) {
        this.done = done;
    }

    public boolean getUnknownLength() {
        return unknownLength;
    }

    public void setUnknownLength(boolean unknownLength) {
        this.unknownLength = unknownLength;
    }

    public long[] getOffsets() {
        return offsets;
    }

    public void setOffsets(long[] offsets) {
        this.offsets = offsets;
    }

    public int getCurrent() {
        return current;
    }

    public File getMetadata() {
        return metadata;
    }

    public void setMetadata(File metadata) {
        this.metadata = metadata;
    }

    public int getMaxRetry() {
        return maxRetry;
    }

    public void setMaxRetry(int maxRetry) {
        this.maxRetry = maxRetry;
    }

    public long getNearLength() {
        return nearLength;
    }

    public void setNearLength(long nearLength) {
        this.nearLength = nearLength;
    }

    public int[] getBlocks() {
        return blocks;
    }

    public void setBlocks(int[] blocks) {
        this.blocks = blocks;
    }

    public long getFallbackResumeOffset() {
        return fallbackResumeOffset;
    }

    public void setFallbackResumeOffset(long fallbackResumeOffset) {
        this.fallbackResumeOffset = fallbackResumeOffset;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public void setThreadCount(int threadCount) {
        this.threadCount = threadCount;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getNotificationGroupId() {
        return notificationGroupId;
    }

    public void setNotificationGroupId(String notificationGroupId) {
        this.notificationGroupId = notificationGroupId;
    }

    public String getNotificationGroupTitle() {
        return notificationGroupTitle;
    }

    public void setNotificationGroupTitle(String notificationGroupTitle) {
        this.notificationGroupTitle = notificationGroupTitle;
    }

    public int getNotificationGroupSize() {
        return notificationGroupSize;
    }

    public void setNotificationGroupSize(int notificationGroupSize) {
        this.notificationGroupSize = notificationGroupSize;
    }

    public boolean isAllowedOverRoaming() {
        return allowedOverRoaming;
    }

    public boolean getAllowedOverRoaming() {
        return allowedOverRoaming;
    }

    public void setAllowedOverRoaming(boolean allowedOverRoaming) {
        this.allowedOverRoaming = allowedOverRoaming;
    }

    public int getAllowedNetworkTypes() {
        return allowedNetworkTypes;
    }

    public void setAllowedNetworkTypes(int allowedNetworkTypes) {
        this.allowedNetworkTypes = allowedNetworkTypes;
    }

    public boolean isRunning() {
        return running;
    }

    public boolean getRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public boolean isEnqueued() {
        return enqueued;
    }

    public boolean getEnqueued() {
        return enqueued;
    }

    public void setEnqueued(boolean enqueued) {
        this.enqueued = enqueued;
        writeThisToFileAsync();
    }

    /**
     * Copies the caller-supplied configuration from {@code other} onto this
     * mission, so that an already tracked mission can pick up a fresh request
     * without exposing every individual setter to the caller.
     */
    public void updateFrom(DownloadMission other) {
        setTitle(other.getTitle());
        if (other.getNotificationGroupId() != null) {
            setNotificationGroupId(other.getNotificationGroupId());
        }
        if (other.getNotificationGroupTitle() != null) {
            setNotificationGroupTitle(other.getNotificationGroupTitle());
        }
        if (other.getNotificationGroupSize() > 0) {
            setNotificationGroupSize(other.getNotificationGroupSize());
        }
        setThreadCount(other.getThreadCount());
        setKind(other.getKind());
        setAllowedOverRoaming(other.getAllowedOverRoaming());
        setAllowedNetworkTypes(other.getAllowedNetworkTypes());
        if (other.getNearLength() > 0) {
            setNearLength(other.getNearLength());
        }
    }

    // Note: original setEnqueued writes file; keep override? This setter already writes. But internal calls setEnqueued(true) in constructor? no.

    public int getErrCode() {
        return errCode;
    }

    public void setErrCode(int errCode) {
        this.errCode = errCode;
    }

    public Exception getErrObject() {
        return errObject;
    }

    public void setErrObject(Exception errObject) {
        this.errObject = errObject;
    }

    public HandlerThreadFactory getHandler() {
        return handler;
    }

    public void setHandler(HandlerThreadFactory handler) {
        this.handler = handler;
    }

    public Thread[] getThreads() {
        return threads;
    }

    public void resetThreads() {
        this.threads = new Thread[0];
    }

    public Thread getInit() {
        return init;
    }

    public void setInit(Thread init) {
        this.init = init;
    }

    /**
     * Acquire a block.
     *
     * @return the block or {@code null} if no more blocks are left
     */
    @Nullable
    Block acquireBlock() {
        synchronized (this) {
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
        synchronized (this) {
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
            setLength(super.getLength() + deltaLen); // Update length before proceeding
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
            notifyError(((HttpError) err).getStatusCode(), null);
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                && err != null && err.getCause() instanceof ErrnoException) {
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
            } else if (getTargetFile() == null || getTargetFile().getParentFile() == null
                    || !getTargetFile().getParentFile().canWrite()) {
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
                offsets[current] = offsets[current - 1] + super.getLength();
                initializer();
                return;
            }
        }

        // this mission is fully finished
        unknownLength = false;
        enqueued = false;
        running = false;

        if (!DownloadUtils.moveCompletedFile(getPartFile(), getTargetFile())) {
            notifyError(ERROR_FILE_CREATION, new IOException("Cannot move the downloaded file to " + getTargetFile()));
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
            synchronized (this) {
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
        setLength(0);
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
        synchronized (this) {
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
        return getTargetFile() != null && getTargetFile().exists();
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
        if (super.getLength() <= 0) {
            return nearLength;
        }

        long calculated = offsets[current < offsets.length ? current : (offsets.length - 1)] + super.getLength();
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
     * @return true if the backing storage is invalid and cannot be used.
     */
    public boolean hasInvalidStorage() {
        if (getTargetFile() == null || getTargetFile().getParentFile() == null) {
            return true;
        }
        File parent = getTargetFile().getParentFile();
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
        synchronized (this) {
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
            who.setName(String.format("%s[%s] %s", TAG, id, getTargetFile().getName()));
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
        private final int statusCode;

        HttpError(int statusCode) {
            this.statusCode = statusCode;
        }

        public int getStatusCode() {
            return statusCode;
        }

        @Override
        public String getMessage() {
            return "HTTP " + statusCode;
        }
    }

    public static class Block {
        private int position;
        private int done;

        public int getPosition() {
            return position;
        }

        public void setPosition(int position) {
            this.position = position;
        }

        public int getDone() {
            return done;
        }

        public void setDone(int done) {
            this.done = done;
        }
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