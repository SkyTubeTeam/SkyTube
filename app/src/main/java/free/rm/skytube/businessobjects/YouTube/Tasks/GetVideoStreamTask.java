/*
 * SkyTube
 * Copyright (C) 2018  Ramon Mifsud
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

package free.rm.skytube.businessobjects.YouTube.Tasks;

import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.stream.StreamInfo;

import java.io.IOException;

import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.AsyncTaskParallel;
import free.rm.skytube.businessobjects.Logger;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;
import free.rm.skytube.businessobjects.YouTube.VideoStream.StreamMetaData;
import free.rm.skytube.businessobjects.YouTube.VideoStream.StreamMetaDataList;
import free.rm.skytube.businessobjects.YouTube.newpipe.NewPipeService;
import free.rm.skytube.businessobjects.interfaces.GetDesiredStreamListener;

/**
 * AsyncTask to retrieve the Uri for the given YouTube video.
 */
public class GetVideoStreamTask extends AsyncTaskParallel<Void, Exception, StreamInfo> {

    private final YouTubeVideo youTubeVideo;
    private final GetDesiredStreamListener listener;
    private final boolean forDownload;

    public GetVideoStreamTask(YouTubeVideo youTubeVideo, GetDesiredStreamListener listener, boolean forDownload) {
        this.youTubeVideo = youTubeVideo;
        this.listener = listener;
        this.forDownload = forDownload;
    }

    @Override
    protected StreamInfo doInBackground(Void... param) {
        try {
            return NewPipeService.get().getStreamInfoByVideoId(youTubeVideo.getId());
        } catch (ExtractionException | IOException e) {
            lastException = e;
            Logger.e(this, "Unable to get stream information for " + youTubeVideo.getId() + " error:" + e.getMessage(), e);
            return null;
        }
    }


    @Override
    protected void showErrorToUi() {
        listener.onGetDesiredStreamError(lastException);
    }

    @Override
    protected void onPostExecute(StreamInfo streamInfo) {
        if (streamInfo != null){
            listener.onGetDesiredStream(streamInfo);
        }
        super.onPostExecute(streamInfo);
    }

}
