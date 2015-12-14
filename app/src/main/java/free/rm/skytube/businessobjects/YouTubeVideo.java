/*
 * SkyTube
 * Copyright (C) 2015  Ramon Mifsud
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

package free.rm.skytube.businessobjects;

import com.google.api.client.util.DateTime;
import com.google.api.services.youtube.model.Thumbnail;
import com.google.api.services.youtube.model.Video;

import org.ocpsoft.prettytime.PrettyTime;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Date;

import free.rm.skytube.R;
import free.rm.skytube.gui.app.SkyTubeApp;

/**
 * Represents a YouTube video.
 */
public class YouTubeVideo {

	/** YouTube video ID. */
	private String	id;
	/** Video title. */
	private String	title;
	/** Channel name. */
	private String	channelName;
	/** The percentage of people that thumbs-up this video (format:  "<percentage>%"). */
	private String	thumbsUpPercentage;
	/** Video duration string (e.g. "5:15"). */
	private String	duration;
	/** Total views count.  This can be <b>null</b> if the video does not allow the user to
	 * like/dislike it. */
	private String	viewsCount;
	/** The date/time of when this video was published (e.g. "7 hours ago"). */
	private String	publishDate;
	/** Thumbnail URL string. */
	private String	thumbnailUrl;


	public YouTubeVideo(Video video) {
		this.id = video.getId();

		if (video.getSnippet() != null) {
			this.title       = video.getSnippet().getTitle();
			this.channelName = video.getSnippet().getChannelTitle();
			setPublishDate(video.getSnippet().getPublishedAt());

			Thumbnail thumbnail = video.getSnippet().getThumbnails().getHigh();
			if (thumbnail != null)
				this.thumbnailUrl = thumbnail.getUrl();
		}

		if (video.getContentDetails() != null) {
			setDuration(video.getContentDetails().getDuration());
		}

		if (video.getStatistics() != null) {
			setThumbsUpPercentage(video.getStatistics().getLikeCount(), video.getStatistics().getDislikeCount());

			this.viewsCount = String.format(SkyTubeApp.getStr(R.string.views),
											video.getStatistics().getViewCount());
		}
	}


	/**
	 * Sets the {@link #thumbsUpPercentage}, i.e. the percentage of people that thumbs-up this video
	 * (format:  "<percentage>%").
	 *
	 * @param likedCountInt		Total number of "likes".
	 * @param dislikedCountInt	Total number of "dislikes".
	 */
	private void setThumbsUpPercentage(BigInteger likedCountInt, BigInteger dislikedCountInt) {
		String percentage = null;

		// some videos do not allow users to like/dislike them:  hence likedCountInt / dislikedCountInt
		// might be null in those cases
		if (likedCountInt != null  &&  dislikedCountInt != null) {
			BigDecimal	likedCount      = new BigDecimal(likedCountInt),
						dislikedCount   = new BigDecimal(dislikedCountInt),
						totalVoteCount  = likedCount.add(dislikedCount),    // liked and disliked counts
						likedPercentage = null;

			if (totalVoteCount.compareTo(BigDecimal.ZERO) != 0) {
				likedPercentage = (likedCount.divide(totalVoteCount, MathContext.DECIMAL128)).multiply(new BigDecimal(100));

				// round the liked percentage to 0 decimal places and convert it to string
				percentage = likedPercentage.setScale(0, RoundingMode.HALF_UP).toString() + "%";
			}
		}

		this.thumbsUpPercentage = percentage;
	}


	/**
	 * Sets the {@link #duration} by converts ISO 8601 duration to human readable string.
	 *
	 * @param duration ISO 8601 duration.
	 */
	private void setDuration(String duration) {
		this.duration = VideoDuration.toHumanReadableString(duration);
	}


	/**
	 * Sets the {@link #publishDate} by converting the given video's publish date into a pretty
	 * string.
	 *
	 * @param publishDateTime {@link DateTime} of when the video was published.
	 */
	private void setPublishDate(DateTime publishDateTime) {
		Long unixEpoch   = publishDateTime.getValue();
		Date publishDate = new Date(unixEpoch);

		this.publishDate = new PrettyTime().format(publishDate);
	}


	public String getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public String getChannelName() {
		return channelName;
	}

	/**
	 * @return The thumbs up percentage.  Can return <b>null</b> if the video does not allow the
	 * user to like/dislike it.
	 */
	public String getThumbsUpPercentage() {
		return thumbsUpPercentage;
	}

	public String getDuration() {
		return duration;
	}

	public String getViewsCount() {
		return viewsCount;
	}

	public String getPublishDate() {
		return publishDate;
	}

	public String getThumbnailUrl() {
		return thumbnailUrl;
	}

}
