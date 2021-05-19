package free.rm.skytube.gui.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.VideoCategory;
import free.rm.skytube.businessobjects.YouTube.POJOs.CardData;
import free.rm.skytube.businessobjects.YouTube.newpipe.ContentId;
import free.rm.skytube.businessobjects.db.DownloadedVideosDb;
import free.rm.skytube.businessobjects.interfaces.CardListener;
import free.rm.skytube.databinding.FragmentDownloadsBinding;
import free.rm.skytube.gui.businessobjects.adapters.OrderableVideoGridAdapter;
import free.rm.skytube.gui.businessobjects.fragments.OrderableVideosGridFragment;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;

/**
 * A fragment that holds videos downloaded by the user.
 */
public class DownloadedVideosFragment extends OrderableVideosGridFragment implements CardListener {
    private FragmentDownloadsBinding binding;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, @Nullable Bundle savedInstanceState) {
        initDownloads(container.getContext(), new OrderableVideoGridAdapter(DownloadedVideosDb.getVideoDownloadsDb()), FragmentDownloadsBinding.inflate(inflater, container, false));
        return binding.getRoot();
    }

    private void initDownloads(final Context context, final OrderableVideoGridAdapter videoGridAdapterParam, final FragmentDownloadsBinding bindingParam) {
        this.binding = bindingParam;
        initOrderableVideos(context, videoGridAdapterParam, bindingParam.videosGridview);
        DownloadedVideosDb.getVideoDownloadsDb().registerListener(this);
        setListVisible(false);

        populateList();
    }

    @Override
    public void onDestroyView() {
        DownloadedVideosDb.getVideoDownloadsDb().unregisterListener(this);
        binding = null;
        super.onDestroyView();
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
            binding.noDownloadedVideosText.setVisibility(View.GONE);
        } else {
            swipeRefreshLayout.setVisibility(View.GONE);
            binding.noDownloadedVideosText.setVisibility(View.VISIBLE);
        }
    }

}
