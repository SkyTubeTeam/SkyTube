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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import android.widget.Toast;

import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.AsyncTaskParallel;
import free.rm.skytube.businessobjects.YouTube.ValidateYouTubeAPIKey;
import free.rm.skytube.gui.businessobjects.adapters.SubsAdapter;

/**
 * Preference fragment for other settings.
 */
public class OthersPreferenceFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preference_others);

		// Default tab
		ListPreference defaultTabPref = (ListPreference)findPreference(getString(R.string.pref_key_default_tab));
		if (defaultTabPref.getValue() == null) {
			defaultTabPref.setValueIndex(0);
		}
		defaultTabPref.setSummary(String.format(getString(R.string.pref_summary_default_tab), defaultTabPref.getEntry()));

//		ListPreference feedNotificationPref = (ListPreference) findPreference(getString(R.string.pref_feed_notification_key));
//		if(feedNotificationPref.getValue() == null) {
//			feedNotificationPref.setValueIndex(0);
//		}
//		feedNotificationPref.setSummary(String.format(getString(R.string.pref_summary_feed_notification), feedNotificationPref.getEntry()));
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
						displayRestartDialog(R.string.pref_youtube_api_key_default);
					}
				}
			} else if (key.equals(getString(R.string.pref_key_subscriptions_alphabetical_order))){
				CheckBoxPreference enableChannelSort = (CheckBoxPreference) findPreference(key);
				SubsAdapter subsAdapter = SubsAdapter.get(getActivity());
				if (enableChannelSort.isChecked()){
					subsAdapter.setChannelsSorted(true);
					subsAdapter.refreshSubsList();
				} else {
					subsAdapter.setChannelsSorted(false);
					subsAdapter.refreshSubsList();
				}
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
	private void displayRestartDialog(int messageID) {
		new AlertDialog.Builder(getActivity())
				.setMessage(messageID)
				.setPositiveButton(R.string.restart, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						SkyTubeApp.restartApp();
					}
				})
				.setCancelable(false)
				.show();
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
				// if the key is valid, then inform the user that the custom API key is valid and
				// that he needs to restart the app in order to use it
				displayRestartDialog(R.string.pref_youtube_api_key_custom);
			}

			// display a toast to show that the key is not valid
			if (!isKeyValid) {
				Toast.makeText(getActivity(), getString(R.string.pref_youtube_api_key_error), Toast.LENGTH_LONG).show();
			}
		}

	}

}
