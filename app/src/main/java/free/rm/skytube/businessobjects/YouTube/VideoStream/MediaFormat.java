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

/*
 * Parts of the code below were written by Christian Schabesberger.
 *
 * Copyright (C) Christian Schabesberger 2015 <chris.schabesberger@mailbox.org>
 *     and Adam Howard <achdisposable1@gmail.com> 2015
 *
 * Code written by Schabesberger and Howard are Licensed under GPL version 3 of the License, or (at
 * your option) any later version.
 */

package free.rm.skytube.businessobjects.YouTube.VideoStream;

import android.util.Log;

/**
 * This class represents the media formats of a YouTube video/stream (e.g. MPEG-4).
 */
public enum MediaFormat {

	//       name		suffix	mime type
	MPEG_4	("MPEG-4",	"mp4",	"video/mp4"),
	V3GPP	("3GPP",	"3gp",	"video/3gpp"),
	WEBM	("WebM",	"webm",	"video/webm"),
	M4A		("m4a",		"m4a",	"audio/mp4"),
	WEBMA	("WebM",	"webm",	"audio/webm"),
	UNKNOWN	("???",		"???",	"???");

	public final String name;
	public final String suffix;
	public final String mimeType;

	private static final String TAG = MediaFormat.class.getSimpleName();

	
	MediaFormat(String name, String suffix, String mimeType) {
		this.name = name;
		this.suffix = suffix;
		this.mimeType = mimeType;
	}


	/**
	 * Convert the itag returned by YouTube to a format.
	 *
	 * <p>List of itags can be found <a href="https://github.com/rg3/youtube-dl/issues/1687">here</a>.</p>
	 *
	 * @param itag itag returned by YouTube
	 *
	 * @return {@link MediaFormat}
	 */
	static MediaFormat itagToMediaFormat(int itag) {
		switch(itag) {
			case 17: return V3GPP;
			case 18: return MPEG_4;
			case 22: return MPEG_4;
			case 36: return V3GPP;
			case 37: return MPEG_4;
			case 38: return MPEG_4;
			case 43: return WEBM;
			case 44: return WEBM;
			case 45: return WEBM;
			case 46: return WEBM;
			default:
				Log.w(TAG, "itag " + itag + " not known or not supported.");
				return UNKNOWN;
		}
	}

}
