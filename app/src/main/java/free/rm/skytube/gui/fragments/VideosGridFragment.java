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

import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.SpinnerAdapter;

import free.rm.skytube.R;
import free.rm.skytube.businessobjects.VideoCategory;
import free.rm.skytube.gui.businessobjects.FragmentEx;
import free.rm.skytube.gui.businessobjects.LoadingProgressBar;
import free.rm.skytube.gui.businessobjects.SubsAdapter;
import free.rm.skytube.gui.businessobjects.VideoGridAdapter;

/**
 * A fragment that will hold a {@link GridView} full of YouTube videos.
 */
@SuppressWarnings("deprecation")
public class VideosGridFragment extends FragmentEx implements ActionBar.OnNavigationListener {

	protected GridView				gridView;
	protected VideoGridAdapter		videoGridAdapter;
	private ListView				subsListView = null;
	private SubsAdapter				subsAdapter = null;
	private ActionBarDrawerToggle	subsDrawerToggle;


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// inflate the layout for this fragment
		View view = inflater.inflate(R.layout.fragment_videos_grid, container, false);

		// set up the loading progress bar
		LoadingProgressBar.get().setProgressBar(view.findViewById(R.id.loading_progress_bar));

		// setup the video grid view
		this.gridView = (GridView) view.findViewById(R.id.grid_view);
		if (this.videoGridAdapter == null) {
			this.videoGridAdapter = new VideoGridAdapter(getActivity());
		}
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

		this.subsListView = (ListView) view.findViewById(R.id.subs_drawer);

		if (subsAdapter == null) {
			this.subsAdapter = SubsAdapter.get(getActivity(), view.findViewById(R.id.subs_drawer_progress_bar));
		}

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
		}

		subsDrawerToggle.syncState();
	}


	@Override
	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
		// scroll to the top
		gridView.setSelection(0);

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
}
