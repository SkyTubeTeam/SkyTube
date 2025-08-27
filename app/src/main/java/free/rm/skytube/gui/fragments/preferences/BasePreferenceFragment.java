/*
 * SkyTube
 * Copyright (C) 2021  Zsombor Gegesy
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

import androidx.preference.PreferenceFragmentCompat;

/**
 * Base class for Preference pages, which wants to act after the preference are saved.
 */
abstract class BasePreferenceFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
    @Override
    public void onCreatePreferences(android.os.Bundle savedInstanceState, String rootKey) {
        if (free.rm.skytube.gui.businessobjects.PinUtils.isPinSet(getContext())) {
            free.rm.skytube.gui.businessobjects.PinUtils.promptForPin(getContext(),
                () -> showPreferencesInternal(rootKey),
                () -> requireActivity().onBackPressed());
        } else {
            showPreferencesInternal(rootKey);
        }
    }

    protected abstract void showPreferencesInternal(String rootKey);

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

}
