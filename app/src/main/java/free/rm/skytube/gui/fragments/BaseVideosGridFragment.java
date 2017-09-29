package free.rm.skytube.gui.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.View;

import butterknife.BindView;
import butterknife.ButterKnife;
import free.rm.skytube.R;
import free.rm.skytube.gui.businessobjects.FragmentEx;
import free.rm.skytube.gui.businessobjects.VideoGridAdapter;

/**
 * A class that supports swipe-to-refresh on {@link VideosGridFragment}.
 */
public abstract class BaseVideosGridFragment extends FragmentEx implements SwipeRefreshLayout.OnRefreshListener {

	protected VideoGridAdapter videoGridAdapter;

	@BindView(R.id.swipeRefreshLayout)
	SwipeRefreshLayout swipeRefreshLayout;

	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		ButterKnife.bind(this, view);
		swipeRefreshLayout.setOnRefreshListener(this);
	}

	@Override
	public void onRefresh() {
		videoGridAdapter.refresh(new Runnable() {
			@Override
			public void run() {
				swipeRefreshLayout.setRefreshing(false);
			}
		});
	}

}
