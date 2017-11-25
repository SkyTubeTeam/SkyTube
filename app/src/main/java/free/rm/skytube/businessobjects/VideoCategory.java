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

/**
 * Represents a video category/group.
 */
public enum VideoCategory {
	/** Featured videos */
	FEATURED (0),
	/** Most popular videos */
	MOST_POPULAR (1),
	/** Videos related to a search query */
	SEARCH_QUERY (2),
	/** Videos that are owned by a channel */
	CHANNEL_VIDEOS (3),
	/** Videos pertaining to the user's subscriptions feed */
	SUBSCRIPTIONS_FEED_VIDEOS (4),
	/** Videos bookmarked by the user */
	BOOKMARKS_VIDEOS (5),
	/** Videos belonging to a playlist */
	PLAYLIST_VIDEOS (7);

	// *****************
	// DON'T FORGET to update #createGetYouTubeVideos() methods...
	// *****************

	private final int id;


	VideoCategory(int id) {
		this.id = id;
	}

	/**
	 * Creates a new instance of {@link GetFeaturedVideos} or {@link GetMostPopularVideos} ...etc
	 * depending on the video category.
	 *
	 * @return New instance of {@link GetYouTubeVideos}.
	 */
	public GetYouTubeVideos createGetYouTubeVideos() {
		if (id == FEATURED.id)
			return new GetFeaturedVideos();
		else if (id == MOST_POPULAR.id)
			return new GetMostPopularVideos();
		else if (id == SEARCH_QUERY.id)
			return new GetYouTubeVideoBySearch();
		else if (id == CHANNEL_VIDEOS.id)
			return new GetChannelVideos();
		else if (id == SUBSCRIPTIONS_FEED_VIDEOS.id)
			return new GetSubscriptionsVideos();
		else if (id == BOOKMARKS_VIDEOS.id)
			return new GetBookmarksVideos();
		else if (id == PLAYLIST_VIDEOS.id)
			return new GetPlaylistVideos();

		// this will notify the developer is he forgot to amend this method when a new type is added
		throw new UnsupportedOperationException();
	}

}
