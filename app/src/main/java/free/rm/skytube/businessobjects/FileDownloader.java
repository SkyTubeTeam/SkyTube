/*
 * SkyTube
 * Copyright (C) 2018  Ramon Mifsud
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation (version 3 of the License).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package free.rm.skytube.businessobjects;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.webkit.MimeTypeMap;

import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.Serializable;
import java.util.regex.Pattern;

import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.gui.activities.PermissionsActivity;

import static free.rm.skytube.app.SkyTubeApp.getContext;

/**
 * Downloads remote files by using Android's {@link DownloadManager}.
 */
public abstract class FileDownloader implements Serializable, PermissionsActivity.PermissionsTask {

	/** The remote file URL that is going to be downloaded. */
	private String  remoteFileUrl = null;
	/** The directory type:  e.g. Environment.DIRECTORY_MOVIES or Environment.DIRECTORY_PICTURES */
	private String  dirType = null;
	/** The title that will be displayed by the Android's download manager. */
	private String  title = null;
	/** The description that will be displayed by the Android's download manager. */
	private String  description = null;
	/** Output file name (without file extension). */
	private String  outputFileName = null;
	private String  outputDirectoryName = null;
	private File  parentDirectory = null;
	private String  outputFileExtension = null;
	/** If set to true, then the download manager will download the file over cellular network. */
	private Boolean allowedOverRoaming = null;
	/** If set, download manager will only download files over the specified networks.
	 *  This is ignored if allowedOverRoaming is set to true. */
	private Integer allowedNetworkTypesFlags = null;

	private long    downloadId;
	private transient BroadcastReceiver onComplete;

	private Pattern invalidCharacters = Pattern.compile("[^\\w\\d]+");

	/**
	 * Displays the {@link PermissionsActivity} which will first ask the user to give us permissions
	 * to write to external storage and once that permission is granted, the {@link FileDownloader}
	 * will start downloading the file.
	 */
	public void displayPermissionsActivity(Context context) {
		Intent i = new Intent(getContext(), PermissionsActivity.class);
		i.putExtra(PermissionsActivity.PERMISSIONS_TASK_OBJ, this);
		context.startActivity(i);
	}


	@Override
	public void onExternalStoragePermissionsGranted() {
		download();
	}


	/**
	 * Download the remote file.
	 *
	 * <p>Android's DownloadManager will be used to download the image on our behalf.</p>
	 */
	private void download() {
		// check if the mandatory variables were set -- if not halt the program.
		checkIfVariablesWereSet();

		// if the external storage is not available then halt the download operation
		if (!isExternalStorageAvailable()) {
			onExternalStorageNotAvailable();
			return;
		}

		Uri     remoteFileUri = Uri.parse(remoteFileUrl);
		String  downloadFileName = getCompleteFileName(remoteFileUri);
		FileInformation fileInformation = new FileInformation(downloadFileName, parentDirectory, dirType);
		String fullDownloadFileName = fileInformation.getFullDownloadFileName();
		final File downloadDestinationFile = fileInformation.getFile();


		Logger.w(this, "Downloading video %s into %s -> %s", outputFileName, outputDirectoryName, downloadDestinationFile.getAbsolutePath());
		if (downloadDestinationFile.exists()) {
			onFileDownloadCompleted(true, Uri.parse(downloadDestinationFile.toURI().toString()));
			return;
		}

		DownloadManager.Request request = new DownloadManager.Request(remoteFileUri)
				.setAllowedOverRoaming(allowedOverRoaming)
				.setTitle(title)
				.setDescription(description)
				.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

		String videoDir = SkyTubeApp.getSettings().getDownloadFolder(null);
		if(videoDir != null) {
			request.setDestinationUri(Uri.fromFile(new File(videoDir, fullDownloadFileName)));
		} else {
			request.setDestinationInExternalPublicDir(dirType, fullDownloadFileName);
		}

		if (!allowedOverRoaming) {
			request.setAllowedNetworkTypes(allowedNetworkTypesFlags);
		}

		// onComplete.onReceive() will be executed once the file is downloaded
		getContext().registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        // start downloading
        try {
            downloadId = ContextCompat.getSystemService(getContext(), DownloadManager.class).enqueue(request);
            onFileDownloadStarted();
        } catch (RuntimeException re) {
            Logger.e(this, "Download failed:"+re.getMessage(), re);
            onDownloadStartFailed(title, re);
        }
	}


	/**
	 * Will check if the mandatory instance variables have been set.
	 */
	private void checkIfVariablesWereSet() {
		if (remoteFileUrl == null  ||  dirType == null  ||  title == null
				||  outputFileName == null  ||  allowedOverRoaming == null
				|| (!allowedOverRoaming &&  allowedNetworkTypesFlags == null)) {
			throw new IllegalStateException("One of the parameters was not set for the FileDownloader");
		}

		onComplete = new BroadcastReceiver() {
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();

				if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
					long referenceId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);

					// check the referenceId for this download
					if (referenceId == downloadId) {
						fileDownloadStatus();
					}
				}
			}
		};
	}


	/**
	 * Checks if the external storage is available for read and write.
	 *
	 * @return True if the external storage is available.
	 */
	private boolean isExternalStorageAvailable() {
		String state = Environment.getExternalStorageState();
		return Environment.MEDIA_MOUNTED.equals(state);
	}


	/**
	 * Concatenates the outputFileName together with the appropriate file extension.
	 */
	private String getCompleteFileName(Uri remoteFileUri) {
		String fileExt = (outputFileExtension != null)  ?  outputFileExtension  :   MimeTypeMap.getFileExtensionFromUrl(remoteFileUri.toString());
		return outputFileName + "." + fileExt;
	}


	/**
	 * Notify the user whether the download was successful or not.
	 */
	private void fileDownloadStatus() {
		boolean downloadSuccessful = false;
		Uri downloadedFileUri  = null;
		Cursor cursor = ContextCompat.getSystemService(getContext(), DownloadManager.class)
				.query(new DownloadManager.Query().setFilterById(downloadId));

		if (cursor != null  &&  cursor.moveToFirst()) {
			final int columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
			downloadSuccessful = (cursor.getInt(columnIndex) == DownloadManager.STATUS_SUCCESSFUL);

			if (downloadSuccessful) {
				downloadedFileUri = Uri.parse(cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)));
			} else {
				final int columnReason = cursor.getColumnIndex(DownloadManager.COLUMN_REASON);
				final int reason = cursor.getInt(columnReason);

				// output why the download has failed...
				Logger.e(this, "Download failed.  Reason=%s", reason);
			}

			cursor.close();
		}

		getContext().unregisterReceiver(onComplete);

		// file download is now completed
		onFileDownloadCompleted(downloadSuccessful, downloadedFileUri);
	}


	public FileDownloader setRemoteFileUrl(String remoteFileUrl) {
		this.remoteFileUrl = remoteFileUrl;
		return this;
	}

	/**
	 * Set the output directory type which is passed to {@link Context#getExternalFilesDir(String)}.
	 *
	 * @param dirType  E.g. Environment.DIRECTORY_MOVIES, Environment.DIRECTORY_PICTURES
	 */
	public FileDownloader setDirType(String dirType) {
		this.dirType = dirType;
		return this;
	}

	public FileDownloader setTitle(String title) {
		this.title = title;
		return this;
	}

	public FileDownloader setDescription(String description) {
		this.description = description;
		return this;
	}

	/**
	 * Set the output file name (excluding the file extension).
	 *
	 * @param outputFileName    E.g. "Hello"
	 */
	public FileDownloader setOutputFileName(String outputFileName) {
		// replace all the special characters with space.
		this.outputFileName = invalidCharacters.matcher(outputFileName).replaceAll(" ").trim();
		return this;
	}

	/**
	 * Set the output file directory name.
	 *
	 * @param outputDirectoryName    E.g. "Hello"
	 */
	public FileDownloader setOutputDirectoryName(String outputDirectoryName) {
		// replace all the special characters with space.
		this.outputDirectoryName = invalidCharacters.matcher(outputDirectoryName).replaceAll(" ").trim();
		return this;
	}

	/**
	 * Set the parent directory.
	 *
	 * @param parentDirectory    E.g. "/storage/emulated/0/videos"
	 */
	public FileDownloader setParentDirectory(File parentDirectory) {
		this.parentDirectory = parentDirectory;
		return this;
	}

	/**
	 * Set the output file's extension.
	 *
	 * @param outputFileExtension   E.g. "mp4"
	 */
	public FileDownloader setOutputFileExtension(String outputFileExtension) {
		this.outputFileExtension = outputFileExtension;
		return this;
	}

	/**
	 * If set to true the {@link FileDownloader} will download the remote file even if the user is
	 * using cellular network.
	 */
	public FileDownloader setAllowedOverRoaming(Boolean allowedOverRoaming) {
		this.allowedOverRoaming = allowedOverRoaming;
		return this;
	}

	public FileDownloader setAllowedNetworkTypesFlags(Integer allowedNetworkTypesFlags) {
		this.allowedNetworkTypesFlags = allowedNetworkTypesFlags;
		return this;
	}

	////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Method called when we just started downloading the file.
	 */
	public abstract void onFileDownloadStarted();

    /**
     * Method called when starting the download failed.
     */
    public abstract void onDownloadStartFailed(String downloadName, RuntimeException runtimeException);

	/**
	 * Method called when the Android's DownloadManager has finished working on the given remote
	 * file.
	 *
	 * @param success       True if the remote file was downloaded successfully.
	 * @param localFileUri  If success == true, then this will hold the Uri of the downloaded file.
	 */
	public abstract void onFileDownloadCompleted(boolean success, Uri localFileUri);


	/**
	 * Method called if the external storage is not available and cannot be used by the app (e.g.
	 * user has ejected the SD card).
	 */
	public abstract void onExternalStorageNotAvailable();

	private class FileInformation {
		private final String fullDownloadFileName;
		private final File file;

		public FileInformation(String downloadFileName,
				File parentDirectory, String dirType) {
			// if there's already a local file for this video for some reason, then do not redownload the
			// file and halt
			final File externalStorageDir = Environment.getExternalStoragePublicDirectory(dirType);
			File parentDir = parentDirectory != null ? parentDirectory : externalStorageDir;
			boolean toDirectories = SkyTubeApp.getSettings().isDownloadToSeparateFolders();

			if (toDirectories && outputDirectoryName != null && !outputDirectoryName.isEmpty()) {
				parentDir = new File(parentDir, outputDirectoryName);
				if (!parentDir.exists()) {
					parentDir.mkdirs();
				}
				fullDownloadFileName = outputDirectoryName + '/' + downloadFileName;
			} else {
				fullDownloadFileName = downloadFileName;
			}
			file = new File(parentDir, downloadFileName);
		}

		public String getFullDownloadFileName() {
			return fullDownloadFileName;
		}

		public File getFile() {
			return file;
		}
	}
}
