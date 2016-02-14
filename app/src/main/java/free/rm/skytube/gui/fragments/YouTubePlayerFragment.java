package free.rm.skytube.gui.fragments;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.IOException;
import java.util.List;

import free.rm.skytube.R;
import free.rm.skytube.businessobjects.GetCommentThreads;
import free.rm.skytube.businessobjects.GetVideoDescription;
import free.rm.skytube.businessobjects.VideoStream.StreamMetaData;
import free.rm.skytube.businessobjects.VideoStream.StreamMetaDataList;
import free.rm.skytube.businessobjects.YouTubeCommentThread;
import free.rm.skytube.businessobjects.YouTubeVideo;
import free.rm.skytube.gui.activities.YouTubePlayerActivity;
import free.rm.skytube.gui.app.SkyTubeApp;
import free.rm.skytube.gui.businessobjects.CommentsAdapter;
import free.rm.skytube.gui.businessobjects.FragmentEx;
import free.rm.skytube.gui.businessobjects.MediaControllerEx;
import hollowsoft.slidingdrawer.OnDrawerOpenListener;
import hollowsoft.slidingdrawer.SlidingDrawer;

/**
 * A fragment that holds a standalone YouTube player.
 */
public class YouTubePlayerFragment extends FragmentEx implements MediaPlayer.OnPreparedListener {

	private YouTubeVideo		youTubeVideo = null;
	private VideoView			videoView = null;
	private MediaControllerEx	mediaController = null;
	private TextView			videoDescriptionTextView = null;
	private View				voidView = null;
	private View				loadingVideoView = null;

	private SlidingDrawer		videoDescriptionDrawer = null;
	private SlidingDrawer		commentsDrawer = null;
	private GetCommentThreads	getCommentThreads = null;
	private CommentsAdapter		commentsAdapter = null;
	private ExpandableListView	commentsExpandableListView = null;

	private Handler				timerHandler = null;

	private static final int HUD_VISIBILITY_TIMEOUT = 7000;
	private static final String TAG = YouTubePlayerFragment.class.getSimpleName();


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// inflate the layout for this fragment
		View view = inflater.inflate(R.layout.fragment_youtube_player, container, false);

		// indicate that this fragment has a action bar menu
		setHasOptionsMenu(true);

		if (youTubeVideo == null) {
			youTubeVideo = (YouTubeVideo) getActivity().getIntent().getExtras().getSerializable(YouTubePlayerActivity.YOUTUBE_VIDEO_OBJ);

			loadingVideoView = view.findViewById(R.id.loadingVideoView);

			videoView = (VideoView) view.findViewById(R.id.video_view);
			// play the video once its loaded
			videoView.setOnPreparedListener(this);

			// setup the media controller (will control the video playing/pausing)
			mediaController = new MediaControllerEx(getActivity(), videoView);

			voidView = view.findViewById(R.id.void_view);
			voidView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					showOrHideHud();
				}
			});

			videoDescriptionDrawer = (SlidingDrawer) view.findViewById(R.id.des_drawer);
			((TextView) view.findViewById(R.id.video_desc_title)).setText(youTubeVideo.getTitle());
			((TextView) view.findViewById(R.id.video_desc_channel)).setText(youTubeVideo.getChannelName());
			((TextView) view.findViewById(R.id.video_desc_views)).setText(youTubeVideo.getViewsCount());

			if (youTubeVideo.isThumbsUpPercentageSet()) {
				((TextView) view.findViewById(R.id.video_desc_likes)).setText(youTubeVideo.getLikeCount());
				((TextView) view.findViewById(R.id.video_desc_dislikes)).setText(youTubeVideo.getDislikeCount());
				((TextView) view.findViewById(R.id.video_desc_publish_date)).setText(youTubeVideo.getPublishDate());

				ProgressBar likesBar = (ProgressBar) view.findViewById(R.id.video_desc_likes_bar);
				likesBar.setProgress(youTubeVideo.getThumbsUpPercentage());
				likesBar.getProgressDrawable().setColorFilter(getResources().getColor(R.color.video_desc_like_bar), PorterDuff.Mode.SRC_IN);
			}

			videoDescriptionTextView = (TextView) view.findViewById(R.id.video_desc_description);

			commentsDrawer = (SlidingDrawer) view.findViewById(R.id.comments_drawer);
			commentsDrawer.setOnDrawerOpenListener(new OnDrawerOpenListener() {
				@Override
				public void onDrawerOpened() {
					Toast.makeText(YouTubePlayerFragment.this.getActivity(), "Drawer opened", Toast.LENGTH_SHORT).show();
					new GetCommentsTask().execute();
				}
			});
			commentsExpandableListView = (ExpandableListView) view.findViewById(R.id.commentsExpandableListView);

			// hide action bar
			getActionBar().hide();

			// load the video
			loadVideo();
		}

		return view;
	}


	@Override
	public void onPrepared(MediaPlayer mediaPlayer) {
		loadingVideoView.setVisibility(View.GONE);
		videoView.start();
		showHud();
	}



	/**
	 * @return True if the HUD is visible (provided that this Fragment is also visible).
	 */
	private boolean isHudVisible() {
		return isVisible()  &&  (mediaController.isShowing()  ||  getActionBar().isShowing());
	}



	/**
	 * Hide or display the HUD depending if the HUD is currently visible or not.
	 */
	private void showOrHideHud() {
		if (isHudVisible())
			hideHud();
		else
			showHud();
	}



	/**
	 * Show the HUD (head-up display), i.e. the Action Bar and Media Controller.
	 */
	private void showHud() {
		if (!isHudVisible()) {
			getActionBar().show();
			getActionBar().setTitle(youTubeVideo.getTitle());
			mediaController.show(0);

			videoDescriptionDrawer.close();
			videoDescriptionDrawer.setVisibility(View.INVISIBLE);
			commentsDrawer.close();
			commentsDrawer.setVisibility(View.INVISIBLE);

			// hide UI after a certain timeout (defined in UI_VISIBILITY_TIMEOUT)
			timerHandler = new Handler();
			timerHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					hideHud();
					timerHandler = null;
				}
			}, HUD_VISIBILITY_TIMEOUT);
		}
	}



	/**
	 * Hide the HUD.
	 */
	private void hideHud() {
		if (isHudVisible()) {
			getActionBar().hide();
			mediaController.hide();

			videoDescriptionDrawer.setVisibility(View.VISIBLE);
			commentsDrawer.setVisibility(View.VISIBLE);

			// If there is a timerHandler running, then cancel it (stop if from running).  This way,
			// if the HUD was hidden on the 5th second, and the user reopens the HUD, this code will
			// prevent the HUD to re-disappear 2 seconds after it was displayed (assuming that
			// UI_VISIBILITY_TIMEOUT = 7 seconds).
			if (timerHandler != null) {
				timerHandler.removeCallbacksAndMessages(null);
				timerHandler = null;
			}
		}
	}


	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.menu_youtube_player, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_reload_video:
				loadVideo();
				return true;
			case R.id.menu_open_video_in_browser:
				Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v="+youTubeVideo.getId()));
				startActivity(browserIntent);
				videoView.pause();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}


	/**
	 * Loads the video specified in {@link #youTubeVideo}.
	 */
	private void loadVideo() {
		// get the video's steam
		new GetStreamTask(youTubeVideo, true).execute();
		// get the video description
		new GetVideoDescriptionTask().execute();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////


	/**
	 * Given a YouTubeVideo, it will asynchronously get a list of streams (supplied by YouTube) and
	 * then it asks the videoView to start playing a stream.
	 */
	private class GetStreamTask extends AsyncTask<Void, Exception, StreamMetaDataList> {

		/** YouTube Video */
		private YouTubeVideo	youTubeVideo;
		/** The current video position (i.e. time).  Set to -1 if we are not interested in reloading
		 *  the video. */
		private	int				currentVideoPosition = -1;


		public GetStreamTask(YouTubeVideo youTubeVideo) {
			this(youTubeVideo, false);
		}

		/**
		 * Returns a stream for the given video.  If reloadVideo is set to true, then it will stop
		 * the current video, get a NEW stream and then resume playing.
		 *
		 * @param youTubeVideo	YouTube video
		 * @param reloadVideo	Set to true to reload a video
		 */
		public GetStreamTask(YouTubeVideo youTubeVideo, boolean reloadVideo) {
			this.youTubeVideo = youTubeVideo;

			if (reloadVideo) {
				boolean isVideoPlaying = videoView.isPlaying();

				videoView.pause();
				this.currentVideoPosition = isVideoPlaying ? videoView.getCurrentPosition() : 0;
				videoView.stopPlayback();
				loadingVideoView.setVisibility(View.VISIBLE);
			}
		}


		@Override
		protected StreamMetaDataList doInBackground(Void... param) {
			return youTubeVideo.getVideoStreamList();
		}


		@Override
		protected void onPostExecute(StreamMetaDataList streamMetaDataList) {
			if (streamMetaDataList == null) {
				// if the stream list is null, then it means an error has occurred
				Toast.makeText(YouTubePlayerFragment.this.getActivity(),
						String.format(getActivity().getString(R.string.error_get_video_streams), youTubeVideo.getId()),
						Toast.LENGTH_LONG).show();
			} else if (streamMetaDataList.size() <= 0) {
				// if steam list if empty, then it means something went wrong...
				Toast.makeText(YouTubePlayerFragment.this.getActivity(),
						String.format(getActivity().getString(R.string.error_video_streams_empty), youTubeVideo.getId()),
						Toast.LENGTH_LONG).show();
			} else {
				Log.i(TAG, streamMetaDataList.toString());

				// get the desired stream based on user preferences
				StreamMetaData desiredStream = streamMetaDataList.getDesiredStream();

				// play the video
				Log.i(TAG, ">> PLAYING: " + desiredStream);
				videoView.setVideoURI(desiredStream.getUri());

				// if we are reloading a video... then seek the correct position
				if (currentVideoPosition >= 0) {
					videoView.seekTo(currentVideoPosition);
				}
			}
		}
	}

	////////////////////////////////////////////////////////////////////////////////////////////////


	/**
	 * Get the video's description and set the appropriate text view.
	 */
	private class GetVideoDescriptionTask extends AsyncTask<Void, Void, String> {

		@Override
		protected String doInBackground(Void... params) {
			GetVideoDescription getVideoDescription = new GetVideoDescription();
			String description = SkyTubeApp.getStr(R.string.error_get_video_desc);

			try {
				getVideoDescription.init(youTubeVideo.getId());
				List<YouTubeVideo> list = getVideoDescription.getNextVideos();

				if (list.size() > 0) {
					description = list.get(0).getDescription();
				}
			} catch (IOException e) {
				Log.e(TAG, description + " - id=" + youTubeVideo.getId(), e);
			}

			return description;
		}

		@Override
		protected void onPostExecute(String description) {
			videoDescriptionTextView.setText(description);
		}

	}

	////////////////////////////////////////////////////////////////////////////////////////////////

	private class GetCommentsTask extends AsyncTask<Void, Void, List<YouTubeCommentThread>> {

		protected GetCommentsTask() {
			if (YouTubePlayerFragment.this.getCommentThreads == null) {
				getCommentThreads = new GetCommentThreads();
			}
		}

		@Override
		protected  List<YouTubeCommentThread> doInBackground(Void... params) {
			return getCommentThreads.get(youTubeVideo.getId());
		}

		@Override
		protected void onPostExecute(List<YouTubeCommentThread> commentThreadsList) {
			commentsAdapter = new CommentsAdapter(commentThreadsList, commentsExpandableListView);
			commentsExpandableListView.setAdapter(commentsAdapter);
		}
	}

}
