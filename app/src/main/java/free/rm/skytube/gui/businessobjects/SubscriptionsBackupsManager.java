package free.rm.skytube.gui.businessobjects;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v13.app.FragmentCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.util.Linkify;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.github.angads25.filepicker.controller.DialogSelectionListener;
import com.github.angads25.filepicker.model.DialogConfigs;
import com.github.angads25.filepicker.model.DialogProperties;
import com.github.angads25.filepicker.view.FilePickerDialog;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.AsyncTaskParallel;
import free.rm.skytube.businessobjects.GetSubscriptionVideosTask;
import free.rm.skytube.businessobjects.YouTubeChannel;
import free.rm.skytube.businessobjects.db.SubscriptionsDb;
import free.rm.skytube.gui.businessobjects.preferences.BackupDatabases;
import free.rm.skytube.gui.fragments.SubscriptionsFeedFragment;

/**
 * Custom class to handle Backups and Subscriptions imports. This class must be instantiated using either a native Fragment
 * or the support library (v4) Fragment. That Fragment must then override onRequestPermissionsResult and call the same method
 * in this class in order to pass the permission request result on to this class for handling.
 */
public class SubscriptionsBackupsManager {
	private Activity activity;
	private android.app.Fragment fragment;
	private Fragment supportFragment;
	private static final int EXT_STORAGE_PERM_CODE_BACKUP = 1950;
	private static final int EXT_STORAGE_PERM_CODE_IMPORT = 1951;
	private static final int IMPORT_SUBSCRIPTIONS_READ_CODE = 42;
	private static final String TAG = SubscriptionsBackupsManager.class.getSimpleName();


	public SubscriptionsBackupsManager(Activity activity, Fragment supportFragment) {
		this.activity = activity;
		this.supportFragment = supportFragment;
	}

	public SubscriptionsBackupsManager(Activity activity, android.app.Fragment fragment) {
		this.activity = activity;
		this.fragment = fragment;
	}


	/**
	 * Backup the databases.
	 */
	public void backupDatabases() {
		// if the user has granted us access to the external storage, then perform the backup
		// operation
		if (hasAccessToExtStorage(EXT_STORAGE_PERM_CODE_BACKUP)) {
			new BackupDatabasesTask().executeInParallel();
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

		DialogProperties properties = new DialogProperties();

		properties.selection_mode = DialogConfigs.SINGLE_MODE;
		properties.selection_type = DialogConfigs.FILE_SELECT;
		properties.root = Environment.getExternalStorageDirectory();
		properties.error_dir = new File(DialogConfigs.DEFAULT_DIR);
		properties.offset = new File(DialogConfigs.DEFAULT_DIR);
		properties.extensions = importDb ? new String[]{"skytube"} : new String[]{"xml"};

		FilePickerDialog dialog = new FilePickerDialog(activity, properties);
		dialog.setDialogSelectionListener(new DialogSelectionListener() {
			@Override
			public void onSelectedFilePaths(String[] files) {
				if (files == null  ||  files.length <= 0)
					Toast.makeText(activity, R.string.databases_import_nothing_selected, Toast.LENGTH_LONG).show();
				else {
					if(importDb)
						displayImportDbsBackupWarningMsg(files[0]);
					else {
						Uri uri = Uri.fromFile(new File(files[0]));
						parseImportedSubscriptions(uri);
					}
				}
			}
		});
		dialog.setTitle(importDb ? R.string.databases_import_select_backup : R.string.subs_import_select_backup);
		dialog.show();
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
			if(fragment != null)
				FragmentCompat.requestPermissions(fragment, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
							permissionRequestCode);
			else if(supportFragment != null)
				supportFragment.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
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
						.setPositiveButton(R.string.continue_, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								new ImportDatabasesTask(backupFilePath).executeInParallel();
							}
						})
						.setNegativeButton(R.string.cancel, null)
						.show();
	}

	/**
	 * A task that imports the subscriptions and bookmarks databases.
	 */
	private class ImportDatabasesTask extends AsyncTaskParallel<Void, Void, Boolean> {

		private String backupFilePath;

		public ImportDatabasesTask(String backupFilePath) {
			this.backupFilePath = backupFilePath;
		}


		@Override
		protected void onPreExecute() {
			Toast.makeText(activity, R.string.databases_importing, Toast.LENGTH_SHORT).show();
		}


		@Override
		protected Boolean doInBackground(Void... params) {
			boolean successful = false;

			try {
				BackupDatabases backupDatabases = new BackupDatabases();
				backupDatabases.importBackupDb(backupFilePath);
				successful = true;
			} catch (Throwable tr) {
				Log.e(TAG, "Unable to import the databases...", tr);
			}

			return successful;
		}


		@Override
		protected void onPostExecute(Boolean successfulImport) {
			// We need to force the app to refresh the subscriptions feed when the app is
			// restarted (irrespective to when the feeds were last refreshed -- which could be
			// during the last 5 mins).  This is as we are loading new databases...
			GetSubscriptionVideosTask.updateFeedsLastUpdateTime(null);

			// ask the user to restart the app
			new AlertDialog.Builder(activity)
							.setCancelable(false)
							.setMessage(successfulImport ? R.string.databases_import_success : R.string.databases_import_fail)
							.setNeutralButton(R.string.restart, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									SkyTubeApp.restartApp();
								}
							})
							.show();
		}

	}

	/**
	 * Parse the XML file that the user selected to import subscriptions from. Each channel contained in the XML
	 * that the user is not already subscribed to will appear in a dialog, to allow the user to select individual channels
	 * to subscribe to, via a new Dialog. Once the user chooses to import the selected channels via the Import Subscriptions
	 * button, {@link SubscribeToImportedChannelsTask} will be executed with a list of the selected channels.
	 *
	 * @param uri The URI pointing to the XML file containing YouTube Channels to subscribe to.
	 */
	private void parseImportedSubscriptions(Uri uri) {
		try {
			final List<ImportSubscriptionsChannel> channels = new ArrayList<>();
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
										channels.add(new ImportSubscriptionsChannel(channelName, channelId));
									}
								}

							}
						}
						break;

				}
				event = myParser.next();
			}

			if(channels.size() > 0) {
				final ImportSubscriptionsAdapter adapter = new ImportSubscriptionsAdapter(channels);
				MaterialDialog dialog = new MaterialDialog.Builder(activity)
								.title(R.string.import_subscriptions)
								.titleColorRes(R.color.dialog_title)
								.customView(R.layout.subs_youtube_import_dialog_list, false)
								.positiveText(R.string.import_subscriptions)
								.backgroundColorRes(R.color.dialog_backgound)
								.contentColorRes(R.color.dialog_content_text)
								.positiveColorRes(R.color.dialog_positive_text)
								.negativeColorRes(R.color.dialog_negative_text)
								.onPositive(new MaterialDialog.SingleButtonCallback() {
									@Override
									public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
										List<ImportSubscriptionsChannel> channelsToSubscribeTo = new ArrayList<>();
										for(ImportSubscriptionsChannel channel: channels) {
											if(channel.isChecked)
												channelsToSubscribeTo.add(channel);
										}
										SubscribeToImportedChannelsTask task = new SubscribeToImportedChannelsTask();
										task.execute(channelsToSubscribeTo);
									}
								})
								.negativeText(R.string.cancel)
								.onNegative(new MaterialDialog.SingleButtonCallback() {
									@Override
									public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
										dialog.dismiss();
									}
								})
								.build();

				RecyclerView list = dialog.getCustomView().findViewById(R.id.channel_list);
				list.setAdapter(adapter);
				list.setLayoutManager(new LinearLayoutManager(activity));

				Button selectAllButton = dialog.getCustomView().findViewById(R.id.select_all_button);
				selectAllButton.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						adapter.selectAll();
					}
				});
				Button selectNoneButton = dialog.getCustomView().findViewById(R.id.select_none_button);
				selectNoneButton.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						adapter.selectNone();
					}
				});


				dialog.show();
			} else {
				Toast.makeText(activity, foundChannels ? R.string.no_new_channels_found : R.string.no_channels_found, Toast.LENGTH_LONG).show();
			}
		} catch(Exception e) {
			e.printStackTrace();
			Toast.makeText(activity, String.format(activity.getString(R.string.import_subscriptions_parse_error), e.getMessage()), Toast.LENGTH_LONG);
		}



	}

	/**
	 * A dialog that asks the user to import subscriptions from a YouTube account.
	 */
	public void displayImportSubscriptionsDialog() {
		SpannableString msg = new SpannableString(activity.getText(R.string.import_subscriptions_description));
		Linkify.addLinks(msg, Linkify.WEB_URLS);
		new MaterialDialog.Builder(activity)
						.title(R.string.import_subscriptions)
						.titleColorRes(R.color.dialog_title)
						.backgroundColorRes(R.color.dialog_backgound)
						.content(msg)
						.contentColorRes(R.color.dialog_content_text)
						.positiveText(R.string.select_xml_file)
						.positiveColorRes(R.color.dialog_positive_text)
						.onPositive(new MaterialDialog.SingleButtonCallback() {
							@Override
							public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
								displayFilePicker(false);
							}
						})
						.negativeText(R.string.cancel)
						.negativeColorRes(R.color.dialog_negative_text)
						.onNegative(new MaterialDialog.SingleButtonCallback() {
							@Override
							public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
								dialog.dismiss();
							}
						})
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

	/**
	 * AsyncTask to loop through a list of channels to subscribe to. A Dialog will appear notifying the user of the progress
	 * of fetching videos for each channel.
	 */
	private class SubscribeToImportedChannelsTask extends AsyncTask<List<ImportSubscriptionsChannel>, Void, Void> {
		MaterialDialog dialog;
		private int numChannelsDone = 0;
		private int totalVideosFetched = 0;
		private int totalChannels = 0;

		public SubscribeToImportedChannelsTask() {

		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();

		}

		private void updateDialog() {
			activity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					dialog.setContent(String.format(activity.getString(R.string.fetched_videos_from_channels), totalVideosFetched, numChannelsDone, totalChannels));
				}
			});
		}

		@Override
		protected Void doInBackground(final List<ImportSubscriptionsChannel>... channels) {
			totalChannels = channels[0].size();
			activity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					dialog = new MaterialDialog.Builder(activity)
									.content(String.format(activity.getString(R.string.fetched_videos_from_channels), 0, 0, totalChannels))
									.progress(false, totalChannels, false)
									.build();
					dialog.show();
				}
			});

			List<YouTubeChannel> channelsList = new ArrayList<>();
			for(ImportSubscriptionsChannel channel : channels[0]) {
				if(!channel.isChecked)
					continue;
				try {
					YouTubeChannel channelObj = new YouTubeChannel();
					channelsList.add(channelObj);
					channelObj.init(channel.channelId);
					SubscriptionsDb.getSubscriptionsDb().subscribe(channelObj);
					// Need to set this channelObj to subscribed, so that when videos are retrieved for the channel, they get saved into the database.
					channelObj.setUserSubscribed(true);
				} catch(IOException e) {
					e.printStackTrace();
				}
			}
			new GetSubscriptionVideosTask(new SubscriptionsFragmentListener() {
				@Override
				public void onChannelVideosFetched(YouTubeChannel channel, int videosFetched, boolean videosDeleted) {
					numChannelsDone++;
					totalVideosFetched += videosFetched;
					dialog.incrementProgress(1);
					updateDialog();
				}

				@Override
				public void onAllChannelVideosFetched() {
					dialog.dismiss();
					Toast.makeText(SkyTubeApp.getContext(), String.format(SkyTubeApp.getStr(R.string.subscriptions_to_channels_imported), numChannelsDone), Toast.LENGTH_SHORT).show();

					// refresh the Feed tab so it shows videos from the newly subscribed channels
					SubscriptionsFeedFragment.refreshSubscriptionsFeed();

					// if the user imported the subs channels from the Feed tab/fragment, then we
					// need to refresh the fragment in order for the fragment to update the feed...
					activity.recreate();

					// refresh the subs adapter
					SubsAdapter.get(activity).refreshSubsList();
				}
			}).setForceRefresh(true).setChannelsToRefresh(channelsList).executeInParallel();

			return null;
		}

		@Override
		protected void onPostExecute(Void aVoid) {
			super.onPostExecute(aVoid);
		}
	}

	/**
	 * A task that backups the subscriptions and bookmarks databases.
	 */
	private class BackupDatabasesTask extends AsyncTaskParallel<Void, Void, String> {

		@Override
		protected void onPreExecute() {
			Toast.makeText(activity, R.string.databases_backing_up, Toast.LENGTH_SHORT).show();
		}


		@Override
		protected String doInBackground(Void... params) {
			String backupPath = null;

			try {
				BackupDatabases backupDatabases = new BackupDatabases();
				backupPath = backupDatabases.backupDbsToSdCard();
			} catch (Throwable tr) {
				Log.e(TAG, "Unable to backup the databases...", tr);
			}

			return backupPath;
		}


		@Override
		protected void onPostExecute(String backupPath) {
			String message =  (backupPath != null)
							? String.format(activity.getString(R.string.databases_backup_success), backupPath)
							: activity.getString(R.string.databases_backup_fail);

			new AlertDialog.Builder(activity)
							.setMessage(message)
							.setNeutralButton(R.string.ok, null)
							.show();
		}

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
