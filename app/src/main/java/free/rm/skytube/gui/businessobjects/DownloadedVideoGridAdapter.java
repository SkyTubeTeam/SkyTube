package free.rm.skytube.gui.businessobjects;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Collections;

import free.rm.skytube.R;
import free.rm.skytube.businessobjects.db.DownloadedVideosDb;

/**
 * Subclass of VideoGridAdapter for Downloaded Videos that supports drag & drop reordering of the items in the grid.
 */
public class DownloadedVideoGridAdapter extends VideoGridAdapter implements ItemTouchHelperAdapter {
	private DownloadedVideosDb.DownloadedVideosListener downloadedVideoListener;

	public DownloadedVideoGridAdapter(Context context, DownloadedVideosDb.DownloadedVideosListener downloadedVideoListener) {
		super(context);
		this.downloadedVideoListener = downloadedVideoListener;
	}

	@Override
	public GridViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.video_cell, parent, false);
		return new GridViewHolder(v, listener, downloadedVideoListener);
	}

	@Override
	public boolean onItemMove(int fromPosition, int toPosition) {
		if (fromPosition < toPosition) {
			for (int i = fromPosition; i < toPosition; i++) {
				Collections.swap(list, i, i + 1);
			}
		} else {
			for (int i = fromPosition; i > toPosition; i--) {
				Collections.swap(list, i, i - 1);
			}
		}
		notifyItemMoved(fromPosition, toPosition);

		// Update the database since the order has changed
		DownloadedVideosDb.getVideoDownloadsDb().updateOrder(list);

		return true;
	}

}
