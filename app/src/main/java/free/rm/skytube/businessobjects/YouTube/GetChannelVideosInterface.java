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

package free.rm.skytube.businessobjects.YouTube;

import java.io.IOException;
import java.util.List;

import free.rm.skytube.businessobjects.YouTube.POJOs.CardData;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;

/**
 * Interface to be used by {@link GetChannelVideosFull} and {@link GetChannelVideosLite}.
 */
public interface GetChannelVideosInterface {

	/**
	 * Used to retrieve channel videos published after the specified date.
	 */
	void setPublishedAfter(long timeInMs);


	/**
	 * Initialise this object.
	 *
	 * @throws IOException
	 */
	void init() throws IOException;


	/**
	 * Sets user's query. [optional]
	 */
	void setChannelQuery(String channelId, boolean filter);


	/**
	 * Gets the next page of videos.
	 *
	 * @return List of {@link YouTubeVideo}s.
	 */
	List<CardData> getNextVideos();

}
