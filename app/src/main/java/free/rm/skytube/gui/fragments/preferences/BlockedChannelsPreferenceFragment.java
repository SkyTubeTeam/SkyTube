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
		final MultiSelectListPreference channelBlacklistPreference = (MultiSelectListPreference) findPreference(getString(R.string.pref_channel_blacklist_key));

		final BlockedChannelsDb blockedChannelsDb = BlockedChannelsDb.getBlockedChannelsDb();

		//Need to have the blocked channels on a separate variable here
		//Otherwise unblocked channels still on blocked channels list.
		String[] blockedChannelsName = getBlockedChannels();

		channelBlacklistPreference.setEntryValues(blockedChannelsName);
		channelBlacklistPreference.setEntries(blockedChannelsName);
		channelBlacklistPreference.setPositiveButtonText(R.string.unblock_button);

		channelBlacklistPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				if (blockedChannelsDb.getBlockedChannelsListName().isEmpty()) {
					Toast.makeText(getActivity(), R.string.channel_blacklist_empty, Toast.LENGTH_LONG).show();
				}
				return true;
			}
		});

		channelBlacklistPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object o) {
				// The selected channels are the channels that user wishes to unblock.
				// Object o gives us a set of blocked channel names.
				Collection<String> selectedChannels = (Collection<String>) o;

				for (String channel : selectedChannels) {
					blockedChannelsDb.remove(channel);
				}

				//We set the last updated version of DB here again
				//So we will not see unblocked channels.
				String[] channels = getBlockedChannels();
				channelBlacklistPreference.setEntryValues(channels);
				channelBlacklistPreference.setEntries(channels);
				
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
