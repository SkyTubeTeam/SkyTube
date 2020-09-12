/*
 * SkyTube
 * Copyright (C) 2017  Ramon Mifsud
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

package free.rm.skytube.gui.businessobjects.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import butterknife.BindView;
import butterknife.ButterKnife;
import free.rm.skytube.R;
import free.rm.skytube.businessobjects.db.PlaybackStatusDb;
import free.rm.skytube.gui.businessobjects.adapters.VideoGridAdapter;
import free.rm.skytube.gui.fragments.VideosGridFragment;

/**
 * A class that supports swipe-to-refresh on {@link VideosGridFragment}.
 */
public abstract class BaseVideosGridFragment extends TabFragment implements SwipeRefreshLayout.OnRefreshListener {

	protected VideoGridAdapter  videoGridAdapter;
	private int updateCount = 0;

	@BindView(R.id.swipeRefreshLayout)
	protected SwipeRefreshLayout swipeRefreshLayout;

	public BaseVideosGridFragment() {
	}

	protected void setVideoGridAdapter(VideoGridAdapter adapter) {
		this.videoGridAdapter = adapter;
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		if (videoGridAdapter == null) {
			videoGridAdapter = new VideoGridAdapter();
		}

		View view = inflater.inflate(getLayoutResource(), container, false);

		ButterKnife.bind(this, view);
		swipeRefreshLayout.setOnRefreshListener(this);

		if (isFragmentSelected()) {
			videoGridAdapter.initializeList();
		}
		return view;
	}


	@Override
	public void onRefresh() {
		videoGridAdapter.refresh(true);
		updateCount = PlaybackStatusDb.getPlaybackStatusDb().getUpdateCounter();
	}

	/**
	 * If the PlaybackStatusDb has been updated (due to clearing or disabling it), refresh the entire VideoGrid.
	 */
	@Override
	public void onResume() {
		super.onResume();
		int newUpdateCounter = PlaybackStatusDb.getPlaybackStatusDb().getUpdateCounter();
		if(newUpdateCounter != updateCount) {
			videoGridAdapter.notifyDataSetChanged();
			updateCount = newUpdateCounter;
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		videoGridAdapter.onDestroy();
		videoGridAdapter = null;
	}

	@Override
	public void onFragmentSelected() {
		super.onFragmentSelected();
		if (videoGridAdapter != null) {
			videoGridAdapter.initializeList();
		}
	}

	/**
	 * Set the layout resource (e.g. Subscriptions resource layout, R.id.grid_view, ...etc).
	 */
	protected  abstract int getLayoutResource();

}
