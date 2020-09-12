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

package free.rm.skytube.businessobjects.db.Tasks;

import android.view.Menu;

import androidx.annotation.NonNull;

import free.rm.skytube.R;
import free.rm.skytube.businessobjects.AsyncTaskParallel;
import free.rm.skytube.businessobjects.db.BookmarksDb;

/**
 * A task that checks if this video is bookmarked or not.  If it is bookmarked, then it will hide
 * the menu option to bookmark the video; otherwise it will hide the option to unbookmark the
 * video.
 */
public class IsVideoBookmarkedTask extends AsyncTaskParallel<Void, Void, Boolean> {
	private @NonNull Menu menu;
	private @NonNull String videoId;

	public IsVideoBookmarkedTask(@NonNull String videoId, @NonNull Menu menu) {
		this.videoId = videoId;
		this.menu = menu;
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		return BookmarksDb.getBookmarksDb().isBookmarked(videoId);
	}

	@Override
	protected void onPostExecute(Boolean videoIsBookmarked) {
		// if this video has been bookmarked, hide the bookmark option and show the unbookmark option.
		menu.findItem(R.id.bookmark_video).setVisible(!videoIsBookmarked);
		menu.findItem(R.id.unbookmark_video).setVisible(videoIsBookmarked);
	}
}
