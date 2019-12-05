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

import android.util.Log;

import free.rm.skytube.businessobjects.YouTube.GetBookmarksVideos;
import free.rm.skytube.businessobjects.YouTube.GetChannelVideosFull;
import free.rm.skytube.businessobjects.YouTube.GetChannelVideosInterface;
import free.rm.skytube.businessobjects.YouTube.GetChannelVideosLite;
import free.rm.skytube.businessobjects.YouTube.GetDownloadedVideos;
import free.rm.skytube.businessobjects.YouTube.GetFeaturedVideos;
import free.rm.skytube.businessobjects.YouTube.GetMostPopularVideos;
import free.rm.skytube.businessobjects.YouTube.GetPlaylistVideos;
import free.rm.skytube.businessobjects.YouTube.GetYouTubeVideoBySearch;
import free.rm.skytube.businessobjects.YouTube.GetYouTubeVideos;
import free.rm.skytube.businessobjects.YouTube.NewPipeChannelVideos;
import free.rm.skytube.businessobjects.YouTube.NewPipeVideoBySearch;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeAPIKey;
import free.rm.skytube.businessobjects.YouTube.newpipe.NewPipeService;
import free.rm.skytube.businessobjects.db.Tasks.GetSubscriptionsVideosFromDb;

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
	BOOKMARKS_VIDEOS (5, false),
	/** Videos belonging to a playlist */
	PLAYLIST_VIDEOS (7),
	/** Videos that have been downloaded */
	DOWNLOADED_VIDEOS (8, false);

	// *****************
	// DON'T FORGET to update #createGetYouTubeVideos() methods...
	// *****************

	private final int id;
	private final boolean videoFiltering;

	VideoCategory(int id) {
		this.id = id;
		this.videoFiltering = true;
	}

	VideoCategory(int id, boolean videoFiltering) {
		this.id = id;
		this.videoFiltering = videoFiltering;
	}

	/**
	 * Creates a new instance of {@link GetFeaturedVideos} or {@link GetMostPopularVideos} ...etc
	 * depending on the video category.
	 *
	 * @return New instance of {@link GetYouTubeVideos}.
	 */
	public GetYouTubeVideos createGetYouTubeVideos() {
		if (id == FEATURED.id) {
			return new GetFeaturedVideos();
		} else if (id == MOST_POPULAR.id) {
			return new GetMostPopularVideos();
		} else if (id == SEARCH_QUERY.id) {
			if (NewPipeService.isPreferred()) {
				return new NewPipeVideoBySearch();
			} else {
				return new GetYouTubeVideoBySearch();
			}
		} else if (id == CHANNEL_VIDEOS.id) {
			return (GetYouTubeVideos) createChannelVideosFetcher();
		} else if (id == SUBSCRIPTIONS_FEED_VIDEOS.id) {
			return new GetSubscriptionsVideosFromDb();
		} else if (id == BOOKMARKS_VIDEOS.id) {
			return new GetBookmarksVideos();
		} else if (id == PLAYLIST_VIDEOS.id) {
			return new GetPlaylistVideos();
		} else if (id == DOWNLOADED_VIDEOS.id) {
			return new GetDownloadedVideos();
		}
		// this will notify the developer that he forgot to edit this method when a new type is added
		throw new UnsupportedOperationException();
	}

	public boolean isVideoFilteringEnabled() {
		return videoFiltering;
	}


	/**
	 * Create an appropriate class to get videos of a channel.
	 *
	 * <p>This class will detect if the user is using his own YouTube API key or not... if they are, then
	 * we are going to use {@link GetChannelVideosFull}; otherwise we are going to use
	 * {@link GetChannelVideosLite}.</p>
	 */
	public static GetChannelVideosInterface createChannelVideosFetcher() {
		if (NewPipeService.isPreferred()) {
			return new NewPipeChannelVideos();
		}
		if (YouTubeAPIKey.get().isUserApiKeySet()) {
			Log.d(VideoCategory.class.getName(), "Using GetChannelVideosFull...");
			return new GetChannelVideosFull();
		} else {
			Log.d(VideoCategory.class.getName(), "Using NewPipeChannelVideos...");
			return new NewPipeChannelVideos();
		}

	}

}
