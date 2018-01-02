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
import android.webkit.MimeTypeMap;

import static free.rm.skytube.app.SkyTubeApp.getContext;

/**
 * Downloads remote files by using Android's {@link DownloadManager}.
 */
public abstract class FileDownloader {
	private Uri     remoteFileUri = null;
	private String  dirType = null;
	private String  title = null;
	private String  description = null;
	private String  outputFileName = null;
	private Boolean allowedOverRoaming = null;
	private Integer allowedNetworkTypesFlags = null;

	private DownloadManager downloadManager;
	private long            downloadId;

	private BroadcastReceiver onComplete = new BroadcastReceiver() {
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


	protected FileDownloader() {
		downloadManager = (DownloadManager) getContext().getSystemService(Context.DOWNLOAD_SERVICE);
	}


	/**
	 * Download the remote file.
	 *
	 * <p>Android's DownloadManager will be used to download the image on our behalf.</p>
	 */
	public void download() {
		checkIfVariablesWereSet();

		DownloadManager.Request request = new DownloadManager.Request(remoteFileUri)
				.setAllowedOverRoaming(allowedOverRoaming)
				.setTitle(title)
				.setDescription(description)
				.setDestinationInExternalFilesDir(getContext(), dirType, getCompleteFileName(outputFileName, remoteFileUri));

		if (!allowedOverRoaming) {
			request.setAllowedNetworkTypes(allowedNetworkTypesFlags);
		}

		// onComplete.onReceive() will be executed once the file is downloaded
		getContext().registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

		// start downloading
		downloadId = downloadManager.enqueue(request);
	}


	/**
	 * Will check if the mandatory instance variables have been set.
	 */
	private void checkIfVariablesWereSet() {
		if (remoteFileUri == null  ||  dirType == null  ||  title == null
				||  outputFileName == null  ||  allowedOverRoaming == null
				|| (allowedOverRoaming == false  &&  allowedNetworkTypesFlags == null)) {
			throw new IllegalStateException("On of the parameters was not set for the FileDownloader");
		}
	}


	/**
	 * Concatenates the outputFileName together with the appropriate file extension.
	 */
	private String getCompleteFileName(String outputFileName, Uri remoteFileUri) {
		return outputFileName + "." + MimeTypeMap.getFileExtensionFromUrl(remoteFileUri.toString());
	}


	/**
	 * Notify the user whether the download was successful or not.
	 */
	private void fileDownloadStatus() {
		boolean downloadSuccessful = false;
		Uri     downloadedFileUri  = null;
		Cursor  cursor = downloadManager.query(new DownloadManager.Query().setFilterById(downloadId));

		if (cursor != null  &&  cursor.moveToFirst()) {
			int columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
			downloadSuccessful = (cursor.getInt(columnIndex) == DownloadManager.STATUS_SUCCESSFUL);

			if (downloadSuccessful) {
				downloadedFileUri = Uri.parse(cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)));
			}

			cursor.close();
		}

		getContext().unregisterReceiver(onComplete);

		onFileDownloadCompleted(downloadSuccessful, downloadedFileUri);
	}


	public FileDownloader setRemoteFileUri(Uri remoteFileUri) {
		this.remoteFileUri = remoteFileUri;
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
		this.outputFileName = outputFileName;
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


	/**
	 * Method called when the Android's DownloadManager has finished working on the given remote
	 * file.
	 *
	 * @param success       True if the remote file was downloaded successfully.
	 * @param localFileUri  If success == true, then this will hold the Uri of the downloaded file.
	 */
	public abstract void onFileDownloadCompleted(boolean success, Uri localFileUri);

}
