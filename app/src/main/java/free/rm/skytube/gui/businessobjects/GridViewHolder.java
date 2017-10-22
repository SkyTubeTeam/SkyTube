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
import android.content.Intent;
import android.net.Uri;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import free.rm.skytube.R;
import free.rm.skytube.businessobjects.YouTubeVideo;

/**
 * A ViewHolder for the videos grid view.
 */
public class GridViewHolder extends RecyclerView.ViewHolder {
	/** YouTube video */
	private YouTubeVideo youTubeVideo = null;
	private Context context = null;
	private MainActivityListener listener;

	@BindView(R.id.channel_layout)
	View channelLayout;
 	@BindView(R.id.title_text_view)
	TextView titleTextView;
 	@BindView(R.id.channel_text_view)
 	TextView channelTextView;
 	@BindView(R.id.thumbs_up_text_view)
 	TextView thumbsUpPercentageTextView;
 	@BindView(R.id.video_duration_text_view)
 	TextView videoDurationTextView;
 	@BindView(R.id.publish_date_text_view)
 	TextView publishDateTextView;
 	@BindView(R.id.thumbnail_image_view)
 	ImageView thumbnailImageView;
 	@BindView(R.id.views_text_view)
 	TextView viewsTextView;


	public GridViewHolder(View view, MainActivityListener listener) {
		super(view);
		ButterKnife.bind(this, view);
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
					YouTubePlayer.launch(youTubeVideo, context);
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

	@OnClick(R.id.options_button)
 	public void onOptionsButtonClick(final View view) {
		final PopupMenu popupMenu = new PopupMenu(view.getContext(), view);
		popupMenu.getMenuInflater().inflate(R.menu.video_options_menu, popupMenu.getMenu());
		Menu menu = popupMenu.getMenu();
		new IsVideoBookmarkedTask(youTubeVideo, menu).executeInParallel();
		popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				switch(item.getItemId()) {
					case R.id.menu_open_video_with:
						Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(youTubeVideo.getVideoUrl()));
						context.startActivity(browserIntent);
						return true;
					case R.id.share:
						youTubeVideo.shareVideo(view.getContext());
						return true;
					case R.id.copyurl:
						youTubeVideo.copyUrl(context);
						return true;
					case R.id.bookmark_video:
						youTubeVideo.bookmarkVideo(context, popupMenu.getMenu());
						return true;
					case R.id.unbookmark_video:
						youTubeVideo.unbookmarkVideo(context, popupMenu.getMenu());
						return true;
				}
				return false;
			}
		});
		popupMenu.show();
	}
}
