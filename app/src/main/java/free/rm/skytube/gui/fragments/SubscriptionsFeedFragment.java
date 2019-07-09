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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import org.joda.time.DateTime;

import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.AsyncTaskParallel;
import free.rm.skytube.businessobjects.FeedUpdaterService;
import free.rm.skytube.businessobjects.VideoCategory;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;
import free.rm.skytube.businessobjects.YouTube.Tasks.GetSubscriptionVideosTask;
import free.rm.skytube.businessobjects.YouTube.Tasks.GetSubscriptionVideosTaskListener;
import free.rm.skytube.businessobjects.db.SubscriptionsDb;
import free.rm.skytube.gui.businessobjects.SubscriptionsBackupsManager;
import free.rm.skytube.gui.businessobjects.adapters.SubsAdapter;

/**
 * Fragment that displays subscriptions videos feed from all channels the user is subscribed to.
 */
public class SubscriptionsFeedFragment extends VideosGridFragment implements GetSubscriptionVideosTaskListener {
	private int numVideosFetched = 0;
	private int numChannelsFetched = 0;
	private int numChannelsSubscribed = 0;
	private boolean refreshInProgress = false;
	private MaterialDialog progressDialog;
	/** When set to true, it will retrieve any published video (wrt subbed channels) by querying the
	 *  remote YouTube servers. */
	private boolean shouldRefresh = false;
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

	@BindView(R.id.noSubscriptionsText)
	View noSubscriptionsText;

	public static final String FLAG_REFRESH_FEED_FROM_CACHE = "SubscriptionsFeedFragment.FLAG_REFRESH_FEED_FROM_CACHE";
	public static final String FLAG_REFRESH_FEED_FULL = "SubscriptionsFeedFragment.FLAG_REFRESH_FEED_FULL";
	/** Refresh the feed (by querying the YT servers) after 3 hours since the last check. */
	private static final int    REFRESH_TIME = 3;


	@Override
	protected int getLayoutResource() {
		return R.layout.fragment_subs_feed;
	}


	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Only do an automatic refresh of subscriptions if it's been more than three hours since the last one was done.
		long l = SkyTubeApp.getPreferenceManager().getLong(SkyTubeApp.KEY_SUBSCRIPTIONS_LAST_UPDATED, -1);
		DateTime subscriptionsLastUpdated = new DateTime(l);
		DateTime threeHoursAgo = new DateTime().minusHours(REFRESH_TIME);
		if(subscriptionsLastUpdated.isBefore(threeHoursAgo)) {
			shouldRefresh = true;
		}

		subscriptionsBackupsManager = new SubscriptionsBackupsManager(getActivity(), SubscriptionsFeedFragment.this);
	}


	@Override
	public void onResume() {
		// setup the UI and refresh the feed (if applicable)
		new RefreshFeedTask(isFragmentSelected()).executeInParallel();

		getActivity().registerReceiver(feedUpdaterReceiver, new IntentFilter(FeedUpdaterService.NEW_SUBSCRIPTION_VIDEOS_FOUND));

		super.onResume();
		// this will detect whether we have previous instructed the app (via refreshSubsFeedFromCache())
		// to refresh the subs feed
		if (isFlagSet(FLAG_REFRESH_FEED_FROM_CACHE)) {
			// unset the flag
			unsetFlag(FLAG_REFRESH_FEED_FROM_CACHE);

			// refresh the subs feed by reading from the cache (i.e. local DB)
			refreshFeedFromCache();
		} else if (isFlagSet(FLAG_REFRESH_FEED_FULL)) {
			// unset the flag
			unsetFlag(FLAG_REFRESH_FEED_FULL);

			// refresh the subs feed by retrieving videos from the YT servers
			onRefresh();
		}
	}


	@Override
	public void onPause() {
		super.onPause();
		getActivity().unregisterReceiver(feedUpdaterReceiver);
	}


	/**
	 * Instruct the {@link SubscriptionsFeedFragment} to refresh the subscriptions feed.
	 *
	 * This might occur due to user subscribing/unsubscribing to a channel.
	 */
	public static void refreshSubsFeedFromCache() {
		setFlag(FLAG_REFRESH_FEED_FROM_CACHE);
	}


	/**
	 * Instruct the {@link SubscriptionsFeedFragment} to refresh the subscriptions feed by retrieving
	 * any newly published videos from the YT servers.
	 *
	 * This might occur due to user just imported the subbed channels from YouTube (XML file).
	 */
	public static void refreshSubsFeedFull() {
		setFlag(FLAG_REFRESH_FEED_FULL);
	}


	public static void setFlag(String flag) {
		SharedPreferences.Editor editor = SkyTubeApp.getPreferenceManager().edit();
		editor.putBoolean(flag, true);
		editor.commit();
	}


	public static void unsetFlag(String flag) {
		SharedPreferences.Editor editor = SkyTubeApp.getPreferenceManager().edit();
		editor.putBoolean(flag, false);
		editor.commit();
	}

	public static boolean isFlagSet(String flag) {
		return SkyTubeApp.getPreferenceManager().getBoolean(flag, false);
	}


	private void showRefreshDialog() {
		progressDialog = new MaterialDialog.Builder(getActivity())
						.title(R.string.fetching_subscription_videos)
						.content(String.format(getContext().getString(R.string.fetched_videos_from_channels), numVideosFetched, numChannelsFetched, numChannelsSubscribed))
						.progress(true, 0)
						.backgroundColorRes(R.color.colorPrimary)
						.titleColorRes(android.R.color.white)
						.contentColorRes(android.R.color.white)
						.build();
		progressDialog.show();
	}


	@Override
	public void onRefresh() {
		shouldRefresh = true;
		new RefreshFeedTask(true).executeInParallel();
	}


	@Override
	public void onChannelVideosFetched(YouTubeChannel channel, List<YouTubeVideo> videosFetched, final boolean videosDeleted) {
		Log.d("SUB FRAGMENT", "onChannelVideosFetched");

		// If any new videos have been fetched for a channel, update the Subscription list in the left navbar for that channel
		if(videosFetched.size() > 0)
			SubsAdapter.get(getActivity()).changeChannelNewVideosStatus(channel.getId(), true);

		numVideosFetched += videosFetched.size();
		numChannelsFetched++;
		if(progressDialog != null)
			progressDialog.setContent(String.format(SkyTubeApp.getStr(R.string.fetched_videos_from_channels), numVideosFetched, numChannelsFetched, numChannelsSubscribed));
		if(numChannelsFetched == numChannelsSubscribed) {
			new Handler().postDelayed(new Runnable() {
				@Override
				public void run() {
					refreshInProgress = false;
					// Remove the progress bar(s)
					swipeRefreshLayout.setRefreshing(false);
					boolean fragmentIsVisible = progressDialog != null;
					if(progressDialog != null)
						progressDialog.dismiss();
					if(numVideosFetched > 0 || videosDeleted) {
						refreshFeedFromCache();
					} else {
						// Only show the toast that no videos were found if the progress dialog is sh
						if(fragmentIsVisible) {
							Toast.makeText(getContext(),
											R.string.no_new_videos_found,
											Toast.LENGTH_LONG).show();
						}
					}
				}
			}, 500);
		}
	}

	@Override
	public void onAllChannelVideosFetched() {
	}

	@Override
	public void onFragmentSelected() {
		super.onFragmentSelected();

		// when the Subscriptions tab is selected, if a refresh is in progress, show the dialog.
		if (refreshInProgress)
			showRefreshDialog();
	}


	@Override
	protected VideoCategory getVideoCategory() {
		return VideoCategory.SUBSCRIPTIONS_FEED_VIDEOS;
	}


	@Override
	public String getFragmentName() {
		return SkyTubeApp.getStr(R.string.feed);
	}


	@OnClick(R.id.importSubscriptionsButton)
	public void importSubscriptions(View v) {
		subscriptionsBackupsManager.displayImportSubscriptionsFromYouTubeDialog();
	}


	@OnClick(R.id.importBackupButton)
	public void importBackup(View v) {
		subscriptionsBackupsManager.displayFilePicker();
	}


	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		subscriptionsBackupsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
	}


	/**
	 * Set up the UI depending to the total number of channel the user is subscribed to.
	 *
	 * @param totalSubbedChannels   Total number of channel the user is subscribed to.
	 */
	private void setupUiAccordingToNumOfSubbedChannels(int totalSubbedChannels) {
		if (totalSubbedChannels <= 0) {
			swipeRefreshLayout.setVisibility(View.GONE);
			noSubscriptionsText.setVisibility(View.VISIBLE);
		} else {
			if (swipeRefreshLayout.getVisibility() != View.VISIBLE) {
				swipeRefreshLayout.setVisibility(View.VISIBLE);
				noSubscriptionsText.setVisibility(View.GONE);
			}
		}
	}



	////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * A task that will retrieve videos published to the user's subscribed channels.
	 *
	 * These videos can either:
	 * 1.  Cached inside the local database;
	 * 2.  No in the DB and hence we need to retrieve them from the YouTube servers.
	 */
	private class RefreshFeedTask extends AsyncTaskParallel<Void, Void, Integer> {

		private MaterialDialog  fetchingChannelInfoDialog;
		private boolean         showDialogs;


		private RefreshFeedTask(boolean showFetchingVideosDialog) {
			this.showDialogs = showFetchingVideosDialog;
		}


		@Override
		protected void onPreExecute() {
			// display the "Fetching channels information …" dialog
			if (showDialogs) {
				fetchingChannelInfoDialog = new MaterialDialog.Builder(getActivity())
						.content(R.string.fetching_subbed_channels_info)
						.progress(true, 0)
						.build();
				fetchingChannelInfoDialog.show();
			}
		}


		@Override
		protected Integer doInBackground(Void... params) {
			// get the total number of channels the user is subscribed to (from the subs adapter)
			return SubsAdapter.get(SubscriptionsFeedFragment.this.getActivity()).getSubsLists().size();
		}


		@Override
		protected void onPostExecute(Integer totalNumberOfChannels) {
			numVideosFetched      = 0;
			numChannelsFetched    = 0;
			numChannelsSubscribed = totalNumberOfChannels;

			// hide the "Fetching channels information …" dialog
			if (showDialogs) {
				fetchingChannelInfoDialog.dismiss();
			}

			// setup the user interface
			setupUiAccordingToNumOfSubbedChannels(totalNumberOfChannels);

			if (numChannelsSubscribed > 0) {
				// get the previously published videos currently cached in the database
				videoGridAdapter.setVideoCategory(VideoCategory.SUBSCRIPTIONS_FEED_VIDEOS);

				// get any videos published after the last time the user used the app...
				if (shouldRefresh) {
					new GetSubscriptionVideosTask(SubscriptionsFeedFragment.this).executeInParallel();      // refer to #onChannelVideosFetched()
					refreshInProgress = true;
					shouldRefresh = false;

					if (showDialogs)
						showRefreshDialog();
				}
			}
		}

	}

	public void refreshFeedFromCache() {
		new RefreshFeedFromCacheTask().executeInParallel();
	}


	/**
	 * A task that refreshes the subscriptions feed by getting published videos currently cached in
	 * the local database.
	 */
	private class RefreshFeedFromCacheTask extends AsyncTaskParallel<Void, Void, List<YouTubeVideo>> {

		@Override
		protected List<YouTubeVideo> doInBackground(Void... params) {
			return SubscriptionsDb.getSubscriptionsDb().getSubscriptionVideos();
		}

		@Override
		protected void onPostExecute(List<YouTubeVideo> youTubeVideos) {
			videoGridAdapter.setList(youTubeVideos);
			setupUiAccordingToNumOfSubbedChannels(youTubeVideos.size());
		}

	}

}
