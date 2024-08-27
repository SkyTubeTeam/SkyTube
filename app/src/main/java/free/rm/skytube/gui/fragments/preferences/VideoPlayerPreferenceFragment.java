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
import android.content.res.XmlResourceParser;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.google.common.base.Joiner;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

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
			final ListPreference    videoPlayersListPref = getPreferenceManager().findPreference(getString(R.string.pref_key_choose_player));
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
		configureLanguageSelector();
	}

	private List<String> getLanguages() {
		List<String> result = new ArrayList<>();
		try {
			XmlResourceParser xpp = getResources().getXml(R.xml._generated_res_locale_config);
			while (xpp.getEventType() != XmlPullParser.END_DOCUMENT) {
				if (xpp.getEventType() == XmlPullParser.START_TAG) {
					if ("locale".equals(xpp.getName()) && xpp.getAttributeCount() > 0 && "name".equals(xpp.getAttributeName(0))) {
						result.add(xpp.getAttributeValue(0));
					}
				}
				xpp.next();
			}
			Logger.i(this, "Language list:"+result);
		} catch(XmlPullParserException|IOException e) {
			Logger.e(this, "Unable to parse locale config: "+e.getMessage(), e);
		}
		return result;
	}

	private void configureLanguageSelector() {
		ListPreference languageSelector = findPreference(getString(R.string.pref_key_app_language));

		List<String> languages = getLanguages();
		LocaleListCompat localeList = LocaleListCompat.forLanguageTags(Joiner.on(",").join(languages));
		int size = localeList.size();
		String[] localeCodes = new String[size];
		String[] localeNames = new String[size];
		for (int i = 0;i<size;i++) {
			Locale locale = localeList.get(i);
			localeNames[i] = locale.getDisplayName();
			localeCodes[i] = locale.getLanguage();
		}
		languageSelector.setEntries(localeNames);
		languageSelector.setEntryValues(localeCodes);
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
		if (getString(R.string.pref_key_app_language).equals(key)) {
			String newLanguage = sharedPreferences.getString(key, null);
			LocaleListCompat appLocale = LocaleListCompat.forLanguageTags(newLanguage);
			AppCompatDelegate.setApplicationLocales(appLocale);
		}
	}
}
