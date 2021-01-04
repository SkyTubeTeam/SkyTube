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
import free.rm.skytube.businessobjects.YouTube.GetYouTubeVideos;
import free.rm.skytube.businessobjects.YouTube.NewPipeChannelVideos;
import free.rm.skytube.businessobjects.YouTube.NewPipePlaylistVideos;
import free.rm.skytube.businessobjects.YouTube.NewPipeVideoBySearch;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeAPIKey;
import free.rm.skytube.businessobjects.YouTube.newpipe.NewPipeService;
import free.rm.skytube.businessobjects.YouTube.newpipe.NewPipeTrendingItems;
import free.rm.skytube.businessobjects.db.Tasks.GetSubscriptionsVideosFromDb;

/**
 * Represents a video category/group.
 */
public enum VideoCategory {
	/** Featured videos */
	FEATURED,
	/** Most popular videos */
	MOST_POPULAR,
	/** Videos related to a search query */
	SEARCH_QUERY ,
	/** Videos that are owned by a channel */
	CHANNEL_VIDEOS ,
	/** Videos pertaining to the user's subscriptions feed */
	SUBSCRIPTIONS_FEED_VIDEOS ,
	/** Videos bookmarked by the user */
	BOOKMARKS_VIDEOS ( false),
	/** Videos belonging to a playlist */
	PLAYLIST_VIDEOS ,
	/** Videos belonging to a playlist, not created by a channel */
	MIXED_PLAYLIST_VIDEOS ,
	/** Videos that have been downloaded */
	DOWNLOADED_VIDEOS ( false);

	// *****************
	// DON'T FORGET to update #createGetYouTubeVideos() methods...
	// *****************

	private final boolean videoFiltering;

	VideoCategory() {
		this.videoFiltering = true;
	}

	VideoCategory(boolean videoFiltering) {
		this.videoFiltering = videoFiltering;
	}

	/**
	 * Creates a new instance of {@link GetFeaturedVideos} or {@link GetMostPopularVideos} ...etc
	 * depending on the video category.
	 *
	 * @return New instance of {@link GetYouTubeVideos}.
	 */
	public GetYouTubeVideos createGetYouTubeVideos() {
		switch (this) {
			case FEATURED: return new GetFeaturedVideos();
			case MOST_POPULAR: return new NewPipeTrendingItems(); //new GetMostPopularVideos();
			case SEARCH_QUERY: return new NewPipeVideoBySearch();
			case CHANNEL_VIDEOS: return (GetYouTubeVideos) createChannelVideosFetcher();
			case SUBSCRIPTIONS_FEED_VIDEOS: return new GetSubscriptionsVideosFromDb();
			case BOOKMARKS_VIDEOS: return new GetBookmarksVideos();
			case MIXED_PLAYLIST_VIDEOS:
			case PLAYLIST_VIDEOS: return new NewPipePlaylistVideos();// new GetPlaylistVideos();
			case DOWNLOADED_VIDEOS: return new GetDownloadedVideos();
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
