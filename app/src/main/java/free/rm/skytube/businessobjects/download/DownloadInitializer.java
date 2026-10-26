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
public class DownloadInitializer implements Runnable {
    private static final String TAG = "DownloadInitializer";
    static final int mId = 0;

    private final DownloadMission downloadMission;
    private HttpURLConnection urlConnection;

    DownloadInitializer(@NonNull DownloadMission mission) {
        downloadMission = mission;
        urlConnection = null;
    }

    private void dispose() {
        try {
            if (urlConnection != null) {
                urlConnection.getInputStream().close();
            }
        } catch (Exception e) {
            // nothing to do
        }
    }

    @Override
    public void run() {
        if (downloadMission.getCurrent() > 0) {
            downloadMission.resetState(false, true, DownloadMission.ERROR_NOTHING);
        }

        int retryCount = 0;
        int httpCode = 204;

        while (true) {
            try {
                if (downloadMission.getBlocks() == null && downloadMission.getCurrent() == 0) {
                    // calculate the whole size of the mission
                    downloadMission.setDone(0);
                    long finalLength = 0;

                    for (int i = 0; i < downloadMission.getUrls().length && downloadMission.getRunning(); i++) {
                        urlConnection = downloadMission.openConnection(downloadMission.getUrls()[i], true, 0, 0);
                        downloadMission.establishConnection(mId, urlConnection);
                        dispose();

                        if (Thread.interrupted()) {
                            return;
                        }
                        long length = DownloadUtils.getTotalContentLength(urlConnection);

                        if (i == 0) {
                            httpCode = urlConnection.getResponseCode();
                            downloadMission.setLength(length);
                        }

                        if (length > 0) {
                            finalLength += length;
                        }
                    }

                    downloadMission.setNearLength(finalLength);
                } else {
                    // ask for the current resource length
                    urlConnection = downloadMission.openConnection(true, 0, 0);
                    downloadMission.establishConnection(mId, urlConnection);
                    dispose();

                    if (!downloadMission.getRunning() || Thread.interrupted()) {
                        return;
                    }

                    httpCode = urlConnection.getResponseCode();
                    downloadMission.setLength(DownloadUtils.getTotalContentLength(urlConnection));
                }

                if (downloadMission.getLength() == 0 || httpCode == 204) {
                    downloadMission.notifyError(DownloadMission.ERROR_HTTP_NO_CONTENT, null);
                    return;
                }

                // check for dynamically generated content
                if (downloadMission.getLength() == -1 && urlConnection.getResponseCode() == 200) {
                    downloadMission.setBlocks(new int[0]);
                    downloadMission.setLength(0);
                    downloadMission.setUnknownLength(true);

                    Logger.d(TAG, "falling back (unknown length)");
                } else {
                    // Open again, this time checking for range request support
                    long rangeStart = Math.max(0, downloadMission.getLength() - 10);
                    urlConnection = downloadMission.openConnection(true, rangeStart, downloadMission.getLength());
                    downloadMission.establishConnection(mId, urlConnection);
                    dispose();

                    if (!downloadMission.getRunning() || Thread.interrupted()) {
                        return;
                    }

                    synchronized (downloadMission) {
                        if (urlConnection.getResponseCode() == 206) {
                            if (downloadMission.getThreadCount() > 1) {
                                int count = (int) (downloadMission.getLength() / DownloadMission.BLOCK_SIZE);
                                if ((count * DownloadMission.BLOCK_SIZE) < downloadMission.getLength()) {
                                    count++;
                                }

                                downloadMission.setBlocks(new int[count]);
                            } else {
                                // if one thread is required don't calculate blocks, it is useless
                                downloadMission.setBlocks(new int[0]);
                                downloadMission.setUnknownLength(false);
                            }

                            Logger.d(TAG, "http response code = " + urlConnection.getResponseCode());
                        } else {
                            // Fallback to single thread
                            downloadMission.setBlocks(new int[0]);
                            downloadMission.setUnknownLength(false);

                            Logger.d(TAG, "falling back due http response code = " + urlConnection.getResponseCode());
                        }
                    }

                    if (!downloadMission.getRunning() || Thread.interrupted()) {
                        return;
                    }
                }

                File partFile = downloadMission.getPartFile();
                DownloadUtils.mkdir(partFile.getParentFile(), true);

                try (FileStream fs = new FileStream(partFile)) {
                    long desiredLength = downloadMission.getOffsets()[downloadMission.getCurrent()] + downloadMission.getLength();
                    if (desiredLength > 0) {
                        fs.setLength(desiredLength);
                    }
                    fs.seek(downloadMission.getOffsets()[downloadMission.getCurrent()]);
                }

                if (!downloadMission.getRunning() || Thread.interrupted()) {
                    return;
                }

                downloadMission.setRunning(false);
                break;
            } catch (InterruptedIOException | ClosedByInterruptException e) {
                return;
            } catch (Exception e) {
                if (!downloadMission.getRunning() || Thread.currentThread().isInterrupted()) {
                    return;
                }

                if (e instanceof DownloadMission.HttpError
                        && ((DownloadMission.HttpError) e).getStatusCode() == DownloadMission.ERROR_HTTP_FORBIDDEN) {
                    // The YouTube stream url has expired.
                    downloadMission.notifyError(DownloadMission.ERROR_HTTP_FORBIDDEN, (DownloadMission.HttpError) e);
                    return;
                }

                if (e instanceof IOException && e.getMessage() != null
                        && e.getMessage().contains("Permission denied")) {
                    downloadMission.notifyError(DownloadMission.ERROR_PERMISSION_DENIED, e);
                    return;
                }

                if (retryCount++ > downloadMission.getMaxRetry()) {
                    Logger.e(TAG, "initializer failed", e);
                    downloadMission.notifyError(e);
                    return;
                }

                Logger.e(TAG, "initializer failed, retrying", e);
            }
        }

        // Do not restart the mission if it was paused while the initializer
        // was finishing.
        if (Thread.currentThread().isInterrupted()) {
            return;
        }

        downloadMission.start();
    }

    public void interrupt() {
        if (urlConnection != null) {
            dispose();
        }
    }
}
