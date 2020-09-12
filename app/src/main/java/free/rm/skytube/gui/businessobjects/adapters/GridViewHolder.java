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
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.MenuRes;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.io.Serializable;

import butterknife.BindView;
import butterknife.ButterKnife;
import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.app.enums.Policy;
import free.rm.skytube.businessobjects.YouTube.POJOs.CardData;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubePlaylist;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;
import free.rm.skytube.businessobjects.db.PlaybackStatusDb;
import free.rm.skytube.businessobjects.db.SubscriptionsDb;
import free.rm.skytube.businessobjects.db.Tasks.IsVideoBookmarkedTask;
import free.rm.skytube.businessobjects.db.Tasks.IsVideoWatchedTask;
import free.rm.skytube.gui.activities.ThumbnailViewerActivity;
import free.rm.skytube.gui.businessobjects.MainActivityListener;
import free.rm.skytube.gui.businessobjects.MobileNetworkWarningDialog;
import free.rm.skytube.gui.businessobjects.YouTubePlayer;

/**
 * A ViewHolder for the videos grid view.
 */
public class GridViewHolder extends RecyclerView.ViewHolder implements Serializable {
	/** YouTube video */
	private CardData currentCard = null;
	private Context                 context = null;
	private MainActivityListener    mainActivityListener;
	private boolean                 showChannelInfo;

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

	@BindView(R.id.video_position_progress_bar)
	ProgressBar videoPositionProgressBar;

	@BindView(R.id.options_button)
	View optionsButton;


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

		ButterKnife.bind(this, view);

		this.mainActivityListener = listener;
		this.showChannelInfo = showChannelInfo;

		thumbnailImageView.setOnClickListener(thumbnailView -> {
			if (currentCard instanceof YouTubeVideo) {
				YouTubePlayer.launch((YouTubeVideo) currentCard, context);
			} else if (currentCard instanceof YouTubePlaylist) {
				mainActivityListener.onPlaylistClick((YouTubePlaylist) currentCard);
			} else if (currentCard instanceof YouTubeChannel) {
				mainActivityListener.onChannelClick( ((YouTubeChannel) currentCard).getId());
			}
		});

		View.OnClickListener channelOnClickListener = v -> {
			if(mainActivityListener != null) {
				if (currentCard instanceof YouTubeVideo) {
					mainActivityListener.onChannelClick(((YouTubeVideo) currentCard).getChannelId());
				} else if (currentCard instanceof YouTubePlaylist) {
					mainActivityListener.onPlaylistClick((YouTubePlaylist) currentCard);
				}
			}
		};

		view.findViewById(R.id.channel_layout).setOnClickListener(showChannelInfo ? channelOnClickListener : null);

		optionsButton.setOnClickListener(this::onOptionsButtonClick);
	}

	/**
	 * Updates the contents of this ViewHolder such that the data of these views is equal to the
	 * given youTubeVideo.
	 *
	 * @param currentCard		{@link YouTubeVideo} or {@link YouTubePlaylist} instance.
	 */
	void updateInfo(@NonNull CardData currentCard, Context context, MainActivityListener listener) {
		this.currentCard = currentCard;
		this.context = context;
		this.mainActivityListener = listener;
		updateViewsData();
	}

	/**
	 * This method will update the {@link View}s of this object reflecting this GridView's video.
	 *
	 */
	public void updateViewsData() {
		titleTextView.setText(currentCard.getTitle());
		if (currentCard.getPublishTimestamp() != null) {
			publishDateTextView.setText(currentCard.getPublishDatePretty());
		} else {
			publishDateTextView.setVisibility(View.GONE);
		}
		Glide.with(context)
				.load(currentCard.getThumbnailUrl())
				.apply(new RequestOptions().placeholder(R.drawable.thumbnail_default))
				.into(thumbnailImageView);

		if (currentCard instanceof YouTubeVideo) {
			updateViewsData((YouTubeVideo) currentCard);
		} else if (currentCard instanceof YouTubePlaylist) {
			updateViewsData((YouTubePlaylist) currentCard);
		} else if (currentCard instanceof YouTubeChannel) {
			updateViewsData((YouTubeChannel) currentCard);
		}
	}

	private void updateViewsData(@NonNull YouTubeChannel currentCard) {
		if (currentCard.getSubscriberCount() >= 0) {
			viewsTextView.setText(currentCard.getTotalSubscribers());
		} else {
			// the subscriber count is hidden/disabled
			viewsTextView.setVisibility(View.GONE);
		}

		thumbsUpPercentageTextView.setVisibility(View.GONE);
		videoDurationTextView.setVisibility(View.GONE);
		videoPositionProgressBar.setVisibility(View.GONE);
		channelTextView.setVisibility(View.GONE);
	}

	private void updateViewsData(@NonNull YouTubePlaylist playlistInfoItem) {
		viewsTextView.setText(String.format(context.getString(R.string.num_videos), playlistInfoItem.getVideoCount()));

		thumbsUpPercentageTextView.setVisibility(View.GONE);
		videoDurationTextView.setVisibility(View.GONE);
		channelTextView.setVisibility(View.GONE);
		optionsButton.setVisibility(View.GONE);

		videoPositionProgressBar.setVisibility(View.GONE);
	}

	private void updateViewsData(@NonNull YouTubeVideo youTubeVideo) {
		channelTextView.setText(showChannelInfo ? youTubeVideo.getChannelName() : "");
		videoDurationTextView.setText(youTubeVideo.getDuration());
		viewsTextView.setText(youTubeVideo.getViewsCount());

		if (youTubeVideo.getThumbsUpPercentageStr() != null) {
			thumbsUpPercentageTextView.setVisibility(View.VISIBLE);
			thumbsUpPercentageTextView.setText(youTubeVideo.getThumbsUpPercentageStr());
		} else {
			thumbsUpPercentageTextView.setVisibility(View.INVISIBLE);
		}

		if(SkyTubeApp.getPreferenceManager().getBoolean(context.getString(R.string.pref_key_disable_playback_status), false)) {
			videoPositionProgressBar.setVisibility(View.INVISIBLE);
		} else {
			PlaybackStatusDb.VideoWatchedStatus videoWatchedStatus = PlaybackStatusDb.getPlaybackStatusDb().getVideoWatchedStatus(youTubeVideo.getId());
			if (videoWatchedStatus.isWatched()) {
				videoPositionProgressBar.setVisibility(View.VISIBLE);
				videoPositionProgressBar.setMax(youTubeVideo.getDurationInSeconds() * 1000);
				if (videoWatchedStatus.isFullyWatched()) {
					videoPositionProgressBar.setProgress(youTubeVideo.getDurationInSeconds() * 1000);
				} else {
					videoPositionProgressBar.setProgress((int) videoWatchedStatus.getPosition());
				}
			} else {
				videoPositionProgressBar.setVisibility(View.INVISIBLE);
			}
		}
	}



 	private void onOptionsButtonClick(final View view) {
		if (currentCard instanceof YouTubeVideo) {
			onOptionsButtonClick(view, (YouTubeVideo) currentCard);
		} else if (currentCard instanceof YouTubeChannel) {
			onOptionsButtonClick(view, (YouTubeChannel) currentCard);
		}
	}
	private void onOptionsButtonClick(final View view, YouTubeChannel channel) {
		final PopupMenu popupMenu = createPopup(R.menu.channel_options_menu, view);
		Menu menu = popupMenu.getMenu();
		if (!SubscriptionsDb.getSubscriptionsDb().isUserSubscribedToChannel(channel.getId())) {
			menu.findItem(R.id.subscribe_channel).setVisible(true);
		}
		popupMenu.setOnMenuItemClickListener(item -> {
			switch (item.getItemId()) {
				case R.id.share:
					SkyTubeApp.shareUrl(context, channel.getChannelUrl());
					return true;
				case R.id.copyurl:
					SkyTubeApp.copyUrl(context, "Channel URL", channel.getChannelUrl());
					return true;
				case R.id.subscribe_channel:
					YouTubeChannel.subscribeChannel(context, popupMenu.getMenu(), channel.getId());
					return true;
				case R.id.open_channel:
					SkyTubeApp.launchChannel(channel.getId(), context);
					return true;
				case R.id.block_channel:
					channel.blockChannel();
					return true;
			}
			return false;
		});
		popupMenu.show();
	}

	private void onOptionsButtonClick(final View view, YouTubeVideo youTubeVideo) {
		final PopupMenu popupMenu = createPopup(R.menu.video_options_menu, view);
		Menu menu = popupMenu.getMenu();
		new IsVideoBookmarkedTask(youTubeVideo.getId(), menu).executeInParallel();

		// If playback history is not disabled, see if this video has been watched. Otherwise, hide the "mark watched" & "mark unwatched" options from the menu.
		if(!SkyTubeApp.getPreferenceManager().getBoolean(context.getString(R.string.pref_key_disable_playback_status), false)) {
			new IsVideoWatchedTask(youTubeVideo.getId(), menu).executeInParallel();
		} else {
			popupMenu.getMenu().findItem(R.id.mark_watched).setVisible(false);
			popupMenu.getMenu().findItem(R.id.mark_unwatched).setVisible(false);
		}

		boolean online = SkyTubeApp.isConnected(view.getContext());

		if(youTubeVideo.isDownloaded()) {
			popupMenu.getMenu().findItem(R.id.delete_download).setVisible(true);
			popupMenu.getMenu().findItem(R.id.download_video).setVisible(false);
		} else {
			popupMenu.getMenu().findItem(R.id.delete_download).setVisible(false);
			popupMenu.getMenu().findItem(R.id.download_video).setVisible(online);
		}
		if(SkyTubeApp.getPreferenceManager().getBoolean(context.getString(R.string.pref_key_enable_video_blocker), true)) {
			popupMenu.getMenu().findItem(R.id.block_channel).setVisible(true);
		} else {
			popupMenu.getMenu().findItem(R.id.block_channel).setVisible(false);
		}
		popupMenu.setOnMenuItemClickListener(item -> {
			switch(item.getItemId()) {
				case R.id.menu_open_video_with:
					youTubeVideo.playVideoExternally(context);
					return true;
				case R.id.share:
					youTubeVideo.shareVideo(view.getContext());
					return true;
				case R.id.copyurl:
					youTubeVideo.copyUrl(context);
					return true;
				case R.id.mark_watched:
					PlaybackStatusDb.getPlaybackStatusDb().setVideoWatchedStatus(youTubeVideo, true);
					updateViewsData();
					return true;
				case R.id.mark_unwatched:
					PlaybackStatusDb.getPlaybackStatusDb().setVideoWatchedStatus(youTubeVideo, false);
					updateViewsData();
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
					final Policy decision = new MobileNetworkWarningDialog(view.getContext())
							.showDownloadWarning(youTubeVideo);

					if (decision == Policy.ALLOW) {
						youTubeVideo.downloadVideo(context);
					}
					return true;
				case R.id.block_channel:
					youTubeVideo.getChannel().blockChannel();
					return true;
			}
			return false;
		});
		popupMenu.show();
	}

	private PopupMenu createPopup(@MenuRes int menuId, View view) {
		final PopupMenu popupMenu = new PopupMenu(view.getContext(), view);
		popupMenu.getMenuInflater().inflate(menuId, popupMenu.getMenu());
		return popupMenu;
	}

}
