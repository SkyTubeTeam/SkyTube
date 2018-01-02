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

package free.rm.skytube.gui.businessobjects;

import android.view.View;

import free.rm.skytube.businessobjects.YouTube.Tasks.GetYouTubeVideosTask;

/**
 * Loading progress bar that will be displayed by {@link GetYouTubeVideosTask}
 * when data is being retrieved from YouTube servers.
 */
public class LoadingProgressBar {

	private View progressBar = null;
	private static volatile LoadingProgressBar	loadingProgressBar = null;

	public synchronized static LoadingProgressBar get() {
		if (loadingProgressBar == null)
			loadingProgressBar = new LoadingProgressBar();

		return loadingProgressBar;
	}


	public synchronized void setProgressBar(View progressBar) {
		// hide the old progress bar (if any)
		hide();

		// set the new progress bar
		this.progressBar = progressBar;
	}


	public synchronized void show() {
		if (progressBar != null)
			progressBar.setVisibility(View.VISIBLE);
	}


	public synchronized void hide() {
		if (progressBar != null)
			progressBar.setVisibility(View.GONE);
	}

}
