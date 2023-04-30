package free.rm.skytube.gui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.VideoCategory;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.gui.businessobjects.adapters.VideoGridAdapter;

/**
 * A fragment that displays the videos belonging to a channel.
 */
public class ChannelVideosFragment extends VideosGridFragment {
	/** YouTube Channel */
	private YouTubeChannel channel;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		// get the channel
		channel = (YouTubeChannel) requireArguments().getSerializable(ChannelBrowserFragment.CHANNEL_OBJ);

		// create and return the view
		View view =  super.onCreateView(inflater, container, savedInstanceState);

		if (channel != null) {
			videoGridAdapter.setYouTubeChannel(channel);
		}

		return view;
	}

	public void setYouTubeChannel(YouTubeChannel youTubeChannel) {
		channel = youTubeChannel;
		if (videoGridAdapter != null) {
			videoGridAdapter.setYouTubeChannel(youTubeChannel);
		}
	}

	VideoGridAdapter getVideoGridAdapter() {
		return videoGridAdapter;
	}

	@Override
	protected VideoCategory getVideoCategory() {
		return VideoCategory.CHANNEL_VIDEOS;
	}

	@Override
	protected String getSearchString() {
		return channel.getId();
	}

	@Override
	public String getFragmentName() {
		return SkyTubeApp.getStr(R.string.videos);
	}

	@Override
	public int getPriority() {
		return 0;
	}
}
