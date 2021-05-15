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

import androidx.annotation.MenuRes;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.io.Serializable;

import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.app.enums.Policy;
import free.rm.skytube.businessobjects.YouTube.POJOs.CardData;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubePlaylist;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;
import free.rm.skytube.businessobjects.db.DatabaseTasks;
import free.rm.skytube.businessobjects.db.DownloadedVideosDb;
import free.rm.skytube.businessobjects.db.PlaybackStatusDb;
import free.rm.skytube.businessobjects.db.SubscriptionsDb;
import free.rm.skytube.databinding.VideoCellBinding;
import free.rm.skytube.gui.activities.ThumbnailViewerActivity;
import free.rm.skytube.gui.businessobjects.MainActivityListener;
import free.rm.skytube.gui.businessobjects.MobileNetworkWarningDialog;
import free.rm.skytube.gui.businessobjects.YouTubePlayer;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

/**
 * A ViewHolder for the videos grid view.
 */
public class GridViewHolder extends RecyclerView.ViewHolder implements Serializable {
	/** YouTube video */
	private CardData currentCard = null;
	private Context context = null;
	private MainActivityListener mainActivityListener;
	private boolean showChannelInfo;

	private final transient VideoCellBinding binding;
	private final transient CompositeDisposable compositeDisposable;

	/**
	 * Constructor.
	 *
	 * @param binding           Cell binding (parent).
	 * @param listener          MainActivity listener.
	 * @param showChannelInfo   True to display channel information (e.g. channel name) and allows
	 *                          user to open and browse the channel; false to hide such information.
	 */
	GridViewHolder(@NonNull VideoCellBinding binding, MainActivityListener listener,
				   boolean showChannelInfo) {
		super(binding.getRoot());

		this.binding = binding;
		this.mainActivityListener = listener;
		this.showChannelInfo = showChannelInfo;
		compositeDisposable = new CompositeDisposable();

		binding.thumbnailImageView.setOnClickListener(thumbnailView -> {
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

		binding.channelLayout.setOnClickListener(showChannelInfo ? channelOnClickListener : null);
		binding.optionsButton.setOnClickListener(this::onOptionsButtonClick);
	}

	void clearBackgroundTasks() {
		compositeDisposable.clear();
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
		binding.titleTextView.setText(currentCard.getTitle());
		if (currentCard.getPublishTimestamp() != null) {
			binding.publishDateTextView.setText(currentCard.getPublishDatePretty());
		} else {
			binding.publishDateTextView.setVisibility(View.GONE);
			binding.separatorTextView.setVisibility(View.GONE);
		}
		Glide.with(context)
				.load(currentCard.getThumbnailUrl())
				.apply(new RequestOptions().placeholder(R.drawable.thumbnail_default))
				.into(binding.thumbnailImageView);

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
			binding.viewsTextView.setText(currentCard.getTotalSubscribers());
		} else {
			// the subscriber count is hidden/disabled
			binding.viewsTextView.setVisibility(View.GONE);
			binding.separatorTextView.setVisibility(View.GONE);
		}

		binding.thumbsUpTextView.setVisibility(View.GONE);
		binding.videoDurationTextView.setVisibility(View.GONE);
		binding.videoPositionProgressBar.setVisibility(View.GONE);
		binding.channelTextView.setVisibility(View.GONE);
	}

	private void updateViewsData(@NonNull YouTubePlaylist playlistInfoItem) {
		binding.viewsTextView.setText(String.format(context.getString(R.string.num_videos), playlistInfoItem.getVideoCount()));

		binding.thumbsUpTextView.setVisibility(View.GONE);
		binding.videoDurationTextView.setVisibility(View.GONE);
		binding.channelTextView.setVisibility(View.GONE);
		binding.optionsButton.setVisibility(View.GONE);

		binding.videoPositionProgressBar.setVisibility(View.GONE);
	}

    private void updateViewsData(@NonNull YouTubeVideo youTubeVideo) {
        binding.channelTextView.setText(showChannelInfo ? youTubeVideo.getChannelName() : "");
        binding.videoDurationTextView.setText(youTubeVideo.getDuration());
        binding.viewsTextView.setText(youTubeVideo.getViewsCount());

        if (youTubeVideo.getThumbsUpPercentageStr() != null) {
            binding.thumbsUpTextView.setVisibility(View.VISIBLE);
            binding.thumbsUpTextView.setText(youTubeVideo.getThumbsUpPercentageStr());
        } else {
            binding.thumbsUpTextView.setVisibility(View.INVISIBLE);
        }

        if(SkyTubeApp.getSettings().isPlaybackStatusEnabled()) {
            PlaybackStatusDb.getPlaybackStatusDb().getVideoWatchedStatusAsync(youTubeVideo.getId()).subscribe(videoWatchedStatus -> {
                if (videoWatchedStatus.isWatched()) {
                    binding.videoPositionProgressBar.setVisibility(View.VISIBLE);
                    binding.videoPositionProgressBar.setMax(youTubeVideo.getDurationInSeconds() * 1000);
                    if (videoWatchedStatus.isFullyWatched()) {
                        binding.videoPositionProgressBar.setProgress(youTubeVideo.getDurationInSeconds() * 1000);
                    } else {
                        binding.videoPositionProgressBar.setProgress((int) videoWatchedStatus.getPosition());
                    }
                } else {
                    binding.videoPositionProgressBar.setVisibility(View.INVISIBLE);
                }
            });
        } else {
            binding.videoPositionProgressBar.setVisibility(View.INVISIBLE);
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
		updateSubscribeMenuItem(channel.getId(), menu);
		popupMenu.setOnMenuItemClickListener(item -> {
			switch (item.getItemId()) {
				case R.id.share:
					SkyTubeApp.shareUrl(context, channel.getChannelUrl());
					return true;
				case R.id.copyurl:
					SkyTubeApp.copyUrl(context, "Channel URL", channel.getChannelUrl());
					return true;
				case R.id.subscribe_channel:
					compositeDisposable.add(YouTubeChannel.subscribeChannel(context, channel.getId()));
					return true;
				case R.id.open_channel:
					SkyTubeApp.launchChannel(channel.getId(), context);
					return true;
				case R.id.block_channel:
					compositeDisposable.add(channel.blockChannel().subscribe());
					return true;
			}
			return false;
		});
		popupMenu.show();
	}

	private void updateSubscribeMenuItem(String channelId, Menu menu) {
		compositeDisposable.add(SubscriptionsDb.getSubscriptionsDb().getUserSubscribedToChannel(channelId)
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe((subscribed) -> {
					if (!subscribed) {
						menu.findItem(R.id.subscribe_channel).setVisible(true);
					}
				}));
	}

	private void onOptionsButtonClick(final View view, YouTubeVideo youTubeVideo) {
		final PopupMenu popupMenu = createPopup(R.menu.video_options_menu, view);
		final Menu menu = popupMenu.getMenu();
		compositeDisposable.add(DatabaseTasks.isVideoBookmarked(youTubeVideo.getId(), menu));

		// If playback history is not disabled, see if this video has been watched. Otherwise, hide the "mark watched" & "mark unwatched" options from the menu.
		if(!SkyTubeApp.getPreferenceManager().getBoolean(context.getString(R.string.pref_key_disable_playback_status), false)) {
			compositeDisposable.add(DatabaseTasks.isVideoWatched(youTubeVideo.getId(), menu));
		} else {
			menu.findItem(R.id.mark_watched).setVisible(false);
			menu.findItem(R.id.mark_unwatched).setVisible(false);
		}

		boolean online = SkyTubeApp.isConnected(view.getContext());

		menu.findItem(R.id.download_video).setVisible(false);
		menu.findItem(R.id.delete_download).setVisible(false);

		compositeDisposable.add(DownloadedVideosDb.getVideoDownloadsDb().isVideoDownloaded(youTubeVideo).subscribe(isDownloaded -> {
			if(isDownloaded) {
				menu.findItem(R.id.delete_download).setVisible(true);
			} else {
				menu.findItem(R.id.download_video).setVisible(online);
			}
		}));
		if(SkyTubeApp.getPreferenceManager().getBoolean(context.getString(R.string.pref_key_enable_video_blocker), true)) {
			menu.findItem(R.id.block_channel).setVisible(true);
		} else {
			menu.findItem(R.id.block_channel).setVisible(false);
		}
		popupMenu.setOnMenuItemClickListener(item -> {
			switch(item.getItemId()) {
				case R.id.menu_open_video_with:
					compositeDisposable.add(youTubeVideo.playVideoExternally(context).subscribe());
					return true;
				case R.id.share:
					youTubeVideo.shareVideo(view.getContext());
					return true;
				case R.id.copyurl:
					youTubeVideo.copyUrl(context);
					return true;
				case R.id.mark_watched:
					compositeDisposable.add(
							PlaybackStatusDb.getPlaybackStatusDb().setVideoWatchedStatusInBackground(youTubeVideo, true)
									.subscribe((success) -> updateViewsData()));
					return true;
				case R.id.mark_unwatched:
					compositeDisposable.add(
							PlaybackStatusDb.getPlaybackStatusDb().setVideoWatchedStatusInBackground(youTubeVideo, false)
									.subscribe((success) -> updateViewsData()));
					return true;
				case R.id.bookmark_video:
					compositeDisposable.add(youTubeVideo.bookmarkVideo(context, menu).subscribe());
					return true;
				case R.id.unbookmark_video:
					compositeDisposable.add(youTubeVideo.unbookmarkVideo(context, menu).subscribe());
					return true;
				case R.id.view_thumbnail:
					Intent i = new Intent(context, ThumbnailViewerActivity.class);
					i.putExtra(ThumbnailViewerActivity.YOUTUBE_VIDEO, youTubeVideo);
					context.startActivity(i);
					return true;
				case R.id.delete_download:
					compositeDisposable.add(
							DownloadedVideosDb.getVideoDownloadsDb().removeDownload(context, youTubeVideo.getVideoId()).subscribe());
					return true;
				case R.id.download_video:
					final Policy decision = new MobileNetworkWarningDialog(view.getContext())
							.showDownloadWarning(youTubeVideo);

					if (decision == Policy.ALLOW) {
						youTubeVideo.downloadVideo(context).subscribe();
					}
					return true;
				case R.id.block_channel:
					compositeDisposable.add(youTubeVideo.getChannel().blockChannel().subscribe());
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
