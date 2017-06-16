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
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
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

import com.github.angads25.filepicker.controller.DialogSelectionListener;
import com.github.angads25.filepicker.model.DialogConfigs;
import com.github.angads25.filepicker.model.DialogProperties;
import com.github.angads25.filepicker.view.FilePickerDialog;

import java.io.File;

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
	private static final int EXT_STORAGE_PERM_CODE_BACKUP = 1950;
	private static final int EXT_STORAGE_PERM_CODE_IMPORT = 1951;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// load the preferences from an XML resource
		addPreferencesFromResource(R.xml.preferences);

		ListPreference resolutionPref = (ListPreference) findPreference(getString(R.string.pref_key_preferred_res));
		resolutionPref.setEntries(VideoResolution.getAllVideoResolutionsNames());
		resolutionPref.setEntryValues(VideoResolution.getAllVideoResolutionsIds());

		Preference backupDbsPref = findPreference(getString(R.string.pref_key_backup_dbs));
		backupDbsPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				backupDatabases();
				return true;
			}
		});

		Preference importBackupsPref = findPreference(getString(R.string.pref_key_import_dbs));
		importBackupsPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				displayFilePicker();
				return true;
			}
		});

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

		// remove the 'use official player' checkbox if we are running an OSS version
		if (BuildConfig.FLAVOR.equals("oss")) {
			PreferenceCategory videoPlayerCategory = (PreferenceCategory) findPreference(getString(R.string.pref_key_video_player_category));
			Preference useOfficialPlayer = findPreference(getString(R.string.pref_key_use_offical_player));
			videoPlayerCategory.removePreference(useOfficialPlayer);
		}

		// set the app's version number
		Preference versionPref = findPreference(getString(R.string.pref_key_version));
		versionPref.setSummary(BuildConfig.VERSION_NAME);

		// credits
		Preference creditsPref = findPreference(getString(R.string.pref_key_credits));
		creditsPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				displayCredits();
				return true;
			}
		});

		// Default tab
		ListPreference defaultTabPref = (ListPreference)findPreference(getString(R.string.pref_key_default_tab));
		if (defaultTabPref.getValue() == null) {
			defaultTabPref.setValueIndex(0);
		}
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
	 * Check if the app has access to the external storage.  If not, ask the user whether he wants
	 * to give us access...
	 *
	 * @param permissionRequestCode The request code (either EXT_STORAGE_PERM_CODE_BACKUP or
	 *                              EXT_STORAGE_PERM_CODE_IMPORT) which is used by
	 *                              {@link #onRequestPermissionsResult(int, String[], int[])} to
	 *                              determine whether we are going to backup (export) or to import.
	 *
	 * @return True if the user has given access to write to the external storage in the past;
	 * false otherwise.
	 */
	private boolean hasAccessToExtStorage(int permissionRequestCode) {
		boolean hasAccessToExtStorage = true;

		// if the user has not yet granted us access to the external storage...
		if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
			// We can request the permission (to the users).  If the user grants us access (or
			// otherwise), then the method #onRequestPermissionsResult() will be called.
			FragmentCompat.requestPermissions(this,
					new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
					permissionRequestCode);

			hasAccessToExtStorage = false;
		}

		return hasAccessToExtStorage;
	}



	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		// EXT_STORAGE_PERM_CODE_BACKUP is used to backup the databases
		if (requestCode == EXT_STORAGE_PERM_CODE_BACKUP) {
			if (grantResults.length > 0  &&  grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				// permission was granted by the user
				new BackupDatabasesTask().executeInParallel();
			} else {
				// permission denied by the user
				Toast.makeText(getActivity(), R.string.databases_backup_fail, Toast.LENGTH_LONG).show();
			}
		}
		// EXT_STORAGE_PERM_CODE_IMPORT is used for the file picker (to import database backups)
		else if (requestCode == EXT_STORAGE_PERM_CODE_IMPORT) {
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				displayFilePicker();
			}
			else {
				// permission not been granted by user
				Toast.makeText(getActivity(), R.string.databases_import_fail, Toast.LENGTH_LONG).show();
			}
		}
	}



	/**
	 * Backup the databases.
	 */
	private void backupDatabases() {
		// if the user has granted us access to the external storage, then perform the backup
		// operation
		if (hasAccessToExtStorage(EXT_STORAGE_PERM_CODE_BACKUP)) {
			new BackupDatabasesTask().executeInParallel();
		}
	}


	/**
	 * Display file picker to be used by the user to select the backup he wants to import.
	 */
	private void displayFilePicker() {
		// do not display the file picker until the user gives us access to the external storage
		if (!hasAccessToExtStorage(EXT_STORAGE_PERM_CODE_IMPORT))
			return;

		DialogProperties properties = new DialogProperties();

		properties.selection_mode = DialogConfigs.SINGLE_MODE;
		properties.selection_type = DialogConfigs.FILE_SELECT;
		properties.root = Environment.getExternalStorageDirectory();
		properties.error_dir = new File(DialogConfigs.DEFAULT_DIR);
		properties.offset = new File(DialogConfigs.DEFAULT_DIR);
		properties.extensions = new String[]{"skytube"};

		FilePickerDialog dialog = new FilePickerDialog(getActivity(), properties);
		dialog.setDialogSelectionListener(new DialogSelectionListener() {
			@Override
			public void onSelectedFilePaths(String[] files) {
				if (files == null  ||  files.length <= 0)
					Toast.makeText(getActivity(), R.string.databases_import_nothing_selected, Toast.LENGTH_LONG).show();
				else
					displayImportDbsBackupWarningMsg(files[0]);
			}
		});
		dialog.setTitle(R.string.databases_import_select_backup);
		dialog.show();
	}


	/**
	 * Display import database warning:  i.e. all the current data will be replaced by that of the
	 * import file.
	 *
	 * @param backupFilePath    The backup file to import.
	 */
	private void displayImportDbsBackupWarningMsg(final String backupFilePath) {
		new AlertDialog.Builder(getActivity())
				.setMessage(R.string.databases_import_warning_message)
				.setPositiveButton(R.string.continue_, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						new ImportDatabasesTask(backupFilePath).executeInParallel();
					}
				})
				.setNegativeButton(R.string.cancel, null)
				.show();
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
	 * A task that backups the subscriptions and bookmarks databases.
	 */
	private class BackupDatabasesTask extends AsyncTaskParallel<Void, Void, String> {

		@Override
		protected void onPreExecute() {
			Toast.makeText(getActivity(), R.string.databases_backing_up, Toast.LENGTH_SHORT).show();
		}


		@Override
		protected String doInBackground(Void... params) {
			String backupPath = null;

			try {
				BackupDatabases backupDatabases = new BackupDatabases();
				backupPath = backupDatabases.backupDbsToSdCard();
			} catch (Throwable tr) {
				Log.e(TAG, "Unable to backup the databases...", tr);
			}

			return backupPath;
		}


		@Override
		protected void onPostExecute(String backupPath) {
			String message =  (backupPath != null)
					? String.format(getString(R.string.databases_backup_success), backupPath)
					: getString(R.string.databases_backup_fail);

			new AlertDialog.Builder(getActivity())
					.setMessage(message)
					.setNeutralButton(R.string.ok, null)
					.show();
		}

	}



	/**
	 * A task that imports the subscriptions and bookmarks databases.
	 */
	private class ImportDatabasesTask extends AsyncTaskParallel<Void, Void, Boolean> {

		private String backupFilePath;

		public ImportDatabasesTask(String backupFilePath) {
			this.backupFilePath = backupFilePath;
		}


		@Override
		protected void onPreExecute() {
			Toast.makeText(getActivity(), R.string.databases_importing, Toast.LENGTH_SHORT).show();
		}


		@Override
		protected Boolean doInBackground(Void... params) {
			boolean successful = false;

			try {
				BackupDatabases backupDatabases = new BackupDatabases();
				backupDatabases.importBackupDb(backupFilePath);
				successful = true;
			} catch (Throwable tr) {
				Log.e(TAG, "Unable to import the databases...", tr);
			}

			return successful;
		}


		@Override
		protected void onPostExecute(Boolean successfulImport) {
			new AlertDialog.Builder(getActivity())
					.setCancelable(false)
					.setMessage(successfulImport ? R.string.databases_import_success : R.string.databases_import_fail)
					.setNeutralButton(R.string.restart, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							SkyTubeApp.restartApp();
						}
					})
					.show();
		}

	}

}
