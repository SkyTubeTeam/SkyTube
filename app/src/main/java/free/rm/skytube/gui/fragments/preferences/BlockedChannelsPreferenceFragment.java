package free.rm.skytube.gui.fragments.preferences;

import android.os.Bundle;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.widget.Toast;
import java.util.Collection;
import free.rm.skytube.R;
import free.rm.skytube.businessobjects.db.BlockedChannelsDb;

public class BlockedChannelsPreferenceFragment extends PreferenceFragment {

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preference_blocked_channels);
        final MultiSelectListPreference multiSelectListPreference = (MultiSelectListPreference) findPreference(getString(R.string.pref_key_blocked_channels_list));

        final BlockedChannelsDb blockedChannelsDb = BlockedChannelsDb.getBlockedChannelsDb();

        //Need to have the blocked channels on a separate variable here
        //Otherwise unblocked channels still on blocked channels list.
        String[] blockedChannelsName = getBlockedChannels();

        multiSelectListPreference.setEntryValues(blockedChannelsName);
        multiSelectListPreference.setEntries(blockedChannelsName);
        multiSelectListPreference.setPositiveButtonText(R.string.unblock_button);

        multiSelectListPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
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

                for (String channel : selectedNames) {
                    blockedChannelsDb.remove(channel);
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
