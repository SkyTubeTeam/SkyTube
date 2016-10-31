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

import android.util.Log;

import com.google.api.client.util.DateTime;
import com.google.api.services.youtube.model.Thumbnail;
import com.google.api.services.youtube.model.Video;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import free.rm.skytube.R;
import free.rm.skytube.businessobjects.VideoStream.ParseStreamMetaData;
import free.rm.skytube.businessobjects.VideoStream.StreamMetaDataList;
import free.rm.skytube.gui.app.SkyTubeApp;

/**
 * Represents a YouTube video.
 */
public class YouTubeVideo implements Serializable {

	/** YouTube video ID. */
	private String	id;
	/** Video title. */
	private String	title;
	/** Channel ID. */
	private String channelId;
	/** Channel name. */
	private String	channelName;
	/** The total number of 'likes'. */
	private String	likeCount;
	/** The total number of 'dislikes'. */
	private String	dislikeCount;
	/** The percentage of people that thumbs-up this video (format:  "<percentage>%"). */
	private String	thumbsUpPercentageStr;
	private int		thumbsUpPercentage;
	/** Video duration string (e.g. "5:15"). */
	private String	duration;
	/** Total views count.  This can be <b>null</b> if the video does not allow the user to
	 * like/dislike it. */
	private String	viewsCount;
	/** The date/time of when this video was published (e.g. "7 hours ago"). */
	private String	publishDate;
	/** The date/time of when this video was published, in ISO 8601 format */
	private DateTime publishDateAsDate;
	/** Thumbnail URL string. */
	private String	thumbnailUrl;
	/** The language of this video.  (This tends to be ISO 639-1).  */
	private String	language;
	/** The description of the video (set by the YouTuber/Owner). */
	private String	description;

	private static final Set<String> defaultPrefLanguages = new HashSet<>(Arrays.asList(SkyTubeApp.getStr(R.string.lang_en)));

	private static final String TAG = YouTubeVideo.class.getSimpleName();


	public YouTubeVideo(Video video) {
		this.id = video.getId();

		if (video.getSnippet() != null) {
			this.title       = video.getSnippet().getTitle();
			this.channelId   = video.getSnippet().getChannelId();
			this.channelName = video.getSnippet().getChannelTitle();
			setPublishDate(video.getSnippet().getPublishedAt());
			publishDateAsDate = video.getSnippet().getPublishedAt();

			if (video.getSnippet().getThumbnails() != null) {
				Thumbnail thumbnail = video.getSnippet().getThumbnails().getHigh();
				if (thumbnail != null)
					this.thumbnailUrl = thumbnail.getUrl();
			}

			this.language = video.getSnippet().getDefaultAudioLanguage() != null ? video.getSnippet().getDefaultAudioLanguage()
					: (video.getSnippet().getDefaultLanguage() != null ? video.getSnippet().getDefaultLanguage() : null);

			this.description = video.getSnippet().getDescription();
		}

		if (video.getContentDetails() != null) {
			setDuration(video.getContentDetails().getDuration());
		}

		if (video.getStatistics() != null) {
			BigInteger	likeCount = video.getStatistics().getLikeCount(),
						dislikeCount = video.getStatistics().getDislikeCount();

			setThumbsUpPercentage(likeCount, dislikeCount);

			this.viewsCount = String.format(SkyTubeApp.getStr(R.string.views),
											video.getStatistics().getViewCount());

			if (likeCount != null)
				this.likeCount = String.format("%,d", video.getStatistics().getLikeCount());

			if (dislikeCount != null)
				this.dislikeCount = String.format("%,d", video.getStatistics().getDislikeCount());
		}
	}



	/**
	 * Returns a list of video/stream meta-data that is supported by this app (with respect to this
	 * video).
	 *
	 * @return A list of {@link StreamMetaDataList}.
	 */
	public StreamMetaDataList getVideoStreamList() {
		StreamMetaDataList streamMetaDataList;

		try {
			ParseStreamMetaData ex = new ParseStreamMetaData();
			String errorMsg = ex.init(id);

			streamMetaDataList = (errorMsg == null) ? ex.getStreamMetaDataList() : new StreamMetaDataList(errorMsg);
		} catch (Exception e) {
			Log.e(TAG, "An error has occurred while getting video metadata/streams for video with id=" + id, e);
			streamMetaDataList = new StreamMetaDataList(SkyTubeApp.getStr(R.string.error_stream_parse_error));
		}

		return streamMetaDataList;
	}



	/**
	 * Sets the {@link #thumbsUpPercentageStr}, i.e. the percentage of people that thumbs-up this video
	 * (format:  "<percentage>%").
	 *
	 * @param likedCountInt		Total number of "likes".
	 * @param dislikedCountInt	Total number of "dislikes".
	 */
	private void setThumbsUpPercentage(BigInteger likedCountInt, BigInteger dislikedCountInt) {
		String	fullPercentageStr = null;
		int		percentageInt = -1;

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
				String percentageStr = likedPercentage.setScale(0, RoundingMode.HALF_UP).toString();
				fullPercentageStr = percentageStr + "%";
				percentageInt = Integer.parseInt(percentageStr);
			}
		}

		this.thumbsUpPercentageStr = fullPercentageStr;
		this.thumbsUpPercentage = percentageInt;
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
		this.publishDate = (publishDateTime != null)
								? new PrettyTimeEx().format(publishDateTime)
								: "???";
	}

	public DateTime getPublishDateAsDate() {
		return publishDateAsDate;
	}

	public String getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public String getChannelId() {
		return channelId;
	}

	public String getChannelName() {
		return channelName;
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

	public String getViewsCount() {
		return viewsCount;
	}

	public String getPublishDate() {
		return publishDate;
	}

	public String getThumbnailUrl() {
		return thumbnailUrl;
	}

	public String getLanguage() {
		return language;
	}

	public String getDescription() {
		return description;
	}

	/**
	 * Return true if this video does not meet the preferred language criteria;  false otherwise.
	 * Many YouTube videos do not set the language, hence this method will not be accurate.
	 *
	 * @return  True to filter out the video; false otherwise.
	 */
	public boolean filterVideoByLanguage() {
		Set<String> preferredLanguages = getPreferredLanguages();

		// if the video's language is not defined (i.e. null)
		//    OR if there is no linguistic content to the video (zxx)
		//    OR if the language is undefined (und)
		// then we are NOT going to filter this video
		if (getLanguage() == null  ||  getLanguage().equalsIgnoreCase("zxx")  ||  getLanguage().equalsIgnoreCase("und"))
			return false;

		// if there are no preferred languages, then it means we must not filter this video
		if (preferredLanguages.isEmpty())
			return false;

		// if this video's language is equal to the user's preferred one... then do NOT filter it out
		for (String prefLanguage : preferredLanguages) {
			if (getLanguage().matches(prefLanguage))
				return false;
		}

		// this video is undesirable, hence we are going to filter it
		Log.i("FILTERING Video", getTitle() + "[" + getLanguage() + "]");
		return true;
	}


	private Set<String> getPreferredLanguages() {
		return SkyTubeApp.getPreferenceManager().getStringSet(SkyTubeApp.getStr(R.string.pref_key_preferred_languages), defaultPrefLanguages);
	}

}
