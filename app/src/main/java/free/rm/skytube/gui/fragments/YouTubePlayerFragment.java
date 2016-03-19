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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import free.rm.skytube.R;
import free.rm.skytube.businessobjects.GetVideoDescription;
import free.rm.skytube.businessobjects.GetVideosDetailsByIDs;
import free.rm.skytube.businessobjects.VideoStream.StreamMetaData;
import free.rm.skytube.businessobjects.VideoStream.StreamMetaDataList;
import free.rm.skytube.businessobjects.YouTubeChannel;
import free.rm.skytube.businessobjects.YouTubeVideo;
import free.rm.skytube.businessobjects.db.CheckIfUserSubbedToChannelTask;
import free.rm.skytube.businessobjects.db.SubscribeToChannelTask;
import free.rm.skytube.gui.activities.YouTubePlayerActivity;
import free.rm.skytube.gui.app.SkyTubeApp;
import free.rm.skytube.gui.businessobjects.CommentsAdapter;
import free.rm.skytube.gui.businessobjects.FragmentEx;
import free.rm.skytube.gui.businessobjects.MediaControllerEx;
import free.rm.skytube.gui.businessobjects.SubscribeButton;
import hollowsoft.slidingdrawer.OnDrawerOpenListener;
import hollowsoft.slidingdrawer.SlidingDrawer;

/**
 * A fragment that holds a standalone YouTube player.
 */
public class YouTubePlayerFragment extends FragmentEx implements MediaPlayer.OnPreparedListener {

	private YouTubeVideo		youTubeVideo = null;
	private YouTubeChannel		youTubeChannel = null;

	private VideoView			videoView = null;
	private MediaControllerEx	mediaController = null;
	private TextView			videoDescTitleTextView = null;
	private TextView			videoDescChannelTextView = null;
	private SubscribeButton		videoDescSubscribeButton = null;
	private TextView			videoDescViewsTextView = null;
	private TextView			videoDescLikesTextView = null;
	private TextView			videoDescDislikesTextView = null;
	private TextView			videoDescPublishDateTextView = null;
	private TextView			videoDescriptionTextView = null;
	private ProgressBar			videoDescLikesBar = null;
	private View				voidView = null;
	private View				loadingVideoView = null;

	private SlidingDrawer		videoDescriptionDrawer = null;
	private SlidingDrawer		commentsDrawer = null;
	private View				commentsProgressBar = null,
								noVideoCommentsView = null;
	private CommentsAdapter		commentsAdapter = null;
	private ExpandableListView	commentsExpandableListView = null;

	private Handler				timerHandler = null;

	private static final int HUD_VISIBILITY_TIMEOUT = 7000;
	private static final String TAG = YouTubePlayerFragment.class.getSimpleName();


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// inflate the layout for this fragment
		View view = inflater.inflate(R.layout.fragment_youtube_player, container, false);

		// indicate that this fragment has an action bar menu
		setHasOptionsMenu(true);

		if (youTubeVideo == null) {
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
			videoDescTitleTextView = (TextView) view.findViewById(R.id.video_desc_title);
			videoDescChannelTextView = (TextView) view.findViewById(R.id.video_desc_channel);
			videoDescViewsTextView = (TextView) view.findViewById(R.id.video_desc_views);
			videoDescLikesTextView = (TextView) view.findViewById(R.id.video_desc_likes);
			videoDescDislikesTextView = (TextView) view.findViewById(R.id.video_desc_dislikes);
			videoDescPublishDateTextView = (TextView) view.findViewById(R.id.video_desc_publish_date);
			videoDescriptionTextView = (TextView) view.findViewById(R.id.video_desc_description);
			videoDescLikesBar = (ProgressBar) view.findViewById(R.id.video_desc_likes_bar);
			videoDescSubscribeButton = (SubscribeButton) view.findViewById(R.id.video_desc_subscribe_button);
			videoDescSubscribeButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					// subscribe / unsubscribe to this video's channel
					new SubscribeToChannelTask(videoDescSubscribeButton, youTubeChannel).execute();
				}
			});

			commentsExpandableListView = (ExpandableListView) view.findViewById(R.id.commentsExpandableListView);
			commentsProgressBar = view.findViewById(R.id.comments_progress_bar);
			noVideoCommentsView = view.findViewById(R.id.no_video_comments_text_view);
			commentsDrawer = (SlidingDrawer) view.findViewById(R.id.comments_drawer);
			commentsDrawer.setOnDrawerOpenListener(new OnDrawerOpenListener() {
				@Override
				public void onDrawerOpened() {
					if (commentsAdapter == null) {
						commentsAdapter = new CommentsAdapter(youTubeVideo.getId(), commentsExpandableListView, commentsProgressBar, noVideoCommentsView);
					}
				}
			});

			// hide action bar
			getActionBar().hide();

			// get which video we need to play...
			Bundle bundle = getActivity().getIntent().getExtras();
			if (bundle != null  &&  bundle.getSerializable(YouTubePlayerActivity.YOUTUBE_VIDEO_OBJ) != null) {
				// ... either the video details are passed through the previous activity
				youTubeVideo = (YouTubeVideo) bundle.getSerializable(YouTubePlayerActivity.YOUTUBE_VIDEO_OBJ);
				setUpHUDAndPlayVideo();

				getVideoInfoTasks();
			} else {
				// ... or the video URL is passed to SkyTube via another Android app
				GetVideoDetailsTask getVideoDetailsTask = new GetVideoDetailsTask();
				getVideoDetailsTask.execute();
			}
		}

		return view;
	}


	private void getVideoInfoTasks() {
		// get Channel info (e.g. avatar...etc) task
		new GetYouTubeChannelInfoTask().execute(youTubeVideo.getChannelId());

		// check if the user has subscribed to a channel... if he has, then change the state of
		// the subscribe button
		new CheckIfUserSubbedToChannelTask(videoDescSubscribeButton, youTubeVideo.getChannelId()).execute();
	}


	/**
	 * Will setup the HUD's details according to the contents of {@link #youTubeVideo}.  Then it
	 * will try to load and play the video.
	 */
	private void setUpHUDAndPlayVideo() {
		videoDescTitleTextView.setText(youTubeVideo.getTitle());
		videoDescChannelTextView.setText(youTubeVideo.getChannelName());
		videoDescViewsTextView.setText(youTubeVideo.getViewsCount());

		if (youTubeVideo.isThumbsUpPercentageSet()) {
			videoDescLikesTextView.setText(youTubeVideo.getLikeCount());
			videoDescDislikesTextView.setText(youTubeVideo.getDislikeCount());
			videoDescPublishDateTextView.setText(youTubeVideo.getPublishDate());

			videoDescLikesBar.setProgress(youTubeVideo.getThumbsUpPercentage());
			videoDescLikesBar.getProgressDrawable().setColorFilter(getResources().getColor(R.color.video_desc_like_bar), PorterDuff.Mode.SRC_IN);
		}

		// load the video
		loadVideo();
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
			case R.id.menu_open_video_with:
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


	/**
	 * This task will, from the given video URL, get the details of the video (e.g. video name,
	 * likes ...etc).
	 */
	private class GetVideoDetailsTask extends AsyncTask<Void, Void, YouTubeVideo> {

		private String videoUrl = null;


		@Override
		protected void onPreExecute() {
			videoUrl = getUrlFromIntent(getActivity().getIntent());
		}


		/**
		 * Returns an instance of {@link YouTubeVideo} from the given {@link #videoUrl}.
		 *
		 * @return {@link YouTubeVideo}; null if an error has occurred.
		 */
		@Override
		protected YouTubeVideo doInBackground(Void... params) {
			String videoId = getYouTubeIdFromUrl(videoUrl);
			YouTubeVideo youTubeVideo = null;

			if (videoId != null) {
				try {
					GetVideosDetailsByIDs getVideo = new GetVideosDetailsByIDs();
					getVideo.init(videoId);
					List<YouTubeVideo> youTubeVideos = getVideo.getNextVideos();

					if (youTubeVideos.size() > 0)
						youTubeVideo = youTubeVideos.get(0);
				} catch (IOException ex) {
					Log.e(TAG, "Unable to get video details, where id="+videoId, ex);
				}
			}

			return youTubeVideo;
		}


		@Override
		protected void onPostExecute(YouTubeVideo youTubeVideo) {
			if (youTubeVideo == null) {
				String err = String.format(getString(R.string.error_invalid_url), videoUrl);
				Toast.makeText(getActivity(), err, Toast.LENGTH_LONG).show();
				Log.e(TAG, err);
				getActivity().finish();
			} else {
				YouTubePlayerFragment.this.youTubeVideo = youTubeVideo;
				setUpHUDAndPlayVideo();	// setup the HUD and play the video

				getVideoInfoTasks();
			}
		}


		/**
		 * The video URL is passed to SkyTube via another Android app (i.e. via an intent).
		 *
		 * @return The URL of the YouTube video the user wants to play.
		 */
		private String getUrlFromIntent(final Intent intent) {
			String url = null;

			if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
				url = intent.getData().toString();
			}

			return url;
		}


		/**
		 * Extracts the video ID from the given video URL.
		 *
		 * @param url	YouTube video URL.
		 * @return ID if everything went as planned; null otherwise.
		 */
		private String getYouTubeIdFromUrl(String url) {
			if (url == null)
				return null;

			final String pattern = "(?<=v=|/videos/|embed/|youtu\\.be/|/v/|/e/)[^#&\\?]*";
			Pattern compiledPattern = Pattern.compile(pattern);
			Matcher matcher = compiledPattern.matcher(url);

			return matcher.find() ? matcher.group() /*video id*/ : null;
		}

	}


	////////////////////////////////////////////////////////////////////////////////////////////////


	private class GetYouTubeChannelInfoTask extends AsyncTask<String, Void, YouTubeChannel> {

		private final String TAG = GetYouTubeChannelInfoTask.class.getSimpleName();

		@Override
		protected YouTubeChannel doInBackground(String... channelId) {
			YouTubeChannel chn = new YouTubeChannel();

			try {
				chn.init(channelId[0]);
			} catch (IOException e) {
				Log.e(TAG, "Unable to get channel info.  ChannelID=" + channelId[0], e);
				chn = null;
			}

			return chn;
		}

		@Override
		protected void onPostExecute(YouTubeChannel youTubeChannel) {
			YouTubePlayerFragment.this.youTubeChannel = youTubeChannel;
		}

	}

}
