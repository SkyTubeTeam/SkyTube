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

    private final DownloadMission mMission;

    private int mRetryCount = 0;
    private InputStream mIs;
    private FileStream mF;
    private HttpURLConnection mConn;

    DownloadRunnableFallback(@NonNull DownloadMission mission) {
        mMission = mission;
    }

    private void dispose() {
        try {
            try {
                if (mIs != null) {
                    mIs.close();
                }
            } finally {
                if (mConn != null) {
                    mConn.disconnect();
                }
            }
        } catch (IOException e) {
            // nothing to do
        }

        if (mF != null) {
            mF.close();
        }
    }

    @Override
    public void run() {
        boolean done;
        long start = mMission.getFallbackResumeOffset();

        if (!mMission.getUnknownLength() && start > 0) {
            Logger.i(TAG, "Resuming a single-thread download at " + start);
        }

        try {
            long rangeStart = (mMission.getUnknownLength() || start < 1) ? -1 : start;

            int mId = 1;
            mConn = mMission.openConnection(false, rangeStart, -1);

            if (mRetryCount == 0 && rangeStart == -1) {
                // workaround: bypass android connection pool
                mConn.setRequestProperty("Range", "bytes=0-");
            }

            mMission.establishConnection(mId, mConn);

            // check if the download can be resumed
            if (mConn.getResponseCode() == 416 && start > 0) {
                mMission.notifyProgress(-start);
                start = 0;
                mRetryCount--;
                throw new HttpError(416);
            }

            // secondary check for the file length
            if (!mMission.getUnknownLength()) {
                mMission.setUnknownLength(DownloadUtils.getContentLength(mConn) == -1);
            }

            if (mMission.getUnknownLength() || mConn.getResponseCode() == 200) {
                // restart amount of bytes downloaded
                mMission.setDone(mMission.getOffsets()[mMission.getCurrent()] - mMission.getOffsets()[0]);
                start = 0; // reset position to avoid writing at wrong offset
            }

            mF = new FileStream(mMission.getPartFile());
            mF.seek(mMission.getOffsets()[mMission.getCurrent()] + start);

            mIs = mConn.getInputStream();

            byte[] buf = new byte[DownloadMission.BUFFER_SIZE];
            int len = 0;

            while (mMission.getRunning() && (len = mIs.read(buf, 0, buf.length)) != -1) {
                mF.write(buf, 0, len);
                start += len;
                mMission.notifyProgress(len);
            }

            dispose();

            // if thread goes interrupted check if the last part is written. This avoids
            // re-downloading the whole file
            done = len == -1;
        } catch (Exception e) {
            dispose();

            mMission.setFallbackResumeOffset(start);

            if (!mMission.getRunning() || e instanceof ClosedByInterruptException) {
                return;
            }

            if (e instanceof HttpError
                    && ((HttpError) e).getStatusCode() == DownloadMission.ERROR_HTTP_FORBIDDEN) {
                mMission.notifyError(DownloadMission.ERROR_HTTP_FORBIDDEN, (HttpError) e);
                return;
            }

            if (mRetryCount++ >= mMission.getMaxRetry()) {
                mMission.notifyError(e);
                return;
            }

            Logger.e(TAG, "got exception, retrying...", e);

            run(); // try again
            return;
        }

        if (done) {
            mMission.notifyFinished();
        } else {
            mMission.setFallbackResumeOffset(start);
        }
    }

    @Override
    public void interrupt() {
        super.interrupt();

        if (mConn != null) {
            try {
                mConn.disconnect();
            } catch (Exception e) {
                // nothing to do
            }
        }
    }
}
