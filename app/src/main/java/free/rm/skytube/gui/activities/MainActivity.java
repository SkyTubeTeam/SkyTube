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

package free.rm.skytube.gui.activities;

import android.app.ProgressDialog;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;

import butterknife.Bind;
import butterknife.ButterKnife;
import free.rm.skytube.R;
import free.rm.skytube.businessobjects.AsyncTaskParallel;
import free.rm.skytube.businessobjects.MainActivityListener;
import free.rm.skytube.businessobjects.YouTubeChannel;
import free.rm.skytube.gui.businessobjects.UpdatesChecker;
import free.rm.skytube.gui.businessobjects.WebStream;
import free.rm.skytube.gui.fragments.ChannelBrowserFragment;
import free.rm.skytube.gui.fragments.MainFragment;
import free.rm.skytube.gui.fragments.SearchVideoGridFragment;

/**
 * Main activity (launcher).  This activity holds {@link free.rm.skytube.gui.fragments.VideosGridFragment}.
 */
public class MainActivity extends AppCompatActivity implements MainActivityListener {
	public static final String ACTION_VIEW_CHANNEL = "MainActivity.ViewChannel";
	public static final String MAIN_FRAGMENT = "MainActivity.MainFragment";
	public static final String CHANNEL_BROWSER_FRAGMENT = "MainActivity.ChannelBrowserFragment";
	public static final String SEARCH_FRAGMENT = "MainActivity.SearchFragment";

	/** Set to true of the UpdatesCheckerTask has run; false otherwise. */
	private static boolean updatesCheckerTaskRan = false;

	@Bind(R.id.fragment_container)
	FrameLayout fragmentContainer;

	MainFragment mainFragment;
	ChannelBrowserFragment channelBrowserFragment;
	SearchVideoGridFragment searchVideoGridFragment;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// check for updates (one time only)
		if (!updatesCheckerTaskRan)
			new UpdatesCheckerTask().executeInParallel();

		setContentView(R.layout.activity_main);
		ButterKnife.bind(this);

		if(fragmentContainer != null) {
			if(savedInstanceState != null) {
				mainFragment = (MainFragment)getSupportFragmentManager().getFragment(savedInstanceState, MAIN_FRAGMENT);
				channelBrowserFragment = (ChannelBrowserFragment) getSupportFragmentManager().getFragment(savedInstanceState, CHANNEL_BROWSER_FRAGMENT);
				searchVideoGridFragment = (SearchVideoGridFragment) getSupportFragmentManager().getFragment(savedInstanceState, SEARCH_FRAGMENT);
			}
			String action = getIntent().getAction();
			if(action != null && action.equals(ACTION_VIEW_CHANNEL)) {
				YouTubeChannel channel = (YouTubeChannel) getIntent().getSerializableExtra(ChannelBrowserFragment.CHANNEL_OBJ);
				onChannelClick(channel);
			} else {
				if(mainFragment == null) {
					mainFragment = new MainFragment();
					getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, mainFragment).commit();
				}
			}
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		if(mainFragment != null)
			getSupportFragmentManager().putFragment(outState, MAIN_FRAGMENT, mainFragment);
		if(channelBrowserFragment != null && channelBrowserFragment.isVisible())
			getSupportFragmentManager().putFragment(outState, CHANNEL_BROWSER_FRAGMENT, channelBrowserFragment);
		if(searchVideoGridFragment != null && searchVideoGridFragment.isVisible())
			getSupportFragmentManager().putFragment(outState, SEARCH_FRAGMENT, searchVideoGridFragment);
		super.onSaveInstanceState(outState);
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_activity_menu, menu);

		// setup the SearchView (actionbar)
		final MenuItem searchItem = menu.findItem(R.id.menu_search);
		final SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
		searchView.setQueryHint(getString(R.string.search_videos));
		searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
			@Override
			public boolean onQueryTextChange(String newText) {
				return false;
			}

			@Override
			public boolean onQueryTextSubmit(String query) {
				// collapse the action-bar's search view
				searchView.setQuery("", false);
				searchView.setIconified(true);
				menu.findItem(R.id.menu_search).collapseActionView();

				searchVideoGridFragment = new SearchVideoGridFragment();
				Bundle bundle = new Bundle();
				bundle.putString(SearchVideoGridFragment.QUERY, query);
				searchVideoGridFragment.setArguments(bundle);
				switchToFragment(searchVideoGridFragment);

				return true;
			}
		});

		return true;
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_preferences:
				Intent i = new Intent(this, PreferencesActivity.class);
				startActivity(i);
				return true;
			case R.id.menu_enter_video_url:
				displayEnterVideoUrlDialog();
				return true;
			case android.R.id.home:
				if(mainFragment == null || !mainFragment.isVisible()) {
					onBackPressed();
					return true;
				}
		}

		return super.onOptionsItemSelected(item);
	}


	/**
	 * Display the Enter Video URL dialog.
	 */
	private void displayEnterVideoUrlDialog() {
		final AlertDialog alertDialog = new AlertDialog.Builder(this)
			.setView(R.layout.dialog_enter_video_url)
			.setTitle(R.string.enter_video_url)
			.setPositiveButton(R.string.play, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// get the inputted URL string
					final String videoUrl = ((EditText)((AlertDialog) dialog).findViewById(R.id.dialog_url_edittext)).getText().toString();

					// play the video
					Intent i = new Intent(MainActivity.this, YouTubePlayerActivity.class);
					i.setAction(Intent.ACTION_VIEW);
					i.setData(Uri.parse(videoUrl));
					startActivity(i);
				}
			})
			.setNegativeButton(R.string.cancel, null)
			.show();

		// paste whatever there is in the clipboard (hopefully it is a video url)
		((EditText) alertDialog.findViewById(R.id.dialog_url_edittext)).setText(getClipboardItem());

		// clear URL edittext button
		alertDialog.findViewById(R.id.dialog_url_clear_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				((EditText) alertDialog.findViewById(R.id.dialog_url_edittext)).setText("");
			}
		});
	}


	/**
	 * Return the last item stored in the clipboard.
	 *
	 * @return	{@link String}
	 */
	private String getClipboardItem() {
		String item = "";

		ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
		if (clipboard.hasPrimaryClip()) {
			android.content.ClipDescription description = clipboard.getPrimaryClipDescription();
			android.content.ClipData data = clipboard.getPrimaryClip();
			if (data != null && description != null && description.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN))
				item = String.valueOf(data.getItemAt(0).getText());
		}

		return item;
	}


	@Override
	public void onBackPressed() {
		// If coming here from the video player (channel was pressed), exit when the back button is pressed
		if(getIntent().getAction() != null && getIntent().getAction().equals(ACTION_VIEW_CHANNEL))
			finish();
		else
			super.onBackPressed();
	}


	private void switchToFragment(Fragment fragment) {
		FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

		transaction.replace(R.id.fragment_container, fragment);
		transaction.addToBackStack(null);
		transaction.commit();
	}



	@Override
	public void onChannelClick(YouTubeChannel channel) {
		Bundle args = new Bundle();
		args.putSerializable(ChannelBrowserFragment.CHANNEL_OBJ, channel);
		switchToChannelBrowserFragment(args);
	}



	@Override
	public void onChannelClick(String channelId) {
		Bundle args = new Bundle();
		args.putString(ChannelBrowserFragment.CHANNEL_ID, channelId);
		switchToChannelBrowserFragment(args);
	}



	private void switchToChannelBrowserFragment(Bundle args) {
		channelBrowserFragment = new ChannelBrowserFragment();
		channelBrowserFragment.setArguments(args);
		switchToFragment(channelBrowserFragment);
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
				new AlertDialog.Builder(MainActivity.this)
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

		private URL apkUrl;
		private ProgressDialog downloadDialog;

		private final String TAG = UpgradeAppTask.class.getSimpleName();


		public UpgradeAppTask(URL apkUrl) {
			this.apkUrl = apkUrl;
		}


		@Override
		protected void onPreExecute() {
			// setup the download dialog and display it
			downloadDialog = new ProgressDialog(MainActivity.this);
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
			WebStream webStream = new WebStream(this.apkUrl);
			File			apkFile = File.createTempFile("skytube-upgrade", ".apk", getCacheDir());
			OutputStream out;

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
				Toast.makeText(MainActivity.this, R.string.update_failure, Toast.LENGTH_LONG).show();
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
