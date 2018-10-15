package free.rm.skytube.gui.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.io.File;
import java.util.Locale;

import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.GetVideoDetailsTask;
import free.rm.skytube.businessobjects.Logger;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannelInterface;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;
import free.rm.skytube.businessobjects.YouTube.Tasks.GetVideoDescriptionTask;
import free.rm.skytube.businessobjects.YouTube.Tasks.GetYouTubeChannelInfoTask;
import free.rm.skytube.businessobjects.YouTube.VideoStream.StreamMetaData;
import free.rm.skytube.businessobjects.db.DownloadedVideosDb;
import free.rm.skytube.businessobjects.db.PlaybackStatusDb;
import free.rm.skytube.businessobjects.db.Tasks.CheckIfUserSubbedToChannelTask;
import free.rm.skytube.businessobjects.db.Tasks.IsVideoBookmarkedTask;
import free.rm.skytube.businessobjects.interfaces.GetDesiredStreamListener;
import free.rm.skytube.businessobjects.interfaces.YouTubePlayerActivityListener;
import free.rm.skytube.businessobjects.interfaces.YouTubePlayerFragmentInterface;
import free.rm.skytube.gui.activities.MainActivity;
import free.rm.skytube.gui.activities.ThumbnailViewerActivity;
import free.rm.skytube.gui.businessobjects.MediaControllerEx;
import free.rm.skytube.gui.businessobjects.MobileNetworkWarningDialog;
import free.rm.skytube.gui.businessobjects.OnSwipeTouchListener;
import free.rm.skytube.gui.businessobjects.ResumeVideoTask;
import free.rm.skytube.gui.businessobjects.YouTubeVideoListener;
import free.rm.skytube.gui.businessobjects.adapters.CommentsAdapter;
import free.rm.skytube.gui.businessobjects.fragments.ImmersiveModeFragment;
import free.rm.skytube.gui.businessobjects.views.ClickableLinksTextView;
import free.rm.skytube.gui.businessobjects.views.SubscribeButton;
import hollowsoft.slidingdrawer.OnDrawerOpenListener;
import hollowsoft.slidingdrawer.SlidingDrawer;

import static free.rm.skytube.gui.activities.YouTubePlayerActivity.YOUTUBE_VIDEO_OBJ;

/**
 * A fragment that holds a standalone YouTube player.
 */
public class YouTubePlayerV1Fragment extends ImmersiveModeFragment implements MediaPlayer.OnPreparedListener, YouTubeVideoListener, YouTubePlayerFragmentInterface {

	private YouTubeVideo		    youTubeVideo = null;
	private YouTubeChannel		    youTubeChannel = null;

	private VideoView			    videoView = null;
	/** The current video position (i.e. play time). */
	private int					    videoCurrentPosition = 0;
	private MediaControllerEx	    mediaController = null;

	private TextView			    videoDescTitleTextView = null;
	private ImageView			    videoDescChannelThumbnailImageView = null;
	private TextView			    videoDescChannelTextView = null;
	private SubscribeButton		    videoDescSubscribeButton = null;
	private TextView			    videoDescViewsTextView = null;
	private ProgressBar			    videoDescLikesBar = null;
	private TextView			    videoDescLikesTextView = null;
	private TextView			    videoDescDislikesTextView = null;
	private View                    videoDescRatingsDisabledTextView = null;
	private TextView			    videoDescPublishDateTextView = null;
	private ClickableLinksTextView  videoDescriptionTextView = null;
	private RelativeLayout          voidView = null;
	private ImageView               indicatorImageView = null;
	private TextView                indicatorTextView = null;
	private RelativeLayout          indicatorView = null;
	private View				    loadingVideoView = null;

	private SlidingDrawer		    videoDescriptionDrawer = null;
	private View                    videoDescriptionDrawerIconView = null;
	private SlidingDrawer		    commentsDrawer = null;
	private View                    commentsDrawerIconView = null;
	private View				    commentsProgressBar = null,
									noVideoCommentsView = null;
	private CommentsAdapter		    commentsAdapter = null;
	private ExpandableListView	    commentsExpandableListView = null;

	private Menu                    menu = null;
	private YouTubePlayerActivityListener listener = null;

	private Handler                 hideHudTimerHandler = null;
	private Handler                 hideVideoDescAndCommentsIconsTimerHandler = null;

	private float                   startBrightness = -1.0f;
	private float                   startVolumePercent  = -1.0f;
	private int                     startVideoTime = -1;

	/** Timeout (in milliseconds) before the HUD (i.e. media controller + action/title bar) is hidden. */
	private static final int HUD_VISIBILITY_TIMEOUT = 5000;
	/** Timeout (in milliseconds) before the navigation bar is hidden (which will occur only after
	 * the HUD is hidden). */
	private static final int NAVBAR_VISIBILITY_TIMEOUT = 500;
	private static final String VIDEO_CURRENT_POSITION = "YouTubePlayerV1Fragment.VideoCurrentPosition";
	private static final String TAG = YouTubePlayerV1Fragment.class.getSimpleName();

	private static final int MAX_VIDEO_STEP_TIME = 60 * 1000;
	private static final int MAX_BRIGHTNESS = 100;


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// hide the navigation bar
		hideNavigationBar();

		// inflate the layout for this fragment
		View view = inflater.inflate(R.layout.fragment_youtube_player_v1, container, false);

		// indicate that this fragment has an action bar menu
		setHasOptionsMenu(true);

		if (savedInstanceState != null)
			videoCurrentPosition = savedInstanceState.getInt(VIDEO_CURRENT_POSITION, 0);

		if (youTubeVideo == null) {
			// initialise the views
			initViews(view);

			// hide action bar
			getSupportActionBar().hide();

			// get which video we need to play...
			Bundle bundle = getActivity().getIntent().getExtras();
			if (bundle != null  &&  bundle.getSerializable(YOUTUBE_VIDEO_OBJ) != null) {
				// ... either the video details are passed through the previous activity
				youTubeVideo = (YouTubeVideo) bundle.getSerializable(YOUTUBE_VIDEO_OBJ);
				setUpHUDAndPlayVideo();

				getVideoInfoTasks();
			} else {
				// ... or the video URL is passed to SkyTube via another Android app
				new GetVideoDetailsTask(getUrlFromIntent(getActivity().getIntent()), this).executeInParallel();
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
	 * Initialise the views.
	 *
	 * @param view Fragment view.
	 */
	private void initViews(View view) {
		loadingVideoView = view.findViewById(R.id.loadingVideoView);

		videoView = view.findViewById(R.id.video_view);
		// videoView should log any errors
		videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
			@Override
			public boolean onError(MediaPlayer mp, int what, int extra) {
				String msg = String.format(Locale.getDefault(),
						"Error has occurred while playing video, url='%s', what=%d, extra=%d",
						youTubeVideo != null ? youTubeVideo.getVideoUrl() : "null",
						what,
						extra);
				Log.e(TAG, msg);
				return false;
			}
		});

		// play the video once its loaded
		videoView.setOnPreparedListener(this);

		// setup the media controller (will control the video playing/pausing)
		mediaController = new MediaControllerEx(getActivity(), videoView, this);
		// ensure that the mediaController is always above the NavBar (given that the NavBar can be
		// in immersive mode)
		mediaController.setPadding(0, 0, 0, getNavBarHeightInPixels());

		voidView = view.findViewById(R.id.void_view);
		indicatorView = view.findViewById(R.id.indicatorView);
		indicatorImageView = view.findViewById(R.id.indicatorImageView);
		indicatorTextView = view.findViewById(R.id.indicatorTextView);
		// detect if user's swipes motions and taps...
		voidView.setOnTouchListener(new OnSwipeTouchListener(getActivity()) {

			/** Enable/Disable video gestures based on user preferences. */
			private final boolean disableGestures = SkyTubeApp.getPreferenceManager().getBoolean(SkyTubeApp.getStr(R.string.pref_key_disable_screen_gestures), false);

			@Override
			public boolean onSwipeLeft() {
				commentsDrawer.animateOpen();
				return true;
			}

			@Override
			public boolean onSwipeTop() {
				videoDescriptionDrawer.animateOpen();
				return true;
			}

			@Override
			public boolean onDoubleTap() {
				if (videoView.isPlaying()) {
					videoView.pause();
				} else {
					videoView.start();
				}
				return true;
			}

			@Override
			public boolean onSingleTap() {
				showOrHideHud();
				return true;
			}

			@Override
			public void onGestureDone() {
				startBrightness = -1.0f;
				startVolumePercent = -1.0f;
				startVideoTime = -1;
				hideIndicator();
			}

			@Override
			public void adjustBrightness(double adjustPercent) {
				if (disableGestures) {
					return;
				}

				// We are setting brightness percent to a value that should be from -1.0 to 1.0. We need to limit it here for these values first
				if (adjustPercent < -1.0f) {
					adjustPercent = -1.0f;
				} else if (adjustPercent > 1.0f) {
					adjustPercent = 1.0f;
				}

				WindowManager.LayoutParams lp = getActivity().getWindow().getAttributes();
				if (startBrightness < 0) {
					startBrightness = lp.screenBrightness;
				}
				// We are getting a final brightness value when summing current brightness and the percent we got from swipe action. Should be >= 0 and <= 1
				float targetBrightness = (float) (startBrightness + adjustPercent * 1.0f);
				if (targetBrightness <= 0.0f) {
					targetBrightness = 0.0f;
				} else if (targetBrightness >= 1.0f) {
					targetBrightness = 1.0f;
				}
				lp.screenBrightness = targetBrightness;
				getActivity().getWindow().setAttributes(lp);

				indicatorImageView.setImageResource(R.drawable.ic_brightness);
				indicatorTextView.setText((int) (targetBrightness * MAX_BRIGHTNESS) + "%");

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

				AudioManager audioManager = (AudioManager) getContext()
						.getSystemService(Context.AUDIO_SERVICE);
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

				if (adjustPercent < -1.0f) {
					adjustPercent = -1.0f;
				} else if (adjustPercent > 1.0f) {
					adjustPercent = 1.0f;
				}

				int totalTime = videoView.getDuration();

				if (startVideoTime < 0) {
					startVideoTime = videoView.getCurrentPosition();
				}
				// adjustPercent: value from -1 to 1.
                double positiveAdjustPercent = Math.max(adjustPercent,-adjustPercent);
				// End of line makes seek speed not linear
				int targetTime = startVideoTime + (int) (MAX_VIDEO_STEP_TIME * adjustPercent * (positiveAdjustPercent / 0.1));
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

				videoView.seekTo(targetTime);
			}

			@Override
			public Rect viewRect() {
				return new Rect(voidView.getLeft(), voidView.getTop(), voidView.getRight() , voidView.getBottom());
			}
		});

		videoDescriptionDrawer = view.findViewById(R.id.des_drawer);
		videoDescriptionDrawerIconView = view.findViewById(R.id.video_desc_icon_image_view);
		videoDescTitleTextView = view.findViewById(R.id.video_desc_title);
		videoDescChannelThumbnailImageView = view.findViewById(R.id.video_desc_channel_thumbnail_image_view);
		videoDescChannelThumbnailImageView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (youTubeChannel != null) {
					Intent i = new Intent(getActivity(), MainActivity.class);
					i.setAction(MainActivity.ACTION_VIEW_CHANNEL);
					i.putExtra(ChannelBrowserFragment.CHANNEL_OBJ, youTubeChannel);
					startActivity(i);
				}
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
		commentsDrawer.setOnDrawerOpenListener(new OnDrawerOpenListener() {
			@Override
			public void onDrawerOpened() {
				if (commentsAdapter == null) {
					commentsAdapter = new CommentsAdapter(getActivity(), youTubeVideo.getId(), commentsExpandableListView, commentsProgressBar, noVideoCommentsView);
				}
			}
		});
		commentsDrawerIconView = view.findViewById(R.id.comments_icon_image_view);
	}


	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(VIDEO_CURRENT_POSITION, videoCurrentPosition);
	}


	/**
	 * Will asynchronously retrieve additional video information such as channel, avatar ...etc
	 */
	private void getVideoInfoTasks() {
		// get Channel info (e.g. avatar...etc) task
		new GetYouTubeChannelInfoTask(getContext(), new YouTubeChannelInterface() {
			@Override
			public void onGetYouTubeChannel(YouTubeChannel youTubeChannel) {
				YouTubePlayerV1Fragment.this.youTubeChannel = youTubeChannel;

				videoDescSubscribeButton.setChannel(YouTubePlayerV1Fragment.this.youTubeChannel);
				if (youTubeChannel != null) {
					if(getActivity() != null)
						Glide.with(getActivity())
										.load(youTubeChannel.getThumbnailNormalUrl())
										.apply(new RequestOptions().placeholder(R.drawable.channel_thumbnail_default))
										.into(videoDescChannelThumbnailImageView);
				}
			}
		}).executeInParallel(youTubeVideo.getChannelId());

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

		new ResumeVideoTask(getContext(), youTubeVideo, new ResumeVideoTask.Callback() {
			@Override
			public void loadVideo(int position) {
				videoCurrentPosition = position;
				YouTubePlayerV1Fragment.this.loadVideo();
			}
		}).ask();
	}


	@Override
	public void onPrepared(MediaPlayer mediaPlayer) {
		loadingVideoView.setVisibility(View.GONE);
		videoView.seekTo(videoCurrentPosition);
		videoView.start();

		showHud();
	}


	@Override
	public void onPause() {
		if (videoView != null && videoView.isPlaying()) {
			videoCurrentPosition = videoView.getCurrentPosition();
		}

		saveCurrentBrightness();
		super.onPause();
	}


	@Override
	public void onResume() {
		super.onResume();

		setupUserPrefs();
	}


	// We can also add volume level or something in the future.
	private void setupUserPrefs() {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
		float brightnessLevel = sp.getFloat(getString(R.string.pref_key_brightness_level), -1.0f);
		setBrightness(brightnessLevel);
	}


	private void saveCurrentBrightness() {
		WindowManager.LayoutParams lp = getActivity().getWindow().getAttributes();
		float brightnessLevel = lp.screenBrightness;
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
		sp.edit().putFloat(getString(R.string.pref_key_brightness_level), brightnessLevel).apply();
		Logger.d(this, "BRIGHTNESS: %f", brightnessLevel);
	}

	private void setBrightness(float level) {
		if(level <= 0.0f && level > 1.0f) return;

		WindowManager.LayoutParams lp = getActivity().getWindow().getAttributes();
		lp.screenBrightness = level;
		getActivity().getWindow().setAttributes(lp);
	}



	/**
	 * @return True if the HUD is visible (provided that this Fragment is also visible).
	 */
	private boolean isHudVisible() {
		return isVisible()  &&  (mediaController.isShowing()  ||  getSupportActionBar().isShowing());
	}



	/**
	 * Hide or display the HUD depending if the HUD is currently visible or not.
	 */
	private void showOrHideHud() {
		if (commentsDrawer.isOpened()) {
			commentsDrawer.animateClose();
		} else if (videoDescriptionDrawer.isOpened()) {
			videoDescriptionDrawer.animateClose();
		} else if (isHudVisible()) {
			hideHud();
		} else {
			showHud();
		}
	}



	/**
	 * Show the HUD (head-up display), i.e. the Action Bar and Media Controller.
	 */
	private void showHud() {
		if (!isHudVisible()) {
			getSupportActionBar().show();
			getSupportActionBar().setTitle(youTubeVideo != null ? youTubeVideo.getTitle() : "");
			mediaController.show(0);

			videoDescriptionDrawer.close();
			videoDescriptionDrawerIconView.setVisibility(View.INVISIBLE);
			commentsDrawer.close();
			commentsDrawerIconView.setVisibility(View.INVISIBLE);

			// hide UI after a certain timeout (defined in HUD_VISIBILITY_TIMEOUT)
			hideHudTimerHandler = new Handler();
			hideHudTimerHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					hideHud();
					hideHudTimerHandler = null;
				}
			}, HUD_VISIBILITY_TIMEOUT);
		}
	}


	/**
	 * Hide the HUD.
	 */
	private void hideHud() {
		if (isHudVisible()) {
			getSupportActionBar().hide();
			mediaController.hideController();

			// Due to the IMMERSIVE mode experience (i.e. comments/desc icons are hidden by default):
			// Hide the navigation bar.  Due to Android pre-defined mechanisms, the nav bar can
			// only be hidden after all animation have been rendered (e.g. mediaController is
			// fully closed).  As a result, a delay is needed in order to explicitly hide the
			// nav bar.
			hideVideoDescAndCommentsIconsTimerHandler = new Handler();
			hideVideoDescAndCommentsIconsTimerHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					hideNavigationBar();
					hideVideoDescAndCommentsIconsTimerHandler = null;
				}
			}, NAVBAR_VISIBILITY_TIMEOUT);

			// If there is a hideHudTimerHandler running, then cancel it (stop if from running).  This way,
			// if the HUD was hidden on the 5th second, and the user reopens the HUD, this code will
			// prevent the HUD to re-disappear 2 seconds after it was displayed (assuming that
			// HUD_VISIBILITY_TIMEOUT = 5 seconds).
			if (hideHudTimerHandler != null) {
				hideHudTimerHandler.removeCallbacksAndMessages(null);
				hideHudTimerHandler = null;
			}
		}
	}


	private void showIndicator() {
		indicatorView.setVisibility(View.VISIBLE);
	}


	private void hideIndicator() {
		indicatorView.setVisibility(View.GONE);
	}


	// Returns a (localized) string for the given duration (in seconds).
	private String formatDuration(int duration) {
		int h = duration / 3600;
		int m = (duration - h * 3600) / 60;
		int s = duration - (h * 3600 + m * 60);
		String durationValue;
		if (h == 0) {
			durationValue = String.format(Locale.getDefault(),"%1$02d:%2$02d", m, s);
		} else {
			durationValue = String.format(Locale.getDefault(),"%1$d:%2$02d:%3$02d", h, m, s);
		}
		return durationValue;
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
				loadVideo();
				return true;

			case R.id.menu_open_video_with:
				youTubeVideo.playVideoExternally(getContext());
				videoView.pause();
				return true;

			case R.id.share:
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
				final boolean warningDialogDisplayed = new MobileNetworkWarningDialog(getContext())
						.onPositive(new MaterialDialog.SingleButtonCallback() {
							@Override
							public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
								youTubeVideo.downloadVideo(getContext());
							}
						})
						.showAndGetStatus(MobileNetworkWarningDialog.ActionType.DOWNLOAD_VIDEO);

				if (!warningDialogDisplayed) {
					youTubeVideo.downloadVideo(getContext());
				}
				return true;

            case R.id.block_channel:
	            youTubeChannel.blockChannel();

			default:
				return super.onOptionsItemSelected(item);
		}
	}


	/**
	 * Loads the video specified in {@link #youTubeVideo}.
	 */
	private void loadVideo() {
		loadVideo(false);
	}


	/**
	 * Loads the video specified in {@link #youTubeVideo}.
	 *
	 * @param skipMobileNetworkWarning Set to true to skip the warning displayed when the user is
	 *                                 using mobile network data (i.e. 4g).
	 */
	private void loadVideo(boolean skipMobileNetworkWarning) {
		boolean mobileNetworkWarningDialogDisplayed = false;

		// if the user is using mobile network (i.e. 4g), then warn him
		if (!skipMobileNetworkWarning) {
			mobileNetworkWarningDialogDisplayed = new MobileNetworkWarningDialog(getActivity())
					.onPositive(new MaterialDialog.SingleButtonCallback() {
						@Override
						public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
							loadVideo(true);
						}
					})
					.onNegative(new MaterialDialog.SingleButtonCallback() {
						@Override
						public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
							closeActivity();
						}
					})
					.showAndGetStatus(MobileNetworkWarningDialog.ActionType.STREAM_VIDEO);
		}

		if (!mobileNetworkWarningDialogDisplayed) {
			// if the video is NOT live
			if (!youTubeVideo.isLiveStream()) {
				videoView.pause();
				videoView.stopPlayback();
				loadingVideoView.setVisibility(View.VISIBLE);
				if (youTubeVideo.isDownloaded()) {
					Uri uri = youTubeVideo.getFileUri();
					File file = new File(uri.getPath());
					// If the file for this video has gone missing, remove it from the Database and then play remotely.
					if (!file.exists()) {
						DownloadedVideosDb.getVideoDownloadsDb().remove(youTubeVideo);
						Toast.makeText(getContext(),
								getContext().getString(R.string.playing_video_file_missing),
								Toast.LENGTH_LONG).show();
						loadVideo();
					} else {
						Logger.i(YouTubePlayerV1Fragment.this, ">> PLAYING LOCALLY: %s", uri);
						videoView.setVideoURI(uri);
					}
				} else {
					youTubeVideo.getDesiredStream(new GetDesiredStreamListener() {
						@Override
						public void onGetDesiredStream(StreamMetaData desiredStream) {
							// play the video
							Logger.i(YouTubePlayerV1Fragment.this, ">> PLAYING: %s", desiredStream.getUri());
							videoView.setVideoURI(desiredStream.getUri());
						}

						@Override
						public void onGetDesiredStreamError(String errorMessage) {
							if (errorMessage != null) {
								new AlertDialog.Builder(getContext())
										.setMessage(errorMessage)
										.setTitle(R.string.error_video_play)
										.setCancelable(false)
										.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
											@Override
											public void onClick(DialogInterface dialog, int which) {
												getActivity().finish();
											}
										})
										.show();
							}
						}
					});
				}

				// get the video description
				new GetVideoDescriptionTask(youTubeVideo, new GetVideoDescriptionTask.GetVideoDescriptionTaskListener() {
					@Override
					public void onFinished(String description) {
						videoDescriptionTextView.setTextAndLinkify(description);
					}
				}).executeInParallel();
			} else {
				// video is live:  ask the user if he wants to play the video using an other app
				new AlertDialog.Builder(getContext())
						.setMessage(R.string.warning_live_video)
						.setTitle(R.string.error_video_play)
						.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								closeActivity();
							}
						})
						.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								youTubeVideo.playVideoExternally(getContext());
								closeActivity();
							}
						})
						.show();
			}
		}
	}

	////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * When a video url is clicked on, attempt to open and play it.
	 * @param videoUrl
	 * @param youTubeVideo
	 */
	@Override
	public void onYouTubeVideo(String videoUrl, YouTubeVideo youTubeVideo) {
		if (youTubeVideo == null) {
			// invalid URL error (i.e. we are unable to decode the URL)
			String err = String.format(getString(R.string.error_invalid_url), videoUrl);
			Toast.makeText(getActivity(), err, Toast.LENGTH_LONG).show();

			// log error
			Log.e(TAG, err);

			// close the video player activity
			closeActivity();
		} else {
			YouTubePlayerV1Fragment.this.youTubeVideo = youTubeVideo;

			// setup the HUD and play the video
			setUpHUDAndPlayVideo();

			getVideoInfoTasks();

			// will now check if the video is bookmarked or not (and then update the menu
			// accordingly)
			new IsVideoBookmarkedTask(youTubeVideo, menu).executeInParallel();
		}
	}

	////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * The video URL is passed to SkyTube via another Android app (i.e. via an intent).
	 *
	 * @return The URL of the YouTube video the user wants to play.
	 */
	public static String getUrlFromIntent(final Intent intent) {
		String url = null;

		if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
			url = intent.getData().toString();
		}

		return url;
	}

	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public void videoPlaybackStopped() {
		int position = videoView.getCurrentPosition();
		videoView.pause();
		videoView.stopPlayback();
		if (!SkyTubeApp.getPreferenceManager().getBoolean(getString(R.string.pref_key_disable_playback_status), false)) {
			PlaybackStatusDb.getVideoDownloadsDb().setVideoPosition(youTubeVideo, position);
		}
	}

	@Override
	public YouTubeVideo getYouTubeVideo() {
		return youTubeVideo;
	}

	@Override
	public int getCurrentVideoPosition() {
		return videoView.getCurrentPosition();
	}

	@Override
	public void pause() {
		videoView.pause();
	}
}
