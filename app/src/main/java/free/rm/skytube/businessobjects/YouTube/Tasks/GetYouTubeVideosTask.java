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

package free.rm.skytube.businessobjects.YouTube.Tasks;

import android.widget.Toast;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;

import java.util.ArrayList;
import java.util.List;

import free.rm.skytube.businessobjects.AsyncTaskParallel;
import free.rm.skytube.businessobjects.VideoCategory;
import free.rm.skytube.businessobjects.YouTube.GetYouTubeVideos;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;
import free.rm.skytube.businessobjects.YouTube.VideoBlocker;
import free.rm.skytube.businessobjects.db.SubscriptionsDb;
import free.rm.skytube.gui.businessobjects.adapters.VideoGridAdapter;


/**
 * An asynchronous task that will retrieve YouTube videos and displays them in the supplied Adapter.
 */
public class GetYouTubeVideosTask extends AsyncTaskParallel<Void, Void, List<YouTubeVideo>> {

	/** Object used to retrieve the desired YouTube videos. */
	private final GetYouTubeVideos getYouTubeVideos;

	/** The Adapter where the retrieved videos will be displayed. */
	private final VideoGridAdapter	videoGridAdapter;

	/** SwipeRefreshLayout will be used to display the progress bar */
	private final SwipeRefreshLayout  swipeRefreshLayout;

	private YouTubeChannel channel;
	private final boolean clearList;

	private final VideoGridAdapter.Callback callback;


	/**
	 * Constructor to get youtube videos as part of a swipe to refresh. Since this functionality has its own progress bar, we'll
	 * skip showing our own.
	 *
	 * @param getYouTubeVideos The object that does the actual fetching of videos.
	 * @param videoGridAdapter The grid adapter the videos will be added to.
	 * @param swipeRefreshLayout The layout which shows animation about the refresh process.
	 * @param clearList Clear the list before adding new values to it.
	 * @param callback To notify about the updated {@link VideoGridAdapter}
	 */
	public GetYouTubeVideosTask(GetYouTubeVideos getYouTubeVideos, VideoGridAdapter videoGridAdapter, SwipeRefreshLayout swipeRefreshLayout, boolean clearList,
								VideoGridAdapter.Callback callback) {
		this.getYouTubeVideos = getYouTubeVideos;
		this.videoGridAdapter = videoGridAdapter;
		this.swipeRefreshLayout = swipeRefreshLayout;
		this.clearList = clearList;
		this.callback = callback;
	}


	@Override
	protected void onPreExecute() {
		// if this task is being called by ChannelBrowserFragment, then get the channel the user is browsing
		channel = videoGridAdapter.getYouTubeChannel();

		if (swipeRefreshLayout != null) {
			swipeRefreshLayout.setRefreshing(true);
		}
	}


	@Override
	protected List<YouTubeVideo> doInBackground(Void... params) {
		if (isCancelled()) {
			return null;
		}

		// get videos from YouTube or the database.
		List<YouTubeVideo> videosList = getNextVideos();

		if (videosList != null) {
			// filter videos
			if (videoGridAdapter.getCurrentVideoCategory().isVideoFilteringEnabled()) {
				videosList = new VideoBlocker().filter(videosList);
			}

			if (channel != null && channel.isUserSubscribed()) {
				for (YouTubeVideo video : videosList) {
					channel.addYouTubeVideo(video);
				}
				SubscriptionsDb.getSubscriptionsDb().saveChannelVideos(channel);
			}
		}

		return videosList;
	}

	private List<YouTubeVideo> getNextVideos() {
		if (this.clearList && videoGridAdapter.getCurrentVideoCategory() == VideoCategory.SUBSCRIPTIONS_FEED_VIDEOS) {
			return collectUntil(videoGridAdapter.getItemCount());
		} else {
			return getYouTubeVideos.getNextVideos();
		}
	}

	private List<YouTubeVideo> collectUntil(int currentSize) {
		List<YouTubeVideo> result = new ArrayList<>(currentSize);
		boolean hasNew = false;
		do {
			List<YouTubeVideo> videosList = getYouTubeVideos.getNextVideos();
			hasNew = !videosList.isEmpty();
			result.addAll(videosList);
		} while(result.size() < currentSize && hasNew);
		return result;
	}

	@Override
	protected void onPostExecute(List<YouTubeVideo> videosList) {
		if (getYouTubeVideos.getLastException() instanceof GoogleJsonResponseException) {
			GoogleJsonResponseException exception = (GoogleJsonResponseException) getYouTubeVideos.getLastException();
			String message = exception.getDetails().getMessage();
			if (message != null) {
				Toast.makeText(videoGridAdapter.getContext(), message, Toast.LENGTH_LONG).show();
			}
		}
		if (this.clearList) {
			videoGridAdapter.clearList();
		}
		videoGridAdapter.appendList(videosList);

		if (callback != null) {
			callback.onVideoGridUpdated(videoGridAdapter.getItemCount());
		}
		if(swipeRefreshLayout != null) {
			swipeRefreshLayout.setRefreshing(false);
		}
	}


	@Override
	protected void onCancelled() {
		if(swipeRefreshLayout != null) {
			swipeRefreshLayout.setRefreshing(false);
		}
	}

}
