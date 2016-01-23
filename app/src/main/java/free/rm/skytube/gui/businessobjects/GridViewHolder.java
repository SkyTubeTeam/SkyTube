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

package free.rm.skytube.gui.businessobjects;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.youtube.player.YouTubeStandalonePlayer;

import free.rm.skytube.R;
import free.rm.skytube.businessobjects.YouTubeVideo;
import free.rm.skytube.gui.activities.ChannelBrowserActivity;
import free.rm.skytube.gui.activities.YouTubePlayerActivity;

/**
 *
 */
public class GridViewHolder {
	private TextView titleTextView,
			channelTextView,
			thumbsUpPercentageTextView,
			videoDurationTextView,
			viewsTextView,
			publishDateTextView;
	private InternetImageView	thumbnailImageView;
	private RelativeLayout bottomLayout;
	/** YouTube video */
	private YouTubeVideo youTubeVideo = null;
	private Context context = null;

	private static final String TAG = GridViewHolder.class.getSimpleName();

	protected GridViewHolder(View row) {
		titleTextView				= (TextView) row.findViewById(R.id.title_text_view);
		channelTextView				= (TextView) row.findViewById(R.id.channel_text_view);
		thumbsUpPercentageTextView	= (TextView) row.findViewById(R.id.thumbs_up_text_view);
		videoDurationTextView		= (TextView) row.findViewById(R.id.video_duration_text_view);
		viewsTextView				= (TextView) row.findViewById(R.id.views_text_view);
		publishDateTextView			= (TextView) row.findViewById(R.id.publish_date_text_view);
		thumbnailImageView	= (InternetImageView) row.findViewById(R.id.thumbnail_image_view);
		bottomLayout		= (RelativeLayout) row.findViewById(R.id.cell_bottom_layout);
	}


	/**
	 * Updates the contents of this ViewHold such that the data of these views is equal to the
	 * given youTubeVide.
	 *
	 * @param youTubeVideo	{@link YouTubeVideo} instance.
	 */
	protected void updateInfo(YouTubeVideo youTubeVideo, Context context) {
		this.youTubeVideo = youTubeVideo;
		this.context = context;
		updateViewsData(this.youTubeVideo);
	}


	/**
	 * This method will update the {@link View}s of this object reflecting the supplied video.
	 *
	 * @param video {@link YouTubeVideo} instance.
	 */
	private void updateViewsData(YouTubeVideo video) {
		titleTextView.setText(video.getTitle());
		channelTextView.setText(video.getChannelName());
		publishDateTextView.setText(video.getPublishDate());
		videoDurationTextView.setText(video.getDuration());
		viewsTextView.setText(video.getViewsCount());
		thumbnailImageView.setImageAsync(video.getThumbnailUrl());

		if (video.getThumbsUpPercentageStr() != null) {
			thumbsUpPercentageTextView.setVisibility(View.VISIBLE);
			thumbsUpPercentageTextView.setText(video.getThumbsUpPercentageStr());
		} else {
			thumbsUpPercentageTextView.setVisibility(View.INVISIBLE);
		}

		setupThumbnailOnClickListener();
		setupChannelOnClickListener();
	}


	private void setupThumbnailOnClickListener() {
		thumbnailImageView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View thumbnailView) {
				if (youTubeVideo != null) {
					// if the user has selected to play the videos using the official YouTube player
					// (in the preferences/settings) ...
					if (useOfficialYouTubePlayer()) {
						launchOfficialYouTubePlayer(youTubeVideo.getId());
					} else {
						// else we use the standalone player
						Intent i = new Intent(context, YouTubePlayerActivity.class);
						i.putExtra(YouTubePlayerActivity.YOUTUBE_VIDEO_OBJ, youTubeVideo);
						context.startActivity(i);
					}
				}
			}
		});
	}


	/**
	 * Read the user's preferences and determine if the user wants to use the official YouTube video
	 * player or not.
	 *
	 * @return True if the user wants to use the official player; false otherwise.
	 */
	private boolean useOfficialYouTubePlayer() {
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
		return sharedPref.getBoolean(context.getString(R.string.pref_key_use_offical_player), false);
	}


	/**
	 * Launches the official YouTube player so that the user can view the selected video.
	 *
	 * @param videoId Video ID to be viewed.
	 */
	private void launchOfficialYouTubePlayer(String videoId) {
		try {
			// try to start the YouTube standalone player
			Intent intent = YouTubeStandalonePlayer.createVideoIntent((Activity) context, context.getString(R.string.API_KEY), videoId);
			context.startActivity(intent);
		} catch (Exception e) {
			String errorMsg = context.getString(R.string.launch_offical_player_error);

			// log the error
			Log.e(TAG, errorMsg, e);

			// display the error in an AlertDialog
			new AlertDialog.Builder(context)
					.setTitle(R.string.error)
					.setMessage(errorMsg)
					.setIcon(android.R.drawable.ic_dialog_alert)
					.setNeutralButton(android.R.string.ok, null)
					.show();
		}
	}


	private void setupChannelOnClickListener() {
		View.OnClickListener channelListener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(context, ChannelBrowserActivity.class);
				i.putExtra(ChannelBrowserActivity.CHANNEL_ID, youTubeVideo.getChannelId());
				context.startActivity(i);
			}
		};

		channelTextView.setOnClickListener(channelListener);
		bottomLayout.setOnClickListener(channelListener);
	}

}
