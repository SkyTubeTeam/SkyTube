package free.rm.skytube.gui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import butterknife.BindView;
import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.VideoCategory;
import free.rm.skytube.businessobjects.YouTube.POJOs.CardData;
import free.rm.skytube.businessobjects.YouTube.newpipe.ContentId;
import free.rm.skytube.businessobjects.db.DownloadedVideosDb;
import free.rm.skytube.businessobjects.interfaces.CardListener;
import free.rm.skytube.gui.businessobjects.adapters.OrderableVideoGridAdapter;
import free.rm.skytube.gui.businessobjects.fragments.OrderableVideosGridFragment;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;

/**
 * A fragment that holds videos downloaded by the user.
 */
public class DownloadedVideosFragment extends OrderableVideosGridFragment implements CardListener {
	@BindView(R.id.noDownloadedVideosText)
	View noDownloadedVideosText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, @Nullable Bundle savedInstanceState) {
        setVideoGridAdapter(new OrderableVideoGridAdapter(DownloadedVideosDb.getVideoDownloadsDb()));
        DownloadedVideosDb.getVideoDownloadsDb().registerListener(this);
        View view = super.onCreateView(inflater, container, savedInstanceState);
        setListVisible(false);
        swipeRefreshLayout.setEnabled(false);

        populateList();
        return view;
    }

    @Override
    public void onDestroyView() {
        DownloadedVideosDb.getVideoDownloadsDb().unregisterListener(this);
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
    public void onCardAdded(final CardData card) {
        videoGridAdapter.onCardAdded(card);
        setListVisible(true);
    }

    @Override
    public void onCardDeleted(final ContentId card) {
        videoGridAdapter.onCardDeleted(card);
        if (videoGridAdapter.getItemCount() == 0) {
            setListVisible(false);
        }
    }

    private void populateList() {
        DownloadedVideosDb.getVideoDownloadsDb().getTotalCount()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess(numberOfBookmarks -> {
                    if (numberOfBookmarks > 0 && swipeRefreshLayout != null) {
                        setListVisible(true);
                        // swipeRefreshLayout.setRefreshing(true);
                    }
                }).subscribe();
    }


    private void setListVisible(boolean visible) {
        if (visible) {
            swipeRefreshLayout.setVisibility(View.VISIBLE);
            noDownloadedVideosText.setVisibility(View.GONE);
        } else {
            swipeRefreshLayout.setVisibility(View.GONE);
            noDownloadedVideosText.setVisibility(View.VISIBLE);
        }
    }

}
