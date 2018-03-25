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

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;

import butterknife.BindView;
import butterknife.ButterKnife;
import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubePlaylist;
import free.rm.skytube.businessobjects.db.DownloadedVideosDb;
import free.rm.skytube.businessobjects.db.SearchHistoryDb;
import free.rm.skytube.businessobjects.db.SearchHistoryTable;
import free.rm.skytube.businessobjects.interfaces.SearchHistoryClickListener;
import free.rm.skytube.gui.businessobjects.MainActivityListener;
import free.rm.skytube.gui.businessobjects.YouTubePlayer;
import free.rm.skytube.gui.businessobjects.adapters.SearchHistoryCursorAdapter;
import free.rm.skytube.gui.businessobjects.updates.UpdatesCheckerTask;
import free.rm.skytube.gui.fragments.ChannelBrowserFragment;
import free.rm.skytube.gui.fragments.MainFragment;
import free.rm.skytube.gui.fragments.PlaylistVideosFragment;
import free.rm.skytube.gui.fragments.SearchVideoGridFragment;

/**
 * Main activity (launcher).  This activity holds {@link free.rm.skytube.gui.fragments.VideosGridFragment}.
 */
public class MainActivity extends AppCompatActivity implements MainActivityListener {

	@BindView(R.id.fragment_container)
	protected FrameLayout fragmentContainer;

	private MainFragment mainFragment;
	private SearchVideoGridFragment searchVideoGridFragment;
	private ChannelBrowserFragment channelBrowserFragment;
	/** Fragment that shows Videos from a specific Playlist */
	private PlaylistVideosFragment playlistVideosFragment;

	private boolean dontAddToBackStack = false;

	/** Set to true of the UpdatesCheckerTask has run; false otherwise. */
	private static boolean updatesCheckerTaskRan = false;

	public static final String ACTION_VIEW_CHANNEL = "MainActivity.ViewChannel";
	public static final String ACTION_VIEW_FEED = "MainActivity.ViewFeed";
	public static final String ACTION_VIEW_PLAYLIST = "MainActivity.ViewPlaylist";
	private static final String MAIN_FRAGMENT   = "MainActivity.MainFragment";
	private static final String SEARCH_FRAGMENT = "MainActivity.SearchFragment";
	public static final String CHANNEL_BROWSER_FRAGMENT = "MainActivity.ChannelBrowserFragment";
	public static final String PLAYLIST_VIDEOS_FRAGMENT = "MainActivity.PlaylistVideosFragment";



	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// check for updates (one time only)
		if (!updatesCheckerTaskRan) {
			new UpdatesCheckerTask(this, false).executeInParallel();
			updatesCheckerTaskRan = true;
		}

		SkyTubeApp.setFeedUpdateInterval();
		// Delete any missing downloaded videos
		new DownloadedVideosDb.RemoveMissingVideosTask().executeInParallel();

		setContentView(R.layout.activity_fragment_holder);
		ButterKnife.bind(this);

		if(fragmentContainer != null) {
			if(savedInstanceState != null) {
				mainFragment = (MainFragment)getSupportFragmentManager().getFragment(savedInstanceState, MAIN_FRAGMENT);
				searchVideoGridFragment = (SearchVideoGridFragment) getSupportFragmentManager().getFragment(savedInstanceState, SEARCH_FRAGMENT);
				channelBrowserFragment = (ChannelBrowserFragment) getSupportFragmentManager().getFragment(savedInstanceState, CHANNEL_BROWSER_FRAGMENT);
				playlistVideosFragment = (PlaylistVideosFragment) getSupportFragmentManager().getFragment(savedInstanceState, PLAYLIST_VIDEOS_FRAGMENT);

			}

			// If this Activity was called to view a particular channel, display that channel via ChannelBrowserFragment, instead of MainFragment
			String action = getIntent().getAction();
			if(ACTION_VIEW_CHANNEL.equals(action)) {
				dontAddToBackStack = true;
				YouTubeChannel channel = (YouTubeChannel) getIntent().getSerializableExtra(ChannelBrowserFragment.CHANNEL_OBJ);
				onChannelClick(channel);
			} else if(ACTION_VIEW_PLAYLIST.equals(action)) {
				dontAddToBackStack = true;
				YouTubePlaylist playlist = (YouTubePlaylist)getIntent().getSerializableExtra(PlaylistVideosFragment.PLAYLIST_OBJ);
				onPlaylistClick(playlist);
			} else {
				if(mainFragment == null) {
					mainFragment = new MainFragment();
					// If we're coming here via a click on the Notification that new videos for subscribed channels have been found, make sure to
					// select the Feed tab.
					if(action != null && action.equals(ACTION_VIEW_FEED)) {
						Bundle args = new Bundle();
						args.putBoolean(MainFragment.SHOULD_SELECTED_FEED_TAB, true);
						mainFragment.setArguments(args);
					}
					getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, mainFragment).commit();
				}
			}
		}
	}


	@Override
	protected void onSaveInstanceState(Bundle outState) {
		if(mainFragment != null)
			getSupportFragmentManager().putFragment(outState, MAIN_FRAGMENT, mainFragment);
		if(searchVideoGridFragment != null && searchVideoGridFragment.isVisible())
			getSupportFragmentManager().putFragment(outState, SEARCH_FRAGMENT, searchVideoGridFragment);
		if(channelBrowserFragment != null && channelBrowserFragment.isVisible())
			getSupportFragmentManager().putFragment(outState, CHANNEL_BROWSER_FRAGMENT, channelBrowserFragment);
		if(playlistVideosFragment != null && playlistVideosFragment.isVisible())
			getSupportFragmentManager().putFragment(outState, PLAYLIST_VIDEOS_FRAGMENT, playlistVideosFragment);
		super.onSaveInstanceState(outState);
	}


	@Override
	protected void onResume() {
		super.onResume();

		// Activity may be destroyed when the devices is rotated, so we need to make sure that the
		// channel play list is holding a reference to the activity being currently in use...
		if (channelBrowserFragment != null)
			channelBrowserFragment.getChannelPlaylistsFragment().setMainActivityListener(this);
	}


	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_activity_menu, menu);

		// setup the SearchView (actionbar)
		final MenuItem searchItem = menu.findItem(R.id.menu_search);
		final SearchView searchView = (SearchView) searchItem.getActionView();

		searchView.setQueryHint(getString(R.string.search_videos));

		// set the query hints to be equal to the previously searched text
		searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
			@Override
			public boolean onQueryTextChange(final String newText) {
				// if the user does not want to have the search string saved, then skip the below...
				if (SkyTubeApp.getPreferenceManager().getBoolean(getString(R.string.pref_key_disable_search_history), false)
						||  newText == null  ||  newText.length() <= 1) {
					return false;
				}

				SearchHistoryCursorAdapter searchHistoryCursorAdapter = (SearchHistoryCursorAdapter) searchView.getSuggestionsAdapter();
				Cursor cursor = SearchHistoryDb.getSearchHistoryDb().getSearchCursor(newText);

				// if the adapter has not been created, then create it
				if (searchHistoryCursorAdapter == null) {
					searchHistoryCursorAdapter = new SearchHistoryCursorAdapter(getBaseContext(),
							R.layout.search_hint,
							cursor,
							new String[]{SearchHistoryTable.COL_SEARCH_TEXT},
							new int[]{android.R.id.text1},
							0);
					searchHistoryCursorAdapter.setSearchHistoryClickListener(new SearchHistoryClickListener() {
						@Override
						public void onClick(String query) {
							displaySearchResults(query);
						}
					});
					searchView.setSuggestionsAdapter(searchHistoryCursorAdapter);
				} else {
					// else just change the cursor...
					searchHistoryCursorAdapter.changeCursor(cursor);
				}

				return true;
			}

			@Override
			public boolean onQueryTextSubmit(String query) {
				// hide the keyboard
				searchView.clearFocus();

				if(!SkyTubeApp.getPreferenceManager().getBoolean(SkyTubeApp.getStr(R.string.pref_key_disable_search_history), false)) {
					// Save this search string into the Search History Database (for Suggestions)
					SearchHistoryDb.getSearchHistoryDb().insertSearchText(query);
				}

				displaySearchResults(query);

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

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
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
					YouTubePlayer.launch(videoUrl, MainActivity.this);
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
		String              clipboardText    = "";
		ClipboardManager    clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

		// if the clipboard contain data ...
		if (clipboardManager != null  &&  clipboardManager.hasPrimaryClip()) {
			ClipData.Item item = clipboardManager.getPrimaryClip().getItemAt(0);

			// gets the clipboard as text.
			clipboardText = item.getText().toString();
		}

		return clipboardText;
	}


	@Override
	public void onBackPressed() {
		if (mainFragment != null  &&  mainFragment.isVisible()) {
			// If the Subscriptions Drawer is open, close it instead of minimizing the app.
			if(mainFragment.isDrawerOpen()) {
				mainFragment.closeDrawer();
			} else {
				// On Android, when the user presses back button, the Activity is destroyed and will be
				// recreated when the user relaunches the app.
				// We do not want that behaviour, instead then the back button is pressed, the app will
				// be **minimized**.
				Intent startMain = new Intent(Intent.ACTION_MAIN);
				startMain.addCategory(Intent.CATEGORY_HOME);
				startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(startMain);
			}
		} else {
			super.onBackPressed();
		}
	}


	private void switchToFragment(Fragment fragment) {
		FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

		transaction.replace(R.id.fragment_container, fragment);
		if(!dontAddToBackStack)
			transaction.addToBackStack(null);
		else
			dontAddToBackStack = false;
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
		channelBrowserFragment.getChannelPlaylistsFragment().setMainActivityListener(this);
		channelBrowserFragment.setArguments(args);
		switchToFragment(channelBrowserFragment);
	}

	@Override
	public void onPlaylistClick(YouTubePlaylist playlist) {
		playlistVideosFragment = new PlaylistVideosFragment();
		Bundle args = new Bundle();
		args.putSerializable(PlaylistVideosFragment.PLAYLIST_OBJ, playlist);
		playlistVideosFragment.setArguments(args);
		switchToFragment(playlistVideosFragment);
	}

	/**
	 * Switch to the Search Video Grid Fragment with the selected query to search for videos.
	 * @param query
	 */
	private void displaySearchResults(String query) {
		// open SearchVideoGridFragment and display the results
		searchVideoGridFragment = new SearchVideoGridFragment();
		Bundle bundle = new Bundle();
		bundle.putString(SearchVideoGridFragment.QUERY, query);
		searchVideoGridFragment.setArguments(bundle);
		switchToFragment(searchVideoGridFragment);
	}
}
