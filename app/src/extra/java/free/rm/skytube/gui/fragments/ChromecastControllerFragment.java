package free.rm.skytube.gui.fragments;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;

import free.rm.skytube.R;
import free.rm.skytube.businessobjects.db.DatabaseTasks;
import free.rm.skytube.databinding.FragmentChromecastControllerBinding;
import free.rm.skytube.databinding.VideoDescriptionBinding;
import free.rm.skytube.gui.businessobjects.views.Linker;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

/**
 * Fragment class that is used for Chromecast control. This Fragment is full screen, and can be accessed by clicking on
 * {@link ChromecastMiniControllerFragment} or dragging it up.
 */
public class ChromecastControllerFragment extends ChromecastBaseControllerFragment implements SeekBar.OnSeekBarChangeListener {
	public static final String CHROMECAST_CONTROLLER_FRAGMENT = "free.rm.skytube.CHROMECAST_CONTROLLER_FRAGMENT";

	private FragmentChromecastControllerBinding fragmentBinding;
	private VideoDescriptionBinding videoDescriptionBinding;

	private final CompositeDisposable compositeDisposable = new CompositeDisposable();

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		fragmentBinding = FragmentChromecastControllerBinding.inflate(inflater, container, false);
		videoDescriptionBinding = fragmentBinding.videoDescription;

		chromecastPlaybackProgressBar = fragmentBinding.chromecastPlaybackProgressBar;
		playButton = fragmentBinding.playButton;
		pauseButton = fragmentBinding.pauseButton;
		forwardButton = fragmentBinding.forwardButton;
		rewindButton = fragmentBinding.rewindButton;
		stopButton = fragmentBinding.stopButton;
		bufferingSpinner = fragmentBinding.bufferingSpinner;

		// Set the background color of the video description layout since by default it doesn't match the background color of the Chromecast controller
		TypedValue typedValue = new TypedValue();
		getContext().getTheme().resolveAttribute(R.attr.colorPrimary, typedValue, true);
		videoDescriptionBinding.videoDescLinearlayout.setBackgroundResource(typedValue.resourceId);

		Linker.configure(videoDescriptionBinding.videoDescDescription);

		fragmentBinding.chromecastPlaybackProgressBar.setOnSeekBarChangeListener(this);
		if(savedInstanceState != null) {
			setupDescription();
		}
		return fragmentBinding.getRoot();
	}

	@Override
	public void onDestroy() {
		compositeDisposable.clear();
		super.onDestroy();
	}

	@Override
	public void init(RemoteMediaClient client, MediaInfo media, int position) {
		super.init(client, media, position);
		if(video != null) {
			setupDescription();
		}
		fragmentBinding.duration.setMilliseconds(chromecastPlaybackProgressBar.getMax());
		if(!media.getMetadata().getImages().isEmpty()) {
			Glide.with(getContext())
							.load(media.getMetadata().getImages().get(0).getUrl().toString())
							.apply(new RequestOptions().placeholder(R.drawable.thumbnail_default))
							.into(fragmentBinding.videoImage);
		}
	}

	private void setupDescription() {
		if(video == null)
			return;
		Linker.setTextAndLinkify(videoDescriptionBinding.videoDescDescription, video.getDescription());

		videoDescriptionBinding.videoDescTitle.setText(video.getTitle());
		videoDescriptionBinding.videoDescChannel.setText(video.getChannelName());
		videoDescriptionBinding.videoDescPublishDate.setText(video.getPublishDatePretty());
		videoDescriptionBinding.videoDescViews.setText(video.getViewsCount());
		if (video.isThumbsUpPercentageSet()) {
			videoDescriptionBinding.videoDescLikesBar.setProgress(video.getThumbsUpPercentage());
			videoDescriptionBinding.videoDescLikes.setText(video.getLikeCount());
			videoDescriptionBinding.videoDescDislikes.setText(video.getDislikeCount());
		} else {
			videoDescriptionBinding.videoDescLikesBar.setVisibility(View.INVISIBLE);
			videoDescriptionBinding.videoDescLikes.setVisibility(View.INVISIBLE);
			videoDescriptionBinding.videoDescDislikes.setVisibility(View.INVISIBLE);
			videoDescriptionBinding.videoDescRatingsDisabled.setVisibility(View.VISIBLE);
		}

		compositeDisposable.add(
				DatabaseTasks.getChannelInfo(requireContext(), video.getChannelId(), false)
						.subscribe(youTubeChannel -> {
							if (youTubeChannel.isUserSubscribed())
								videoDescriptionBinding.videoDescSubscribeButton.setUnsubscribeState();
							else
								videoDescriptionBinding.videoDescSubscribeButton.setSubscribeState();
							videoDescriptionBinding.videoDescSubscribeButton.setChannel(youTubeChannel);

							Glide.with(requireContext())
									.load(youTubeChannel.getThumbnailUrl())
									.apply(new RequestOptions().placeholder(R.drawable.channel_thumbnail_default))
									.into(videoDescriptionBinding.videoDescChannelThumbnailImageView);
						})
		);
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		if (!fromUser && fragmentBinding.duration.getMilliseconds() != seekBar.getMax())
			fragmentBinding.duration.setMilliseconds(seekBar.getMax());
		fragmentBinding.currentRuntime.setMilliseconds(progress);
	}

	@Override
	public void setProgress(int progress) {
		super.setProgress(progress);
		fragmentBinding.currentRuntime.setMilliseconds(progress);
	}

	@Override
	public void setDuration(int duration) {
		super.setDuration(duration);
		fragmentBinding.duration.setMilliseconds(duration);
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		isSeeking = true;
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		isSeeking = false;
		remoteMediaClient.seek(seekBar.getProgress());
		if(remoteMediaClient.getPlayerState() == MediaStatus.PLAYER_STATE_PAUSED)
			remoteMediaClient.play();
	}

	@Override
	protected long getProgressBarPeriod() {
		return 1000;
	}
}
