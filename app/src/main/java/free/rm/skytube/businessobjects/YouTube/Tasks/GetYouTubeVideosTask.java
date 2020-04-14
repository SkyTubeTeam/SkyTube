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

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.List;

import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.AsyncTaskParallel;
import free.rm.skytube.businessobjects.VideoCategory;
import free.rm.skytube.businessobjects.YouTube.GetYouTubeVideos;
import free.rm.skytube.businessobjects.YouTube.POJOs.CardData;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;
import free.rm.skytube.businessobjects.YouTube.VideoBlocker;
import free.rm.skytube.businessobjects.db.SubscriptionsDb;
import free.rm.skytube.gui.businessobjects.adapters.VideoGridAdapter;


/**
 * An asynchronous task that will retrieve YouTube videos and displays them in the supplied Adapter.
 */
public class GetYouTubeVideosTask extends AsyncTaskParallel<Void, Void, List<CardData>> {

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
		getYouTubeVideos.resetKey();
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
	protected List<CardData> doInBackground(Void... params) {
		if (isCancelled()) {
			return null;
		}

		// get videos from YouTube or the database.
		List<CardData> videosList = getNextVideos();

		if (videosList != null) {
			// filter videos
			if (videoGridAdapter.getCurrentVideoCategory().isVideoFilteringEnabled()) {
				videosList = new VideoBlocker().filter(videosList);
			}

			if (channel != null && channel.isUserSubscribed()) {
				for (CardData video : videosList) {
					if (video instanceof YouTubeVideo) {
						channel.addYouTubeVideo((YouTubeVideo) video);
					}
				}
				SubscriptionsDb.getSubscriptionsDb().saveChannelVideos(channel.getYouTubeVideos(), channel.getId());
			}
		}

		return videosList;
	}

	private List<CardData> getNextVideos() {
		if (this.clearList && videoGridAdapter.getCurrentVideoCategory() == VideoCategory.SUBSCRIPTIONS_FEED_VIDEOS) {
			return collectUntil(videoGridAdapter.getItemCount());
		} else {
			return getYouTubeVideos.getNextVideos();
		}
	}

	private List<CardData> collectUntil(int currentSize) {
		List<CardData> result = new ArrayList<>(currentSize);
		boolean hasNew;
		do {
			List<CardData> videosList = getYouTubeVideos.getNextVideos();
			hasNew = !videosList.isEmpty();
			result.addAll(videosList);
		} while(result.size() < currentSize && hasNew);
		return result;
	}

	@Override
	protected void onPostExecute(List<CardData> videosList) {
		SkyTubeApp.notifyUserOnError(videoGridAdapter.getContext(), getYouTubeVideos.getLastException());

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
