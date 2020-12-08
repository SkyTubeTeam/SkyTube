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

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import free.rm.skytube.R;
import free.rm.skytube.gui.businessobjects.SubscriptionsBackupsManager;

/**
 * Preference fragment for backup related settings.
 */
public class BackupPreferenceFragment extends PreferenceFragmentCompat {
	private SubscriptionsBackupsManager subscriptionsBackupsManager;

	@Override
	public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
		addPreferencesFromResource(R.xml.preference_backup);

		subscriptionsBackupsManager = new SubscriptionsBackupsManager(getActivity(), this);

		// backup/export databases
		Preference backupDbsPref = findPreference(getString(R.string.pref_key_backup_dbs));
		backupDbsPref.setOnPreferenceClickListener(preference -> {
			subscriptionsBackupsManager.backupDatabases();
			return true;
		});

		// import databases
		Preference importBackupsPref = findPreference(getString(R.string.pref_key_import_dbs));
		importBackupsPref.setOnPreferenceClickListener(preference -> {
			subscriptionsBackupsManager.displayFilePicker();
			return true;
		});

		// import from youtube
		Preference importSubsPref = findPreference(getString(R.string.pref_key_import_subs));
		importSubsPref.setOnPreferenceClickListener(preference -> {
			subscriptionsBackupsManager.displayImportSubscriptionsFromYouTubeDialog();
			return true;
		});
	}

	@Override
	public void onDestroy() {
		subscriptionsBackupsManager.clearBackgroundTasks();
		super.onDestroy();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		subscriptionsBackupsManager.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		subscriptionsBackupsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
	}
}
