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
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import android.view.Menu;
import android.widget.Toast;

import com.google.api.client.util.DateTime;
import com.google.api.services.youtube.model.Thumbnail;
import com.google.api.services.youtube.model.Video;

import org.joda.time.Period;
import org.joda.time.format.ISOPeriodFormat;
import org.joda.time.format.PeriodFormatter;

import java.io.File;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import free.rm.skytube.BuildConfig;
import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.FileDownloader;
import free.rm.skytube.businessobjects.Logger;
import free.rm.skytube.businessobjects.YouTube.Tasks.GetVideoStreamTask;
import free.rm.skytube.businessobjects.YouTube.VideoStream.StreamMetaData;
import free.rm.skytube.businessobjects.db.BookmarksDb;
import free.rm.skytube.businessobjects.db.DownloadedVideosDb;
import free.rm.skytube.businessobjects.interfaces.GetDesiredStreamListener;

import static free.rm.skytube.app.SkyTubeApp.getContext;
import static free.rm.skytube.app.SkyTubeApp.getStr;

/**
 * Represents a YouTube video.
 */
public class YouTubeVideo implements Serializable {

	/**
	 * YouTube video ID.
	 */
	private String id;
	/**
	 * Video title.
	 */
	private String title;
	/**
	 * Channel (only id and name are set).
	 */
	private YouTubeChannel channel;
	/**
	 * The total number of 'likes'.
	 */
	private String likeCount;
	/**
	 * The total number of 'dislikes'.
	 */
	private String dislikeCount;
	/**
	 * The percentage of people that thumbs-up this video (format:  "<percentage>%").
	 */
	private String thumbsUpPercentageStr;
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
	 * The video publish date in pretty format (e.g. "17 hours ago").
	 */
	private transient String publishDatePretty;
	/**
	 * The time when the publishDatePretty was calculated.
	 */
	private transient long publishDatePrettyCalculationTime;
	/**
	 * Thumbnail URL.
	 */
	private String thumbnailUrl;
	/**
	 * Thumbnail URL (maximum resolution).
	 */
	private String thumbnailMaxResUrl;
	/**
	 * The language of this video.  (This tends to be ISO 639-1).
	 */
	private String language;
	/**
	 * The description of the video (set by the YouTuber/Owner).
	 */
	private String description;
	/**
	 * Set to true if the video is a current live stream.
	 */
	private boolean isLiveStream;
	
	/** publishDate will remain valid for 1 hour. */
	private final static long PUBLISH_DATE_VALIDITY_TIME = 60 * 60 * 1000L;


	/**
	 * Constructor.
	 */
	public YouTubeVideo(Video video) {
		this.id = video.getId();

		if (video.getSnippet() != null) {
			this.title = video.getSnippet().getTitle();

			this.channel = new YouTubeChannel(video.getSnippet().getChannelId(), video.getSnippet().getChannelTitle());
			setPublishDate(video.getSnippet().getPublishedAt());

			if (video.getSnippet().getThumbnails() != null) {
				Thumbnail thumbnail = video.getSnippet().getThumbnails().getHigh();
				if (thumbnail != null)
					this.thumbnailUrl = thumbnail.getUrl();

				thumbnail = video.getSnippet().getThumbnails().getMaxres();
				if (thumbnail != null)
					this.thumbnailMaxResUrl = thumbnail.getUrl();
			}

			this.language = video.getSnippet().getDefaultAudioLanguage() != null ? video.getSnippet().getDefaultAudioLanguage()
					: (video.getSnippet().getDefaultLanguage());

			this.description = video.getSnippet().getDescription();
		}

		if (video.getContentDetails() != null) {
			setDuration(video.getContentDetails().getDuration());
			setIsLiveStream();
			setDurationInSeconds(video.getContentDetails().getDuration());
		}

		if (video.getStatistics() != null) {
			BigInteger  likeCount = video.getStatistics().getLikeCount(),
						dislikeCount = video.getStatistics().getDislikeCount();

			setThumbsUpPercentage(likeCount, dislikeCount);

			this.viewsCountInt = video.getStatistics().getViewCount();
			this.viewsCount = String.format(getStr(R.string.views), viewsCountInt);

			if (likeCount != null)
				this.likeCount = String.format(Locale.getDefault(), "%,d", video.getStatistics().getLikeCount());

			if (dislikeCount != null)
				this.dislikeCount = String.format(Locale.getDefault(), "%,d", video.getStatistics().getDislikeCount());
		}
	}

	/**
	 * Extracts the video ID from the given video URL.
	 *
	 * @param url YouTube video URL.
	 * @return ID if everything went as planned; null otherwise.
	 */
	public static String getYouTubeIdFromUrl(String url) {
		if (url == null)
			return null;

		// TODO:  support playlists (i.e. video_ids=... <-- URL submitted via email by YouTube)
		final String pattern = "(?<=v=|/videos/|embed/|youtu\\.be/|/v/|/e/|video_ids=)[^#&?%]*";
		Pattern compiledPattern = Pattern.compile(pattern);
		Matcher matcher = compiledPattern.matcher(url);

		return matcher.find() ? matcher.group() /*video id*/ : null;
	}

	/**
	 * Sets the {@link #thumbsUpPercentageStr}, i.e. the percentage of people that thumbs-up this video
	 * (format:  "<percentage>%").
	 *
	 * @param likedCountInt	Total number of "likes".
	 * @param dislikedCountInt Total number of "dislikes".
	 */
	private void setThumbsUpPercentage(BigInteger likedCountInt, BigInteger dislikedCountInt) {
		String fullPercentageStr = null;
		int percentageInt = -1;

		// some videos do not allow users to like/dislike them:  hence likedCountInt / dislikedCountInt
		// might be null in those cases
		if (likedCountInt != null && dislikedCountInt != null) {
			BigDecimal likedCount = new BigDecimal(likedCountInt),
					dislikedCount = new BigDecimal(dislikedCountInt),
					totalVoteCount = likedCount.add(dislikedCount),	// liked and disliked counts
					likedPercentage = null;

			if (totalVoteCount.compareTo(BigDecimal.ZERO) != 0) {
				likedPercentage = (likedCount.divide(totalVoteCount, MathContext.DECIMAL128)).multiply(new BigDecimal(100));

				// round the liked percentage to 0 decimal places and convert it to string
				String percentageStr = likedPercentage.setScale(0, RoundingMode.HALF_UP).toString();
				fullPercentageStr = percentageStr + "%";
				percentageInt = Integer.parseInt(percentageStr);
			}
		}

		this.thumbsUpPercentageStr = fullPercentageStr;
		this.thumbsUpPercentage = percentageInt;
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

	public void setDescription(String description) {
		this.description = description;
	}

	public String getId() {
		return id;
	}

	public String getTitle() {
		return title;
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
		return (thumbsUpPercentageStr != null);
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
		return thumbsUpPercentageStr;
	}

	/**
	 * @return The total number of 'likes'.  Can return <b>null</b> if the video does not allow the
	 * users to like/dislike it.  Refer to {@link #isThumbsUpPercentageSet}.
	 */
	public String getLikeCount() {
		return likeCount;
	}

	/**
	 * @return The total number of 'dislikes'.  Can return <b>null</b> if the video does not allow the
	 * users to like/dislike it.  Refer to {@link #isThumbsUpPercentageSet}.
	 */
	public String getDislikeCount() {
		return dislikeCount;
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

	/**
	 * Sets the publishDate and publishDatePretty.
	 */
	private void setPublishDate(DateTime publishDate) {
		this.publishDate = publishDate;
		this.publishDatePretty = null;
	}

	/**
	 * Gets the {@link #publishDate} as a pretty string.
	 */
	public String getPublishDatePretty() {
		long now = System.currentTimeMillis();
		// if pretty is not yet calculated, or the publish date was generated more than (1 hour) PUBLISH_DATE_VALIDITY_TIME ago...
		if (publishDatePretty == null || (PUBLISH_DATE_VALIDITY_TIME < now - publishDatePrettyCalculationTime)) {
			this.publishDatePretty = (publishDate != null) ? new PrettyTimeEx().format(publishDate) : "???";
			this.publishDatePrettyCalculationTime = now;
		}
		return publishDatePretty;
	}

	/**
	 * Given that {@link #publishDatePretty} is being cached once generated, this method will allow
	 * you to regenerate and reset the {@link #publishDatePretty}.
	 */
	public void forceRefreshPublishDatePretty() {
		// Will force the publishDatePretty to be regenerated.  Refer to getPublishDatePretty()
		this.publishDatePretty = null;
	}

	public String getThumbnailUrl() {
		return thumbnailUrl;
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

	public String getDescription() {
		return description;
	}

	public boolean isLiveStream() {
		return isLiveStream;
	}

	public void bookmarkVideo(Context context, Menu menu) {
		boolean successBookmark = BookmarksDb.getBookmarksDb().add(this);
		Toast.makeText(context,
				successBookmark ? R.string.video_bookmarked : R.string.video_bookmarked_error,
				Toast.LENGTH_LONG).show();

		if (successBookmark) {
			menu.findItem(R.id.bookmark_video).setVisible(false);
			menu.findItem(R.id.unbookmark_video).setVisible(true);
		}
	}

	public void unbookmarkVideo(Context context, Menu menu) {
		boolean successUnbookmark = BookmarksDb.getBookmarksDb().remove(this);
		Toast.makeText(context,
				successUnbookmark ? R.string.video_unbookmarked : R.string.video_unbookmarked_error,
				Toast.LENGTH_LONG).show();

		if (successUnbookmark) {
			menu.findItem(R.id.bookmark_video).setVisible(true);
			menu.findItem(R.id.unbookmark_video).setVisible(false);
		}
	}

	public void shareVideo(Context context) {
		Intent intent = new Intent(android.content.Intent.ACTION_SEND);
		intent.setType("text/plain");
		intent.putExtra(android.content.Intent.EXTRA_TEXT, getVideoUrl());
		context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_via)));
	}

	public void copyUrl(Context context) {
		ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
		ClipData clip = ClipData.newPlainText("Video URL", getVideoUrl());
		clipboard.setPrimaryClip(clip);
		Toast.makeText(context, R.string.url_copied_to_clipboard, Toast.LENGTH_SHORT).show();
	}


	/**
	 * If the user have previously downloaded the video, this method will return the Uri of the file;
	 * else, get the stream for this video (based on the user's preference) by communicating with
	 * YouTube servers.  When done, the stream Uri will be returned via the passed
	 * {@link GetDesiredStreamListener}.
	 *
	 * @param listener Instance of {@link GetDesiredStreamListener} to pass the stream through.
	 */
	public void getDesiredStream(GetDesiredStreamListener listener) {
		new GetVideoStreamTask(this, listener).executeInParallel();
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
		});
	}

	/**
	 * Play the video using an external app
	 */
	public void playVideoExternally(Context context) {
		Uri fileUri = getFileUri();
		if (fileUri != null) {
			File file = new File(fileUri.getPath());
			Uri uri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", file);
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
