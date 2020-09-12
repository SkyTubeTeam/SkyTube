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

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.AsyncTaskParallel;
import free.rm.skytube.businessobjects.FeedUpdaterService;
import free.rm.skytube.businessobjects.VideoCategory;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeAPIKey;
import free.rm.skytube.businessobjects.YouTube.Tasks.GetBulkSubscriptionVideosTask;
import free.rm.skytube.businessobjects.YouTube.Tasks.GetSubscriptionVideosTask;
import free.rm.skytube.businessobjects.YouTube.Tasks.GetSubscriptionVideosTaskListener;
import free.rm.skytube.businessobjects.YouTube.newpipe.NewPipeService;
import free.rm.skytube.businessobjects.db.SubscriptionsDb;
import free.rm.skytube.businessobjects.db.Tasks.GetSubscribedChannelsTask;
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
	private static final int    REFRESH_TIME_HOURS = 3;
	private static final long   REFRESH_TIME_IN_MS = REFRESH_TIME_HOURS * (1000L*3600L);

	private static final String NOTIFICATION_CHANNEL_NAME = "SkyTube";
	private static final String NOTIFICATION_CHANNEL_ID = "subscriptionChecking";
	private static final int NOTIFICATION_ID = 1;

	@Override
	protected int getLayoutResource() {
		return R.layout.fragment_subs_feed;
	}


	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		subscriptionsBackupsManager = new SubscriptionsBackupsManager(getActivity(), SubscriptionsFeedFragment.this);

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View result = super.onCreateView(inflater, container, savedInstanceState);
		videoGridAdapter.setVideoGridUpdated(this::setupUiAccordingToNumOfSubbedChannels);
		return result;
	}

	private boolean checkRefreshTime() {
		// Only do an automatic refresh of subscriptions if it's been more than three hours since the last one was done.
		Long subscriptionsLastUpdated = SkyTubeApp.getSettings().getFeedsLastUpdateTime();
		if (subscriptionsLastUpdated == null) {
			return true;
		}
		long threeHoursAgo = System.currentTimeMillis() - REFRESH_TIME_IN_MS;
		return subscriptionsLastUpdated <= threeHoursAgo;
	}


	@Override
	public void onResume() {

		getActivity().registerReceiver(feedUpdaterReceiver, new IntentFilter(FeedUpdaterService.NEW_SUBSCRIPTION_VIDEOS_FOUND));

		super.onResume();

		// setup the UI and refresh the feed (if applicable)
		startRefreshTask(isFragmentSelected(), checkRefreshTime() || isFlagSet(FLAG_REFRESH_FEED_FULL));

		// this will detect whether we have previous instructed the app (via refreshSubsFeedFromCache())
		// to refresh the subs feed
		if (isFlagSet(FLAG_REFRESH_FEED_FROM_CACHE)) {
			// unset the flag
			unsetFlag(FLAG_REFRESH_FEED_FROM_CACHE);

			// refresh the subs feed by reading from the cache (i.e. local DB)
			refreshFeedFromCache();
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

	private void showNotification() {
		// Sets an ID for the notification, so it can be updated.
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
			NotificationManager mNotificationManager =
					(NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);

			NotificationChannel mChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
			mChannel.setSound(null,null);
			// Create a notification and set the notification channel.
			Notification notification = new Notification.Builder(getContext(), NOTIFICATION_CHANNEL_NAME)
					.setSmallIcon(R.drawable.ic_notification_icon)
					.setContentTitle(getString(R.string.fetching_subscription_videos))
					.setContentText(String.format(getContext().getString(R.string.fetched_videos_from_channels), numVideosFetched, numChannelsFetched, numChannelsSubscribed))
					.setChannelId(NOTIFICATION_CHANNEL_ID)
					.build();
			mNotificationManager.createNotificationChannel(mChannel);

// Issue the notification.
			mNotificationManager.notify(NOTIFICATION_ID , notification);
		} else {
			final Intent emptyIntent = new Intent();
			PendingIntent pendingIntent = PendingIntent.getActivity(getContext(), 1, emptyIntent, PendingIntent.FLAG_UPDATE_CURRENT);
			NotificationCompat.Builder builder = new NotificationCompat.Builder(getContext(), "")
					.setSmallIcon(R.drawable.ic_notification_icon)
					.setContentTitle(getString(R.string.fetching_subscription_videos))
					.setContentText(String.format(getContext().getString(R.string.fetched_videos_from_channels), numVideosFetched, numChannelsFetched, numChannelsSubscribed))
					.setPriority(NotificationCompat.FLAG_ONGOING_EVENT)
					.setContentIntent(pendingIntent);


			NotificationManager notificationManager = (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
			notificationManager.notify(NOTIFICATION_ID, builder.build());
		}
	}

	@Override
	public void onRefresh() {
		startRefreshTask(true, true);
	}


	protected synchronized void startRefreshTask(boolean showFetchingVideosDialog, boolean forcedFullRefresh) {
		if (refreshInProgress) {
			return;
		}
		if (forcedFullRefresh && SkyTubeApp.isConnected(getContext())) {
			unsetFlag(FLAG_REFRESH_FEED_FULL);
			refreshInProgress = true;
			new GetSubscribedChannelsTask(this.getContext(), channelsRefreshed ->
					Log.i("SUB FRAGMENT", "Refreshed "+ channelsRefreshed.size())).executeInParallel();
			new RefreshFeedTask(showFetchingVideosDialog, forcedFullRefresh).executeInParallel();
		} else {
			videoGridAdapter.refresh(true);
		}
	}

	@Override
	public void onChannelVideosFetched(String channelId, int videosFetched, final boolean videosDeleted) {
		Log.d("SUB FRAGMENT", "onChannelVideosFetched");

		// If any new videos have been fetched for a channel, update the Subscription list in the left navbar for that channel
		if(videosFetched > 0) {
			SubsAdapter.get(getActivity()).changeChannelNewVideosStatus(channelId, true);
		}

		numVideosFetched += videosFetched;
		numChannelsFetched++;

		showNotification();
	}

	@Override
	public void onAllChannelVideosFetched(boolean changed) {
		Log.i("SUB FRAGMENT", "onAllChannelVideosFetched:" + changed);
		new Handler().postDelayed(() -> {
			refreshInProgress = false;
			// Remove the progress bar(s)
			if (swipeRefreshLayout != null) {
				swipeRefreshLayout.setRefreshing(false);
			}
			Context context = getContext();

			NotificationManager notificationManager = (NotificationManager)  context.getSystemService(Context.NOTIFICATION_SERVICE);

			notificationManager.cancel(NOTIFICATION_ID);

			if(changed) {
				refreshFeedFromCache();
				Toast.makeText(context,
						String.format(context.getString(R.string.notification_new_videos_found),
								numVideosFetched), Toast.LENGTH_LONG).show();
			} else {
				Toast.makeText(context,
									R.string.no_new_videos_found,
									Toast.LENGTH_LONG).show();
			}
		}, 500);
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
	private class RefreshFeedTask extends AsyncTaskParallel<Void, Void, List<String>> {

		private MaterialDialog  fetchingChannelInfoDialog;
		private boolean         showDialogs;
		private boolean 		fullRefresh;


		private RefreshFeedTask(boolean showFetchingVideosDialog, boolean fullRefresh) {
			this.showDialogs = showFetchingVideosDialog;
			this.fullRefresh = fullRefresh;
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
		protected List<String> doInBackground(Void... params) {
			return SubscriptionsDb.getSubscriptionsDb().getSubscribedChannelIds();
		}


		@Override
		protected void onPostExecute(List<String> totalChannels) {
			numVideosFetched      = 0;
			numChannelsFetched    = 0;
			numChannelsSubscribed = totalChannels.size();

			// hide the "Fetching channels information …" dialog
			if (showDialogs) {
				fetchingChannelInfoDialog.dismiss();
			}

			// setup the user interface
			setupUiAccordingToNumOfSubbedChannels(numChannelsSubscribed);

			if (numChannelsSubscribed > 0) {
				// get the previously published videos currently cached in the database
				videoGridAdapter.setVideoCategory(VideoCategory.SUBSCRIPTIONS_FEED_VIDEOS);

				// get any videos published after the last time the user used the app...
				if (fullRefresh) {
					//new GetSubscriptionVideosTask(SubscriptionsFeedFragment.this).executeInParallel();      // refer to #onChannelVideosFetched()
					getRefreshTask(totalChannels).executeInParallel();

					showNotification();
				}
			} else {
				refreshInProgress = false;
			}
		}

	}

	public void refreshFeedFromCache() {
		videoGridAdapter.refresh(true);
	}

	private AsyncTaskParallel<?,?,?> getRefreshTask(List<String> channelIds) {
		if (NewPipeService.isPreferred() || !YouTubeAPIKey.get().isUserApiKeySet()) {
			return new GetBulkSubscriptionVideosTask(channelIds, SubscriptionsFeedFragment.this);
		} else {
			return new GetSubscriptionVideosTask(SubscriptionsFeedFragment.this, channelIds);
		}
	}

}
