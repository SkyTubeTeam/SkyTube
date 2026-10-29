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

package free.rm.skytube.businessobjects.YouTube.POJOs;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.view.Menu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.Thumbnail;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoSnippet;
import com.google.api.services.youtube.model.VideoStatistics;

import org.ocpsoft.prettytime.PrettyTime;
import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.VideoStream;

import java.io.File;
import java.io.Serializable;
import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.UUID;

import free.rm.skytube.BuildConfig;
import free.rm.skytube.R;
import free.rm.skytube.app.Settings;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.app.StreamSelectionPolicy;
import free.rm.skytube.businessobjects.FileDownloader;
import free.rm.skytube.businessobjects.Logger;
import free.rm.skytube.businessobjects.YouTube.YouTubeTasks;
import free.rm.skytube.businessobjects.YouTube.newpipe.ChannelId;
import free.rm.skytube.businessobjects.YouTube.newpipe.NewPipeUtils;
import free.rm.skytube.businessobjects.YouTube.newpipe.VideoId;
import free.rm.skytube.businessobjects.db.BookmarksDb;
import free.rm.skytube.businessobjects.db.DatabaseResult;
import free.rm.skytube.businessobjects.db.DownloadedVideosDb;
import free.rm.skytube.businessobjects.interfaces.GetDesiredStreamListener;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

import static free.rm.skytube.app.SkyTubeApp.getContext;
import static free.rm.skytube.app.SkyTubeApp.getStr;

/**
 * Represents a YouTube video.
 */
public class YouTubeVideo extends CardData implements Serializable {

	/**
	 * Channel (only id and name are set).
	 */
	private YouTubeChannel channel;

	/**
	 * The total number of 'likes'.
	 */
	private Long likeCountNumber;


	/**
	 * The total number of 'dislikes'.
	 */
	private Long dislikeCountNumber;

	/**
	 * The percentage of people that thumbs-up this video.
	 */
	private int thumbsUpPercentage;
	/**
	 * Video duration string (e.g. "5:15").
	 */
	private String duration;
	/**
	 *  Video duration in seconds
	 */
	private int durationInSeconds = -1;
	/**
	 * Total views count.
	 */
	private Long viewsCountInt;
	/**
	 * The date/time of when this video was published.
	 */
	private transient ZonedDateTime publishDate;
	/**
	 * Thumbnail URL (maximum resolution).
	 */
	private String thumbnailMaxResUrl;
	/**
	 * The language of this video.  (This tends to be ISO 639-1).
	 */
	private String language;
	/**
	 * Set to true if the video is a current live stream.
	 */
	private boolean isLiveStream;


	private Integer categoryId;

	/**
	 * Constructor.
	 */
	public YouTubeVideo(Video video) throws IllegalArgumentException {
		this.id = video.getId();

		VideoSnippet snippet = video.getSnippet();
		if (snippet == null) {
			throw new IllegalArgumentException("Missing snippet in "+video);
		}
		this.title = snippet.getTitle();

		this.channel = new YouTubeChannel(snippet.getChannelId(), snippet.getChannelTitle());
		setPublishTimestamp(snippet.getPublishedAt().getValue());
		setPublishTimestampExact(true);

		if (snippet.getThumbnails() != null) {
			Thumbnail thumbnail = snippet.getThumbnails().getHigh();
			if (thumbnail != null) {
				this.thumbnailUrl = thumbnail.getUrl();
			}

			thumbnail = snippet.getThumbnails().getMaxres();
			if (thumbnail != null) {
				this.thumbnailMaxResUrl = thumbnail.getUrl();
			}
		}

		this.language = snippet.getDefaultAudioLanguage() != null ? snippet.getDefaultAudioLanguage()
				: (snippet.getDefaultLanguage());

		this.description = snippet.getDescription();

		if (video.getContentDetails() != null) {
			setDuration(video.getContentDetails().getDuration());
			setIsLiveStream();
			setDurationInSeconds(video.getContentDetails().getDuration());
		}

		VideoStatistics statistics = video.getStatistics();
		if (statistics != null) {
			setLikeDislikeCount(statistics.getLikeCount() != null ? statistics.getLikeCount().longValue() : null, statistics.getDislikeCount() != null ? statistics.getDislikeCount().longValue() : null);

			setViewCount(statistics.getViewCount().longValue());
		}
	}

	public void setCategoryId(Integer categoryId) {
		this.categoryId = categoryId;
	}

	public Integer getCategoryId() {
		return categoryId;
	}

	public void setViewCount(Long viewsCountInt) {
		this.viewsCountInt = viewsCountInt;
	}

	public void setThumbnailMaxResUrl(String thumbnailMaxResUrl) {
		this.thumbnailMaxResUrl = thumbnailMaxResUrl;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public void setLiveStream(boolean liveStream) {
		this.isLiveStream = liveStream;
	}

	public void setThumbsUpPercentage(int thumbsUpPercentage) {
		this.thumbsUpPercentage = thumbsUpPercentage;
	}

        public YouTubeVideo() {
            // no-arg constructor for manual (de)serialization
        }

        public YouTubeVideo(String id, String title, String description, long durationInSeconds,
							YouTubeChannel channel, long viewCount, Instant publishDate,
							boolean publishDateExact, String thumbnailUrl) {
            this.id = id;
            this.title = title;
            this.description = description;
            setDurationInSeconds((int) durationInSeconds);
            if (viewCount >= 0) {
                setViewCount(viewCount);
            }
            if (publishDate != null) {
                setPublishTimestamp(publishDate.toEpochMilli());
            }
            setPublishTimestampExact(publishDateExact);
            this.thumbnailMaxResUrl = thumbnailUrl;
            this.thumbnailUrl = thumbnailUrl;
            this.channel = channel;
            this.thumbsUpPercentage = -1;
        }


        public VideoId getVideoId() {
            // TODO: this should be created by the NewPipe backend
            return new VideoId(id, getVideoUrl(), null);
        }

	/**
	 * Sets the {@link #thumbsUpPercentage}, i.e. the percentage of people that thumbs-up this video
	 * (format:  "<percentage>%").
	 *
	 * @param likedCountInt	Total number of "likes".
	 * @param dislikedCountInt Total number of "dislikes".
	 */
	public void setLikeDislikeCount(Long likedCountInt, Long dislikedCountInt) {
		this.thumbsUpPercentage = -1;

		// Logger.d(this, "setLikeDislikeCount "+likedCountInt+" "+dislikedCountInt +" -> "+likeCountNumber+" "+dislikeCountNumber);
		final Long likes = filterNegative(likedCountInt);
		final Long dislikes = filterNegative(dislikedCountInt);

		// some videos do not allow users to like/dislike them:  hence likedCountInt / dislikedCountInt
		// might be null in those cases
		if (likes != null && dislikes != null) {

			long likedCount = likes;
			long dislikedCount = dislikes;
			long totalVoteCount = likedCount + dislikedCount;	// liked and disliked counts

			if (totalVoteCount != 0) {
				this.thumbsUpPercentage = (int) Math.round((double)likedCount*100/totalVoteCount);

			}
		}
		this.likeCountNumber = likes;
		this.dislikeCountNumber = dislikes;
	}

	private static Long filterNegative(Long value) {
		if (value != null && value.longValue() < 0) {
			return null;
		}
		return value;
	}

	/**
	 * Using {@link #duration} it detects if the video/stream is live or not.
	 * <p>
	 * <p>If it is live, then it will change {@link #duration} to "LIVE" and modify {@link #publishDate}
	 * to current time (which will appear as "moments ago" when using {@link PrettyTime}).</p>
	 */
	private void setIsLiveStream() {
		// is live stream?
		if (duration.equals("0:00")) {
			isLiveStream = true;
			duration = getStr(R.string.LIVE);
			// set publishDate to current (as there is a bug in YouTube API in which live videos's date is incorrect)
			setPublishTimestamp(Instant.now().toEpochMilli());
		} else {
			isLiveStream = false;
		}
	}

	public YouTubeChannel getChannel() {
		return channel;
	}

	public void setChannel(YouTubeChannel channel) {
		this.channel = channel;
	}

    public String getSafeChannelId() {
        return channel != null ? channel.getChannelId().getRawId() : null;
    }

    public String getSafeChannelName() {
        return channel != null ? channel.getTitle() : null;
    }

	public ChannelId getChannelId() {
		return channel.getChannelId();
	}

	public String getChannelName() {
		return channel.getTitle();
	}

	/**
	 * @return True if the video allows the users to like/dislike it.
	 */
	public boolean isThumbsUpPercentageSet() {
		return (thumbsUpPercentage >= 0);
	}

	/**
	 * @return The thumbs up percentage (as an integer).  Can return <b>-1</b> if the video does not
	 * allow the users to like/dislike it.  Refer to {@link #isThumbsUpPercentageSet}.
	 */
	public int getThumbsUpPercentage() {
		return thumbsUpPercentage;
	}

	/**
	 * @return The thumbs up percentage (format:  "«percentage»%").  Can return <b>null</b> if the
	 * video does not allow the users to like/dislike it.  Refer to {@link #isThumbsUpPercentageSet}.
	 */
	public String getThumbsUpPercentageStr() {
		// round the liked percentage to 0 decimal places and convert it to string
		return thumbsUpPercentage >= 0 ? thumbsUpPercentage + "%" : null;
	}

	/**
	 * @return The total number of 'likes'.  Can return <b>null</b> if the video does not allow the
	 * users to like/dislike it.  Refer to {@link #isThumbsUpPercentageSet}.
	 */
	public String getLikeCount() {
		if (likeCountNumber != null) {
			return String.format(Locale.getDefault(), "%,d", likeCountNumber);
		}
		return null;
	}

	/**
	 * @return The total number of 'likes'.  Can return <b>null</b> for videos serialized with only a 'string' like count.
	 */
	public Long getLikeCountNumber() {
		return likeCountNumber;
	}

	/**
	 * @return The total number of 'dislikes'.  Can return <b>null</b> if the video does not allow the
	 * users to like/dislike it.  Refer to {@link #isThumbsUpPercentageSet}.
	 */
	public String getDislikeCount() {
		if (dislikeCountNumber != null) {
			return String.format(Locale.getDefault(), "%,d", dislikeCountNumber);
		}
		return null;
	}

	/**
	 * @return The total number of 'dislikes'.  Can return <b>null</b> for videos serialized with only a 'string' like count.
	 */
	public Long getDislikeCountNumber() {
		return dislikeCountNumber;
	}

	public String getDuration() {
		return duration;
	}

	public int getDurationInSeconds() {
		return durationInSeconds;
	}

	/**
	 * Sets the {@link #duration} by converts ISO 8601 duration to human readable string.
	 *
	 * @param duration ISO 8601 duration.
	 */
	private void setDuration(String duration) {
		this.duration = VideoDuration.toHumanReadableString(duration);
	}

	public String getViewsCount() {
		return viewsCountInt != null ? String.format(getStr(R.string.views), viewsCountInt) : null;
	}

	public Long getViewsCountInt() {
		return viewsCountInt;
	}

	/*
	 * Sets the {@link #durationInSeconds}
	 * @param durationInSeconds The duration in seconds.
	 */
	public void setDurationInSeconds(String durationInSeconds) {
		this.durationInSeconds = (int) Duration.parse(durationInSeconds).toMillis() / 1000;
	}

	public void setDurationInSeconds(int durationInSeconds) {
		this.durationInSeconds = durationInSeconds;
		this.duration = VideoDuration.toHumanReadableString(durationInSeconds);
	}

	/**
	 * Update the {@link #publishTimestamp} from {@link #publishDate} if the former is not set, just the later.
	 * Useful when deserialized from json.
	 * @return self.
	 */
	public YouTubeVideo updatePublishTimestampFromDate() {
		if (this.publishTimestamp == null) {
			if (this.publishDate != null) {
				setPublishTimestamp(this.publishDate.toInstant().toEpochMilli());
			}
		}
		return this;
	}

	public String getThumbnailMaxResUrl() {
		return thumbnailMaxResUrl;
	}

	public String getVideoUrl() {
		return String.format("https://youtu.be/%s", id);
	}

	public String getLanguage() {
		return language;
	}

	public boolean isLiveStream() {
		return isLiveStream;
	}

    public Single<Boolean> bookmarkVideo(Context context) {
        return bookmarkVideo(context, null);
    }

    public Single<Boolean> bookmarkVideo(Context context, Menu menu) {
        return BookmarksDb.getBookmarksDb().bookmarkAsync(this)
                .map(result -> {
                    Toast.makeText(context,
                            getBookmarkMessage(result),
                            Toast.LENGTH_LONG).show();

                    if (result.isPositive() && menu != null) {
                        menu.findItem(R.id.bookmark_video).setVisible(false);
                        menu.findItem(R.id.unbookmark_video).setVisible(true);
                    }
                    return result.isPositive();
        });
    }

	static int getBookmarkMessage(@NonNull DatabaseResult result) {
		switch (result) {
			case ERROR: return R.string.video_bookmarked_error;
			case NOT_MODIFIED: return R.string.video_already_bookmarked;
			case SUCCESS: return R.string.video_bookmarked;
		}
		throw new IllegalStateException("Result "+ result);
	}

	static int getUnBookmarkMessage(@NonNull DatabaseResult result) {
		switch (result) {
			case ERROR: return R.string.video_unbookmarked_error;
			case NOT_MODIFIED: return R.string.video_was_not_bookmarked;
			case SUCCESS: return R.string.video_unbookmarked;
		}
		throw new IllegalStateException("Result "+ result);
	}

    public Single<Boolean> unbookmarkVideo(Context context, Menu menu) {
        return BookmarksDb.getBookmarksDb().unbookmarkAsync(getVideoId())
                .map(result -> {
                    Toast.makeText(context,
                            getUnBookmarkMessage(result),
                            Toast.LENGTH_LONG).show();

                    if (result.isPositive() && menu != null) {
                        menu.findItem(R.id.bookmark_video).setVisible(true);
                        menu.findItem(R.id.unbookmark_video).setVisible(false);
                    }
                    return result.isPositive();
                });
    }

	public void shareVideo(Context context) {
		SkyTubeApp.shareUrl(context, getVideoUrl());
	}

	public void copyUrl(Context context) {
		SkyTubeApp.copyUrl(context, "Video URL", getVideoUrl());
	}

	public void updateFromStreamInfo(StreamInfo streamInfo) {
		final Long like = filterNegative(streamInfo.getLikeCount());
		final Long dislike = filterNegative(streamInfo.getDislikeCount());
		this.setLikeDislikeCount(like != null ? like : likeCountNumber, dislike != null ? dislike : dislikeCountNumber);

		final Long views = filterNegative(streamInfo.getViewCount());
		if (views != null) {
			this.setViewCount(views);
		}
		this.setDescription(NewPipeUtils.filterHtml(streamInfo.getDescription()));
	}

	/**
	 * Downloads this video.
	 *
	 * @return The download task.
	 * @param context Context
	 */
	public Completable downloadVideo(final Context context) {
		return DownloadedVideosDb.getVideoDownloadsDb().isVideoDownloaded(YouTubeVideo.this.getVideoId())
				.flatMapCompletable((downloadedVideo) -> {
			if (downloadedVideo) {
				return Completable.complete();
			} else {
				return YouTubeTasks.getDesiredStream(this, new GetDesiredStreamListener() {
					@Override
					public void onGetDesiredStream(StreamInfo streamInfo, YouTubeVideo video) {

						final Settings settings = SkyTubeApp.getSettings();
						StreamSelectionPolicy selectionPolicy = settings.getDesiredVideoResolution(true);
						// Prefer a muxed (progressive) stream, so the download is a single file;
						// fall back to a video-only stream with a separate audio file.
						StreamSelectionPolicy.StreamSelection streamSelection =
								selectionPolicy.withAllowVideoOnly(false).select(streamInfo);
						if (streamSelection == null) {
							// No muxed stream is available: download the video-only and the audio
							// streams separately, downloading works even if the preference is off.
							streamSelection = selectionPolicy.withAllowVideoOnly(true).select(streamInfo);
						}
						if (streamSelection != null) {
							// download the video (and the audio separately when it is not muxed)
							downloadTheVideo(streamSelection, settings);
						} else {
							Toast.makeText(context, selectionPolicy.getErrorMessage(context), Toast.LENGTH_LONG).show();
						}
					}

					private void downloadTheVideo(StreamSelectionPolicy.StreamSelection streamSelection, Settings settings) {
						final VideoStream videoStream = streamSelection.getVideoStream();
						final AudioStream audioStream = streamSelection.getAudioStream();
						final String audioUrl = audioStream != null && audioStream.isUrl() ? audioStream.getUrl() : null;
						final String audioExtension = audioStream != null ? audioStream.getFormat().suffix : null;
						final String notificationGroupId = UUID.randomUUID().toString();
						final int notificationGroupSize = audioUrl != null ? 2 : 1;
						final VideoDownloader videoDownloader =
								new VideoDownloader(audioUrl, audioExtension, notificationGroupId, notificationGroupSize);
						videoDownloader
								.setRemoteFileUrl(videoStream.getUrl())
								.setDirType(Environment.DIRECTORY_MOVIES)
								.setNotificationGroup(notificationGroupId, getTitle(), notificationGroupSize)
								.setTitle(getTitle())
								.setDescription(getStr(R.string.video) + " ― " + getChannelName())
								.setOutputFileName(getId() + " - " + getTitle())
								.setOutputDirectoryName(getChannelName())
								.setParentDirectory(settings.getDownloadParentFolder())
								.setOutputFileExtension(videoStream.getFormat().suffix)
								.setAllowedOverRoaming(false)
								.setAllowedNetworkTypesFlags(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE)
								.displayPermissionsActivity(context);
					}

					@Override
					public void onGetDesiredStreamError(Throwable throwable) {
						Logger.e(YouTubeVideo.this, "Stream error: " + throwable.getMessage(), throwable);
						Context context = getContext();
						final String reason = NewPipeUtils.getUserFriendlyErrorMessage(context, throwable);
						final String message = reason != null
								? context.getString(R.string.download_failed_because, getTitle(), reason)
								: context.getString(R.string.video_download_stream_error, getTitle());
						Toast.makeText(context, message, Toast.LENGTH_LONG).show();
					}
				});
			}
		});
	}

	/**
	 * Play the video using an external app
	 */
	public Single<DownloadedVideosDb.Status> playVideoExternally(Context context) {
		return DownloadedVideosDb.getVideoDownloadsDb().getDownloadedFileStatus(context, getVideoId()).map(fileStatus -> {
			if (fileStatus.getLocalVideoFile() != null) {
				Uri uri;
				File file = fileStatus.getLocalVideoFile();
				try {
					uri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", file);
				} catch (Exception e) {
					Logger.e(YouTubeVideo.this, "Error accessing path: " + file + ", message:" + e.getMessage(), e);
					uri = fileStatus.getUri();
				}
				Intent intent = new Intent(Intent.ACTION_VIEW, uri);
				intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
				context.startActivity(intent);
			} else {
				Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getVideoUrl()));
				context.startActivity(browserIntent);
			}
			return fileStatus;
		});
	}

	////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Downloads the video file, and - when the video stream is not muxed - the audio file as well.
	 */
	private class VideoDownloader extends FileDownloader {
		/** The URL of the audio stream to download separately, or null when the video is muxed. */
		private final String audioUrl;
		/** The file extension of the audio stream, or null when the video is muxed. */
		private final String audioExtension;
		/** Identifier shared by the video and audio download missions. */
		private final String notificationGroupId;
		/** Number of files that make up this download. */
		private final int notificationGroupSize;
		/** The downloader of the audio stream, started in parallel, or null when the video is muxed. */
		private AudioDownloader audioDownloader;
		/** True once the pair could not be completed and the files have been removed. */
		private boolean pairFailed = false;
		/** True once the pair has been registered in the downloads database. */
		private boolean pairSaved = false;
		/**
		 * True once the video file has been downloaded.  Completion is tracked
		 * with flags (instead of probing the file system) because the
		 * completion callbacks run on the UI thread, where disk access is
		 * forbidden by StrictMode.
		 */
		private boolean videoCompleted = false;
		/** URI of the downloaded video file; only valid when {@link #videoCompleted} is true. */
		private transient Uri videoUri = null;
		/** True once the audio file has been downloaded. */
		private boolean audioCompleted = false;
		/** URI of the downloaded audio file; only valid when {@link #audioCompleted} is true. */
		private transient Uri audioUri = null;

		VideoDownloader(@Nullable String audioUrl,
						@Nullable String audioExtension,
						String notificationGroupId,
						int notificationGroupSize) {
			this.audioUrl = audioUrl;
			this.audioExtension = audioExtension;
			this.notificationGroupId = notificationGroupId;
			this.notificationGroupSize = notificationGroupSize;
		}

		@Override
		public void onFileDownloadStarted() {
			Toast.makeText(getContext(),
					String.format(getContext().getString(R.string.starting_video_download), getTitle()),
					Toast.LENGTH_LONG).show();
			if (audioUrl != null) {
				// the audio stream is downloaded in parallel with the video
				downloadAudio();
			}
		}

		@Override
		public void onFileDownloadCompleted(boolean success, Uri localFileUri) {
			if (!success || pairFailed) {
				failPair();
				return;
			}
			videoCompleted = true;
			videoUri = localFileUri;
			if (audioUrl == null) {
				// the video is muxed, the single file is done
				saveDownload(localFileUri, null);
				return;
			}

			// register the pair as soon as the audio file is there too,
			// otherwise (re)start the audio download
			registerPairIfBothCompleted();
			downloadAudio();
		}

		@Override
		public void onDownloadStartFailed(String downloadName, final RuntimeException runtimeException) {
			failPair();
			showDownloadStartFailed(downloadName, runtimeException);
		}

		@Override
		public void onExternalStorageNotAvailable() {
			showExternalStorageNotAvailable();
		}

		/**
		 * Downloads the audio stream in parallel with the video stream.
		 */
		private void downloadAudio() {
			if (pairFailed || audioDownloader != null || audioUrl == null) {
				return;
			}
			audioDownloader = new AudioDownloader(this);
			audioDownloader
					.setRemoteFileUrl(audioUrl)
					.setDirType(Environment.DIRECTORY_MOVIES)
					.setNotificationGroup(notificationGroupId, notificationGroupSize)
					.setTitle(getTitle())
					.setDescription(getStr(R.string.audio) + " ― " + getChannelName())
					.setOutputFileName(getId() + " - " + getTitle() + " - audio")
					.setOutputDirectoryName(getChannelName())
					.setParentDirectory(SkyTubeApp.getSettings().getDownloadParentFolder())
					.setOutputFileExtension(audioExtension)
					.setAllowedOverRoaming(false)
					.setAllowedNetworkTypesFlags(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
			audioDownloader.onExternalStoragePermissionsGranted();
		}

		/**
		 * Called by the {@link AudioDownloader} when the audio file is on
		 * disk.
		 */
		private void notifyAudioCompleted(Uri uri) {
			this.audioCompleted = true;
			this.audioUri = uri;
			registerPairIfBothCompleted();
		}

		/**
		 * Registers the pair once both files have been downloaded.  No disk
		 * access is involved: the completion of each file is tracked by the
		 * download callbacks.
		 */
		private void registerPairIfBothCompleted() {
			if (videoCompleted && audioCompleted && videoUri != null && audioUri != null) {
				registerPair(videoUri, audioUri);
			}
		}

		/**
		 * Registers the pair (video and audio) in the downloads database.
		 * Called once both files are on disk.
		 */
		private void registerPair(Uri videoUri, Uri audioUri) {
			if (pairSaved || pairFailed) {
				return;
			}
			pairSaved = true;
			saveDownload(videoUri, audioUri);
		}

		/**
		 * A file of the pair could not be downloaded: a video file without
		 * its audio (and vice versa) is not usable, so remove both files and
		 * make sure the pair is neither registered nor completed.
		 */
		private void failPair() {
			if (pairFailed) {
				return;
			}
			pairFailed = true;
			deleteDownloadedFile();
			if (audioDownloader != null) {
				audioDownloader.deleteDownloadedFile();
			}
			showToast(false);
		}
	}

	/**
	 * Downloads the audio stream that belongs to a video-only download.
	 */
	private class AudioDownloader extends FileDownloader {
		/** The downloader of the related video file. */
		private final VideoDownloader videoDownloader;

		AudioDownloader(VideoDownloader videoDownloader) {
			this.videoDownloader = videoDownloader;
		}

		@Override
		public void onFileDownloadStarted() {
			// the video download already announced the start of the pair
		}

		@Override
		public void onFileDownloadCompleted(boolean success, Uri localFileUri) {
			if (!success) {
				videoDownloader.failPair();
				return;
			}

			// the audio file is on disk: register the pair as soon as the
			// video file is there too, otherwise the video downloader does it
			// when its file completes
			videoDownloader.notifyAudioCompleted(localFileUri);
		}

		@Override
		public void onDownloadStartFailed(String downloadName, final RuntimeException runtimeException) {
			videoDownloader.failPair();
		}

		@Override
		public void onExternalStorageNotAvailable() {
			showExternalStorageNotAvailable();
		}
	}

	private void saveDownload(Uri videoUri, @Nullable Uri audioUri) {
		DownloadedVideosDb.getVideoDownloadsDb().add(YouTubeVideo.this, videoUri, audioUri)
				.doOnSuccess(this::showToast)
				.doOnError(err -> SkyTubeApp.notifyUserOnError(getContext(), err))
				.subscribe();
	}

	void showToast(boolean success) {
		Toast.makeText(getContext(),
				String.format(getContext().getString(success ? R.string.video_downloaded : R.string.video_download_stream_error), getTitle()),
				Toast.LENGTH_LONG).show();
	}

	private void showDownloadStartFailed(String downloadName, RuntimeException runtimeException) {
		Toast.makeText(getContext(),
				String.format(getContext().getString(R.string.download_failed_because), downloadName, runtimeException.getMessage()),
				Toast.LENGTH_LONG).show();
	}

	private void showExternalStorageNotAvailable() {
		Toast.makeText(getContext(), R.string.external_storage_not_available, Toast.LENGTH_LONG).show();
	}

}
