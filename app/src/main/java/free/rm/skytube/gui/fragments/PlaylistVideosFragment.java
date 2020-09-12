package free.rm.skytube.gui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import butterknife.BindView;
import butterknife.ButterKnife;
import free.rm.skytube.R;
import free.rm.skytube.businessobjects.VideoCategory;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubePlaylist;

/**
 * A Fragment that displays the videos of a playlist in a {@link VideosGridFragment}
 */
public class PlaylistVideosFragment extends VideosGridFragment {

	private YouTubePlaylist youTubePlaylist;

	@BindView(R.id.toolbar)
	Toolbar     toolbar;
	@BindView(R.id.playlist_banner_image_view)
	ImageView   playlistBannerImageView;
	@BindView(R.id.playlist_thumbnail_image_view)
	ImageView   playlistThumbnailImageView;
	@BindView(R.id.playlist_title_text_view)
	TextView    playlistTitleTextView;

	public static final String PLAYLIST_OBJ = "PlaylistVideosFragment.PLAYLIST_OBJ";


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// sets the play list
		youTubePlaylist = (YouTubePlaylist)getArguments().getSerializable(PLAYLIST_OBJ);

		View view = super.onCreateView(inflater, container, savedInstanceState);

		if (youTubePlaylist == null) {
			Log.e(this.getClass().getSimpleName(), "No playlist object found:" + getArguments());
			showMain();
			return view;
		}

		ButterKnife.bind(this, view);
		playlistTitleTextView.setText(youTubePlaylist.getTitle());

		// setup the toolbar/actionbar
		setSupportActionBar(toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		ActionBar actionBar = getSupportActionBar();
		String channelTitle = youTubePlaylist.getChannelTitle();
		if (actionBar != null && channelTitle != null) {
			actionBar.setTitle(channelTitle);
		}

		// set the playlist's thumbnail
		Glide.with(getActivity())
				.load(youTubePlaylist.getThumbnailUrl())
				.apply(new RequestOptions().placeholder(R.drawable.channel_thumbnail_default))
				.into(playlistThumbnailImageView);

		// set the channel's banner
		Glide.with(getActivity())
				.load(youTubePlaylist.getBannerUrl())
				.apply(new RequestOptions().placeholder(R.drawable.banner_default))
				.into(playlistBannerImageView);

		// Force initialization
		videoGridAdapter.initializeList();
		return view;
	}

	private void showMain() {
		Intent startMain = new Intent(Intent.ACTION_MAIN);
		startMain.addCategory(Intent.CATEGORY_HOME);
		startActivity(startMain);
	}

	@Override
	protected int getLayoutResource() {
		return R.layout.fragment_playlist_videos;
	}

	@Override
	public String getFragmentName() {
		return null;
	}


	@Override
	protected VideoCategory getVideoCategory() {
		return youTubePlaylist.getChannelTitle() != null ? VideoCategory.PLAYLIST_VIDEOS : VideoCategory.MIXED_PLAYLIST_VIDEOS;
	}


	@Override
	protected String getSearchString() {
		return youTubePlaylist.getId();
	}

	@Override
	public int getPriority() {
		return 0;
	}
}
