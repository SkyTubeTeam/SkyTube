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

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import free.rm.skytube.R;
import free.rm.skytube.businessobjects.MainActivityListener;
import free.rm.skytube.businessobjects.YouTubeVideo;
import free.rm.skytube.businessobjects.db.BookmarksDb;

/**
 * A ViewHolder for the videos grid view.
 */
public class GridViewHolder extends RecyclerView.ViewHolder {
	/** YouTube video */
	private YouTubeVideo youTubeVideo = null;
	private Context context = null;
	private MainActivityListener listener;

	@Bind(R.id.channel_layout)
	View channelLayout;
	@Bind(R.id.title_text_view)
	TextView titleTextView;
	@Bind(R.id.channel_text_view)
	TextView channelTextView;
	@Bind(R.id.thumbs_up_text_view)
	TextView thumbsUpPercentageTextView;
	@Bind(R.id.video_duration_text_view)
	TextView videoDurationTextView;
	@Bind(R.id.publish_date_text_view)
	TextView publishDateTextView;
	@Bind(R.id.thumbnail_image_view)
	ImageView thumbnailImageView;
	@Bind(R.id.views_text_view)
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

	@OnClick(R.id.options_button)
	public void onOptionsButtonClick(final View view) {
		PopupMenu popupMenu = new PopupMenu(view.getContext(), view);
		popupMenu.getMenuInflater().inflate(R.menu.video_options_menu, popupMenu.getMenu());

		// If this video has been bookmarked, hide the add option and show the remove option.
		if(BookmarksDb.getBookmarksDb().isBookmarked(youTubeVideo)) {
			popupMenu.getMenu().findItem(R.id.bookmark_video).setVisible(false);
			popupMenu.getMenu().findItem(R.id.unbookmark_video).setVisible(true);
		}

		popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				switch(item.getItemId()) {
					case R.id.share:
						Intent intent = new Intent(android.content.Intent.ACTION_SEND);
						intent.setType("text/plain");
						intent.putExtra(android.content.Intent.EXTRA_TEXT, youTubeVideo.getVideoUrl());
						view.getContext().startActivity(Intent.createChooser(intent, view.getContext().getString(R.string.share_via)));
						return true;

					case R.id.copyurl:
						ClipboardManager clipboard = (ClipboardManager)view.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
						ClipData clip = ClipData.newPlainText("Video URL", youTubeVideo.getVideoUrl());
						clipboard.setPrimaryClip(clip);
						Toast.makeText(view.getContext(), R.string.url_copied_to_clipboard, Toast.LENGTH_SHORT).show();
						return true;

					case R.id.bookmark_video:
						Toast.makeText(context,
								BookmarksDb.getBookmarksDb().add(youTubeVideo)  ?  R.string.video_bookmarked  :  R.string.video_bookmarked_error,
								Toast.LENGTH_LONG).show();
						return true;

					case R.id.unbookmark_video:
						Toast.makeText(context,
								BookmarksDb.getBookmarksDb().remove(youTubeVideo)  ?  R.string.video_unbookmarked  :  R.string.video_unbookmarked_error,
								Toast.LENGTH_LONG).show();
						return true;
				}
				return false;
			}
		});
		popupMenu.show();
	}
}
