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

import butterknife.Bind;
import free.rm.skytube.R;
import free.rm.skytube.businessobjects.MainActivityListener;
import free.rm.skytube.businessobjects.db.BookmarksDb;
import free.rm.skytube.gui.businessobjects.SavedVideoGridAdapter;
import free.rm.skytube.gui.businessobjects.SimpleItemTouchHelperCallback;

/**
 * Fragment that displays bookmarked videos.
 */
public class BookmarksFragment extends VideosGridFragment implements BookmarksDb.SavedVideosDbListener {
	@Bind(R.id.noSavedVideosText)
	View noBookmarkedVideosText;

	private SavedVideoGridAdapter savedVideoGridAdapter;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setLayoutResource(R.layout.videos_gridview_bookmarks);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);
		if(savedVideoGridAdapter == null)
			savedVideoGridAdapter = new SavedVideoGridAdapter(getActivity());
		gridView.setAdapter(savedVideoGridAdapter);

		savedVideoGridAdapter.clearList();

		savedVideoGridAdapter.setListener((MainActivityListener)getActivity());

		ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(savedVideoGridAdapter);
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
		int numVideosSaved = BookmarksDb.getBookmarksDb().getNumVideos();
		// If no videos have been saved, show the text notifying the user, otherwise
		// show the swipe refresh layout that contains the actual video grid.
		if (numVideosSaved == 0) {
			swipeRefreshLayout.setVisibility(View.GONE);
			noBookmarkedVideosText.setVisibility(View.VISIBLE);
		} else {
			swipeRefreshLayout.setVisibility(View.VISIBLE);
			noBookmarkedVideosText.setVisibility(View.GONE);

			savedVideoGridAdapter.updateList(BookmarksDb.getBookmarksDb().getSavedVideos());
		}
	}

	/**
	 * When
	 */
	public void onSelected() {
		if(BookmarksDb.getBookmarksDb().isHasUpdated()) {
			populateList();
			BookmarksDb.getBookmarksDb().setHasUpdated(false);
		}
	}

	@Override
	public void onUpdated() {
		populateList();
	}

}
