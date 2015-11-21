/*
 * SkyTube
 * Copyright (C) 2015  Ramon Mifsud
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
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

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.api.services.youtube.model.Thumbnail;
import com.google.api.services.youtube.model.Video;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import free.rm.skytube.R;
import free.rm.skytube.businessobjects.GetFeaturedVideos;
import free.rm.skytube.businessobjects.GetMostPopularVideos;
import free.rm.skytube.businessobjects.GetYouTubeVideos;
import free.rm.skytube.businessobjects.GetYouTubeVideosTask;
import free.rm.skytube.businessobjects.VideoDuration;

/**
 *
 */
public class GridAdapter extends BaseAdapterEx<Video> {

	private GetYouTubeVideos getYouTubeVideos = new GetMostPopularVideos(); //GetFeaturedVideos();
	private BitmapCache		bitmapCache;

	private static final String TAG = GridAdapter.class.getSimpleName();


	public GridAdapter(Context context) {
		super(context);
		bitmapCache = new BitmapCache(context);

		try {
			getYouTubeVideos.init(context);
			new GetYouTubeVideosTask(getYouTubeVideos, this).execute();
		} catch (IOException e) {
			Log.e(TAG, "Could not init featured videos...", e);
			Toast.makeText(context, context.getString(R.string.could_not_get_videos), Toast.LENGTH_LONG).show();
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
		}

		// if it reached the bottom of the list, then try to get the next page of videos
		if (position == getCount() - 1) {
			Log.w(TAG, "BOTTOM REACHED!!!");
			new GetYouTubeVideosTask(getYouTubeVideos, this).execute();
		}

		return row;
	}




	////////////////////////////////////////////////////////////////////////////////////////////////

	protected class ViewHolder {
		private TextView	titleTextView,
							channelTextView,
							thumbsUpPercentageTextView,
							videoDurationTextView;
		private InternetImageView thumbnailImageView;

		protected ViewHolder(View row) {
			titleTextView				= (TextView) row.findViewById(R.id.title_text_view);
			channelTextView				= (TextView) row.findViewById(R.id.channel_text_view);
			thumbsUpPercentageTextView	= (TextView) row.findViewById(R.id.thumbs_up_text_view);
			videoDurationTextView		= (TextView) row.findViewById(R.id.video_duration_text_view);
			thumbnailImageView	= (InternetImageView) row.findViewById(R.id.thumbnail_image_view);
		}

		/**
		 * This method will update the {@link View}s of this object reflecting the supplied video.
		 *
		 * @param video Video instance.
		 */
		protected void updateViewsData(Video video) {
			titleTextView.setText(video.getSnippet().getTitle());
			channelTextView.setText(video.getSnippet().getChannelTitle());
			thumbsUpPercentageTextView.setText(getThumbsUpPercentage(video));
			if (video.getContentDetails() != null)
				videoDurationTextView.setText(VideoDuration.toHumanReadableString(video.getContentDetails().getDuration()));

			Thumbnail thumbnail = video.getSnippet().getThumbnails().getHigh();
			if (thumbnail != null)
				thumbnailImageView.setImageAsync(bitmapCache, thumbnail.getUrl());
		}


		/**
		 * Returns the percentage of people that thumbs-upped the given video.
		 *
		 * @param video {@link Video} instance
		 * @return Percentage as a string.  Format:  "<percentage>%"
		 */
		private String getThumbsUpPercentage(Video video) {
			String percentage;

			try {
				BigDecimal likedCount = new BigDecimal(video.getStatistics().getLikeCount()),
						dislikedCount = new BigDecimal(video.getStatistics().getDislikeCount()),
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
