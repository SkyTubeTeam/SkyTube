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

package free.rm.skytube.businessobjects;

import android.util.Log;

import com.google.api.client.util.DateTime;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Activity;
import com.google.api.services.youtube.model.ActivityListResponse;

import java.io.IOException;
import java.util.List;

import free.rm.skytube.BuildConfig;

/**
 * Check if there has been an activity (e.g. a new video has been published) on the given channel.
 */
public class CheckChannelActivity {

	private YouTube.Activities.List activitiesList = null;

	private static final Long	MAX_RESULTS = 30L;
	private static final String	TAG = CheckChannelActivity.class.getSimpleName();


	public void init() throws IOException {
		this.activitiesList = YouTubeAPI.create().activities()
				.list("snippet")
				.setFields("items(snippet/publishedAt)")
				.setKey(BuildConfig.YOUTUBE_API_KEY)
				.setMaxResults(MAX_RESULTS);
	}


	/**
	 * Will check if videos have been been published on the given channel since the user's last
	 * visit to the channel.
	 *
	 * @return	True if at least one video has been published, false otherwise.
	 */
	public boolean checkIfVideosBeenPublishedSinceLastVisit(YouTubeChannel channel) {
		if (channel.getLastVisitTime() < 0)
			return false;

		boolean videosPublished = false;

		this.activitiesList.setChannelId(channel.getId());
		this.activitiesList.setPublishedAfter(new DateTime(channel.getLastVisitTime()));

		try {
			// communicate with YouTube and get the channel's activities
			ActivityListResponse response = activitiesList.execute();
			List<Activity> activitiesList = response.getItems();

			videosPublished = (!activitiesList.isEmpty());
		} catch (IOException ex) {
			Log.e(TAG, "An error has occurred while retrieving activities", ex);
		}

		return videosPublished;
	}

}
