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

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.CommentThread;
import com.google.api.services.youtube.model.CommentThreadListResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import free.rm.skytube.R;
import free.rm.skytube.gui.app.SkyTubeApp;

/**
 * Queries the YouTube service and gets the comments of a video.
 */
public class GetCommentThreads {

	private static final Long	MAX_RESULTS = 50L;
	private static final String	TAG = GetCommentThreads.class.getSimpleName();

	public List<YouTubeCommentThread> get(String videoId) {
		List<YouTubeCommentThread> commentThreadList = new ArrayList<>();
		HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
		JsonFactory jsonFactory = com.google.api.client.extensions.android.json.AndroidJsonFactory.getDefaultInstance();

		YouTube youtube = new YouTube.Builder(httpTransport, jsonFactory, null /*timeout here?*/).build();

		try {
			CommentThreadListResponse commentsResponse = youtube.commentThreads()
					.list("snippet, replies")
					.setKey(SkyTubeApp.getStr(R.string.API_KEY))
					.setVideoId(videoId)
					.setTextFormat("plainText")
					.setMaxResults(MAX_RESULTS)
					.setOrder("relevance")
					.execute();
			List<CommentThread> videoComments = commentsResponse.getItems();

			if (!videoComments.isEmpty()) {
				for (CommentThread thread : videoComments) {
					commentThreadList.add( new YouTubeCommentThread(thread) );
				}
			}
		} catch(IOException ex) {
			Log.e(TAG, "An error has occurred while retrieving comments for video with id="+videoId, ex);
		}

		return commentThreadList;
	}
}
