/*
 * SkyTube
 * Copyright (C) 2016  Ramon Mifsud
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

package free.rm.skytube.businessobjects.VideoStream;

import android.util.Log;

import com.google.api.client.util.DateTime;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Activity;
import com.google.api.services.youtube.model.ActivityListResponse;

import java.io.IOException;
import java.util.List;

import free.rm.skytube.BuildConfig;
import free.rm.skytube.businessobjects.GetVideosDetailsByIDs;
import free.rm.skytube.businessobjects.GetYouTubeVideos;
import free.rm.skytube.businessobjects.YouTubeAPI;
import free.rm.skytube.businessobjects.YouTubeVideo;

/**
 * Returns the videos of a channel.  The channel is specified by calling {@link #setQuery(String)}.
 */
public class GetChannelVideos extends GetYouTubeVideos {

	protected YouTube.Activities.List activitiesList;

	private static final String	TAG = GetChannelVideos.class.getSimpleName();
	protected static final Long	MAX_RESULTS = 45L;

	private String channelId;

	@Override
	public void init() throws IOException {
		activitiesList = YouTubeAPI.create().activities().list("contentDetails");
		activitiesList.setFields("items(contentDetails/upload/videoId), nextPageToken");
		activitiesList.setKey(BuildConfig.YOUTUBE_API_KEY);
		activitiesList.setMaxResults(MAX_RESULTS);
		nextPageToken = null;
	}

	public void setPublishedAfter(DateTime dateTime) {
		if(activitiesList != null)
			activitiesList.setPublishedAfter(dateTime);
	}

	@Override
	public List<YouTubeVideo> getNextVideos() {
		List<YouTubeVideo> videosList = null;

		if (!noMoreVideoPages) {
			try {
				activitiesList.setPageToken(nextPageToken);

				ActivityListResponse response = activitiesList.execute();
				List<Activity> activityList = response.getItems();
				if(activityList != null && activityList.size() > 0) {
					videosList = getVideosList(activityList);
				}

				nextPageToken = response.getNextPageToken();

				if(nextPageToken == null)
					noMoreVideoPages = true;

			} catch (IOException ex) {
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
	private List<YouTubeVideo> getVideosList(List<Activity> activityList) throws IOException {
		StringBuilder videoIds = new StringBuilder();

		// append the video IDs into a strings (CSV)
		for (Activity res : activityList) {
			videoIds.append(res.getContentDetails().getUpload().getVideoId());
			videoIds.append(',');
		}

		// get video details by supplying the videos IDs
		GetVideosDetailsByIDs getVideo = new GetVideosDetailsByIDs();
		getVideo.init(videoIds.toString());

		return getVideo.getNextVideos();
	}

	@Override
	public void setQuery(String query) {
		if(activitiesList != null) {
			channelId = query;
			activitiesList.setChannelId(query);
		}
	}

	@Override
	public boolean noMoreVideoPages() {
		return noMoreVideoPages;
	}
}
