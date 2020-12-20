/*
 * SkyTube
 * Copyright (C) 2017  Ramon Mifsud
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

package free.rm.skytube.gui.businessobjects.updates;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;

import free.rm.skytube.R;
import free.rm.skytube.businessobjects.AsyncTaskParallel;

/**
 * This task will download the remote APK file and it will install it for the user (provided that
 * the user accepts such installation).
 */
public class UpgradeAppTask extends AsyncTaskParallel<Void, Integer, Pair<File, Throwable>> {

	private URL             apkUrl;
	private ProgressDialog  downloadDialog;

	/** The directory where the apks are downloaded to. */
	private final File      apkDir;
	private final Context   context;
	private final String TAG = UpgradeAppTask.class.getSimpleName();


	public UpgradeAppTask(URL apkUrl, Context context) {
		this.apkUrl = apkUrl;
		this.context = context;
		this.apkDir = context.getCacheDir();
	}


	@Override
	protected void onPreExecute() {
		// setup the download dialog and display it
		downloadDialog = new ProgressDialog(context);
		downloadDialog.setMessage(context.getString(R.string.downloading));
		downloadDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		downloadDialog.setProgress(0);
		downloadDialog.setMax(100);
		downloadDialog.setCancelable(false);
		downloadDialog.setProgressNumberFormat(null);
		downloadDialog.show();
	}

	@Override
	protected Pair<File, Throwable> doInBackground(Void... params) {
		File		apkFile;
		Throwable	exception = null;

		// delete old apk files
		deleteOldApkFiles();

		// try to download the remote APK file
		try {
			apkFile = downloadApk();
		} catch (Throwable e) {
			apkFile = null;
			exception = e;
		}

		return new Pair<>(apkFile, exception);
	}


	/**
	 * Delete old (previously-downloaded) APK files.
	 */
	private void deleteOldApkFiles() {
		// get all previously downloaded APK files
		File[] apkFiles = apkDir.listFiles((dir, filename) -> filename.endsWith(".apk"));

		// delete the previously downloaded APK files
		if (apkFiles != null) {
			for (File apkFile : apkFiles) {
				if (apkFile.delete()) {
					Log.i(TAG, "Deleted " + apkFile.getAbsolutePath());
				} else {
					Log.e(TAG, "Cannot delete " + apkFile.getAbsolutePath());
				}
			}
		}
	}


	/**
	 * Download the remote APK file and return an instance of {@link File}.
	 *
	 * @return	A {@link File} instance of the downloaded APK.
	 * @throws IOException
	 */
	private File downloadApk() throws IOException {
		try (WebStream webStream = new WebStream(this.apkUrl)) {
			File apkFile = File.createTempFile("skytube-upgrade", ".apk", apkDir);

			// set the APK file to readable to every user so that this file can be read by Android's
			// package manager program
			apkFile.setReadable(true /*set file to readable*/, false /*set readable to every user on the system*/);
			try (OutputStream out = new FileOutputStream(apkFile)) {
				byte[] buf = new byte[1024];
				int totalBytesRead = 0;
				for (int bytesRead; (bytesRead = webStream.getStream().read(buf)) > 0; ) {
					out.write(buf, 0, bytesRead);

					// update the progressbar of the downloadDialog
					totalBytesRead += bytesRead;
					publishProgress(totalBytesRead, webStream.getStreamSize());
				}
			}

			return apkFile;
		}
	}

	@Override
	protected void onProgressUpdate(Integer... values) {
		float	totalBytesRead = values[0];
		float	fileSize = values[1];
		float	percentageDownloaded = (totalBytesRead / fileSize) * 100f;

		downloadDialog.setProgress((int)percentageDownloaded);
	}

	@Override
	protected void onPostExecute(Pair<File, Throwable> out) {
		File		apkFile   = out.first;
		Throwable	exception = out.second;

		// hide the download dialog
		downloadDialog.dismiss();

		if (exception != null) {
			Log.e(TAG, "Unable to upgrade app", exception);
			Toast.makeText(context, R.string.update_failure, Toast.LENGTH_LONG).show();
		} else {
			displayUpgradeAppDialog(apkFile);
		}
	}


	/**
	 * Ask the user whether he wants to install the latest SkyTube's APK file.
	 *
	 * @param apkFile	APK file to install.
	 */
	private void displayUpgradeAppDialog(File apkFile) {
		Uri apkFileURI = (android.os.Build.VERSION.SDK_INT >= 24)
				? FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".provider", apkFile)  // we now need to call FileProvider.getUriForFile() due to security changes in Android 7.0+
				: Uri.fromFile(apkFile);
		Intent intent = new Intent(Intent.ACTION_VIEW);

		intent.setDataAndType(apkFileURI, "application/vnd.android.package-archive");
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK /* asks the user to open the newly updated app */
				| Intent.FLAG_GRANT_READ_URI_PERMISSION /* to avoid a crash due to security changes in Android 7.0+ */);
		context.startActivity(intent);
	}

}