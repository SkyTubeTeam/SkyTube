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

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.channels.ClosedByInterruptException;
import java.util.Objects;

import free.rm.skytube.businessobjects.Logger;
import free.rm.skytube.businessobjects.download.DownloadMission.Block;
import free.rm.skytube.businessobjects.download.DownloadMission.HttpError;

/**
 * Downloads blocks of a file until the file is completely downloaded, an error
 * occurs or the process is stopped.
 *
 * <p>Ported from NewPipe's {@code us.shandian.giga.get.DownloadRunnable}.</p>
 */
public class DownloadRunnable implements Runnable {
    private static final String TAG = "DownloadRunnable";

    private final DownloadMission downloadMission;
    private final int mId;

    private HttpURLConnection urlConnection;

    DownloadRunnable(DownloadMission mission, int id) {
        downloadMission = Objects.requireNonNull(mission);
        mId = id;
    }

    private void releaseBlock(Block block, long remain) {
        // set the block offset to -1 if it is completed
        downloadMission.releaseBlock(block.getPosition(), remain < 0 ? -1 : block.getDone());
    }

    @Override
    public void run() {
        boolean retry = false;
        Block block = null;
        int retryCount = 0;
        FileStream f;

        try {
            f = new FileStream(downloadMission.getPartFile());
        } catch (IOException e) {
            downloadMission.notifyError(e); // this never should happen
            return;
        }

        while (downloadMission.getRunning() && downloadMission.getErrCode() == DownloadMission.ERROR_NOTHING) {
            if (!retry) {
                block = downloadMission.acquireBlock();
            }

            if (block == null) {
                Logger.d(TAG, mId + ":no more blocks left, exiting");
                break;
            }

            long start = (long) block.getPosition() * DownloadMission.BLOCK_SIZE;
            long end = start + DownloadMission.BLOCK_SIZE - 1;

            start += block.getDone();

            if (end >= downloadMission.getLength()) {
                end = downloadMission.getLength() - 1;
            }

            try {
                urlConnection = downloadMission.openConnection(false, start, end);
                downloadMission.establishConnection(mId, urlConnection);

                // check if the download can be resumed
                if (urlConnection.getResponseCode() == 416) {
                    if (block.getDone() > 0) {
                        // try again from the start (of the block);
                        downloadMission.notifyProgress(-block.getDone());
                        block.setDone(0);
                        retry = true;
                        urlConnection.disconnect();
                        continue;
                    }

                    throw new HttpError(416);
                }

                retry = false;

                // The server may be ignoring the range request
                if (urlConnection.getResponseCode() != 206) {
                    Logger.e(TAG, mId + ":Unsupported " + urlConnection.getResponseCode());
                    downloadMission.notifyError(new HttpError(urlConnection.getResponseCode()));
                    break;
                }

                f.seek(downloadMission.getOffsets()[downloadMission.getCurrent()] + start);

                try (InputStream is = urlConnection.getInputStream()) {
                    byte[] buf = new byte[DownloadMission.BUFFER_SIZE];
                    int len;

                    // use always start <= end
                    // fixes a deadlock because in some videos, youtube is sending one byte alone
                    while (start <= end && downloadMission.getRunning()
                            && (len = is.read(buf, 0, buf.length)) != -1) {
                        f.write(buf, 0, len);
                        start += len;
                        block.setDone(block.getDone() + len);
                        downloadMission.notifyProgress(len);
                    }
                }

                if (downloadMission.getRunning()) {
                    Logger.d(TAG, mId + ":position " + block.getPosition() + " stopped " + start + "/" + end);
                }
            } catch (Exception e) {
                if (!downloadMission.getRunning() || e instanceof ClosedByInterruptException) {
                    break;
                }

                if (e instanceof HttpError
                        && ((HttpError) e).getStatusCode() == DownloadMission.ERROR_HTTP_FORBIDDEN) {
                    // The YouTube stream url has expired.
                    f.close();

                    if (mId == 1) {
                        // only the first thread will execute the recovery procedure
                        downloadMission.notifyError(DownloadMission.ERROR_HTTP_FORBIDDEN, (HttpError) e);
                    }
                    return;
                }

                if (retryCount++ >= downloadMission.getMaxRetry()) {
                    downloadMission.notifyError(e);
                    break;
                }

                retry = true;
            } finally {
                if (!retry) {
                    releaseBlock(block, end - start);
                }
            }
        }

        f.close();

        Logger.d(TAG, "thread " + mId + " exited from main download loop");

        if (downloadMission.getErrCode() == DownloadMission.ERROR_NOTHING && downloadMission.getRunning()) {
            downloadMission.notifyFinished();
        }
    }
}
