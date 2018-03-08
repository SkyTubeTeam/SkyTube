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

import free.rm.skytube.businessobjects.AsyncTaskParallel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;
import free.rm.skytube.businessobjects.YouTube.VideoStream.ParseStreamMetaData;
import free.rm.skytube.businessobjects.YouTube.VideoStream.StreamMetaData;
import free.rm.skytube.businessobjects.YouTube.VideoStream.StreamMetaDataList;
import free.rm.skytube.businessobjects.interfaces.GetDesiredStreamListener;

/**
 * AsyncTask to retrieve the Uri for the given YouTube video.
 */
public class GetVideoStreamTask extends AsyncTaskParallel<Void, Exception, StreamMetaDataList> {

	private YouTubeVideo                youTubeVideo;
	private GetDesiredStreamListener    listener;

	public GetVideoStreamTask(YouTubeVideo youTubeVideo, GetDesiredStreamListener listener) {
		this.youTubeVideo = youTubeVideo;
		this.listener = listener;
	}

	@Override
	protected StreamMetaDataList doInBackground(Void... param) {

		StreamMetaDataList streamMetaDataList;

		ParseStreamMetaData streamParser = new ParseStreamMetaData(youTubeVideo.getId());
		streamMetaDataList = streamParser.getStreamMetaDataList();

		return streamMetaDataList;
	}


	@Override
	protected void onPostExecute(StreamMetaDataList streamMetaDataList) {
		if (streamMetaDataList == null || streamMetaDataList.size() <= 0) {
			listener.onGetDesiredStreamError(streamMetaDataList.getErrorMessage());
		} else {
			// get the desired stream based on user preferences
			StreamMetaData desiredStream = streamMetaDataList.getDesiredStream();

			listener.onGetDesiredStream(desiredStream);
		}
	}

}
