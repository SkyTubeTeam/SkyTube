package free.rm.skytube.gui.fragments.preferences;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.NonNull;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import free.rm.skytube.BuildConfig;
import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.db.BlockedChannelsDb;
import free.rm.skytube.gui.businessobjects.MultiSelectListPreferenceDialog;
import free.rm.skytube.gui.businessobjects.MultiSelectListPreferenceItem;

/**
 * Video blocker preference.
 */
public class VideoBlockerPreferenceFragment extends PreferenceFragment {

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preference_video_blocker);

		final MultiSelectListPreference channelBlacklistPreference = (MultiSelectListPreference) findPreference(getString(R.string.pref_key_channel_blacklist));
		final Preference.OnPreferenceChangeListener settingUpdatesPreferenceChange = new Preference.OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				Toast.makeText(getActivity(), R.string.setting_updated, Toast.LENGTH_LONG).show();
				return true;
			}
		};

		//Need to have the blocked channels on a separate variable here
		//Otherwise unblocked channels still on blocked channels list.
		String[] blockedChannelsName = getBlockedChannels();

		channelBlacklistPreference.setEntryValues(blockedChannelsName);
		channelBlacklistPreference.setEntries(blockedChannelsName);
		channelBlacklistPreference.setPositiveButtonText(R.string.unblock_button);
		channelBlacklistPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				if (BlockedChannelsDb.getBlockedChannelsDb().getBlockedChannelsListName().isEmpty()) {
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
				final Collection<String> selectedChannels = (Collection<String>) o;

				if (!selectedChannels.isEmpty()) {
					for (String channel : selectedChannels) {
						BlockedChannelsDb.getBlockedChannelsDb().remove(channel);
					}

					//We set the last updated version of DB here again
					//So we will not see unblocked channels.
					String[] channels = getBlockedChannels();
					channelBlacklistPreference.setEntryValues(channels);
					channelBlacklistPreference.setEntries(channels);

					Toast.makeText(getActivity(), R.string.channel_blacklist_updated, Toast.LENGTH_LONG).show();
				}
				
				return true;
			}
		});

		findPreference(getString(R.string.pref_key_preferred_region)).setOnPreferenceChangeListener(settingUpdatesPreferenceChange);

		findPreference(getString(R.string.pref_key_preferred_languages)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				displayPreferredLanguageDialog();
				return true;
			}
		});

		findPreference(getString(R.string.pref_key_lang_detection_video_filtering)).setOnPreferenceChangeListener(settingUpdatesPreferenceChange);
		findPreference(getString(R.string.pref_key_low_views_filter)).setOnPreferenceChangeListener(settingUpdatesPreferenceChange);
		findPreference(getString(R.string.pref_key_dislikes_filter)).setOnPreferenceChangeListener(settingUpdatesPreferenceChange);
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


	/**
	 * Display a dialog which allows the user to select his preferred language(s).
	 */
	private void displayPreferredLanguageDialog() {
		final MultiSelectListPreferenceDialog prefLangDialog = new MultiSelectListPreferenceDialog(getActivity(), getLanguagesAvailable());
		prefLangDialog.title(R.string.pref_title_preferred_languages)
				.positiveText(R.string.ok)
				.onPositive(new MaterialDialog.SingleButtonCallback() {
					@Override
					public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
						final Set<String> preferredLangIsoCodes = prefLangDialog.getSelectedItemsIds();

						// if at least one language was selected, then save the settings
						if (preferredLangIsoCodes.size() > 0) {
							SharedPreferences sharedPref = SkyTubeApp.getPreferenceManager();
							SharedPreferences.Editor editor = sharedPref.edit();
							editor.putStringSet(getString(R.string.pref_key_preferred_languages), prefLangDialog.getSelectedItemsIds());
							editor.apply();

							Toast.makeText(getActivity(), R.string.preferred_lang_updated, Toast.LENGTH_LONG).show();
						} else {
							// no languages were selected... action is ignored
							Toast.makeText(getActivity(), R.string.no_preferred_lang_selected, Toast.LENGTH_LONG).show();
						}
					}
				})
				.negativeText(R.string.cancel)
				.onNegative(new MaterialDialog.SingleButtonCallback() {
					@Override
					public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
						dialog.dismiss();
					}
				})
				.build()
				.show();
	}


	/**
	 * @return A list of languages supported by YouTube - if a language is currently preferred by the
	 * user, the language is ticked/marked.
	 */
	private List<MultiSelectListPreferenceItem> getLanguagesAvailable() {
		List<MultiSelectListPreferenceItem> languagesAvailable = getLanguagesList();
		Set<String>                         preferredLanguages = getPreferredLanguages();

		for (MultiSelectListPreferenceItem language : languagesAvailable) {
			language.isChecked = preferredLanguages.contains(language.id);
		}

		return languagesAvailable;
	}


	/**
	 * @return A list of languages supported by YouTube.
	 */
	private List<MultiSelectListPreferenceItem> getLanguagesList() {
		String[] languagesNames = getResources().getStringArray(R.array.languages_names);
		String[] languagesIsoCodes = getResources().getStringArray(R.array.languages_iso639_codes);
		List<MultiSelectListPreferenceItem> languageAvailableList = new ArrayList<>();

		if (BuildConfig.DEBUG  &&  (languagesNames.length != languagesIsoCodes.length)) {
			throw new AssertionError("languages names array is NOT EQUAL to languages ISO codes array.");
		}

		for (int i = 0;  i < languagesNames.length;  i++) {
			languageAvailableList.add(new MultiSelectListPreferenceItem(languagesIsoCodes[i], languagesNames[i]));
		}

		return languageAvailableList;
	}


	/**
	 * @return A set of languages preferred by the user.
	 */
	private Set<String> getPreferredLanguages() {
		SharedPreferences pref = SkyTubeApp.getPreferenceManager();
		return pref.getStringSet(getString(R.string.pref_key_preferred_languages), getDefaultPreferredLanguages());
	}


	/**
	 * @return The default setting for preferred languages (i.e. no language preference).
	 */
	private Set<String> getDefaultPreferredLanguages() {
		String[] languagesIsoCodes = getResources().getStringArray(R.array.languages_iso639_codes);
		return new HashSet<>(Arrays.asList(languagesIsoCodes));
	}

}
