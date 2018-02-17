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
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import com.bumptech.glide.Glide;

import free.rm.skytube.R;
import free.rm.skytube.businessobjects.VideoCategory;
import free.rm.skytube.gui.businessobjects.MainActivityListener;
import free.rm.skytube.gui.businessobjects.adapters.VideoGridAdapter;
import free.rm.skytube.gui.businessobjects.fragments.BaseVideosGridFragment;

/**
 * A fragment that will hold a {@link GridView} full of YouTube videos.
 */
public abstract class VideosGridFragment extends BaseVideosGridFragment {

	protected RecyclerView	gridView;


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// inflate the layout for this fragment
		View view = super.onCreateView(inflater, container, savedInstanceState);

		// setup the video grid view
		gridView = view.findViewById(R.id.grid_view);
		if (videoGridAdapter == null) {
			videoGridAdapter = new VideoGridAdapter(getActivity());
		} else {
			videoGridAdapter.setContext(getActivity());
		}
		videoGridAdapter.setSwipeRefreshLayout(swipeRefreshLayout);

		if (getVideoCategory() != null)
			videoGridAdapter.setVideoCategory(getVideoCategory(), getSearchString());

		videoGridAdapter.setListener((MainActivityListener)getActivity());

		gridView.setHasFixedSize(true);
		gridView.setLayoutManager(new GridLayoutManager(getActivity(), getResources().getInteger(R.integer.video_grid_num_columns)));
		gridView.setAdapter(videoGridAdapter);

		return view;
	}


	@Override
	public void onDestroy() {
		super.onDestroy();
		Glide.get(getActivity()).clearMemory();
	}


	@Override
	protected int getLayoutResource() {
		return R.layout.videos_gridview;
	}


	/**
	 * @return Returns the category of videos being displayed by this fragment.
	 */
	protected abstract VideoCategory getVideoCategory();


	/**
	 * @return Returns the search string used when setting the video category.  (Can be used to
	 * set the channel ID in case of VideoCategory.CHANNEL_VIDEOS).
	 */
	protected String getSearchString() {
		return null;
	}

	/**
	 * @return The fragment/tab name/title.
	 */
	public abstract String getFragmentName();

}
