package free.rm.skytube.gui.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;

import butterknife.BindView;
import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.AsyncTaskParallel;
import free.rm.skytube.businessobjects.VideoCategory;
import free.rm.skytube.businessobjects.db.DownloadedVideosDb;
import free.rm.skytube.gui.businessobjects.OrderableVideoGridAdapter;
import free.rm.skytube.gui.businessobjects.fragments.OrderableVideosGridFragment;

public class DownloadedVideosFragment extends OrderableVideosGridFragment implements DownloadedVideosDb.DownloadedVideosListener {
	@BindView(R.id.noDownloadedVideosText)
	View noDownloadedVideosText;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		videoGridAdapter = new OrderableVideoGridAdapter(getActivity(), DownloadedVideosDb.getVideoDownloadsDb());
		setLayoutResource(R.layout.fragment_downloads);
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
		videoGridAdapter.refresh();
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
				videoGridAdapter.setVideoCategory(VideoCategory.DOWNLOADED_VIDEOS);
			}
		}

	}
}
