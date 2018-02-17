/*
 * SkyTube
 * Copyright (C) 2016  Ramon Mifsud
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
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import free.rm.skytube.R;
import free.rm.skytube.businessobjects.VideoCategory;

/**
 * Fragment that will hold a list of videos corresponding to the user's query.
 */
public class SearchVideoGridFragment extends VideosGridFragment {

	/** User's search query string. */
	private String  searchQuery = "";

	public static final String QUERY = "SearchVideoGridFragment.Query";


	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// set the user's search query
		searchQuery = getArguments().getString(QUERY);
	}


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// inflate the layout for this fragment
		View view = super.onCreateView(inflater, container, savedInstanceState);

		// setup the toolbar/actionbar
		Toolbar toolbar = view.findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		// set the action bar's title
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null)
			actionBar.setTitle(searchQuery);

		// the app will call onCreateOptionsMenu() for when the user wants to search
		setHasOptionsMenu(true);

		return view;
	}


	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		final MenuItem   searchMenuItem = menu.findItem(R.id.menu_search);
		final SearchView searchView = (SearchView) searchMenuItem.getActionView();

		// will be called when the user clicks on the actionbar's search icon
		searchMenuItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
			@Override
			public boolean onMenuItemActionExpand(MenuItem item) {
				// if the user has previously search, then copy the query into the search view
				if (searchQuery != null  &&  !searchQuery.isEmpty()) {
					searchView.onActionViewExpanded();
					searchView.setQuery(searchQuery, false);
				}

				// now expand the search view
				return true;
			}

			@Override
			public boolean onMenuItemActionCollapse(MenuItem item) {
				return true;
			}
		});
	}


	@Override
	protected int getLayoutResource() {
		return R.layout.fragment_search;
	}

	@Override
	protected VideoCategory getVideoCategory() {
		return VideoCategory.SEARCH_QUERY;
	}


	@Override
	protected String getSearchString() {
		return searchQuery;
	}


	@Override
	public String getFragmentName() {
		return null;
	}

}
