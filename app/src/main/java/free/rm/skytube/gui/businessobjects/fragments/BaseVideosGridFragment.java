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

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import javax.annotation.Nonnull;

import free.rm.skytube.businessobjects.Logger;
import free.rm.skytube.businessobjects.db.PlaybackStatusDb;
import free.rm.skytube.gui.businessobjects.adapters.VideoGridAdapter;
import free.rm.skytube.gui.fragments.VideosGridFragment;

/**
 * A class that supports swipe-to-refresh on {@link VideosGridFragment}.
 */
public abstract class BaseVideosGridFragment extends TabFragment implements SwipeRefreshLayout.OnRefreshListener {
	protected VideoGridAdapter videoGridAdapter;
	private int updateCount = 0;

	protected SwipeRefreshLayout swipeRefreshLayout;

	public BaseVideosGridFragment() {
	}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Logger.w(this, "onCreateView called - call init(...) from the descendant- with videoGridAdapter=%s swipeRefreshLayout=%s", videoGridAdapter, swipeRefreshLayout);
        initBase(container.getContext(), videoGridAdapter, swipeRefreshLayout);
        return null;
    }

    protected void initBase(@NonNull Context context, VideoGridAdapter videoGridAdapterParam, @Nonnull SwipeRefreshLayout swipeRefreshLayoutParam) {
        if (videoGridAdapterParam != null) {
            this.videoGridAdapter = videoGridAdapterParam;
        } else {
            this.videoGridAdapter = new VideoGridAdapter();
        }
        videoGridAdapter.setContext(context);
        this.swipeRefreshLayout = swipeRefreshLayoutParam;
        if (isFragmentSelected()) {
            videoGridAdapter.initializeList();
        }
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
		if (videoGridAdapter != null) {
			videoGridAdapter.initializeList();
		}

		int newUpdateCounter = PlaybackStatusDb.getPlaybackStatusDb().getUpdateCounter();
		if(newUpdateCounter != updateCount) {
			videoGridAdapter.notifyDataSetChanged();
			updateCount = newUpdateCounter;
		}
	}

	@Override
	public void onDestroyView() {
		videoGridAdapter.onDestroy();
		videoGridAdapter = null;
		swipeRefreshLayout = null;
		super.onDestroyView();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void onFragmentSelected() {
		super.onFragmentSelected();
		if (videoGridAdapter != null) {
			videoGridAdapter.initializeList();
		}
	}
}
