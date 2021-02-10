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

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.util.Arrays;

import free.rm.skytube.BuildConfig;
import free.rm.skytube.R;
import free.rm.skytube.app.EventBus;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.Logger;
import free.rm.skytube.businessobjects.YouTube.newpipe.NewPipeService;

/**
 * Preference fragment for video player related settings.
 */
public class VideoPlayerPreferenceFragment extends BasePreferenceFragment {
	@Override
	public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
		addPreferencesFromResource(R.xml.preference_video_player);

		// if we are running an OSS version, then remove the last option (i.e. the "official" player
		// option)
		if (BuildConfig.FLAVOR.equals("oss")) {
			final ListPreference    videoPlayersListPref = (ListPreference) getPreferenceManager().findPreference(getString(R.string.pref_key_choose_player));
			final CharSequence[]    videoPlayersList = videoPlayersListPref.getEntries();
			CharSequence[]          modifiedVideoPlayersList = Arrays.copyOf(videoPlayersList, videoPlayersList.length - 1);

			videoPlayersListPref.setEntries(modifiedVideoPlayersList);
		}

		Preference creditsPref = findPreference(getString(R.string.pref_key_switch_volume_and_brightness));
		creditsPref.setOnPreferenceClickListener(preference -> {
			SkyTubeApp.getSettings().showTutorialAgain();
			return true;
		});

		configureCountrySelector();
	}

	private void configureCountrySelector() {
		ListPreference countrySelector = findPreference(getString(R.string.pref_key_default_content_country));
		String[] countryCodes = SkyTubeApp.getStringArray(R.array.country_codes);
		String[] countryNames = SkyTubeApp.getStringArray(R.array.country_names);
		countrySelector.setEntryValues(countryCodes);
		String[] countryNamesWithSystemDefault = new String[countryNames.length];
		System.arraycopy(countryNames, 1, countryNamesWithSystemDefault, 1, countryNames.length - 1);
		countryNamesWithSystemDefault[0] = getString(R.string.system_default_country);
		countrySelector.setEntries(countryNamesWithSystemDefault);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		Logger.i(this, "onSharedPreferenceChanged %s - key: %s", sharedPreferences, key);
		if (getString(R.string.pref_key_default_content_country).equals(key)) {
			String newCountry = sharedPreferences.getString(key, null);
			NewPipeService.setCountry(newCountry);
			EventBus.getInstance().notifyMainTabChanged(EventBus.SettingChange.CONTENT_COUNTRY);
		}
	}
}
