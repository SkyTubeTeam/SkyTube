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

import com.google.api.client.util.DateTime;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;

import org.schabi.newpipe.extractor.exceptions.ExtractionException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import free.rm.skytube.businessobjects.Logger;
import free.rm.skytube.businessobjects.YouTube.POJOs.CardData;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeAPI;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeAPIKey;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;
import free.rm.skytube.businessobjects.YouTube.newpipe.ChannelId;
import free.rm.skytube.businessobjects.YouTube.newpipe.NewPipeService;
import free.rm.skytube.businessobjects.db.SubscriptionsDb;

/**
 * Returns the videos of a channel.
 *
 * <p>The is the "full" edition (as opposed to {@link NewPipeChannelVideos}) which is used for users
 * that ARE using their own YouTube API key.  This is as YouTube.Search.List consumes 100 unit and
 * the results are accurate.
 */
public class GetChannelVideosFull extends GetYouTubeVideos {

	private ChannelId channelId;
	private boolean filterSubscribedVideos;

	private YouTube.Search.List videosList = null;

	private static final Long	MAX_RESULTS = 45L;

	@Override
	public void init() throws IOException {
		videosList = YouTubeAPI.create().search().list("id");
		videosList.setFields("items(id/videoId), nextPageToken");
		videosList.setKey(YouTubeAPIKey.get().getYouTubeAPIKey());
		videosList.setType("video");
		//videosList.setRegionCode(getPreferredRegion());	// there is a bug in V3 API, so this does not work:  https://code.google.com/p/gdata-issues/issues/detail?id=6383 and https://code.google.com/p/gdata-issues/issues/detail?id=6913
		videosList.setSafeSearch("none");
		videosList.setMaxResults(MAX_RESULTS);
		videosList.setOrder("date");
		nextPageToken = null;
	}

	@Override
	public void resetKey() {
		videosList.setKey(YouTubeAPIKey.get().getYouTubeAPIKey());
	}

	@Override
	public List<CardData> getNextVideos() {
		setLastException(null);

		List<CardData> videosList = null;

		if (!noMoreVideoPages()) {
			try {
				// set the page token/id to retrieve
				this.videosList.setPageToken(nextPageToken);

				// communicate with YouTube
				SearchListResponse searchResponse = this.videosList.execute();

				// get videos
				List<SearchResult> searchResultList = searchResponse.getItems();
				if (searchResultList != null) {
					videosList = getVideosList(searchResultList);
				}

				// set the next page token
				nextPageToken = searchResponse.getNextPageToken();

				// if nextPageToken is null, it means that there are no more videos
				if (nextPageToken == null)
					noMoreVideoPages = true;
			} catch (IOException ex) {
				setLastException(ex);
				Logger.e(this, "Error has occurred while getting Featured Videos:" + ex.getMessage(), ex);
			}
		}

		return videosList;
	}


	/**
	 * YouTube's search functionality (i.e. {@link SearchResult} does not return enough information
	 * about the YouTube videos.
	 *
	 * <p>Hence, we need to submit the video IDs to YouTube to retrieve more information about the
	 * given video list.</p>
	 *
	 * @param searchResultList Search results
	 * @return List of {@link YouTubeVideo}s.
	 * @throws IOException
	 */
	private List<CardData> getVideosList(List<SearchResult> searchResultList) throws IOException {
		List<String> videoIds = new ArrayList<>(searchResultList.size());

		// append the video IDs into a strings (CSV)
		for (SearchResult res : searchResultList) {
			videoIds.add(res.getId().getVideoId());
		}

		return getVideoListFromIds(videoIds);
	}


	/**
	 * Set the channel id.
	 *
	 * @param channelId	Channel ID.
	 * @param filterSubscribedVideos to filter out the subscribed videos.
	 */
	public void setChannelQuery(ChannelId channelId, boolean filterSubscribedVideos) {
		this.channelId = channelId;
		this.filterSubscribedVideos = filterSubscribedVideos;
		if (videosList != null) {
			videosList.setChannelId(channelId.getRawId());
		}
	}

	@Override
	public void setQuery(String query) {
		setChannelQuery(new ChannelId(query), false);
	}

	public void setPublishedAfter(long timeInMs) {
		if (videosList != null) {
			videosList.setPublishedAfter(new DateTime(timeInMs));
		}
	}

	private List<CardData> getVideoListFromIds(List<String> videoIds) throws IOException {
		if (videoIds != null && !videoIds.isEmpty() && channelId != null && filterSubscribedVideos) {
			final Set<String> videosByChannel = SubscriptionsDb.getSubscriptionsDb().getSubscribedChannelVideosByChannel(channelId);
			videoIds.removeAll(videosByChannel);
		}
		return impGetVideoListFromIds(videoIds);
	}

	/**
	 * <p>Hence, we need to submit the video IDs to YouTube to retrieve more information about the
	 * given video list.</p>
	 *
	 * @param videoIds Search results
	 * @return List of {@link YouTubeVideo}s.
	 * @throws IOException
	 */
	private List<CardData> impGetVideoListFromIds(List<String> videoIds) throws IOException {
		if (videoIds == null || videoIds.isEmpty()) {
			return Collections.emptyList();
		}
		return getVideoListFromIdsWithAPI(videoIds);
	}

	private List<CardData> getWithNewPipe(List<String> videoIds) {
		NewPipeService newPipe = NewPipeService.get();
		List<CardData> result = new ArrayList<>(videoIds.size());
		for (String id : videoIds) {
			try {
				result.add(newPipe.getDetails(id));
			} catch (ExtractionException | IOException e) {
				Logger.e(this, "Unable to fetch "+id+", error:"+ e.getMessage(), e);
			}
		}
		return result;
	}

	private List<CardData> getVideoListFromIdsWithAPI(List<String> videoIds) throws IOException {
		final StringBuilder videoIdsStr = new StringBuilder();
		try {

			// append the video IDs into a strings (CSV)
			for (String id : videoIds) {
				videoIdsStr.append(id);
				videoIdsStr.append(',');
			}

			if (videoIdsStr.length() > 0) {
				videoIdsStr.setLength(videoIdsStr.length() - 1);
			}
			// get video details by supplying the videos IDs
			GetVideosDetailsByIDs getVideo = new GetVideosDetailsByIDs();
			getVideo.init(videoIdsStr.toString());
			Logger.i(this, "getVideoList light from %s id, video ids: %s", videoIds.size(), videoIdsStr);

			List<CardData> cards = getVideo.getNextVideos();
			if (cards == null || cards.isEmpty()) {
				Logger.e(this, "Unable to fetch with API, use Newpipe,ids="+videoIdsStr);
				return getWithNewPipe(videoIds);
			}
			return cards;
		} catch (IOException e) {
			Logger.e(this, "Unable to fetch with API, revert to newpipe:"+e.getMessage()+",ids="+videoIdsStr, e);
			setLastException(e);
			return getWithNewPipe(videoIds);
		}
	}

}
