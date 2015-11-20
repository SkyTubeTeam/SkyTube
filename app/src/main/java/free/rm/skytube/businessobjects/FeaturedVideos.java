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
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;

import java.io.IOException;
import java.util.List;

import free.rm.skytube.R;

/**
 *
 */
public class FeaturedVideos {

	private YouTube.Videos.List featuredVideosList = null;
	private String nextPageToken = null;

	private static String	TAG = "FeaturedVideos";
	private static Long		MAX_RESULTS = 50L;


	public void init(Context context) throws IOException {
		HttpTransport	httpTransport = AndroidHttp.newCompatibleTransport();
		JsonFactory		jsonFactory = com.google.api.client.extensions.android.json.AndroidJsonFactory.getDefaultInstance();
		YouTube			youtube = new YouTube.Builder(httpTransport, jsonFactory, null /*timeout here?*/).build();

		featuredVideosList = youtube.videos().list("id, snippet, statistics, contentDetails");
		featuredVideosList.setFields("items(id, snippet/publishedAt, snippet/title, snippet/channelTitle, "
									+"snippet/thumbnails/high, contentDetails/duration, statistics)");
		featuredVideosList.setKey(context.getString(R.string.API_KEY));
		featuredVideosList.setChart("mostPopular");
		featuredVideosList.setMaxResults(MAX_RESULTS);
		nextPageToken = null;
	}


	public List<Video> getNextVideos() {
		List<Video> searchResultList = null;

		try {
			/// TODO featuredVideosList.setPage
			VideoListResponse searchResponse = featuredVideosList.execute();
			/// TODO nextPageToken = searchResponse.getNextPageToken();

			searchResultList = searchResponse.getItems();
		} catch (IOException e) {
			Log.e(TAG, "Error has occurred while getting Featured Videos.", e);
		}

		return searchResultList;
	}

}
