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
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.PreferenceFragmentCompat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import free.rm.skytube.R;
import free.rm.skytube.app.EventBus;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.AsyncTaskParallel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeAPIKey;
import free.rm.skytube.businessobjects.YouTube.ValidateYouTubeAPIKey;

/**
 * Preference fragment for other settings.
 */
public class OthersPreferenceFragment extends BasePreferenceFragment {
	ListPreference defaultTabPref;

	@Override
	public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
		addPreferencesFromResource(R.xml.preference_others);

		// Default tab
		defaultTabPref = findPreference(getString(R.string.pref_key_default_tab_name));
		Set<String> hiddenFragments = SkyTubeApp.getPreferenceManager().getStringSet(getString(R.string.pref_key_hide_tabs), new HashSet<>());
		final String[] tabListValues = SkyTubeApp.getStringArray(R.array.tab_list_values);
		final String[] tabListLabels = SkyTubeApp.getFragmentNames().getAllNames(tabListValues);
		if(hiddenFragments.isEmpty()) {
			defaultTabPref.setEntries(tabListLabels);
			defaultTabPref.setEntryValues(tabListValues);
		} else {
			List<String> defaultTabEntries = new ArrayList<>();
			List<String> defaultTabEntryValues = new ArrayList<>();
			for(int i = 0; i< tabListValues.length; i++) {
				if(!hiddenFragments.contains(tabListValues[i])) {
					defaultTabEntries.add(tabListLabels[i]);
					defaultTabEntryValues.add(tabListValues[i]);

				}
			}
			defaultTabPref.setEntries(defaultTabEntries.toArray(new CharSequence[defaultTabEntries.size()]));
			defaultTabPref.setEntryValues(defaultTabEntryValues.toArray(new CharSequence[defaultTabEntryValues.size()]));
		}
		if (defaultTabPref.getValue() == null) {
			defaultTabPref.setValueIndex(0);
		}
		defaultTabPref.setSummary(String.format(getString(R.string.pref_summary_default_tab), defaultTabPref.getEntry()));

		MultiSelectListPreference hiddenTabsPref = findPreference(getString(R.string.pref_key_hide_tabs));
		hiddenTabsPref.setEntries(tabListLabels);

//		ListPreference feedNotificationPref = (ListPreference) findPreference(getString(R.string.pref_feed_notification_key));
//		if(feedNotificationPref.getValue() == null) {
//			feedNotificationPref.setValueIndex(0);
//		}
//		feedNotificationPref.setSummary(String.format(getString(R.string.pref_summary_feed_notification), feedNotificationPref.getEntry()));
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if(key != null) {
			if (key.equals(getString(R.string.pref_key_default_tab_name))) {
				// If the user changed the Default Tab Preference, update the summary to show the new default tab
				ListPreference defaultTabPref = (ListPreference) findPreference(key);
				defaultTabPref.setSummary(String.format(getString(R.string.pref_summary_default_tab), defaultTabPref.getEntry()));
			} else if (key.equals(getString(R.string.pref_key_hide_tabs))) {
				//
				EventBus.getInstance().notifyMainTabChanged(EventBus.SettingChange.HIDE_TABS);

				// displayRestartDialog(R.string.pref_hide_tabs_restart, true);
			} else if (key.equals(getString(R.string.pref_youtube_api_key))) {
				// Validate the entered API Key when saved (and not empty), with a simple call to get the most popular video
				EditTextPreference    youtubeAPIKeyPref = (EditTextPreference) findPreference(getString(R.string.pref_youtube_api_key));
				String                youtubeAPIKey     = youtubeAPIKeyPref.getText();

				if (youtubeAPIKey != null) {
					youtubeAPIKey = youtubeAPIKey.trim();

					if (!youtubeAPIKey.isEmpty()) {
						// validate the user's API key
						new ValidateYouTubeAPIKeyTask(youtubeAPIKey).executeInParallel();
					}
					else {
						// inform the user that we are going to use the default YouTube API key and
						// that we need to restart the app
						displayRestartDialog(R.string.pref_youtube_api_key_default,false);
					}
				}
			} else if (key.equals(getString(R.string.pref_key_subscriptions_alphabetical_order))) {
				EventBus.getInstance().notifyMainTabChanged(EventBus.SettingChange.SUBSCRIPTION_LIST_CHANGED);
			}/*else if (key.equals(getString(R.string.pref_feed_notification_key))) {
				ListPreference feedNotificationPref = (ListPreference) findPreference(key);
				feedNotificationPref.setSummary(String.format(getString(R.string.pref_summary_feed_notification), feedNotificationPref.getEntry()));

				int interval = Integer.parseInt(feedNotificationPref.getValue());

				SkyTubeApp.setFeedUpdateInterval(interval);
			}*/
		}
	}

	/**
	 * Display a dialog with message <code>messageID</code> and force the user to restart the app by
	 * tapping on the restart button.
	 *
	 * @param messageID Message resource ID.
	 */
	private void displayRestartDialog(int messageID, boolean restart) {
		final AlertDialog.Builder b = new AlertDialog.Builder(getActivity())
				.setMessage(messageID)
				.setCancelable(false);
		if (restart) {
			b.setPositiveButton(R.string.restart, (dialog, which) -> SkyTubeApp.restartApp());
		} else {
			b.setPositiveButton(R.string.ok, null);
		}
		b.show();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////


	/**
	 * A task that validates the given YouTube API key.
	 */
	private class ValidateYouTubeAPIKeyTask extends AsyncTaskParallel<Void, Void, Boolean> {

		private String youtubeAPIKey;


		public ValidateYouTubeAPIKeyTask(String youtubeAPIKey) {
			this.youtubeAPIKey = youtubeAPIKey;
		}


		@Override
		protected Boolean doInBackground(Void... voids) {
			ValidateYouTubeAPIKey validateKey = new ValidateYouTubeAPIKey(youtubeAPIKey);
			return validateKey.isKeyValid();
		}


		@Override
		protected void onPostExecute(Boolean isKeyValid) {
			if (!isKeyValid) {
				// if the validation failed, reset the preference to null
				((EditTextPreference) findPreference(getString(R.string.pref_youtube_api_key))).setText(null);
			} else {
				YouTubeAPIKey key = YouTubeAPIKey.reset();
				if (key.isUserApiKeySet()) {
					// if the key is valid, then inform the user that the custom API key is valid and
					// that he needs to restart the app in order to use it

					displayRestartDialog(R.string.pref_youtube_api_key_custom, false);
				} else {
					displayRestartDialog(R.string.pref_youtube_api_key_default, false);
				}
			}

			// display a toast to show that the key is not valid
			if (!isKeyValid) {
				Toast.makeText(getActivity(), getString(R.string.pref_youtube_api_key_error), Toast.LENGTH_LONG).show();
			}
		}

	}

}
