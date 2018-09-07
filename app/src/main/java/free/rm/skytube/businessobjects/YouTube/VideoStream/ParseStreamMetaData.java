/*
 * SkyTube
 * Copyright (C) 2015  Ramon Mifsud
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
 *
 *
 * Parts of the code below were written by Christian Schabesberger.
 *
 * Copyright (C) Christian Schabesberger 2015 <chris.schabesberger@mailbox.org>
 * Code written by Schabesberger is licensed under GPL version 3 of the License, or (at your
 * option) any later version.
 */

package free.rm.skytube.businessobjects.YouTube.VideoStream;


import android.util.Log;

import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.exceptions.ContentNotAvailableException;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.VideoStream;

import free.rm.skytube.R;


/**
 * Parses stream/video meta-data and returns
 */
public class ParseStreamMetaData {

	/** YouTube video URL (e.g. https://www.youtube.com/watch?v=XXXXXXXXX) */
	private	String youtubeVideoUrl;

	private static final String TAG = ParseStreamMetaData.class.getSimpleName();


	/**
	 * Initialise the {@link ParseStreamMetaData} object.
	 *
	 * @param videoId	The ID of the video we are going to get its streams.
	 */
	public ParseStreamMetaData(String videoId) {
		NewPipe.init(new HttpDownloader());
		setYoutubeVideoUrl(videoId);
	}



	/**
	 * Returns a list of video/stream meta-data that is supported by this app.
	 *
	 * @return List of {@link StreamMetaData}.
	 */
	public StreamMetaDataList getStreamMetaDataList() {
		StreamMetaDataList list = new StreamMetaDataList();

		try {
			StreamingService youtubeService = ServiceList.YouTube;

			// actual extraction
			StreamInfo streamInfo = StreamInfo.getInfo(youtubeService, youtubeVideoUrl);

			// now print the stream url and we are done
			for(VideoStream stream : streamInfo.getVideoStreams()) {
				list.add( new StreamMetaData(stream) );
			}
		} catch (ContentNotAvailableException exception) {
			list = new StreamMetaDataList(exception.getMessage());
		} catch (Throwable tr) {
			Log.e(TAG, "An error has occurred while getting streams metadata.  URL=" + this.youtubeVideoUrl, tr);
			list = new StreamMetaDataList(R.string.error_video_streams);
		}

		return list;
	}



	/**
	 * Given video ID it will set the video's page URL.
	 *
	 * @param videoId	The ID of the video.
	 */
	private void setYoutubeVideoUrl(String videoId) {
		this.youtubeVideoUrl = "https://www.youtube.com/watch?v=" + videoId;
	}

}
