/*
 * SkyTube
 * Copyright (C) 2015  Ramon Mifsud
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
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.youtube.player.YouTubeStandalonePlayer;
import com.google.api.client.util.DateTime;
import com.google.api.services.youtube.model.Thumbnail;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoStatistics;

import org.ocpsoft.prettytime.PrettyTime;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Date;

import free.rm.skytube.R;
import free.rm.skytube.businessobjects.GetYouTubeVideos;
import free.rm.skytube.businessobjects.GetYouTubeVideosTask;
import free.rm.skytube.businessobjects.VideoCategory;
import free.rm.skytube.businessobjects.VideoDuration;
import free.rm.skytube.gui.activities.YouTubePlayerActivity;

/**
 * An adapter that will display videos in a {@link android.widget.GridView}.
 */
public class GridAdapter extends BaseAdapterEx<Video> {

	/** Class used to get YouTube videos from the web. */
	private GetYouTubeVideos getYouTubeVideos;
	/** Cache used to temporary store bitmap instances. */
	private BitmapCache		bitmapCache;

	private static final String TAG = GridAdapter.class.getSimpleName();


	public GridAdapter(Context context) {
		super(context);
		getYouTubeVideos = null;
		bitmapCache = new BitmapCache(context);
	}


	/**
	 * Set the video category.  Upon set, the adapter will download the videos of the specified
	 * category asynchronously.
	 *
	 * @param videoCategory	The video category you want to change to.
	 * @param context		{@link Context} instance.
	 */
	public void setVideoCategory(VideoCategory videoCategory, Context context) {
		try {
			Log.i(TAG, videoCategory.toString());

			// clear all previous items in this adapter
			this.clearList();

			// create a new instance of GetYouTubeVideos
			this.getYouTubeVideos = videoCategory.createGetYouTubeVideos();
			this.getYouTubeVideos.init(context);

			// get the videos from the web asynchronously
			new GetYouTubeVideosTask(getYouTubeVideos, this).execute();
		} catch (IOException e) {
			Log.e(TAG, "Could not init " + videoCategory, e);
			Toast.makeText(context,
					String.format(context.getString(R.string.could_not_get_videos), videoCategory.toString()),
					Toast.LENGTH_LONG).show();
		}
	}


	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View row;
		ViewHolder viewHolder;

		if (convertView == null) {
			row = getLayoutInflater().inflate(R.layout.cell, parent, false);
			viewHolder = new ViewHolder(row);
			row.setTag(viewHolder);
		} else {
			row = convertView;
			viewHolder = (ViewHolder) row.getTag();
		}

		if (viewHolder != null) {
			viewHolder.updateViewsData(get(position));
			viewHolder.updateVideoId(get(position).getId());
		}

		// if it reached the bottom of the list, then try to get the next page of videos
		if (position == getCount() - 1) {
			Log.w(TAG, "BOTTOM REACHED!!!");
			new GetYouTubeVideosTask(getYouTubeVideos, this).execute();
		}

		// if the user clicks the row, then play the video
		row.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View rowView) {
				ViewHolder viewHolder = (ViewHolder) rowView.getTag();

				if (viewHolder != null) {
					// if the user has selected to play the videos using the official YouTube player
					// (in the preferences/settings) ...
					if (useOfficialYouTubePlayer()) {
						launchOfficialYouTubePlayer(viewHolder.getVideoId());
					} else {
						// else we use the standalone player
						Intent i = new Intent(getContext(), YouTubePlayerActivity.class);
						i.putExtra(YouTubePlayerActivity.VIDEO_ID, viewHolder.getVideoId());
						getContext().startActivity(i);
					}
				}
			}
		});

		return row;
	}


	/**
	 * Read the user's preferences and determine if the user wants to use the official YouTube video
	 * player or not.
	 *
	 * @return True if the user wants to use the official player; false otherwise.
	 */
	private boolean useOfficialYouTubePlayer() {
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
		return sharedPref.getBoolean(getContext().getString(R.string.pref_key_use_offical_player), false);
	}


	/**
	 * Launches the official YouTube player so that the user can view the selected video.
	 *
	 * @param videoId Video ID to be viewed.
	 */
	private void launchOfficialYouTubePlayer(String videoId) {
		try {
			// try to start the YouTube standalone player
			Intent intent = YouTubeStandalonePlayer.createVideoIntent((Activity) getContext(), getContext().getString(R.string.API_KEY), videoId);
			getContext().startActivity(intent);
		} catch (Exception e) {
			String errorMsg = getContext().getString(R.string.launch_offical_player_error);

			// log the error
			Log.e(TAG, errorMsg, e);

			// display the error in an AlertDialog
			new AlertDialog.Builder(getContext())
					.setTitle(R.string.error)
					.setMessage(errorMsg)
					.setIcon(android.R.drawable.ic_dialog_alert)
					.setNeutralButton(android.R.string.ok, null)
					.show();
		}
	}




	////////////////////////////////////////////////////////////////////////////////////////////////

	protected class ViewHolder {
		private TextView	titleTextView,
							channelTextView,
							thumbsUpPercentageTextView,
							videoDurationTextView,
							viewsTextView,
							publishDateTextView;
		private InternetImageView thumbnailImageView;
		/** YouTube video ID */
		private String videoId;

		protected ViewHolder(View row) {
			titleTextView				= (TextView) row.findViewById(R.id.title_text_view);
			channelTextView				= (TextView) row.findViewById(R.id.channel_text_view);
			thumbsUpPercentageTextView	= (TextView) row.findViewById(R.id.thumbs_up_text_view);
			videoDurationTextView		= (TextView) row.findViewById(R.id.video_duration_text_view);
			viewsTextView				= (TextView) row.findViewById(R.id.views_text_view);
			publishDateTextView			= (TextView) row.findViewById(R.id.publish_date_text_view);
			thumbnailImageView	= (InternetImageView) row.findViewById(R.id.thumbnail_image_view);
		}


		protected void updateVideoId(String videoId) {
			this.videoId = videoId;
		}

		protected String getVideoId() {
			return videoId;
		}


		/**
		 * This method will update the {@link View}s of this object reflecting the supplied video.
		 *
		 * @param video Video instance.
		 */
		protected void updateViewsData(Video video) {
			if (video.getSnippet() != null) {
				titleTextView.setText(video.getSnippet().getTitle());
				channelTextView.setText(video.getSnippet().getChannelTitle());
				publishDateTextView.setText(getPublishDate(video.getSnippet().getPublishedAt()));
			}

			if (video.getContentDetails() != null)
				videoDurationTextView.setText(VideoDuration.toHumanReadableString(video.getContentDetails().getDuration()));

			if (video.getStatistics() != null) {
				thumbsUpPercentageTextView.setText(getThumbsUpPercentage(video.getStatistics()));

				BigInteger views = video.getStatistics().getViewCount();
				viewsTextView.setText(String.format(getContext().getString(R.string.views), views));
			}

			Thumbnail thumbnail = video.getSnippet().getThumbnails().getHigh();
			if (thumbnail != null)
				thumbnailImageView.setImageAsync(bitmapCache, thumbnail.getUrl());
		}


		/**
		 * Converts the given video's publish date into a pretty string.
		 *
		 * @param publishDateTime {@link DateTime} of when the video was published.
		 *
		 * @return String representing the publish time (e.g. "7 hours ago").
		 */
		private String getPublishDate(DateTime publishDateTime) {
			Long unixEpoch   = publishDateTime.getValue();
			Date publishDate = new Date(unixEpoch);
			return new PrettyTime().format(publishDate);
		}


		/**
		 * Returns the percentage of people that thumbs-upped the given video.
		 *
		 * @param videoStatistics {@link VideoStatistics} instance
		 * @return Percentage as a string.  Format:  "<percentage>%"
		 */
		private String getThumbsUpPercentage(VideoStatistics videoStatistics) {
			String percentage;

			try {
				BigDecimal likedCount = new BigDecimal(videoStatistics.getLikeCount()),
						dislikedCount = new BigDecimal(videoStatistics.getDislikeCount()),
						totalVoteCount = likedCount.add(dislikedCount),    // liked and disliked counts
						likedPercentage = (likedCount.divide(totalVoteCount, MathContext.DECIMAL128)).multiply(new BigDecimal(100));

				// round the liked percentage to 0 decimal places and convert it to string
				percentage = likedPercentage.setScale(0, RoundingMode.HALF_UP).toString();
			} catch (Exception e) {
				percentage = "??";
			}

			return percentage + "%";
		}

	}

}
