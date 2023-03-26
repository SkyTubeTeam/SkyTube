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

import static free.rm.skytube.app.SkyTubeApp.getStr;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Environment;

import androidx.annotation.StringRes;
import androidx.preference.PreferenceManager;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import free.rm.skytube.R;
import free.rm.skytube.app.enums.Policy;
import free.rm.skytube.businessobjects.Logger;
import free.rm.skytube.businessobjects.YouTube.VideoStream.VideoQuality;
import free.rm.skytube.businessobjects.YouTube.VideoStream.VideoResolution;
import free.rm.skytube.gui.fragments.MainFragment;
import free.rm.skytube.gui.fragments.SubscriptionsFeedFragment;

/**
 * Type safe wrapper to access the various preferences.
 */
public class Settings {
    private final SkyTubeApp app;
    private static final String TUTORIAL_COMPLETED = "YouTubePlayerActivity.TutorialCompleted";
    private static final String LATEST_RELEASE_NOTES_DISPLAYED = "Settings.LATEST_RELEASE_NOTES_DISPLAYED";
    private static final String FLAG_REFRESH_FEED_FROM_CACHE = "SubscriptionsFeedFragment.FLAG_REFRESH_FEED_FROM_CACHE";
    private static final String FLAG_REFRESH_FEED_FULL = "SubscriptionsFeedFragment.FLAG_REFRESH_FEED_FULL";
    /** Refresh the feed (by querying the YT servers) after 3 hours since the last check. */
    private static final int    REFRESH_TIME_HOURS = 3;
    private static final long   REFRESH_TIME_IN_MS = REFRESH_TIME_HOURS * (1000L*3600L);

    Settings(SkyTubeApp app) {
        this.app = app;
    }

    void migrate() {
        SharedPreferences sharedPreferences = getSharedPreferences();
        migrate(sharedPreferences, "pref_preferred_resolution", R.string.pref_key_maximum_res);
        migrate(sharedPreferences, "pref_preferred_resolution_mobile", R.string.pref_key_maximum_res_mobile);
        migrate(sharedPreferences, "pref_key_video_preferred_resolution", R.string.pref_key_video_download_maximum_resolution);
        setDefault(sharedPreferences, R.string.pref_key_video_quality, VideoQuality.BEST_QUALITY.name());
        setDefault(sharedPreferences, R.string.pref_key_video_quality_for_downloads, VideoQuality.BEST_QUALITY.name());
        setDefault(sharedPreferences, R.string.pref_key_video_quality_on_mobile, VideoQuality.LEAST_BANDWITH.name());
        setDefault(sharedPreferences, R.string.pref_key_use_newer_formats, Build.VERSION.SDK_INT > 16);
        setDefault(sharedPreferences, R.string.pref_key_playback_speed, "1.0");
        Set<String> defaultTabs = new HashSet<>();
        defaultTabs.add(MainFragment.FEATURED_VIDEOS_FRAGMENT);
        setDefault(sharedPreferences, R.string.pref_key_hide_tabs, defaultTabs);
        setDefault(sharedPreferences, R.string.pref_key_default_tab_name, MainFragment.MOST_POPULAR_VIDEOS_FRAGMENT);
    }

    private void migrate(SharedPreferences sharedPreferences, String oldKey, @StringRes int newKey) {
        String oldValue = sharedPreferences.getString(oldKey, null);
        if (oldValue != null) {
            String newKeyStr = app.getString(newKey);
            Logger.i(this, "Migrate %s : %s into %s", oldKey, oldValue, newKeyStr);
            final SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(newKeyStr, oldValue);
            editor.remove(oldKey);
            editor.commit();
        }
    }

    private void setDefault(SharedPreferences sharedPreferences, @StringRes int key, Object defaultValue) {
        String keyStr = app.getString(key);
        if (!sharedPreferences.contains(keyStr)) {
            final SharedPreferences.Editor editor = sharedPreferences.edit();
            Logger.i(this, "Set default %s to %s", keyStr, defaultValue);
            if (defaultValue instanceof String) {
                editor.putString(keyStr, (String) defaultValue);
            } else if (defaultValue instanceof Boolean) {
                editor.putBoolean(keyStr, (Boolean) defaultValue);
            } else if (defaultValue instanceof Set) {
                editor.putStringSet(keyStr, (Set<String>) defaultValue);
            } else {
                throw new IllegalArgumentException("Default value is " + defaultValue + " for " + keyStr);
            }
            editor.commit();
        }
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

    public Set<String> getSponsorblockCategories() {
        String[] defaultFilterList = SkyTubeApp.getStringArray(R.array.sponsorblock_filtering_list_default_values);
        Set<String> filterList = SkyTubeApp.getPreferenceManager().getStringSet("pref_key_sponsorblock_category_list", new HashSet<String>(Arrays.asList(defaultFilterList)));

        return filterList;
    }

    public boolean isSponsorblockEnabled() {
        return SkyTubeApp.getPreferenceManager().getBoolean("pref_key_enable_sponsorblock", false);
    }

    public boolean isUseDislikeApi() {
        return getPreference(R.string.pref_key_use_dislike_api, false);
    }

    public boolean isDownloadToSeparateFolders() {
        return getPreference(R.string.pref_key_download_to_separate_directories,false);
    }

    public Set<String> getHiddenTabs() {
        return getPreference(R.string.pref_key_hide_tabs, Collections.emptySet());
    }

    public Policy getWarningMeteredPolicy() {
        String currentValue = getSharedPreferences().getString(getStr(R.string.pref_key_mobile_network_usage_policy),
                getStr(R.string.pref_metered_network_usage_value_ask));
        return Policy.valueOf(currentValue.toUpperCase());
    }

    public String getPreferredContentCountry() {
        String defaultCountryCode = Locale.getDefault().getCountry();
        String code = getPreference(R.string.pref_key_default_content_country, "");
        Logger.i(this, "Default country code is %s - app selection: %s", defaultCountryCode, code);
        return (code != null && !code.isEmpty()) ? code : defaultCountryCode;
    }

    public boolean isPlaybackStatusEnabled() {
        return !getPreference(R.string.pref_key_disable_playback_status, false);
    }

    /**
     * Gets the policy which defines the desired video resolution by the user in the app preferences.
     *
     * @return Desired {@link StreamSelectionPolicy}.
     */
    public StreamSelectionPolicy getDesiredVideoResolution(boolean forDownload, boolean onMetered) {
        SharedPreferences prefs = getSharedPreferences();
        String maxKey = SkyTubeApp.getStr(forDownload ? R.string.pref_key_video_download_maximum_resolution : R.string.pref_key_maximum_res);
        String maxResIdValue = prefs.getString(maxKey, Integer.toString(VideoResolution.DEFAULT_VIDEO_RES_ID));

        String minKey = SkyTubeApp.getStr(forDownload ? R.string.pref_key_video_download_minimum_resolution : R.string.pref_key_minimum_res);
        String minResIdValue = prefs.getString(minKey, null);

        String qualityKey = SkyTubeApp.getStr(forDownload ? R.string.pref_key_video_quality_for_downloads : R.string.pref_key_video_quality);
        String qualityValue = prefs.getString(qualityKey, null);

        // if on metered network, use the preferred resolution under metered network if defined
        if (onMetered) {
            // default res for mobile network = that of wifi
            maxResIdValue = prefs.getString(SkyTubeApp.getStr(R.string.pref_key_maximum_res_mobile), maxResIdValue);
            minResIdValue = prefs.getString(SkyTubeApp.getStr(R.string.pref_key_minimum_res_mobile), minResIdValue);
            qualityValue = prefs.getString(SkyTubeApp.getStr(R.string.pref_key_video_quality_on_mobile), qualityValue);
        }
        VideoResolution maxResolution = VideoResolution.videoResIdToVideoResolution(maxResIdValue);
        VideoResolution minResolution = VideoResolution.videoResIdToVideoResolution(minResIdValue);
        VideoQuality quality = VideoQuality.valueOf(qualityValue);

        boolean useNewFormats = prefs.getBoolean(SkyTubeApp.getStr(R.string.pref_key_use_newer_formats), false);

        return new StreamSelectionPolicy(!forDownload && useNewFormats, maxResolution, minResolution, quality);
    }

    public StreamSelectionPolicy getDesiredVideoResolution(boolean forDownload) {
        return getDesiredVideoResolution(forDownload, SkyTubeApp.isActiveNetworkMetered());
    }

    public boolean isDisableSearchHistory() {
        return getSharedPreferences().getBoolean(SkyTubeApp.getStr(R.string.pref_key_disable_search_history), false);
    }

    public int getFeedUpdaterInterval() {
        return Integer.parseInt(getPreference(R.string.pref_key_feed_notification, "0"));
    }

    public void setWarningMobilePolicy(Policy warnPolicy) {
        setPreference(R.string.pref_key_mobile_network_usage_policy, warnPolicy.name().toLowerCase());
    }

    public boolean isDisableGestures() {
        return getPreference(R.string.pref_key_disable_screen_gestures, false);
    }

    public boolean isEnableVideoBlocker() {
        return getPreference(R.string.pref_key_enable_video_blocker, true);
    }

    /**
     * @return True if channel deny list is enabled;  false if channel allow list is enabled.
     */
    public boolean isChannelDenyListEnabled() {
        final String defValue = getStr(R.string.channel_blacklisting_filtering);
        final String channelFilter = getPreference(R.string.pref_key_channel_filter_method, defValue);
        return channelFilter.equals(defValue);
    }

    public void setDisableGestures(boolean disableGestures) {
        setPreference(R.string.pref_key_disable_screen_gestures, disableGestures);
    }

    public boolean isSwitchVolumeAndBrightness() {
        return getPreference(R.string.pref_key_switch_volume_and_brightness, false);
    }

    /**
     * Instruct the {@link SubscriptionsFeedFragment} to refresh the subscriptions feed.
     *
     * This might occur due to user subscribing/unsubscribing to a channel.
     */
    public void setRefreshSubsFeedFromCache(boolean flag) {
        setPreference(FLAG_REFRESH_FEED_FROM_CACHE, flag);
    }

    public boolean isRefreshSubsFeedFromCache() {
        return getPreference(FLAG_REFRESH_FEED_FROM_CACHE, false);
    }

    /**
     * Instruct the {@link SubscriptionsFeedFragment} to refresh the subscriptions feed by retrieving
     * any newly published videos from the YT servers.
     *
     * This might occur due to user just imported the subbed channels from YouTube (XML file).
     */
    public void setRefreshSubsFeedFull(boolean flag) {
        setPreference(FLAG_REFRESH_FEED_FULL, flag);
    }

    public boolean isRefreshSubsFeedFull() {
        return getPreference(FLAG_REFRESH_FEED_FULL, false);
    }

    public boolean isFullRefreshTimely() {
        // Only do an automatic refresh of subscriptions if it's been more than three hours since the last one was done.
        Long subscriptionsLastUpdated = getFeedsLastUpdateTime();
        if (subscriptionsLastUpdated == null) {
            return true;
        }
        long threeHoursAgo = System.currentTimeMillis() - REFRESH_TIME_IN_MS;
        return subscriptionsLastUpdated <= threeHoursAgo;
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

    public File getDownloadParentFolder() {
        String parentDirectory = getDownloadFolder(null);
        return parentDirectory != null ? new File(parentDirectory) : Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
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

    public float getDefaultPlaybackSpeed() {
        try {
            return Float.parseFloat(getPreference(R.string.pref_key_playback_speed, "1.0"));
        } catch (NumberFormatException nfe) {
            return 1.0F;
        }
    }
}
