/*
 * SkyTube
 * Copyright (C) 2015  Ramon Mifsud
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

package free.rm.skytube.gui.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.mediarouter.media.MediaRouteSelector;
import androidx.mediarouter.media.MediaRouter;
import androidx.preference.PreferenceManager;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaLoadOptions;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.Session;
import com.google.android.gms.cast.framework.SessionManager;
import com.google.android.gms.cast.framework.SessionManagerListener;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.images.WebImage;
import com.google.gson.Gson;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.schabi.newpipe.extractor.stream.StreamInfo;

import free.rm.skytube.BuildConfig;
import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.app.StreamSelectionPolicy;
import free.rm.skytube.businessobjects.ChromecastListener;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;
import free.rm.skytube.businessobjects.YouTube.YouTubeTasks;
import free.rm.skytube.businessobjects.interfaces.GetDesiredStreamListener;
import free.rm.skytube.databinding.ActivityMainBinding;
import free.rm.skytube.gui.businessobjects.MainActivityListener;
import free.rm.skytube.gui.businessobjects.YouTubePlayer;
import free.rm.skytube.gui.fragments.ChromecastControllerFragment;
import free.rm.skytube.gui.fragments.ChromecastMiniControllerFragment;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

/**
 * Base Activity class that handles all Chromecast-related functionality. Any Activity that needs to use the Cast Icon and
 * Sliding Up Panel will need to extend this class. The OSS variant just contains a bunch of no-op methods.
 */
public abstract class BaseActivity extends AppCompatActivity implements MainActivityListener, ChromecastListener {
	public static final String KEY_POSITION = "position";
	public static final String KEY_VIDEO = "video";

	public static final String ACTION_NOTIFICATION_CLICK = "free.rm.skytube.ACTION_NOTIFICATION_CLICK";

	public static final String PANEL_EXPANDED = "free.rm.skytube.PANEL_EXPANDED";

	private static final String PREF_GPS_POPUP_VIEWED = "BaseActivity.pref_gps_poup_viewed";

	private boolean panelShouldExpand = false;

	private CastSession mCastSession;
	private SessionManager mSessionManager;
	private final SessionManagerListener mSessionManagerListener =
					new SessionManagerListenerImpl();
	private ChromecastMiniControllerFragment chromecastMiniControllerFragment;
	private ChromecastControllerFragment chromecastControllerFragment;

	private MediaRouter mediaRouter;
	private Intent externalPlayIntent;
	private Intent notificationClickIntent;

    protected ActivityMainBinding binding;

	private final CompositeDisposable compositeDisposable = new CompositeDisposable();


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());

        /**
         * Google Play Services is required to set up Chromecast support. If it's not available, display a popup that alerts the user,
         * then set a flag to never show the popup again. If the user later installs GPS, Chromecast support will work after that.
         */
		boolean googlePlayServicesAvailable = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS;

		if(googlePlayServicesAvailable) {
			if (savedInstanceState != null) {
				chromecastMiniControllerFragment = (ChromecastMiniControllerFragment) getSupportFragmentManager().getFragment(savedInstanceState, ChromecastMiniControllerFragment.CHROMECAST_MINI_CONTROLLER_FRAGMENT);
				chromecastControllerFragment = (ChromecastControllerFragment) getSupportFragmentManager().getFragment(savedInstanceState, ChromecastControllerFragment.CHROMECAST_CONTROLLER_FRAGMENT);
				if (savedInstanceState.getBoolean(PANEL_EXPANDED, false)) {
					panelShouldExpand = true;
				}
			}

			CastContext mCastContext = CastContext.getSharedInstance(BaseActivity.this);
			mSessionManager = mCastContext.getSessionManager();

			mediaRouter = MediaRouter.getInstance(getApplicationContext());
			MediaRouteSelector mediaRouteSelector = new MediaRouteSelector.Builder()
					.addControlCategory(CastMediaControlIntent.categoryForCast(BuildConfig.CHROMECAST_APP_ID)).build();
			mediaRouter.addCallback(mediaRouteSelector, new MediaRouter.Callback() {
				private void onRouteAddedOrChanged(MediaRouter.RouteInfo route) {
					SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(BaseActivity.this);
					String defaultChromecastId = sharedPref.getString(getString(R.string.pref_key_autocast), getString(R.string.pref_title_chromecast_none));
					if (route.getId().equals(defaultChromecastId)) {
						if (externalPlayIntent != null) {
							mediaRouter.selectRoute(route);
						}
					}
				}

				@Override
				public void onRouteAdded(MediaRouter router, MediaRouter.RouteInfo route) {
					onRouteAddedOrChanged(route);
				}

				@Override
				public void onRouteChanged(MediaRouter router, MediaRouter.RouteInfo route) {
					onRouteAddedOrChanged(route);
				}

				@Override
				public void onRouteRemoved(MediaRouter router, MediaRouter.RouteInfo route) {
				}
			});
			handleExternalPlayOnChromecast(getIntent());
		} else {
			final SharedPreferences preferences = SkyTubeApp.getPreferenceManager();
			if(!preferences.getBoolean(PREF_GPS_POPUP_VIEWED, false)) {
				MaterialDialog gpsMissingDialog = new MaterialDialog.Builder(this)
						.title(R.string.gps_missing_title)
						.content(R.string.gps_missing_description)
						.backgroundColorRes(R.color.colorPrimary)
						.positiveText(R.string.ok)
						.onPositive((dialog, which) -> {
							SharedPreferences.Editor editor = preferences.edit();
							editor.putBoolean(PREF_GPS_POPUP_VIEWED, true);
							editor.apply();
							dialog.dismiss();
						})
						.build();
				gpsMissingDialog.show();
			}
		}
	}

	@Override
	protected void onDestroy() {
		compositeDisposable.clear();
		super.onDestroy();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		handleExternalPlayOnChromecast(intent);
		handleNotificationClick(intent);
	}

	private void handleNotificationClick(Intent intent) {
		if(intent != null) {
			// User clicked on the notification while a video is playing on Chromecast.
			if (intent.getAction() != null && intent.getAction().equals(ACTION_NOTIFICATION_CLICK)) {
				// If the app is being resumed. If we're not, this will get called once we resume our connection.
				if(notificationClickIntent == null && chromecastMiniControllerFragment == null) {
					notificationClickIntent = intent;
				} else {
					chromecastMiniControllerFragment.setDidClickNotification(true);
				}
			}
		}
	}

	/**
	 * If the Preference has been chosen to always launch videos via external apps via the chosen
	 * default Chromecast.
	 */
	private static boolean launchExternalOnDefaultChromecast(Context context) {
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
		return !sharedPref.getString(context.getString(R.string.pref_key_autocast), context.getString(R.string.pref_key_chromecast_none)).equals(context.getString(R.string.pref_key_chromecast_none));
	}

	private void handleExternalPlayOnChromecast(final Intent intent) {
		if(intent != null) {
			// If we're connected to a chromecast when we receive this video to play, play it there, otherwise
			// forward it on to the local player. HOWEVER, we need to skip this if the calling Intent's class name
			// is YouTubePlayerActivity, otherwise we'll get stuck in an endless loop!
			if(intent.getAction() != null &&
							intent.getAction().equals(Intent.ACTION_VIEW) &&
							!intent.getComponent().getClassName().equals(YouTubePlayerActivity.class.getName()) &&
							(intent.getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) == 0) {


				/**
				 * If a default Chromecast has been set for videos received via an external app (AutoCast), show the
				 * loading spinner and save the intent (which contains the video to play). When the Chromecast (route) is
				 * discovered above, it will find the default Chromecast and connect to it. Once the connection is
				 * established, {@link #handleExternalPlayOnChromecast(Intent)} will be called again, this time with
				 * externalPlayIntent set, which will allow the video to be launched on the Chromecast. Also only do
				 * this if we aren't already connected to a Chromecast.
				 */
				final boolean connectedToChromecast = YouTubePlayer.isConnectedToChromecast();
				if(launchExternalOnDefaultChromecast(this) && externalPlayIntent == null && !connectedToChromecast) {
					showLoadingSpinner();
					externalPlayIntent = intent;
				} else {
					if (connectedToChromecast) {
						compositeDisposable.add(YouTubeTasks.getVideoDetails(this, intent)
								.subscribe(video -> playVideoOnChromecast(video, 0)));
					} else {
						Intent i = new Intent(this, YouTubePlayerActivity.class);
						i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
						i.setAction(Intent.ACTION_VIEW);
						i.setData(intent.getData());
						startActivity(i);
						finish(); // Finish this activity, so that the back button returns to the app that launched this video
					}
				}
			}
		}
	}

	/**
	 * This will be called when the options menu has been created. It's needed to set up the cast icon
	 */
	protected void onOptionsMenuCreated(Menu menu) {
		MenuItem mediaRouteMenuItem = CastButtonFactory.setUpMediaRouteButton(getApplicationContext(), menu, R.id.media_route_menu_item);
	}

	@Override
	protected void onResume() {
        /**
         * When resuming, make sure Google Play Services is installed before trying to resume everything for Chromecast support.
         */
		boolean googlePlayServicesAvailable = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS;
		if(googlePlayServicesAvailable) {
			if (mCastSession == null)
				mCastSession = mSessionManager.getCurrentCastSession();
			mSessionManager.addSessionManagerListener(mSessionManagerListener);
			if (mCastSession != null && mCastSession.getRemoteMediaClient() != null) {
				if (mCastSession.getRemoteMediaClient().getPlayerState() != MediaStatus.PLAYER_STATE_IDLE) {
					chromecastMiniControllerFragment.init(mCastSession.getRemoteMediaClient());
					chromecastControllerFragment.init(mCastSession.getRemoteMediaClient());
					showPanel();
				} else
					hidePanel();
			}
			handleNotificationClick(getIntent());
		}
		super.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
		boolean googlePlayServicesAvailable = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS;
		if(googlePlayServicesAvailable) {
			mSessionManager.removeSessionManagerListener(mSessionManagerListener);
			mCastSession = null;
		}
	}

	/**
	 * This is overridden in YouTubePlayerActivity to return true
	 */
	protected boolean isLocalPlayer() {
		return false;
	}

	/**
	 * This is overridden in YouTubePlayerActivity
	 */
	protected void returnToMainAndResume() {
	}

	/**
	 * If we're returning from YouTubePlayerActivity as a result of the user connecting to a Chromecast
	 * in the middle of playback, launch that video on the Chromecast.
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(requestCode == YouTubePlayerActivity.YOUTUBE_PLAYER_RESUME_RESULT && resultCode == RESULT_OK) {
			// We're returning to the main activity from the local video player, where a chromecast was connected
			// to in the middle of playback. We should launch the youtube player on the chromecast now
			Bundle bundle = data.getExtras();
			YouTubeVideo video = (YouTubeVideo)bundle.getSerializable(YouTubePlayerActivity.YOUTUBE_VIDEO);
			int position = bundle.getInt(YouTubePlayerActivity.YOUTUBE_VIDEO_POSITION);
			playVideoOnChromecast(video, position);
		}
	}

	private class SessionManagerListenerImpl implements SessionManagerListener {
		@Override
		public void onSessionStarted(Session session, String sessionId) {
			mCastSession = CastContext.getSharedInstance(BaseActivity.this).getSessionManager().getCurrentCastSession();
			invalidateOptionsMenu();
			YouTubePlayer.setConnectedToChromecast(true);
			YouTubePlayer.setConnectingToChromecast(false);
			// If we are connecting in YouTubePlayerActivity, finish that activity and launch the currently playing
			// video on the Chromecast.
			if(isLocalPlayer()) {
				returnToMainAndResume();
			} else if(externalPlayIntent != null) {
				// A default Chromecast has been set to handle external intents, and that Chromecast has now been
				// connected to. Play the video (which is stored in externalPlayIntent).
				handleExternalPlayOnChromecast(externalPlayIntent);
				externalPlayIntent = null;
			}
			if(notificationClickIntent != null) {
				handleNotificationClick(notificationClickIntent);
				notificationClickIntent = null;
			}
		}

		@Override
		public void onSessionResumed(Session session, boolean wasSuspended) {
			mCastSession = CastContext.getSharedInstance(BaseActivity.this).getSessionManager().getCurrentCastSession();
			Runnable r = () -> {
				if(mCastSession.getRemoteMediaClient().getPlayerState() != MediaStatus.PLAYER_STATE_IDLE) {
					chromecastMiniControllerFragment.init(mCastSession.getRemoteMediaClient());
					chromecastControllerFragment.init(mCastSession.getRemoteMediaClient());
					binding.slidingLayout.addPanelSlideListener(getOnPanelDisplayed((int) mCastSession.getRemoteMediaClient().getApproximateStreamPosition(), (int) mCastSession.getRemoteMediaClient().getStreamDuration()));
				} else if(externalPlayIntent != null) {
					// A default Chromecast has been set to handle external intents, and that Chromecast has now been
					// connected to. Play the video (which is stored in externalPlayIntent).
					handleExternalPlayOnChromecast(externalPlayIntent);
					externalPlayIntent = null;
				}
			};
			// Sometimes when we resume a chromecast session, even if media is actually playing, the player state is still idle here.
			// In that case, wait 500ms and check again (above Runnable). But if it's not idle, do the above right away.
			int delay = mCastSession.getRemoteMediaClient().getPlayerState() != MediaStatus.PLAYER_STATE_IDLE ? 0 : 500;
			new Handler().postDelayed(r, delay);

			invalidateOptionsMenu();
			YouTubePlayer.setConnectedToChromecast(true);
			YouTubePlayer.setConnectingToChromecast(false);
		}

		@Override
		public void onSessionEnded(Session session, int error) {
			YouTubePlayer.setConnectedToChromecast(false);
			hidePanel();
		}

		@Override
		public void onSessionSuspended(Session session, int i) {
			YouTubePlayer.setConnectedToChromecast(false);
		}

		@Override
		public void onSessionStarting(Session session) {
			if(isLocalPlayer()) {
				BaseActivity.this.onSessionStarting();
			}
			YouTubePlayer.setConnectingToChromecast(true);
		}

		@Override
		public void onSessionStartFailed(Session session, int i) {
			YouTubePlayer.setConnectingToChromecast(false);
			hideLoadingSpinner();
		}

		@Override
		public void onSessionEnding(Session session) {

		}

		@Override
		public void onSessionResuming(Session session, String s) {
			YouTubePlayer.setConnectingToChromecast(true);
		}

		@Override
		public void onSessionResumeFailed(Session session, int i) {
			YouTubePlayer.setConnectingToChromecast(false);
			hideLoadingSpinner();
		}
	}

	protected void onLayoutSet() {
		if(chromecastMiniControllerFragment == null)
			chromecastMiniControllerFragment = (ChromecastMiniControllerFragment)getSupportFragmentManager().findFragmentById(R.id.chromecastMiniControllerFragment);
		chromecastMiniControllerFragment.setSlidingLayout(binding.slidingLayout);

		if(chromecastControllerFragment == null)
			chromecastControllerFragment = (ChromecastControllerFragment)getSupportFragmentManager().findFragmentById(R.id.chromecastControllerFragment);

		chromecastMiniControllerFragment.setMainActivityListener(this);
		// Let both controller fragments know about each other, so that they can adjust the other's progress bar when needed
		chromecastMiniControllerFragment.setOtherControllerFragment(chromecastControllerFragment);
		chromecastControllerFragment.setOtherControllerFragment(chromecastMiniControllerFragment);

		if(notificationClickIntent != null) {
			handleNotificationClick(notificationClickIntent);
			notificationClickIntent = null;
		}
	}

    /**
     * When returning to {@link free.rm.skytube.gui.fragments.MainFragment} from a fragment that uses
     * CoordinatorLayout, redraw the Sliding Panel. This fixes an apparent bug in CoordinatorLayout that
     * causes the panel to be positioned improperly (the bottom half of the panel ends up below the screen)
     */
    @Override
    public void redrawPanel() {
        final LinearLayout chromecastControllersContainer = binding.chromecastControllersContainer;
        chromecastControllersContainer.post(binding.chromecastControllersContainer::requestLayout);
    }

    private void showPanel() {
        final SlidingUpPanelLayout slidingLayout = binding.slidingLayout;
        if(slidingLayout != null) {
            if(slidingLayout.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED || panelShouldExpand) {
                slidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
                chromecastMiniControllerFragment.setSlidingLayout(slidingLayout);
            } else {
                slidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
            }
        }
    }

    private void hidePanel() {
        final SlidingUpPanelLayout slidingLayout = binding.slidingLayout;
        if(slidingLayout != null) {
            slidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
        }
    }

	@Override
	public void onChannelClick(String channelId) {
	}

	@Override
	public void onSessionStarting() {
	}

	public void playVideoOnChromecast(final YouTubeVideo video, final int position) {
		showLoadingSpinner();
		if(video.getDescription() == null) {
			compositeDisposable.add(YouTubeTasks.getVideoDescription(video)
					.subscribe(description -> playVideoOnChromecast(video, position)));
		} else {
			compositeDisposable.add(
					YouTubeTasks.getDesiredStream(video, new GetDesiredStreamListener() {
						@Override
						public void onGetDesiredStream(StreamInfo desiredStream, YouTubeVideo video) {
							if(mCastSession == null)
								return;
							Gson gson = new Gson();
							final RemoteMediaClient remoteMediaClient = mCastSession.getRemoteMediaClient();
							MediaMetadata metadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_GENERIC);
							metadata.putInt(KEY_POSITION, position);
							metadata.putString(KEY_VIDEO, gson.toJson(video));

							metadata.addImage(new WebImage(Uri.parse(video.getThumbnailUrl())));

					StreamSelectionPolicy policy = SkyTubeApp.getSettings().getDesiredVideoResolution(false).withAllowVideoOnly(false);
					StreamSelectionPolicy.StreamSelection selection = policy.select(desiredStream);
					MediaInfo currentPlayingMedia = new MediaInfo.Builder(selection.getVideoStreamUri().toString())
									.setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
									.setContentType(selection.getVideoStream().getFormat().mimeType)
									.setMetadata(metadata)
									.build();

							MediaLoadOptions options = new MediaLoadOptions.Builder().setAutoplay(true).setPlayPosition(0).build();
							remoteMediaClient.load(currentPlayingMedia, options);
							chromecastMiniControllerFragment.init(remoteMediaClient, currentPlayingMedia, position);
							chromecastControllerFragment.init(remoteMediaClient, currentPlayingMedia, position);
							// If the Controller panel isn't visible, setting the progress of the progressbar in the mini controller won't
							// work until the panel is visible, so do it as soon as the sliding panel is visible. Adding this listener when
							// the panel is not hidden will lead to a java.util.ConcurrentModificationException the next time a video is
							// switching from local playback to chromecast, so we should only do this if the panel is hidden.
                            final SlidingUpPanelLayout slidingLayout = binding.slidingLayout;
                            if (slidingLayout.getPanelState() == SlidingUpPanelLayout.PanelState.HIDDEN) {
                                slidingLayout.addPanelSlideListener(getOnPanelDisplayed(position, video.getDurationInSeconds() * 1000));
                            }
						}

						@Override
						public void onGetDesiredStreamError(Throwable throwable) {
							if (throwable != null) {
                                setChromecastLoadingSpinnerVisibility(View.GONE);
								new AlertDialog.Builder(BaseActivity.this)
										.setMessage(throwable.getMessage())
										.setTitle(R.string.error_video_play)
										.setCancelable(false)
										.setPositiveButton(R.string.ok, null)
										.show();
							}
						}
					})
						.subscribe());
		}
	}

	private SlidingUpPanelLayout.PanelSlideListener getOnPanelDisplayed(final int position, final int duration) {
		return new SlidingUpPanelLayout.PanelSlideListener() {
			@Override
			public void onPanelSlide(View view, float v) {

			}

			@Override
			public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState) {
				if (newState == SlidingUpPanelLayout.PanelState.COLLAPSED) {
					chromecastMiniControllerFragment.setDuration(duration);
					chromecastMiniControllerFragment.setProgress(position);
					chromecastControllerFragment.setDuration(duration);
					chromecastControllerFragment.setProgress(position);
					binding.slidingLayout.removePanelSlideListener(this);
				}
			}
		};
	}

    /**
     * When connected to a Chromecast and a video is clicked, this shows a spinner to indicate
     * that the video is being loaded.
     */
    @Override
    public void showLoadingSpinner() {
        setChromecastLoadingSpinnerVisibility(View.VISIBLE);
    }

    /**
     * Hide the Chromecast Loading Spinner
     */
    public void hideLoadingSpinner() {
        setChromecastLoadingSpinnerVisibility(View.GONE);
    }

    /**
     * Hide the spinner when play has started, and show the panel that contains the Controller
     */
    @Override
    public void onPlayStarted() {
        setChromecastLoadingSpinnerVisibility(View.GONE);
        if(binding.slidingLayout.getPanelState() == SlidingUpPanelLayout.PanelState.HIDDEN) {
            showPanel();
        }
    }

	/**
	 * Chromecast playback has stopped, so hide the panel.
	 */
	@Override
	public void onPlayStopped() {
		hidePanel();
	}

	/**
	 * MainActivity should minimize instead of exit, when the back button is pressed, and mainFragment is visible.
	 * However, if the Chromecast Controller is visible and expanded, we want to collapse it instead of exiting or
	 * returning to the homescreen. This method will be called from MainActivity.onBackPressed, so if the Chromecast
	 * Controller is not expanded, we return true.
	 *
	 * @return false if the Chromecast Controller is visible and expanded, otherwise true.
	 */
	public boolean shouldMinimizeOnBack() {
		SlidingUpPanelLayout slidingLayout = binding.slidingLayout;
		if(slidingLayout != null && slidingLayout.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED) {
			slidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
			return false;
		}
		return true;
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if(chromecastMiniControllerFragment != null && chromecastControllerFragment != null) {
			getSupportFragmentManager().putFragment(outState, ChromecastMiniControllerFragment.CHROMECAST_MINI_CONTROLLER_FRAGMENT, chromecastMiniControllerFragment);
			getSupportFragmentManager().putFragment(outState, ChromecastControllerFragment.CHROMECAST_CONTROLLER_FRAGMENT, chromecastControllerFragment);
			outState.putBoolean(PANEL_EXPANDED, binding.slidingLayout.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
	}

	/**
	 * No-op method, in order to be able to connect to a Chromecast from
	 * this activity, it needs to implement this method, but doesn't need to do anything, since it doesn't use
	 * SubscriptionsFeedFragment.
	 */
	@Override
	public void refreshSubscriptionsFeedVideos() {}

    protected void setChromecastLoadingSpinnerVisibility(final int visibility) {
        binding.chromecastLoadingSpinner.setVisibility(visibility);
    }

}
