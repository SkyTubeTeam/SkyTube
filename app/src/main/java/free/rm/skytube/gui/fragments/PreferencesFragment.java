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

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.support.annotation.NonNull;
import android.support.v13.app.FragmentCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import free.rm.skytube.BuildConfig;
import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.AsyncTaskParallel;
import free.rm.skytube.businessobjects.ValidateYouTubeAPIKey;
import free.rm.skytube.businessobjects.VideoStream.VideoResolution;
import free.rm.skytube.gui.businessobjects.preferences.BackupDatabases;

/**
 * A fragment that allows the user to change the settings of this app.  This fragment is called by
 * {@link free.rm.skytube.gui.activities.PreferencesActivity}
 */
public class PreferencesFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

	private static final String TAG = PreferencesFragment.class.getSimpleName();
	private static final int    PERMISSION_WRITE_EXTERNAL = 1950;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// load the preferences from an XML resource
		addPreferencesFromResource(R.xml.preferences);

		// preferred resolution
		ListPreference resolutionPref = (ListPreference) findPreference(R.string.pref_key_preferred_res);
		resolutionPref.setEntries(VideoResolution.getAllVideoResolutionsNames());
		resolutionPref.setEntryValues(VideoResolution.getAllVideoResolutionsIds());

		// export database
		Preference exportDbPref = findPreference(R.string.pref_key_export_dbs);
		exportDbPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				askForExternalStoragePermission();
				return true;
			}
		});

		// default tab
		ListPreference defaultTabPref = (ListPreference)findPreference(R.string.pref_key_default_tab);
		if (defaultTabPref.getValue() == null) {
			defaultTabPref.setValueIndex(0);
		}
		defaultTabPref.setSummary(String.format(getString(R.string.pref_summary_default_tab), defaultTabPref.getEntry()));

		// set the app's version number
		Preference versionPref = findPreference(R.string.pref_key_version);
		versionPref.setSummary(BuildConfig.VERSION_NAME);

		// if the user clicks on the website link, then open it using an external browser
		Preference websitePref = findPreference(R.string.pref_key_website);
		websitePref.setSummary(BuildConfig.SKYTUBE_WEBSITE);
		websitePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				// view the app's website in a web browser
				Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.SKYTUBE_WEBSITE));
				startActivity(browserIntent);
				return true;
			}
		});

		// credits
		Preference creditsPref = findPreference(R.string.pref_key_credits);
		creditsPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				displayCredits();
				return true;
			}
		});

		// if the user clicks on the license, then open the display the actual license
		Preference licensePref = findPreference(R.string.pref_key_license);
		licensePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				displayAppLicense();
				return true;
			}
		});

		// remove the 'use official player' checkbox if we are running an OSS version
		if (BuildConfig.FLAVOR.equals("oss")) {
			PreferenceCategory videoPlayerCategory = (PreferenceCategory) findPreference(R.string.pref_key_video_player_category);
			Preference useOfficialPlayer = findPreference(R.string.pref_key_use_offical_player);
			videoPlayerCategory.removePreference(useOfficialPlayer);
		}
	}


	/**
	 * Finds a {@link Preference} based on its key.
	 *
	 * @param keyStringResId The key of the preference to retrieve.
	 * @return The {@link Preference} with the key, or null.
	 */
	public Preference findPreference(int keyStringResId) {
		return findPreference(getString(keyStringResId));
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
			}
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


	/**
	 * Displays the credits (i.e. contributors).
	 */
	private void displayCredits() {
		AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

		WebView webView = new WebView(getActivity());
		webView.setWebViewClient(new WebViewClient());
		webView.getSettings().setJavaScriptEnabled(false);
		webView.loadUrl(BuildConfig.SKYTUBE_WEBSITE_CREDITS);

		alert.setView(webView);
		alert.setPositiveButton(R.string.ok, null);
		alert.show();
	}


	/**
	 * Check if the user has given the app access to the external storage.  If he has, then backup
	 * the databases.  Else, ask the user to give us permission...
	 */
	private void askForExternalStoragePermission() {
		if (ContextCompat.checkSelfPermission(getActivity(),
				Manifest.permission.WRITE_EXTERNAL_STORAGE)
				!= PackageManager.PERMISSION_GRANTED) {

			// Ask the user to allow the app to use the External Storage.  The result of the user's
			// choice will be passed to onRequestPermissionsResult()
			FragmentCompat.requestPermissions(this,
					new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
					PERMISSION_WRITE_EXTERNAL);
		} else {
			new BackupDatabasesTask().executeInParallel();
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		switch (requestCode) {
			case PERMISSION_WRITE_EXTERNAL: {
				// If request is cancelled, the result arrays are empty.
				if (grantResults.length > 0  &&  grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					// permission was granted:  now export/backup the database
					new BackupDatabasesTask().executeInParallel();
				} else {
					// permission denied by the user...
					Toast.makeText(getActivity(), R.string.databases_export_fail, Toast.LENGTH_LONG).show();
				}
				break;
			}
		}

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


	/**
	 * Backsup the subscriptions and bookmarks databases.
	 */
	private class BackupDatabasesTask extends AsyncTaskParallel<Void, Void, String> {

		private ProgressDialog exportDbsDialog;
		private final String TAG = BackupDatabasesTask.class.getSimpleName();

		@Override
		protected void onPreExecute() {
			// setup the backup dialog and display it
			exportDbsDialog = new ProgressDialog(PreferencesFragment.this.getActivity());
			exportDbsDialog.setMessage(getString(R.string.databases_backing_up));
			exportDbsDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			exportDbsDialog.setCancelable(false);
			exportDbsDialog.show();
		}


		@Override
		protected String doInBackground(Void... params) {
			String backupPath = null;

			try {
				BackupDatabases backupDatabases = new BackupDatabases();
				backupPath = backupDatabases.backupDbsToSdCard();
			} catch (Throwable tr) {
				Log.e(TAG, "An error has occurred while backing up the DBs", tr);
			}

			return backupPath;
		}


		@Override
		protected void onPostExecute(String backupPath) {
			exportDbsDialog.dismiss();

			String backupStatusMsg;

			// generate a message whether the backup was successful or not
			if (backupPath != null) {
				backupStatusMsg = String.format(getString(R.string.databases_export_success), backupPath);
			} else {
				backupStatusMsg = getString(R.string.databases_export_fail);
			}

			// display the backup success/failure message to the user
			new AlertDialog.Builder(getActivity())
					.setMessage(backupStatusMsg)
					.setPositiveButton(R.string.ok, null)
					.show();
		}

	}

}
