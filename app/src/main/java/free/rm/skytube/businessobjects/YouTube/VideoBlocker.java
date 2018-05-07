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

package free.rm.skytube.businessobjects.YouTube;

import android.widget.Toast;

import com.google.common.base.Optional;
import com.optimaize.langdetect.DetectedLanguage;
import com.optimaize.langdetect.LanguageDetector;
import com.optimaize.langdetect.LanguageDetectorBuilder;
import com.optimaize.langdetect.i18n.LdLocale;
import com.optimaize.langdetect.ngram.NgramExtractors;
import com.optimaize.langdetect.profiles.LanguageProfile;
import com.optimaize.langdetect.profiles.LanguageProfileReader;
import com.optimaize.langdetect.text.CommonTextObjectFactories;
import com.optimaize.langdetect.text.TextObject;
import com.optimaize.langdetect.text.TextObjectFactory;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.Logger;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;
import free.rm.skytube.businessobjects.db.ChannelFilteringDb;
import free.rm.skytube.businessobjects.db.SubscriptionsDb;

import static free.rm.skytube.app.SkyTubeApp.getStr;

/**
 * Filters videos base on constraints set by the user.
 */
public class VideoBlocker {

	/**
	 * Default preferred language(s) -- by default, no language shall be filtered out.
	 */
	private static final Set<String> defaultPrefLanguages = new HashSet<>(SkyTubeApp.getStringArrayAsList(R.array.languages_iso639_codes));


	/**
	 * Block a channel.  This operation depends on what filtering method was enabled by the user:
	 * i.e. either channel blacklisting or whitelisting.
	 *
	 * @param channelId     Channel ID.
	 * @param channelName   Channel name.
	 */
	public static void blockChannel(String channelId, String channelName) {
		try {
			// if user is subscribed to the channel, then ask the user to unsubscribe first...
			if (SubscriptionsDb.getSubscriptionsDb().isUserSubscribedToChannel(channelId)) {
				Toast.makeText(SkyTubeApp.getContext(), R.string.channel_blockage_error_user_subscribed, Toast.LENGTH_LONG).show();
			} else {
				if (isChannelBlacklistEnabled()) {
					blacklistChannel(channelId, channelName);
				} else {
					unwhitelistChannel(channelId);
				}
			}
		} catch (IOException e) {
			Logger.e(VideoBlocker.class, "Error occurred while blocking a channel", e);
		}
	}


	private static void blacklistChannel(String channelId, String channelName) {
		boolean successBlockChannel = ChannelFilteringDb.getChannelFilteringDb().blacklist(channelId, channelName);

		Toast.makeText(SkyTubeApp.getContext(),
				successBlockChannel ? R.string.channel_blacklisted : R.string.channel_blacklist_error,
				Toast.LENGTH_LONG).show();
	}


	private static void unwhitelistChannel(String channelId) {
		boolean success = ChannelFilteringDb.getChannelFilteringDb().unwhitelist(channelId);

		Toast.makeText(SkyTubeApp.getContext(),
				success ? R.string.channel_unwhitelist_success : R.string.channel_unwhitelist_error,
				Toast.LENGTH_LONG).show();
	}



	/**
	 * Filters videos base on constraints set by the user.
	 *
	 * @param videosList    Videos list to be filtered.
	 *
	 * @return  A list of valid videos that fit the user's criteria.
	 */
	public List<YouTubeVideo> filter(List<YouTubeVideo> videosList) {
		List<YouTubeVideo>  filteredVideosList    = new ArrayList<>();
		final boolean       isChannelBlacklistEnabled = isChannelBlacklistEnabled();
		final List<String>  blacklistedChannelIds = isChannelBlacklistEnabled ? ChannelFilteringDb.getChannelFilteringDb().getBlacklistedChannelIdsList() : null;
		final List<String>  whitelistedChannelIds = !isChannelBlacklistEnabled ? ChannelFilteringDb.getChannelFilteringDb().getWhitelistedChannelsIdsList() : null;
		// set of user's preferred ISO 639 language codes (regex)
		final Set<String>   preferredLanguages    = SkyTubeApp.getPreferenceManager().getStringSet(getStr(R.string.pref_key_preferred_languages), defaultPrefLanguages);
		final BigInteger    minimumVideoViews     = getViewsFilteringValue();
		final int           minimumVideoDislikes  = getDislikesFilteringValue();

		for (YouTubeVideo video : videosList) {
			if ( !((isChannelBlacklistEnabled  &&  filterByBlacklistedChannels(video, blacklistedChannelIds))
					|| (!isChannelBlacklistEnabled  &&  filterByWhitelistedChannels(video, whitelistedChannelIds))
					||  filterByLanguage(video, preferredLanguages)
					||  filterByLanguageDetection(video, preferredLanguages)
					||  filterByViews(video, minimumVideoViews)
					||  filterByDislikes(video, minimumVideoDislikes)) ) {
				filteredVideosList.add(video);
			}
		}

		return filteredVideosList;
	}


	/**
	 * Log filtered videos.
	 *
	 * @param video         Video being filtered.
	 * @param filteringType Criteria (why being filtered - e.g. channel blocked).
	 * @param reason        The criteria hit (e.g. ID of the channel blocked).
	 */
	private void log(YouTubeVideo video, FilterType filteringType, String reason) {
		Logger.i(this, "\uD83D\uDED1 VIDEO='%s'  |  FILTER='%s'  |  REASON='%s'", video.getTitle(), filteringType, reason);
	}


	/**
	 * @return True if channel blacklisting is enabled;  false if channel whitelisting is enabled.
	 */
	private static boolean isChannelBlacklistEnabled() {
		final String channelFilter = SkyTubeApp.getPreferenceManager().getString(getStr(R.string.pref_key_channel_filter_method), getStr(R.string.channel_blacklisting_filtering));
		return channelFilter.equals(getStr(R.string.channel_blacklisting_filtering));
	}


	/**
	 * Filter the video for blacklisted channels.
	 *
	 * @param video                 Video to be checked.
	 * @param blacklistedChannelIds Blacklisted channels IDs.
	 *
	 * @return True if the video is to be filtered; false otherwise.
	 */
	private boolean filterByBlacklistedChannels(YouTubeVideo video, List<String> blacklistedChannelIds) {
		if (blacklistedChannelIds.contains(video.getChannelId())) {
			log(video, FilterType.CHANNEL_BLACKLIST, video.getChannelName());
			return true;
		} else {
			return false;
		}
	}


	private boolean filterByWhitelistedChannels(YouTubeVideo video, List<String> whitelistedChannelIds) {
		if (whitelistedChannelIds.contains(video.getChannelId())) {
			return false;
		} else {
			log(video, FilterType.CHANNEL_WHITELIST, video.getChannelName());
			return true;
		}
	}


	/**
	 * Return true if this video does not meet the preferred language criteria;  false otherwise.
	 * Many YouTube videos do not set the language, hence this method will not be accurate.
	 *
	 * @param video                 Video that is going to be checked for filtering purposes.
	 * @param preferredLanguages    A set of user's preferred ISO 639 language codes (regex).
	 *
	 * @return True to filter out the video; false otherwise.
	 */
	private boolean filterByLanguage(YouTubeVideo video, Set<String> preferredLanguages) {
		// if the video's language is not defined (i.e. null) or empty
		//	OR if there is no linguistic content to the video (zxx)
		//	OR if the language is undefined (und)
		// then we are NOT going to filter this video
		if (video.getLanguage() == null
				|| video.getLanguage().isEmpty()
				|| video.getLanguage().equalsIgnoreCase("zxx")
				|| video.getLanguage().equalsIgnoreCase("und"))
			return false;

		// if there are no preferred languages, then it means we must not filter this video
		if (preferredLanguages.isEmpty())
			return false;

		// if this video's language is equal to the user's preferred one... then do NOT filter it out
		for (String prefLanguage : preferredLanguages) {
			if (video.getLanguage().matches(prefLanguage))
				return false;
		}

		// this video is undesirable, hence we are going to filter it
		log(video, FilterType.LANGUAGE, video.getLanguage());
		return true;
	}


	/**
	 * Filter out the given video if it does not meet the preferred language criteria.  The app will
	 * try to determine the language of the video by analyzing the video's title.
	 *
	 * @param video                 Video that is going to be checked for filtering purposes.
	 * @param preferredLanguages    A set of user's preferred ISO 639 language codes (regex).
	 *
	 * @return True to filter out the video; false otherwise.
	 */
	private boolean filterByLanguageDetection(YouTubeVideo video, Set<String> preferredLanguages) {
		final String text = video.getTitle().toLowerCase();
		List<String> detectLanguageList = new ArrayList<>();

		// if the user does not want to block videos based on artificial language detection, then do
		// not filter the video
		if (!SkyTubeApp.getPreferenceManager().getBoolean(getStr(R.string.pref_key_lang_detection_video_filtering), false))
			return false;

		// if there are no preferred languages, then it means we must not filter this video
		if (preferredLanguages.isEmpty())
			return false;

		try {
			// detect language
			TextObject textObject = LanguageDetectionSingleton.get().getTextObjectFactory().forText(text);
			Optional<LdLocale> lang = LanguageDetectionSingleton.get().getLanguageDetector().detect(textObject);

			// if the confidence in the language detection is 100%, then ...
			if (lang.isPresent()) {
				final String langDetected = lang.get().getLanguage();

				detectLanguageList.add(langDetected);

				// if this video's language is equal to the user's preferred one... then do NOT filter it out
				for (String prefLanguage : preferredLanguages) {
					if (langDetected.matches(prefLanguage))
						return false;
				}
			} else {
				// else if the library is not 100% that the language detected is the correct one...
				List<DetectedLanguage> detectedLangList = LanguageDetectionSingleton.get().getLanguageDetector().getProbabilities(text);

				for (DetectedLanguage detectedLanguage : detectedLangList) {
					String langDetected = detectedLanguage.getLocale().getLanguage();

					detectLanguageList.add(langDetected);

					for (String prefLanguage : preferredLanguages) {
						if (langDetected.matches(prefLanguage))
							return false;
					}
				}
			}
		} catch (Throwable tr) {
			Logger.e(this, "Exception caught while detecting language", tr);
		}

		log(video, FilterType.LANGUAGE_DETECTION, detectLanguageList.toString());
		return true;
	}


	/**
	 * @return The views filtering value set by the user.
	 */
	private BigInteger getViewsFilteringValue() {
		final  String viewsFiltering = SkyTubeApp.getPreferenceManager().getString(getStr(R.string.pref_key_low_views_filter), getStr(R.string.views_filtering_disabled));
		return new BigInteger(viewsFiltering);
	}


	/**
	 * Filter videos by minimum views.  I.e. if videos has less views than minimumVideoViews, then
	 * filter it out.
	 *
	 * @param video             Video being processed.
	 * @param minimumVideoViews The minimum amount of views that a video should have as set by the
	 *                          user.
	 *
	 * @return True to filter out the video; false otherwise.
	 */
	private boolean filterByViews(YouTubeVideo video, BigInteger minimumVideoViews) {
		// if the user has not enabled the view filtering (i.e. it is set as -1), then do not filter
		// this video
		if (minimumVideoViews.signum() < 0)
			return false;

		// if the video has less views than minimumVideoViews, then filter it out
		if (video.getViewsCountInt().compareTo(minimumVideoViews) < 0) {
			log(video, FilterType.VIEWS, String.format(getStr(R.string.views), video.getViewsCountInt()));
			return true;
		}

		return false;
	}


	/**
	 * @return The dislikes filtering value set by the user.
	 */
	private int getDislikesFilteringValue() {
		final  String dislikesFiltering = SkyTubeApp.getPreferenceManager().getString(getStr(R.string.pref_key_dislikes_filter), getStr(R.string.dislikes_filtering_disabled));
		return Integer.parseInt(dislikesFiltering);
	}


	/**
	 * Filter videos by dislikes.  I.e. if videos has more dislikes than minimumVideoDislikes, then
	 * filter it out.
	 *
	 * @param video                 Video being processed.
	 * @param minimumVideoDislikes  The minimum amount of dislikes that a video should have as set
	 *                              by the user.
	 *
	 * @return True to filter out the video; false otherwise.
	 */
	private boolean filterByDislikes(YouTubeVideo video, int minimumVideoDislikes) {
		// if the user has not enabled the dislikes filtering (i.e. it is set as -1), then do not
		// filter this video
		if (minimumVideoDislikes < 0)
			return false;

		// a video may not allow users to like/dislike...
		if (video.getThumbsUpPercentage() == -1)
			return false;

		final int dislikesPercentage = 100 - video.getThumbsUpPercentage();

		// if the video has more dislikes than minimumVideoDislikes, then filter it out
		if (dislikesPercentage >= minimumVideoDislikes) {
			log(video, FilterType.DISLIKES, String.format(getStr(R.string.dislikes), dislikesPercentage));
			return true;
		}

		return false;
	}



	////////////////////////////////////////////////////////////////////////////////////////////////


	/**
	 * The type of video filtering.
	 */
	public enum FilterType {
		CHANNEL_BLACKLIST,
		CHANNEL_WHITELIST,
		LANGUAGE,
		LANGUAGE_DETECTION,
		VIEWS,
		DISLIKES,
	}


	/**
	 * A singleton of objects used to detect languages.  This is required to improve the language
	 * detection performance...
	 */
	private static class LanguageDetectionSingleton {

		private static LanguageDetectionSingleton languageDetectionSingleton = null;
		private TextObjectFactory textObjectFactory;
		private LanguageDetector  languageDetector;


		private LanguageDetectionSingleton() throws IOException {
			// load all languages
			List<LanguageProfile> languageProfiles = new LanguageProfileReader().readAllBuiltIn();

			// build language detector
			languageDetector = LanguageDetectorBuilder.create(NgramExtractors.standard())
					.withProfiles(languageProfiles)
					.build();

			// create a text object factory
			textObjectFactory = CommonTextObjectFactories.forDetectingShortCleanText();
		}


		public static LanguageDetectionSingleton get() throws IOException {
			if (languageDetectionSingleton == null) {
				languageDetectionSingleton = new LanguageDetectionSingleton();
			}

			return languageDetectionSingleton;
		}


		TextObjectFactory getTextObjectFactory() {
			return textObjectFactory;
		}


		LanguageDetector getLanguageDetector() {
			return languageDetector;
		}

	}

}
