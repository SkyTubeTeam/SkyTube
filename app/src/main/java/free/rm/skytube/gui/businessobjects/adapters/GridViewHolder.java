/*
 * SkyTube
 * Copyright (C) 2018  Ramon Mifsud
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

package free.rm.skytube.gui.businessobjects.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.Logger;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;
import free.rm.skytube.gui.activities.ThumbnailViewerActivity;
import free.rm.skytube.businessobjects.db.DownloadedVideosDb;
import free.rm.skytube.businessobjects.db.Tasks.IsVideoBookmarkedTask;
import free.rm.skytube.gui.businessobjects.MainActivityListener;
import free.rm.skytube.gui.businessobjects.YouTubePlayer;

/**
 * A ViewHolder for the videos grid view.
 */
class GridViewHolder extends RecyclerView.ViewHolder {
	/** YouTube video */
	private YouTubeVideo            youTubeVideo = null;
	private Context                 context = null;
	private MainActivityListener    mainActivityListener;
	private boolean                 showChannelInfo;

	private TextView titleTextView;
	private TextView channelTextView;
	private TextView thumbsUpPercentageTextView;
	private TextView videoDurationTextView;
	private TextView publishDateTextView;
	private ImageView thumbnailImageView;
	private TextView viewsTextView;


	/**
	 * Constructor.
	 *
	 * @param view              Cell view (parent).
	 * @param listener          MainActivity listener.
	 * @param showChannelInfo   True to display channel information (e.g. channel name) and allows
	 *                          user to open and browse the channel; false to hide such information.
	 */
	GridViewHolder(View view, MainActivityListener listener, boolean showChannelInfo) {
		super(view);

		titleTextView = view.findViewById(R.id.title_text_view);
		channelTextView = view.findViewById(R.id.channel_text_view);
		thumbsUpPercentageTextView = view.findViewById(R.id.thumbs_up_text_view);
		videoDurationTextView = view.findViewById(R.id.video_duration_text_view);
		publishDateTextView = view.findViewById(R.id.publish_date_text_view);
		thumbnailImageView = view.findViewById(R.id.thumbnail_image_view);
		viewsTextView = view.findViewById(R.id.views_text_view);

		this.mainActivityListener = listener;
		this.showChannelInfo = showChannelInfo;

		thumbnailImageView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View thumbnailView) {
				if (youTubeVideo != null) {
					YouTubePlayer.launch(youTubeVideo, context);
				}
			}
		});

		View.OnClickListener channelOnClickListener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(mainActivityListener != null)
					mainActivityListener.onChannelClick(youTubeVideo.getChannelId());
			}
		};

		view.findViewById(R.id.channel_layout).setOnClickListener(showChannelInfo ? channelOnClickListener : null);

		view.findViewById(R.id.options_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onOptionsButtonClick(v);
			}
		});
	}



	/**
	 * Updates the contents of this ViewHold such that the data of these views is equal to the
	 * given youTubeVideo.
	 *
	 * @param youTubeVideo		{@link YouTubeVideo} instance.
	 */
	void updateInfo(YouTubeVideo youTubeVideo, Context context, MainActivityListener listener) {
		this.youTubeVideo = youTubeVideo;
		this.context = context;
		this.mainActivityListener = listener;
		updateViewsData(this.youTubeVideo);
	}


	/**
	 * This method will update the {@link View}s of this object reflecting the supplied video.
	 *
	 * @param video		{@link YouTubeVideo} instance.
	 */
	private void updateViewsData(YouTubeVideo video) {
		titleTextView.setText(video.getTitle());
		channelTextView.setText(showChannelInfo ? video.getChannelName() : "");
		publishDateTextView.setText(video.getPublishDatePretty());
		videoDurationTextView.setText(video.getDuration());
		viewsTextView.setText(video.getViewsCount());
		Glide.with(context)
				.load(video.getThumbnailUrl())
				.apply(new RequestOptions().placeholder(R.drawable.thumbnail_default))
				.into(thumbnailImageView);

		if (video.getThumbsUpPercentageStr() != null) {
			thumbsUpPercentageTextView.setVisibility(View.VISIBLE);
			thumbsUpPercentageTextView.setText(video.getThumbsUpPercentageStr());
		} else {
			thumbsUpPercentageTextView.setVisibility(View.INVISIBLE);
		}
	}



 	private void onOptionsButtonClick(final View view) {
		final PopupMenu popupMenu = new PopupMenu(view.getContext(), view);
		popupMenu.getMenuInflater().inflate(R.menu.video_options_menu, popupMenu.getMenu());
		Menu menu = popupMenu.getMenu();
		new IsVideoBookmarkedTask(youTubeVideo, menu).executeInParallel();


		if(youTubeVideo.isDownloaded()) {
			popupMenu.getMenu().findItem(R.id.delete_download).setVisible(true);
			popupMenu.getMenu().findItem(R.id.download_video).setVisible(false);
		} else {
			popupMenu.getMenu().findItem(R.id.delete_download).setVisible(false);
			boolean allowDownloadsOnMobile = SkyTubeApp.getPreferenceManager().getBoolean(SkyTubeApp.getStr(R.string.pref_key_allow_mobile_downloads), false);
			if(SkyTubeApp.isConnectedToWiFi() || (SkyTubeApp.isConnectedToMobile() && allowDownloadsOnMobile))
				popupMenu.getMenu().findItem(R.id.download_video).setVisible(true);
			else
				popupMenu.getMenu().findItem(R.id.download_video).setVisible(false);
		}
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
					case R.id.view_thumbnail:
						Intent i = new Intent(context, ThumbnailViewerActivity.class);
						i.putExtra(ThumbnailViewerActivity.YOUTUBE_VIDEO, youTubeVideo);
						context.startActivity(i);
						return true;
					case R.id.delete_download:
						youTubeVideo.removeDownload();
						return true;
					case R.id.download_video:
						youTubeVideo.downloadVideo(context);
						return true;
					case R.id.block_channel:
						youTubeVideo.blockChannel(context);
						Logger.e(this,"get blocked channel info" +youTubeVideo.getChannelName() +
								" -- "+ youTubeVideo.getChannelId() );
				}
				return false;
			}
		});
		popupMenu.show();
	}

}
