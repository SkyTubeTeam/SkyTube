/*
 * SkyTube
 * Copyright (C) 2018  Ramon Mifsud
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

package free.rm.skytube.gui.businessobjects.adapters;

import android.content.Context;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.io.IOException;

import free.rm.skytube.R;
import free.rm.skytube.businessobjects.VideoCategory;
import free.rm.skytube.businessobjects.YouTube.GetYouTubeVideos;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;
import free.rm.skytube.businessobjects.YouTube.Tasks.GetYouTubeVideosTask;
import free.rm.skytube.gui.businessobjects.MainActivityListener;

/**
 * An adapter that will display videos in a {@link android.widget.GridView}.
 */
public class VideoGridAdapter extends RecyclerViewAdapterEx<YouTubeVideo, GridViewHolder> {

	/** Class used to get YouTube videos from the web. */
	private GetYouTubeVideos	getYouTubeVideos;
	/** Set to true to display channel information (e.g. channel name) and allows user to open and
	 *  browse the channel;  false to hide such information. */
	private boolean				showChannelInfo = true;
	/** Current video category */
	private VideoCategory		currentVideoCategory = null;

	// This allows the grid items to pass messages back to MainActivity
	protected MainActivityListener listener;

	/** If this is set, new videos being displayed will be saved to the database, if subscribed.
	 *  RM:  This is only set and used by ChannelBrowserFragment */
	private YouTubeChannel			youTubeChannel;

	/** Holds a progress bar */
	private SwipeRefreshLayout      swipeRefreshLayout = null;

	private GridViewHolder activeGridViewHolder;

	private static final String TAG = VideoGridAdapter.class.getSimpleName();


	/**
	 * @see #VideoGridAdapter(Context, boolean)
	 */
	public VideoGridAdapter(Context context) {
		this(context, true);
	}

	public void setListener(MainActivityListener listener) {
		this.listener = listener;
	}

	/**
	 * Constructor.
	 *
	 * @param context			Context.
	 * @param showChannelInfo	True to display channel information (e.g. channel name) and allows
	 *                          user to open and browse the channel; false to hide such information.
	 */
	public VideoGridAdapter(Context context, boolean showChannelInfo) {
		super(context);
		this.getYouTubeVideos = null;
		this.showChannelInfo = showChannelInfo;
	}


	/**
	 * Set the video category.  Upon set, the adapter will download the videos of the specified
	 * category asynchronously.
	 *
	 * @see #setVideoCategory(VideoCategory, String)
	 */
	public void setVideoCategory(VideoCategory videoCategory) {
		setVideoCategory(videoCategory, null);
	}


	/**
	 * Set the video category.  Upon set, the adapter will download the videos of the specified
	 * category asynchronously.
	 *
	 * @param videoCategory	The video category you want to change to.
	 * @param searchQuery	The search query.  Should only be set if videoCategory is equal to
	 *                      SEARCH_QUERY.
	 */
	public void setVideoCategory(VideoCategory videoCategory, String searchQuery) {
		// do not change the video category if its the same!
		if (videoCategory == currentVideoCategory)
			return;

		try {
			Log.i(TAG, videoCategory.toString());

			// clear all previous items in this adapter
			this.clearList();

			// create a new instance of GetYouTubeVideos
			this.getYouTubeVideos = videoCategory.createGetYouTubeVideos();
			this.getYouTubeVideos.init();

			// set the query
			if (searchQuery != null) {
				getYouTubeVideos.setQuery(searchQuery);
			}

			// set current video category
			this.currentVideoCategory = videoCategory;

			// get the videos from the web asynchronously
			new GetYouTubeVideosTask(getYouTubeVideos, this, swipeRefreshLayout).executeInParallel();
		} catch (IOException e) {
			Log.e(TAG, "Could not init " + videoCategory, e);
			Toast.makeText(getContext(),
					String.format(getContext().getString(R.string.could_not_get_videos), videoCategory.toString()),
					Toast.LENGTH_LONG).show();
			this.currentVideoCategory = null;
		}
	}



	@Override
	public GridViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.video_cell, parent, false);
		final GridViewHolder gridViewHolder = new GridViewHolder(v, listener, showChannelInfo);
		gridViewHolder.setGridViewHolderListener(new GridViewHolder.GridViewHolderListener() {
			@Override
			public void onClick() {
				activeGridViewHolder = gridViewHolder;
			}
		});
		return gridViewHolder;
	}

	public void refreshActiveGridViewHolder() {
		if(activeGridViewHolder != null)
			activeGridViewHolder.updateViewsData(getContext());
	}

	/**
	 * Refresh the video grid, by running the task to get the videos again.
	 */
	public void refresh() {
		refresh(false);
	}


	/**
	 * Refresh the video grid, by running the task to get the videos again.
	 *
	 * @param clearVideosList If set to true, it will clear out any previously loaded videos (found
	 *                        in this adapter).
	 */
	public void refresh(boolean clearVideosList) {
		if (getYouTubeVideos != null) {
			if (clearVideosList) {
				getYouTubeVideos.reset();
				clearList();
			}

			new GetYouTubeVideosTask(getYouTubeVideos, this, swipeRefreshLayout).executeInParallel();
		}
	}


	@Override
	public void onBindViewHolder(GridViewHolder viewHolder, int position) {
		if (viewHolder != null) {
			viewHolder.updateInfo(get(position), getContext(), listener);
		}

		// if it reached the bottom of the list, then try to get the next page of videos
		if (position >= getItemCount() - 1) {
			Log.w(TAG, "BOTTOM REACHED!!!");
			if(getYouTubeVideos != null)
				new GetYouTubeVideosTask(getYouTubeVideos, this, swipeRefreshLayout).executeInParallel();
		}

	}


	public void setSwipeRefreshLayout(SwipeRefreshLayout swipeRefreshLayout) {
		this.swipeRefreshLayout = swipeRefreshLayout;
	}

	public void setYouTubeChannel(YouTubeChannel youTubeChannel) {
		this.youTubeChannel = youTubeChannel;
	}

	public YouTubeChannel getYouTubeChannel() {
		return youTubeChannel;
	}

}
