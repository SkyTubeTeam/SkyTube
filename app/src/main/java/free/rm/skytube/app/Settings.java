/*
 * SkyTube
 * Copyright (C) 2019 Zsombor Gegesy
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

package free.rm.skytube.app;

import android.content.SharedPreferences;

import androidx.annotation.StringRes;
import androidx.preference.PreferenceManager;

import java.util.Collections;
import java.util.Set;

import free.rm.skytube.R;
import free.rm.skytube.app.enums.Policy;

/**
 * Type safe wrapper to access the various preferences.
 */
public class Settings {
    private final SkyTubeApp app;
    private static final String TUTORIAL_COMPLETED = "YouTubePlayerActivity.TutorialCompleted";
    private static final String LATEST_RELEASE_NOTES_DISPLAYED = "Settings.LATEST_RELEASE_NOTES_DISPLAYED";

    Settings(SkyTubeApp app) {
        this.app = app;
    }

    /**
     * Returns a localised string.
     *
     * @param stringResId String resource ID (e.g. R.string.my_string)
     * @return Localised string, from the strings XML file.
     */
    public String getStr(int stringResId) {
        return app.getString(stringResId);
    }

    public boolean isDownloadToSeparateFolders() {
        return getPreference(R.string.pref_key_download_to_separate_directories,false);
    }

    public Set<String> getHiddenTabs() {
        return getPreference(R.string.pref_key_hide_tabs, Collections.emptySet());
    }

    public Policy getWarningMobilePolicy() {
        String currentValue = getSharedPreferences().getString(getStr(R.string.pref_key_mobile_network_usage_policy),
                getStr(R.string.pref_mobile_network_usage_value_ask));
        return Policy.valueOf(currentValue.toUpperCase());
    }

    public boolean isDisableSearchHistory() {
        return getSharedPreferences().getBoolean(SkyTubeApp.getStr(R.string.pref_key_disable_search_history), false);
    }

    public void setWarningMobilePolicy(Policy warnPolicy) {
        setPreference(R.string.pref_key_mobile_network_usage_policy, warnPolicy.name().toLowerCase());
    }

    public boolean isDisableGestures() {
        return getPreference(R.string.pref_key_disable_screen_gestures, false);
    }

    public void setDisableGestures(boolean disableGestures) {
        setPreference(R.string.pref_key_disable_screen_gestures, disableGestures);
    }

    public boolean isSwitchVolumeAndBrightness() {
        return getPreference(R.string.pref_key_switch_volume_and_brightness, false);
    }

    /**
     * Will check whether the video player tutorial was completed before.  If no, it will return
     * false and will save the value accordingly.
     *
     * @return True if the tutorial was completed in the past.
     */
    public boolean wasTutorialDisplayedBefore() {
        boolean wasTutorialDisplayedBefore = getPreference(TUTORIAL_COMPLETED, false);
        setPreference(TUTORIAL_COMPLETED, true);
        return wasTutorialDisplayedBefore;
    }

    public void showTutorialAgain() {
        setPreference(TUTORIAL_COMPLETED, false);
    }

    /**
     * @return The last time we updated the subscriptions videos feed.  Will return null if the
     * last refresh time is set to -1.
     */
    public Long getFeedsLastUpdateTime() {
        long l = getSharedPreferences().getLong(SkyTubeApp.KEY_SUBSCRIPTIONS_LAST_UPDATED, -1);
        return (l != -1)  ?  l  :  null;
    }

    /**
     * Update the feeds' last update time to the current time.
     */
    public void updateFeedsLastUpdateTime() {
        updateFeedsLastUpdateTime(System.currentTimeMillis());
    }

    /**
     * Update the feed's last update time to dateTime.
     *
     * @param dateTimeInMs  The feed's last update time.  If it is set to null, then -1 will be stored
     *                  to indicate that the app needs to force refresh the feeds...
     */
    public void updateFeedsLastUpdateTime(Long dateTimeInMs) {
        setPreference(SkyTubeApp.KEY_SUBSCRIPTIONS_LAST_UPDATED, dateTimeInMs != null ? dateTimeInMs : -1);
    }

    public void setDownloadFolder(String dir) {
        setPreference(R.string.pref_key_video_download_folder, dir);
    }

    public String getDownloadFolder(String defaultValue) {
        return getPreference(R.string.pref_key_video_download_folder, defaultValue);
    }

    private void setPreference(@StringRes int resId, boolean value) {
        setPreference(getStr(resId), value);
    }

    private void setPreference(String key, boolean value) {
        final SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    private void setPreference(String preferenceName, Long value) {
        final SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putLong(preferenceName, value);
        editor.apply();
    }

    private void setPreference(String preferenceName, String value) {
        final SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putString(preferenceName, value);
        editor.apply();
    }

    private void setPreference(@StringRes int resId, String value) {
        setPreference(getStr(resId), value);
    }

    private String getPreference(@StringRes int resId, String defaultValue) {
        return getSharedPreferences().getString(SkyTubeApp.getStr(resId), defaultValue);
    }

    private boolean getPreference(@StringRes int resId, boolean defaultValue) {
        return getSharedPreferences().getBoolean(SkyTubeApp.getStr(resId), defaultValue);
    }

    private boolean getPreference(String preference, boolean defaultValue) {
        return getSharedPreferences().getBoolean(preference, defaultValue);
    }

    private Set<String> getPreference(@StringRes int resId, Set<String> defaultValue) {
        return getSharedPreferences().getStringSet(SkyTubeApp.getStr(resId), defaultValue);
    }

    private SharedPreferences getSharedPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(app) ;
    }

    public String getDisplayedReleaseNoteTag() {
        return getSharedPreferences().getString(LATEST_RELEASE_NOTES_DISPLAYED, "");
    }

    public void setDisplayedReleaseNoteTag(String newValue) {
        setPreference(LATEST_RELEASE_NOTES_DISPLAYED, newValue);
    }
}
