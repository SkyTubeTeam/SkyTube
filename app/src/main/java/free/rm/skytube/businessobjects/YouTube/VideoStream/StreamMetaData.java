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
 */

package free.rm.skytube.businessobjects.YouTube.VideoStream;


import android.net.Uri;

import org.schabi.newpipe.extractor.stream.VideoStream;

/**
 * Represents the meta-data of a YouTube video stream.
 */
public class StreamMetaData {

	/** URL of the stream */
	private Uri uri;
	/** Video resolution (e.g. 1080p) */
	private VideoResolution resolution;
	/** Video format (e.g. MPEG-4) */
	private MediaFormat format;

	private static final String TAG = StreamMetaData.class.getSimpleName();


//	public StreamMetaData(String url, int itag) {
//		setUri(url);
//		setMediaFormat(itag);
//		setResolution(itag);
//	}


	public StreamMetaData(VideoStream videoStream) {
		setUri(videoStream.url);
		setMediaFormat(videoStream.format);
		this.resolution = VideoResolution.resolutionToVideoResolution(videoStream.resolution);
	}


	private void setUri(String url) {
		this.uri = Uri.parse(url);
	}


	/**
	 * Converts the given itag into {@link MediaFormat}.
	 */
	private void setMediaFormat(int itag) {
		this.format = MediaFormat.itagToMediaFormat(itag);
	}


//	/**
//	 * Converts the given itag into {@link VideoResolution}.
//	 */
//	private void setResolution(int itag) {
//		this.resolution = VideoResolution.itagToVideoResolution(itag);
//	}


	public Uri getUri() {
		return uri;
	}

	public VideoResolution getResolution() {
		return resolution;
	}

	public MediaFormat getFormat() {
		return format;
	}

	@Override
	public String toString() {
		StringBuilder str = new StringBuilder();

		str.append("URI:  ");
		str.append(uri);
		str.append('\n');

		str.append("FORMAT:  ");
		str.append(format);
		str.append('\n');

		str.append("RESOLUTION:  ");
		str.append(resolution);

		return str.toString();
	}

}
