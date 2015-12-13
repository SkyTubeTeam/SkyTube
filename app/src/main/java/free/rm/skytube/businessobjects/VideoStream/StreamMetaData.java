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

package free.rm.skytube.businessobjects.VideoStream;


import android.net.Uri;
import android.util.Log;

/**
 * Represents the meta-data of a YouTube video stream.
 */
 public class StreamMetaData {

	/** URL of the stream */
	public Uri uri;
	public String resolution;
	public MediaFormat format;

	private static final String TAG = StreamMetaData.class.getSimpleName();


	public StreamMetaData(String url, int itag) {
		setUri(url);
		setMediaFormat(itag);
		setResolution(itag);
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


	private void setResolution(int itag) {
		switch(itag) {
			case 17:
				this.resolution = "144p";
				break;
			case 18:
				this.resolution = "360p";
				break;
			case 22:
				this.resolution = "720p";
				break;
			case 36:
				this.resolution = "240p";
				break;
			case 37:
				this.resolution = "1080p";
				break;
			case 38:
				this.resolution = "1080p";
				break;
			case 43:
				this.resolution = "360p";
				break;
			case 44:
				this.resolution = "480p";
				break;
			case 45:
				this.resolution = "720p";
				break;
			case 46:
				this.resolution = "1080p";
				break;
			default:
				Log.e(TAG, "itag " + itag + " not known or not supported.");
				this.resolution = "???";
		}
	}


	public Uri getUri() {
		return uri;
	}

	public String getResolution() {
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
