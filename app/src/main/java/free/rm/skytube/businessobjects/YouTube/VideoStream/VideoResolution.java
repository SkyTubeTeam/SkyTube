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

import android.preference.ListPreference;

/**
 * Video resolution (e.g. 1080p).
 */
public enum VideoResolution {

	//			(id, vertical pixels, itags list)
	/** Unknown video resolution */
	RES_UNKNOWN	(-1, -1),
	RES_144P	(0, 144),
	RES_240P	(1, 240),
	RES_360P	(2, 360),
	RES_480P	(3, 480),
	/** 720p - HD */
	RES_720P	(4, 720),
	/** 1080p - HD */
	RES_1080P	(5, 1080);

	// these will be added eventually
	/** 1440p - HD */
	//RES_1440P,
	/** 2160p - 4k */
	//RES_2160P,
	/** 4320p - 8k */
	//RES_4320P;

	/** Video resolution ID */
	private final int id;
	/** Number of vertical pixels this video resolution has (e.g. 1080) */
	private final int verticalPixels;

	/**
	 * The default video resolution (ID) that will be used if the user has not choose a desired
	 * one.
	 */
	public  static final int	DEFAULT_VIDEO_RES_ID = RES_1080P.id;
	private static final String TAG = VideoResolution.class.getSimpleName();


	VideoResolution(int id, int verticalPixels) {
		this.id = id;
		this.verticalPixels = verticalPixels;
	}


	@Override
	public String toString() {
		return verticalPixels + "p";
	}


	/**
	 * Returns a {@link VideoResolution} that is next-step lower than the current one.
	 *
	 * @return A lower {@link VideoResolution}.
	 */
	public VideoResolution getLowerVideoResolution() {
		if (this == RES_UNKNOWN)
			return RES_UNKNOWN;

		VideoResolution[] resList = VideoResolution.values();
		return resList[this.id];
	}


	public static VideoResolution resolutionToVideoResolution(String resolution) {
		VideoResolution[] resList = VideoResolution.values();

		for (VideoResolution res : resList) {
			if (res.verticalPixels == Integer.parseInt(resolution.substring(0, resolution.length()-1)))
				return res;
		}

		return RES_UNKNOWN;
	}


	/**
	 * Converts the ID of a {@link VideoResolution} to an instance of {@link VideoResolution}.
	 *
	 * @param resIdString Video resolution ID
	 * @return {@link VideoResolution}
	 */
	public static VideoResolution videoResIdToVideoResolution(String resIdString) {
		VideoResolution[] resList = VideoResolution.values();
		int resId = Integer.parseInt(resIdString);

		for (VideoResolution res : resList) {
			if (res.id == resId)
				return res;
		}

		return RES_UNKNOWN;
	}


	/**
	 * Returns a list of video resolutions names (e.g. "720p" ...).
	 *
	 * @return List of {@link String}.
	 */
	public static String[] getAllVideoResolutionsNames() {
		VideoResolution[] resList = VideoResolution.values();
		String[] resStringList = new String[resList.length - 1];

		for (int i = 1;  i < resList.length;  i++) {
			resStringList[i-1] = resList[i].toString();
		}

		return resStringList;
	}


	/**
	 * Returns a list of video resolutions IDs.
	 *
	 * @return List of {@link String}.
	 */
	public static String[] getAllVideoResolutionsIds() {
		VideoResolution[] resList = VideoResolution.values();
		String[] resStringList = new String[resList.length - 1];

		for (int i = 1;  i < resList.length;  i++) {
			resStringList[i-1] = Integer.toString(resList[i].id);
		}

		return resStringList;
	}

	/**
	 * Configures the given preference to list all video resolution.
	 *
	 * @param preference
	 */
	public static void setupListPreferences(ListPreference preference) {
		preference.setEntries(VideoResolution.getAllVideoResolutionsNames());
		preference.setEntryValues(VideoResolution.getAllVideoResolutionsIds());
	}
}
