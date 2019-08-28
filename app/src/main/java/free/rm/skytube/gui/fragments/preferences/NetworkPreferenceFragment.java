package free.rm.skytube.gui.fragments.preferences;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import com.obsez.android.lib.filechooser.ChooserDialog;

import free.rm.skytube.R;
import free.rm.skytube.businessobjects.YouTube.VideoStream.VideoResolution;

public class NetworkPreferenceFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preference_downloads);

        final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        final SharedPreferences.Editor editor = pref.edit();

        final Preference folderChooser = findPreference(getString(R.string.pref_key_video_download_folder));
        folderChooser.setOnPreferenceClickListener(preference -> {
            new ChooserDialog(NetworkPreferenceFragment.this)
                    .withFilter(true, false)
                    .titleFollowsDir(true)
                    .enableOptions(true)
                    .withResources(R.string.pref_popup_title_video_download_folder, R.string.ok, R.string.cancel)
                    .withChosenListener((dir, dirFile) -> {

                        editor.putString(getString(R.string.pref_key_video_download_folder), dir);
                        editor.apply();
                        folderChooser.setSummary(getString(R.string.pref_summary_video_download_folder, dir));
                    })
                    .build()
                    .show();
            return true;
        });
        String dir = pref.getString(getString(R.string.pref_key_video_download_folder), null);
        folderChooser.setSummary(getString(R.string.pref_summary_video_download_folder, dir != null ? dir : Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)));

        ListPreference videoResolutionPref = (ListPreference)findPreference(getString(R.string.pref_key_video_download_preferred_resolution));
        String preferredVideoResolution = pref.getString(getString(R.string.pref_key_video_download_preferred_resolution), null);
        String preferredVideoResolutionName = preferredVideoResolution == null ? "" : VideoResolution.getAllVideoResolutionsNames()[Integer.parseInt(preferredVideoResolution)];
        videoResolutionPref.setSummary(getString(R.string.pref_video_download_preferred_resolution_summary, preferredVideoResolutionName));
        VideoResolution.setupListPreferences(videoResolutionPref);

        // set up the list of available video resolutions on mobile network
        VideoResolution.setupListPreferences ((ListPreference) findPreference(getString(R.string.pref_key_preferred_res_mobile)));

    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key != null) {
            if (key.equals(getString(R.string.pref_key_video_download_preferred_resolution))) {
                ListPreference videoResolutionPref = (ListPreference)findPreference(getString(R.string.pref_key_video_download_preferred_resolution));
                videoResolutionPref.setSummary(getString(R.string.pref_video_download_preferred_resolution_summary, videoResolutionPref.getEntry()));
            }
        }
    }
            @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }



    @Override
    public void onPause() {
        super.onPause();
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

}
