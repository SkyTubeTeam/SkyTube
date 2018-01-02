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

import android.view.View;

import java.util.List;

import free.rm.skytube.businessobjects.AsyncTaskParallel;
import free.rm.skytube.businessobjects.YouTube.GetYouTubeVideos;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;
import free.rm.skytube.businessobjects.db.SubscriptionsDb;
import free.rm.skytube.gui.businessobjects.LoadingProgressBar;
import free.rm.skytube.gui.businessobjects.adapters.VideoGridAdapter;

/**
 * An asynchronous task that will retrieve YouTube videos and displays them in the supplied Adapter.
 */
public class GetYouTubeVideosTask extends AsyncTaskParallel<Void, Void, List<YouTubeVideo>> {

	/** Object used to retrieve the desired YouTube videos. */
	private GetYouTubeVideos getYouTubeVideos;

	/** The Adapter where the retrieved videos will be displayed. */
	private VideoGridAdapter	videoGridAdapter;

	/** Class tag. */
	private static final String TAG = GetYouTubeVideosTask.class.getSimpleName();

	/** Optional non-static progressBar. If this isn't set, a static one will be used */
	private View progressBar = null;

	/** Whether or not to skip showing the progress bar. This is needed when doing swipe to refresh, since that functionality shows its own progress bar. */
	private boolean skipProgressBar = false;

	/** Runnable to be run when this task completes */
	private Runnable onFinished;

	private YouTubeChannel channel;


	public GetYouTubeVideosTask(GetYouTubeVideos getYouTubeVideos, VideoGridAdapter videoGridAdapter, View progressBar) {
		this.getYouTubeVideos = getYouTubeVideos;
		this.videoGridAdapter = videoGridAdapter;
		this.progressBar = progressBar;
	}

	/**
	 * Constructor to get youtube videos as part of a swipe to refresh. Since this functionality has its own progress bar, we'll
	 * skip showing our own.
	 *
	 * @param getYouTubeVideos The object that does the actual fetching of videos.
	 * @param videoGridAdapter The grid adapter the videos will be added to.
	 * @param onFinished
	 */
	public GetYouTubeVideosTask(GetYouTubeVideos getYouTubeVideos, VideoGridAdapter videoGridAdapter, Runnable onFinished) {
		this.getYouTubeVideos = getYouTubeVideos;
		this.videoGridAdapter = videoGridAdapter;
		this.skipProgressBar = true;
		this.onFinished = onFinished;
		this.getYouTubeVideos.reset();
		this.videoGridAdapter.clearList();
	}


	@Override
	protected void onPreExecute() {
		// if this task is being called by ChannelBrowserFragment, then get the channel the user is browsing
		channel = videoGridAdapter.getYouTubeChannel();

		if(!skipProgressBar) {
			if (progressBar != null)
				progressBar.setVisibility(View.VISIBLE);
			else
				LoadingProgressBar.get().show();
		}
	}

	@Override
	protected List<YouTubeVideo> doInBackground(Void... params) {
		List<YouTubeVideo> videosList = null;

		if (!isCancelled()) {
			// get videos from YouTube
			videosList = getYouTubeVideos.getNextVideos();

			if (videosList != null  &&  channel != null  &&  channel.isUserSubscribed()) {
				for (YouTubeVideo video : videosList) {
					channel.addYouTubeVideo(video);
				}

				SubscriptionsDb.getSubscriptionsDb().saveChannelVideos(channel);
			}
		}

		return videosList;
	}


	@Override
	protected void onPostExecute(List<YouTubeVideo> videosList) {
		videoGridAdapter.appendList(videosList);

		if(progressBar != null)
			progressBar.setVisibility(View.GONE);
		else
			LoadingProgressBar.get().hide();
		if(onFinished != null)
			onFinished.run();
	}


	@Override
	protected void onCancelled() {
		LoadingProgressBar.get().hide();
	}

}
