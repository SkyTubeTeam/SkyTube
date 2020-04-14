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

import android.util.Log;

import com.google.api.client.util.DateTime;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Activity;
import com.google.api.services.youtube.model.ActivityListResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import free.rm.skytube.businessobjects.YouTube.POJOs.CardData;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeAPI;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeAPIKey;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;
import free.rm.skytube.businessobjects.db.SubscriptionsDb;

/**
 * Returns the videos of a channel.
 *
 * <p>The is the "lite" edition (as opposed to {@link GetChannelVideosFull}) which is used for users
 * that are NOT using their own YouTube API key.  This is as YouTube.Activities.List only consumes
 * 1 unit - however the results might not always be accurate.
 */
public class GetChannelVideosLite extends GetYouTubeVideos implements GetChannelVideosInterface {

	private YouTube.Activities.List activitiesList;
	private String channelId;
	private boolean filterSubscribedVideos;

	private static final String	TAG = GetChannelVideosLite.class.getSimpleName();
	private static final Long	MAX_RESULTS = 45L;


	@Override
	public void init() throws IOException {
		activitiesList = YouTubeAPI.create().activities().list("contentDetails");
		activitiesList.setFields("items(contentDetails/upload/videoId), nextPageToken");
		activitiesList.setKey(YouTubeAPIKey.get().getYouTubeAPIKey());
		activitiesList.setMaxResults(MAX_RESULTS);
		nextPageToken = null;
	}


	@Override
	public void setPublishedAfter(long timeInMs) {
		if(activitiesList != null) {
			activitiesList.setPublishedAfter(new DateTime(timeInMs));
		}
	}

	@Override
	public void resetKey() {
		activitiesList.setKey(YouTubeAPIKey.get().getYouTubeAPIKey());
	}

	@Override
	public List<CardData> getNextVideos() {
		setLastException(null);
		List<CardData> videosList = null;

		if (!noMoreVideoPages) {
			try {
				activitiesList.setPageToken(nextPageToken);

				ActivityListResponse response = activitiesList.execute();
				List<Activity> activityList = response.getItems();
				if(activityList != null && !activityList.isEmpty()) {
					videosList = getVideosList(activityList);
				}

				nextPageToken = response.getNextPageToken();

				if(nextPageToken == null)
					noMoreVideoPages = true;

			} catch (IOException ex) {
				setLastException(ex);
				Log.e(TAG, ex.getLocalizedMessage());
			}
		}


		return videosList;
	}


	/**
	 * YouTube's activity functionality (i.e. {@link Activity} does not return enough information
	 * about the YouTube videos.
	 *
	 * <p>Hence, we need to submit the video IDs to YouTube to retrieve more information about the
	 * given video list.</p>
	 *
	 * @param activityList Search results
	 * @return List of {@link YouTubeVideo}s.
	 * @throws IOException
	 */
	private List<CardData> getVideosList(List<Activity> activityList) throws IOException {
        List<String> videoIds = new ArrayList<>(activityList.size());

		// append the video IDs into a strings (CSV)
		for (Activity res : activityList) {
			videoIds.add(res.getContentDetails().getUpload().getVideoId());
		}
		if (!videoIds.isEmpty() && channelId != null && filterSubscribedVideos) {
			final Set<String> videosByChannel = SubscriptionsDb.getSubscriptionsDb().getSubscribedChannelVideosByChannel(channelId);
			videoIds.removeAll(videosByChannel);
		}
        return getVideoListFromIds(videoIds);
	}


	/**
	 * Set the channel id.
	 *
	 * @param channelId	Channel ID.
     * @param filterSubscribedVideos to filter out the subscribed videos.
	 */
	@Override
	public void setChannelQuery(String channelId, boolean filterSubscribedVideos) {
		this.channelId = channelId;
		this.filterSubscribedVideos = filterSubscribedVideos;
		if (activitiesList != null) {
			activitiesList.setChannelId(channelId);
		} else {
			throw new NullPointerException("activitiesList not initialized!");
		}
	}

    @Override
    public void setQuery(String query) {
        setChannelQuery(query, false);
    }
}
