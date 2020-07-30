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

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.io.IOException;
import java.util.Objects;

import free.rm.skytube.R;
import free.rm.skytube.businessobjects.Logger;
import free.rm.skytube.businessobjects.VideoCategory;
import free.rm.skytube.businessobjects.YouTube.GetYouTubeVideos;
import free.rm.skytube.businessobjects.YouTube.POJOs.CardData;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.YouTube.YouTubeTasks;
import free.rm.skytube.businessobjects.YouTube.newpipe.ContentId;
import free.rm.skytube.businessobjects.db.PlaybackStatusDb;
import free.rm.skytube.businessobjects.interfaces.CardListener;
import free.rm.skytube.businessobjects.interfaces.VideoPlayStatusUpdateListener;
import free.rm.skytube.databinding.VideoCellBinding;
import free.rm.skytube.gui.businessobjects.MainActivityListener;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

/**
 * An adapter that will display videos in a {@link android.widget.GridView}.
 */
public class VideoGridAdapter extends RecyclerViewAdapterEx<CardData, GridViewHolder> implements VideoPlayStatusUpdateListener, CardListener {
	private static final String TAG = VideoGridAdapter.class.getSimpleName();

	public interface Callback {
		void onVideoGridUpdated(boolean hasItems);
	}

	/**
	 * Class used to get YouTube videos from the web.
	 */
	private GetYouTubeVideos getYouTubeVideos;
	/**
	 * Set to true to display channel information (e.g. channel name) and allows user to open and
	 * browse the channel;  false to hide such information.
	 */
	private boolean showChannelInfo = true;
	/**
	 * Current video category
	 */
	private VideoCategory currentVideoCategory = null;

	// This allows the grid items to pass messages back to MainActivity
	protected MainActivityListener listener;

	/**
	 * If this is set, new videos being displayed will be saved to the database, if subscribed.
	 * RM:  This is only set and used by ChannelBrowserFragment
	 */
	private YouTubeChannel youTubeChannel;

	/**
	 * Holds a progress bar
	 */
	private SwipeRefreshLayout swipeRefreshLayout = null;

	/** Set to true if the video adapter is initialized. */
	private boolean initialized = false;
    private boolean refreshHappens = false;

	private VideoGridAdapter.Callback videoGridUpdated;

	private final CompositeDisposable compositeDisposable = new CompositeDisposable();

	/**
	 * Constructor.
	 */
	public VideoGridAdapter() {
		super();
		this.getYouTubeVideos = null;
		PlaybackStatusDb.getPlaybackStatusDb().addListener(this);
	}

	public void onDestroy() {
		compositeDisposable.clear();
		PlaybackStatusDb.getPlaybackStatusDb().removeListener(this);
		this.listener = null;
		this.videoGridUpdated = null;
	}


	public void setListener(MainActivityListener listener) {
		this.listener = listener;
	}

    /**
     * Will be called once the DB is updated - by a video insertion.
     */
    @Override
    public void onCardAdded(final CardData card) {
        prepend(card);
    }

    /**
     * Will be called once the DB is updated - by a video deletion.
     */
    @Override
    public void onCardDeleted(final ContentId contentId) {
        remove(card -> contentId.getId().equals(card.getId()));
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
	 * @param videoCategory The video category you want to change to.
	 * @param searchQuery   The search query.  Should only be set if videoCategory is equal to
	 *                      SEARCH_QUERY.
	 */
	public void setVideoCategory(VideoCategory videoCategory, String searchQuery) {
		// do not change the video category if its the same!
		if (videoCategory == currentVideoCategory)
			return;

		try {
			Logger.d(this, "setVideoCategory:" + videoCategory.toString());

			// do not show channel name if the video category == CHANNEL_VIDEOS or PLAYLIST_VIDEOS
			this.showChannelInfo = !(videoCategory == VideoCategory.CHANNEL_VIDEOS  ||  videoCategory == VideoCategory.PLAYLIST_VIDEOS);

			// create a new instance of GetYouTubeVideos
			this.getYouTubeVideos = videoCategory.createGetYouTubeVideos();
			this.getYouTubeVideos.init();

			// set the query
			if (searchQuery != null) {
				getYouTubeVideos.setQuery(searchQuery);
			}

			// set current video category
			this.currentVideoCategory = videoCategory;

		} catch (IOException e) {
			Logger.e(this, "Could not init " + videoCategory, e);
			Toast.makeText(getContext(),
							String.format(getContext().getString(R.string.could_not_get_videos), videoCategory.toString()),
							Toast.LENGTH_LONG).show();
			this.currentVideoCategory = null;
		}
	}


	@Override
	public GridViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		setContext(parent.getContext());
		final VideoCellBinding binding = VideoCellBinding.inflate(LayoutInflater.from(getContext()),
				parent, false);
		return new GridViewHolder(binding, listener, showChannelInfo);
	}

	/**
	 * Initialize the video list, if it's not yet initialized.
	 */
	public void initializeList() {
		if (!initialized && getYouTubeVideos != null) {
			initialized = true;
			refresh(true);
		}
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
	public synchronized void refresh(boolean clearVideosList) {
		if (getYouTubeVideos != null && !refreshHappens) {
			refreshHappens = true;
			if (clearVideosList) {
				getYouTubeVideos.reset();
			}
			// now, we consider this as initialized - sometimes 'refresh' can be called before the initializeList is called.
			initialized = true;

			compositeDisposable.add(YouTubeTasks.getYouTubeVideos(getYouTubeVideos, this,
					swipeRefreshLayout, clearVideosList).subscribe());
		}
	}

	@Override
	public void onBindViewHolder(@NonNull GridViewHolder viewHolder, int position) {
		viewHolder.updateInfo(get(position), getContext(), listener);

		// if it reached the bottom of the list, then try to get the next page of videos
		if (position >= getItemCount() - 1) {
			Logger.d(this, "BOTTOM REACHED!!!");
			if(getYouTubeVideos != null) {
				refreshHappens = true;
				compositeDisposable.add(YouTubeTasks.getYouTubeVideos(getYouTubeVideos, this,
						swipeRefreshLayout, false).subscribe());
			}
		}
	}

    public synchronized void notifyVideoGridUpdated() {
        refreshHappens = false;
        if (videoGridUpdated != null) {
            int itemCount = getItemCount();
            videoGridUpdated.onVideoGridUpdated(itemCount > 0);
        }
    }

	@Override
	public void onViewRecycled(@NonNull GridViewHolder holder) {
		holder.clearBackgroundTasks();
	}

	public void setSwipeRefreshLayout(SwipeRefreshLayout swipeRefreshLayout) {
		this.swipeRefreshLayout = swipeRefreshLayout;
	}

	public void setYouTubeChannel(YouTubeChannel youTubeChannel) {
		this.youTubeChannel = youTubeChannel;
	}

	public void setVideoGridUpdated(Callback videoGridUpdated) {
		this.videoGridUpdated = videoGridUpdated;
	}

	public YouTubeChannel getYouTubeChannel() {
		return youTubeChannel;
	}

	public VideoCategory getCurrentVideoCategory() {
		return currentVideoCategory;
	}

    @Override
    public void onVideoStatusUpdated(CardData video) {
        if (video != null) {
            replace(item -> Objects.equals(item.getId(), video.getId()), video);
        } else {
            notifyDataSetChanged();
        }
    }
}

