package free.rm.skytube.gui.fragments.preferences;

import android.app.Dialog;
import android.os.Bundle;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;

import free.rm.skytube.R;
import free.rm.skytube.businessobjects.db.BlockedChannelsDb;

public class BlockedChannelsPreferenceFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preference_blocked_channels);

        BlockedChannelsDb blockedChannelsDb = BlockedChannelsDb.getBlockedChannelsDb();
        String[] blockedChannelsName = blockedChannelsDb.getBlockedChannelsListName().
                                        toArray(new String[blockedChannelsDb.getBlockedChannelsListName().size()]);

        final MultiSelectListPreference multiSelectListPreference = (MultiSelectListPreference) findPreference(getString(R.string.pref_blocked_channels_list_key));

        multiSelectListPreference.setEntries(blockedChannelsName);
        multiSelectListPreference.setEntryValues(blockedChannelsName);




    }

}
