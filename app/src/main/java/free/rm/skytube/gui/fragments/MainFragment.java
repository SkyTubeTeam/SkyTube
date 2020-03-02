package free.rm.skytube.gui.fragments;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.Logger;
import free.rm.skytube.businessobjects.db.BookmarksDb;
import free.rm.skytube.businessobjects.db.DownloadedVideosDb;
import free.rm.skytube.gui.activities.BaseActivity;
import free.rm.skytube.gui.businessobjects.MainActivityListener;
import free.rm.skytube.gui.businessobjects.adapters.SubsAdapter;
import free.rm.skytube.gui.businessobjects.fragments.FragmentEx;

public class MainFragment extends FragmentEx {

	private static final int TOP_LIST_INDEX = 0;

	private RecyclerView				subsListView = null;
	private SubsAdapter					subsAdapter  = null;
	private ActionBarDrawerToggle		subsDrawerToggle;
	private TabLayout                   tabLayout = null;
	private DrawerLayout 				subsDrawerLayout = null;
	private SearchView 					subSearchView = null;

	/** List of fragments that will be displayed as tabs. */
	private List<VideosGridFragment>	videoGridFragmentsList = new ArrayList<>();
	private FeaturedVideosFragment		featuredVideosFragment = null;
	private MostPopularVideosFragment	mostPopularVideosFragment = null;
	private SubscriptionsFeedFragment   subscriptionsFeedFragment = null;
	private BookmarksFragment			bookmarksFragment = null;
	private DownloadedVideosFragment    downloadedVideosFragment = null;

	// Constants for saving the state of this Fragment's child Fragments
	public static final String FEATURED_VIDEOS_FRAGMENT = "MainFragment.featuredVideosFragment";
	public static final String MOST_POPULAR_VIDEOS_FRAGMENT = "MainFragment.mostPopularVideosFragment";
	public static final String SUBSCRIPTIONS_FEED_FRAGMENT = "MainFragment.subscriptionsFeedFragment";
	public static final String BOOKMARKS_FRAGMENT = "MainFragment.bookmarksFragment";
	public static final String DOWNLOADED_VIDEOS_FRAGMENT = "MainFragment.downloadedVideosFragment";

	private VideosPagerAdapter			videosPagerAdapter = null;
	private ViewPager					viewPager;

	public static final String SHOULD_SELECTED_FEED_TAB = "MainFragment.SHOULD_SELECTED_FEED_TAB";

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if(savedInstanceState != null) {
			featuredVideosFragment = (FeaturedVideosFragment) getChildFragmentManager().getFragment(savedInstanceState, FEATURED_VIDEOS_FRAGMENT);
			mostPopularVideosFragment = (MostPopularVideosFragment) getChildFragmentManager().getFragment(savedInstanceState, MOST_POPULAR_VIDEOS_FRAGMENT);
			subscriptionsFeedFragment = (SubscriptionsFeedFragment)getChildFragmentManager().getFragment(savedInstanceState, SUBSCRIPTIONS_FEED_FRAGMENT);
			bookmarksFragment = (BookmarksFragment) getChildFragmentManager().getFragment(savedInstanceState, BOOKMARKS_FRAGMENT);
			downloadedVideosFragment = (DownloadedVideosFragment) getChildFragmentManager().getFragment(savedInstanceState, DOWNLOADED_VIDEOS_FRAGMENT);
		}
	}


	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_main, container, false);

		// For the non-oss version, when using a Chromecast, returning to this fragment from another fragment that uses
		// CoordinatorLayout results in the SlidingUpPanel to be positioned improperly. We need to redraw the panel
		// to fix this. The oss version just has a no-op method.
		((BaseActivity)getActivity()).redrawPanel();

		// setup the toolbar / actionbar
		Toolbar toolbar = view.findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(false);

		// indicate that this fragment has an action bar menu
		setHasOptionsMenu(true);

		subsDrawerLayout = view.findViewById(R.id.subs_drawer_layout);
		subsDrawerToggle = new ActionBarDrawerToggle(
						getActivity(),
						subsDrawerLayout,
						R.string.app_name,
						R.string.app_name
		);
		subsDrawerToggle.setDrawerIndicatorEnabled(true);
		final ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
			actionBar.setHomeButtonEnabled(true);
		}

		subsListView = view.findViewById(R.id.subs_drawer);
		subSearchView = view.findViewById(R.id.subs_search_view);
		AutoCompleteTextView autoCompleteTextView = subSearchView.findViewById(androidx.appcompat.R.id.search_src_text);
		int fontColor = getResources().getColor(R.color.subs_text);
		autoCompleteTextView.setTextColor(fontColor);
		autoCompleteTextView.setHintTextColor(fontColor);
		final ImageView searchIcon = subSearchView.findViewById(androidx.appcompat.R.id.search_mag_icon);
		searchIcon.setColorFilter(fontColor);


		if (subsAdapter == null) {
			subsAdapter = SubsAdapter.get(getActivity(), view.findViewById(R.id.subs_drawer_progress_bar));
		} else {
			subsAdapter.setContext(getActivity());
		}
		subsAdapter.setListener((MainActivityListener)getActivity());

		subsListView.setLayoutManager(new LinearLayoutManager(getActivity()));
		subsListView.setAdapter(subsAdapter);

		subSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
			@Override
			public boolean onQueryTextSubmit(String s) {
				return true;
			}

			@Override
			public boolean onQueryTextChange(String s) {
				subsAdapter.filterSubSearch(s);
				return true;
			}

		});

		subSearchView.setOnCloseListener(new SearchView.OnCloseListener() {
			@Override
			public boolean onClose() {

				Logger.i(this,"closed search");

				return false;
			}
		});

		videosPagerAdapter = new VideosPagerAdapter(getChildFragmentManager());
		viewPager = view.findViewById(R.id.pager);
		viewPager.setOffscreenPageLimit(videoGridFragmentsList.size() > 3 ? videoGridFragmentsList.size() - 1 : videoGridFragmentsList.size());
		viewPager.setAdapter(videosPagerAdapter);

		tabLayout = view.findViewById(R.id.tab_layout);
		tabLayout.setupWithViewPager(viewPager);

		tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
			@Override
			public void onTabSelected(TabLayout.Tab tab) {
				viewPager.setCurrentItem(tab.getPosition());
				videoGridFragmentsList.get(tab.getPosition()).onFragmentSelected();
			}

			@Override
			public void onTabUnselected(TabLayout.Tab tab) {
				videoGridFragmentsList.get(tab.getPosition()).onFragmentUnselected();
			}

			@Override
			public void onTabReselected(TabLayout.Tab tab) {
				//When current tab reselected scroll to the top of the video list
				VideosGridFragment fragment = videoGridFragmentsList.get(tab.getPosition());
				if(fragment != null && fragment.gridView != null) {
					fragment.gridView.smoothScrollToPosition(TOP_LIST_INDEX);
				}
			}
		});

		// select the default tab:  the default tab is defined by the user through the Preferences
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());

		// If the app is being opened via the Notification that new videos from Subscribed channels have been found, select the Subscriptions Feed Fragment
		Bundle args = getArguments();
		if(args != null && args.getBoolean(SHOULD_SELECTED_FEED_TAB, false)) {
			viewPager.setCurrentItem(videoGridFragmentsList.indexOf(subscriptionsFeedFragment));
		} else {
			String defaultTab = sp.getString(getString(R.string.pref_key_default_tab_name), null);
			String[] tabListValues = SkyTubeApp.getStringArray(R.array.tab_list_values);

			if(defaultTab == null) {
				int defaultTabNum = Integer.parseInt(sp.getString(getString(R.string.pref_key_default_tab), "0"));
				defaultTab = tabListValues[defaultTabNum];
				sp.edit().putString(getString(R.string.pref_key_default_tab_name), tabListValues[defaultTabNum]).apply();
			}

			// Create a list of non-hidden fragments in order to default to the proper tab
			Set<String> hiddenFragments = SkyTubeApp.getPreferenceManager().getStringSet(getString(R.string.pref_key_hide_tabs), new HashSet<>());
			List<String> shownFragmentList = new ArrayList<>();
			for(int i=0;i<tabListValues.length;i++) {
				if(!hiddenFragments.contains(tabListValues[i]))
					shownFragmentList.add(tabListValues[i]);
			}
			viewPager.setCurrentItem(shownFragmentList.indexOf(defaultTab));
		}

		// Set the current viewpager fragment as selected, as when the Activity is recreated, the Fragment
		// won't know that it's selected. When the Feeds fragment is the default tab, this will prevent the
		// refresh dialog from showing when an automatic refresh happens.
		videoGridFragmentsList.get(viewPager.getCurrentItem()).onFragmentSelected();

		return view;
	}


	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		subsDrawerToggle.syncState();
	}


	@Override
	public void onResume() {
		super.onResume();

		// when the MainFragment is resumed (e.g. after Preferences is minimized), inform the
		// current fragment that it is selected.
		if (videoGridFragmentsList != null  &&  tabLayout != null) {
			Logger.d(this, "MAINFRAGMENT RESUMED " + tabLayout.getSelectedTabPosition());
			videoGridFragmentsList.get(tabLayout.getSelectedTabPosition()).onFragmentSelected();
		}
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


	private class VideosPagerAdapter extends FragmentPagerAdapter {

		public VideosPagerAdapter(FragmentManager fm) {
			super(fm);

			// initialise fragments
			if (featuredVideosFragment == null)
				featuredVideosFragment = new FeaturedVideosFragment();

			if (mostPopularVideosFragment == null)
				mostPopularVideosFragment = new MostPopularVideosFragment();

			if (subscriptionsFeedFragment == null)
				subscriptionsFeedFragment = new SubscriptionsFeedFragment();

			if (bookmarksFragment == null) {
				bookmarksFragment = new BookmarksFragment();
				BookmarksDb.getBookmarksDb().addListener(bookmarksFragment);
			}

			if(downloadedVideosFragment == null) {
				downloadedVideosFragment = new DownloadedVideosFragment();
				DownloadedVideosDb.getVideoDownloadsDb().setListener(downloadedVideosFragment);
			}

			Set<String> hiddenFragments = SkyTubeApp.getPreferenceManager().getStringSet(getString(R.string.pref_key_hide_tabs), new HashSet<>());

			// add fragments to list:  do NOT forget to ***UPDATE*** @string/tab_list and @string/tab_list_values
			videoGridFragmentsList.clear();
			if(!hiddenFragments.contains(FEATURED_VIDEOS_FRAGMENT))
				videoGridFragmentsList.add(featuredVideosFragment);
			if(!hiddenFragments.contains(MOST_POPULAR_VIDEOS_FRAGMENT))
				videoGridFragmentsList.add(mostPopularVideosFragment);
			if(!hiddenFragments.contains(SUBSCRIPTIONS_FEED_FRAGMENT))
				videoGridFragmentsList.add(subscriptionsFeedFragment);
			if(!hiddenFragments.contains(BOOKMARKS_FRAGMENT))
				videoGridFragmentsList.add(bookmarksFragment);
			if(!hiddenFragments.contains(DOWNLOADED_VIDEOS_FRAGMENT))
				videoGridFragmentsList.add(downloadedVideosFragment);


		}

		@Override
		public int getCount() {
			return videoGridFragmentsList.size();
		}

		@Override
		public Fragment getItem(int position) {
			return videoGridFragmentsList.get(position);
		}

		@Override
		public CharSequence getPageTitle(int position) {
			return videoGridFragmentsList.get(position).getFragmentName();
		}

	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		if(featuredVideosFragment != null && featuredVideosFragment.isAdded())
			getChildFragmentManager().putFragment(outState, FEATURED_VIDEOS_FRAGMENT, featuredVideosFragment);
		if(mostPopularVideosFragment != null && mostPopularVideosFragment.isAdded())
			getChildFragmentManager().putFragment(outState, MOST_POPULAR_VIDEOS_FRAGMENT, mostPopularVideosFragment);
		if(subscriptionsFeedFragment != null && subscriptionsFeedFragment.isAdded())
			getChildFragmentManager().putFragment(outState, SUBSCRIPTIONS_FEED_FRAGMENT, subscriptionsFeedFragment);
		if(bookmarksFragment != null && bookmarksFragment.isAdded())
			getChildFragmentManager().putFragment(outState, BOOKMARKS_FRAGMENT, bookmarksFragment);
		if(downloadedVideosFragment != null && downloadedVideosFragment.isAdded())
			getChildFragmentManager().putFragment(outState, DOWNLOADED_VIDEOS_FRAGMENT, downloadedVideosFragment);

		super.onSaveInstanceState(outState);
	}


	/**
	 * Returns true if the subscriptions drawer is opened.
	 */
	public boolean isDrawerOpen() {
		return subsDrawerLayout.isDrawerOpen(GravityCompat.START);
	}

	
	/**
	 * Close the subscriptions drawer.
	 */
	public void closeDrawer() {
		subsDrawerLayout.closeDrawer(GravityCompat.START);
	}

	/*
	 * Returns the SubscriptionsFeedFragment
	 * @return {@link free.rm.skytube.gui.fragments.SubscriptionsFeedFragment}
	 */
	public SubscriptionsFeedFragment getSubscriptionsFeedFragment() {
		return subscriptionsFeedFragment;
	}
}
