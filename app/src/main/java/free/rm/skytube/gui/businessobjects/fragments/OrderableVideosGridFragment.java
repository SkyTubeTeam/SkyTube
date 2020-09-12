package free.rm.skytube.gui.businessobjects.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.ItemTouchHelper;

import free.rm.skytube.gui.businessobjects.SimpleItemTouchHelperCallback;
import free.rm.skytube.gui.businessobjects.adapters.OrderableVideoGridAdapter;
import free.rm.skytube.gui.fragments.VideosGridFragment;

/**
 * A VideosGridFragment that supports reordering of the videos in the Grid.
 */
public abstract class OrderableVideosGridFragment extends VideosGridFragment {
	public OrderableVideosGridFragment() {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);
		if(videoGridAdapter instanceof OrderableVideoGridAdapter) {

			ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback((OrderableVideoGridAdapter)videoGridAdapter);
			ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
			touchHelper.attachToRecyclerView(gridView);
		}
		return view;
	}
}
