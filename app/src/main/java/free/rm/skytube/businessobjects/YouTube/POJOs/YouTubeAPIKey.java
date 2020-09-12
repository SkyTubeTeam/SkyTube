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

import java.util.Random;

import free.rm.skytube.BuildConfig;
import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;

/**
 * Represents a YouTube API key.
 */
public class YouTubeAPIKey {

	/** User's YouTube API key which is inputted via the
	 * {@link free.rm.skytube.gui.fragments.preferences.OthersPreferenceFragment}.  Will be null if the user did not
	 * input a key. **/
	private String userAPIKey;
	private String debugKey;
	private String[] apiKeys;

	private Random random = new Random();

	private static YouTubeAPIKey youTubeAPIKey = null;



	/**
	 * Constructor.  Will retrieve user's YouTube API key if set.
	 */
	private YouTubeAPIKey() {
		userAPIKey = getUserApiKey();
		debugKey = reconstruct(BuildConfig.YOUTUBE_API_KEYS_DEBUG);
		apiKeys = new String[BuildConfig.YOUTUBE_API_KEYS.length];
		for (int i=0;i<apiKeys.length;i++) {
			apiKeys[i] = reconstruct(BuildConfig.YOUTUBE_API_KEYS[i]);
		}
	}

	private String reconstruct(String youtubeApiKey) {
		if (youtubeApiKey.startsWith("hidden:")) {
			String realHiddenKey = youtubeApiKey.substring("hidden:".length());
			// We don't need UTF-8 here
			return new ObfuscateKey("SkyTubeIsTheBest").decrypt(realHiddenKey);
		} else {
			return youtubeApiKey;
		}
	}


	/**
	 * @return An instance of {@link YouTubeAPIKey}.
	 */
	public synchronized static YouTubeAPIKey get() {
		if (youTubeAPIKey == null) {
			youTubeAPIKey = new YouTubeAPIKey();
		}

		return youTubeAPIKey;
	}


	public synchronized static YouTubeAPIKey reset() {
		youTubeAPIKey = null;
		return get();
	}

	/**
	 * @return Return YouTube API key.
	 */
	public String getYouTubeAPIKey() {
		if (isUserApiKeySet()) {
			// if the user has not set his own API key, then use the default SkyTube key
			return userAPIKey;
		} else {
			if (BuildConfig.DEBUG) {
				return debugKey;
			} else {
				// else we are going to choose one of the defaults keys at random
				int i = random.nextInt(apiKeys.length);
				return apiKeys[i];
			}
		}
	}


	/**
	 * @return True if the user has set his own YouTube API key (via the
	 * {@link free.rm.skytube.gui.fragments.preferences.OthersPreferenceFragment}); false otherwise.
	 */
	public boolean isUserApiKeySet() {
		return (userAPIKey != null);
	}


	/**
	 * @return User's YouTube API key (if set).  If the user did not set a key, then it will return null.
	 */
	private String getUserApiKey() {
		String userApiKey = SkyTubeApp.getPreferenceManager().getString(SkyTubeApp.getStr(R.string.pref_youtube_api_key), null);

		if (userApiKey != null) {
			userApiKey = userApiKey.trim();

			if (userApiKey.isEmpty()) {
				return null;
			}
			if (userApiKey.equals(BuildConfig.YOUTUBE_API_KEYS_DEBUG)) {
				return null;
			}
			for (String builtInKey : BuildConfig.YOUTUBE_API_KEYS) {
				if (builtInKey.equals(userApiKey)) {
					return null;
				}
			}
			return userApiKey;
		}

		return null;
	}

}
