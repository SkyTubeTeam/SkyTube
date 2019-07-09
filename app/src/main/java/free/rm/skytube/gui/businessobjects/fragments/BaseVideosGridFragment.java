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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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

	@BindView(R.id.swipeRefreshLayout)
	protected SwipeRefreshLayout swipeRefreshLayout;


	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = inflater.inflate(getLayoutResource(), container, false);

		ButterKnife.bind(this, view);
		swipeRefreshLayout.setOnRefreshListener(this);

		return view;
	}


	@Override
	public void onRefresh() {
		videoGridAdapter.refresh(true);
	}

	/**
	 * If the PlaybackStatusDb has been updated (due to clearing or disabling it), refresh the entire VideoGrid.
	 */
	@Override
	public void onResume() {
		super.onResume();
		if(PlaybackStatusDb.isHasUpdated()) {
			videoGridAdapter.notifyDataSetChanged();
			PlaybackStatusDb.setHasUpdated(false);
		}
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
