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
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.mikepenz.actionitembadge.library.ActionItemBadge;
import com.mikepenz.actionitembadge.library.utils.BadgeStyle;

import java.io.Serializable;
import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.Logger;
import free.rm.skytube.businessobjects.TLSSocketFactory;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubePlaylist;
import free.rm.skytube.businessobjects.YouTube.VideoBlocker;
import free.rm.skytube.businessobjects.db.DownloadedVideosDb;
import free.rm.skytube.businessobjects.db.SearchHistoryDb;
import free.rm.skytube.businessobjects.db.SearchHistoryTable;
import free.rm.skytube.gui.businessobjects.BlockedVideosDialog;
import free.rm.skytube.gui.businessobjects.adapters.SearchHistoryCursorAdapter;
import free.rm.skytube.gui.businessobjects.fragments.FragmentEx;
import free.rm.skytube.gui.businessobjects.updates.UpdatesCheckerTask;
import free.rm.skytube.gui.fragments.ChannelBrowserFragment;
import free.rm.skytube.gui.fragments.MainFragment;
import free.rm.skytube.gui.fragments.PlaylistVideosFragment;
import free.rm.skytube.gui.fragments.SearchVideoGridFragment;
import free.rm.skytube.gui.fragments.SubscriptionsFeedFragment;

/**
 * Main activity (launcher).  This activity holds {@link free.rm.skytube.gui.fragments.VideosGridFragment}.
 * Do NOT change this activity's superclass, as it needs to be {@link free.rm.skytube.gui.activities.BaseActivity} in order
 * for Chromecast support to work (on the Extra variant - OSS variant's BaseActivity just has empty no-op methods for
 * the Chromecast specific functionality)
 */
public class MainActivity extends BaseActivity {
	@BindView(R.id.fragment_container)
	protected FrameLayout           fragmentContainer;

	private MainFragment            mainFragment;
	private SearchVideoGridFragment searchVideoGridFragment;
	private ChannelBrowserFragment  channelBrowserFragment;
	/** Fragment that shows Videos from a specific Playlist */
	private PlaylistVideosFragment  playlistVideosFragment;
	private VideoBlockerPlugin      videoBlockerPlugin;

	private boolean dontAddToBackStack = false;

	/** Set to true of the UpdatesCheckerTask has run; false otherwise. */
	private static boolean updatesCheckerTaskRan = false;

	public static final String ACTION_VIEW_CHANNEL = "MainActivity.ViewChannel";
	public static final String ACTION_VIEW_FEED = "MainActivity.ViewFeed";
	public static final String ACTION_VIEW_PLAYLIST = "MainActivity.ViewPlaylist";
	private static final String MAIN_FRAGMENT   = "MainActivity.MainFragment";
	private static final String SEARCH_FRAGMENT = "MainActivity.SearchFragment";
	private static final String CHANNEL_BROWSER_FRAGMENT = "MainActivity.ChannelBrowserFragment";
	private static final String PLAYLIST_VIDEOS_FRAGMENT = "MainActivity.PlaylistVideosFragment";
	private static final String VIDEO_BLOCKER_PLUGIN = "MainActivity.VideoBlockerPlugin";


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// To enable downloading with https on pre-kitkat devices.
		if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
			TLSSocketFactory.setAsDefault();
		}

		// check for updates (one time only)
		if (!updatesCheckerTaskRan) {
			new UpdatesCheckerTask(this, false).executeInParallel();
			updatesCheckerTaskRan = true;
		}

		SkyTubeApp.setFeedUpdateInterval();
		// Delete any missing downloaded videos
		new DownloadedVideosDb.RemoveMissingVideosTask().executeInParallel();

		setContentView(R.layout.activity_main);

		// The Extra variant needs to initialize some Fragments that are used for Chromecast control. This is done in onLayoutSet of BaseActivity.
		// The OSS variant has a no-op version of this method, since it doesn't need to do anything else here.
		onLayoutSet();

		ButterKnife.bind(this);

		if(fragmentContainer != null) {
			if(savedInstanceState != null) {
				final FragmentManager supportFragmentManager = getSupportFragmentManager();
				mainFragment = (MainFragment) supportFragmentManager.getFragment(savedInstanceState, MAIN_FRAGMENT);
				searchVideoGridFragment = (SearchVideoGridFragment) supportFragmentManager.getFragment(savedInstanceState, SEARCH_FRAGMENT);
				channelBrowserFragment = (ChannelBrowserFragment) supportFragmentManager.getFragment(savedInstanceState, CHANNEL_BROWSER_FRAGMENT);
				playlistVideosFragment = (PlaylistVideosFragment) supportFragmentManager.getFragment(savedInstanceState, PLAYLIST_VIDEOS_FRAGMENT);
			}
			handleIntent(getIntent());

		}

		if (savedInstanceState != null) {
			// restore the video blocker plugin
			this.videoBlockerPlugin = (VideoBlockerPlugin) savedInstanceState.getSerializable(VIDEO_BLOCKER_PLUGIN);
			this.videoBlockerPlugin.setActivity(this);
		} else {
			this.videoBlockerPlugin = new VideoBlockerPlugin(this);
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		Logger.i(MainActivity.this, "onNewIntent "+ intent +" old "+getIntent());
		setIntent(intent);
		Logger.i(MainActivity.this, "---> "+getIntent());
		handleIntent(intent);
	}

	@Override
	protected void onResumeFragments() {
		super.onResumeFragments();
		Logger.i(MainActivity.this, "onResumeFragments "+getIntent());
	}

	private void handleIntent(Intent intent) {
		// If this Activity was called to view a particular channel, display that channel via ChannelBrowserFragment, instead of MainFragment
		String action = intent.getAction();
		Logger.i(MainActivity.this, "Action is : " + action);
		initMainFragment(action);
		if(ACTION_VIEW_CHANNEL.equals(action)) {
			dontAddToBackStack = true;
			YouTubeChannel channel = (YouTubeChannel) intent.getSerializableExtra(ChannelBrowserFragment.CHANNEL_OBJ);
			Logger.i(MainActivity.this, "Channel found: " + channel);
			onChannelClick(channel);
		} else if(ACTION_VIEW_PLAYLIST.equals(action)) {
			dontAddToBackStack = true;
			YouTubePlaylist playlist = (YouTubePlaylist) intent.getSerializableExtra(PlaylistVideosFragment.PLAYLIST_OBJ);
			Logger.i(MainActivity.this, "playlist found: " + playlist);
			onPlaylistClick(playlist);
		}
	}

	private void initMainFragment(String action) {
		if(mainFragment == null) {
			Logger.i(MainActivity.this,"initMainFragment called "+action);
			mainFragment = new MainFragment();
			// If we're coming here via a click on the Notification that new videos for subscribed channels have been found, make sure to
			// select the Feed tab.
			if(action != null && action.equals(ACTION_VIEW_FEED)) {
				Bundle args = new Bundle();
				args.putBoolean(MainFragment.SHOULD_SELECTED_FEED_TAB, true);
				mainFragment.setArguments(args);
			}
			getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, mainFragment).commit();
		} else {
			Logger.i(MainActivity.this, "mainFragment already exists, action:"+action+" fragment:"+mainFragment +", manager:"+mainFragment.getFragmentManager() +", support="+getSupportFragmentManager());
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		final FragmentManager supportFragmentManager = getSupportFragmentManager();
		if(mainFragment != null) {
			putFragment(supportFragmentManager, outState, MAIN_FRAGMENT, mainFragment);
		}
		if(searchVideoGridFragment != null && searchVideoGridFragment.isVisible()) {
			putFragment(supportFragmentManager, outState, SEARCH_FRAGMENT, searchVideoGridFragment);
		}
		if(channelBrowserFragment != null && channelBrowserFragment.isVisible()) {
			putFragment(supportFragmentManager, outState, CHANNEL_BROWSER_FRAGMENT, channelBrowserFragment);
		}
		if(playlistVideosFragment != null && playlistVideosFragment.isVisible()) {
			putFragment(supportFragmentManager, outState, PLAYLIST_VIDEOS_FRAGMENT, playlistVideosFragment);
		}

		// save the video blocker plugin
		outState.putSerializable(VIDEO_BLOCKER_PLUGIN, videoBlockerPlugin);
	}

	private void putFragment(FragmentManager fragmentManager,  Bundle bundle, @NonNull String key,
						@NonNull Fragment fragment) {
		if (fragment.getFragmentManager() != fragmentManager) {
			Logger.e(MainActivity.this, "Error fragment has a different FragmentManager than expected: Fragment=" + fragment + ", manager=" + fragmentManager + ", Fragment.manager=" + fragment.getFragmentManager() + " for key=" + key);
		} else {
			fragmentManager.putFragment(bundle, key, fragment);
		}
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
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.main_activity_menu, menu);

		// setup the video blocker notification icon which will be displayed in the tool bar
		videoBlockerPlugin.setupIconForToolBar(menu);

		onOptionsMenuCreated(menu);

		// setup the SearchView (actionbar)
		final MenuItem searchItem = menu.findItem(R.id.menu_search);
		final SearchView searchView = (SearchView) searchItem.getActionView();

		searchView.setQueryHint(getString(R.string.search_videos));

		AutoCompleteTextView autoCompleteTextView = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
		autoCompleteTextView.setThreshold(0);

		SearchHistoryCursorAdapter searchHistoryCursorAdapter = (SearchHistoryCursorAdapter) searchView.getSuggestionsAdapter();
		Cursor cursor = SearchHistoryDb.getSearchHistoryDb().getSearchCursor("");
		if (searchHistoryCursorAdapter == null) {
			searchHistoryCursorAdapter = new SearchHistoryCursorAdapter(getBaseContext(),
					R.layout.search_hint,
					cursor,
					new String[]{SearchHistoryTable.COL_SEARCH_TEXT},
					new int[]{android.R.id.text1},
					0);
			searchHistoryCursorAdapter.setSearchHistoryClickListener(query -> displaySearchResults(query, searchView));
			searchView.setSuggestionsAdapter(searchHistoryCursorAdapter);
		} else {
			// else just change the cursor...
			searchHistoryCursorAdapter.changeCursor(cursor);
		}
		searchView.setSuggestionsAdapter(searchHistoryCursorAdapter);

		// set the query hints to be equal to the previously searched text
		searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
			@Override
			public boolean onQueryTextChange(final String newText) {
				Logger.d(MainActivity.this, "on query text change: %s", newText);
				// if the user does not want to have the search string saved, then skip the below...
				if (newText == null || SkyTubeApp.getSettings().isDisableSearchHistory()) {
					return false;
				}

				Cursor cursor = SearchHistoryDb.getSearchHistoryDb().getSearchCursor(newText);

				// if the adapter has not been created, then create it
				SearchHistoryCursorAdapter searchHistoryCursorAdapter = new SearchHistoryCursorAdapter(getBaseContext(),
						R.layout.search_hint,
						cursor,
						new String[]{SearchHistoryTable.COL_SEARCH_TEXT},
						new int[]{android.R.id.text1},
						0);
				searchHistoryCursorAdapter.setSearchHistoryClickListener(query -> displaySearchResults(query, searchView));
				searchView.setSuggestionsAdapter(searchHistoryCursorAdapter);

				// update the current search string
				searchHistoryCursorAdapter.setSearchBarString(newText);

				return true;
			}

			@Override
			public boolean onQueryTextSubmit(String query) {
				if(!SkyTubeApp.getSettings().isDisableSearchHistory()) {
					// Save this search string into the Search History Database (for Suggestions)
					SearchHistoryDb.getSearchHistoryDb().insertSearchText(query);
				}

				displaySearchResults(query, searchView);

				return true;
			}
		});

		return true;
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_blocker:
				videoBlockerPlugin.onMenuBlockerIconClicked();
				return true;
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
			.setPositiveButton(R.string.play, (dialog, which) -> {
				// get the inputted URL string
				final String videoUrl = ((EditText)((AlertDialog) dialog).findViewById(R.id.dialog_url_edittext)).getText().toString();

				// play the video
				SkyTubeApp.openUrl(MainActivity.this, videoUrl, true);
			})
			.setNegativeButton(R.string.cancel, null)
			.show();

		// paste whatever there is in the clipboard (hopefully it is a video url)
		((EditText) alertDialog.findViewById(R.id.dialog_url_edittext)).setText(getClipboardItem());

		// clear URL edittext button
		alertDialog.findViewById(R.id.dialog_url_clear_button).setOnClickListener(v -> ((EditText) alertDialog.findViewById(R.id.dialog_url_edittext)).setText(""));
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

	/**
	 * For the extra variant, if the Chromecast Controller is visible and expanded, we want to collapse it. So, we must
	 * intercept onBackPressed to make sure it doesn't return us to the homescreen. shouldMinimizeOnBack will take care
	 * of this - on the Extra variant, if the Chromecast Controller is visible and expanded, it will collapse it, and
	 * return false, thus the app will not exit nor will it return to the homescreen. If it's collapsed, or not visible,
	 * it will return true, which then will check if the mainFragment is visible (as opposed to searchFragment). If it is,
	 * it will return to the home screen without exiting, otherwise it will do super.onBackPressed (so in searchFragment,
	 * it will exit from that and return to mainFragment).
	 *
	 * On the OSS variant, shouldMinimizeOnBack will always return true, and the normal checks for mainFragment being visible
	 * will be done.
	 */
	@Override
	public void onBackPressed() {
		if(shouldMinimizeOnBack()) {
			if (mainFragment != null && mainFragment.isVisible()) {
				// If the Subscriptions Drawer is open, close it instead of minimizing the app.
				if (mainFragment.isDrawerOpen()) {
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
	}

	private void switchToFragment(FragmentEx fragment) {
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
	 * Hide the virtual keyboard and then switch to the Search Video Grid Fragment with the selected
	 * query to search for videos.
	 *
	 * @param query Query text submitted by the user.
	 */
	private void displaySearchResults(String query, @NonNull final View searchView) {
		// hide the keyboard
		searchView.clearFocus();

		// open SearchVideoGridFragment and display the results
		searchVideoGridFragment = new SearchVideoGridFragment();
		Bundle bundle = new Bundle();
		bundle.putString(SearchVideoGridFragment.QUERY, query);
		searchVideoGridFragment.setArguments(bundle);
		switchToFragment(searchVideoGridFragment);
	}



	//////////////////////////////////////////////////////////////////////////////////////////


	/**
	 * A module/"plugin"/icon that displays the total number of blocked videos.
	 */
	private static class VideoBlockerPlugin implements VideoBlocker.VideoBlockerListener,
			BlockedVideosDialog.BlockedVideosDialogListener,
			Serializable {

		private ArrayList<VideoBlocker.BlockedVideo> blockedVideos = new ArrayList<>();
		private transient AppCompatActivity activity = null;


		VideoBlockerPlugin(AppCompatActivity activity) {
			// notify this class whenever a video is blocked...
			VideoBlocker.setVideoBlockerListener(this);
			this.activity = activity;
		}


		public void setActivity(AppCompatActivity activity) {
			this.activity = activity;
		}


		@Override
		public void onVideoBlocked(VideoBlocker.BlockedVideo blockedVideo) {
			blockedVideos.add(blockedVideo);
			activity.invalidateOptionsMenu();
		}


		/**
		 * Setup the video blocker notification icon which will be displayed in the tool bar.
 		 */
		void setupIconForToolBar(final Menu menu) {
			if (getTotalBlockedVideos() > 0) {
				// display a red bubble containing the number of blocked videos
				ActionItemBadge.update(activity,
						menu.findItem(R.id.menu_blocker),
						ContextCompat.getDrawable(activity, R.drawable.ic_video_blocker),
						ActionItemBadge.BadgeStyles.RED,
						getTotalBlockedVideos());
			} else {
				// Else, set the bubble to transparent.  This is required so that when the user
				// clicks on the icon, the app will be able to detect such click and displays the
				// BlockedVideosDialog (otherwise, the ActionItemBadge would just ignore such clicks.
				ActionItemBadge.update(activity,
						menu.findItem(R.id.menu_blocker),
						ContextCompat.getDrawable(activity, R.drawable.ic_video_blocker),
						new BadgeStyle(BadgeStyle.Style.DEFAULT, com.mikepenz.actionitembadge.library.R.layout.menu_action_item_badge, Color.TRANSPARENT, Color.TRANSPARENT, Color.WHITE),
						"");
			}
		}


		void onMenuBlockerIconClicked() {
			new BlockedVideosDialog(activity, this, blockedVideos).show();
		}


		@Override
		public void onClearBlockedVideos() {
			blockedVideos.clear();
			activity.invalidateOptionsMenu();
		}


		/**
		 * @return Total number of blocked videos.
		 */
		private int getTotalBlockedVideos() {
			return blockedVideos.size();
		}

	}

	/**
	 * This will tell the SubscriptionsFeedFragment (which lives in MainFragment) that it should refresh its video grid.
	 * This happens when a channel is subscribed to/unsubscribed from. This is called from {@link free.rm.skytube.gui.fragments.ChromecastMiniControllerFragment}.
	 */
	@Override
	public void refreshSubscriptionsFeedVideos() {
		SubscriptionsFeedFragment.unsetFlag(SubscriptionsFeedFragment.FLAG_REFRESH_FEED_FROM_CACHE);
		mainFragment.refreshSubscriptionsFeedVideos();
	}
}
