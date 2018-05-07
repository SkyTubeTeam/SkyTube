package free.rm.skytube.gui.fragments.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.NonNull;
import android.text.InputType;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.channel.ChannelInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import free.rm.skytube.BuildConfig;
import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.AsyncTaskParallel;
import free.rm.skytube.businessobjects.Logger;
import free.rm.skytube.businessobjects.YouTube.VideoStream.HttpDownloader;
import free.rm.skytube.businessobjects.db.ChannelFilteringDb;
import free.rm.skytube.gui.businessobjects.MultiSelectListPreferenceDialog;
import free.rm.skytube.gui.businessobjects.MultiSelectListPreferenceItem;
import free.rm.skytube.gui.businessobjects.SkyTubeMaterialDialog;

/**
 * Video blocker preference.
 */
public class VideoBlockerPreferenceFragment extends PreferenceFragment {

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preference_video_blocker);

		final MultiSelectListPreference channelBlacklistPreference = (MultiSelectListPreference) findPreference(getString(R.string.pref_key_channel_blacklist));
		final Preference channelWhitelistPreference = findPreference(getString(R.string.pref_key_channel_whitelist));

		// initialize the channel filtering UI
		initChannelFilteringPreferences(channelBlacklistPreference, channelWhitelistPreference);
		findPreference(getString(R.string.pref_key_channel_filter_method)).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				initChannelFilteringPreferences((String) newValue, channelBlacklistPreference, channelWhitelistPreference);
				return true;
			}
		});

		final Preference.OnPreferenceChangeListener settingUpdatesPreferenceChange = new Preference.OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				Toast.makeText(getActivity(), R.string.setting_updated, Toast.LENGTH_LONG).show();
				return true;
			}
		};


		findPreference(getString(R.string.pref_key_preferred_region)).setOnPreferenceChangeListener(settingUpdatesPreferenceChange);
		findPreference(getString(R.string.pref_key_preferred_languages)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				new PreferredLanguageDialog(getActivity()).show();
				return true;
			}
		});
		findPreference(getString(R.string.pref_key_lang_detection_video_filtering)).setOnPreferenceChangeListener(settingUpdatesPreferenceChange);
		findPreference(getString(R.string.pref_key_low_views_filter)).setOnPreferenceChangeListener(settingUpdatesPreferenceChange);
		findPreference(getString(R.string.pref_key_dislikes_filter)).setOnPreferenceChangeListener(settingUpdatesPreferenceChange);
	}


	/**
	 * Initialized the channel filtering preference.
	 *
	 * @param channelBlacklistPreference    {@link Preference} for channel blacklisting.
	 * @param channelWhitelistPreference    {@link Preference} for channel whitelisting.
	 */
	private void initChannelFilteringPreferences(MultiSelectListPreference channelBlacklistPreference, Preference channelWhitelistPreference) {
		// get the current channel filtering method selected by the user...
		final String channelFilter = SkyTubeApp.getPreferenceManager().getString(getString(R.string.pref_key_channel_filter_method), getString(R.string.channel_blacklisting_filtering));
		initChannelFilteringPreferences(channelFilter, channelBlacklistPreference, channelWhitelistPreference);
	}


	/**
	 * Initialized the channel filtering preference.
	 *
	 * @param channelFilter                 The current channel filtering method (e.g. "Channel Blacklisting").
	 * @param channelBlacklistPreference    {@link Preference} for channel blacklisting.
	 * @param channelWhitelistPreference    {@link Preference} for channel whitelisting.
	 */
	private void initChannelFilteringPreferences(String channelFilter, MultiSelectListPreference channelBlacklistPreference, Preference channelWhitelistPreference) {
		if (channelFilter.equals(getString(R.string.channel_blacklisting_filtering))) {
			initChannelBlacklistingPreference(channelBlacklistPreference);
			channelBlacklistPreference.setEnabled(true);
			channelWhitelistPreference.setEnabled(false);
		} else if (channelFilter.equals(getString(R.string.channel_whitelisting_filtering))) {
			initChannelWhitelistingPreference(channelWhitelistPreference);
			channelBlacklistPreference.setEnabled(false);
			channelWhitelistPreference.setEnabled(true);
		} else {
			Logger.e(this, "Unknown channel filtering preference", channelFilter);
		}
	}


	/**
	 * Initialized the channel blacklist preference.
	 */
	private void initChannelBlacklistingPreference(final MultiSelectListPreference channelBlacklistPreference) {
		//Need to have the blocked channels on a separate variable here
		//Otherwise unblocked channels still on blocked channels list.
		String[] blockedChannelsName = getBlacklistedChannelNames();

		channelBlacklistPreference.setEntryValues(blockedChannelsName);
		channelBlacklistPreference.setEntries(blockedChannelsName);
		channelBlacklistPreference.setPositiveButtonText(R.string.unblock_button);
		channelBlacklistPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				if (ChannelFilteringDb.getChannelFilteringDb().getBlacklistedChannelNamesList().isEmpty()) {
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
						ChannelFilteringDb.getChannelFilteringDb().unblacklist(channel);
					}

					//We set the last updated version of DB here again
					//So we will not see unblocked channels.
					String[] channels = getBlacklistedChannelNames();
					channelBlacklistPreference.setEntryValues(channels);
					channelBlacklistPreference.setEntries(channels);

					Toast.makeText(getActivity(), R.string.channel_blacklist_updated, Toast.LENGTH_LONG).show();
				}

				return true;
			}
		});
	}


	/**
	 * @return A list of blacklisted channel names.
	 */
	private String[] getBlacklistedChannelNames() {
		final List<String> channelNames = ChannelFilteringDb.getChannelFilteringDb().getBlacklistedChannelNamesList();
		return channelNames.toArray(new String[channelNames.size()]);
	}


	/**
	 * Initialized the channel whitelist preference.
	 */
	private void initChannelWhitelistingPreference(final Preference channelWhitelistPreference) {
		channelWhitelistPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				new WhitelistChannelsDialog(getActivity()).show();
				return true;
			}
		});
	}



	////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Display a dialog which allows the user to select his preferred language(s).
	 */
	private class PreferredLanguageDialog extends MultiSelectListPreferenceDialog {

		public PreferredLanguageDialog(@NonNull Context context) {
			super(context);

			setItems(getLanguagesAvailable());

			title(R.string.pref_title_preferred_languages);
			onPositive(new MaterialDialog.SingleButtonCallback() {
				@Override
				public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
					final Set<String> preferredLangIsoCodes = getSelectedItemsIds();

					// if at least one language was selected, then save the settings
					if (preferredLangIsoCodes.size() > 0) {
						SharedPreferences sharedPref = SkyTubeApp.getPreferenceManager();
						SharedPreferences.Editor editor = sharedPref.edit();
						editor.putStringSet(getString(R.string.pref_key_preferred_languages), getSelectedItemsIds());
						editor.apply();

						Toast.makeText(getActivity(), R.string.preferred_lang_updated, Toast.LENGTH_LONG).show();
					} else {
						// no languages were selected... action is ignored
						Toast.makeText(getActivity(), R.string.no_preferred_lang_selected, Toast.LENGTH_LONG).show();
					}
				}
			});
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



	//////////////////////////

	/**
	 * Display a dialog which allows the user to whitelist channels.
	 */
	private class WhitelistChannelsDialog extends MultiSelectListPreferenceDialog implements OnGetChannelInfoListener {

		public WhitelistChannelsDialog(@NonNull Context context) {
			super(context);

			// set the items of this list as the whitelisted channels
			setItems(ChannelFilteringDb.getChannelFilteringDb().getWhitelistedChannels());

			// when the "Add" button is clicked, do not close this dialog...
			autoDismiss(false);

			title(R.string.pref_title_channel_whitelist);
			positiveText(R.string.block);
			onPositive(new MaterialDialog.SingleButtonCallback() {
				@Override
				public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
					final List<MultiSelectListPreferenceItem> channels = getSelectedItems();

					if (channels != null  &&  !channels.isEmpty()) {
						// unwhitelist the selected channels
						final boolean success = ChannelFilteringDb.getChannelFilteringDb().unwhitelist(channels);

						Toast.makeText(getActivity(),
								success ? R.string.channel_unwhitelist_success : R.string.channel_unwhitelist_failure,
								Toast.LENGTH_LONG)
								.show();
					}

					dialog.dismiss();
				}
			});

			neutralText(R.string.add);
			onNeutral(new MaterialDialog.SingleButtonCallback() {
				@Override
				public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
					displayInputChannelUrlDialog();
				}
			});
		}


		/**
		 * Display a dialog which allows the user to input the channel's URL that he wished to be
		 * whitelisted.
		 */
		private void displayInputChannelUrlDialog() {
			new SkyTubeMaterialDialog(getActivity())
					.autoDismiss(true)
					.content(R.string.input_channel_url)
					.positiveText(R.string.add)
					.inputType(InputType.TYPE_TEXT_VARIATION_URI)
					.input("https://www.youtube.com/channel/xxxxxxxxx", null, false, new MaterialDialog.InputCallback() {
						@Override
						public void onInput(@NonNull MaterialDialog dialog, CharSequence channelUrl) {
							new GetChannelIdFromUrlTask(channelUrl.toString(), WhitelistChannelsDialog.this).executeInParallel();
						}
					})
					.show();
		}


		@Override
		public void onChannelInfo(MultiSelectListPreferenceItem channel) {
			if (channel != null  &&  channel.id != null  &&  !channel.id.isEmpty()) {
				// add item to the dialog's adapter
				if (!addItem(channel)) {
					Toast.makeText(getActivity(), R.string.channel_already_whitelisted, Toast.LENGTH_LONG).show();
				} else {
					// save the whitelisted channel in the database
					final boolean success = ChannelFilteringDb.getChannelFilteringDb().whitelist(channel.id, channel.text);

					Toast.makeText(getActivity(),
							success ? R.string.channel_whitelist_updated : R.string.channel_whitelist_update_failure,
							Toast.LENGTH_LONG)
							.show();
				}
			}
		}

	}


	/**
	 * A task that given a channel URL will return the channel's ID.
	 */
	private class GetChannelIdFromUrlTask extends AsyncTaskParallel<Void, Void, MultiSelectListPreferenceItem> {

		private String                      channelUrl;
		private OnGetChannelInfoListener    onGetChannelInfoListener;
		private MaterialDialog              getChannelInfoDialog;


		public GetChannelIdFromUrlTask(String channelUrl, OnGetChannelInfoListener onGetChannelInfoListener) {
			this.channelUrl = channelUrl;
			this.onGetChannelInfoListener = onGetChannelInfoListener;
		}


		@Override
		protected void onPreExecute() {
			getChannelInfoDialog = new SkyTubeMaterialDialog(getActivity())
					.progress(true, 0)
					.content(R.string.please_wait)
					.positiveText("")
					.negativeText("")
					.show();
		}

		@Override
		protected MultiSelectListPreferenceItem doInBackground(Void... v) {
			MultiSelectListPreferenceItem channel = null;

			try {
				NewPipe.init(new HttpDownloader());
				StreamingService youtubeService = ServiceList.YouTube.getService();

				ChannelInfo channelInfo = ChannelInfo.getInfo(youtubeService, channelUrl);
				channel = new MultiSelectListPreferenceItem(channelInfo.getId(), channelInfo.getName(), false);

			} catch (Throwable tr) {
				Logger.e(this, "An error occurred while getting channel ID", tr);
			}

			return channel;
		}


		@Override
		protected void onPostExecute(MultiSelectListPreferenceItem channel) {
			onGetChannelInfoListener.onChannelInfo(channel);
			getChannelInfoDialog.dismiss();
			getChannelInfoDialog = null;
		}

	}


	/**
	 * To be called once the channel info is retrieved from the given channel's URL.
	 */
	private interface OnGetChannelInfoListener {
		void onChannelInfo(MultiSelectListPreferenceItem channel);
	}

}
