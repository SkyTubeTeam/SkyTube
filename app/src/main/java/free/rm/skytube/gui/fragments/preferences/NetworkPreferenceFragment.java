package free.rm.skytube.gui.fragments.preferences;

import free.rm.skytube.R;
import free.rm.skytube.businessobjects.YouTube.VideoStream.VideoQuality;
import free.rm.skytube.businessobjects.YouTube.VideoStream.VideoResolution;

public class NetworkPreferenceFragment extends BasePreferenceFragment {
    @Override
    public void onSharedPreferenceChanged(android.content.SharedPreferences sharedPreferences, String key) {
        // No-op
    }

    @Override
    protected void showPreferencesInternal(String rootKey) {
        addPreferencesFromResource(R.xml.preference_downloads);

        // set up the list of available video resolutions
        VideoResolution.setupListPreferences(findPreference(getString(R.string.pref_key_maximum_res)));
        VideoResolution.setupListPreferences(findPreference(getString(R.string.pref_key_minimum_res)));

        // set up the list of available video resolutions on mobile network
        VideoResolution.setupListPreferences(findPreference(getString(R.string.pref_key_maximum_res_mobile)));
        VideoResolution.setupListPreferences(findPreference(getString(R.string.pref_key_minimum_res_mobile)));

        VideoQuality.setupListPreferences(findPreference(getString(R.string.pref_key_video_quality)));
        VideoQuality.setupListPreferences(findPreference(getString(R.string.pref_key_video_quality_on_mobile)));
    }
}
