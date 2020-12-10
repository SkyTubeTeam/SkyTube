package free.rm.skytube.gui.businessobjects;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.text.SpannableString;
import android.text.util.Linkify;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.text.util.LinkifyCompat;
import androidx.fragment.app.Fragment;

import com.afollestad.materialdialogs.MaterialDialog;
import com.obsez.android.lib.filechooser.ChooserDialog;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.Logger;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.db.DatabaseTasks;
import free.rm.skytube.businessobjects.db.SubscriptionsDb;
import free.rm.skytube.gui.businessobjects.adapters.SubsAdapter;
import free.rm.skytube.gui.businessobjects.preferences.BackupDatabases;
import free.rm.skytube.gui.fragments.SubscriptionsFeedFragment;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * Custom class to handle Backups and Subscriptions imports. This class must be instantiated using either a native Fragment
 * or the support library (v4) Fragment. That Fragment must then override onRequestPermissionsResult and call the same method
 * in this class in order to pass the permission request result on to this class for handling.
 */
public class SubscriptionsBackupsManager {
	private final Activity activity;
	private final Fragment fragment;
	private static final int EXT_STORAGE_PERM_CODE_BACKUP = 1950;
	private static final int EXT_STORAGE_PERM_CODE_IMPORT = 1951;
	private static final int IMPORT_SUBSCRIPTIONS_READ_CODE = 42;
	private static final String TAG = SubscriptionsBackupsManager.class.getSimpleName();
	private boolean isUnsubsribeAllChecked = false;

	private final CompositeDisposable compositeDisposable = new CompositeDisposable();

	public SubscriptionsBackupsManager(Activity activity, Fragment fragment) {
		this.activity = activity;
		this.fragment = fragment;
	}

	public void clearBackgroundTasks() {
		compositeDisposable.clear();
	}

	/**
	 * Backup the databases.
	 */
	public void backupDatabases() {
		// if the user has granted us access to the external storage, then perform the backup
		// operation
		if (hasAccessToExtStorage(EXT_STORAGE_PERM_CODE_BACKUP)) {
			Toast.makeText(activity, R.string.databases_backing_up, Toast.LENGTH_SHORT).show();
			compositeDisposable.add(
					Single.fromCallable(() -> new BackupDatabases().backupDbsToSdCard())
							.subscribeOn(Schedulers.io())
							.observeOn(AndroidSchedulers.mainThread())
							.onErrorReturn(throwable -> {
								Log.e(TAG, "Unable to backup the databases...", throwable);
								return "";
							})
							.subscribe(backupPath -> {
								String message = (!backupPath.isEmpty()) ?
										String.format(activity.getString(R.string.databases_backup_success),
												backupPath) :
										activity.getString(R.string.databases_backup_fail);

								new AlertDialog.Builder(activity)
										.setMessage(message)
										.setNeutralButton(R.string.ok, null)
										.show();
							})
			);
		}
	}


	/**
	 * Display file picker to be used by the user to select the BACKUP (database) or
	 * YOUTUBE SUBS (xml file) to import.
	 */
	public void displayFilePicker() {
		displayFilePicker(true);
	}


	/**
	 * Display file picker to be used by the user to select the BACKUP (database) or
	 * YOUTUBE SUBS (xml file) to import.
	 *
	 * @param importDb  If set to true, the app will import (previously backed-up) database;
	 *                  Otherwise, it will import YouTube subs (xml file).
	 */
	private void displayFilePicker(final boolean importDb) {
		// do not display the file picker until the user gives us access to the external storage
		if (!hasAccessToExtStorage(importDb ? EXT_STORAGE_PERM_CODE_IMPORT : IMPORT_SUBSCRIPTIONS_READ_CODE))
			return;

		ChooserDialog dialog = new ChooserDialog(activity)
				.withStartFile(importDb ? Environment.getExternalStorageDirectory().getAbsolutePath() : Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath())
				.displayPath(true)
				.withChosenListener((file, dirFile) -> {
					if (importDb)
						displayImportDbsBackupWarningMsg(file);
					else {
						Uri uri = Uri.fromFile(new File(file));
						parseImportedSubscriptions(uri);
					}
				})
				.withOnCancelListener(DialogInterface::cancel);
		if(importDb) {
			dialog.withFilter(false, false, "skytube");
		} else {
			dialog.withFilterRegex(false, false, ".*(xml|subscription_manager)$");
		}
		dialog.build().show();
	}

	/**
	 * Check if the app has access to the external storage.  If not, ask the user whether he wants
	 * to give us access...
	 *
	 * @param permissionRequestCode The request code (either EXT_STORAGE_PERM_CODE_BACKUP or
	 *                              EXT_STORAGE_PERM_CODE_IMPORT) which is used by
	 *                              {onRequestPermissionsResult(int, String[], int[])} to
	 *                              determine whether we are going to backup (export) or to import.
	 *
	 * @return True if the user has given access to write to the external storage in the past;
	 * false otherwise.
	 */
	private boolean hasAccessToExtStorage(int permissionRequestCode) {
		boolean hasAccessToExtStorage = true;

		// if the user has not yet granted us access to the external storage...
		if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
			// We can request the permission (to the users).  If the user grants us access (or
			// otherwise), then the method #onRequestPermissionsResult() will be called.
			fragment.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
					permissionRequestCode);

			hasAccessToExtStorage = false;
		}

		return hasAccessToExtStorage;
	}

	/**
	 * Display import database warning:  i.e. all the current data will be replaced by that of the
	 * import file.
	 *
	 * @param backupFilePath    The backup file to import.
	 */
	private void displayImportDbsBackupWarningMsg(final String backupFilePath) {
		new AlertDialog.Builder(activity)
						.setMessage(R.string.databases_import_warning_message)
						.setPositiveButton(R.string.continue_, (dialog, which) -> importDatabases(backupFilePath))
						.setNegativeButton(R.string.cancel, null)
						.show();
	}

	/**
	 * Imports the subscription and bookmark databases.
	 */
	private void importDatabases(String backupFilePath) {
		Toast.makeText(activity, R.string.databases_importing, Toast.LENGTH_SHORT).show();

		compositeDisposable.add(
				Single.fromCallable(() -> {
					BackupDatabases backupDatabases = new BackupDatabases();
					backupDatabases.importBackupDb(backupFilePath);
					return true;
				})
						.subscribeOn(Schedulers.io())
						.observeOn(AndroidSchedulers.mainThread())
						.onErrorReturn(throwable -> {
							Log.e(TAG, "Unable to import the databases...", throwable);
							return false;
						})
						.subscribe(successfulImport -> {
							// We need to force the app to refresh the subscriptions feed when the app is
							// restarted (irrespective to when the feeds were last refreshed -- which could be
							// during the last 5 mins).  This is as we are loading new databases...
							SkyTubeApp.getSettings().updateFeedsLastUpdateTime(null);

							// ask the user to restart the app
							new AlertDialog.Builder(activity)
									.setCancelable(false)
									.setMessage(successfulImport ? R.string.databases_import_success : R.string.databases_import_fail)
									.setNeutralButton(R.string.restart, (dialog, which) -> SkyTubeApp.restartApp())
									.show();
						})
		);
	}

	/**
	 * Parse the XML file that the user selected to import subscriptions from. Each channel contained in the XML
	 * that the user is not already subscribed to will appear in a dialog, to allow the user to select individual channels
	 * to subscribe to, via a new Dialog. Once the user chooses to import the selected channels via the Import Subscriptions
	 * button, {@link #subscribeToImportedChannels(List)} will be executed with a list of the selected channels.
	 *
	 * @param uri The URI pointing to the XML file containing YouTube Channels to subscribe to.
	 */
	private void parseImportedSubscriptions(Uri uri) {
		try {
			final List<MultiSelectListPreferenceItem> channels = new ArrayList<>();
			Pattern channelPattern = Pattern.compile(".*channel_id=([^&]+)");
			Matcher matcher;
			XmlPullParserFactory xmlFactoryObject = XmlPullParserFactory.newInstance();
			XmlPullParser myParser = xmlFactoryObject.newPullParser();
			myParser.setInput(activity.getContentResolver().openInputStream(uri), null);
			int event = myParser.getEventType();
			// If channels are found in the XML file but they are all already subscribed to, alert the user with a different
			// message than if no channels were found at all.
			boolean foundChannels = false;
			while (event != XmlPullParser.END_DOCUMENT) {
				String name=myParser.getName();
				switch (event) {
					case XmlPullParser.START_TAG:
						break;

					case XmlPullParser.END_TAG:
						if(name.equals("outline")){
							String xmlUrl = myParser.getAttributeValue(null,"xmlUrl");
							if(xmlUrl != null) {
								matcher = channelPattern.matcher(xmlUrl);
								if(matcher.matches()) {
									foundChannels = true;
									String channelId = matcher.group(1);
									String channelName = myParser.getAttributeValue(null, "title");
									if(channelId != null && !SubscriptionsDb.getSubscriptionsDb().isUserSubscribedToChannel(channelId)) {
										channels.add(new MultiSelectListPreferenceItem(channelId, channelName));
									}
								}

							}
						}
						break;

				}
				event = myParser.next();
			}

			if(channels.size() > 0) {
				// display a dialog which allows the user to select the channels to import
				new MultiSelectListPreferenceDialog(activity, channels)
						.title(R.string.import_subscriptions)
						.positiveText(R.string.import_subscriptions)
						.onPositive((dialog, which) -> {
							// if the user checked the "Unsubscribe to all subscribed channels" checkbox
							if (isUnsubsribeAllChecked) {
								compositeDisposable.add(DatabaseTasks.unsubscribeFromAllChannels());
							}

							List<MultiSelectListPreferenceItem> channelsToSubscribeTo = new ArrayList<>();
							for(MultiSelectListPreferenceItem channel: channels) {
								if(channel.isChecked)
									channelsToSubscribeTo.add(channel);
							}

							// subscribe to the channels selected by the user
							subscribeToImportedChannels(channelsToSubscribeTo);
						})
						.negativeText(R.string.cancel)
						.build()
						.show();
			} else {
				new AlertDialog.Builder(activity)
						.setMessage(foundChannels ? R.string.no_new_channels_found : R.string.no_channels_found)
						.setNeutralButton(R.string.ok, null)
						.show();
			}
		} catch(Exception e) {
			Logger.e(this, "An error encountered while attempting to parse the XML file uploaded", e);
			Toast.makeText(activity, String.format(activity.getString(R.string.import_subscriptions_parse_error), e.getMessage()), Toast.LENGTH_LONG).show();
		}
	}



	/**
	 * A dialog that asks the user to import subscriptions from a YouTube account.
	 */
	public void displayImportSubscriptionsFromYouTubeDialog() {
		SpannableString msg = new SpannableString(activity.getText(R.string.import_subscriptions_description));
		LinkifyCompat.addLinks(msg, Linkify.WEB_URLS);
		new SkyTubeMaterialDialog(activity)
				.title(R.string.import_subscriptions)
				.content(msg)
				.positiveText(R.string.select_xml_file)
				.checkBoxPromptRes(R.string.unsubscribe_from_all_current_sibbed_channels, false, (compoundButton, b) -> isUnsubsribeAllChecked = true)
				.onPositive((dialog, which) -> displayFilePicker(false))
				.build()
				.show();
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode == IMPORT_SUBSCRIPTIONS_READ_CODE && resultCode == Activity.RESULT_OK) {
			if (data != null) {
				Uri uri = data.getData();
				parseImportedSubscriptions(uri);
			}
		}
	}

	private void subscribeToImportedChannels(final List<MultiSelectListPreferenceItem> channels) {
		// display the "Subscribing to channels …" dialog
		final MaterialDialog dialog = new MaterialDialog.Builder(activity)
				.content(R.string.subscribing_to_channels)
				.progress(true, 0)
				.build();
		dialog.show();

		compositeDisposable.add(Single.fromCallable(() -> {
			for (MultiSelectListPreferenceItem channel : channels) {
				SubscriptionsDb.getSubscriptionsDb().subscribe(new YouTubeChannel(channel.id, null));
			}

			return channels.size();
		})
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(totalChannelsSubscribedTo -> {
					// inform the SubsAdapter that it needs to repopulate the subbed channels list
					SubsAdapter.get(activity).refreshSubsList();

					// hide the dialog
					dialog.dismiss();

					Toast.makeText(activity,
							String.format(SkyTubeApp.getStr(R.string.subscriptions_to_channels_imported), totalChannelsSubscribedTo),
							Toast.LENGTH_SHORT).show();

					// refresh the Feed tab so it shows videos from the newly subscribed channels
					SubscriptionsFeedFragment.refreshSubsFeedFull();

					// if the user imported the subs channels from the Feed tab/fragment, then we
					// need to refresh the fragment in order for the fragment to update the feed...
					ActivityCompat.recreate(activity);
				}));
	}

	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		// EXT_STORAGE_PERM_CODE_BACKUP is used to backup the databases
		if (requestCode == EXT_STORAGE_PERM_CODE_BACKUP) {
			if (grantResults.length > 0  &&  grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				// permission was granted by the user
				backupDatabases();

			} else {
				// permission denied by the user
				Toast.makeText(activity, R.string.databases_backup_fail, Toast.LENGTH_LONG).show();
			}
		}
		// EXT_STORAGE_PERM_CODE_IMPORT is used for the file picker (to import database backups)
		else if (requestCode == EXT_STORAGE_PERM_CODE_IMPORT) {
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				displayFilePicker();
			}
			else {
				// permission not been granted by user
				Toast.makeText(activity, R.string.databases_import_fail, Toast.LENGTH_LONG).show();
			}
		}

		else if (requestCode == IMPORT_SUBSCRIPTIONS_READ_CODE)
		{
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				displayFilePicker(false);
			}
			else {
				// permission not been granted by user
				Toast.makeText(activity, R.string.failed_to_import_subscriptions, Toast.LENGTH_LONG).show();
			}
		}
	}

}
