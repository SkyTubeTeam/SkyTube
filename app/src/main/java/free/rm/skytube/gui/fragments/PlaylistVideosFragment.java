package free.rm.skytube.gui.fragments;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import butterknife.BindView;
import butterknife.ButterKnife;
import free.rm.skytube.R;
import free.rm.skytube.businessobjects.VideoCategory;
import free.rm.skytube.businessobjects.YouTubePlaylist;
import free.rm.skytube.gui.businessobjects.LoadingProgressBar;
import free.rm.skytube.gui.businessobjects.VideoGridAdapter;

/**
 * A Fragment that displays the Videos of a Playlist in a {@link VideosGridFragment}
 */
public class PlaylistVideosFragment extends VideosGridFragment {
	private YouTubePlaylist youTubePlaylist;

	public static final String PLAYLIST_OBJ = "PlaylistVideosFragment.PLAYLIST_OBJ";

	@BindView(R.id.toolbar)
	Toolbar toolbar;
	@BindView(R.id.playlist_banner_image_view)
	ImageView playlistBannerImageView;
	@BindView(R.id.playlist_title_text_view)
	TextView playlistTitleTextView;

	@Override
	public String getFragmentName() {
		return null;
	}

	@Override
	protected VideoCategory getVideoCategory() {
		return VideoCategory.PLAYLIST_VIDEOS;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_playlist_videos, container, false);

		ButterKnife.bind(this, view);

		gridView = view.findViewById(R.id.grid_view);

		// set up the loading progress bar
		LoadingProgressBar.get().setProgressBar(view.findViewById(R.id.loading_progress_bar));

		youTubePlaylist = (YouTubePlaylist)getArguments().getSerializable(PLAYLIST_OBJ);

		playlistTitleTextView.setText(youTubePlaylist.getTitle());

		// setup the toolbar/actionbar
		setSupportActionBar(toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null && youTubePlaylist.getChannel() != null) {
			actionBar.setTitle(youTubePlaylist.getChannel().getTitle());
		}

		Glide.with(getActivity())
						.load(youTubePlaylist.getBannerUrl())
						.apply(new RequestOptions().placeholder(R.drawable.banner_default))
						.into(playlistBannerImageView);

		if (videoGridAdapter == null) {
			videoGridAdapter = new VideoGridAdapter(getActivity(), false /*hide channel name*/);
			videoGridAdapter.setVideoCategory(VideoCategory.PLAYLIST_VIDEOS, youTubePlaylist.getId());
		}

		gridView.setHasFixedSize(false);
		gridView.setLayoutManager(new GridLayoutManager(getActivity(), getResources().getInteger(R.integer.video_grid_num_columns)));
		gridView.setAdapter(videoGridAdapter);

		return view;
	}
}
