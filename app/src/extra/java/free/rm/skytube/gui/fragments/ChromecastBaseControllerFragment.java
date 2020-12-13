package free.rm.skytube.gui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;
import free.rm.skytube.gui.activities.BaseActivity;
import free.rm.skytube.gui.businessobjects.MainActivityListener;
import free.rm.skytube.gui.businessobjects.fragments.FragmentEx;

/**
 * Abstract Fragment class that {@link ChromecastControllerFragment} and {@link ChromecastMiniControllerFragment} extend.
 */
public abstract class ChromecastBaseControllerFragment extends FragmentEx {
	public static final String KEY_DURATION = "free.rm.skytube.KEY_DURATION";
	public static final String KEY_CURRENT_POSITION = "free.rm.skytube.KEY_CURRENT_POSITION";

	/** {@link com.google.android.gms.cast.MediaInfo} object that contains all the metadata for the currently playing video. */
	protected MediaInfo currentPlayingMedia;
	/** {@link RemoteMediaClient} representing the Chromecast this fragment controls. */
	protected RemoteMediaClient remoteMediaClient;

	/** The current playback state of the Chromecast */
	protected int currentPlayerState = MediaStatus.PLAYER_STATE_IDLE;

	protected int duration = 0;

	protected boolean didSeek = false;
	protected boolean isSeeking = false;

	protected YouTubeVideo video;

	protected MainActivityListener mainActivityListener;

	protected ChromecastBaseControllerFragment otherControllerFragment;

	protected ProgressBar chromecastPlaybackProgressBar;

	/** Playback buttons */
	protected ImageButton playButton;
	protected ImageButton pauseButton;
	protected ImageButton forwardButton;
	protected ImageButton rewindButton;
	protected ImageButton stopButton;
	protected View bufferingSpinner;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		return super.onCreateView(inflater, container, savedInstanceState);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		playButton.setOnClickListener(v -> remoteMediaClient.play());
		pauseButton.setOnClickListener(v -> remoteMediaClient.pause());
		forwardButton.setOnClickListener(v -> {
			if(remoteMediaClient.getApproximateStreamPosition() + 30000 < remoteMediaClient.getStreamDuration()) {
				didSeek = true;
				remoteMediaClient.seek(remoteMediaClient.getApproximateStreamPosition() + 30000);
			}
		});
		rewindButton.setOnClickListener(v -> {
			didSeek = true;
			remoteMediaClient.seek(remoteMediaClient.getApproximateStreamPosition() - 10000);
		});
		stopButton.setOnClickListener(v -> remoteMediaClient.stop());

		if(savedInstanceState != null) {
			int currentPosition = savedInstanceState.getInt(KEY_CURRENT_POSITION, 0);
			int duration = savedInstanceState.getInt(KEY_DURATION, 0);
			setDuration(duration);
			setProgress(currentPosition);
		}
	}

	@Override
	public void onDestroyView() {
		chromecastPlaybackProgressBar = null;
		playButton = null;
		pauseButton = null;
		forwardButton = null;
		rewindButton = null;
		stopButton = null;
		bufferingSpinner = null;
		super.onDestroyView();
	}

	public void init(RemoteMediaClient client) {
		init(client, client.getMediaInfo(), (int)client.getApproximateStreamPosition());
	}

	public void init(RemoteMediaClient client, MediaInfo media, int position) {
		remoteMediaClient = client;
		currentPlayingMedia = media;
		currentPlayerState = remoteMediaClient.getPlayerState();

		Gson gson = new Gson();
		this.video = gson.fromJson(currentPlayingMedia.getMetadata().getString(BaseActivity.KEY_VIDEO), new TypeToken<YouTubeVideo>(){}.getType());

		if(currentPlayerState != MediaStatus.PLAYER_STATE_IDLE) {
			updateButtons();
		}
		remoteMediaClient.unregisterCallback(mediaCallback);
		remoteMediaClient.registerCallback(mediaCallback);
		setProgressBarUpdater();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if(remoteMediaClient != null) {
			outState.putInt(KEY_CURRENT_POSITION, (int) remoteMediaClient.getApproximateStreamPosition());
			outState.putInt(KEY_DURATION, (int) currentPlayingMedia.getStreamDuration());
		}
	}

	public void setOtherControllerFragment(ChromecastBaseControllerFragment fragment) {
		otherControllerFragment = fragment;
	}

	protected abstract long getProgressBarPeriod();

	protected void setProgressBarUpdater() {
		remoteMediaClient.removeProgressListener(progressBarUpdater);
		remoteMediaClient.addProgressListener(progressBarUpdater, getProgressBarPeriod());
	}

	RemoteMediaClient.ProgressListener progressBarUpdater = new RemoteMediaClient.ProgressListener() {
		@Override
		public void onProgressUpdated(long progress, long duration) {
			if(chromecastPlaybackProgressBar.getMax() != duration) {
				chromecastPlaybackProgressBar.setMax((int) duration);
			}
			if(!isSeeking) {
				chromecastPlaybackProgressBar.setProgress((int) progress);
			}
		}
	};

	protected void onPlayStopped() {}
	protected void onPlayStarted() {}

	public void setDuration(int duration) {
		chromecastPlaybackProgressBar.setMax(duration);
	}

	public void setProgress(int progress) {
		chromecastPlaybackProgressBar.setProgress(progress);
	}

	private RemoteMediaClient.Callback mediaCallback = new RemoteMediaClient.Callback() {
		@Override
		public void onStatusUpdated() {
			MediaStatus status = remoteMediaClient.getMediaStatus();
			if(status == null)
				return;
			int oldState = currentPlayerState;
			currentPlayerState = status.getPlayerState();

			/** If the new playback state is idle and it is because playback finished or was stopped, let the activity
			 * know that playback has stopped.
			 */
			if(status.getPlayerState() == MediaStatus.PLAYER_STATE_IDLE && (status.getIdleReason() == MediaStatus.IDLE_REASON_FINISHED || status.getIdleReason() == MediaStatus.IDLE_REASON_CANCELED)) {
				if(oldState != MediaStatus.PLAYER_STATE_IDLE) {
					onPlayStopped();
				}
				return;
			}

			updateButtons();

			if(didSeek) {
				didSeek = false;
				chromecastPlaybackProgressBar.setProgress((int)remoteMediaClient.getApproximateStreamPosition());
				if(otherControllerFragment != null)
					otherControllerFragment.setProgress((int)remoteMediaClient.getApproximateStreamPosition());
			}
			/** If the previous playback state of the Chromecast was idle, and it is no longer idle, let the activity
			 * know that playback has started.
			 */
			if (oldState == MediaStatus.PLAYER_STATE_IDLE && currentPlayerState != MediaStatus.PLAYER_STATE_IDLE) {
				currentPlayingMedia = remoteMediaClient.getMediaInfo();
				/**
				 * Reset the ProgressBar with the new media, and add a progress listener to update the progress bar.
				 * If the video is under 100 seconds long, it will update every second. If the video is over 16.6 minutes
				 * long, it will update every 10 seconds. Inbetween those, it will update in exactly 100 steps.
				 */
				setProgressBarUpdater();
				onPlayStarted();
			}
		}
	};

	/**
	 * Change the visibility of the play/pause/buffering buttons depending on the current playback state.
	 */
	protected void updateButtons() {
		if(currentPlayerState == MediaStatus.PLAYER_STATE_PLAYING) {
			playButton.setVisibility(View.GONE);
			pauseButton.setVisibility(View.VISIBLE);
			bufferingSpinner.setVisibility(View.GONE);
		} else if(currentPlayerState == MediaStatus.PLAYER_STATE_PAUSED) {
			pauseButton.setVisibility(View.GONE);
			playButton.setVisibility(View.VISIBLE);
			bufferingSpinner.setVisibility(View.GONE);
		} else if(currentPlayerState == MediaStatus.PLAYER_STATE_BUFFERING) {
			pauseButton.setVisibility(View.GONE);
			playButton.setVisibility(View.GONE);
			bufferingSpinner.setVisibility(View.VISIBLE);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		if(remoteMediaClient != null) {
			setProgressBarUpdater();
			chromecastPlaybackProgressBar.setProgress((int)remoteMediaClient.getApproximateStreamPosition());
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		if(remoteMediaClient != null)
			remoteMediaClient.removeProgressListener(progressBarUpdater);
	}

	public void setMainActivityListener(MainActivityListener mainActivityListener) {
		this.mainActivityListener = mainActivityListener;
	}
}
