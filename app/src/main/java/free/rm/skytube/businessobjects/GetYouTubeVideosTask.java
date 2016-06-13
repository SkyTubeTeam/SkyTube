/*
 * SkyTube
 * Copyright (C) 2015  Ramon Mifsud
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

import java.util.List;

import free.rm.skytube.gui.businessobjects.LoadingProgressBar;
import free.rm.skytube.gui.businessobjects.VideoGridAdapter;

/**
 * An asynchronous task that will retrieve YouTube videos and displays them in the supplied Adapter.
 */
public class GetYouTubeVideosTask extends AsyncTaskParallel<Void, Void, List<YouTubeVideo>> {

	/** Object used to retrieve the desired YouTube videos. */
	private GetYouTubeVideos	getYouTubeVideos;

	/** The Adapter where the retrieved videos will be displayed. */
	private VideoGridAdapter	videoGridAdapter;

	/** Class tag. */
	private static final String TAG = GetYouTubeVideosTask.class.getSimpleName();


	public GetYouTubeVideosTask(GetYouTubeVideos getYouTubeVideos, VideoGridAdapter videoGridAdapter) {
		this.getYouTubeVideos = getYouTubeVideos;
		this.videoGridAdapter = videoGridAdapter;
	}


	@Override
	protected void onPreExecute() {
		LoadingProgressBar.get().show();
	}

	@Override
	protected List<YouTubeVideo> doInBackground(Void... params) {
		List<YouTubeVideo> videos = null;

		if (!isCancelled()) {
			videos = getYouTubeVideos.getNextVideos();
		}

		return videos;
	}


	@Override
	protected void onPostExecute(List<YouTubeVideo> videosList) {
		videoGridAdapter.appendList(videosList);
		LoadingProgressBar.get().hide();
	}


	@Override
	protected void onCancelled() {
		LoadingProgressBar.get().hide();
	}

}
