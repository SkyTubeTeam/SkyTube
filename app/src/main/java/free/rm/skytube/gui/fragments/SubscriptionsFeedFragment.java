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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.afollestad.materialdialogs.MaterialDialog;


import free.rm.skytube.R;
import free.rm.skytube.app.EventBus;
import free.rm.skytube.app.FeedUpdateTask;
import free.rm.skytube.app.Settings;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.FeedUpdaterService;
import free.rm.skytube.businessobjects.VideoCategory;
import free.rm.skytube.businessobjects.YouTube.Tasks.GetSubscriptionVideosTaskListener;
import free.rm.skytube.databinding.FragmentSubsFeedBinding;
import free.rm.skytube.gui.businessobjects.SubscriptionsBackupsManager;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

/**
 * Fragment that displays subscriptions videos feed from all channels the user is subscribed to.
 */
public class SubscriptionsFeedFragment extends VideosGridFragment implements GetSubscriptionVideosTaskListener {
	private SubscriptionsBackupsManager subscriptionsBackupsManager;

	/**
	 * BroadcastReceiver that will receive a message that new subscription videos have been found by the
	 * {@link FeedUpdaterService}. The video grid will be updated when this happens.
	 */
	private BroadcastReceiver feedUpdaterReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			refreshFeedFromCache();
		}
	};

	private FragmentSubsFeedBinding binding;

	private final CompositeDisposable compositeDisposable = new CompositeDisposable();
	private MaterialDialog fetchingChannelInfoDialog;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		subscriptionsBackupsManager = new SubscriptionsBackupsManager(getActivity(), SubscriptionsFeedFragment.this);

		EventBus.getInstance().registerSubscriptionListener(this);
	}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        init(container.getContext(), FragmentSubsFeedBinding.inflate(inflater, container, false));
        return binding.getRoot();
    }

    private void init(Context context, FragmentSubsFeedBinding bindingParam) {
        this.binding = bindingParam;
        initVideos(context, videoGridAdapter, binding.videosGridview);
        binding.importSubscriptionsButton.setOnClickListener(v -> subscriptionsBackupsManager
                .displayImportSubscriptionsFromYouTubeDialog());
        binding.importBackupButton.setOnClickListener(v -> subscriptionsBackupsManager.displayFilePicker());
        videoGridAdapter.setVideoGridUpdated(this::setupUiAccordingToNumOfSubbedChannels);
        // get the previously published videos currently cached in the database
        videoGridAdapter.setVideoCategory(VideoCategory.SUBSCRIPTIONS_FEED_VIDEOS);
		binding.videosGridview.swipeRefreshLayout.setOnRefreshListener(this);
    }

	@Override
	public void onResume() {
		requireActivity().registerReceiver(feedUpdaterReceiver, new IntentFilter(FeedUpdaterService.NEW_SUBSCRIPTION_VIDEOS_FOUND));

		super.onResume();

		// setup the UI and refresh the feed (if applicable)
		Settings settings = SkyTubeApp.getSettings();
		startRefreshTask(isFragmentSelected(), settings.isFullRefreshTimely() || settings.isRefreshSubsFeedFull());

		// this will detect whether we have previous instructed the app (via refreshSubsFeedFromCache())
		// to refresh the subs feed
		if (settings.isRefreshSubsFeedFromCache()) {
			// unset the flag
			settings.setRefreshSubsFeedFromCache(false);

			// refresh the subs feed by reading from the cache (i.e. local DB)
			refreshFeedFromCache();
		}
	}

	@Override
	public synchronized void onPause() {
		hideFetchingVideosDialog();
		super.onPause();
		requireActivity().unregisterReceiver(feedUpdaterReceiver);
	}

	@Override
	public void onDestroy() {
		subscriptionsBackupsManager.clearBackgroundTasks();
		compositeDisposable.clear();
		subscriptionsBackupsManager = null;
		EventBus.getInstance().unregisterSubscriptionListener(this);
		super.onDestroy();
	}

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }

	@Override
	public void onRefresh() {
		startRefreshTask(false, true);
	}


	private synchronized void startRefreshTask(boolean isShowFetchingVideosDialog, boolean forcedFullRefresh) {
		FeedUpdateTask fut = FeedUpdateTask.getInstance();
		if (fut.isRefreshInProgress()) {
			if (isShowFetchingVideosDialog) {
				showFetchingVideosDialog();
			}
			return;
		}
		if (forcedFullRefresh && SkyTubeApp.isConnected(requireContext())) {
			if (isShowFetchingVideosDialog) {
				showFetchingVideosDialog();
			}

			fut.start(requireContext());

		} else {
			videoGridAdapter.refresh(true);
		}
	}

	@Override
	public void onSubscriptionRefreshStarted() {
		if (gridviewBinding.swipeRefreshLayout != null) {
			gridviewBinding.swipeRefreshLayout.setRefreshing(true);
		}
	}

	@Override
	public void onChannelsFound(boolean hasChannels) {
		setupUiAccordingToNumOfSubbedChannels(hasChannels);
	}

	@Override
	public void onSubscriptionRefreshFinished() {
		// Remove the progress bar(s)
		if (gridviewBinding.swipeRefreshLayout != null) {
			gridviewBinding.swipeRefreshLayout.setRefreshing(false);
		}
		hideFetchingVideosDialog();
	}

	@Override
	public void onChannelVideoFetchFinish(boolean changes) {
		// refresh the subs feed by reading from the cache (i.e. local DB)
		if (changes) {
			refreshFeedFromCache();
		}
	}

	@Override
	protected VideoCategory getVideoCategory() {
		return VideoCategory.SUBSCRIPTIONS_FEED_VIDEOS;
	}

	@Override
	public String getFragmentName() {
		return SkyTubeApp.getStr(R.string.feed);
	}

	@Override
	public int getPriority() {
		return 2;
	}

	@Override
	public String getBundleKey() {
		return MainFragment.SUBSCRIPTIONS_FEED_FRAGMENT;
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		subscriptionsBackupsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
	}

	/**
	 * Set up the UI depending to the total number of channel the user is subscribed to.
	 *
	 * @param hasChannels   If the user has already subscribed to at least one channel.
	 */
	private void setupUiAccordingToNumOfSubbedChannels(boolean hasChannels) {
		final SwipeRefreshLayout swipeRefreshLayout = gridviewBinding.swipeRefreshLayout;
		if (hasChannels) {
			if (swipeRefreshLayout.getVisibility() != View.VISIBLE) {
				swipeRefreshLayout.setVisibility(View.VISIBLE);
				binding.noSubscriptionsText.setVisibility(View.GONE);
			}
		} else {
			swipeRefreshLayout.setVisibility(View.GONE);
			binding.noSubscriptionsText.setVisibility(View.VISIBLE);
		}
	}

	private synchronized void hideFetchingVideosDialog() {
		if (fetchingChannelInfoDialog != null) {
			fetchingChannelInfoDialog.dismiss();
			fetchingChannelInfoDialog = null;
		}
	}

	private synchronized void showFetchingVideosDialog() {
		hideFetchingVideosDialog();
		fetchingChannelInfoDialog = new MaterialDialog.Builder(getActivity())
				.content(R.string.fetching_subbed_channels_info)
				.progress(true, 0)
				.build();
		fetchingChannelInfoDialog.show();
	}

	public void refreshFeedFromCache() {
		if (videoGridAdapter != null) {
			videoGridAdapter.refresh(true);
		}
	}
}
