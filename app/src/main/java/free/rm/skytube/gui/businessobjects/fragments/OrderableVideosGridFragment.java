package free.rm.skytube.gui.businessobjects.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;

import free.rm.skytube.databinding.VideosGridviewBinding;
import free.rm.skytube.gui.businessobjects.SimpleItemTouchHelperCallback;
import free.rm.skytube.gui.businessobjects.adapters.OrderableVideoGridAdapter;
import free.rm.skytube.gui.businessobjects.adapters.VideoGridAdapter;
import free.rm.skytube.gui.fragments.VideosGridFragment;

/**
 * A VideosGridFragment that supports reordering of the videos in the Grid.
 */
public abstract class OrderableVideosGridFragment extends VideosGridFragment {
    public OrderableVideosGridFragment() {
    }

    protected void initOrderableVideos(@NonNull Context context, @NonNull OrderableVideoGridAdapter videoGridAdapterParam, @NonNull VideosGridviewBinding gridviewBindingParam) {
        initVideos(context, videoGridAdapterParam, gridviewBindingParam);
        swipeRefreshLayout.setEnabled(false);

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(videoGridAdapterParam);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(gridviewBinding.gridView);
    }
}
