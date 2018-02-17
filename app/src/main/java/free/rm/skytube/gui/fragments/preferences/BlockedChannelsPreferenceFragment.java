package free.rm.skytube.gui.fragments.preferences;

import android.os.Bundle;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;
import android.widget.Toast;

import java.util.Arrays;
import java.util.Collection;

import free.rm.skytube.R;
import free.rm.skytube.businessobjects.db.BlockedChannelsDb;

public class BlockedChannelsPreferenceFragment extends PreferenceFragment {


    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preference_blocked_channels);
        final MultiSelectListPreference multiSelectListPreference = (MultiSelectListPreference) findPreference(getString(R.string.pref_blocked_channels_list_key));

        final BlockedChannelsDb blockedChannelsDb = BlockedChannelsDb.getBlockedChannelsDb();

         String[] blockedChannelsName = blockedChannelsDb.getBlockedChannelsListName().
                toArray(new String[blockedChannelsDb.getBlockedChannelsListName().size()]);


        multiSelectListPreference.setEntryValues(blockedChannelsName);
        multiSelectListPreference.setEntries(blockedChannelsName);
        multiSelectListPreference.setPositiveButtonText(R.string.unblock_button);

        multiSelectListPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                Log.d("BLOCKED CHANNELS", "onPreferenceClick: " + Arrays.toString(getBlockedChannels()));

                if (blockedChannelsDb.getBlockedChannelsListName().isEmpty()) {
                    Toast.makeText(getActivity(), "There is not any blocked channel.", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });

        multiSelectListPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {


                //selected names are the channels that user chooses
                //Object o gives us a set of blocked channel names
                Collection<String> selectedNames = (Collection<String>) o;

                Log.d("SELECTED NAMES", "onPreferenceChange: " + selectedNames.toString());
                for (String channel : selectedNames) {
                    Log.wtf("CHANNEL NAME", "onPreferenceChange: " +channel + "");
                    boolean removed = blockedChannelsDb.remove(channel);
                    Log.wtf("REMOVED", "onPreferenceChange: " + removed +"");
                }

                //We set the last updated version of DB here again
                //So we will not see unblocked channels.
                String[] channels = getBlockedChannels();
                multiSelectListPreference.setEntryValues(channels);
                multiSelectListPreference.setEntries(channels);
                return true;
            }
        });
    }

    /**
     * Method to get updated version of blocked channels
     *
     * @return updated version of blocked channels.
     */
    private String[] getBlockedChannels() {
        final BlockedChannelsDb blockedChannelsDb = BlockedChannelsDb.getBlockedChannelsDb();
        return blockedChannelsDb.getBlockedChannelsListName().
                toArray(new String[blockedChannelsDb.getBlockedChannelsListName().size()]);
    }
}
