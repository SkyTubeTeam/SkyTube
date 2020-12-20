package free.rm.skytube.gui.fragments;

import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.BuildCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import free.rm.skytube.R;

public class ThemePreferenceFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preference_theme);

        Preference preference = findPreference(getString(R.string.pref_key_night));
        preference.setOnPreferenceChangeListener((preference1, newValue) -> {

            if (newValue.equals(getString(R.string.pref_night_on))) {
                updateTheme(AppCompatDelegate.MODE_NIGHT_YES);
            }
            else if (newValue.equals(getString(R.string.pref_night_off))) {
                updateTheme(AppCompatDelegate.MODE_NIGHT_NO);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY);
            }

            return true;
        });
    }

    private boolean updateTheme(int nightMode) {

        AppCompatDelegate.setDefaultNightMode(nightMode);
        requireActivity().recreate();
        return true;
    }

}
