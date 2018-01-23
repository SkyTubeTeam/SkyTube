package free.rm.skytube.gui.fragments.preferences;

import android.os.Bundle;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;
import android.widget.Toast;

import java.util.Collection;

import free.rm.skytube.R;
import free.rm.skytube.businessobjects.db.BlockedChannelsDb;

public class BlockedChannelsPreferenceFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preference_blocked_channels);
        MultiSelectListPreference multiSelectListPreference = (MultiSelectListPreference) findPreference(getString(R.string.pref_blocked_channels_list_key));

        final BlockedChannelsDb blockedChannelsDb = BlockedChannelsDb.getBlockedChannelsDb();
        String[] blockedChannelsName = blockedChannelsDb.getBlockedChannelsListName().
                toArray(new String[blockedChannelsDb.getBlockedChannelsListName().size()]);

        multiSelectListPreference.setEntries(blockedChannelsName);
        multiSelectListPreference.setEntryValues(blockedChannelsName);

        multiSelectListPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Toast.makeText(getActivity(), "Please check the channel you want to unblock, leave the others unchecked", Toast.LENGTH_SHORT).show();
                Log.d("", "onPreferenceClick: ");
                return true;
            }
        });

        multiSelectListPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {

                Collection<String> selectedNames = (Collection<String>) o;

                for (String channel : selectedNames) {
                    Log.d("", "onPreferenceChange: " + "for loop");
                    blockedChannelsDb.remove(channel);

                    Toast.makeText(getActivity(), "Please refresh the main page.", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });
    }
}
