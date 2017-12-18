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
import free.rm.skytube.businessobjects.VideoCategory;
import free.rm.skytube.businessobjects.db.DownloadedVideosDb;
import free.rm.skytube.gui.businessobjects.DownloadedVideoGridAdapter;
import free.rm.skytube.gui.businessobjects.SimpleItemTouchHelperCallback;

public class DownloadedVideosFragment extends VideosGridFragment implements DownloadedVideosDb.DownloadedVideosListener {
	private DownloadedVideoGridAdapter downloadedVideoGridAdapter;

	@BindView(R.id.noDownloadedVideosText)
	View noDownloadedVideosText;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setLayoutResource(R.layout.videos_gridview_downloads);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);

		videoGridAdapter = null;

		if(downloadedVideoGridAdapter ==  null) {
			downloadedVideoGridAdapter = new DownloadedVideoGridAdapter(getActivity(), this);
		} else {
			downloadedVideoGridAdapter.setContext(getActivity());
		}

		gridView.setAdapter(downloadedVideoGridAdapter);

		ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(downloadedVideoGridAdapter);
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

	@Override
	public void onFragmentSelected() {
		super.onFragmentSelected();

		if (DownloadedVideosDb.getVideoDownloadsDb().isHasUpdated()) {
			populateList();
			DownloadedVideosDb.getVideoDownloadsDb().setHasUpdated(false);
		}
	}

	@Override
	protected VideoCategory getVideoCategory() {
		return VideoCategory.DOWNLOADED_VIDEOS;
	}

	@Override
	public String getFragmentName() {
		return SkyTubeApp.getStr(R.string.downloads);
	}

	@Override
	public void onDownloadedVideosUpdated() {
		populateList();
		downloadedVideoGridAdapter.refresh();
	}

	private void populateList() {
		new PopulateBookmarksTask().executeInParallel();
	}

	/**
	 * A task that:
	 *   1. gets the current total number of bookmarks
	 *   2. updated the UI accordingly (wrt step 1)
	 *   3. get the bookmarked videos asynchronously.
	 */
	private class PopulateBookmarksTask extends AsyncTaskParallel<Void, Void, Integer> {

		@Override
		protected Integer doInBackground(Void... params) {
			return DownloadedVideosDb.getVideoDownloadsDb().getNumDownloads();
		}


		@Override
		protected void onPostExecute(Integer numVideosBookmarked) {
			// If no videos have been bookmarked, show the text notifying the user, otherwise
			// show the swipe refresh layout that contains the actual video grid.
			if (numVideosBookmarked <= 0) {
				swipeRefreshLayout.setVisibility(View.GONE);
				noDownloadedVideosText.setVisibility(View.VISIBLE);
			} else {
				swipeRefreshLayout.setVisibility(View.VISIBLE);
				noDownloadedVideosText.setVisibility(View.GONE);

				// set video category and get the bookmarked videos asynchronously
				downloadedVideoGridAdapter.setVideoCategory(VideoCategory.DOWNLOADED_VIDEOS);
			}
		}

	}

	@Override
	public void onRefresh() {
		downloadedVideoGridAdapter.refresh(new Runnable() {
			@Override
			public void run() {
				swipeRefreshLayout.setRefreshing(false);
			}
		});
	}
}
