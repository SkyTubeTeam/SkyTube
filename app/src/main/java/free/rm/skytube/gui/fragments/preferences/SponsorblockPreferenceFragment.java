/*
 * SkyTube
 * Copyright (C) 2021
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
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import free.rm.skytube.R;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

/**
 * Sponsorblock preference.
 */
public class SponsorblockPreferenceFragment extends PreferenceFragmentCompat {
	@Override
	public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
		addPreferencesFromResource(R.xml.preference_sponsorblock);

		findPreference("pref_key_enable_sponsorblock").setOnPreferenceChangeListener((preference, newValue) -> {
			Toast.makeText(getActivity(), R.string.setting_updated, Toast.LENGTH_LONG).show();
			return true;
		});

		findPreference("pref_key_sponsorblock_credit").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://sponsor.ajay.app/"));
				startActivity(myIntent);
				return false;
			}
		});
	}
}
