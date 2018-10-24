package free.rm.skytube.gui.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.ChromecastListener;
import free.rm.skytube.businessobjects.db.PlaybackStatusDb;

/**
 * Fragment class that is used for Chromecast control. This Fragment will appear at the bottom of the app when a video
 * has started casting. It provides a few controls: Stop, Rewind, Fast Forward, and Play/Pause. Clicking on this Fragment
 * will expand it full screen, showing {@link ChromecastControllerFragment}. Dragging this fragment up will do the same thing.
 */
public class ChromecastMiniControllerFragment extends ChromecastBaseControllerFragment {
	public static final String CHROMECAST_MINI_CONTROLLER_FRAGMENT = "free.rm.skytube.CHROMECAST_MINI_CONTROLLER_FRAGMENT";

	/** The {@link free.rm.skytube.businessobjects.ChromecastListener} Activity that will be notified when play has started and stopped */
	private ChromecastListener activityListener;

	@BindView(R.id.videoTitle)
	TextView videoTitle;
	@BindView(R.id.channelName)
	TextView channelName;

	@BindView(R.id.chromecastMiniControllerChevron)
	ImageView chromecastMiniControllerChevron;

	@BindView(R.id.chromecastMiniControllerShareButton)
	ImageButton chromecastMiniControllerShareButton;

	@BindView(R.id.chromecastMiniControllerLeftContainer)
	View chromecastMiniControllerLeftContainer;
	@BindView(R.id.chromecastMiniControllerRightContainer)
	View chromecastMiniControllerRightContainer;

	private SlidingUpPanelLayout slidingLayout;

	private boolean didClickNotification = false;

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_chromecast_mini_controller, container);
		ButterKnife.bind(this, view);
		return view;
	}

	@Override
	public void init(RemoteMediaClient client, MediaInfo media, int position) {
		super.init(client, media, position);

		videoTitle.setText(video.getTitle());
		channelName.setText(video.getChannelName());

		// We just either started playback of a video, or resumed the cast session. In the latter case, if there's a video playing, let the activity
		// know so that the panel will appear.
		if(currentPlayerState != MediaStatus.PLAYER_STATE_IDLE) {
			activityListener.onPlayStarted();
		}
		chromecastMiniControllerLeftContainer.setAlpha(1);
		chromecastMiniControllerRightContainer.setAlpha(1);
		setDuration((int)client.getStreamDuration());
	}

	@OnClick(R.id.chromecastMiniControllerShareButton)
	public void shareVideo(View v) {
		Intent intent = new Intent(android.content.Intent.ACTION_SEND);
		intent.setType("text/plain");
		intent.putExtra(android.content.Intent.EXTRA_TEXT, video.getVideoUrl());
		startActivity(Intent.createChooser(intent, "Share via"));
	}

	@Override
	protected long getProgressBarPeriod() {
		long period = remoteMediaClient.getStreamDuration() / 100;
		if(period < 1000)
			period = 1000;
		if(period > 10000)
			period = 10000;
		return period;
	}

	@Override
	protected void onPlayStopped() {
		if(!SkyTubeApp.getPreferenceManager().getBoolean(getString(R.string.pref_key_disable_playback_status), false)) {
			PlaybackStatusDb.getVideoDownloadsDb().setVideoPosition(video, chromecastPlaybackProgressBar.getProgress());
		}

		activityListener.onPlayStopped();
	}

	@Override
	protected void onPlayStarted() {
		activityListener.onPlayStarted();
	}

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		try {
			activityListener = (ChromecastListener)context;
		} catch (ClassCastException e) {
			throw new ClassCastException(context.toString() + " must implement ChromecastListener to use ChromecastMiniControllerFragment");
		}
	}

	public ImageView getChromecastMiniControllerChevron() {
		return chromecastMiniControllerChevron;
	}


	public void setSlidingLayout(SlidingUpPanelLayout layout) {
		slidingLayout = layout;
		slidingLayout.addPanelSlideListener(panelSlideListener);
		if(slidingLayout.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED) {
			chromecastPlaybackProgressBar.setVisibility(View.INVISIBLE);
			chromecastMiniControllerShareButton.setVisibility(View.VISIBLE);

			chromecastMiniControllerLeftContainer.setAlpha(0);
			chromecastMiniControllerRightContainer.setAlpha(0);
		}
	}

	private SlidingUpPanelLayout.PanelSlideListener panelSlideListener = new SlidingUpPanelLayout.PanelSlideListener() {
		private boolean wasHidden = true; // hidden by default

		@Override
		public void onPanelSlide(View panel, float slideOffset) {
			// slide offset goes from 0.00xxx to 1.0000 as it is slides from the bottom all the way to the top
			// Fade the buttons in (for sliding down) and out (for sliding up)
			if(slideOffset > 0) {
				chromecastMiniControllerLeftContainer.setAlpha(1 - slideOffset);
				chromecastMiniControllerRightContainer.setAlpha(1 - slideOffset);
				if (slideOffset < 1)
					chromecastMiniControllerShareButton.setVisibility(View.INVISIBLE);
			}
		}

		@Override
		public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState) {
			// As soon as the panel gets hidden, set wasHidden to true to prevent the animation from showing until we finish going to
			// collapsed from hidden
			if(previousState == SlidingUpPanelLayout.PanelState.HIDDEN)
				wasHidden = true;

			// Hide the mini controller progress bar when the panel is expanded, and show it when it's collapsed
			if(newState == SlidingUpPanelLayout.PanelState.EXPANDED) {
				chromecastPlaybackProgressBar.setVisibility(View.INVISIBLE);
				chromecastMiniControllerShareButton.setVisibility(View.VISIBLE);

				chromecastMiniControllerLeftContainer.setAlpha(0);
				chromecastMiniControllerRightContainer.setAlpha(0);
			} else if(newState == SlidingUpPanelLayout.PanelState.COLLAPSED) {
				chromecastPlaybackProgressBar.setVisibility(View.VISIBLE);

				// If the user subscribed to or unsubscribed from the Channel this video belongs to, from the ChromecastController, when the panel collapses, we should
				// update the Feed tab to reflect the change.
				if(SubscriptionsFeedFragment.isFlagSet(SubscriptionsFeedFragment.FLAG_REFRESH_FEED_FROM_CACHE)) {
					SubscriptionsFeedFragment.unsetFlag(SubscriptionsFeedFragment.FLAG_REFRESH_FEED_FROM_CACHE);
					if(mainActivityListener != null)
						mainActivityListener.refreshSubscriptionsFeedVideos();
				}
			}

			// wasHidden boolean being true will prevent the animation from happening when going from hidden to collapsed
			if ((!wasHidden || didClickNotification) && previousState == SlidingUpPanelLayout.PanelState.DRAGGING && (newState == SlidingUpPanelLayout.PanelState.EXPANDED || newState == SlidingUpPanelLayout.PanelState.COLLAPSED)) {
				didClickNotification = false;
				float begin;
				float end;
				if (newState == SlidingUpPanelLayout.PanelState.EXPANDED) {
					begin = 0f;
					end = 180f;
				} else {
					begin = 180f;
					end = 360f;
				}

				RotateAnimation anim = new RotateAnimation(begin, end, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
				anim.setInterpolator(new LinearInterpolator());
				anim.setDuration(500);
				anim.setFillAfter(true);
				getChromecastMiniControllerChevron().startAnimation(anim);
			}

			// Since we're now at collapsed, set wasHidden to false to show the animation the next time the state changes.
			if(newState == SlidingUpPanelLayout.PanelState.COLLAPSED)
				wasHidden = false;
		}
	};

	public void setDidClickNotification(boolean didClickNotification) {
		this.didClickNotification = didClickNotification;
	}
}
