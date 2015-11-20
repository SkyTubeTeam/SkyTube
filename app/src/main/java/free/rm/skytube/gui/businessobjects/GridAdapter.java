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

import com.google.api.services.youtube.model.Thumbnail;
import com.google.api.services.youtube.model.Video;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;

import free.rm.skytube.R;
import free.rm.skytube.businessobjects.FeaturedVideosTask;
import free.rm.skytube.businessobjects.Iso8601Duration;

/**
 *
 */
public class GridAdapter extends BaseAdapterEx<Video> {

	private FeaturedVideosTask featuredVideosTask;
	private BitmapCache		bitmapCache;

	private static final String TAG = GridAdapter.class.getSimpleName();


	public GridAdapter(Context context) {
		super(context);
		bitmapCache = new BitmapCache(context);
		featuredVideosTask = new FeaturedVideosTask(context, this);
		featuredVideosTask.execute();
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
			Video video = get(position);

			viewHolder.titleTextView.setText(video.getSnippet().getTitle());
			viewHolder.channelTextView.setText(video.getSnippet().getChannelTitle());
			viewHolder.thumbsUpPercentageTextView.setText(getThumbsUpPercentage(video));
			if (video.getContentDetails() != null)
				viewHolder.videoDurationTextView.setText(Iso8601Duration.toHumanReadableString(video.getContentDetails().getDuration()));

			Thumbnail thumbnail = video.getSnippet().getThumbnails().getHigh();
			if (thumbnail != null)
				viewHolder.thumbnailImageView.setImageAsync(bitmapCache, thumbnail.getUrl());

		}

		return row;
	}


	/**
	 * Returns the percentage of people that thumbs-upped the given video.
	 *
	 * @param video
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


	protected class ViewHolder {
		TextView	titleTextView,
					channelTextView,
					thumbsUpPercentageTextView,
					videoDurationTextView;
		InternetImageView thumbnailImageView;

		ViewHolder(View row) {
			titleTextView				= (TextView) row.findViewById(R.id.title_text_view);
			channelTextView				= (TextView) row.findViewById(R.id.channel_text_view);
			thumbsUpPercentageTextView	= (TextView) row.findViewById(R.id.thumbs_up_text_view);
			videoDurationTextView		= (TextView) row.findViewById(R.id.video_duration_text_view);
			thumbnailImageView	= (InternetImageView) row.findViewById(R.id.thumbnail_image_view);
		}
	}

}
