package free.rm.skytube.gui.fragments.preferences;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.obsez.android.lib.filechooser.ChooserDialog;

import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.YouTube.VideoStream.VideoResolution;

public class NetworkPreferenceFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preference_downloads);

        final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        final SharedPreferences.Editor editor = pref.edit();

        final Preference folderChooser = findPreference(getString(R.string.pref_key_video_download_folder));
        folderChooser.setOnPreferenceClickListener(preference -> {
            new ChooserDialog(requireActivity())
                    .withFilter(true, false)
                    .titleFollowsDir(true)
                    .enableOptions(true)
                    .withResources(R.string.pref_popup_title_video_download_folder, R.string.ok, R.string.cancel)
                    .withChosenListener((dir, dirFile) -> {
                        SkyTubeApp.getSettings().setDownloadFolder(dir);
                        folderChooser.setSummary(getString(R.string.pref_summary_video_download_folder, dir));
                    })
                    .build()
                    .show();
            return true;
        });
        String dir = SkyTubeApp.getSettings().getDownloadFolder(null);
        folderChooser.setSummary(getString(R.string.pref_summary_video_download_folder, dir != null ? dir : Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)));

        VideoResolution.setupListPreferences ((ListPreference) findPreference(getString(R.string.pref_key_video_download_maximum_resolution)));
        VideoResolution.setupListPreferences ((ListPreference) findPreference(getString(R.string.pref_key_video_download_minimum_resolution)));

        // set up the list of available video resolutions on mobile network
        VideoResolution.setupListPreferences ((ListPreference) findPreference(getString(R.string.pref_key_maximum_res_mobile)));
        VideoResolution.setupListPreferences ((ListPreference) findPreference(getString(R.string.pref_key_minimum_res_mobile)));
    }

}
