package free.rm.skytube.gui.fragments;

import android.graphics.PorterDuff;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import free.rm.skytube.R;
import free.rm.skytube.businessobjects.VideoStream.StreamMetaData;
import free.rm.skytube.businessobjects.VideoStream.StreamMetaDataList;
import free.rm.skytube.businessobjects.YouTubeVideo;
import free.rm.skytube.gui.activities.YouTubePlayerActivity;
import free.rm.skytube.gui.businessobjects.MediaControllerEx;
import hollowsoft.slidingdrawer.SlidingDrawer;

/**
 * A fragment that holds a standalone YouTube player.
 */
public class YouTubePlayerFragment extends FragmentEx implements MediaPlayer.OnPreparedListener {

	private YouTubeVideo		youTubeVideo = null;
	private VideoView			videoView = null;
	private MediaControllerEx	mediaController = null;
	private View				voidView = null;

	private SlidingDrawer		videoDescriptionDrawer = null;
	private SlidingDrawer		commentsDrawer = null;

	private Handler				timerHandler = null;

	private static final int UI_VISIBILITY_TIMEOUT = 7000;
	private static final String TAG = YouTubePlayerFragment.class.getSimpleName();


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// inflate the layout for this fragment
		View view = inflater.inflate(R.layout.fragment_youtube_player, container, false);

		if (youTubeVideo == null) {
			youTubeVideo = (YouTubeVideo) getActivity().getIntent().getExtras().getSerializable(YouTubePlayerActivity.YOUTUBE_VIDEO_OBJ);

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

			commentsDrawer = (SlidingDrawer) view.findViewById(R.id.comments_drawer);

			// hide action bar
			getActionBar().hide();

			new GetStreamTask(youTubeVideo).execute();
		}

		return view;
	}


	@Override
	public void onPrepared(MediaPlayer mediaPlayer) {
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
			}, UI_VISIBILITY_TIMEOUT);
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



	/**
	 * Given a YouTubeVideo, it will asynchronously get a list of streams (supplied by YouTube) and
	 * then it asks the videoView to start playing a stream.
	 */
	private class GetStreamTask extends AsyncTask<Void, Exception, StreamMetaDataList> {

		/** YouTube Video */
		private YouTubeVideo youTubeVideo;


		public GetStreamTask(YouTubeVideo youTubeVideo) {
			this.youTubeVideo = youTubeVideo;
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
			}
		}
	}

}
