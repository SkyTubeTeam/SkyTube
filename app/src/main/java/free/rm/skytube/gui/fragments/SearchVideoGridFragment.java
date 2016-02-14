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

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import free.rm.skytube.R;
import free.rm.skytube.businessobjects.VideoCategory;
import free.rm.skytube.gui.businessobjects.FragmentEx;
import free.rm.skytube.gui.businessobjects.VideoGridAdapter;

/**
 * Fragment that will hold a list of videos corresponding to the user's query.
 */
public class SearchVideoGridFragment extends FragmentEx {

	protected GridView		gridView;
	protected VideoGridAdapter videoGridAdapter;


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// inflate the layout for this fragment
		View view = inflater.inflate(R.layout.fragment_videos_grid, container, false);

		this.gridView = (GridView) view.findViewById(R.id.grid_view);

		if (videoGridAdapter == null) {
			this.videoGridAdapter = new VideoGridAdapter(getActivity());

			String searchQuery = getSearchQuery();
			if (searchQuery != null) {
				// set the video category (if the user wants to search)... otherwise it will be set-
				// up by the VideoGridFragment
				this.videoGridAdapter.setVideoCategory(VideoCategory.SEARCH_QUERY, searchQuery);

				// set the action bar's title
				ActionBar actionBar = getActionBar();
				if (actionBar != null)
					actionBar.setTitle(searchQuery);
			}
		}

		this.gridView.setAdapter(this.videoGridAdapter);

		return view;
	}


	private String getSearchQuery() {
		String	searchQuery = null;
		Intent	intent = getActivity().getIntent();

		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			searchQuery = intent.getStringExtra(Intent.ACTION_SEARCH);
			Log.d("SEARCH", "Query=" + searchQuery);
		}

		return searchQuery;
	}

}
