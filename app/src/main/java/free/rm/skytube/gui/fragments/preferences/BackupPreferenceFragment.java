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

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.NonNull;
import android.support.v13.app.FragmentCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.github.angads25.filepicker.controller.DialogSelectionListener;
import com.github.angads25.filepicker.model.DialogConfigs;
import com.github.angads25.filepicker.model.DialogProperties;
import com.github.angads25.filepicker.view.FilePickerDialog;

import java.io.File;

import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.AsyncTaskParallel;
import free.rm.skytube.gui.businessobjects.preferences.BackupDatabases;

/**
 * Preference fragment for backup related settings.
 */
public class BackupPreferenceFragment extends PreferenceFragment {

	private static final int EXT_STORAGE_PERM_CODE_BACKUP = 1950;
	private static final int EXT_STORAGE_PERM_CODE_IMPORT = 1951;
	private static final String TAG = BackupPreferenceFragment.class.getSimpleName();


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preference_backup);

		// backup/export databases
		Preference backupDbsPref = findPreference(getString(R.string.pref_key_backup_dbs));
		backupDbsPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				backupDatabases();
				return true;
			}
		});

		// import databases
		Preference importBackupsPref = findPreference(getString(R.string.pref_key_import_dbs));
		importBackupsPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				displayFilePicker();
				return true;
			}
		});
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



	////////////////////////////////////////////////////////////////////////////////////////////////

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
