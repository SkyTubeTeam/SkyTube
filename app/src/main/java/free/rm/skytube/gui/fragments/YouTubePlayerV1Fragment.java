package free.rm.skytube.gui.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseExpandableListAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.media.AudioManagerCompat;
import androidx.preference.PreferenceManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.stream.StreamInfo;

import java.io.File;
import java.util.Locale;

import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.app.StreamSelectionPolicy;
import free.rm.skytube.app.Utils;
import free.rm.skytube.app.enums.Policy;
import free.rm.skytube.businessobjects.Logger;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;
import free.rm.skytube.businessobjects.YouTube.YouTubeTasks;
import free.rm.skytube.businessobjects.YouTube.newpipe.ContentId;
import free.rm.skytube.businessobjects.db.DatabaseTasks;
import free.rm.skytube.businessobjects.db.DownloadedVideosDb;
import free.rm.skytube.businessobjects.db.PlaybackStatusDb;
import free.rm.skytube.businessobjects.interfaces.GetDesiredStreamListener;
import free.rm.skytube.businessobjects.interfaces.YouTubePlayerActivityListener;
import free.rm.skytube.businessobjects.interfaces.YouTubePlayerFragmentInterface;
import free.rm.skytube.databinding.FragmentYoutubePlayerV1Binding;
import free.rm.skytube.databinding.VideoDescriptionBinding;
import free.rm.skytube.gui.activities.ThumbnailViewerActivity;
import free.rm.skytube.gui.businessobjects.MediaControllerEx;
import free.rm.skytube.gui.businessobjects.MobileNetworkWarningDialog;
import free.rm.skytube.gui.businessobjects.OnSwipeTouchListener;
import free.rm.skytube.gui.businessobjects.ResumeVideoTask;
import free.rm.skytube.gui.businessobjects.SkyTubeMaterialDialog;
import free.rm.skytube.gui.businessobjects.adapters.CommentsAdapter;
import free.rm.skytube.gui.businessobjects.fragments.ImmersiveModeFragment;
import free.rm.skytube.gui.businessobjects.views.Linker;
import free.rm.skytube.businessobjects.interfaces.PlaybackStateListener;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

import static free.rm.skytube.gui.activities.YouTubePlayerActivity.YOUTUBE_VIDEO_OBJ;

/**
 * A fragment that holds a standalone YouTube player.
 */
public class YouTubePlayerV1Fragment extends ImmersiveModeFragment implements MediaPlayer.OnPreparedListener, YouTubePlayerFragmentInterface {


	/** Timeout (in milliseconds) before the HUD (i.e. media controller + action/title bar) is hidden. */
	private static final int HUD_VISIBILITY_TIMEOUT = 5000;
	/** Timeout (in milliseconds) before the navigation bar is hidden (which will occur only after
	 * the HUD is hidden). */
	private static final int NAVBAR_VISIBILITY_TIMEOUT = 500;
	private static final String VIDEO_CURRENT_POSITION = "YouTubePlayerV1Fragment.VideoCurrentPosition";
	private static final String TAG = YouTubePlayerV1Fragment.class.getSimpleName();

	private static final int MAX_VIDEO_STEP_TIME = 60 * 1000;
	private static final int MAX_BRIGHTNESS = 100;

	private PlaybackStateListener playbackStateListener;

	private YouTubeVideo youTubeVideo = null;
	private YouTubeChannel youTubeChannel = null;

	private FragmentYoutubePlayerV1Binding fragmentBinding;
	private VideoDescriptionBinding videoDescriptionBinding;

	/** The current video position (i.e. play time). */
	private int videoCurrentPosition = 0;
	private MediaControllerEx mediaController = null;

	private BaseExpandableListAdapter commentsAdapter = null;

	private Menu menu = null;
	private YouTubePlayerActivityListener listener = null;

	private Handler hideHudTimerHandler = null;
	private Handler hideVideoDescAndCommentsIconsTimerHandler = null;

	private float startBrightness = -1.0f;
	private float startVolumePercent  = -1.0f;
	private int startVideoTime = -1;

	private final CompositeDisposable compositeDisposable = new CompositeDisposable();

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// hide the navigation bar
		hideNavigationBar();

		// inflate the layout for this fragment
		fragmentBinding = FragmentYoutubePlayerV1Binding.inflate(inflater, container, false);
		videoDescriptionBinding = fragmentBinding.desContent;

		// indicate that this fragment has an action bar menu
		setHasOptionsMenu(true);

		if (savedInstanceState != null)
			videoCurrentPosition = savedInstanceState.getInt(VIDEO_CURRENT_POSITION, 0);

		if (youTubeVideo == null) {
			// initialise the views
			initViews();

			// hide action bar
			getSupportActionBar().hide();

			// get which video we need to play...
			Bundle bundle = requireActivity().getIntent().getExtras();
			if (bundle != null  &&  bundle.getSerializable(YOUTUBE_VIDEO_OBJ) != null) {
				// ... either the video details are passed through the previous activity
				youTubeVideo = (YouTubeVideo) bundle.getSerializable(YOUTUBE_VIDEO_OBJ);
				setUpHUDAndPlayVideo();

				getVideoInfoTasks();
			} else {
				// ... or the video URL is passed to SkyTube via another Android app
				final ContentId contentId = SkyTubeApp.getUrlFromIntent(requireContext(), requireActivity().getIntent());
				Utils.isTrue(contentId.getType() == StreamingService.LinkType.STREAM, "Content is a video:"+contentId);
				compositeDisposable.add(YouTubeTasks.getVideoDetails(requireContext(), contentId)
						.subscribe(youTubeVideo1 -> {
							if (youTubeVideo1 == null) {
								// invalid URL error (i.e. we are unable to decode the URL)
								String err = String.format(getString(R.string.error_invalid_url), contentId.getCanonicalUrl());
								Toast.makeText(getActivity(), err, Toast.LENGTH_LONG).show();

								// log error
								Log.e(TAG, err);

								// close the video player activity
								closeActivity();
							} else {
								YouTubePlayerV1Fragment.this.youTubeVideo = youTubeVideo1;

								// setup the HUD and play the video
								setUpHUDAndPlayVideo();

								getVideoInfoTasks();

								// will now check if the video is bookmarked or not (and then update the menu
								// accordingly)
								compositeDisposable.add(DatabaseTasks.isVideoBookmarked(youTubeVideo.getId(), menu));
							}
						}));
			}
		}

		return fragmentBinding.getRoot();
	}

	@Override
	public void onAttach(@NonNull Context context) {
		super.onAttach(context);
		try {
			Activity activity = (Activity)context;
			listener = (YouTubePlayerActivityListener)activity;
		} catch (ClassCastException e) {
			throw new ClassCastException("YouTubePlayerFragment must be instantiated from an Activity " +
					"that implements YouTubePlayerActivityListener");
		}
	}

	@Override
	public void onDestroy() {
		compositeDisposable.clear();
		videoDescriptionBinding.videoDescSubscribeButton.clearBackgroundTasks();
		fragmentBinding = null;
		videoDescriptionBinding = null;
		super.onDestroy();
	}

	/**
	 * Initialise the views.
	 *
	 */
	private void initViews() {
		// videoView should log any errors
		fragmentBinding.videoView.setOnErrorListener((mp, what, extra) -> {
			String msg = String.format(Locale.getDefault(),
					"Error has occurred while playing video, url='%s', what=%d, extra=%d",
					youTubeVideo != null ? youTubeVideo.getVideoUrl() : "null",
					what,
					extra);
			Logger.e(TAG, msg);
			return false;
		});

		// play the video once its loaded
		fragmentBinding.videoView.setOnPreparedListener(this);

		// setup the media controller (will control the video playing/pausing)
		mediaController = new MediaControllerEx(getActivity(), fragmentBinding.videoView, this);
		// ensure that the mediaController is always above the NavBar (given that the NavBar can be
		// in immersive mode)
		mediaController.setPadding(0, 0, 0, getNavBarHeightInPixels());

		// detect if user's swipes motions and taps...
		fragmentBinding.voidView.setOnTouchListener(new OnSwipeTouchListener(getActivity()) {
			/** Enable/Disable video gestures based on user preferences. */
			private final boolean disableGestures = SkyTubeApp.getPreferenceManager().getBoolean(SkyTubeApp.getStr(R.string.pref_key_disable_screen_gestures), false);

			@Override
			public boolean onSwipeLeft() {
				fragmentBinding.commentsDrawer.animateOpen();
				return true;
			}

			@Override
			public boolean onSwipeTop() {
				fragmentBinding.desDrawer.animateOpen();
				return true;
			}

			@Override
			public boolean onDoubleTap() {
				if (fragmentBinding.videoView.isPlaying()) {
					pause();
				} else {
					play();
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

				fragmentBinding.indicatorImageView.setImageResource(R.drawable.ic_brightness);
				fragmentBinding.indicatorTextView.setText((int) (targetBrightness * MAX_BRIGHTNESS) + "%");

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

				AudioManager audioManager = ContextCompat.getSystemService(requireContext(), AudioManager.class);
				final int STREAM = AudioManager.STREAM_MUSIC;

				// Max volume will return INDEX of volume not the percent. For example, on my device it is 15
				int maxVolume = AudioManagerCompat.getStreamMaxVolume(audioManager, STREAM);
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

				fragmentBinding.indicatorImageView.setImageResource(R.drawable.ic_volume);
				fragmentBinding.indicatorTextView.setText(index * 100 / maxVolume + "%");

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

				int totalTime = fragmentBinding.videoView.getDuration();

				if (startVideoTime < 0) {
					startVideoTime = fragmentBinding.videoView.getCurrentPosition();
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
					fragmentBinding.indicatorImageView.setImageResource(R.drawable.ic_forward);
				} else {
					fragmentBinding.indicatorImageView.setImageResource(R.drawable.ic_rewind);
				}
				fragmentBinding.indicatorTextView.setText(targetTimeString);

				showIndicator();

				fragmentBinding.videoView.seekTo(targetTime);
			}

			@Override
			public Rect viewRect() {
				return new Rect(fragmentBinding.voidView.getLeft(), fragmentBinding.voidView.getTop(),
						fragmentBinding.voidView.getRight(), fragmentBinding.voidView.getBottom());
			}
		});

		videoDescriptionBinding.videoDescChannelThumbnailImageView.setOnClickListener(v -> {
			if (youTubeChannel != null) {
				SkyTubeApp.launchChannel(youTubeChannel, getActivity());
			}
		});

		fragmentBinding.commentsDrawer.setOnDrawerOpenListener(() -> {
			if (commentsAdapter == null && youTubeVideo != null) {
				commentsAdapter = CommentsAdapter.createAdapter(getActivity(), null, youTubeVideo.getId(),
						fragmentBinding.commentsExpandableListView, fragmentBinding.commentsProgressBar,
						fragmentBinding.noVideoCommentsTextView, fragmentBinding.videoCommentsAreDisabled);
			}
		});

		Linker.configure(videoDescriptionBinding.videoDescDescription, null);
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(VIDEO_CURRENT_POSITION, videoCurrentPosition);
	}

	/**
	 * Will asynchronously retrieve additional video information such as channel, avatar ...etc
	 */
	private void getVideoInfoTasks() {
		// get Channel info (e.g. avatar...etc) task
		compositeDisposable.add(DatabaseTasks.getChannelInfo(requireContext(), youTubeVideo.getChannelId(), false)
				.subscribe(youTubeChannel1 -> {
					youTubeChannel = youTubeChannel1;

					videoDescriptionBinding.videoDescSubscribeButton.setChannel(youTubeChannel);
					if (youTubeChannel != null) {
						Glide.with(requireContext())
								.load(youTubeChannel.getThumbnailUrl())
								.apply(new RequestOptions().placeholder(R.drawable.channel_thumbnail_default))
								.into(videoDescriptionBinding.videoDescChannelThumbnailImageView);
					}
				}));
	}

	/**
	 * Will setup the HUD's details according to the contents of {@link #youTubeVideo}.  Then it
	 * will try to load and play the video.
	 */
	private void setUpHUDAndPlayVideo() {
		setupInfoDisplay(youTubeVideo);

		new ResumeVideoTask(requireContext(), youTubeVideo.getId(), position -> {
			videoCurrentPosition = position;
			YouTubePlayerV1Fragment.this.loadVideo();
		}).ask();

	}

	private void setupInfoDisplay(YouTubeVideo video) {
		getSupportActionBar().setTitle(video.getTitle());

		videoDescriptionBinding.videoDescTitle.setText(video.getTitle());
		videoDescriptionBinding.videoDescChannel.setText(video.getChannelName());
		videoDescriptionBinding.videoDescViews.setText(video.getViewsCount());
		videoDescriptionBinding.videoDescPublishDate.setText(video.getPublishDatePretty());

		if (video.getDescription() != null) {
			Linker.setTextAndLinkify(videoDescriptionBinding.videoDescDescription, video.getDescription());
		}

		if (video.isThumbsUpPercentageSet()) {
			videoDescriptionBinding.videoDescLikes.setText(video.getLikeCount());
			videoDescriptionBinding.videoDescDislikes.setText(video.getDislikeCount());
			videoDescriptionBinding.videoDescLikesBar.setProgress(video.getThumbsUpPercentage());

			videoDescriptionBinding.videoDescRatingsDisabled.setVisibility(View.GONE);

			videoDescriptionBinding.videoDescLikes.setVisibility(View.VISIBLE);
			videoDescriptionBinding.videoDescDislikes.setVisibility(View.VISIBLE);
			videoDescriptionBinding.videoDescLikesBar.setVisibility(View.VISIBLE);

		} else {
			videoDescriptionBinding.videoDescLikes.setVisibility(View.GONE);
			videoDescriptionBinding.videoDescDislikes.setVisibility(View.GONE);
			videoDescriptionBinding.videoDescLikesBar.setVisibility(View.GONE);
			videoDescriptionBinding.videoDescRatingsDisabled.setVisibility(View.VISIBLE);
		}
	}

	@Override
	public void onPrepared(MediaPlayer mediaPlayer) {
		fragmentBinding.loadingVideoView.setVisibility(View.GONE);
		fragmentBinding.videoView.seekTo(videoCurrentPosition);
		play();

		showHud();
	}


	@Override
	public void onPause() {
		if (fragmentBinding != null && fragmentBinding.videoView.isPlaying()) {
			videoCurrentPosition = fragmentBinding.videoView.getCurrentPosition();
		}
		saveVideoPosition(videoCurrentPosition);

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
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(requireActivity());
		float brightnessLevel = sp.getFloat(getString(R.string.pref_key_brightness_level), -1.0f);
		setBrightness(brightnessLevel);
	}


	private void saveCurrentBrightness() {
		WindowManager.LayoutParams lp = requireActivity().getWindow().getAttributes();
		float brightnessLevel = lp.screenBrightness;
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
		sp.edit().putFloat(getString(R.string.pref_key_brightness_level), brightnessLevel).apply();
		Logger.d(this, "BRIGHTNESS: %f", brightnessLevel);
	}

	private void setBrightness(float level) {
		if(level <= 0.0f && level > 1.0f) return;

		WindowManager.LayoutParams lp = requireActivity().getWindow().getAttributes();
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
		if (fragmentBinding.commentsDrawer.isOpened()) {
			fragmentBinding.commentsDrawer.animateClose();
		} else if (fragmentBinding.desDrawer.isOpened()) {
			fragmentBinding.desDrawer.animateClose();
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

			fragmentBinding.desDrawer.close();
			fragmentBinding.videoDescIconImageView.setVisibility(View.INVISIBLE);
			fragmentBinding.commentsDrawer.close();
			fragmentBinding.commentsIconImageView.setVisibility(View.INVISIBLE);

			// hide UI after a certain timeout (defined in HUD_VISIBILITY_TIMEOUT)
			hideHudTimerHandler = new Handler();
			hideHudTimerHandler.postDelayed(() -> {
				hideHud();
				hideHudTimerHandler = null;
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
			hideVideoDescAndCommentsIconsTimerHandler.postDelayed(() -> {
				hideNavigationBar();
				hideVideoDescAndCommentsIconsTimerHandler = null;
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
		fragmentBinding.indicatorView.setVisibility(View.VISIBLE);
	}

	private void hideIndicator() {
		fragmentBinding.indicatorView.setVisibility(View.GONE);
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
	public void onPrepareOptionsMenu(@NonNull Menu menu) {
		DatabaseTasks.updateDownloadedVideoMenu(youTubeVideo, menu);
	}

	@Override
	public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
		inflater.inflate(R.menu.menu_youtube_player, menu);

		this.menu = menu;

		listener.onOptionsMenuCreated(menu);

		// Will now check if the video is bookmarked or not (and then update the menu accordingly).
		//
		// youTubeVideo might be null if we have only passed the video URL to this fragment (i.e.
		// the app is still trying to construct youTubeVideo in the background).
		if (youTubeVideo != null) {
			compositeDisposable.add(DatabaseTasks.isVideoBookmarked(youTubeVideo.getId(), menu));
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_reload_video:
				loadVideo();
				return true;

			case R.id.menu_open_video_with:
				compositeDisposable.add(youTubeVideo.playVideoExternally(getContext())
						.subscribe(status -> pause()));
				return true;

			case R.id.share:
				youTubeVideo.shareVideo(getContext());
				return true;

			case R.id.copyurl:
				youTubeVideo.copyUrl(getContext());
				return true;

			case R.id.bookmark_video:
				compositeDisposable.add(youTubeVideo.bookmarkVideo(getContext(), menu).subscribe());
				return true;

			case R.id.unbookmark_video:
				compositeDisposable.add(youTubeVideo.unbookmarkVideo(getContext(), menu).subscribe());
				return true;

			case R.id.view_thumbnail:
				Intent i = new Intent(getActivity(), ThumbnailViewerActivity.class);
				i.putExtra(ThumbnailViewerActivity.YOUTUBE_VIDEO, youTubeVideo);
				startActivity(i);
				return true;

			case R.id.download_video:
				final Policy decision = new MobileNetworkWarningDialog(requireContext())
						.showDownloadWarning(youTubeVideo);

				if (decision == Policy.ALLOW) {
					youTubeVideo.downloadVideo(getContext()).subscribe();
				}
				return true;

            case R.id.block_channel:
	            compositeDisposable.add(youTubeChannel.blockChannel().subscribe());
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}


	/**
	 * Loads the video specified in {@link #youTubeVideo}.
	 */
	private void loadVideo() {
		loadVideo(true);
	}


	/**
	 * Loads the video specified in {@link #youTubeVideo}.
	 *
	 * @param showMobileNetworkWarning Set to true to show the warning displayed when the user is
	 *                                 using mobile network data (i.e. 4g).
	 */
	private void loadVideo(boolean showMobileNetworkWarning) {
		Context ctx = getContext();
		compositeDisposable.add(
			DownloadedVideosDb.getVideoDownloadsDb().getDownloadedFileStatus(ctx, youTubeVideo.getVideoId())
				.subscribe (downloadStatus -> {
					Policy decision = Policy.ALLOW;
					Logger.i(YouTubePlayerV1Fragment.this, "Opening video : "+downloadStatus.getLocalVideoFile() + " for "+downloadStatus.getUri());

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
							fragmentBinding.loadingVideoView.setVisibility(View.VISIBLE);

							if (downloadStatus.isDisappeared()) {
								// If the file for this video has gone missing, warn and then play remotely.
								Toast.makeText(getContext(),
										getString(R.string.playing_video_file_missing),
										Toast.LENGTH_LONG).show();
								loadVideo();
								return;
							}
							if (downloadStatus.getUri() != null) {
								fragmentBinding.loadingVideoView.setVisibility(View.GONE);
								Logger.i(this, ">> PLAYING LOCALLY: %s", downloadStatus.getUri());
								fragmentBinding.videoView.setVideoURI(downloadStatus.getUri());

								// get the video statistics
								compositeDisposable.add(YouTubeTasks.getVideoDetails(ctx, youTubeVideo.getVideoId())
										.subscribe(video -> {
											if (video != null) {
												setupInfoDisplay(video);
											}
										}));

							} else {
								compositeDisposable.add(
									YouTubeTasks.getDesiredStream(youTubeVideo,
										new GetDesiredStreamListener() {
											@Override
											public void onGetDesiredStream(StreamInfo desiredStream, YouTubeVideo video) {
												// hide the loading video view (progress bar)
												fragmentBinding.loadingVideoView.setVisibility(View.GONE);

												// Play the video.  Check if this fragment is visible before playing the
												// video.  It might not be visible if the user clicked on the back button
												// before the video streams are retrieved (such action would cause the app
												// to crash if not catered for...).
												if (isVisible()) {
													StreamSelectionPolicy selectionPolicy = SkyTubeApp.getSettings().getDesiredVideoResolution(false).withAllowVideoOnly(false);
													StreamSelectionPolicy.StreamSelection selection = selectionPolicy.select(desiredStream);
													if (selection != null) {
														Uri uri = selection.getVideoStreamUri();
														Logger.i(YouTubePlayerV1Fragment.this, ">> PLAYING: %s, audio: %s", uri, selection.getAudioStreamUri());
														fragmentBinding.videoView.setVideoURI(uri);
														setupInfoDisplay(video);
													} else {
														videoPlaybackError(selectionPolicy.getErrorMessage(getContext()));
													}
												}
											}

											@Override
											public void onGetDesiredStreamError(Throwable throwable) {
												if (throwable != null) {
													videoPlaybackError(throwable.getMessage());
												}
											}
										}).subscribe());
									}
								} else {
									openAsLiveStream();
								}
							}
						}));
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
						youTubeVideo.playVideoExternally(getContext())
								.subscribe(status -> closeActivity());
					})
					.show();
		}
	}


	private void loadVideoX(boolean showMobileNetworkWarning) {
		Policy decision = Policy.ALLOW;

		// if the user is using mobile network (i.e. 4g), then warn him
		if (showMobileNetworkWarning) {
			decision = new MobileNetworkWarningDialog(requireContext())
					.onPositive((dialog, which) -> loadVideo(false))
					.onNegativeOrCancel(dialog -> closeActivity())
					.showAndGetStatus(MobileNetworkWarningDialog.ActionType.STREAM_VIDEO);
		}

		if (decision == Policy.ALLOW) {
			// if the video is NOT live
			if (!youTubeVideo.isLiveStream()) {
				stopPlayback();
				fragmentBinding.loadingVideoView.setVisibility(View.VISIBLE);
				compositeDisposable.add(
					DownloadedVideosDb.getVideoDownloadsDb().getDownloadedFileStatus(getContext(), youTubeVideo.getVideoId())
						.subscribe(status -> {
							if (status.getLocalVideoFile() != null) {
								File file = status.getLocalVideoFile();
								// If the file for this video has gone missing, remove it from the Database and then play remotely.
								if (!file.exists()) {
									Toast.makeText(getContext(),
											getContext().getString(R.string.playing_video_file_missing),
											Toast.LENGTH_LONG).show();
									loadVideo();
								} else {
									Logger.i(YouTubePlayerV1Fragment.this, ">> PLAYING LOCALLY: %s", file);
									fragmentBinding.videoView.setVideoURI(status.getUri());
								}
							} else {
								compositeDisposable.add(
										YouTubeTasks.getDesiredStream(youTubeVideo, new GetDesiredStreamListener() {
											@Override
											public void onGetDesiredStream(StreamInfo desiredStream, YouTubeVideo youTubeVideo) {
												Linker.setTextAndLinkify(videoDescriptionBinding.videoDescDescription, youTubeVideo.getDescription());

												StreamSelectionPolicy selectionPolicy = SkyTubeApp.getSettings().getDesiredVideoResolution(false).withAllowVideoOnly(false);
												StreamSelectionPolicy.StreamSelection selection = selectionPolicy.select(desiredStream);
												if (selection != null) {
													Uri uri = selection.getVideoStreamUri();
													Logger.i(YouTubePlayerV1Fragment.this, ">> PLAYING: %s", uri);
													fragmentBinding.videoView.setVideoURI(uri);
												} else {
													videoPlaybackError(selectionPolicy.getErrorMessage(getContext()));
												}
											}

											@Override
											public void onGetDesiredStreamError(Throwable throwable) {
												if (throwable != null) {
													Logger.e(YouTubePlayerV1Fragment.this, "Error getting stream info: " + throwable.getMessage(), throwable);
													videoPlaybackError(throwable.getMessage());
												}
											}
										})
								.subscribe());
							}
						}));
			} else {
				// video is live:  ask the user if he wants to play the video using an other app
				new AlertDialog.Builder(requireContext())
						.setMessage(R.string.warning_live_video)
						.setTitle(R.string.error_video_play)
						.setNegativeButton(R.string.cancel, (dialog, which) -> closeActivity())
						.setPositiveButton(R.string.ok, (dialog, which) ->
								youTubeVideo.playVideoExternally(getContext()).subscribe(status -> closeActivity()))
						.show();
			}
		}
	}

	void videoPlaybackError(String errorMessage) {
		if (playbackStateListener != null) {
			playbackStateListener.ended();
		}

		Context ctx = getContext();
		if (ctx == null) {
			Logger.e(YouTubePlayerV1Fragment.this, "Error during getting stream: %s", errorMessage);
			return;
		}

		new AlertDialog.Builder(ctx)
				.setMessage(errorMessage)
				.setTitle(R.string.error_video_play)
				.setCancelable(false)
				.setPositiveButton(R.string.ok, (dialog, which) -> closeActivity())
				.show();
	}

	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public void videoPlaybackStopped() {
		int position = fragmentBinding.videoView.getCurrentPosition();
		stopPlayback();

		saveVideoPosition(position);
	}

	private void saveVideoPosition(int position) {
		compositeDisposable.add(
				PlaybackStatusDb.getPlaybackStatusDb().setVideoPositionInBackground(youTubeVideo, position));
	}

	@Override
	public YouTubeVideo getYouTubeVideo() {
		return youTubeVideo;
	}

	@Override
	public int getCurrentVideoPosition() {
		return fragmentBinding.videoView.getCurrentPosition();
	}


	@Override
	public boolean isPlaying() {
		return fragmentBinding.videoView.isPlaying();
	}


	private void stopPlayback() {
		fragmentBinding.videoView.pause();
		fragmentBinding.videoView.stopPlayback();

		if (playbackStateListener != null) {
			playbackStateListener.ended();
		}
	}


	@Override
	public void pause() {
		fragmentBinding.videoView.pause();

		if (playbackStateListener != null && !fragmentBinding.videoView.isPlaying()) {
			playbackStateListener.paused();
		}
	}


	@Override
	public void play() {
		fragmentBinding.videoView.start();

		if (playbackStateListener != null && fragmentBinding.videoView.isPlaying()) {
			playbackStateListener.started();
		}
	}


	@Override
	public void setPlaybackStateListener(PlaybackStateListener listener) {
		playbackStateListener = listener;
	}
}
