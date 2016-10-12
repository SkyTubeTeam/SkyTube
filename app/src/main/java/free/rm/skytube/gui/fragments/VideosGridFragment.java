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

import free.rm.skytube.R;
import free.rm.skytube.businessobjects.MainActivityListener;
import free.rm.skytube.businessobjects.VideoCategory;
import free.rm.skytube.gui.businessobjects.FragmentEx;
import free.rm.skytube.gui.businessobjects.VideoGridAdapter;

/**
 * A fragment that will hold a {@link GridView} full of YouTube videos.
 */
@SuppressWarnings("deprecation")
public class VideosGridFragment extends FragmentEx {
	public static final String VIDEO_CATEGORY = "VideosGridFragment.VideoCategory";

	protected RecyclerView			gridView;
	protected VideoGridAdapter		videoGridAdapter;
	private View					progressBar = null;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// inflate the layout for this fragment
		View view = inflater.inflate(R.layout.videos_gridview, container, false);

		// set up the loading progress bar
		progressBar = view.findViewById(R.id.loading_progress_bar);

		// setup the video grid view
		gridView = (RecyclerView) view.findViewById(R.id.grid_view);
		if (videoGridAdapter == null) {
			videoGridAdapter = new VideoGridAdapter(getActivity());
		}
		videoGridAdapter.setProgressBar(progressBar);
		if(getArguments() != null)
			videoGridAdapter.setVideoCategory(VideoCategory.getVideoCategory(getArguments().getInt(VIDEO_CATEGORY, 0)));
		videoGridAdapter.setListener((MainActivityListener)getActivity());
		gridView.setLayoutManager(new GridLayoutManager(getActivity(), getResources().getInteger(R.integer.video_grid_num_columns)));
		gridView.setAdapter(videoGridAdapter);

		return view;
	}
}
