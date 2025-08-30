/*
 * SkyTube
 * Copyright (C) 2025
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
import android.text.InputFilter;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;

import androidx.preference.CheckBoxPreference;
import androidx.preference.EditTextPreference;

import java.util.regex.Pattern;

import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;

public class SecurityPreferenceFragment extends BasePreferenceFragment {
    private EditTextPreference securityPinSetting;
    private CheckBoxPreference securityPinToggle;
    private final static Pattern PIN_CODE = Pattern.compile("\\d*");

    @Override
    protected void showPreferencesInternal(String rootKey) {
        addPreferencesFromResource(R.xml.preference_security);

        securityPinToggle = findPreference(getString(R.string.pref_key_require_pin_code));
        securityPinSetting = findPreference(getString(R.string.pref_key_security_pin));
//        securityPinSetting.setOnPreferenceChangeListener((pref, newValue) -> {
//            return newValue != null && PIN_CODE.matcher(newValue.toString()).matches();
//        });
        securityPinSetting.setOnBindEditTextListener(editText -> {
            editText.setFilters(new InputFilter[]{ new NumericInputFilter() } );
        });

        boolean pinSet = SkyTubeApp.getSettings().isPinSet();
        securityPinToggle.setChecked(pinSet);
        securityPinSetting.setEnabled(pinSet);
        updateSecurityPinLabels(pinSet);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (getString(R.string.pref_key_require_pin_code).equals(key)) {
            boolean toggleChecked = securityPinToggle.isChecked();
            securityPinSetting.setEnabled(toggleChecked);
            if (!toggleChecked) {
                SkyTubeApp.getSettings().removePin();
                updateSecurityPinLabels(false);
            }
        } else if (getString(R.string.pref_key_security_pin).equals(key)) {
            boolean pinSet = SkyTubeApp.getSettings().isPinSet();
            updateSecurityPinLabels(pinSet);
        }
    }

    private void updateSecurityPinLabels(boolean alreadySet) {
        if (alreadySet) {
            securityPinSetting.setTitle(R.string.pref_title_change_security_pin);
            securityPinSetting.setSummary(R.string.pref_summary_change_security_pin);
        } else {
            securityPinSetting.setTitle(R.string.pref_title_set_security_pin);
            securityPinSetting.setSummary(R.string.pref_summary_set_security_pin);
        }
    }

    class NumericInputFilter implements InputFilter {
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            boolean keepOriginal = true;
            StringBuilder sb = new StringBuilder(end - start);
            for (int i = start; i < end; i++) {
                char c = source.charAt(i);
                if (isCharAllowed(c)) {
                    sb.append(c);
                } else {
                    keepOriginal = false;
                }
            }
            if (keepOriginal)
                return null;
            else {
                if (source instanceof Spanned) {
                    SpannableString sp = new SpannableString(sb);
                    TextUtils.copySpansFrom((Spanned) source, start, sb.length(), null, sp, 0);
                    return sp;
                } else {
                    return sb;
                }
            }
        }

        private boolean isCharAllowed(char c) {
            return Character.isDigit(c);
        }
    }

}
