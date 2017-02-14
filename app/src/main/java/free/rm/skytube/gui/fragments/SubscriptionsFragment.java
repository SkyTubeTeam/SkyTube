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
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import butterknife.Bind;
import free.rm.skytube.R;
import free.rm.skytube.businessobjects.AsyncTaskParallel;
import free.rm.skytube.businessobjects.GetSubscriptionVideosTask;
import free.rm.skytube.businessobjects.YouTubeChannel;
import free.rm.skytube.businessobjects.db.SubscriptionsDb;
import free.rm.skytube.gui.businessobjects.SubsAdapter;
import free.rm.skytube.gui.businessobjects.SubscriptionsFragmentListener;

/**
 * Fragment that displays videos from all channels the user is subscribed to
 */
public class SubscriptionsFragment extends VideosGridFragment implements SubscriptionsFragmentListener {
	private int numVideosFetched = 0;
	private int numChannelsFetched = 0;
	private int numChannelsSubscribed = 0;
	private boolean refreshInProgress = false;
	private MaterialDialog progressDialog;
	private boolean shouldRefresh = false;

	@Bind(R.id.noSubscriptionsText)
	View noSubscriptionsText;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		shouldRefresh = true;
		setLayoutResource(R.layout.videos_gridview_subscriptions);
	}

	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		videoGridAdapter.clearList();

		numChannelsSubscribed = SubscriptionsDb.getSubscriptionsDb().numSubscribedChannels();
		if(numChannelsSubscribed == 0) {
			swipeRefreshLayout.setVisibility(View.GONE);
			noSubscriptionsText.setVisibility(View.VISIBLE);
		} else {
			if(swipeRefreshLayout.getVisibility() != View.VISIBLE) {
				swipeRefreshLayout.setVisibility(View.VISIBLE);
				noSubscriptionsText.setVisibility(View.GONE);
			}


			populateList();

			// Launch a refresh of subscribed videos when this Fragment is created, but don't show the progress dialog. It will be shown when the tab is shown.
			if (shouldRefresh)
				doRefresh(false);
			shouldRefresh = false;
		}
	}

	private void populateList() {
		// {@link SubscriptionsDb.getSubscriptionsDb().getSubscriptionVideos()} should not be called in the UI thread here, so as to slow down the UI.
		new AsyncTaskParallel<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... voids) {
				// Modifying any views needs to be done in the UI thread.
				new Handler(Looper.getMainLooper()).post(new Runnable() {
					@Override
					public void run() {
						videoGridAdapter.appendList(SubscriptionsDb.getSubscriptionsDb().getSubscriptionVideos());
					}
				});
				return null;
			}
		}.executeInParallel();
	}

	private void doRefresh(boolean showDialog) {
		numVideosFetched = 0;
		numChannelsFetched = 0;
		numChannelsSubscribed = SubscriptionsDb.getSubscriptionsDb().numSubscribedChannels();
		if(numChannelsSubscribed > 0) {
			new GetSubscriptionVideosTask(this).executeInParallel();
			refreshInProgress = true;
			if (showDialog) {
				showRefreshDialog();
			}
		}
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
		doRefresh(true);
	}

	@Override
	public void onChannelVideosFetched(YouTubeChannel channel, int videosFetched, final boolean videosDeleted) {
		// If any new videos have been fetched for a channel, update the Subscription list in the left navbar for that channel
		if(videosFetched > 0)
			SubsAdapter.get(getActivity()).changeChannelNewVideosStatus(channel.getId(), true);

		numVideosFetched += videosFetched;
		numChannelsFetched++;
		if(progressDialog != null)
			progressDialog.setContent(String.format(getContext().getString(R.string.fetched_videos_from_channels), numVideosFetched, numChannelsFetched, numChannelsSubscribed));
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
						videoGridAdapter.clearList();
						videoGridAdapter.appendList(SubscriptionsDb.getSubscriptionsDb().getSubscriptionVideos());
					} else {
						// Only show the toast that no videos were found if the progress diaog is sh
						if(fragmentIsVisible) {
							Toast.makeText(getContext(),
											String.format(getContext().getString(R.string.no_new_videos_found)),
											Toast.LENGTH_LONG).show();
						}
					}
				}
			}, 500);
		}
	}

	/**
	 * When the Subscriptions tab is selected, if a refresh is in progress, show the dialog.
	 */
	public void onSelected() {
		if(refreshInProgress)
			showRefreshDialog();
	}
}
