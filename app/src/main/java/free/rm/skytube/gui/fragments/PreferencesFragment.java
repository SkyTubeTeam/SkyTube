/*
 * SkyTube
 * Copyright (C) 2015  Ramon Mifsud
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
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
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;

import free.rm.skytube.R;

/**
 * A fragment that allows the user to change the settings of this app.  This fragment is called by
 * {@link free.rm.skytube.gui.activities.PreferencesActivity}
 */
public class PreferencesFragment extends PreferenceFragment {

	private static final String TAG = PreferencesFragment.class.getSimpleName();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// load the preferences from an XML resource
		addPreferencesFromResource(R.xml.preferences);

		// if the user clicks on the license, then open the display the actual license
		Preference licensePref = findPreference("pref_license");
		licensePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				displayAppLicense();
				return true;
			}
		});

		// if the user clicks on the website link, then open it using an external browser
		Preference websitePref = findPreference("pref_website");
		websitePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				// view the app's website in a web browser
				Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.pref_summary_website)));
				startActivity(browserIntent);
				return true;
			}
		});

		// set the app's version number
		Preference versionPref = findPreference("pref_version");
		versionPref.setSummary(getAppVersion());
	}



	/**
	 * Displays the app's license in an AlertDialog.
	 */
	private void displayAppLicense() {
		new AlertDialog.Builder(getActivity())
				.setMessage(R.string.app_license)
				.setIcon(android.R.drawable.ic_dialog_info)
				.setNeutralButton(R.string.i_agree, null)
				.setCancelable(false)	// do not allow the user to click outside the dialog or press the back button
				.show();
	}



	/**
	 * Returns the version number of this app.
	 *
	 * @return App's version number if everything is ok;  "??" otherwise.
	 */
	private String getAppVersion() {
		String version;

		try {
			PackageInfo pInfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
			version = pInfo.versionName;
		} catch (PackageManager.NameNotFoundException e) {
			Log.i(TAG, "Unable to get the app's version number", e);
			version = "??";
		}

		return version;
	}

}
