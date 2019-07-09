package free.rm.skytube.gui.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import com.squareup.picasso.Picasso;

import butterknife.BindView;
import butterknife.ButterKnife;
import free.rm.skytube.R;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannelInterface;
import free.rm.skytube.businessobjects.YouTube.Tasks.GetYouTubeChannelInfoTask;
import free.rm.skytube.gui.businessobjects.RuntimeView;
import free.rm.skytube.gui.businessobjects.views.ClickableLinksTextView;
import free.rm.skytube.gui.businessobjects.views.SubscribeButton;

/**
 * Fragment class that is used for Chromecast control. This Fragment is full screen, and can be accessed by clicking on
 * {@link ChromecastMiniControllerFragment} or dragging it up.
 */
public class ChromecastControllerFragment extends ChromecastBaseControllerFragment implements SeekBar.OnSeekBarChangeListener {
	public static final String CHROMECAST_CONTROLLER_FRAGMENT = "free.rm.skytube.CHROMECAST_CONTROLLER_FRAGMENT";

	@BindView(R.id.currentRuntime)
	RuntimeView currentPositionTextView;
	@BindView(R.id.duration)
	RuntimeView durationTextView;
	@BindView(R.id.videoImage)
	ImageView videoImage;
	@BindView(R.id.videoDescription)
	View videoDescriptionInclude;

	VideoDescriptionLayout videoDescriptionLayout;

	static class VideoDescriptionLayout {
		@BindView(R.id.video_desc_linearlayout)
		LinearLayout linearLayout;
		@BindView(R.id.video_desc_title)
		TextView title;
		@BindView(R.id.video_desc_channel_thumbnail_image_view)
		ImageView thumbnail;
		@BindView(R.id.video_desc_channel)
		TextView channel;
		@BindView(R.id.video_desc_subscribe_button)
		SubscribeButton subscribeButton;
		@BindView(R.id.video_desc_views)
		TextView views;
		@BindView(R.id.video_desc_likes_bar)
		ProgressBar likesBar;
		@BindView(R.id.video_desc_likes)
		TextView likes;
		@BindView(R.id.video_desc_dislikes)
		TextView dislikes;
		@BindView(R.id.video_desc_ratings_disabled)
		TextView ratingsDisabled;
		@BindView(R.id.video_desc_publish_date)
		TextView publishDate;
		@BindView(R.id.video_desc_description)
		ClickableLinksTextView videoDescription;
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_chromecast_controller, container);
		ButterKnife.bind(this, view);
		videoDescriptionLayout = new VideoDescriptionLayout();
		ButterKnife.bind(videoDescriptionLayout, videoDescriptionInclude);

		// Set the background color of the video description layout since by default it doesn't match the background color of the Chromecast controller
		TypedValue typedValue = new TypedValue();
		getContext().getTheme().resolveAttribute(R.attr.colorPrimary, typedValue, true);
		videoDescriptionLayout.linearLayout.setBackgroundResource(typedValue.resourceId);

		((SeekBar)chromecastPlaybackProgressBar).setOnSeekBarChangeListener(this);
		if(savedInstanceState != null) {
			setupDescription();
		}
		return view;
	}

	@Override
	public void init(RemoteMediaClient client, MediaInfo media, int position) {
		super.init(client, media, position);
		if(video != null) {
			setupDescription();
		}
		durationTextView.setMilliseconds(chromecastPlaybackProgressBar.getMax());
		if(media.getMetadata().getImages().size() > 0) {
			Picasso.with(getActivity().getApplicationContext())
							.load(media.getMetadata().getImages().get(0).getUrl().toString())
							.placeholder(R.drawable.thumbnail_default)
							.into(videoImage);
		}
	}

	private void setupDescription() {
		if(video == null)
			return;
		videoDescriptionLayout.videoDescription.setTextAndLinkify(video.getDescription());
		videoDescriptionLayout.title.setText(video.getTitle());
		videoDescriptionLayout.channel.setText(video.getChannelName());
		videoDescriptionLayout.publishDate.setText(video.getPublishDatePretty());
		videoDescriptionLayout.views.setText(video.getViewsCount());
		if (video.isThumbsUpPercentageSet()) {
			videoDescriptionLayout.likesBar.setProgress(video.getThumbsUpPercentage());
			videoDescriptionLayout.likes.setText(video.getLikeCount());
			videoDescriptionLayout.dislikes.setText(video.getDislikeCount());
		} else {
			videoDescriptionLayout.likesBar.setVisibility(View.INVISIBLE);
			videoDescriptionLayout.likes.setVisibility(View.INVISIBLE);
			videoDescriptionLayout.dislikes.setVisibility(View.INVISIBLE);
			videoDescriptionLayout.ratingsDisabled.setVisibility(View.VISIBLE);
		}


		new GetYouTubeChannelInfoTask(getActivity(), new YouTubeChannelInterface() {
			@Override
			public void onGetYouTubeChannel(final YouTubeChannel youTubeChannel) {
				if(youTubeChannel.isUserSubscribed())
					videoDescriptionLayout.subscribeButton.setUnsubscribeState();
				else
					videoDescriptionLayout.subscribeButton.setSubscribeState();
				videoDescriptionLayout.subscribeButton.setChannel(youTubeChannel);

				if (youTubeChannel != null) {
					Glide.with(getActivity())
									.load(youTubeChannel.getThumbnailNormalUrl())
									.apply(new RequestOptions().placeholder(R.drawable.channel_thumbnail_default))
									.into(videoDescriptionLayout.thumbnail);
				}
			}
		}).executeInParallel(video.getChannelId());

	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		if(fromUser) {
		} else {
			if(durationTextView.getMilliseconds() != seekBar.getMax())
				durationTextView.setMilliseconds(seekBar.getMax());
		}
		currentPositionTextView.setMilliseconds(progress);
	}

	@Override
	public void setProgress(int progress) {
		super.setProgress(progress);
		currentPositionTextView.setMilliseconds(progress);
	}

	@Override
	public void setDuration(int duration) {
		super.setDuration(duration);
		this.durationTextView.setMilliseconds(duration);
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
