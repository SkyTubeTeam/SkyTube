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

package free.rm.skytube.gui.fragments;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.widget.Toast;

import com.google.api.services.youtube.YouTube;

import java.io.IOException;

import free.rm.skytube.BuildConfig;
import free.rm.skytube.R;
import free.rm.skytube.businessobjects.VideoStream.VideoResolution;
import free.rm.skytube.businessobjects.YouTubeAPI;
import free.rm.skytube.gui.app.SkyTubeApp;

/**
 * A fragment that allows the user to change the settings of this app.  This fragment is called by
 * {@link free.rm.skytube.gui.activities.PreferencesActivity}
 */
public class PreferencesFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

	private static final String TAG = PreferencesFragment.class.getSimpleName();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// load the preferences from an XML resource
		addPreferencesFromResource(R.xml.preferences);

		ListPreference resolutionPref = (ListPreference) findPreference(getString(R.string.pref_key_preferred_res));
		resolutionPref.setEntries(VideoResolution.getAllVideoResolutionsNames());
		resolutionPref.setEntryValues(VideoResolution.getAllVideoResolutionsIds());

		// if the user clicks on the license, then open the display the actual license
		Preference licensePref = findPreference(getString(R.string.pref_key_license));
		licensePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				displayAppLicense();
				return true;
			}
		});

		// if the user clicks on the website link, then open it using an external browser
		Preference websitePref = findPreference(getString(R.string.pref_key_website));
		websitePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				// view the app's website in a web browser
				Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.pref_summary_website)));
				startActivity(browserIntent);
				return true;
			}
		});

		// remove the 'use official player' checkbox if we are running an OSS version
		if (BuildConfig.FLAVOR.equals("oss")) {
			PreferenceCategory videoPlayerCategory = (PreferenceCategory) findPreference(getString(R.string.pref_key_video_player_category));
			Preference useOfficialPlayer = findPreference(getString(R.string.pref_key_use_offical_player));
			videoPlayerCategory.removePreference(useOfficialPlayer);
		}

		// set the app's version number
		Preference versionPref = findPreference(getString(R.string.pref_key_version));
		versionPref.setSummary(BuildConfig.VERSION_NAME);

		// Default tab
		ListPreference defaultTabPref = (ListPreference)findPreference(getString(R.string.pref_key_default_tab));
		if(defaultTabPref.getValue() == null)
			defaultTabPref.setValueIndex(0);
		defaultTabPref.setSummary(String.format(getString(R.string.pref_summary_default_tab), defaultTabPref.getEntry()));
	}

	@Override
	public void onResume() {
		super.onResume();
		getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onPause() {
		super.onPause();
		getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if(key != null) {
			if (key.equals(getString(R.string.pref_key_default_tab))) {
				// If the user changed the Default Tab Preference, update the summary to show the new default tab
				ListPreference defaultTabPref = (ListPreference) findPreference(key);
				defaultTabPref.setSummary(String.format(getString(R.string.pref_summary_default_tab), defaultTabPref.getEntry()));
			} else if (key.equals(getString(R.string.pref_youtube_api_key))) {
				// Validate the entered API Key when saved (and not empty), with a simple call to get the most popular video
				final EditTextPreference    youtubeAPIKeyPref = (EditTextPreference) findPreference(getString(R.string.pref_youtube_api_key));
				final String                youtubeAPIKey     = youtubeAPIKeyPref.getText();

				if (youtubeAPIKey != null  &&  !youtubeAPIKey.isEmpty()) {
					new AsyncTask<Void, Void, Boolean>() {
						@Override
						protected Boolean doInBackground(Void... voids) {
							try {
								YouTube.Videos.List videosList = YouTubeAPI.create().videos().list("snippet");
								videosList.setFields("items(id)");
								videosList.setKey(youtubeAPIKey);
								videosList.setChart("mostPopular");
								String regionCode = SkyTubeApp.getPreferenceManager().getString(SkyTubeApp.getStr(R.string.pref_key_preferred_region), "").trim();
								videosList.setRegionCode(regionCode.isEmpty() ? null : regionCode);
								videosList.setMaxResults(1l);
								videosList.execute();
							} catch (IOException e) {
								return true;
							}
							return false;
						}


						@Override
						protected void onPostExecute(Boolean error) {
							super.onPostExecute(error);
							// If the validation failed, reset the preference to null
							if (error)
								youtubeAPIKeyPref.setText(null);
							// Show a message depending on if the validation passed or failed
							Toast.makeText(getActivity(), getString(error ? R.string.pref_youtube_api_key_error : R.string.pref_youtube_api_key_saved), Toast.LENGTH_LONG).show();
						}
					}.execute();
				}
			}
		}
	}

	/**
	 * Displays the app's license in an AlertDialog.
	 */
	private void displayAppLicense() {
		new AlertDialog.Builder(getActivity())
				.setMessage(R.string.app_license)
				.setNeutralButton(R.string.i_agree, null)
				.setCancelable(false)	// do not allow the user to click outside the dialog or press the back button
				.show();
	}

}
