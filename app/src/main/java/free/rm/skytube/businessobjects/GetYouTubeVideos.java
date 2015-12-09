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

package free.rm.skytube.businessobjects;

import android.content.Context;

import com.google.api.services.youtube.model.Video;

import java.io.IOException;
import java.util.List;

/**
 * Returns a list of YouTube videos.
 *
 * <p>Do not run this directly, but rather use {@link GetYouTubeVideosTask}.</p>
 */
public interface GetYouTubeVideos {

	/**
	 * Initialise this object.
	 *
	 * @param context {@link Context}
	 * @throws IOException
	 */
	void init(Context context) throws IOException;


	/**
	 * Gets the next page of videos.
	 *
	 * @return List of {@link Video}s
	 */
	List<Video> getNextVideos();


	/**
	 * @return True if YouTube states that there will be no more video pages; false otherwise.
	 */
	boolean noMoreVideoPages();

}
