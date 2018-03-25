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

import android.net.Uri;

import java.io.File;

import free.rm.skytube.businessobjects.AsyncTaskParallel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;
import free.rm.skytube.businessobjects.YouTube.VideoStream.ParseStreamMetaData;
import free.rm.skytube.businessobjects.YouTube.VideoStream.StreamMetaDataList;
import free.rm.skytube.businessobjects.interfaces.GetDesiredStreamListener;

/**
 * AsyncTask to retrieve the Uri for the given YouTube video.
 *
 * <p>If the video was already downloaded/saved by the user, it will return the Uri of that file;
 * otherwise, if will get the desired stream from the YouTube servers.</p>
 */
public class GetVideoStreamTask extends AsyncTaskParallel<Void, Exception, Uri> {

	private YouTubeVideo                youTubeVideo;
	private GetDesiredStreamListener    listener;
	private String                      errorMessage = "";


	public GetVideoStreamTask(YouTubeVideo youTubeVideo, GetDesiredStreamListener listener) {
		this.youTubeVideo = youTubeVideo;
		this.listener = listener;
	}


	@Override
	protected Uri doInBackground(Void... param) {
		Uri videoUri = null;

		// if the user has previously saved/downloaded the video...
		if (youTubeVideo.isDownloaded()) {
			videoUri = youTubeVideo.getFileUri();

			// If the file for this video has gone missing, remove it from the Database and then
			// play remotely.
			if (!new File(videoUri.getPath()).exists()) {
				youTubeVideo.removeDownload();

				videoUri = null;
			}
		}

		if (videoUri == null) {
			videoUri = getVideoStreamUri();
		}

		return videoUri;
	}


	/**
	 * Returns a list of video/stream meta-data that is supported by this app (with respect to this
	 * video).
	 *
	 * @return A list of {@link StreamMetaDataList}.
	 */
	private Uri getVideoStreamUri() {
		StreamMetaDataList streamMetaDataList;

		ParseStreamMetaData streamParser = new ParseStreamMetaData(youTubeVideo.getId());
		streamMetaDataList = streamParser.getStreamMetaDataList();

		if (streamMetaDataList == null || streamMetaDataList.size() <= 0) {
			errorMessage = streamMetaDataList.getErrorMessage();
			return null;
		} else {
			return streamMetaDataList.getDesiredStream().getUri();
		}
	}


	@Override
	protected void onPostExecute(Uri videoUri) {
		if (videoUri == null) {
			listener.onGetDesiredStreamError(errorMessage);
		} else {
			listener.onGetDesiredStream(videoUri);
		}
	}

}
