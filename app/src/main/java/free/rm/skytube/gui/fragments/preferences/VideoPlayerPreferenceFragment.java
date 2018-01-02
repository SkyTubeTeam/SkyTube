/*
 * SkyTube
 * Copyright (C) 2017  Ramon Mifsud
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

package free.rm.skytube.gui.fragments.preferences;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;

import free.rm.skytube.BuildConfig;
import free.rm.skytube.R;
import free.rm.skytube.businessobjects.YouTube.VideoStream.VideoResolution;

/**
 * Preference fragment for video player related settings.
 */
public class VideoPlayerPreferenceFragment extends PreferenceFragment {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preference_video_player);

		// set up the list of available video resolutions
		ListPreference resolutionPref = (ListPreference) findPreference(getString(R.string.pref_key_preferred_res));
		resolutionPref.setEntries(VideoResolution.getAllVideoResolutionsNames());
		resolutionPref.setEntryValues(VideoResolution.getAllVideoResolutionsIds());

		// remove the 'use official player' checkbox if we are running an OSS version
		if (BuildConfig.FLAVOR.equals("oss")) {
			PreferenceScreen screen = getPreferenceScreen();
			Preference useOfficialPlayer = getPreferenceManager().findPreference(getString(R.string.pref_key_use_offical_player));
			screen.removePreference(useOfficialPlayer);
		}
	}

}
