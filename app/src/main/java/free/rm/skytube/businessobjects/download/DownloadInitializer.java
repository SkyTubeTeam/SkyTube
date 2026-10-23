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
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.HttpURLConnection;
import java.nio.channels.ClosedByInterruptException;

import free.rm.skytube.businessobjects.Logger;

/**
 * Determines the size of the remote resource and prepares the download blocks.
 *
 * <p>Ported from NewPipe's {@code us.shandian.giga.get.DownloadInitializer}.</p>
 */
public class DownloadInitializer extends Thread {
    private static final String TAG = "DownloadInitializer";
    static final int mId = 0;

    private final DownloadMission mMission;
    private HttpURLConnection mConn;

    DownloadInitializer(@NonNull DownloadMission mission) {
        mMission = mission;
        mConn = null;
    }

    private void dispose() {
        try {
            if (mConn != null) {
                mConn.getInputStream().close();
            }
        } catch (Exception e) {
            // nothing to do
        }
    }

    @Override
    public void run() {
        if (mMission.current > 0) {
            mMission.resetState(false, true, DownloadMission.ERROR_NOTHING);
        }

        int retryCount = 0;
        int httpCode = 204;

        while (true) {
            try {
                if (mMission.blocks == null && mMission.current == 0) {
                    // calculate the whole size of the mission
                    mMission.done = 0;
                    long finalLength = 0;

                    for (int i = 0; i < mMission.urls.length && mMission.running; i++) {
                        mConn = mMission.openConnection(mMission.urls[i], true, 0, 0);
                        mMission.establishConnection(mId, mConn);
                        dispose();

                        if (Thread.interrupted()) {
                            return;
                        }
                        long length = DownloadUtils.getTotalContentLength(mConn);

                        if (i == 0) {
                            httpCode = mConn.getResponseCode();
                            mMission.length = length;
                        }

                        if (length > 0) {
                            finalLength += length;
                        }
                    }

                    mMission.nearLength = finalLength;
                } else {
                    // ask for the current resource length
                    mConn = mMission.openConnection(true, 0, 0);
                    mMission.establishConnection(mId, mConn);
                    dispose();

                    if (!mMission.running || Thread.interrupted()) {
                        return;
                    }

                    httpCode = mConn.getResponseCode();
                    mMission.length = DownloadUtils.getTotalContentLength(mConn);
                }

                if (mMission.length == 0 || httpCode == 204) {
                    mMission.notifyError(DownloadMission.ERROR_HTTP_NO_CONTENT, null);
                    return;
                }

                // check for dynamically generated content
                if (mMission.length == -1 && mConn.getResponseCode() == 200) {
                    mMission.blocks = new int[0];
                    mMission.length = 0;
                    mMission.unknownLength = true;

                    Logger.d(TAG, "falling back (unknown length)");
                } else {
                    // Open again, this time checking for range request support
                    long rangeStart = Math.max(0, mMission.length - 10);
                    mConn = mMission.openConnection(true, rangeStart, mMission.length);
                    mMission.establishConnection(mId, mConn);
                    dispose();

                    if (!mMission.running || Thread.interrupted()) {
                        return;
                    }

                    synchronized (mMission.LOCK) {
                        if (mConn.getResponseCode() == 206) {
                            if (mMission.threadCount > 1) {
                                int count = (int) (mMission.length / DownloadMission.BLOCK_SIZE);
                                if ((count * DownloadMission.BLOCK_SIZE) < mMission.length) {
                                    count++;
                                }

                                mMission.blocks = new int[count];
                            } else {
                                // if one thread is required don't calculate blocks, it is useless
                                mMission.blocks = new int[0];
                                mMission.unknownLength = false;
                            }

                            Logger.d(TAG, "http response code = " + mConn.getResponseCode());
                        } else {
                            // Fallback to single thread
                            mMission.blocks = new int[0];
                            mMission.unknownLength = false;

                            Logger.d(TAG, "falling back due http response code = " + mConn.getResponseCode());
                        }
                    }

                    if (!mMission.running || Thread.interrupted()) {
                        return;
                    }
                }

                File partFile = mMission.getPartFile();
                DownloadUtils.mkdir(partFile.getParentFile(), true);

                try (FileStream fs = new FileStream(partFile)) {
                    long desiredLength = mMission.offsets[mMission.current] + mMission.length;
                    if (desiredLength > 0) {
                        fs.setLength(desiredLength);
                    }
                    fs.seek(mMission.offsets[mMission.current]);
                }

                if (!mMission.running || Thread.interrupted()) {
                    return;
                }

                mMission.running = false;
                break;
            } catch (InterruptedIOException | ClosedByInterruptException e) {
                return;
            } catch (Exception e) {
                if (!mMission.running || super.isInterrupted()) {
                    return;
                }

                if (e instanceof DownloadMission.HttpError
                        && ((DownloadMission.HttpError) e).statusCode == DownloadMission.ERROR_HTTP_FORBIDDEN) {
                    // The YouTube stream url has expired.
                    mMission.notifyError(DownloadMission.ERROR_HTTP_FORBIDDEN, (DownloadMission.HttpError) e);
                    return;
                }

                if (e instanceof IOException && e.getMessage() != null
                        && e.getMessage().contains("Permission denied")) {
                    mMission.notifyError(DownloadMission.ERROR_PERMISSION_DENIED, e);
                    return;
                }

                if (retryCount++ > mMission.maxRetry) {
                    Logger.e(TAG, "initializer failed", e);
                    mMission.notifyError(e);
                    return;
                }

                Logger.e(TAG, "initializer failed, retrying", e);
            }
        }

        // Do not restart the mission if it was paused while the initializer
        // was finishing.
        if (isInterrupted()) {
            return;
        }

        mMission.start();
    }

    @Override
    public void interrupt() {
        super.interrupt();
        if (mConn != null) {
            dispose();
        }
    }
}
