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

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.SearchView;

import free.rm.skytube.R;
import free.rm.skytube.businessobjects.VideoCategory;
import free.rm.skytube.databinding.FragmentSearchBinding;
import free.rm.skytube.databinding.SearchActionbarBinding;
import free.rm.skytube.gui.businessobjects.adapters.VideoGridAdapter;

/**
 * Fragment that will hold a list of videos corresponding to the user's query.
 */
public class SearchVideoGridFragment extends VideosGridFragment {
	public static final String QUERY = "SearchVideoGridFragment.Query";

	/** User's search query string. */
	private String searchQuery = "";
	/** Edit searched query through long press on search query**/
	private SearchView editSearchView;
    private SearchActionbarBinding searchActionbar;
    private MenuItem   searchMenuItem;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// set the user's search query
		searchQuery = requireArguments().getString(QUERY);
	}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // inflate the layout for this fragment
        searchActionbar = SearchActionbarBinding.inflate(inflater);

        FragmentSearchBinding binding = FragmentSearchBinding.inflate(inflater, container, false);
        initSearch(container.getContext(), videoGridAdapter, binding);

        return binding.getRoot();
    }

    protected void initSearch(@NonNull Context context, VideoGridAdapter videoGridAdapterParam, FragmentSearchBinding binding) {
        initVideos(context, videoGridAdapterParam, binding.videosGridview);
        // setup the toolbar/actionbar
        setSupportActionBar(binding.toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setCustomView(searchActionbar.getRoot());
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);

        searchActionbar.actionbarTitle.setText(searchQuery);
        // make searched query editable on touching the textView
        searchActionbar.actionbarTitle.setOnClickListener(click -> {
            showSearchArea(true);
        });

        //makes searched query editable on long press
        binding.toolbar.setOnLongClickListener(view1 -> {
            showSearchArea(true);
            return false;
        });
        // the app will call onCreateOptionsMenu() for when the user wants to search
        setHasOptionsMenu(true);

        // Enforce loading of the list
        videoGridAdapter.initializeList();
    }

    private void showSearchArea(boolean visible) {
        getSupportActionBar().setDisplayShowCustomEnabled(!visible);
        if (visible) {
            searchMenuItem.expandActionView();
        }
    }

    private void updateSearchView(boolean visible) {
        if (visible) {
            editSearchView.onActionViewExpanded();
            editSearchView.setQuery(searchQuery, false);
        } else {
            editSearchView.setIconified(true);
        }
    }

	@Override
	public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
		searchMenuItem = menu.findItem(R.id.menu_search);
		SearchView searchView = (SearchView) searchMenuItem.getActionView();
		editSearchView = searchView;
		// will be called when the user clicks on the actionbar's search icon
		searchMenuItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
			@Override
			public boolean onMenuItemActionExpand(MenuItem item) {
				// if the user has previously search, then copy the query into the search view
                if (searchQuery != null  &&  !searchQuery.isEmpty()) {
                    updateSearchView(true);
                }
				// now expand the search view
				return true;
			}

			@Override
			public boolean onMenuItemActionCollapse(MenuItem item) {
                showSearchArea(false);
                return true;
			}
		});
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

	@Override
	public int getPriority() {
		return 0;
	}
}
