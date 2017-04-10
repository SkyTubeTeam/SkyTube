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

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import free.rm.skytube.R;
import free.rm.skytube.businessobjects.MainActivityListener;
import free.rm.skytube.businessobjects.YouTubeVideo;

/**
 * A ViewHolder for the videos grid view.
 */
public class GridViewHolder extends RecyclerView.ViewHolder {
	/** YouTube video */
	private YouTubeVideo youTubeVideo = null;
	private Context context = null;
	private MainActivityListener listener;

	private View channelLayout;
	private TextView titleTextView;
	private TextView channelTextView;
	private TextView thumbsUpPercentageTextView;
	private TextView videoDurationTextView;
	private TextView publishDateTextView;
	private TextView viewsTextView;
	private ImageView thumbnailImageView;


	public GridViewHolder(View view, MainActivityListener listener) {
		super(view);
		channelLayout = view.findViewById(R.id.channel_layout);
		titleTextView = (TextView) view.findViewById(R.id.title_text_view);
		channelTextView = (TextView) view.findViewById(R.id.channel_text_view);
		thumbsUpPercentageTextView = (TextView) view.findViewById(R.id.thumbs_up_text_view);
		videoDurationTextView = (TextView) view.findViewById(R.id.video_duration_text_view);
		publishDateTextView = (TextView) view.findViewById(R.id.publish_date_text_view);
		viewsTextView = (TextView) view.findViewById(R.id.views_text_view);
		thumbnailImageView = (ImageView) view.findViewById(R.id.thumbnail_image_view);

		this.listener = listener;
	}


	/**
	 * Updates the contents of this ViewHold such that the data of these views is equal to the
	 * given youTubeVideo.
	 *
	 * @param youTubeVideo		{@link YouTubeVideo} instance.
	 * @param showChannelInfo   True to display channel information (e.g. channel name) and allows
	 *                          user to open and browse the channel; false to hide such information.
	 */
	protected void updateInfo(YouTubeVideo youTubeVideo, Context context, MainActivityListener listener, boolean showChannelInfo) {
		this.youTubeVideo = youTubeVideo;
		this.context = context;
		this.listener = listener;
		updateViewsData(this.youTubeVideo, showChannelInfo);
	}


	/**
	 * This method will update the {@link View}s of this object reflecting the supplied video.
	 *
	 * @param video				{@link YouTubeVideo} instance.
	 * @param showChannelInfo   True to display channel information (e.g. channel name); false to
	 *                          hide such information.
	 */
	private void updateViewsData(YouTubeVideo video, boolean showChannelInfo) {
		titleTextView.setText(video.getTitle());
		channelTextView.setText(showChannelInfo ? video.getChannelName() : "");
		publishDateTextView.setText(video.getPublishDatePretty());
		videoDurationTextView.setText(video.getDuration());
		viewsTextView.setText(video.getViewsCount());
		Glide.with(context)
				.load(video.getThumbnailUrl())
				.placeholder(R.drawable.thumbnail_default)
				.into(thumbnailImageView);

		if (video.getThumbsUpPercentageStr() != null) {
			thumbsUpPercentageTextView.setVisibility(View.VISIBLE);
			thumbsUpPercentageTextView.setText(video.getThumbsUpPercentageStr());
		} else {
			thumbsUpPercentageTextView.setVisibility(View.INVISIBLE);
		}

		setupThumbnailOnClickListener();
		setupChannelOnClickListener(showChannelInfo);
	}


	private void setupThumbnailOnClickListener() {
		thumbnailImageView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View thumbnailView) {
				if (youTubeVideo != null) {
					YouTubePlayer.launch(youTubeVideo, (Context)listener);
				}
			}
		});
	}



	private void setupChannelOnClickListener(boolean openChannelOnClick) {
		View.OnClickListener channelListener = null;

		if (openChannelOnClick) {
			channelListener = new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if(listener != null)
						listener.onChannelClick(youTubeVideo.getChannelId());
				}
			};
		}
		channelLayout.setOnClickListener(channelListener);
	}

}
