package free.rm.skytube.gui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.VideoCategory;
import free.rm.skytube.businessobjects.YouTube.LegacyGetChannelPlaylists;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubePlaylist;
import free.rm.skytube.businessobjects.YouTube.YouTubeTasks;
import free.rm.skytube.businessobjects.YouTube.newpipe.GetPlaylistsForChannel;
import free.rm.skytube.businessobjects.YouTube.newpipe.NewPipeService;
import free.rm.skytube.gui.businessobjects.MainActivityListener;
import free.rm.skytube.gui.businessobjects.PlaylistClickListener;
import free.rm.skytube.gui.businessobjects.adapters.PlaylistsGridAdapter;

/**
 * A fragment that displays the Playlists belonging to a Channel
 */
public class ChannelPlaylistsFragment extends VideosGridFragment implements PlaylistClickListener, SwipeRefreshLayout.OnRefreshListener {
	private PlaylistsGridAdapter    playlistsGridAdapter;
	private MainActivityListener    mainActivityListener;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);


		if (playlistsGridAdapter == null) {
			playlistsGridAdapter = new PlaylistsGridAdapter(getActivity(), this);
		} else {
			playlistsGridAdapter.setContext(getActivity());
		}

		YouTubeChannel channel = (YouTubeChannel) requireArguments()
				.getSerializable(ChannelBrowserFragment.CHANNEL_OBJ);
		playlistsGridAdapter.setFetcher(createFetcher(channel));

		gridviewBinding.swipeRefreshLayout.setOnRefreshListener(this);

		gridviewBinding.gridView.setAdapter(playlistsGridAdapter);

		return view;
	}

	private YouTubeTasks.ChannelPlaylistFetcher createFetcher(YouTubeChannel channel) {
		if (NewPipeService.isPreferred()) {
			return new GetPlaylistsForChannel(channel);
		} else {
			return new LegacyGetChannelPlaylists(channel);
		}
	}

	@Override
	public void onDestroy() {
		playlistsGridAdapter.clearBackgroundTasks();
		super.onDestroy();
	}

	@Override
	public String getFragmentName() {
		return SkyTubeApp.getStr(R.string.playlists);
	}

	@Override
	public void onClickPlaylist(YouTubePlaylist playlist) {
		if(mainActivityListener != null)
			mainActivityListener.onPlaylistClick(playlist);
	}

	public void setMainActivityListener(MainActivityListener mainActivityListener) {
		this.mainActivityListener = mainActivityListener;
	}

	@Override
	public void onRefresh() {
		playlistsGridAdapter.refresh(youTubePlaylists -> gridviewBinding.swipeRefreshLayout.setRefreshing(false));
	}

	@Override
	protected VideoCategory getVideoCategory() {
		return null;
	}

	@Override
	public int getPriority() {
		return 6;
	}
}
