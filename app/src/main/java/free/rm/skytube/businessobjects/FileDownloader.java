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

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.webkit.MimeTypeMap;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.Serializable;
import java.util.regex.Pattern;

import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.download.DownloadManager;
import free.rm.skytube.businessobjects.download.DownloadMission;
import free.rm.skytube.businessobjects.download.DownloadService;
import free.rm.skytube.gui.activities.PermissionsActivity;

import static free.rm.skytube.app.SkyTubeApp.getContext;

/**
 * Downloads remote files by using the download engine ported from NewPipe.
 *
 * <p>The engine downloads the file in multiple ranges in parallel, supports
 * resuming interrupted downloads and reports its progress through a
 * foreground service.</p>
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

	/** Identifier of the logical download that this file belongs to. */
	private String notificationGroupId = null;
	/** Human readable title shared by all files of the same download. */
	private String notificationGroupTitle = null;
	/** Number of files that make up the logical download. */
	private int notificationGroupSize = 0;

	/** Number of parallel connections used by the download engine. */
	private int threadCount = 3;

	private transient DownloadService.MissionCompletionListener completionListener;
	/** The destination file of the last started download, or null before the first start. */
	private transient File downloadDestinationFile = null;

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
	 * <p>The multi-threaded download engine ported from NewPipe is used to
	 * download the file on our behalf.</p>
	 */
	private void download() {
		// check if the mandatory variables were set -- if not halt the program.
		checkIfVariablesWereSet();

		Uri     remoteFileUri = Uri.parse(remoteFileUrl);
		String  downloadFileName = getCompleteFileName(remoteFileUri);
		FileInformation fileInformation = new FileInformation(downloadFileName, parentDirectory, dirType);
		final File downloadDestinationFile = fileInformation.getFile();

		Logger.w(this, "Downloading video %s into %s -> %s", outputFileName, outputDirectoryName, downloadDestinationFile.getAbsolutePath());

		this.downloadDestinationFile = downloadDestinationFile;

		// if the external storage is not available then halt the download operation
		if (!isExternalStorageAvailable()) {
			reportTask(false);
			onExternalStorageNotAvailable();
			return;
		}

		// A file is only complete if there is no pending mission for it: a
		// partially downloaded file must be resumed instead of being treated
		// as already downloaded.
		if (downloadDestinationFile.exists()
				&& !DownloadManager.hasPendingMission(getContext(), downloadDestinationFile)) {
			// let the aggregate notification know that this file is done,
			// even though the download engine never worked on it
			reportTask(true);
			onFileDownloadCompleted(true, Uri.parse(downloadDestinationFile.toURI().toString()));
			return;
		}

		DownloadMission mission = createMission(downloadDestinationFile);

		completionListener = new DownloadService.MissionCompletionListener() {
			@Override
			public void onCompleted(DownloadMission completedMission) {
				onFileDownloadCompleted(true,
						Uri.parse(completedMission.targetFile.toURI().toString()));
			}

			@Override
			public void onFailed(DownloadMission failedMission, Exception error) {
				RuntimeException runtimeException = error instanceof RuntimeException
						? (RuntimeException) error
						: new RuntimeException(error);
				onDownloadStartFailed(title, runtimeException);
			}
		};

		// start downloading
		try {
			DownloadService.startDownload(getContext(), mission, completionListener);
			onFileDownloadStarted();
		} catch (RuntimeException re) {
			Logger.e(this, "Download failed:" + re.getMessage(), re);
			reportTask(false);
			onDownloadStartFailed(title, re);
		}
	}

	private DownloadMission createMission(File downloadDestinationFile) {
		DownloadMission mission = new DownloadMission(
				new String[]{remoteFileUrl}, downloadDestinationFile);
		mission.title = title;
		mission.kind = getKind();
		mission.notificationGroupId = notificationGroupId;
		mission.notificationGroupTitle = notificationGroupTitle;
		mission.notificationGroupSize = notificationGroupSize;
		mission.threadCount = threadCount;
		mission.allowedOverRoaming = allowedOverRoaming != null ? allowedOverRoaming : true;
		mission.allowedNetworkTypes = allowedNetworkTypesFlags != null
				? allowedNetworkTypesFlags
				: (android.app.DownloadManager.Request.NETWORK_WIFI
						| android.app.DownloadManager.Request.NETWORK_MOBILE);
		return mission;
	}

	/**
	 * Reports the outcome of this task to the download service, so the
	 * aggregate notification of the related download can reach its final
	 * state.  The completion listeners are not involved.
	 */
	private void reportTask(boolean finished) {
		if (downloadDestinationFile == null) {
			return;
		}
		DownloadService.reportTaskResult(getContext(), createMission(downloadDestinationFile), finished);
	}

	/**
	 * The destination file of the last started download, or null before the
	 * first start.
	 */
	@Nullable
	public File getDownloadDestinationFile() {
		return downloadDestinationFile;
	}

	/**
	 * Deletes the downloaded (or partially downloaded) file of this
	 * downloader.  The completion listeners of the related download are not
	 * involved.
	 */
	public void deleteDownloadedFile() {
		if (downloadDestinationFile == null) {
			return;
		}
		File partFile = new File(downloadDestinationFile.getAbsolutePath() + ".part");
		boolean deleted = true;
		if (partFile.exists() && !partFile.delete()) {
			deleted = false;
		}
		if (downloadDestinationFile.exists() && !downloadDestinationFile.delete()) {
			deleted = false;
		}
		if (!deleted) {
			Logger.w(this, "Could not delete downloaded file: %s", downloadDestinationFile);
		}
	}

	private char getKind() {
		if (Environment.DIRECTORY_MOVIES.equals(dirType)) {
			return 'v';
		} else if (Environment.DIRECTORY_PICTURES.equals(dirType)) {
			return 's';
		}
		return '?';
	}


	/**
	 * Will check if the mandatory instance variables were set.
	 */
	private void checkIfVariablesWereSet() {
		if (remoteFileUrl == null  ||  dirType == null  ||  title == null
				||  outputFileName == null  ||  allowedOverRoaming == null
				|| (!allowedOverRoaming &&  allowedNetworkTypesFlags == null)) {
			throw new IllegalStateException("One of the parameters was not set for the FileDownloader");
		}
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
	 * @param parentDirectory   E.g. "/storage/emulated/0/videos"
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

	/**
	 * Associates this file with a logical download that may consist of
	 * several files, so the service can show one aggregate notification.
	 *
	 * @param groupId unique identifier of the logical download
	 * @param title   title shown in the aggregate notification
	 * @param size    number of files that make up the logical download
	 */
	public FileDownloader setNotificationGroup(String groupId, String title, int size) {
		this.notificationGroupId = groupId;
		this.notificationGroupTitle = title;
		this.notificationGroupSize = Math.max(0, size);
		return this;
	}

	/**
	 * Associates this file with an already started logical download (see
	 * {@link #setNotificationGroup(String, String, int)}).  Used for the
	 * members of a group that are started after the first one.
	 *
	 * @param groupId unique identifier of the logical download
	 * @param size    number of files that make up the logical download
	 */
	public FileDownloader setNotificationGroup(String groupId, int size) {
		this.notificationGroupId = groupId;
		this.notificationGroupSize = Math.max(0, size);
		return this;
	}

	/**
	 * Set the number of parallel connections used by the download engine.
	 *
	 * @param threadCount E.g. 1 for a single connection, 3 for the default
	 */
	public FileDownloader setThreadCount(int threadCount) {
		this.threadCount = Math.max(1, threadCount);
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
	 * Method called when the download engine has finished working on the given
	 * remote file.
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
		private final File file;

		public FileInformation(String downloadFileName,
				File parentDirectory, String dirType) {
			// if there's already a local file for this video for some reason, then do not redownload the
			// file and halt
			File parentDir;
			if (parentDirectory != null) {
				parentDir = parentDirectory;
			} else {
				File externalStorageDir = Environment.getExternalStoragePublicDirectory(dirType);
				parentDir = externalStorageDir != null
						? externalStorageDir
						: new File(System.getProperty("user.dir"), dirType);
			}
			boolean toDirectories = SkyTubeApp.getSettings().isDownloadToSeparateFolders();

			if (toDirectories && outputDirectoryName != null && !outputDirectoryName.isEmpty()) {
				parentDir = new File(parentDir, outputDirectoryName);
				if (!parentDir.exists()) {
					parentDir.mkdirs();
				}
			}
			file = new File(parentDir, downloadFileName);
		}

		public File getFile() {
			return file;
		}
	}
}
