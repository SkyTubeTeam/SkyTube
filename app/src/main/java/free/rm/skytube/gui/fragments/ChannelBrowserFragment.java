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

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.TextView;

import java.io.IOException;

import free.rm.skytube.R;
import free.rm.skytube.businessobjects.VideoCategory;
import free.rm.skytube.businessobjects.YouTubeChannel;
import free.rm.skytube.businessobjects.db.SubscribeToChannelTask;
import free.rm.skytube.gui.activities.ChannelBrowserActivity;
import free.rm.skytube.gui.businessobjects.FragmentEx;
import free.rm.skytube.gui.businessobjects.InternetImageView;
import free.rm.skytube.gui.businessobjects.SubsAdapter;
import free.rm.skytube.gui.businessobjects.SubscribeButton;
import free.rm.skytube.gui.businessobjects.VideoGridAdapter;

/**
 * A Fragment that displays information about a channel.
 */
public class ChannelBrowserFragment extends FragmentEx {

	private YouTubeChannel	channel = null;
	private GridView		gridView;
	private VideoGridAdapter videoGridAdapter;

	private InternetImageView	channelThumbnailImage = null;
	private InternetImageView	channelBannerImage = null;
	private TextView			channelSubscribersTextView = null;
	private SubscribeButton		channelSubscribeButton = null;
	private GetChannelInfoTask	task = null;

	private static final String TAG = ChannelBrowserActivity.class.getSimpleName();


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final String channelId;
		Bundle bundle = getActivity().getIntent().getExtras();

		// we need to create a YouTubeChannel object:  this can be done by either:
		//   (1) the object is passed to this Fragment
		//   (2) passing the channel ID... a task is then created to create a YouTubeChannel
		//       instance using the given channel ID
		if (bundle != null  &&  bundle.getSerializable(ChannelBrowserActivity.CHANNEL_OBJ) != null) {
			this.channel = (YouTubeChannel) bundle.getSerializable(ChannelBrowserActivity.CHANNEL_OBJ);
			channelId = channel.getId();
		} else {
			channelId = getActivity().getIntent().getStringExtra(ChannelBrowserActivity.CHANNEL_ID);
		}

		// inflate the layout for this fragment
		View fragment = inflater.inflate(R.layout.fragment_channel_browser, container, false);

		channelBannerImage = (InternetImageView) fragment.findViewById(R.id.channel_banner_image_view);
		channelThumbnailImage = (InternetImageView) fragment.findViewById(R.id.channel_thumbnail_image_view);
		channelSubscribersTextView = (TextView) fragment.findViewById(R.id.channel_subs_text_view);
		channelSubscribeButton = (SubscribeButton) fragment.findViewById(R.id.channel_subscribe_button);
		channelSubscribeButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// subscribe / unsubscribe to this video's channel
				new SubscribeToChannelTask(channelSubscribeButton, channel).execute();
			}
		});

		if (channel == null) {
			if (task == null) {
				task = new GetChannelInfoTask();
				task.execute(channelId);
			}
		} else {
			initViews();
		}

		gridView = (GridView) fragment.findViewById(R.id.grid_view);

		if (videoGridAdapter == null) {
			videoGridAdapter = new VideoGridAdapter(getActivity(), false /*hide channel name*/);
			videoGridAdapter.setVideoCategory(VideoCategory.CHANNEL_VIDEOS, channelId);
		}

		this.gridView.setAdapter(this.videoGridAdapter);

		return fragment;
	}


	/**
	 * Initialise views that are related to {@link #channel}.
	 */
	private void initViews() {
		if (channel != null) {
			channelThumbnailImage.setImageAsync(channel.getThumbnailNormalUrl());
			channelBannerImage.setImageAsync(channel.getBannerUrl());
			channelSubscribersTextView.setText(channel.getTotalSubscribers());

			ActionBar actionBar = getSupportActionBar();
			if (actionBar != null) {
				actionBar.setTitle(channel.getTitle());
			}

			// if the user has subscribed to this channel, then change the state of the
			// subscribe button
			if (channel.isUserSubscribed()) {
				channelSubscribeButton.setUnsubscribeState();
			} else {
				channelSubscribeButton.setSubscribeState();
			}

			if (channel.isUserSubscribed()) {
				// the user is visiting the channel, so we need to update the last visit time
				channel.updateLastVisitTime();

				if (channel.newVideosSinceLastVisit()) {
					// since we are visiting the channel, then we need to disable the new videos notification
					channel.setNewVideosSinceLastVisit(false);
					// since this.channel and SubsAdapter's channel are different instances, then we
					// need to modify both of them [different because of bundle.getSerializable(channel)]
					SubsAdapter.get(getActivity()).changeChannelNewVideosStatus(channel.getId(), false);
				}
			}
		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////

	private class GetChannelInfoTask extends AsyncTask<String, Void, YouTubeChannel> {

		private final String TAG = GetChannelInfoTask.class.getSimpleName();

		@Override
		protected YouTubeChannel doInBackground(String... channelId) {
			YouTubeChannel chn = new YouTubeChannel();

			try {
				// initialise the channel
				chn.init(channelId[0]);
			} catch (IOException e) {
				Log.e(TAG, "Unable to get channel info.  ChannelID=" + channelId[0], e);
				chn = null;
			}

			return chn;
		}

		@Override
		protected void onPostExecute(YouTubeChannel youTubeChannel) {
			ChannelBrowserFragment.this.channel = youTubeChannel;
			initViews();
		}

	}

}
