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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.Logger;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;
import free.rm.skytube.businessobjects.db.BlockedChannelsDb;

import static free.rm.skytube.app.SkyTubeApp.getStr;

/**
 * Filters videos base on constraints set by the user.
 */
public class FilterVideos {

	/**
	 * Default preferred language(s) -- by default, no language shall be filtered out.
	 */
	private static final Set<String> defaultPrefLanguages = new HashSet<>(SkyTubeApp.getStringArrayAsList(R.array.languages_iso639_codes));


	/**
	 * Filters videos base on constraints set by the user.
	 *
	 * @param videosList    Videos list to be filtered.
	 *
	 * @return  A list of valid videos that fit the user's criteria.
	 */
	public List<YouTubeVideo> filter(List<YouTubeVideo> videosList) {
		List<YouTubeVideo>  filteredVideosList = new ArrayList<>();
		final List<String>  blockedChannelIds = BlockedChannelsDb.getBlockedChannelsDb().getBlockedChannelsListId();
		// set of user's preferred ISO 639 language codes (regex)
		final Set<String>   preferredLanguages = SkyTubeApp.getPreferenceManager().getStringSet(getStr(R.string.pref_key_preferred_languages), defaultPrefLanguages);

		for (YouTubeVideo video : videosList) {
			if (!filterByBlockedChannels(video, blockedChannelIds)  &&  !filterByLanguage(video, preferredLanguages)) {
				filteredVideosList.add(video);
			}
		}

		return videosList;
	}


	/**
	 * Log filtered videos.
	 *
	 * @param video     Video being filtered.
	 * @param criteria  Criteria (why being filtered - e.g. channel blocked).
	 * @param reason    The criteria hit (e.g. ID of the channel blocked).
	 */
	private void log(YouTubeVideo video, String criteria, String reason) {
		Logger.d(this, "\uD83D\uDED1 VIDEO='%s'  |  CRITERIA='%s'  |  REASON='%s'", video.getTitle(), criteria, reason);
	}


	/**
	 * Filter the videosList for blocked channels.
	 *
	 * @param video
	 * @param blockedChannelIds
	 *
	 * @return True if the video is to be filtered; false otherwise.
	 */
	private boolean filterByBlockedChannels(YouTubeVideo video, List<String> blockedChannelIds) {
		if (blockedChannelIds.contains(video.getChannelId())) {
			log(video, "channel", video.getChannelId());
			return true;
		} else {
			return false;
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
		// if the video's language is not defined (i.e. null) or emptyr
		//	OR if there is no linguistic content to the video (zxx)
		//	OR if the language is undefined (und)
		// then we are NOT going to filter this video
		if (video.getLanguage() == null
				||  video.getLanguage().isEmpty()
				||  video.getLanguage().equalsIgnoreCase("zxx")
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
		log(video, "language", video.getLanguage());
		return true;
	}

}
