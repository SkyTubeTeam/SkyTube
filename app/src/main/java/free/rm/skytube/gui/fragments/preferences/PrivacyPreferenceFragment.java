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

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import free.rm.skytube.R;
import free.rm.skytube.businessobjects.db.PlaybackStatusDb;
import free.rm.skytube.businessobjects.db.SearchHistoryDb;

public class PrivacyPreferenceFragment extends BasePreferenceFragment {
	@Override
	public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
		addPreferencesFromResource(R.xml.preference_privacy);

		Preference clearPlaybackStatus = findPreference(getString(R.string.pref_key_clear_playback_status));
		clearPlaybackStatus.setOnPreferenceClickListener(preference -> {
			PlaybackStatusDb.getPlaybackStatusDb().deleteAllPlaybackHistory();
			Toast.makeText(getActivity(), getString(R.string.pref_playback_status_cleared), Toast.LENGTH_LONG).show();
			return true;
		});
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals(getString(R.string.pref_key_disable_search_history))) {
			CheckBoxPreference disableSearchHistoryPreference = findPreference(key);
			// If Search History is disabled, clear the Search History database.
			if(disableSearchHistoryPreference.isChecked()) {
				SearchHistoryDb.getSearchHistoryDb().deleteAllSearchHistory();
				Toast.makeText(getActivity(), getString(R.string.pref_disable_search_history_deleted), Toast.LENGTH_LONG).show();
			}
		} else if (key.equals(getString(R.string.pref_key_disable_playback_status))) {
			CheckBoxPreference disablePlaybackStatusPreference = findPreference(key);
			if(disablePlaybackStatusPreference.isChecked()) {
				PlaybackStatusDb.getPlaybackStatusDb().deleteAllPlaybackHistory();
				Toast.makeText(getActivity(), getString(R.string.pref_disable_playback_status_deleted), Toast.LENGTH_LONG).show();
			}
		}
	}
}
