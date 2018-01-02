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
import free.rm.skytube.businessobjects.YouTube.Tasks.GetSubscriptionVideosTask;
import free.rm.skytube.businessobjects.VideoCategory;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;
import free.rm.skytube.businessobjects.db.SubscriptionsDb;
import free.rm.skytube.businessobjects.YouTube.Tasks.GetSubscriptionVideosTaskListener;
import free.rm.skytube.gui.businessobjects.adapters.SubsAdapter;
import free.rm.skytube.gui.businessobjects.SubscriptionsBackupsManager;

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
			new RefreshFeedFromCacheTask().executeInParallel();
		}
	};

	@BindView(R.id.noSubscriptionsText)
	View noSubscriptionsText;

	private static final String KEY_SET_UPDATE_FEED_TAB = "SubscriptionsFeedFragment.KEY_SET_UPDATE_FEED_TAB";
	/** Refresh the feed (by querying the YT servers) after 3 hours since the last check. */
	private static final int    REFRESH_TIME = 3;


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

		setLayoutResource(R.layout.fragment_subs_feed);
		subscriptionsBackupsManager = new SubscriptionsBackupsManager(getActivity(), SubscriptionsFeedFragment.this);
	}


	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		new RefreshFeedTask(isFragmentSelected()).executeInParallel();
	}


	@Override
	public void onResume() {
		getActivity().registerReceiver(feedUpdaterReceiver, new IntentFilter(FeedUpdaterService.NEW_SUBSCRIPTION_VIDEOS_FOUND));

		super.onResume();
		// this will detect whether we have previous instructed the app (via refreshSubscriptionsFeed())
		// to refresh the subs feed
		if (SkyTubeApp.getPreferenceManager().getBoolean(KEY_SET_UPDATE_FEED_TAB, false)) {
			// unset the flag
			SharedPreferences.Editor editor = SkyTubeApp.getPreferenceManager().edit();
			editor.putBoolean(KEY_SET_UPDATE_FEED_TAB, false);
			editor.commit();

			// refresh the subs feed
			new RefreshFeedFromCacheTask().executeInParallel();
		}
	}


	@Override
	public void onPause() {
		super.onPause();
		getActivity().unregisterReceiver(feedUpdaterReceiver);
	}


	/**
	 * Instruct the {@link SubscriptionsFeedFragment} to refresh the subscriptions feed.  This might
	 * occur due to user subscribing/unsubscribing to a channel or a user just imported the subbed
	 * channels from YouTube (XML file).
	 */
	public static void refreshSubscriptionsFeed() {
		SharedPreferences.Editor editor = SkyTubeApp.getPreferenceManager().edit();
		editor.putBoolean(KEY_SET_UPDATE_FEED_TAB, true);
		editor.commit();
	}


	private void doRefresh(boolean showDialog) {
		new RefreshFeedTask(showDialog).executeInParallel();
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
		doRefresh(true);
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
						new RefreshFeedFromCacheTask().executeInParallel();
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
		subscriptionsBackupsManager.displayImportSubscriptionsDialog();
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

		private boolean showDialog;


		private RefreshFeedTask(boolean showDialog) {
			this.showDialog = showDialog;
		}


		@Override
		protected Integer doInBackground(Void... params) {
			// get the total number of channels the user is subscribed to
			return SubscriptionsDb.getSubscriptionsDb().getTotalSubscribedChannels();
		}


		@Override
		protected void onPostExecute(Integer totalNumberOfChannels) {
			numVideosFetched      = 0;
			numChannelsFetched    = 0;
			numChannelsSubscribed = totalNumberOfChannels;

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

					if (showDialog)
						showRefreshDialog();
				}
			}
		}

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
