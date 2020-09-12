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
import free.rm.skytube.businessobjects.db.DownloadedVideosDb;
import free.rm.skytube.gui.businessobjects.adapters.OrderableVideoGridAdapter;
import free.rm.skytube.gui.businessobjects.fragments.OrderableVideosGridFragment;

/**
 * A fragment that holds videos downloaded by the user.
 */
public class DownloadedVideosFragment extends OrderableVideosGridFragment implements DownloadedVideosDb.DownloadedVideosListener {
	@BindView(R.id.noDownloadedVideosText)
	View noDownloadedVideosText;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, @Nullable Bundle savedInstanceState) {
		setVideoGridAdapter(new OrderableVideoGridAdapter(DownloadedVideosDb.getVideoDownloadsDb()));
		View view = super.onCreateView(inflater, container, savedInstanceState);
		swipeRefreshLayout.setEnabled(false);
		DownloadedVideosDb.getVideoDownloadsDb().addListener(this);
		populateList();
		return view;
	}

	@Override
	public void onDestroyView() {
		DownloadedVideosDb.getVideoDownloadsDb().removeListener(this);
		super.onDestroyView();
	}

	@Override
	protected int getLayoutResource() {
		return R.layout.fragment_downloads;
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
	public int getPriority() {
		return 4;
	}

	@Override
	public String getBundleKey() {
		return MainFragment.DOWNLOADED_VIDEOS_FRAGMENT;
	}

	@Override
	public void onDownloadedVideosUpdated() {
		populateList();
		videoGridAdapter.refresh(true);
	}

	private void populateList() {
		new PopulateDownloadsTask().executeInParallel();
	}


	/**
	 * A task that:
	 *   1. gets the current total number of downloads
	 *   2. updated the UI accordingly (wrt step 1)
	 *   3. get the downloaded videos asynchronously.
	 */
	private class PopulateDownloadsTask extends AsyncTaskParallel<Void, Void, Integer> {

		@Override
		protected Integer doInBackground(Void... params) {
			return DownloadedVideosDb.getVideoDownloadsDb().getMaximumOrderNumber();
		}


		@Override
		protected void onPostExecute(Integer maximumOrderNumber) {
			if (swipeRefreshLayout == null) {
				// fragment already disposed
				return;
			}
			// If no videos have been downloaded, show the text notifying the user, otherwise
			// show the swipe refresh layout that contains the actual video grid.
			if (maximumOrderNumber <= 0) {
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
