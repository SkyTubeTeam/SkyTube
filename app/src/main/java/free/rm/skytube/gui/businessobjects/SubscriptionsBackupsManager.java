package free.rm.skytube.gui.businessobjects;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.text.SpannableString;
import android.text.util.Linkify;
import android.util.Log;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.text.util.LinkifyCompat;
import androidx.fragment.app.Fragment;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import com.afollestad.materialdialogs.MaterialDialog;
import com.obsez.android.lib.filechooser.ChooserDialog;

import java.io.File;

import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.subscription.SubscriptionExtractor;
import org.schabi.newpipe.extractor.subscription.SubscriptionItem;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import free.rm.skytube.R;
import free.rm.skytube.app.EventBus;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.Logger;
import free.rm.skytube.businessobjects.YouTube.POJOs.PersistentChannel;
import free.rm.skytube.businessobjects.YouTube.newpipe.ChannelId;
import free.rm.skytube.businessobjects.YouTube.newpipe.ContentId;
import free.rm.skytube.businessobjects.YouTube.newpipe.NewPipeException;
import free.rm.skytube.businessobjects.YouTube.newpipe.NewPipeService;
import free.rm.skytube.businessobjects.db.DatabaseTasks;
import free.rm.skytube.businessobjects.db.SubscriptionsDb;
import free.rm.skytube.businessobjects.opml.OpmlExporter;
import free.rm.skytube.gui.businessobjects.preferences.BackupDatabases;
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
    private static final int EXPORT_OPML_PERM_CODE = 1952;
    private static final int IMPORT_SUBSCRIPTIONS_READ_CODE = 42;
    private static final String OPML_MIMETYPE = "text/x-opml";
    private static final String TAG = SubscriptionsBackupsManager.class.getSimpleName();
    private boolean isUnsubscribeAllChecked = false;

    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private final ActivityResultLauncher<Intent> opmlExportLauncher;
    private final ActivityResultLauncher<Intent> importSubscriptionsLauncher;

    public SubscriptionsBackupsManager(Activity activity, Fragment fragment) {
        this.activity = activity;
        this.fragment = fragment;
        opmlExportLauncher = fragment.registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null && data.getData() != null) {
                        Uri outputUri = data.getData();
                        exportSubscriptionsToOpmlWithSaf(outputUri);
                    }
                }
            }
        );

        importSubscriptionsLauncher = fragment.registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null && data.getData() != null) {
                        Uri uri = data.getData();
                        parseImportedSubscriptions(uri);
                    }
                }
            }
        );
    }

    private static class Result {
        private final  List<MultiSelectListPreferenceItem> newChannels;
        private final boolean noChannelFound;

        private Result(final List<MultiSelectListPreferenceItem> newChannels, final boolean noChannelFound) {
            this.newChannels = newChannels;
            this.noChannelFound = noChannelFound;
        }
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
     * Export subscriptions to OPML file.
     */
    public void exportSubscriptionsToOpml() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // Use Storage Access Framework - no storage permission needed!
            showOpmlFileSaveDialog();
        } else {
            if (hasAccessToExtStorage(EXPORT_OPML_PERM_CODE)) {
                showOpmlExportFilenameDialog(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS));
            }
        }
    }

    /**
     * Show file save dialog using Storage Access Framework.
     */
    private void showOpmlFileSaveDialog() {
        // Use Storage Access Framework to let user choose where to save
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(OPML_MIMETYPE);
        intent.putExtra(Intent.EXTRA_TITLE, OpmlExporter.getDefaultExportFileName());
        
        try {
            if (opmlExportLauncher != null) {
                opmlExportLauncher.launch(intent);
            } else {
                Toast.makeText(activity, R.string.subscriptions_export_opml_fail, Toast.LENGTH_SHORT).show();
            }
        } catch (ActivityNotFoundException e) {
            Logger.e(this, "Unable to trigger activity: {}", e.getMessage(), e);
            Toast.makeText(activity, R.string.subscriptions_export_opml_fail, Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Show dialog for user to specify OPML export filename.
     *
     * @param exportDirectory The directory where the file will be saved
     */
    private void showOpmlExportFilenameDialog(File exportDirectory) {
        // Create a dialog with an edit text for filename input
        final EditText input = new EditText(activity);
        input.setText(OpmlExporter.getDefaultExportFileName());
        input.setSelection(input.getText().length());
        
        // Add a text view to show the selected directory
        TextView directoryView = new TextView(activity);
        directoryView.setText(String.format(activity.getString(R.string.opml_export_location), exportDirectory.getAbsolutePath()));
        directoryView.setPadding(0, 0, 0, 20);
        
        // Set up the dialog layout
        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 10);
        layout.addView(directoryView);
        layout.addView(input);
        
        new AlertDialog.Builder(activity)
                .setTitle(R.string.opml_export_title)
                .setMessage(R.string.opml_export_message)
                .setView(layout)
                .setPositiveButton(R.string.opml_export_confirm, (dialog, which) -> {
                    String filename = input.getText().toString().trim();
                    if (!filename.isEmpty()) {
                        exportSubscriptionsToOpmlWithFilename(filename, exportDirectory);
                    } else {
                        Toast.makeText(activity, R.string.subscriptions_export_opml_fail, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.opml_export_cancel, null)
                .show();
    }

    /**
     * Export subscriptions to OPML using Storage Access Framework.
     *
     * @param outputUri The URI to export to (from SAF)
     */
    private void exportSubscriptionsToOpmlWithSaf(Uri outputUri) {
        Toast.makeText(activity, R.string.subscriptions_exporting_opml, Toast.LENGTH_SHORT).show();
        compositeDisposable.add(
            Single.fromCallable(() -> {
                boolean success = OpmlExporter.exportSubscriptionsToOpmlWithSaf(activity, outputUri);
                if (success) {
                    return getDisplayNameFromUri(outputUri);
                } else {
                    throw new IOException("Failed to export subscriptions to OPML using SAF");
                }
            })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .onErrorReturn(throwable -> {
                Log.e(TAG, "Unable to export subscriptions to OPML using SAF...", throwable);
                return null;
            })
            .subscribe(displayName -> {
                if (displayName != null) {
                    final String message = String.format(activity.getString(R.string.subscriptions_export_opml_success),
                            displayName);
                    
                    new AlertDialog.Builder(activity)
                            .setMessage(message)
                            .setNeutralButton(R.string.ok, null)
                            .show();
                } else {
                    new AlertDialog.Builder(activity)
                            .setMessage(activity.getString(R.string.subscriptions_export_opml_fail))
                            .setNeutralButton(R.string.ok, null)
                            .show();
                }
            })
        );
    }

    /**
     * Get a user-friendly display name from a content URI.
     */
    private String getDisplayNameFromUri(Uri uri) {
        if (uri == null) {
            return activity.getString(R.string.opml_export_unknown_filename);
        }

        // Try to get the display name from the content resolver
        try (Cursor cursor = activity.getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (displayNameIndex != -1) {
                    String displayName = cursor.getString(displayNameIndex);
                    if (displayName != null && !displayName.isEmpty()) {
                        return displayName;
                    }
                }
            }
        } catch (Exception e) {
            Logger.e(this, "Retrieving displayNameFromUri failed, uri={}, msg={}", uri, e.getMessage(), e);
        }

        // Fallback to a generic success message
        return activity.getString(R.string.opml_export_unknown_filename);
    }
    
    /**
     * Export subscriptions to OPML file with specified filename and directory.
     *
     * @param filename The filename to use for the OPML export
     * @param exportDirectory The directory where the file will be saved
     */
    private void exportSubscriptionsToOpmlWithFilename(String filename, File exportDirectory) {
        Toast.makeText(activity, R.string.subscriptions_exporting_opml, Toast.LENGTH_SHORT).show();
        compositeDisposable.add(
            Single.fromCallable(() -> {
                // Ensure the filename has .opml extension
                String finalFilename = filename;
                if (!finalFilename.toLowerCase().endsWith(".opml")) {
                    finalFilename += ".opml";
                }
                
                // Use the selected directory as the export location
                File outputFile = new File(exportDirectory, finalFilename);
                
                boolean success = OpmlExporter.exportSubscriptionsToOpml(outputFile);
                if (success) {
                    return outputFile.getAbsolutePath();
                } else {
                    throw new IOException("Failed to export subscriptions to OPML");
                }
            })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .onErrorReturn(throwable -> {
                Log.e(TAG, "Unable to export subscriptions to OPML...", throwable);
                return "";
            })
            .subscribe(exportPath -> {
                String message = (!exportPath.isEmpty()) ?
                        String.format(activity.getString(R.string.subscriptions_export_opml_success),
                                exportPath) :
                        activity.getString(R.string.subscriptions_export_opml_fail);

                new AlertDialog.Builder(activity)
                        .setMessage(message)
                        .setNeutralButton(R.string.ok, null)
                        .show();
            })
        );
    }

    /**
     * Display file picker to be used by the user to select the BACKUP (database) or
     * YOUTUBE SUBS (json or xml file) to import.
     */
    public void displayFilePicker() {
        displayFilePicker(true);
    }

    /**
     * Display file picker to be used by the user to select the BACKUP (database) or
     * YOUTUBE SUBS (json or xml file) to import.
     *
     * @param importDb  If set to true, the app will import (previously backed-up) database;
     *                  Otherwise, it will import YouTube subs (json or xml file).
     */
    private void displayFilePicker(final boolean importDb) {
        // do not display the file picker until the user gives us access to the external storage
        if (!hasAccessToExtStorage(importDb ? EXT_STORAGE_PERM_CODE_IMPORT : IMPORT_SUBSCRIPTIONS_READ_CODE))
            return;

        ChooserDialog dialog = new ChooserDialog(activity)
                .withStartFile((importDb ? Environment.getExternalStorageDirectory()
                        : Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS))
                        .getAbsolutePath())
                .displayPath(true)
                .withChosenListener((file, dirFile) -> {
                    if (importDb)
                        displayImportDbsBackupWarningMsg(file);
                    else {
                        Uri uri = Uri.fromFile(new File(file));
                        parseWithNewPipe(uri);
                    }
                })
                .withOnCancelListener(DialogInterface::cancel);
        if(importDb) {
            dialog.withFilter(false, false, "skytube");
        } else {
            dialog.withFilterRegex(false, false, ".*(json|xml|subscription_manager|zip|csv)$");
        }
        dialog.build().show();
    }

    /**
     * Launch file picker using Storage Access Framework for importing subscriptions.
     */
    private void launchImportFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        String[] mimeTypes = {"application/json", "text/xml", "application/xml", "application/zip", "text/csv"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        
        try {
            if (importSubscriptionsLauncher != null) {
                importSubscriptionsLauncher.launch(intent);
            } else {
                Toast.makeText(activity, R.string.failed_to_import_subscriptions, Toast.LENGTH_SHORT).show();
            }
        } catch (ActivityNotFoundException e) {
            Toast.makeText(activity, R.string.failed_to_import_subscriptions, Toast.LENGTH_SHORT).show();
        }
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

    private void parseWithNewPipe(Uri uri) {
        parseWithNewPipeBackground(uri)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    importChannels(result.newChannels, result.noChannelFound);
                });
    }

    private Single<Result> parseWithNewPipeBackground(Uri uri) {
        return Single.fromCallable(() -> {
            SubscriptionExtractor extractor = NewPipeService.get().createSubscriptionExtractor();
            String extension = getExtension(uri);
            if (extractor != null && extension != null) {
                Log.i(TAG, "Parsing with " + extractor + " : " + uri);
                try (InputStream input = activity.getContentResolver().openInputStream(uri)) {
                    if ("csv".equals(extension) || "json".equals(extension) || "zip".equals(extension)) {
                        List<SubscriptionItem> items = extractor.fromInputStream(input, extension);
                        return importChannels(items);
                    }
                } catch (IOException | ExtractionException e) {
                    Log.e(TAG, "Unable to extract subscriptions: " + e.getMessage(), e);
                    SkyTubeApp.notifyUserOnError(activity, e);
                }
            }
            Log.i(TAG, "Parsing with old code : "+ uri.toString());
            return parseImportedSubscriptions(uri);
        });
    }

    private Result importChannels(final List<SubscriptionItem> items) {
        List<MultiSelectListPreferenceItem> result = new ArrayList();
        NewPipeService newPipeService = NewPipeService.get();
        SubscriptionsDb subscriptionsDb = SubscriptionsDb.getSubscriptionsDb();
        for (SubscriptionItem item : items){
            String url = item.getUrl();
            ContentId contentId = newPipeService.getContentId(url);
            if (contentId != null && contentId.getType() == StreamingService.LinkType.CHANNEL && !subscriptionsDb.isUserSubscribedToChannel(new ChannelId(contentId.getId()))) {
                result.add(new MultiSelectListPreferenceItem(contentId.getId(), item.getName()));
            }
        }
        return new Result(result, items.isEmpty());
    }

    private String getExtension(Uri uri) {
        String name = uri.toString().toLowerCase(Locale.ROOT);
        int lastDot = name.lastIndexOf('.');
        if (lastDot >= 0) {
            return name.substring(lastDot + 1);
        }
        return "";
    }

    /**
     * Parse the file that the user selected to import subscriptions from. Each channel contained in the file
     * that the user is not already subscribed to will appear in a dialog, to allow the user to select individual channels
     * to subscribe to, via a new Dialog. Once the user chooses to import the selected channels via the Import Subscriptions
     * button, {@link #subscribeToImportedChannels(List)} will be executed with a list of the selected channels.
     *
     * @param uri The URI pointing to the file containing YouTube Channels to subscribe to.
     * @return
     */
    private Result parseImportedSubscriptions(Uri uri) {
        ArrayList<MultiSelectListPreferenceItem> channels;
        String uriString = uri.toString();
        int lastIndexOf = uriString.lastIndexOf(".");
        if (lastIndexOf > 0 && uriString.substring(lastIndexOf).equalsIgnoreCase("xml")) {
            channels = parseChannelsXML(uri);
        } else {
            channels = parseChannelsJson(uri);
        }

        // Check the channel list for new channels
        ArrayList<MultiSelectListPreferenceItem> newChannels = new ArrayList<>();
        for (MultiSelectListPreferenceItem channel : channels) {
            if (channel.id != null && !SubscriptionsDb.getSubscriptionsDb().isUserSubscribedToChannel(new ChannelId(channel.id))) {
                newChannels.add(channel);
            }
        }

        return new Result(newChannels, channels.isEmpty());
    }

    private void importChannels(List<MultiSelectListPreferenceItem> newChannels, boolean noChannelFound) {
        if(newChannels.size() > 0) {
            // display a dialog which allows the user to select the channels to import
            new MultiSelectListPreferenceDialog(activity, newChannels)
                    .title(R.string.import_subscriptions)
                    .positiveText(R.string.import_subscriptions)
                    .onPositive((dialog, which) -> {

                        List<MultiSelectListPreferenceItem> channelsToSubscribeTo = new ArrayList<>();
                        for(MultiSelectListPreferenceItem channel: newChannels) {
                            if(channel.isChecked)
                                channelsToSubscribeTo.add(channel);
                        }

                        // if the user checked the "Unsubscribe to all subscribed channels" checkbox
                        if (isUnsubscribeAllChecked) {
                            compositeDisposable.add(DatabaseTasks.completableUnsubscribeFromAllChannels().andThen(
                                    subscribeToImportedChannels(channelsToSubscribeTo)
                            ).subscribe());
                        } else {
                            // subscribe to the channels selected by the user
                            compositeDisposable.add(subscribeToImportedChannels(channelsToSubscribeTo).subscribe());
                        }
                    })
                    .negativeText(R.string.cancel)
                    .build()
                    .show();
        } else {
            new AlertDialog.Builder(activity)
                    .setMessage(noChannelFound ? R.string.no_channels_found : R.string.no_new_channels_found)
                    .setNeutralButton(R.string.ok, null)
                    .show();
        }
    }

    /**
     * Parse the JSON file that the user selected to import subscriptions from.
     *
     * @param uri The URI pointing to the JSON file containing YouTube Channels to subscribe to
     * @return The channels found in the given file
     */
    private ArrayList<MultiSelectListPreferenceItem> parseChannelsJson(Uri uri) {
        JsonArray jsonArray;
        final ArrayList<MultiSelectListPreferenceItem> channels = new ArrayList<>();

        try {
            InputStreamReader fileReader = new InputStreamReader(activity.getContentResolver().openInputStream(uri));
            jsonArray = JsonParser.parseReader(fileReader).getAsJsonArray();
            fileReader.close();
        } catch (IOException e) {
            Logger.e(this, "An error occurred while reading the file", e);
            Toast.makeText(activity, String.format(activity.getString(R.string.import_subscriptions_parse_error), e.getMessage()), Toast.LENGTH_LONG).show();
            return channels;
        }

        for (JsonElement obj : jsonArray) {
            JsonObject snippet = obj.getAsJsonObject().getAsJsonObject("snippet");
            if (snippet == null) {
                continue;
            }
            JsonPrimitive channelName = snippet.getAsJsonPrimitive("title");
            JsonObject resourceId = snippet.getAsJsonObject("resourceId");
            if (resourceId == null) {
                continue;
            }
            JsonPrimitive channelId = resourceId.getAsJsonPrimitive("channelId");
            if (channelId != null && channelName != null) {
                channels.add(new MultiSelectListPreferenceItem(channelId.getAsString(), channelName.getAsString()));
            }
        }
        return channels;
    }

    /**
     * Parse the XML file that the user selected to import subscriptions from.
     *
     * @param uri The URI pointing to the XML file containing YouTube Channels to subscribe to
     * @return The channels found in the given file
     */
    private ArrayList<MultiSelectListPreferenceItem> parseChannelsXML(Uri uri) {
        final ArrayList<MultiSelectListPreferenceItem> channels = new ArrayList<>();
        Pattern channelPattern = Pattern.compile(".*channel_id=([^&]+)");
        Matcher matcher;

        try {
            XmlPullParserFactory xmlFactoryObject = XmlPullParserFactory.newInstance();
            XmlPullParser parser = xmlFactoryObject.newPullParser();
            parser.setInput(activity.getContentResolver().openInputStream(uri), null);
            int event = parser.getEventType();
            // If channels are found in the XML file but they are all already subscribed to, alert the user with a different
            // message than if no channels were found at all.
            while (event != XmlPullParser.END_DOCUMENT) {
                String name = parser.getName();
                switch (event) {
                    case XmlPullParser.START_TAG:
                        break;

                    case XmlPullParser.END_TAG:
                        if (name.equals("outline")) {
                            String xmlUrl = parser.getAttributeValue(null, "xmlUrl");
                            if (xmlUrl != null) {
                                matcher = channelPattern.matcher(xmlUrl);
                                if (matcher.matches()) {
                                    String channelId = matcher.group(1);
                                    String channelName = parser.getAttributeValue(null, "title");
                                    channels.add(new MultiSelectListPreferenceItem(channelId, channelName));
                                }

                            }
                        }
                        break;

                }
                event = parser.next();
            }
        } catch (IOException e) {
            Logger.e(this, "An error occurred while reading the file", e);
            Toast.makeText(activity, String.format(activity.getString(R.string.import_subscriptions_parse_error), e.getMessage()), Toast.LENGTH_LONG).show();
        } catch (XmlPullParserException e) {
            Logger.e(this, "An error occurred while attempting to parse the XML file uploaded", e);
            Toast.makeText(activity, String.format(activity.getString(R.string.import_subscriptions_parse_error), e.getMessage()), Toast.LENGTH_LONG).show();
        }
        return channels;
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
                .positiveText(R.string.select_sub_file)
                .checkBoxPromptRes(R.string.unsubscribe_from_all_current_sibbed_channels, false, (compoundButton, b) -> isUnsubscribeAllChecked = true)
                .onPositive((dialog, which) -> launchImportFilePicker())
                .build()
                .show();
    }

    private @NonNull Single<Object[]> subscribeToImportedChannels(final List<MultiSelectListPreferenceItem> channels) {
        return Single.fromCallable(() -> {
            // display the "Subscribing to channels …" dialog
            final MaterialDialog dialog = new MaterialDialog.Builder(activity)
                    .content(R.string.subscribing_to_channels)
                    .progress(true, 0)
                    .build();
            dialog.show();
            return dialog;
        }).subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(Schedulers.io())
                .map(dialog -> {
                    SubscriptionsDb db = SubscriptionsDb.getSubscriptionsDb();
                    int success = 0;
                    for (MultiSelectListPreferenceItem selectedItem : channels) {
                        try {
                            ChannelId channelId = new ChannelId(selectedItem.id);
                            PersistentChannel channelInfo = DatabaseTasks.getChannelOrRefresh(activity, channelId, true);
                            if (!channelInfo.isSubscribed()) {
                                db.subscribe(channelInfo, Collections.emptyList());
                                success += 1;
                            }
                        } catch (NewPipeException newPipeException) {
                            Log.e(TAG, "Error: " + newPipeException.getMessage(), newPipeException);
                        }
                    }

                    return new Object[] { dialog, success };
                })
                .observeOn(AndroidSchedulers.mainThread())
                .map(inputs -> {
                    // hide the dialog
                    ((MaterialDialog) inputs[0]).dismiss();
                    int totalChannelsSubscribedTo = (Integer) inputs[1];
                    // inform the SubsAdapter that it needs to repopulate the subbed channels list
                    EventBus.getInstance().notifyMainTabChanged(EventBus.SettingChange.SUBSCRIPTION_LIST_CHANGED);


                    Toast.makeText(activity,
                            String.format(SkyTubeApp.getStr(R.string.subscriptions_to_channels_imported), totalChannelsSubscribedTo),
                            Toast.LENGTH_SHORT).show();

                    // refresh the Feed tab so it shows videos from the newly subscribed channels
                    SkyTubeApp.getSettings().setRefreshSubsFeedFull(true);

                    // if the user imported the subs channels from the Feed tab/fragment, then we
                    // need to refresh the fragment in order for the fragment to update the feed...
                    ActivityCompat.recreate(activity);
                    return inputs;
                });
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
        else if (requestCode == EXPORT_OPML_PERM_CODE)
        {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                exportSubscriptionsToOpml();
            }
            else {
                // permission not been granted by user
                Toast.makeText(activity, R.string.subscriptions_export_opml_fail, Toast.LENGTH_LONG).show();
            }
        }
    }

}
