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
import android.support.annotation.Nullable;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import butterknife.BindView;
import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.AsyncTaskParallel;
import free.rm.skytube.gui.businessobjects.MainActivityListener;
import free.rm.skytube.businessobjects.VideoCategory;
import free.rm.skytube.businessobjects.db.BookmarksDb;
import free.rm.skytube.gui.businessobjects.SimpleItemTouchHelperCallback;
import free.rm.skytube.gui.businessobjects.bookmarksGridAdapter;

/**
 * Fragment that displays bookmarked videos.
 */
public class BookmarksFragment extends VideosGridFragment implements BookmarksDb.BookmarksDbListener {
	@BindView(R.id.noBookmarkedVideosText)
	View noBookmarkedVideosText;

	private bookmarksGridAdapter bookmarksGridAdapter;


	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setLayoutResource(R.layout.videos_gridview_bookmarks);
	}


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);
		videoGridAdapter = null;

		if (bookmarksGridAdapter == null) {
			bookmarksGridAdapter = new bookmarksGridAdapter(getActivity());
		} else {
			bookmarksGridAdapter.setContext(getActivity());
		}

		bookmarksGridAdapter.setListener((MainActivityListener)getActivity());
		gridView.setAdapter(bookmarksGridAdapter);

		ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(bookmarksGridAdapter);
		ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
		touchHelper.attachToRecyclerView(gridView);

		return view;
	}


	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		swipeRefreshLayout.setEnabled(false);
		populateList();
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
	public void onBookmarksDbUpdated() {
		populateList();
		bookmarksGridAdapter.refresh();
	}

	@Override
	protected VideoCategory getVideoCategory() {
		return VideoCategory.BOOKMARKS_VIDEOS;
	}

	@Override
	protected String getFragmentName() {
		return SkyTubeApp.getStr(R.string.bookmarks);
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
			// If no videos have been bookmarked, show the text notifying the user, otherwise
			// show the swipe refresh layout that contains the actual video grid.
			if (numVideosBookmarked <= 0) {
				swipeRefreshLayout.setVisibility(View.GONE);
				noBookmarkedVideosText.setVisibility(View.VISIBLE);
			} else {
				swipeRefreshLayout.setVisibility(View.VISIBLE);
				noBookmarkedVideosText.setVisibility(View.GONE);

				// set video category and get the bookmarked videos asynchronously
				bookmarksGridAdapter.setVideoCategory(VideoCategory.BOOKMARKS_VIDEOS);
			}
		}

	}

}
