/*
 * SkyTube
 * Copyright (C) 2015  Ramon Mifsud
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package free.rm.skytube.businessobjects;

import android.util.Log;

/**
 * Represents a video category/group.
 */
public enum VideoCategory {
	/** Featured videos */
	FEATURED (0),

	/** Most popular videos */
	MOST_POPULAR (1);

	// *****************
	// DON'T FORGET to update getVideoCategory() and toGetYouTubeVideos methods...
	// *****************

	private final int id;
	private static final String TAG = VideoCategory.class.getSimpleName();


	VideoCategory(int id) {
		this.id = id;
	}


	/**
	 * Convert the given id integer number to {@link VideoCategory}.
	 *
	 * @param id ID number representing the position of the item in video_categories array (see
	 *           the respective strings XML file).
	 *
	 * @return A new instance of {@link VideoCategory}.
	 */
	public static VideoCategory getVideoCategory(int id) {
		if (id < FEATURED.id  ||  id > MOST_POPULAR.id) {
			Log.e(TAG, "ILLEGAL ID VALUE=" + id);
			Log.e(TAG, "Do NOT forget to update VideoCategories enum.");
			id = FEATURED.id;
		}

		return VideoCategory.values()[id];
	}


	/**
	 * Creates a new instance of {@link GetFeaturedVideos} or {@link GetMostPopularVideos} depending
	 * on the video category.
	 *
	 * @return New instance of {@link GetYouTubeVideos}.
	 */
	public GetYouTubeVideos createGetYouTubeVideos() {
		if (id == FEATURED.id)
			return new GetFeaturedVideos();
		else if (id == MOST_POPULAR.id)
			return new GetMostPopularVideos();

		return null;
	}

}
