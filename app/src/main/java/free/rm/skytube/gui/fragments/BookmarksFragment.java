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

package free.rm.skytube.gui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import butterknife.BindView;
import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.AsyncTaskParallel;
import free.rm.skytube.businessobjects.VideoCategory;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;
import free.rm.skytube.businessobjects.YouTube.newpipe.VideoId;
import free.rm.skytube.businessobjects.db.BookmarksDb;
import free.rm.skytube.gui.businessobjects.adapters.OrderableVideoGridAdapter;
import free.rm.skytube.gui.businessobjects.fragments.OrderableVideosGridFragment;

/**
 * Fragment that displays bookmarked videos.
 */
public class BookmarksFragment extends OrderableVideosGridFragment implements BookmarksDb.BookmarksDbListener {
	@BindView(R.id.noBookmarkedVideosText)
	View noBookmarkedVideosText;

	public BookmarksFragment() {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, @Nullable Bundle savedInstanceState) {
		setVideoGridAdapter(new OrderableVideoGridAdapter(BookmarksDb.getBookmarksDb()));
		View view = super.onCreateView(inflater, container, savedInstanceState);
		swipeRefreshLayout.setEnabled(false);
		BookmarksDb.getBookmarksDb().addListener(this);
		populateList();
		return view;
	}

	private void populateList() {
		new PopulateBookmarksTask().executeInParallel();
	}

	@Override
	public void onFragmentSelected() {
		super.onFragmentSelected();

		if (BookmarksDb.getBookmarksDb().isHasUpdated()) {
			populateList();
			BookmarksDb.getBookmarksDb().setHasUpdated(false);
		}
	}

	@Override
	public void onBookmarkAdded(YouTubeVideo video) {
		videoGridAdapter.prepend(video);
	}

	@Override
	public void onBookmarkDeleted(VideoId videoId) {
		videoGridAdapter.remove( card -> videoId.getId().equals(card.getId()));
	}

	@Override
	protected int getLayoutResource() {
		return R.layout.fragment_bookmarks;
	}
	

	@Override
	protected VideoCategory getVideoCategory() {
		return VideoCategory.BOOKMARKS_VIDEOS;
	}
	

	@Override
	public String getFragmentName() {
		return SkyTubeApp.getStr(R.string.bookmarks);
	}

	@Override
	public int getPriority() {
		return 3;
	}

	@Override
	public String getBundleKey() {
		return MainFragment.BOOKMARKS_FRAGMENT;
	}

	@Override
	public void onDestroyView() {
		BookmarksDb.getBookmarksDb().removeListener(this);
		super.onDestroyView();
	}

	////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * A task that:
	 *   1. gets the current total number of bookmarks
	 *   2. updated the UI accordingly (wrt step 1)
	 *   3. get the bookmarked videos asynchronously.
	 */
	private class PopulateBookmarksTask extends AsyncTaskParallel<Void, Void, Integer> {

		@Override
		protected Integer doInBackground(Void... params) {
			return BookmarksDb.getBookmarksDb().getTotalBookmarks();
		}

		@Override
		protected void onPostExecute(Integer numVideosBookmarked) {
			if (swipeRefreshLayout == null) {
				// fragment already disposed
				return;
			}
			// If no videos have been bookmarked, show the text notifying the user, otherwise
			// show the swipe refresh layout that contains the actual video grid.
			if (numVideosBookmarked <= 0) {
				swipeRefreshLayout.setVisibility(View.GONE);
				noBookmarkedVideosText.setVisibility(View.VISIBLE);
			} else {
				swipeRefreshLayout.setVisibility(View.VISIBLE);
				noBookmarkedVideosText.setVisibility(View.GONE);

				// set video category and get the bookmarked videos asynchronously
				videoGridAdapter.setVideoCategory(VideoCategory.BOOKMARKS_VIDEOS);
			}
		}
	}
}
