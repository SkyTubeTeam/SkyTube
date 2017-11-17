package free.rm.skytube.gui.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.VideoCategory;
import free.rm.skytube.businessobjects.YouTubeChannel;
import free.rm.skytube.gui.businessobjects.LoadingProgressBar;
import free.rm.skytube.gui.businessobjects.MainActivityListener;
import free.rm.skytube.gui.businessobjects.VideoGridAdapter;

/**
 * A fragment that displays the Videos belonging to a Channel
 */
public class ChannelVideosFragment extends VideosGridFragment {
	private RecyclerView gridView;

	private YouTubeChannel channel;

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.videos_gridview, container, false);

		gridView = view.findViewById(R.id.grid_view);

		// set up the loading progress bar
		LoadingProgressBar.get().setProgressBar(view.findViewById(R.id.loading_progress_bar));

		channel = (YouTubeChannel)getArguments().getSerializable(ChannelBrowserFragment.CHANNEL_OBJ);

		if (videoGridAdapter == null) {
			videoGridAdapter = new VideoGridAdapter(getActivity(), false /*hide channel name*/);
			videoGridAdapter.setVideoCategory(VideoCategory.CHANNEL_VIDEOS, channel.getId());
		}
		videoGridAdapter.setListener((MainActivityListener)getActivity());
		if(channel != null)
			videoGridAdapter.setYouTubeChannel(channel);

		this.gridView.setHasFixedSize(false);
		this.gridView.setLayoutManager(new GridLayoutManager(getActivity(), getResources().getInteger(R.integer.video_grid_num_columns)));
		this.gridView.setAdapter(this.videoGridAdapter);


		return view;
	}

	public void setYouTubeChannel(YouTubeChannel youTubeChannel) {
		channel = youTubeChannel;
		videoGridAdapter.setYouTubeChannel(youTubeChannel);
	}

	public VideoGridAdapter getVideoGridAdapter() {
		return videoGridAdapter;
	}

	@Override
	protected VideoCategory getVideoCategory() {
		return VideoCategory.CHANNEL_VIDEOS;
	}

	@Override
	public String getFragmentName() {
		return SkyTubeApp.getStr(R.string.videos);
	}

	@Override
	public void onFragmentSelected() {

	}
}
