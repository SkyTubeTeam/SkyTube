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
import androidx.core.content.FileProvider;

import com.google.api.client.util.DateTime;
import com.google.api.services.youtube.model.Thumbnail;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoSnippet;
import com.google.api.services.youtube.model.VideoStatistics;

import org.joda.time.Period;
import org.joda.time.format.ISOPeriodFormat;
import org.joda.time.format.PeriodFormatter;

import java.io.File;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.Date;
import java.util.Locale;

import free.rm.skytube.BuildConfig;
import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.FileDownloader;
import free.rm.skytube.businessobjects.Logger;
import free.rm.skytube.businessobjects.YouTube.Tasks.GetVideoDescriptionTask;
import free.rm.skytube.businessobjects.YouTube.Tasks.GetVideoStreamTask;
import free.rm.skytube.businessobjects.YouTube.VideoStream.StreamMetaData;
import free.rm.skytube.businessobjects.YouTube.newpipe.VideoId;
import free.rm.skytube.businessobjects.db.BookmarksDb;
import free.rm.skytube.businessobjects.db.DatabaseResult;
import free.rm.skytube.businessobjects.db.DownloadedVideosDb;
import free.rm.skytube.businessobjects.interfaces.GetDesiredStreamListener;

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
	 * Total views count.  This can be <b>null</b> if the video does not allow the user to
	 * like/dislike it.  Format:  "<number> Views"
	 */
	private String viewsCount;
	/**
	 * Total views count.
	 */
	private BigInteger viewsCountInt;
	/**
	 * The date/time of when this video was published.
	 */
	private DateTime publishDate;
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

	/**
	 * Timestamp of the data retrieval.
	 */
	private Long retrievalTimestamp;


	/**
	 * Constructor.
	 */
	public YouTubeVideo(Video video) {
		this.id = video.getId();

		VideoSnippet snippet = video.getSnippet();
		if (snippet != null) {
			this.title = snippet.getTitle();

			this.channel = new YouTubeChannel(snippet.getChannelId(), snippet.getChannelTitle());
			setPublishDate(snippet.getPublishedAt());

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
		}

		if (video.getContentDetails() != null) {
			setDuration(video.getContentDetails().getDuration());
			setIsLiveStream();
			setDurationInSeconds(video.getContentDetails().getDuration());
		}

		VideoStatistics statistics = video.getStatistics();
		if (statistics != null) {
			setLikeDislikeCount(statistics.getLikeCount() != null ? statistics.getLikeCount().longValue() : null, statistics.getDislikeCount() != null ? statistics.getDislikeCount().longValue() : null);

			setViewCount(statistics.getViewCount());
		}
	}

	private void setViewCount(BigInteger viewsCountInt) {
		this.viewsCountInt = viewsCountInt;
		this.viewsCount = String.format(getStr(R.string.views), viewsCountInt);
	}

        public YouTubeVideo(String id, String title, String description, long durationInSeconds, YouTubeChannel channel, long viewCount,
							Long publishDate, Boolean publishDateExact, String thumbnailUrl) {
            this.id = id;
            this.title = title;
            this.description = description;
            setDurationInSeconds((int) durationInSeconds);
            this.setViewCount(BigInteger.valueOf(viewCount));
            if (publishDate != null) {
                this.setPublishTimestamp(publishDate);
                this.publishDate = new DateTime(publishDate);
            }
            this.setPublishTimestampExact(publishDateExact);
            this.thumbnailMaxResUrl = thumbnailUrl;
            this.thumbnailUrl = thumbnailUrl;
            this.channel = channel;
            this.thumbsUpPercentage = -1;
        }


        public VideoId getVideoId() {
            // TODO: this should be created by the NewPipe backend
            return new VideoId(id, getVideoUrl());
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

		// some videos do not allow users to like/dislike them:  hence likedCountInt / dislikedCountInt
		// might be null in those cases
		if (likedCountInt != null && dislikedCountInt != null) {

			long likedCount = likedCountInt;
			long dislikedCount = dislikedCountInt;
			long totalVoteCount = likedCount + dislikedCount;	// liked and disliked counts

			if (totalVoteCount != 0) {
				this.thumbsUpPercentage = (int) Math.round((double)likedCount*100/totalVoteCount);

			}
		}
		this.likeCountNumber = likedCountInt;
		this.dislikeCountNumber = dislikedCountInt;
	}

	/**
	 * Using {@link #duration} it detects if the video/stream is live or not.
	 * <p>
	 * <p>If it is live, then it will change {@link #duration} to "LIVE" and modify {@link #publishDate}
	 * to current time (which will appear as "moments ago" when using {@link PrettyTimeEx}).</p>
	 */
	private void setIsLiveStream() {
		// is live stream?
		if (duration.equals("0:00")) {
			isLiveStream = true;
			duration = getStr(R.string.LIVE);
			setPublishDate(new DateTime(new Date()));	// set publishDate to current (as there is a bug in YouTube API in which live videos's date is incorrect)
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

	public String getChannelId() {
		return channel.getId();
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
		return viewsCount;
	}

	public BigInteger getViewsCountInt() {
		return viewsCountInt;
	}

	public DateTime getPublishDate() {
		return publishDate;
	}

	/*
	 * Sets the {@link #durationInSeconds}
	 * @param durationInSeconds The duration in seconds.
	 */
	public void setDurationInSeconds(String durationInSeconds) {
		PeriodFormatter formatter = ISOPeriodFormat.standard();
		Period p = formatter.parsePeriod(durationInSeconds);
		this.durationInSeconds = p.toStandardSeconds().getSeconds();
	}

	public void setDurationInSeconds(int durationInSeconds) {
		this.durationInSeconds = durationInSeconds;
		this.duration = VideoDuration.toHumanReadableString(durationInSeconds);
	}

	/**
	 * Sets the {@link #publishDate}, {@link #publishTimestamp}.
	 */
	private void setPublishDate(DateTime publishDate) {
		this.publishDate = publishDate;
		if (this.publishDate != null) {
			setPublishTimestamp(this.publishDate.getValue());
		}
	}

	/**
	 * Update the {@link #publishTimestamp} from {@link #publishDate} if the former is not set, just the later.
	 * Useful when deserialized from json.
	 * @return self.
	 */
	public YouTubeVideo updatePublishTimestampFromDate() {
		if (this.publishTimestamp == null) {
			if (this.publishDate != null) {
				setPublishTimestamp(this.publishDate.getValue());
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

	public boolean bookmarkVideo(Context context) {
		return bookmarkVideo(context, null);
	}

	public boolean bookmarkVideo(Context context, Menu menu) {
		DatabaseResult result = BookmarksDb.getBookmarksDb().add(this);
		Toast.makeText(context,
				getBookmarkMessage(result),
				Toast.LENGTH_LONG).show();

		if (result.isPositive() && menu != null) {
			menu.findItem(R.id.bookmark_video).setVisible(false);
			menu.findItem(R.id.unbookmark_video).setVisible(true);
		}
		return result.isPositive();
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

	public void unbookmarkVideo(Context context, Menu menu) {
		DatabaseResult result = BookmarksDb.getBookmarksDb().remove(getVideoId());
		Toast.makeText(context,
				getUnBookmarkMessage(result),
				Toast.LENGTH_LONG).show();

		if (result.isPositive() && menu != null) {
			menu.findItem(R.id.bookmark_video).setVisible(true);
			menu.findItem(R.id.unbookmark_video).setVisible(false);
		}
	}

	public void shareVideo(Context context) {
		SkyTubeApp.shareUrl(context, getVideoUrl());
	}

	public void copyUrl(Context context) {
		SkyTubeApp.copyUrl(context, "Video URL", getVideoUrl());
	}

	/**
	 * If the user have previously downloaded the video, this method will return the Uri of the file;
	 * else, get the stream for this video (based on the user's preference) by communicating with
	 * YouTube servers.  When done, the stream Uri will be returned via the passed
	 * {@link GetDesiredStreamListener}.
	 *
	 * @param listener Instance of {@link GetDesiredStreamListener} to pass the stream through.
	 */
	public void getDesiredStream(GetDesiredStreamListener listener, boolean forDownload) {
		new GetVideoStreamTask(this, listener, forDownload).executeInParallel();
	}

	public void getDesiredStream(GetDesiredStreamListener listener) {
		getDesiredStream(listener, false);
	}

	public Long getRetrievalTimestamp() {
		return retrievalTimestamp;
	}

	public void setRetrievalTimestamp(Long retrievalTimestamp) {
		this.retrievalTimestamp = retrievalTimestamp;
	}

	/**
	 * Remove local copy of this video, and delete it from the VideoDownloads DB.
	 */
	public void removeDownload() {
		Uri uri = DownloadedVideosDb.getVideoDownloadsDb().getVideoFileUri(YouTubeVideo.this);
		File file = new File(uri.getPath());
		if (file.exists()) {
			file.delete();
		}
		DownloadedVideosDb.getVideoDownloadsDb().remove(YouTubeVideo.this);
	}


	/**
	 * Get the Uri for the local copy of this Video.
	 *
	 * @return Uri
	 */
	public Uri getFileUri() {
		return DownloadedVideosDb.getVideoDownloadsDb().getVideoFileUri(this);
	}


	/**
	 * Returns whether or not this video has been downloaded.
	 *
	 * @return  True if the video was previously saved by the user.
	 */
	public boolean isDownloaded() {
		return DownloadedVideosDb.getVideoDownloadsDb().isVideoDownloaded(YouTubeVideo.this);
	}


	/**
	 * Downloads this video.
	 *
	 * @param context Context
	 */
	public void downloadVideo(final Context context) {
		if (isDownloaded())
			return;

		// if description is not yet downloaded, get it, and call the download action again.
		if (description == null) {
			new GetVideoDescriptionTask(this, description1 -> downloadVideo(context)).executeInParallel();
		} else {
			getDesiredStream(new GetDesiredStreamListener() {
				@Override
				public void onGetDesiredStream(StreamMetaData desiredStream) {
					// download the video
					new VideoDownloader()
							.setRemoteFileUrl(desiredStream.getUri().toString())
							.setDirType(Environment.DIRECTORY_MOVIES)
							.setTitle(getTitle())
							.setDescription(getStr(R.string.video) + " ― " + getChannelName())
							.setOutputFileName(getId() + " - " + getTitle())
							.setOutputDirectoryName(getChannelName())
							.setParentDirectory(SkyTubeApp.getPreferenceManager().getString(SkyTubeApp.getStr(R.string.pref_key_video_download_folder), null))
							.setOutputFileExtension("mp4")
							.setAllowedOverRoaming(false)
							.setAllowedNetworkTypesFlags(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE)
							.displayPermissionsActivity(context);
				}

				@Override
				public void onGetDesiredStreamError(String errorMessage) {
					Logger.e(YouTubeVideo.this, "Stream error: %s", errorMessage);
					Toast.makeText(getContext(),
							String.format(getContext().getString(R.string.video_download_stream_error), getTitle()),
							Toast.LENGTH_LONG).show();
				}
			}, true);
		}
	}

	/**
	 * Play the video using an external app
	 */
	public void playVideoExternally(Context context) {
		Uri fileUri = getFileUri();
		if (fileUri != null) {
			File file = new File(fileUri.getPath());
			Uri uri;
			try {
				uri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", file);
			} catch (Exception e) {
				Logger.e(YouTubeVideo.this, "Error accessing path: " + file +", message:"+ e.getMessage(), e);
				uri = fileUri;
			}
			Intent intent = new Intent(Intent.ACTION_VIEW, uri);
			intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
			context.startActivity(intent);
		} else {
			Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getVideoUrl()));
			context.startActivity(browserIntent);
		}
	}

	////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Downloads a YouTube video.
	 */
	private class VideoDownloader extends FileDownloader implements Serializable {

		@Override
		public void onFileDownloadStarted() {
			Toast.makeText(getContext(),
					String.format(getContext().getString(R.string.starting_video_download), getTitle()),
					Toast.LENGTH_LONG).show();
		}

		@Override
		public void onFileDownloadCompleted(boolean success, Uri localFileUri) {
			if (success) {
				success = DownloadedVideosDb.getVideoDownloadsDb().add(YouTubeVideo.this, localFileUri.toString());
			}

			Toast.makeText(getContext(),
					String.format(getContext().getString(success ? R.string.video_downloaded : R.string.video_download_stream_error), getTitle()),
					Toast.LENGTH_LONG).show();
		}

		@Override
		public void onExternalStorageNotAvailable() {
			Toast.makeText(getContext(),
					R.string.external_storage_not_available,
					Toast.LENGTH_LONG).show();
		}

	}

}
