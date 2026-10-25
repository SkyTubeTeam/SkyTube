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

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.channels.ClosedByInterruptException;

import free.rm.skytube.businessobjects.Logger;
import free.rm.skytube.businessobjects.download.DownloadMission.HttpError;

/**
 * Single-threaded fallback mode, used when the server does not support range
 * requests or when the content length is unknown.
 *
 * <p>Ported from NewPipe's {@code us.shandian.giga.get.DownloadRunnableFallback}.</p>
 */
public class DownloadRunnableFallback extends Thread {
    private static final String TAG = "DLRunnableFallback";

    private final DownloadMission downloadMission;

    private int retryCount = 0;
    private InputStream inputStream;
    private FileStream fileStream;
    private HttpURLConnection urlConnection;

    DownloadRunnableFallback(@NonNull DownloadMission mission) {
        downloadMission = mission;
    }

    private void dispose() {
        try {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
        } catch (IOException e) {
            // nothing to do
        }

        if (fileStream != null) {
            fileStream.close();
        }
    }

    @Override
    public void run() {
        boolean done;
        long start = downloadMission.getFallbackResumeOffset();

        if (!downloadMission.getUnknownLength() && start > 0) {
            Logger.i(TAG, "Resuming a single-thread download at " + start);
        }

        try {
            long rangeStart = (downloadMission.getUnknownLength() || start < 1) ? -1 : start;

            int mId = 1;
            urlConnection = downloadMission.openConnection(false, rangeStart, -1);

            if (retryCount == 0 && rangeStart == -1) {
                // workaround: bypass android connection pool
                urlConnection.setRequestProperty("Range", "bytes=0-");
            }

            downloadMission.establishConnection(mId, urlConnection);

            // check if the download can be resumed
            if (urlConnection.getResponseCode() == 416 && start > 0) {
                downloadMission.notifyProgress(-start);
                start = 0;
                retryCount--;
                throw new HttpError(416);
            }

            // secondary check for the file length
            if (!downloadMission.getUnknownLength()) {
                downloadMission.setUnknownLength(DownloadUtils.getContentLength(urlConnection) == -1);
            }

            if (downloadMission.getUnknownLength() || urlConnection.getResponseCode() == 200) {
                // restart amount of bytes downloaded
                downloadMission.setDone(downloadMission.getOffsets()[downloadMission.getCurrent()] - downloadMission.getOffsets()[0]);
                start = 0; // reset position to avoid writing at wrong offset
            }

            fileStream = new FileStream(downloadMission.getPartFile());
            fileStream.seek(downloadMission.getOffsets()[downloadMission.getCurrent()] + start);

            inputStream = urlConnection.getInputStream();

            byte[] buf = new byte[DownloadMission.BUFFER_SIZE];
            int len = 0;

            while (downloadMission.getRunning() && (len = inputStream.read(buf, 0, buf.length)) != -1) {
                fileStream.write(buf, 0, len);
                start += len;
                downloadMission.notifyProgress(len);
            }

            dispose();

            // if thread goes interrupted check if the last part is written. This avoids
            // re-downloading the whole file
            done = len == -1;
        } catch (Exception e) {
            dispose();

            downloadMission.setFallbackResumeOffset(start);

            if (!downloadMission.getRunning() || e instanceof ClosedByInterruptException) {
                return;
            }

            if (e instanceof HttpError
                    && ((HttpError) e).getStatusCode() == DownloadMission.ERROR_HTTP_FORBIDDEN) {
                downloadMission.notifyError(DownloadMission.ERROR_HTTP_FORBIDDEN, (HttpError) e);
                return;
            }

            if (retryCount++ >= downloadMission.getMaxRetry()) {
                downloadMission.notifyError(e);
                return;
            }

            Logger.e(TAG, "got exception, retrying...", e);

            run(); // try again
            return;
        }

        if (done) {
            downloadMission.notifyFinished();
        } else {
            downloadMission.setFallbackResumeOffset(start);
        }
    }

    @Override
    public void interrupt() {
        super.interrupt();

        if (urlConnection != null) {
            try {
                urlConnection.disconnect();
            } catch (Exception e) {
                // nothing to do
            }
        }
    }
}
