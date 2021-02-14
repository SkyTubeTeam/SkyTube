package free.rm.skytube.gui.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import free.rm.skytube.R;
import free.rm.skytube.app.EventBus;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.app.utils.WeaklyReferencedMap;
import free.rm.skytube.businessobjects.Logger;
import free.rm.skytube.gui.activities.BaseActivity;
import free.rm.skytube.gui.businessobjects.MainActivityListener;
import free.rm.skytube.gui.businessobjects.adapters.SubsAdapter;
import free.rm.skytube.gui.businessobjects.fragments.FragmentEx;

public class MainFragment extends FragmentEx {

	private static final int TOP_LIST_INDEX = 0;

	private SubsAdapter					subsAdapter  = null;
	private ActionBarDrawerToggle		subsDrawerToggle;
	private TabLayout                   tabLayout = null;
	private DrawerLayout 				subsDrawerLayout = null;


	// Constants for saving the state of this Fragment's child Fragments
	public static final String FEATURED_VIDEOS_FRAGMENT = "MainFragment.featuredVideosFragment";
	public static final String MOST_POPULAR_VIDEOS_FRAGMENT = "MainFragment.mostPopularVideosFragment";
	public static final String SUBSCRIPTIONS_FEED_FRAGMENT = "MainFragment.subscriptionsFeedFragment";
	public static final String BOOKMARKS_FRAGMENT = "MainFragment.bookmarksFragment";
	public static final String DOWNLOADED_VIDEOS_FRAGMENT = "MainFragment.downloadedVideosFragment";

	private SimplePagerAdapter			videosPagerAdapter = null;

	public static final String SHOULD_SELECTED_FEED_TAB = "MainFragment.SHOULD_SELECTED_FEED_TAB";

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_main, container, false);

		// For the non-oss version, when using a Chromecast, returning to this fragment from another fragment that uses
		// CoordinatorLayout results in the SlidingUpPanel to be positioned improperly. We need to redraw the panel
		// to fix this. The oss version just has a no-op method.
		((BaseActivity) getActivity()).redrawPanel();

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

		RecyclerView subsListView = view.findViewById(R.id.subs_drawer);
		SearchView subSearchView = view.findViewById(R.id.subs_search_view);
		AutoCompleteTextView autoCompleteTextView = subSearchView.findViewById(androidx.appcompat.R.id.search_src_text);
		int fontColor = ContextCompat.getColor(getContext(), R.color.subs_text);
		autoCompleteTextView.setTextColor(fontColor);
		autoCompleteTextView.setHintTextColor(fontColor);
		final ImageView searchIcon = subSearchView.findViewById(androidx.appcompat.R.id.search_mag_icon);
		searchIcon.setColorFilter(fontColor);


		if (subsAdapter == null) {
			subsAdapter = SubsAdapter.get(getActivity(), view.findViewById(R.id.subs_drawer_progress_bar));
		} else {
			subsAdapter.setContext(getActivity());
		}
		subsAdapter.addListener((MainActivityListener) getActivity());

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

				Logger.i(this, "closed search");

				return false;
			}
		});

		videosPagerAdapter = new SimplePagerAdapter(getChildFragmentManager());

		ViewPager viewPager = view.findViewById(R.id.pager);
		final int tabCount = videosPagerAdapter.getCount();
		viewPager.setOffscreenPageLimit(tabCount > 3 ? tabCount - 1 : tabCount);
		viewPager.setAdapter(videosPagerAdapter);

		tabLayout = view.findViewById(R.id.tab_layout);
		tabLayout.setupWithViewPager(viewPager);

		tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
			@Override
			public void onTabSelected(TabLayout.Tab tab) {
				viewPager.setCurrentItem(tab.getPosition());

				videosPagerAdapter.notifyTab(tab, true);
			}

			@Override
			public void onTabUnselected(TabLayout.Tab tab) {
				videosPagerAdapter.notifyTab(tab, false);
			}

			@Override
			public void onTabReselected(TabLayout.Tab tab) {
				//When current tab reselected scroll to the top of the video list
				VideosGridFragment fragment = videosPagerAdapter.getFragmentFrom(tab, true);
				if (fragment != null && fragment.gridView != null) {
					fragment.gridView.smoothScrollToPosition(TOP_LIST_INDEX);
				}
			}
		});

		// select the default tab:  the default tab is defined by the user through the Preferences
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());

		// If the app is being opened via the Notification that new videos from Subscribed channels have been found, select the Subscriptions Feed Fragment
		Bundle args = getArguments();
		if (args != null && args.getBoolean(SHOULD_SELECTED_FEED_TAB, false)) {
			viewPager.setCurrentItem(videosPagerAdapter.getIndexOf(SUBSCRIPTIONS_FEED_FRAGMENT));
		} else {
			String defaultTab = sp.getString(getString(R.string.pref_key_default_tab_name), null);
			String[] tabListValues = getTabListValues();

			if (defaultTab == null) {
				int defaultTabNum = Integer.parseInt(sp.getString(getString(R.string.pref_key_default_tab), "0"));
				defaultTab = tabListValues[defaultTabNum];
				sp.edit().putString(getString(R.string.pref_key_default_tab_name), defaultTab).apply();
			}

			// Create a list of non-hidden fragments in order to default to the proper tab
			Set<String> hiddenFragments = SkyTubeApp.getSettings().getHiddenTabs();
			List<String> shownFragmentList = new ArrayList<>();
			for (final String tabListValue : tabListValues) {
				if (!hiddenFragments.contains(tabListValue))
					shownFragmentList.add(tabListValue);
			}
			viewPager.setCurrentItem(shownFragmentList.indexOf(defaultTab));
		}

		// Set the current viewpager fragment as selected, as when the Activity is recreated, the Fragment
		// won't know that it's selected. When the Feeds fragment is the default tab, this will prevent the
		// refresh dialog from showing when an automatic refresh happens.
		videosPagerAdapter.selectTabAtPosition(viewPager.getCurrentItem());

		EventBus.getInstance().registerMainFragment(this);

		return view;
	}

	private static String[] getTabListValues() {
		return SkyTubeApp.getStringArray(R.array.tab_list_values);
	}

	@Override
	public void onDestroyView() {
		subsAdapter.removeListener((MainActivityListener) getActivity());
		subsDrawerLayout = null;
		videosPagerAdapter = null;
		subsDrawerToggle = null;
		tabLayout = null;
		super.onDestroyView();
	}

	@Override
	public void onDestroy() {
		// this should happen after onSaveInstanceState
		super.onDestroy();
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
		if (videosPagerAdapter != null && tabLayout != null) {
			Logger.d(this, "MAINFRAGMENT RESUMED " + tabLayout.getSelectedTabPosition());
			videosPagerAdapter.selectTabAtPosition(tabLayout.getSelectedTabPosition());
		}
		FragmentActivity activity = getActivity();
		subsAdapter.setContext(activity);
	}

	@Override
	public void onPause() {
		super.onPause();
		subsAdapter.setContext(null);
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

	public void refreshTabs(EventBus.SettingChange settingChange) {
		Logger.i(this, "refreshTabs called");
		if (settingChange == EventBus.SettingChange.HIDE_TABS) {
			videosPagerAdapter.updateVisibleTabs(tabLayout);
		} else {
			videosPagerAdapter.notifyDataSetChanged();
		}
	}

	public static class SimplePagerAdapter extends FragmentPagerAdapter {
		private final WeaklyReferencedMap<String, VideosGridFragment> instantiatedFragments = new WeaklyReferencedMap<>();
		private final List<String> visibleTabs = new ArrayList<>();

		public SimplePagerAdapter(FragmentManager fm) {
			// TODO: Investigate, if we need this
			super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
			setupVisibleTabs();
		}

		synchronized void setupVisibleTabs() {
			visibleTabs.clear();
			Set<String> hiddenTabs = SkyTubeApp.getSettings().getHiddenTabs();
			for (String key : getTabListValues()) {
				if (hiddenTabs.contains(key)) {
					instantiatedFragments.remove(key);
				} else {
					visibleTabs.add(key);
				}
			}
		}

		synchronized void updateVisibleTabs(TabLayout pager) {
			final int currentItem = pager.getSelectedTabPosition();
			String oldSelection = getTabKey(currentItem);
			setupVisibleTabs();
			notifyDataSetChanged();
			if (oldSelection != null && !isVisible(oldSelection)) {
				int newSelection = Math.min(visibleTabs.size() - 1, currentItem);
				if (newSelection>=0) {
					TabLayout.Tab tab = pager.getTabAt(newSelection);
					pager.selectTab(tab);
				}
			}
		}

		@Override
		public int getItemPosition(@NonNull Object object) {
			VideosGridFragment fragment = (VideosGridFragment) object;
			String key = fragment.getBundleKey();
			int currPos = visibleTabs.indexOf(key);
			return currPos < 0 ? POSITION_NONE : currPos;
		}

		@Override
		public synchronized Fragment getItem(int position) {
			String key = getTabKey(position);
			if (key != null) {
				return createOrGetFromCache(key, true);
			}
			return null;
		}

		private VideosGridFragment createOrGetFromCache(String key, boolean create) {
			VideosGridFragment fragment = instantiatedFragments.get(key);
			if (fragment == null && create){
				fragment = create(key);
				instantiatedFragments.put(key, fragment);
			}
			return fragment;
		}

		private VideosGridFragment create(String key) {
			// add fragments to list:  do NOT forget to ***UPDATE*** @string/tab_list and @string/tab_list_values
			if (MOST_POPULAR_VIDEOS_FRAGMENT.equals(key)) {
				return new MostPopularVideosFragment();
			}
			if (FEATURED_VIDEOS_FRAGMENT.equals(key)) {
				return new FeaturedVideosFragment();
			}
			if (SUBSCRIPTIONS_FEED_FRAGMENT.equals(key)) {
				return new SubscriptionsFeedFragment();
			}
			if (BOOKMARKS_FRAGMENT.equals(key)) {
				return new BookmarksFragment();
			}
			if (DOWNLOADED_VIDEOS_FRAGMENT.equals(key)) {
				return new DownloadedVideosFragment();
			}
			return null;
		}

		@Override
		public synchronized int getCount() {
			return visibleTabs.size();
		}

		@Override
		public synchronized void destroyItem(final ViewGroup container, final int position, final Object object) {
			super.destroyItem(container, position, object);
			instantiatedFragments.remove(getTabKey(position));
		}

		private String getTabKey(int position) {
			if (0 <= position && position < visibleTabs.size()) {
				return visibleTabs.get(position);
			}
			return null;
		}

		private int getIndexOf(String fragmentName) {
			return visibleTabs.indexOf(fragmentName);
		}

		private synchronized VideosGridFragment getFragmentFrom(int position, boolean createIfNotFound) {
			String key = getTabKey(position);
			return createOrGetFromCache(key, createIfNotFound);
		}

		private synchronized VideosGridFragment getFragmentFrom(TabLayout.Tab tab, boolean createIfNotFound) {
			return getFragmentFrom(tab.getPosition(), createIfNotFound);
		}

		public synchronized void notifyTab(TabLayout.Tab tab, boolean onSelect) {
			VideosGridFragment fragment = getFragmentFrom(tab, onSelect);
			if (fragment != null) {
				if (onSelect) {
					fragment.onFragmentSelected();
				} else {
					fragment.onFragmentUnselected();
				}
			}
		}

		public synchronized void selectTabAtPosition(int position) {
			VideosGridFragment fragment = getFragmentFrom(position, true);
			if (fragment != null) {
				fragment.onFragmentSelected();
			}
		}

		@Override
		public synchronized CharSequence getPageTitle(int position) {
			String tabKey = getTabKey(position);
			if (tabKey != null) {
				return SkyTubeApp.getFragmentNames().getName(tabKey);
			}
			return "Unknown";
		}

		private boolean isVisible(String key) {
			return getIndexOf(key) >= 0;
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
//		videosPagerAdapter.onSaveInstanceState(outState);

		super.onSaveInstanceState(outState);
	}

	/**
	 * Returns true if the subscriptions drawer is opened.
	 */
	public boolean isDrawerOpen() {
		if (subsDrawerLayout != null) {
			return subsDrawerLayout.isDrawerOpen(GravityCompat.START);
		} else {
			Logger.e(this, "subsDrawerLayout is null for isDrawerOpen");
			Thread.dumpStack();
			return false;
		}
	}


	/**
	 * Close the subscriptions drawer.
	 */
	public void closeDrawer() {
		if (subsDrawerLayout != null) {
			subsDrawerLayout.closeDrawer(GravityCompat.START);
		} else {
			Logger.e(this, "subsDrawerLayout is null for closeDrawer");
			Thread.dumpStack();
		}
	}

	/**
	 * Refresh the SubscriptionsFeedFragment's feed.
	 */
	public void refreshSubscriptionsFeedVideos() {
		SubscriptionsFeedFragment subscriptionsFeedFragment = (SubscriptionsFeedFragment) videosPagerAdapter.createOrGetFromCache(SUBSCRIPTIONS_FEED_FRAGMENT, false);
		if (subscriptionsFeedFragment != null) {
			subscriptionsFeedFragment.refreshFeedFromCache();
		}
	}
}