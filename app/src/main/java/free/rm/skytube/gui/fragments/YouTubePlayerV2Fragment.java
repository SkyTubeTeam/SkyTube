/*
 * SkyTube
 * Copyright (C) 2018  Ramon Mifsud
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation (version 3 of the License).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package free.rm.skytube.gui.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;

import java.util.Locale;

import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.app.enums.Policy;
import free.rm.skytube.businessobjects.GetVideoDetailsTask;
import free.rm.skytube.businessobjects.Logger;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;
import free.rm.skytube.businessobjects.YouTube.Tasks.GetVideoDescriptionTask;
import free.rm.skytube.businessobjects.YouTube.VideoStream.StreamMetaData;
import free.rm.skytube.businessobjects.db.DownloadedVideosDb;
import free.rm.skytube.businessobjects.db.PlaybackStatusDb;
import free.rm.skytube.businessobjects.db.Tasks.CheckIfUserSubbedToChannelTask;
import free.rm.skytube.businessobjects.db.Tasks.GetChannelInfo;
import free.rm.skytube.businessobjects.db.Tasks.IsVideoBookmarkedTask;
import free.rm.skytube.businessobjects.interfaces.GetDesiredStreamListener;
import free.rm.skytube.businessobjects.interfaces.YouTubePlayerActivityListener;
import free.rm.skytube.businessobjects.interfaces.YouTubePlayerFragmentInterface;
import free.rm.skytube.gui.activities.ThumbnailViewerActivity;
import free.rm.skytube.gui.businessobjects.MobileNetworkWarningDialog;
import free.rm.skytube.gui.businessobjects.PlayerViewGestureDetector;
import free.rm.skytube.gui.businessobjects.ResumeVideoTask;
import free.rm.skytube.gui.businessobjects.SkyTubeMaterialDialog;
import free.rm.skytube.gui.businessobjects.YouTubePlayer;
import free.rm.skytube.gui.businessobjects.adapters.CommentsAdapter;
import free.rm.skytube.gui.businessobjects.fragments.ImmersiveModeFragment;
import free.rm.skytube.gui.businessobjects.views.ClickableLinksTextView;
import free.rm.skytube.gui.businessobjects.views.SubscribeButton;
import hollowsoft.slidingdrawer.SlidingDrawer;

import static free.rm.skytube.gui.activities.YouTubePlayerActivity.YOUTUBE_VIDEO_OBJ;

/**
 * A fragment that holds a standalone YouTube player (version 2).
 */
public class YouTubePlayerV2Fragment extends ImmersiveModeFragment implements YouTubePlayerFragmentInterface {

	private YouTubeVideo		    youTubeVideo = null;
	private YouTubeChannel          youTubeChannel = null;

	private PlayerView              playerView;
	private SimpleExoPlayer         player;
	private long				    playerInitialPosition = 0;

	private Menu                    menu = null;

	private TextView			    videoDescTitleTextView = null;
	private ImageView			    videoDescChannelThumbnailImageView = null;
	private TextView			    videoDescChannelTextView = null;
	private SubscribeButton         videoDescSubscribeButton = null;
	private TextView			    videoDescViewsTextView = null;
	private ProgressBar             videoDescLikesBar = null;
	private TextView			    videoDescLikesTextView = null;
	private TextView			    videoDescDislikesTextView = null;
	private View                    videoDescRatingsDisabledTextView = null;
	private TextView			    videoDescPublishDateTextView = null;
	private ClickableLinksTextView	videoDescriptionTextView = null;
	private View				    loadingVideoView = null;
	private SlidingDrawer           videoDescriptionDrawer = null;
	private SlidingDrawer		    commentsDrawer = null;
	private View				    commentsProgressBar = null,
									noVideoCommentsView = null;
	private CommentsAdapter         commentsAdapter = null;
	private ExpandableListView      commentsExpandableListView = null;
	private YouTubePlayerActivityListener listener = null;


	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		hideNavigationBar();

		// inflate the layout for this fragment
		View view = inflater.inflate(R.layout.fragment_youtube_player_v2, container, false);

		// indicate that this fragment has an action bar menu
		setHasOptionsMenu(true);

//		final View decorView = getActivity().getWindow().getDecorView();
//		decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
//			@Override
//			public void onSystemUiVisibilityChange(int visibility) {
//				hideNavigationBar();
//			}
//		});

		///if (savedInstanceState != null)
		///	videoCurrentPosition = savedInstanceState.getInt(VIDEO_CURRENT_POSITION, 0);

		if (youTubeVideo == null) {
			// initialise the views
			initViews(view);

			// get which video we need to play...
			Intent intent = getActivity().getIntent();
			Bundle bundle = intent.getExtras();
			if (bundle != null  &&  bundle.getSerializable(YOUTUBE_VIDEO_OBJ) != null) {
				// ... either the video details are passed through the previous activity
				youTubeVideo = (YouTubeVideo) bundle.getSerializable(YOUTUBE_VIDEO_OBJ);
				setUpHUDAndPlayVideo();

				getVideoInfoTasks();
			} else {
				// ... or the video URL is passed to SkyTube via another Android app
				new GetVideoDetailsTask(getUrlFromIntent(intent),  this::youTubeVideoListener).executeInParallel();
			}

		}

		return view;
	}

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		try {
			Activity activity = (Activity)context;
			listener = (YouTubePlayerActivityListener)activity;
		} catch (ClassCastException e) {
			throw new ClassCastException("YouTubePlayerFragment must be instantiated from an Activity that implements YouTubePlayerActivityListener");
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

	protected void youTubeVideoListener(String videoUrl, YouTubeVideo video) {
		if (video == null) {
			// invalid URL error (i.e. we are unable to decode the URL)
			String err = String.format(getString(R.string.error_invalid_url), videoUrl);
			Toast.makeText(getActivity(), err, Toast.LENGTH_LONG).show();

			// log error
			Logger.e(this, err);

			// close the video player activity
			closeActivity();
		} else {
			this.youTubeVideo = video;

			// setup the HUD and play the video
			setUpHUDAndPlayVideo();

			getVideoInfoTasks();

			// will now check if the video is bookmarked or not (and then update the menu
			// accordingly)
			new IsVideoBookmarkedTask(youTubeVideo, menu).executeInParallel();
		}
	}

	
	/**
	 * Initialise the views.
	 *
	 * @param view Fragment view.
	 */
	private void initViews(View view) {
		// setup the toolbar / actionbar
		Toolbar toolbar = view.findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setDisplayShowHomeEnabled(true);

		// setup the player
		playerView = view.findViewById(R.id.player_view);
		final PlayerViewGestureHandler playerViewGestureHandler = new PlayerViewGestureHandler();
		playerViewGestureHandler.initView(view);
		playerView.setOnTouchListener(playerViewGestureHandler);
		playerView.requestFocus();

		player = createExoPlayer();
		player.addListener(new Player.DefaultEventListener() {
			@Override
			public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
				Logger.i(this, ">> onPlayerStateChanged " + playWhenReady + " state="+playbackState);
				if (playbackState == Player.STATE_READY && playWhenReady) {
					preventDeviceSleeping(true);
				} else {
					preventDeviceSleeping(false);
				}
			}
		});
		player.setPlayWhenReady(true);
		playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);               // ensure that videos are played in their correct aspect ratio
		player.setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);    // ensure that videos are played in their correct aspect ratio
		playerView.setPlayer(player);

		loadingVideoView = view.findViewById(R.id.loadingVideoView);

		videoDescriptionDrawer = view.findViewById(R.id.des_drawer);
		videoDescTitleTextView = view.findViewById(R.id.video_desc_title);
		videoDescChannelThumbnailImageView = view.findViewById(R.id.video_desc_channel_thumbnail_image_view);
		videoDescChannelThumbnailImageView.setOnClickListener(v -> {
			if (youTubeChannel != null) {
				YouTubePlayer.launchChannel(youTubeChannel, getActivity());
			}
		});
		videoDescChannelTextView = view.findViewById(R.id.video_desc_channel);
		videoDescViewsTextView = view.findViewById(R.id.video_desc_views);
		videoDescLikesTextView = view.findViewById(R.id.video_desc_likes);
		videoDescDislikesTextView = view.findViewById(R.id.video_desc_dislikes);
		videoDescRatingsDisabledTextView = view.findViewById(R.id.video_desc_ratings_disabled);
		videoDescPublishDateTextView = view.findViewById(R.id.video_desc_publish_date);
		videoDescriptionTextView = view.findViewById(R.id.video_desc_description);
		videoDescLikesBar = view.findViewById(R.id.video_desc_likes_bar);
		videoDescSubscribeButton = view.findViewById(R.id.video_desc_subscribe_button);

		commentsExpandableListView = view.findViewById(R.id.commentsExpandableListView);
		commentsProgressBar = view.findViewById(R.id.comments_progress_bar);
		noVideoCommentsView = view.findViewById(R.id.no_video_comments_text_view);
		commentsDrawer = view.findViewById(R.id.comments_drawer);
		commentsDrawer.setOnDrawerOpenListener(() -> {
			if (commentsAdapter == null) {
				commentsAdapter = new CommentsAdapter(getActivity(), youTubeVideo.getId(), commentsExpandableListView, commentsProgressBar, noVideoCommentsView);
			}
		});
	}

	private SimpleExoPlayer createExoPlayer() {
		DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();

		TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory();
		DefaultTrackSelector trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);
		Context context = getContext();
		DefaultRenderersFactory defaultRenderersFactory = new DefaultRenderersFactory(context);

		return ExoPlayerFactory.newSimpleInstance(getContext(), defaultRenderersFactory, trackSelector, new DefaultLoadControl(), null, bandwidthMeter);
	}


	/**
	 * Will setup the HUD's details according to the contents of {@link #youTubeVideo}.  Then it
	 * will try to load and play the video.
	 */
	private void setUpHUDAndPlayVideo() {
		getSupportActionBar().setTitle(youTubeVideo.getTitle());
		videoDescTitleTextView.setText(youTubeVideo.getTitle());
		videoDescChannelTextView.setText(youTubeVideo.getChannelName());
		videoDescViewsTextView.setText(youTubeVideo.getViewsCount());
		videoDescPublishDateTextView.setText(youTubeVideo.getPublishDatePretty());

		if (youTubeVideo.isThumbsUpPercentageSet()) {
			videoDescLikesTextView.setText(youTubeVideo.getLikeCount());
			videoDescDislikesTextView.setText(youTubeVideo.getDislikeCount());
			videoDescLikesBar.setProgress(youTubeVideo.getThumbsUpPercentage());
		} else {
			videoDescLikesTextView.setVisibility(View.GONE);
			videoDescDislikesTextView.setVisibility(View.GONE);
			videoDescLikesBar.setVisibility(View.GONE);
			videoDescRatingsDisabledTextView.setVisibility(View.VISIBLE);
		}

        new ResumeVideoTask(getContext(), youTubeVideo, position -> {
			playerInitialPosition = position;
			YouTubePlayerV2Fragment.this.loadVideo();
		}).ask();

	}


	/**
	 * Loads the video specified in {@link #youTubeVideo}.
	 */
	private void loadVideo() {
		loadVideo(true);
	}

	private void preventDeviceSleeping(boolean flag) {
		// prevent the device from sleeping while playing
		Activity activity = getActivity();
		if (activity != null) {
			Window window = activity.getWindow();
			if (window != null) {
				if (flag) {
					Logger.i(this, ">> Setting FLAG_KEEP_SCREEN_ON");
					window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
				} else {
					Logger.i(this, ">> Clearing FLAG_KEEP_SCREEN_ON");
					window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
				}
			}
		}
	}

	/**
	 * Loads the video specified in {@link #youTubeVideo}.
	 *
	 * @param showMobileNetworkWarning Set to true to show the warning displayed when the user is
	 *                                 using mobile network data (i.e. 4g).
	 */
	private void loadVideo(boolean showMobileNetworkWarning) {
		Policy decision = Policy.ALLOW;
        DownloadedVideosDb.Status downloadStatus = DownloadedVideosDb.getVideoDownloadsDb().getVideoFileUriAndValidate(youTubeVideo.getId());

		// if the user is using mobile network (i.e. 4g), then warn him
		if (showMobileNetworkWarning && downloadStatus.getUri() == null) {
			decision = new MobileNetworkWarningDialog(getActivity())
					.onPositive((dialog, which) -> loadVideo(false))
					.onNegativeOrCancel((dialog) -> closeActivity())
					.showAndGetStatus(MobileNetworkWarningDialog.ActionType.STREAM_VIDEO);
		}

		if (decision == Policy.ALLOW) {
			// if the video is NOT live
			if (!youTubeVideo.isLiveStream()) {
				loadingVideoView.setVisibility(View.VISIBLE);

				if (downloadStatus.isDisapeared()) {
                    // If the file for this video has gone missing, warn and then play remotely.
                    Toast.makeText(getContext(),
                            getString(R.string.playing_video_file_missing),
                            Toast.LENGTH_LONG).show();
                    loadVideo();
                    return;
                }
				if (downloadStatus.getUri() != null) {
                    loadingVideoView.setVisibility(View.GONE);
                    Logger.i(this, ">> PLAYING LOCALLY: %s", downloadStatus.getUri());
                    playVideo(downloadStatus.getUri());
                    return;
				} else {
					youTubeVideo.getDesiredStream(new GetDesiredStreamListener() {
						@Override
						public void onGetDesiredStream(StreamMetaData desiredStream) {
							// hide the loading video view (progress bar)
							loadingVideoView.setVisibility(View.GONE);

							// Play the video.  Check if this fragment is visible before playing the
							// video.  It might not be visible if the user clicked on the back button
							// before the video streams are retrieved (such action would cause the app
							// to crash if not catered for...).
							if (isVisible()) {
								Logger.i(YouTubePlayerV2Fragment.this, ">> PLAYING: %s", desiredStream.getUri());
								playVideo(desiredStream.getUri());
							}
						}

						@Override
						public void onGetDesiredStreamError(String errorMessage) {
							if (errorMessage != null) {
								Context ctx = getContext();
								if (ctx == null) {
									Logger.e(YouTubePlayerV2Fragment.this, "Error during getting stream: %s", errorMessage);
									return;
								}
								new SkyTubeMaterialDialog(ctx)
										.content(errorMessage)
										.title(R.string.error_video_play)
										.cancelable(false)
										.onPositive((dialog, which) -> closeActivity())
										.show();
							}
						}
					});
				}
			} else {
				openAsLiveStream();
			}
		}
	}

	private void openAsLiveStream() {
		// else, if the video is a LIVE STREAM
		// video is live:  ask the user if he wants to play the video using an other app
		Context ctx = getContext();
		if (ctx != null) {
			new SkyTubeMaterialDialog(ctx)
					.onNegativeOrCancel((dialog) -> closeActivity())
					.content(R.string.warning_live_video)
					.title(R.string.error_video_play)
					.onPositive((dialog, which) -> {
						youTubeVideo.playVideoExternally(getContext());
						closeActivity();
					})
					.show();
		}
	}


	/**
	 * Play video.
	 *
	 * @param videoUri  The Uri of the video that is going to be played.
	 */
	private void playVideo(Uri videoUri) {
		DefaultDataSourceFactory dataSourceFactory = new DefaultDataSourceFactory(getContext(), "ST. Agent", new DefaultBandwidthMeter());
		ExtractorMediaSource.Factory extMediaSourceFactory = new ExtractorMediaSource.Factory(dataSourceFactory);
		ExtractorMediaSource mediaSource = extMediaSourceFactory.createMediaSource(videoUri);
		player.prepare(mediaSource);

		if (playerInitialPosition > 0)
			player.seekTo(playerInitialPosition);
	}


	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		if (youTubeVideo != null && !youTubeVideo.isDownloaded()) {
			menu.findItem(R.id.download_video).setVisible(true);
		} else {
			menu.findItem(R.id.download_video).setVisible(false);
		}
	}


	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.menu_youtube_player, menu);

		this.menu = menu;

		listener.onOptionsMenuCreated(menu);

		// Will now check if the video is bookmarked or not (and then update the menu accordingly).
		//
		// youTubeVideo might be null if we have only passed the video URL to this fragment (i.e.
		// the app is still trying to construct youTubeVideo in the background).
		if (youTubeVideo != null)
			new IsVideoBookmarkedTask(youTubeVideo, menu).executeInParallel();
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_reload_video:
				player.seekToDefaultPosition();
				return true;

			case R.id.menu_open_video_with:
				player.setPlayWhenReady(false);
				youTubeVideo.playVideoExternally(getContext());
				return true;

			case R.id.share:
				player.setPlayWhenReady(false);
				youTubeVideo.shareVideo(getContext());
				return true;

			case R.id.copyurl:
				youTubeVideo.copyUrl(getContext());
				return true;

			case R.id.bookmark_video:
				youTubeVideo.bookmarkVideo(getContext(), menu);
				return true;

			case R.id.unbookmark_video:
				youTubeVideo.unbookmarkVideo(getContext(), menu);
				return true;

			case R.id.view_thumbnail:
				Intent i = new Intent(getActivity(), ThumbnailViewerActivity.class);
				i.putExtra(ThumbnailViewerActivity.YOUTUBE_VIDEO, youTubeVideo);
				startActivity(i);
				return true;

			case R.id.download_video:
				final Policy decision = new MobileNetworkWarningDialog(getContext())
						.showDownloadWarning(youTubeVideo);

				if (decision == Policy.ALLOW) {
					youTubeVideo.downloadVideo(getContext());
				}
				return true;

			case R.id.block_channel:
				youTubeChannel.blockChannel();
				return true;

			default:
				return super.onOptionsItemSelected(item);
		}
	}


	/**
	 * Called when the options menu is closed.
	 *
	 * <p>The Navigation Bar is displayed when the Option Menu is visible.  Hence the objective of
	 * this method is to hide the Navigation Bar once the Options Menu is hidden.</p>
	 */
	public void onMenuClosed() {
		hideNavigationBar();
	}


	/**
	 * Will asynchronously retrieve additional video information such as channel avatar ...etc
	 */
	private void getVideoInfoTasks() {
		// get Channel info (e.g. avatar...etc) task
		new GetChannelInfo(getContext(), youTubeChannel -> {
			YouTubePlayerV2Fragment.this.youTubeChannel = youTubeChannel;

			videoDescSubscribeButton.setChannel(YouTubePlayerV2Fragment.this.youTubeChannel);
			if (youTubeChannel != null) {
				if(getActivity() != null)
					Glide.with(getActivity())
							.load(youTubeChannel.getThumbnailNormalUrl())
							.apply(new RequestOptions().placeholder(R.drawable.channel_thumbnail_default))
							.into(videoDescChannelThumbnailImageView);
			}
		}).executeInParallel(youTubeVideo.getChannelId());

		// get the video description
		new GetVideoDescriptionTask(youTubeVideo, description -> videoDescriptionTextView.setTextAndLinkify(description)).executeInParallel();

		// check if the user has subscribed to a channel... if he has, then change the state of
		// the subscribe button
		new CheckIfUserSubbedToChannelTask(videoDescSubscribeButton, youTubeVideo.getChannelId()).execute();
	}


	@Override
	public void videoPlaybackStopped() {
		player.stop();
		playerView.setPlayer(null);
		if(!SkyTubeApp.getPreferenceManager().getBoolean(getString(R.string.pref_key_disable_playback_status), false)) {
			PlaybackStatusDb.getPlaybackStatusDb().setVideoPosition(youTubeVideo, player.getCurrentPosition());
		}
	}


	@Override
	public void onDestroy() {
		super.onDestroy();
		// stop the player from playing (when this fragment is going to be destroyed) and clean up
		player.stop();
		player = null;
		playerView.setPlayer(null);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////


	/**
	 * This will handle any gesture swipe event performed by the user on the player view.
	 */
	private class PlayerViewGestureHandler extends PlayerViewGestureDetector {

		private ImageView           indicatorImageView = null;
		private TextView            indicatorTextView = null;
		private RelativeLayout      indicatorView = null;

		private boolean             isControllerVisible = true;
		private VideoBrightness     videoBrightness;
		private float               startVolumePercent = -1.0f;
		private long                startVideoTime = -1;

		/** Enable/Disable video gestures based on user preferences. */
		private final boolean       disableGestures = SkyTubeApp.getPreferenceManager().getBoolean(SkyTubeApp.getStr(R.string.pref_key_disable_screen_gestures), false);

		private static final int    MAX_VIDEO_STEP_TIME = 60 * 1000;


		PlayerViewGestureHandler() {
			super(getContext());

			videoBrightness = new VideoBrightness(getActivity(), disableGestures);
			playerView.setControllerVisibilityListener(visibility -> isControllerVisible = (visibility == View.VISIBLE));
		}


		void initView(View view) {
			indicatorView = view.findViewById(R.id.indicatorView);
			indicatorImageView = view.findViewById(R.id.indicatorImageView);
			indicatorTextView = view.findViewById(R.id.indicatorTextView);
		}


		@Override
		public void onCommentsGesture() {
			commentsDrawer.animateOpen();
		}


		@Override
		public void onVideoDescriptionGesture() {
			videoDescriptionDrawer.animateOpen();
		}


		@Override
		public void onDoubleTap() {
			// if the user is playing a video...
			if (player.getPlayWhenReady()) {
				// pause video
				pause();
			} else {
				// play video
				player.setPlayWhenReady(true);
				player.getPlaybackState();
			}

			playerView.hideController();
		}


		@Override
		public boolean onSingleTap() {
			return showOrHideHud();
		}


		/**
		 * Hide or display the HUD depending if the HUD is currently visible or not.
		 */
		private boolean showOrHideHud() {
			if (commentsDrawer.isOpened()) {
				commentsDrawer.animateClose();
				return !isControllerVisible;
			}

			if (videoDescriptionDrawer.isOpened()) {
				videoDescriptionDrawer.animateClose();
				return !isControllerVisible;
			}

			if (isControllerVisible) {
				playerView.hideController();
				hideNavigationBar();
			} else {
				playerView.showController();
				showNavigationBar();
			}

			return false;
		}


		@Override
		public void onGestureDone() {
			videoBrightness.onGestureDone();
			startVolumePercent = -1.0f;
			startVideoTime = -1;
			hideIndicator();
		}


		@Override
		public void adjustBrightness(double adjustPercent) {
			if (disableGestures) {
				return;
			}

			// adjust the video's brightness
			videoBrightness.setVideoBrightness(adjustPercent, getActivity());

			// set indicator
			indicatorImageView.setImageResource(R.drawable.ic_brightness);
			indicatorTextView.setText(videoBrightness.getBrightnessString());

			// Show indicator. It will be hidden once onGestureDone will be called
			showIndicator();
		}


		@Override
		public void adjustVolumeLevel(double adjustPercent) {
			if (disableGestures) {
				return;
			}

			// We are setting volume percent to a value that should be from -1.0 to 1.0. We need to limit it here for these values first
			if (adjustPercent < -1.0f) {
				adjustPercent = -1.0f;
			} else if (adjustPercent > 1.0f) {
				adjustPercent = 1.0f;
			}

			AudioManager audioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
			final int STREAM = AudioManager.STREAM_MUSIC;

			// Max volume will return INDEX of volume not the percent. For example, on my device it is 15
			int maxVolume = audioManager.getStreamMaxVolume(STREAM);
			if (maxVolume == 0) return;

			if (startVolumePercent < 0) {
				// We are getting actual volume index (NOT volume but index). It will be >= 0.
				int curVolume = audioManager.getStreamVolume(STREAM);
				// And counting percents of maximum volume we have now
				startVolumePercent = curVolume * 1.0f / maxVolume;
			}
			// Should be >= 0 and <= 1
			double targetPercent = startVolumePercent + adjustPercent;
			if (targetPercent > 1.0f) {
				targetPercent = 1.0f;
			} else if (targetPercent < 0) {
				targetPercent = 0;
			}

			// Calculating index. Test values are 15 * 0.12 = 1 ( because it's int)
			int index = (int) (maxVolume * targetPercent);
			if (index > maxVolume) {
				index = maxVolume;
			} else if (index < 0) {
				index = 0;
			}
			audioManager.setStreamVolume(STREAM, index, 0);

			indicatorImageView.setImageResource(R.drawable.ic_volume);
			indicatorTextView.setText(index * 100 / maxVolume + "%");

			// Show indicator. It will be hidden once onGestureDone will be called
			showIndicator();
		}

		@Override
		public void adjustVideoPosition(double adjustPercent, boolean forwardDirection) {
			if (disableGestures) {
				return;
			}

			long totalTime = player.getDuration();

			if (adjustPercent < -1.0f) {
				adjustPercent = -1.0f;
			} else if (adjustPercent > 1.0f) {
				adjustPercent = 1.0f;
			}

			if (startVideoTime < 0) {
				startVideoTime = player.getCurrentPosition();
			}
			// adjustPercent: value from -1 to 1.
			double positiveAdjustPercent = Math.max(adjustPercent, -adjustPercent);
			// End of line makes seek speed not linear
			long targetTime = startVideoTime + (long) (MAX_VIDEO_STEP_TIME * adjustPercent * (positiveAdjustPercent / 0.1));
			if (targetTime > totalTime) {
				targetTime = totalTime;
			}
			if (targetTime < 0) {
				targetTime = 0;
			}

			String targetTimeString = formatDuration(targetTime / 1000);

			if (forwardDirection) {
				indicatorImageView.setImageResource(R.drawable.ic_forward);
				indicatorTextView.setText(targetTimeString);
			} else {
				indicatorImageView.setImageResource(R.drawable.ic_rewind);
				indicatorTextView.setText(targetTimeString);
			}

			showIndicator();

			player.seekTo(targetTime);
		}


		@Override
		public Rect getPlayerViewRect() {
			return new Rect(playerView.getLeft(), playerView.getTop(), playerView.getRight(), playerView.getBottom());
		}


		private void showIndicator() {
			indicatorView.setVisibility(View.VISIBLE);
		}


		private void hideIndicator() {
			indicatorView.setVisibility(View.GONE);
		}


		/**
		 * Returns a (localized) string for the given duration (in seconds).
		 *
		 * @param duration
		 * @return  a (localized) string for the given duration (in seconds).
		 */
		private String formatDuration(long duration) {
			long    h = duration / 3600;
			long    m = (duration - h * 3600) / 60;
			long    s = duration - (h * 3600 + m * 60);
			String  durationValue;

			if (h == 0) {
				durationValue = String.format(Locale.getDefault(),"%1$02d:%2$02d", m, s);
			} else {
				durationValue = String.format(Locale.getDefault(),"%1$d:%2$02d:%3$02d", h, m, s);
			}

			return durationValue;
		}

	}



	////////////////////////////////////////////////////////////////////////////////////////////////


	/**
	 * Adjust video's brightness.  Once the brightness is adjust, it is saved in the preferences to
	 * be used when a new video is played.
	 */
	private static class VideoBrightness {

		/** Current video brightness. */
		private float   brightness;
		/** Initial video brightness. */
		private float   initialBrightness;
		private final boolean disableGestures;

		private static final String BRIGHTNESS_LEVEL_PREF = SkyTubeApp.getStr(R.string.pref_key_brightness_level);


		/**
		 * Constructor:  load the previously saved video brightness from the preference and set it.
		 *
		 * @param activity  Activity.
		 */
		public VideoBrightness(final Activity activity, final boolean disableGestures) {
			loadBrightnessFromPreference();
			initialBrightness = brightness;
			this.disableGestures = disableGestures;

			setVideoBrightness(0, activity);
		}


		/**
		 * Set the video brightness.  Once the video brightness is updated, save it in the preference.
		 *
		 * @param adjustPercent Percentage.
		 * @param activity      Activity.
		 */
		public void setVideoBrightness(double adjustPercent, final Activity activity) {
			if (disableGestures) {
				return;
			}

			// We are setting brightness percent to a value that should be from -1.0 to 1.0. We need to limit it here for these values first
			if (adjustPercent < -1.0f) {
				adjustPercent = -1.0f;
			} else if (adjustPercent > 1.0f) {
				adjustPercent = 1.0f;
			}

			// set the brightness instance variable
			setBrightness(initialBrightness + (float) adjustPercent);
			// adjust the video brightness as per this.brightness
			adjustVideoBrightness(activity);
			// save brightness to the preference
			saveBrightnessToPreference();
		}


		/**
		 * Adjust the video brightness.
		 *
		 * @param activity  Current activity.
		 */
		private void adjustVideoBrightness(final Activity activity) {
			WindowManager.LayoutParams lp = activity.getWindow().getAttributes();
			lp.screenBrightness = brightness;
			activity.getWindow().setAttributes(lp);
		}


		/**
		 * Saves {@link #brightness} to preference.
		 */
		private void saveBrightnessToPreference() {
			SharedPreferences.Editor editor = SkyTubeApp.getPreferenceManager().edit();
			editor.putFloat(BRIGHTNESS_LEVEL_PREF, brightness);
			editor.apply();
			Logger.d(this, "BRIGHTNESS: %f", brightness);
		}


		/**
		 * Loads the brightness from preference and set the {@link #brightness} instance variable.
		 */
		private void loadBrightnessFromPreference() {
			final float brightnessPref = SkyTubeApp.getPreferenceManager().getFloat(BRIGHTNESS_LEVEL_PREF, 1);
			setBrightness(brightnessPref);
		}


		/**
		 * Set the {@link #brightness} instance variable.
		 *
		 * @param brightness    Brightness (from 0.0 to 1.0).
		 */
		private void setBrightness(float brightness) {
			if (brightness < 0) {
				brightness = 0;
			} else if (brightness > 1) {
				brightness = 1;
			}

			this.brightness = brightness;
		}


		/**
		 * @return Brightness as string:  e.g. "21%"
		 */
		public String getBrightnessString() {
			return ((int) (brightness * 100)) + "%";
		}


		/**
		 * To be called once the swipe gesture is done/completed.
		 */
		public void onGestureDone() {
			initialBrightness = brightness;
		}

	}

	@Override
	public YouTubeVideo getYouTubeVideo() {
		return youTubeVideo;
	}

	@Override
	public int getCurrentVideoPosition() {
		return (int)player.getCurrentPosition();
	}

	@Override
	public void pause() {
		player.setPlayWhenReady(false);
	}
}
