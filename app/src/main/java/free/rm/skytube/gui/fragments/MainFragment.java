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

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
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
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.Logger;
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
				VideosGridFragment fragment = videosPagerAdapter.getTab(tab);
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

		return view;
	}

	private static String[] getTabListValues() {
		return SkyTubeApp.getStringArray(R.array.tab_list_values);
	}

	@Override
	public void onDestroyView() {
		subsAdapter.removeListener((MainActivityListener) getActivity());
		subsListView.setAdapter(null); // cleanup the reference from the SubsAdapter back to the view
		subsDrawerLayout = null;
		subsDrawerToggle = null;
		tabLayout = null;
		super.onDestroyView();
	}

	@Override
	public void onDestroy() {
		// this should happen after onSaveInstanceState
		videosPagerAdapter = null;
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

	public class SimplePagerAdapter extends FragmentPagerAdapter {
		private final SparseArray<WeakReference<Fragment>> instantiatedFragments = new SparseArray<>();
		private final List<String> visibleTabs = new ArrayList<>();

		public SimplePagerAdapter(FragmentManager fm) {
			// TODO: Investigate, if we need this
			super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
			Set<String> hiddenTabs = SkyTubeApp.getSettings().getHiddenTabs();
			for (String key : getTabListValues()) {
				if (!hiddenTabs.contains(key)) {
					visibleTabs.add(key);
				}
			}
		}

		@Override
		public Fragment getItem(int position) {
			if (0<=position && position<visibleTabs.size()) {
				String key = visibleTabs.get(position);
				return create(key);
			}
			return null;
		}

		VideosGridFragment create(String key) {
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
		public int getCount() {
			return visibleTabs.size();
		}

		@Override
		public Object instantiateItem(final ViewGroup container, final int position) {
			final Fragment fragment = (Fragment) super.instantiateItem(container, position);
			instantiatedFragments.put(position, new WeakReference<>(fragment));
			return fragment;
		}

		@Override
		public void destroyItem(final ViewGroup container, final int position, final Object object) {
			instantiatedFragments.remove(position);
			super.destroyItem(container, position, object);
		}

		@Nullable
		public VideosGridFragment getFragment(final int position) {
			if (0 <= position && position <= instantiatedFragments.size()) {
				final WeakReference<Fragment> wr = instantiatedFragments.get(position);
				if (wr != null) {
					return (VideosGridFragment) wr.get();
				}
			}
			return null;
		}

		public int getIndexOf(String fragmentName) {
			return visibleTabs.indexOf(fragmentName);
		}

		public VideosGridFragment getTabOf(String fragmentName) {
			return getFragment(getIndexOf(fragmentName));
		}

		public VideosGridFragment getTab(TabLayout.Tab tab) {
			return getFragment(tab.getPosition());
		}

		public void notifyTab(TabLayout.Tab tab, boolean onSelect) {
			VideosGridFragment fragment = getTab(tab);
			if (fragment != null) {
				if (onSelect) {
					fragment.onFragmentSelected();
				} else {
					fragment.onFragmentUnselected();
				}
			}
		}

		public void selectTabAtPosition(int position) {
			VideosGridFragment fragment = getFragment(position);
			if (fragment != null) {
				fragment.onFragmentSelected();
			}
		}

		@Override
		public CharSequence getPageTitle(int position) {
			String title = null;
			if (0<=position && position<visibleTabs.size()) {
				String tabKey = visibleTabs.get(position);
				title = SkyTubeApp.getFragmentNames().getName(tabKey);
			}
			return title != null ? title : "Unknown";
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
		SubscriptionsFeedFragment subscriptionsFeedFragment = (SubscriptionsFeedFragment) videosPagerAdapter.getTabOf(SUBSCRIPTIONS_FEED_FRAGMENT);
		if (subscriptionsFeedFragment != null) {
			subscriptionsFeedFragment.refreshFeedFromCache();
		}
	}
}