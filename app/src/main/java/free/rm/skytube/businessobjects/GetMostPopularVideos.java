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

import android.content.Context;
import android.util.Log;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.Video;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;

import free.rm.skytube.R;

/**
 * Get today's most popular YouTube videos.
 */
public class GetMostPopularVideos implements GetYouTubeVideos {

	private YouTube.Search.List videosList = null;
	private String nextPageToken = null;
	private boolean noMoreVideoPages = false;
	private Context context;

	private static final String	TAG = GetMostPopularVideos.class.getSimpleName();
	private static final Long	MAX_RESULTS = 45L;


	@Override
	public void init(Context context) throws IOException {
		this.context = context;

		HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
		JsonFactory jsonFactory = com.google.api.client.extensions.android.json.AndroidJsonFactory.getDefaultInstance();
		YouTube youtube = new YouTube.Builder(httpTransport, jsonFactory, null /*timeout here?*/).build();

		videosList = youtube.search().list("id");
		videosList.setFields("items(id/videoId), nextPageToken");
		videosList.setKey(context.getString(R.string.API_KEY));
		videosList.setType("video");
		videosList.setMaxResults(MAX_RESULTS);
		videosList.setPublishedAfter(getTodaysDate());
		videosList.setOrder("viewCount");
		nextPageToken = null;
	}


	/**
	 * Returns today's date whose time is 00:00 (midnight).
	 *
	 * @return Today's date
	 */
	private DateTime getTodaysDate() {
		String dateRFC3339 = String.format("%d-%02d-%02dT00:00:00Z", Calendar.getInstance().get(Calendar.YEAR), Calendar.getInstance().get(Calendar.MONTH)+1, Calendar.getInstance().get(Calendar.DAY_OF_MONTH));
		return new DateTime(dateRFC3339);
	}


	@Override
	public List<Video> getNextVideos() {
		List<Video> videosList = null;

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
				Log.e(TAG, ex.getLocalizedMessage());
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
	 * @return List of {@link Video}s.
	 * @throws IOException
	 */
	private List<Video> getVideosList(List<SearchResult> searchResultList) throws IOException {
		StringBuilder videoIds = new StringBuilder();

		// append the video IDs into a strings (CSV)
		for (SearchResult res : searchResultList) {
			videoIds.append(res.getId().getVideoId());
			videoIds.append(',');
		}

		// get video details by supplying the videos IDs
		GetVideosDetailsByIDs getVideo = new GetVideosDetailsByIDs();
		getVideo.init(context, videoIds.toString());

		return getVideo.getNextVideos();
	}


	@Override
	public boolean noMoreVideoPages() {
		return noMoreVideoPages;
	}

}
