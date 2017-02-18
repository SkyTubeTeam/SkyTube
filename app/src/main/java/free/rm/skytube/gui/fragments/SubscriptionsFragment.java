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
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.List;

import butterknife.Bind;
import free.rm.skytube.R;
import free.rm.skytube.businessobjects.AsyncTaskParallel;
import free.rm.skytube.businessobjects.GetSubscriptionVideosTask;
import free.rm.skytube.businessobjects.VideoCategory;
import free.rm.skytube.businessobjects.YouTubeChannel;
import free.rm.skytube.businessobjects.YouTubeVideo;
import free.rm.skytube.businessobjects.db.SubscriptionsDb;
import free.rm.skytube.gui.app.SkyTubeApp;
import free.rm.skytube.gui.businessobjects.SubsAdapter;
import free.rm.skytube.gui.businessobjects.SubscriptionsFragmentListener;

/**
 * Fragment that displays videos from all channels the user is subscribed to.
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

		new GetTotalNumberOfChannelsTask().executeInParallel();
	}


	private void doRefresh(boolean showDialog) {
		new RefreshTask(showDialog).executeInParallel();
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
		Log.d("SUB FRAGMENT", "onChannelVideosFetched");

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
						new SetVideosListTask().executeInParallel();
					} else {
						// Only show the toast that no videos were found if the progress dialog is sh
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


	@Override
	protected void onFragmentSelected() {
		// when the Subscriptions tab is selected, if a refresh is in progress, show the dialog.
		if (refreshInProgress)
			showRefreshDialog();
	}


	@Override
	protected VideoCategory getVideoCategory() {
		return VideoCategory.SUBSCRIPTIONS_FEED_VIDEOS;
	}


	@Override
	protected String getFragmentName() {
		return SkyTubeApp.getStr(R.string.subscriptions);
	}



	////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * A task that fetched the total number of subscribed channels from the DB and updated the
	 * SubscriptionsFragment UI accordingly.
	 */
	private class GetTotalNumberOfChannelsTask extends AsyncTaskParallel<Void, Void, Integer> {

		@Override
		protected Integer doInBackground(Void... params) {
			return SubscriptionsDb.getSubscriptionsDb().getTotalSubscribedChannels();
		}


		@Override
		protected void onPostExecute(Integer totalNumberOfChannels) {
			numChannelsSubscribed = totalNumberOfChannels;

			if (numChannelsSubscribed <= 0) {
				swipeRefreshLayout.setVisibility(View.GONE);
				noSubscriptionsText.setVisibility(View.VISIBLE);
			} else {
				if(swipeRefreshLayout.getVisibility() != View.VISIBLE) {
					swipeRefreshLayout.setVisibility(View.VISIBLE);
					noSubscriptionsText.setVisibility(View.GONE);
				}

				videoGridAdapter.setVideoCategory(VideoCategory.SUBSCRIPTIONS_FEED_VIDEOS);

				// Launch a refresh of subscribed videos when this Fragment is created, but don't
				// show the progress dialog. It will be shown when the tab is shown.
				if (shouldRefresh)
					doRefresh(false);
				shouldRefresh = false;
			}
		}
	}


	/**
	 * A task that refreshes the videos of the {@link SubscriptionsFragment}.
	 */
	private class RefreshTask extends AsyncTaskParallel<Void, Void, Integer> {

		private boolean showDialog;


		public RefreshTask(boolean showDialog) {
			this.showDialog = showDialog;
		}


		@Override
		protected Integer doInBackground(Void... params) {
			return SubscriptionsDb.getSubscriptionsDb().getTotalSubscribedChannels();
		}


		@Override
		protected void onPostExecute(Integer totalNumberOfChannels) {
			numVideosFetched      = 0;
			numChannelsFetched    = 0;
			numChannelsSubscribed = totalNumberOfChannels;

			if (numChannelsSubscribed > 0) {
				new GetSubscriptionVideosTask(SubscriptionsFragment.this).executeInParallel();
				refreshInProgress = true;

				if (showDialog)
					showRefreshDialog();
			}
		}

	}



	private class SetVideosListTask extends AsyncTaskParallel<Void, Void, List<YouTubeVideo>> {

		@Override
		protected List<YouTubeVideo> doInBackground(Void... params) {
			return SubscriptionsDb.getSubscriptionsDb().getSubscriptionVideos();
		}

		@Override
		protected void onPostExecute(List<YouTubeVideo> youTubeVideos) {
			videoGridAdapter.setList(youTubeVideos);
		}

	}

}
