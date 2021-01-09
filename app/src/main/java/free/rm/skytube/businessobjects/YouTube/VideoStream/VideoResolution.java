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

import androidx.preference.ListPreference;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;

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
	RES_1080P	(5, 1080),

	// these will be added eventually
	/** 1440p - HD */
	RES_1440P   (6, 1440),
	/** 2160p - 4k */
	RES_2160P (7, 2160);
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
	private static final Pattern NUMBERS = Pattern.compile("[0-9]*");


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


	public boolean isBetterQualityThan(VideoResolution other) {
		return this.ordinal() > other.ordinal();
	}

	public boolean isLessNetworkUsageThan(VideoResolution other) {
		return this != RES_UNKNOWN && this.ordinal() < other.ordinal();
	}

	public static VideoResolution resolutionToVideoResolution(String resolution) {
		VideoResolution[] resList = VideoResolution.values();

		Matcher matcher = NUMBERS.matcher(resolution);
		if (matcher.find()) {
			final int verticalPixel = Integer.parseInt(matcher.group());
			for (VideoResolution res : resList) {
				if (res.verticalPixels == verticalPixel) {
					return res;
				}
			}
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
		if (resIdString == null) {
			return VideoResolution.RES_UNKNOWN;
		}
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
		String[] resStringList = new String[resList.length];

		resStringList[0] = SkyTubeApp.getStr(R.string.no_resolution_specified);
		for (int i = 1;  i < resList.length;  i++) {
			resStringList[i] = resList[i].toString();
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
		String[] resStringList = new String[resList.length];

		for (int i = 0;  i < resList.length;  i++) {
			resStringList[i] = Integer.toString(resList[i].id);
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
