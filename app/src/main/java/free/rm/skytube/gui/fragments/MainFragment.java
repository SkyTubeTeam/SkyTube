package free.rm.skytube.gui.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import free.rm.skytube.R;
import free.rm.skytube.businessobjects.MainActivityListener;
import free.rm.skytube.gui.businessobjects.FragmentEx;
import free.rm.skytube.gui.businessobjects.SubsAdapter;


public class MainFragment extends FragmentEx {
	private RecyclerView			subsListView = null;
	private SubsAdapter				subsAdapter = null;
	private ActionBarDrawerToggle	subsDrawerToggle;
	private VideosGridFragment featuredVideosFragment;
	private VideosGridFragment mostPopularVideosFragment;

	private VideosPagerAdapter videosPagerAdapter;
	private ViewPager viewPager;

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_main, container, false);

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
		final ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
			actionBar.setHomeButtonEnabled(true);
		}

		subsListView = (RecyclerView) view.findViewById(R.id.subs_drawer);
		if (subsAdapter == null) {
			subsAdapter = SubsAdapter.get(getActivity(), view.findViewById(R.id.subs_drawer_progress_bar));
		}
		subsAdapter.setListener((MainActivityListener)getActivity());

		subsListView.setLayoutManager(new LinearLayoutManager(getActivity()));
		subsListView.setAdapter(subsAdapter);


		viewPager = (ViewPager)view.findViewById(R.id.pager);
		videosPagerAdapter = new VideosPagerAdapter(getActivity(), getChildFragmentManager());
		viewPager.setAdapter(videosPagerAdapter);

		TabLayout tabLayout = (TabLayout)view.findViewById(R.id.tab_layout);
		tabLayout.setupWithViewPager(viewPager);

		tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
			@Override
			public void onTabSelected(TabLayout.Tab tab) {
				viewPager.setCurrentItem(tab.getPosition());
			}

			@Override
			public void onTabUnselected(TabLayout.Tab tab) {

			}

			@Override
			public void onTabReselected(TabLayout.Tab tab) {

			}
		});



		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		subsDrawerToggle.syncState();
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



	public class VideosPagerAdapter extends FragmentPagerAdapter {
		Context context = null;

		public VideosPagerAdapter(Context context, FragmentManager fm) {
			super(fm);
			this.context = context;
		}

		@Override
		public int getCount() {
			return 2;
		}

		@Override
		public Fragment getItem(int position) {
			switch (position) {
				case 0:
					featuredVideosFragment = new VideosGridFragment();
					Bundle args = new Bundle();
					args.putInt(VideosGridFragment.VIDEO_CATEGORY, 0);
					featuredVideosFragment.setArguments(args);
					return featuredVideosFragment;
				case 1:
					mostPopularVideosFragment = new VideosGridFragment();
					Bundle args1 = new Bundle();
					args1.putInt(VideosGridFragment.VIDEO_CATEGORY, 1);
					mostPopularVideosFragment.setArguments(args1);
					return mostPopularVideosFragment;
			}
			return null;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			switch(position) {
				case 0:
					return getString(R.string.featured);
				case 1:
					return getString(R.string.popular);
				default:
					return null;
			}
		}
	}
}
