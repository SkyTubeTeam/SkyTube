/*
 * SkyTube
 * Copyright (C) 2015  Ramon Mifsud
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

package free.rm.skytube.gui.fragments;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.SpinnerAdapter;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;

import free.rm.skytube.R;
import free.rm.skytube.businessobjects.AsyncTaskParallel;
import free.rm.skytube.businessobjects.VideoCategory;
import free.rm.skytube.gui.businessobjects.FragmentEx;
import free.rm.skytube.gui.businessobjects.LoadingProgressBar;
import free.rm.skytube.gui.businessobjects.Logger;
import free.rm.skytube.gui.businessobjects.SubsAdapter;
import free.rm.skytube.gui.businessobjects.UpdatesChecker;
import free.rm.skytube.gui.businessobjects.VideoGridAdapter;
import free.rm.skytube.gui.businessobjects.WebStream;

/**
 * A fragment that will hold a {@link GridView} full of YouTube videos.
 */
@SuppressWarnings("deprecation")
public class VideosGridFragment extends FragmentEx implements ActionBar.OnNavigationListener {

	protected RecyclerView			gridView;
	protected VideoGridAdapter		videoGridAdapter;
	private RecyclerView			subsListView = null;
	private SubsAdapter				subsAdapter = null;
	private ActionBarDrawerToggle	subsDrawerToggle;
	private View					progressBar = null;
	private int					 spinnerSelectedValue = 0;

	/** Set to true of the UpdatesCheckerTask has run; false otherwise. */
	private static boolean updatesCheckerTaskRan = false;


	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		// check for updates (one time only)
		if (!updatesCheckerTaskRan)
			new UpdatesCheckerTask().executeInParallel();
	}


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// inflate the layout for this fragment
		View view = inflater.inflate(R.layout.fragment_videos_grid, container, false);

		// set up the loading progress bar
		this.progressBar = view.findViewById(R.id.loading_progress_bar);

		// setup the video grid view
		this.gridView = (RecyclerView) view.findViewById(R.id.grid_view);
		if (this.videoGridAdapter == null) {
			this.videoGridAdapter = new VideoGridAdapter(getActivity());
		}
		this.gridView.setLayoutManager(new GridLayoutManager(getActivity(), getResources().getInteger(R.integer.video_grid_num_columns)));
		this.gridView.setAdapter(this.videoGridAdapter);

		// setup the toolbar / actionbar
		Toolbar toolbar = (Toolbar) view.findViewById(R.id.activity_main_toolbar);
		setSupportActionBar(toolbar);

		// indicate that this fragment has an action bar menu
		setHasOptionsMenu(true);

		DrawerLayout subsDrawerLayout = (DrawerLayout) view.findViewById(R.id.subs_drawer_layout);
		subsDrawerToggle = new ActionBarDrawerToggle(
				getActivity(),
				subsDrawerLayout,
				R.string.app_name,
				R.string.app_name
		);
		subsDrawerToggle.setDrawerIndicatorEnabled(true);
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
			actionBar.setHomeButtonEnabled(true);
		}

		this.subsListView = (RecyclerView) view.findViewById(R.id.subs_drawer);
		if (subsAdapter == null) {
			this.subsAdapter = SubsAdapter.get(getActivity(), view.findViewById(R.id.subs_drawer_progress_bar));
		}

		this.subsListView.setLayoutManager(new LinearLayoutManager(getActivity()));
		this.subsListView.setAdapter(this.subsAdapter);
		return view;
	}


	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			SpinnerAdapter spinnerAdapter =
					ArrayAdapter.createFromResource(actionBar.getThemedContext(), R.array.video_categories,
							android.R.layout.simple_spinner_dropdown_item);

			actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
			actionBar.setDisplayShowTitleEnabled(false);
			actionBar.setListNavigationCallbacks(spinnerAdapter, this);

			actionBar.setSelectedNavigationItem(spinnerSelectedValue);
		}

		subsDrawerToggle.syncState();
	}


	@Override
	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
		spinnerSelectedValue = itemPosition;
		// scroll to the top
		gridView.smoothScrollToPosition(0);

		LoadingProgressBar.get().setProgressBar(progressBar);

		// set/change the video category
		videoGridAdapter.setVideoCategory(VideoCategory.getVideoCategory(itemPosition));

		return true;	// true means event was handled
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Pass the event to ActionBarDrawerToggle, if it returns true, then it has handled the app
		// icon touch event
		if (subsDrawerToggle.onOptionsItemSelected(item)) {
			return true;
		}

		// Handle your other action bar items...
		return super.onOptionsItemSelected(item);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////


	private class UpdatesCheckerTask extends AsyncTaskParallel<Void, Void, UpdatesChecker> {

		@Override
		protected UpdatesChecker doInBackground(Void... params) {
			UpdatesChecker updatesChecker = new UpdatesChecker();
			updatesChecker.checkForUpdates();
			return updatesChecker;
		}

		@Override
		protected void onPostExecute(final UpdatesChecker updatesChecker) {
			updatesCheckerTaskRan = true;

			if (updatesChecker != null && updatesChecker.getLatestApkUrl() != null) {
				new AlertDialog.Builder(getActivity())
						.setTitle(R.string.update_available)
						.setMessage( String.format(getResources().getString(R.string.update_dialog_msg), updatesChecker.getLatestApkVersion()) )
						.setPositiveButton(R.string.update, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								new UpgradeAppTask(updatesChecker.getLatestApkUrl()).executeInParallel();
							}
						})
						.setNegativeButton(R.string.later, null)
						.show();
			}
		}

	}


	/**
	 * This task will download the remote APK file and it will install it for the user (provided that
	 * the user accepts such installation).
	 */
	private class UpgradeAppTask extends AsyncTaskParallel<Void, Integer, Pair<File, Throwable>> {

		private URL				apkUrl;
		private ProgressDialog	downloadDialog;

		private final String TAG = UpgradeAppTask.class.getSimpleName();


		public UpgradeAppTask(URL apkUrl) {
			this.apkUrl = apkUrl;
		}


		@Override
		protected void onPreExecute() {
			// setup the download dialog and display it
			downloadDialog = new ProgressDialog(getActivity());
			downloadDialog.setMessage(getString(R.string.downloading));
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
		 * Download the remote APK file and return an instance of {@link File}.
		 *
		 * @return	A {@link File} instance of the downloaded APK.
		 * @throws IOException
		 */
		private File downloadApk() throws IOException {
			WebStream		webStream = new WebStream(this.apkUrl);
			File			apkFile = File.createTempFile("skytube-upgrade", ".apk", getActivity().getCacheDir());
			OutputStream	out;

			// set the APK file to readable to every user so that this file can be read by Android's
			// package manager program
			apkFile.setReadable(true /*set file to readable*/, false /*set readable to every user on the system*/);
			out = new FileOutputStream(apkFile);

			// download the file by transferring bytes from in to out
			byte[]	buf = new byte[1024];
			int		totalBytesRead = 0;
			for (int bytesRead; (bytesRead = webStream.getStream().read(buf)) > 0; ) {
				out.write(buf, 0, bytesRead);

				// update the progressbar of the downloadDialog
				totalBytesRead += bytesRead;
				publishProgress(totalBytesRead, webStream.getStreamSize());
			}

			// close the streams
			webStream.getStream().close();
			out.close();

			return apkFile;
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
				Toast.makeText(getActivity(), R.string.update_failure, Toast.LENGTH_LONG).show();
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
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);	// asks the user to open the newly updated app
			startActivity(intent);
		}

	}

}
