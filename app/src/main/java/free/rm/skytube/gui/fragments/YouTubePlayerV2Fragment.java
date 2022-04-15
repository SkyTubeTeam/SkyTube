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

import static java.security.AccessController.getContext;
import static free.rm.skytube.gui.activities.YouTubePlayerActivity.YOUTUBE_VIDEO_OBJ;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.BaseExpandableListAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.media.AudioManagerCompat;

import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceManager;


import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.UnrecognizedInputFormatException;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;

import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.stream.StreamInfo;

import java.util.Locale;

import free.rm.skytube.R;
import free.rm.skytube.app.Settings;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.app.StreamSelectionPolicy;
import free.rm.skytube.app.Utils;
import free.rm.skytube.app.enums.Policy;
import free.rm.skytube.businessobjects.Logger;
import free.rm.skytube.businessobjects.Sponsorblock.SBSegment;
import free.rm.skytube.businessobjects.Sponsorblock.SBTasks;
import free.rm.skytube.businessobjects.Sponsorblock.SBTimeBarView;
import free.rm.skytube.businessobjects.Sponsorblock.SBVideoInfo;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;
import free.rm.skytube.businessobjects.YouTube.YouTubeTasks;
import free.rm.skytube.businessobjects.YouTube.newpipe.ContentId;
import free.rm.skytube.businessobjects.YouTube.newpipe.VideoId;
import free.rm.skytube.businessobjects.db.DatabaseTasks;
import free.rm.skytube.businessobjects.db.DownloadedVideosDb;
import free.rm.skytube.businessobjects.db.PlaybackStatusDb;
import free.rm.skytube.businessobjects.interfaces.GetDesiredStreamListener;
import free.rm.skytube.businessobjects.interfaces.PlaybackStateListener;
import free.rm.skytube.businessobjects.interfaces.YouTubePlayerActivityListener;
import free.rm.skytube.businessobjects.interfaces.YouTubePlayerFragmentInterface;
import free.rm.skytube.databinding.FragmentYoutubePlayerV2Binding;
import free.rm.skytube.databinding.VideoDescriptionBinding;
import free.rm.skytube.gui.activities.ThumbnailViewerActivity;
import free.rm.skytube.gui.businessobjects.DatasourceBuilder;
import free.rm.skytube.gui.businessobjects.MobileNetworkWarningDialog;
import free.rm.skytube.gui.businessobjects.PlaybackSpeedController;
import free.rm.skytube.gui.businessobjects.PlayerViewGestureDetector;
import free.rm.skytube.gui.businessobjects.ResumeVideoTask;
import free.rm.skytube.gui.businessobjects.SkyTubeMaterialDialog;
import free.rm.skytube.gui.businessobjects.adapters.CommentsAdapter;
import free.rm.skytube.gui.businessobjects.fragments.ImmersiveModeFragment;
import free.rm.skytube.gui.businessobjects.views.ChannelActionHandler;
import free.rm.skytube.gui.businessobjects.views.Linker;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.internal.functions.Functions;

/**
 * A fragment that holds a standalone YouTube player (version 2).
 */
@RequiresApi(api = 14)
public class YouTubePlayerV2Fragment extends ImmersiveModeFragment implements YouTubePlayerFragmentInterface, Linker.CurrentActivity {
    private static final String TAG = YouTubePlayerV2Fragment.class.getSimpleName();
    private YouTubeVideo youTubeVideo = null;
    private VideoId videoId;
    private YouTubeChannel youTubeChannel = null;

    private FragmentYoutubePlayerV2Binding fragmentBinding;
    private VideoDescriptionBinding videoDescriptionBinding;
    private TextView playbackSpeedTextView;

    private SimpleExoPlayer player;
    private long playerInitialPosition = 0;
    private DatasourceBuilder datasourceBuilder;

    private Menu menu = null;

    private BaseExpandableListAdapter commentsAdapter = null;
    private YouTubePlayerActivityListener listener = null;
    private PlayerViewGestureHandler playerViewGestureHandler;

    private PlaybackSpeedController playbackSpeedController;

    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private final ChannelActionHandler actionHandler = new ChannelActionHandler(compositeDisposable);
    private boolean videoIsPlaying;
    private PlaybackStateListener playbackStateListener = null;

    private SBVideoInfo sponsorBlockVideoInfo;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        hideNavigationBar();

        playerViewGestureHandler = new PlayerViewGestureHandler(SkyTubeApp.getSettings());

        // inflate the layout for this fragment
        fragmentBinding = FragmentYoutubePlayerV2Binding.inflate(inflater, container, false);
        videoDescriptionBinding = fragmentBinding.desContent;
        playbackSpeedTextView = fragmentBinding.getRoot().findViewById(R.id.playbackSpeed);

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
            initViews();

            // get which video we need to play...
            Intent intent = requireActivity().getIntent();
            Bundle bundle = intent.getExtras();
            if (bundle != null && bundle.getSerializable(YOUTUBE_VIDEO_OBJ) != null) {
                // ... either the video details are passed through the previous activity
                setYouTubeVideo((YouTubeVideo) bundle.getSerializable(YOUTUBE_VIDEO_OBJ));
                setUpHUDAndPlayVideo();

                fetchVideoInformations();
            } else {
                // ... or the video URL is passed to SkyTube via another Android app
                final ContentId contentId = SkyTubeApp.getUrlFromIntent(requireContext(), intent);
                openVideo(contentId);
            }
        }

        return fragmentBinding.getRoot();
    }

    private void openVideo(ContentId contentId) {
        Utils.isTrue(contentId.getType() == StreamingService.LinkType.STREAM, "Content is a video:" + contentId);
        compositeDisposable.add(YouTubeTasks.getVideoDetails(requireContext(), contentId)
            .subscribe(video -> {
                if (video == null) {
                    // invalid URL error (i.e. we are unable to decode the URL)
                    String err = String.format(getString(R.string.error_invalid_url), contentId.getCanonicalUrl());
                    Toast.makeText(getActivity(), err, Toast.LENGTH_LONG).show();

                    // log error
                    Logger.e(this, err);

                    // close the video player activity
                    closeActivity();
                } else {
                    setYouTubeVideo(video);

                    // setup the HUD and play the video
                    setUpHUDAndPlayVideo();

                    fetchVideoInformations();

                    // will now check if the video is bookmarked or not (and then update the menu
                    // accordingly)
                    compositeDisposable.add(DatabaseTasks.isVideoBookmarked(youTubeVideo.getId(), menu));
                }
            }));
    }

    protected void setYouTubeVideo(YouTubeVideo video) {
        this.youTubeVideo = video;
        this.videoId = video != null ? video.getVideoId() : null;
    }
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            Activity activity = (Activity) context;
            listener = (YouTubePlayerActivityListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException("YouTubePlayerFragment must be instantiated from an Activity that implements YouTubePlayerActivityListener");
        }
    }

    /**
     * Initialise the views.
     */
    private void initViews() {
        // setup the toolbar / actionbar
        Toolbar toolbar = fragmentBinding.getRoot().findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        // setup the player
        playerViewGestureHandler.initView();
        fragmentBinding.playerView.setOnTouchListener(playerViewGestureHandler);
        fragmentBinding.playerView.requestFocus();

        setupPlayer();

        // ensure that videos are played in their correct aspect ratio
        fragmentBinding.playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);

        videoDescriptionBinding.videoDescChannelThumbnailImageView.setOnClickListener(v -> {
            if (youTubeChannel != null) {
                SkyTubeApp.launchChannel(youTubeChannel, getActivity());
            }
        });
        fragmentBinding.commentsDrawer.setOnDrawerOpenListener(() -> {
            if (commentsAdapter == null) {
                commentsAdapter = CommentsAdapter.createAdapter(getActivity(), this, youTubeVideo.getId(),
                        fragmentBinding.commentsExpandableListView, fragmentBinding.commentsProgressBar,
                        fragmentBinding.noVideoCommentsTextView, fragmentBinding.videoCommentsAreDisabled);
            }
        });
        this.playbackSpeedController = new PlaybackSpeedController(getContext(),
                playbackSpeedTextView, player);

        //set playback speed
        float playbackSpeed = SkyTubeApp.getSettings().getDefaultPlaybackSpeed();
        playbackSpeedController.setPlaybackSpeed(playbackSpeed);

        Linker.configure(videoDescriptionBinding.videoDescDescription, this);
    }

    private synchronized void setupPlayer() {
        if (fragmentBinding.playerView.getPlayer() == null) {
            if (player == null) {
                player = createExoPlayer();
                datasourceBuilder = new DatasourceBuilder(getContext(), player);
            } else {
                Logger.i(this, ">> found already existing player, re-using it, to avoid duplicate usage");
            }
            player.addListener(new Player.EventListener() {
                @Override
                public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                    Logger.i(this, ">> onPlayerStateChanged " + playWhenReady + " state=" + playbackState);
                    videoIsPlaying = playbackState == Player.STATE_READY && playWhenReady;

                    if (videoIsPlaying) {
                        preventDeviceSleeping(true);
                        playbackSpeedController.updateMenu();
                    } else {
                        preventDeviceSleeping(false);
                    }

                    if (playbackStateListener != null) {
                        boolean videoIsPaused = playbackState == Player.STATE_READY && !playWhenReady;

                        if (videoIsPlaying) {
                            playbackStateListener.started();
                        } else if (videoIsPaused) {
                            playbackStateListener.paused();
                        } else {
                            playbackStateListener.ended();
                        }
                    }
                }

                @Override
                public void onPlayerError(ExoPlaybackException error) {
                    Logger.e(this, ":: onPlayerError " + error.getMessage(), error);

                    saveVideoPosition();

                    if (playbackStateListener != null) {
                        playbackStateListener.ended();
                    }

                    boolean askForDelete = askForDelete(error);
                    String errorMessage = error.getCause().getMessage();
                    Context ctx = YouTubePlayerV2Fragment.this.getContext();
                    new SkyTubeMaterialDialog(ctx)
                            .onNegativeOrCancel(dialog -> closeActivity())
                            .content(askForDelete ? R.string.error_downloaded_file_is_corrupted : R.string.error_video_parse_error, errorMessage)
                            .title(R.string.error_video_play)
                            .negativeText(R.string.close)
                            .positiveText(null)
                            .positiveText(askForDelete ? R.string.delete_download : 0)
                            .onPositive((dialog, which) -> {
                                if (askForDelete) {
                                    compositeDisposable.add(
                                            DownloadedVideosDb.getVideoDownloadsDb().removeDownload(ctx, youTubeVideo.getVideoId())
                                                    .subscribe(
                                                            status -> closeActivity(),
                                                            err -> Logger.e(YouTubePlayerV2Fragment.this, "Error:" + err.getMessage(), err)));
                                } else {
                                    closeActivity();
                                }
                            }).show();
                }

                private boolean askForDelete(ExoPlaybackException error) {
                    Throwable cause = error.getCause();
                    if (cause instanceof UnrecognizedInputFormatException) {
                        UnrecognizedInputFormatException uie = (UnrecognizedInputFormatException) cause;
                        return "file".equals(uie.uri.getScheme());
                    }
                    return false;
                }
            });
            player.setPlayWhenReady(true);
            player.setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);    // ensure that videos are played in their correct aspect ratio
            fragmentBinding.playerView.setPlayer(player);
        }
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
        setupInfoDisplay(youTubeVideo);

        new ResumeVideoTask(getContext(), youTubeVideo.getId(), position -> {
            playerInitialPosition = position;
            YouTubePlayerV2Fragment.this.loadVideo();
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

        setupLikeCounters(video);

        if (SkyTubeApp.getSettings().isSponsorblockEnabled()) {
            initSponsorBlock();
        }
    }

    private void setupLikeCounters(YouTubeVideo video) {
        final boolean hasLikes = video.getLikeCountNumber() != null;
        setTextAndVisibility(videoDescriptionBinding.videoDescLikes, hasLikes, video.getLikeCount());
        final boolean hasDislikes = video.getDislikeCountNumber() != null;
        setTextAndVisibility(videoDescriptionBinding.videoDescDislikes, hasDislikes, video.getDislikeCount());
        setValueAndVisibility(videoDescriptionBinding.videoDescLikesBar, video.isThumbsUpPercentageSet(), video.getThumbsUpPercentage());
        setVisibility(videoDescriptionBinding.videoDescRatingsDisabled, !hasLikes && !hasDislikes);
        if (!hasDislikes) {
            YouTubeTasks.getDislikeCountFromApi(video.getId()).subscribe(dislikeCount -> {
                video.setLikeDislikeCount(video.getLikeCountNumber(), dislikeCount);
                final boolean hasDislikesFresh = video.getDislikeCountNumber() != null;
                setTextAndVisibility(videoDescriptionBinding.videoDescDislikes, hasDislikesFresh, video.getDislikeCount());
                setVisibility(videoDescriptionBinding.videoDescRatingsDisabled, !hasLikes && !hasDislikesFresh);
            });
        }
    }

    /**
     * Retrieve the sponsorBlock information, either from the internal downloaded videos table, or from the network.
     */
    private void retrieveSponsorBlockIfPossible() {
        if (SkyTubeApp.getSettings().isSponsorblockEnabled()) {
            if (sponsorBlockVideoInfo == null) {
                sponsorBlockVideoInfo = DownloadedVideosDb.getVideoDownloadsDb().getDownloadedVideoSponsorblock(youTubeVideo.getId());
                if (sponsorBlockVideoInfo == null) {
                    sponsorBlockVideoInfo = SBTasks.retrieveSponsorblockSegmentsBk(youTubeVideo.getVideoId());
                }
                initSponsorBlock();
            }
        }
    }

    private void initSponsorBlock() {
        if (sponsorBlockVideoInfo != null) {
            Log.d(TAG, "SBInfo has loaded");
            Handler handler = new Handler(Looper.getMainLooper());
            for (SBSegment segment : sponsorBlockVideoInfo.getSegments()) {
                long startPosMs = Math.round(segment.getStartPos() * 1000);
                player.createMessage((messageType, payload) -> {
                            SBSegment payloadSegment = (SBSegment) payload;

                            handler.post(() -> {
                                SBTasks.LabelAndColor labelAndColor = SBTasks.getLabelAndColor(payloadSegment.getCategory());

                                if (labelAndColor != null) {
                                    String categoryLabel = getString(labelAndColor.label);
                                    Toast.makeText(getContext(),
                                            getString(R.string.sponsorblock_skipped, categoryLabel),
                                            Toast.LENGTH_SHORT).show();
                                } else {
                                    Log.w(TAG, "Unknown sponsorBlock category: " + payloadSegment.getCategory());
                                }
                            });

                            long pos = Math.round(payloadSegment.getEndPos() * 1000);
                            player.seekTo(pos);
                        })
                        .setHandler(handler)
                        .setPosition(startPosMs)
                        .setPayload(segment)
                        .setDeleteAfterDelivery(false)
                        .send();
            }

            SBTimeBarView sbView = fragmentBinding.getRoot().findViewById(R.id.exo_sponsorblock_progress);
            if (sbView != null) {
                sbView.setSegments(sponsorBlockVideoInfo);
            } else {
                Log.e(TAG, "SBView not found!");
            }
        } else {
            Log.d(TAG, "SBInfo not loaded yet");
        }
    }

    private void setTextAndVisibility(TextView view, boolean visible, String text) {
        setVisibility(view, visible);
        if (visible) {
            view.setText(text);
        }
    }

    private void setValueAndVisibility(ProgressBar view, boolean visible, int percentage) {
        setVisibility(view, visible);
        if (visible) {
            view.setProgress(percentage);
        }
    }

    private void setVisibility(View view, boolean visible) {
        view.setVisibility(visible ? View.VISIBLE : View.GONE);
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
     * Loads the video specified in {@link #videoId}.
     *
     * @param showMobileNetworkWarning Set to true to show the warning displayed when the user is
     *                                 using mobile network data (i.e. 4g).
     */
    private void loadVideo(boolean showMobileNetworkWarning) {
        Context ctx = getContext();
        compositeDisposable.add(
                DownloadedVideosDb.getVideoDownloadsDb().getDownloadedFileStatus(ctx, videoId)
                        .subscribe(downloadStatus -> {
                            Policy decision = Policy.ALLOW;

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
                                        playVideo(downloadStatus.getUri(), downloadStatus.getAudioUri(), null);

                                        retrieveSponsorBlockIfPossible();

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
                                                                    StreamSelectionPolicy selectionPolicy = SkyTubeApp.getSettings().getDesiredVideoResolution(false);
                                                                    StreamSelectionPolicy.StreamSelection selection = selectionPolicy.select(desiredStream);
                                                                    if (selection != null) {
                                                                        Uri uri = selection.getVideoStreamUri();
                                                                        Logger.i(YouTubePlayerV2Fragment.this, ">> PLAYING: %s, audio: %s", uri, selection.getAudioStreamUri());
                                                                        playVideo(uri, selection.getAudioStreamUri(), desiredStream);
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

    private void videoPlaybackError(String errorMessage) {
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

    /**
     * Play video.
     *
     * @param videoUri   The Uri of the video that is going to be played.
     * @param audioUri   The Uri of the audio part that is going to be played. Can be null.
     * @param streamInfo Additional information about the stream.
     */
    private void playVideo(Uri videoUri, @Nullable Uri audioUri, @Nullable StreamInfo streamInfo) {
        datasourceBuilder.play(videoUri, audioUri, streamInfo);
        if (playerInitialPosition > 0) {
            player.seekTo(playerInitialPosition);
        }
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        DatabaseTasks.updateDownloadedVideoMenu(youTubeVideo, menu);
        final MenuItem subscribeChannel = menu.findItem(R.id.subscribe_channel);
        final MenuItem openChannel = menu.findItem(R.id.open_channel);
        if (youTubeVideo != null && youTubeVideo.getChannelId() != null) {
            if (subscribeChannel != null) {
                subscribeChannel.setVisible(true);
            }
            if (openChannel != null) {
                openChannel.setVisible(true);
            }
        } else {
            if (subscribeChannel != null) {
                subscribeChannel.setVisible(false);
            }
            if (openChannel != null) {
                openChannel.setVisible(false);
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_youtube_player, menu);

        this.menu = menu;
        menu.findItem(R.id.disable_gestures).setChecked(playerViewGestureHandler.disableGestures);

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
        Context context = getContext();
        if (actionHandler.handleChannelActions(context, youTubeChannel, item.getItemId())) {
            return true;
        }
        switch (item.getItemId()) {
            case R.id.menu_reload_video:
                player.seekToDefaultPosition();
                return true;

            case R.id.menu_open_video_with:
                player.setPlayWhenReady(false);
                compositeDisposable.add(youTubeVideo.playVideoExternally(context).subscribe());
                return true;

            case R.id.share:
                player.setPlayWhenReady(false);
                youTubeVideo.shareVideo(context);
                return true;

            case R.id.copyurl:
                youTubeVideo.copyUrl(context);
                return true;

            case R.id.bookmark_video:
                compositeDisposable.add(youTubeVideo.bookmarkVideo(context, menu).subscribe());
                return true;

            case R.id.unbookmark_video:
                compositeDisposable.add(youTubeVideo.unbookmarkVideo(context, menu).subscribe());
                return true;

            case R.id.view_thumbnail:
                Intent i = new Intent(getActivity(), ThumbnailViewerActivity.class);
                i.putExtra(ThumbnailViewerActivity.YOUTUBE_VIDEO, youTubeVideo);
                startActivity(i);
                return true;

            case R.id.download_video:
                final Policy decision = new MobileNetworkWarningDialog(context)
                        .showDownloadWarning(youTubeVideo);

                if (decision == Policy.ALLOW) {
                    youTubeVideo.downloadVideo(context).subscribe();
                }
                return true;
            case R.id.disable_gestures:
                boolean disableGestures = !item.isChecked();
                item.setChecked(disableGestures);
                SkyTubeApp.getSettings().setDisableGestures(disableGestures);
                playerViewGestureHandler.setDisableGestures(disableGestures);
                return true;
            case R.id.video_repeat_toggle:
                boolean repeat = !item.isChecked();
                player.setRepeatMode(repeat ? Player.REPEAT_MODE_ALL : Player.REPEAT_MODE_OFF);
                item.setChecked(repeat);
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
    private void fetchVideoInformations() {
        // get Channel info (e.g. avatar...etc) task
        compositeDisposable.add(
                DatabaseTasks.getChannelInfo(requireContext(), youTubeVideo.getChannelId(), false)
                        .subscribe(youTubeChannel1 -> {
                            youTubeChannel = youTubeChannel1;

                            videoDescriptionBinding.videoDescSubscribeButton.setChannel(youTubeChannel);
                            if (youTubeChannel != null) {
                                Glide.with(requireContext())
                                        .load(youTubeChannel.getThumbnailUrl())
                                        .apply(new RequestOptions().placeholder(R.drawable.channel_thumbnail_default))
                                        .into(videoDescriptionBinding.videoDescChannelThumbnailImageView);

                            }
                        })
        );

        if (SkyTubeApp.getSettings().isSponsorblockEnabled()) {
            compositeDisposable.add(
                    SBTasks.retrieveSponsorblockSegmentsCtx(requireContext(), youTubeVideo.getVideoId())
                            .subscribe(segments -> {
                                Log.d(TAG, "Received SB Info with " + segments.getSegments().size() + " segments for duration of " + segments.getVideoDuration());
                                sponsorBlockVideoInfo = segments;
                                initSponsorBlock();
                            }, Functions.ON_ERROR_MISSING, () -> {
                                Log.d(TAG, "No SB info received for " + youTubeVideo.getVideoId());
                            })
            );
        }
    }

    @Override
    public void videoPlaybackStopped() {
        player.stop();
        // playerView.setPlayer(null);
        saveVideoPosition();
    }

    private void saveVideoPosition() {
        compositeDisposable.add(
                PlaybackStatusDb.getPlaybackStatusDb().setVideoPositionInBackground(youTubeVideo, player.getCurrentPosition()));
    }

    @Override
    public void onDestroy() {
        compositeDisposable.clear();
        super.onDestroy();
        // stop the player from playing (when this fragment is going to be destroyed) and clean up
        player.stop();
        player.release();
        player = null;
        fragmentBinding.playerView.setPlayer(null);
        videoDescriptionBinding.videoDescSubscribeButton.clearBackgroundTasks();
        fragmentBinding = null;
        videoDescriptionBinding = null;
    }

    @Override
    public boolean canNavigateTo(ContentId contentId) {
        if (contentId instanceof VideoId) {
            VideoId newVideoId = (VideoId) contentId;
            if (videoId.isSameContent(newVideoId)) {
                // same video, maybe different timestamp?
                Integer timestamp = newVideoId.getTimestamp();
                if (timestamp != null) {
                    player.seekTo(timestamp.longValue() * 1000L);
                }
            } else {
                openVideo(newVideoId);
            }
            return true;
        }
        return false;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * This will handle any gesture swipe event performed by the user on the player view.
     */
    class PlayerViewGestureHandler extends PlayerViewGestureDetector {
        private boolean isControllerVisible = true;
        private VideoBrightness videoBrightness;
        private float startVolumePercent = -1.0f;
        private long startVideoTime = -1;

        /**
         * Enable/Disable video gestures based on user preferences.
         */
        private boolean disableGestures;

        private static final int MAX_VIDEO_STEP_TIME = 60 * 1000;

        PlayerViewGestureHandler(Settings settings) {
            super(getContext(), settings);

            this.disableGestures = settings.isDisableGestures();
            videoBrightness = new VideoBrightness(getActivity(), disableGestures);
        }

        void initView() {
            fragmentBinding.playerView.setControllerVisibilityListener(visibility -> {
                isControllerVisible = (visibility == View.VISIBLE);
                switch (visibility) {
                    case View.VISIBLE: {
                        showNavigationBar();
                        if (fragmentBinding != null) {
                            fragmentBinding.playerView.getOverlayFrameLayout().setVisibility(View.VISIBLE);
                        }
                        break;
                    }
                    case View.GONE: {
                        hideNavigationBar();
                        if (fragmentBinding != null) {
                            fragmentBinding.playerView.getOverlayFrameLayout().setVisibility(View.GONE);
                        }
                        break;
                    }
                }
            });
        }

        @Override
        public void onCommentsGesture() {
            if (SkyTubeApp.isConnected(requireContext())) {
                fragmentBinding.commentsDrawer.animateOpen();
            } else {
                Toast.makeText(requireContext(),
                        getString(R.string.error_get_comments_no_network),
                        Toast.LENGTH_LONG).show();
            }
        }

        @Override
        public void onVideoDescriptionGesture() {
            fragmentBinding.desDrawer.animateOpen();
        }

        @Override
        public void onDoubleTap() {
            // if the user is playing a video...
            if (player.getPlayWhenReady()) {
                // pause video - without showing the controller automatically
                boolean controllerAutoshow = fragmentBinding.playerView.getControllerAutoShow();
                fragmentBinding.playerView.setControllerAutoShow(false);
                pause();
                fragmentBinding.playerView.setControllerAutoShow(controllerAutoshow);
            } else {
                // play video
                player.setPlayWhenReady(true);
                // This is to force that the automatic hiding of the controller is re-triggered.
                if (isControllerVisible) {
                    fragmentBinding.playerView.showController();
                }
            }
        }

        @Override
        public boolean onSingleTap() {
            return showOrHideHud();
        }

        /**
         * Hide or display the HUD depending if the HUD is currently visible or not.
         */
        private boolean showOrHideHud() {
            if (fragmentBinding.commentsDrawer.isOpened()) {
                fragmentBinding.commentsDrawer.animateClose();
                return !isControllerVisible;
            }

            if (fragmentBinding.desDrawer.isOpened()) {
                fragmentBinding.desDrawer.animateClose();
                return !isControllerVisible;
            }

            if (isControllerVisible) {
                fragmentBinding.playerView.hideController();
            } else {
                fragmentBinding.playerView.showController();
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
            fragmentBinding.indicatorImageView.setImageResource(R.drawable.ic_brightness);
            fragmentBinding.indicatorTextView.setText(videoBrightness.getBrightnessString());

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
                fragmentBinding.indicatorImageView.setImageResource(R.drawable.ic_forward);
            } else {
                fragmentBinding.indicatorImageView.setImageResource(R.drawable.ic_rewind);
            }
            fragmentBinding.indicatorTextView.setText(targetTimeString);

            showIndicator();

            player.seekTo(targetTime);
        }


        @Override
        public Rect getPlayerViewRect() {
            return new Rect(fragmentBinding.playerView.getLeft(), fragmentBinding.playerView.getTop(),
                    fragmentBinding.playerView.getRight(), fragmentBinding.playerView.getBottom());
        }

        private void showIndicator() {
            fragmentBinding.indicatorView.setVisibility(View.VISIBLE);
        }

        private void hideIndicator() {
            fragmentBinding.indicatorView.setVisibility(View.GONE);
        }

        /**
         * Returns a (localized) string for the given duration (in seconds).
         *
         * @param duration
         * @return a (localized) string for the given duration (in seconds).
         */
        private String formatDuration(long duration) {
            long h = duration / 3600;
            long m = (duration - h * 3600) / 60;
            long s = duration - (h * 3600 + m * 60);
            String durationValue;

            if (h == 0) {
                durationValue = String.format(Locale.getDefault(), "%1$02d:%2$02d", m, s);
            } else {
                durationValue = String.format(Locale.getDefault(), "%1$d:%2$02d:%3$02d", h, m, s);
            }

            return durationValue;
        }

        public void setDisableGestures(boolean disableGestures) {
            this.disableGestures = disableGestures;
            this.videoBrightness.setDisableGestures(disableGestures);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////


    /**
     * Adjust video's brightness.  Once the brightness is adjust, it is saved in the preferences to
     * be used when a new video is played.
     */
    private static class VideoBrightness {

        /**
         * Current video brightness.
         */
        private float brightness;
        /**
         * Initial video brightness.
         */
        private float initialBrightness;
        private boolean disableGestures;

        private static final String BRIGHTNESS_LEVEL_PREF = SkyTubeApp.getStr(R.string.pref_key_brightness_level);


        /**
         * Constructor:  load the previously saved video brightness from the preference and set it.
         *
         * @param activity Activity.
         */
        public VideoBrightness(final Activity activity, final boolean disableGestures) {
            loadBrightnessFromPreference();
            initialBrightness = brightness;
            this.disableGestures = disableGestures;

            setVideoBrightness(0, activity);
        }

        public void setDisableGestures(boolean disableGestures) {
            this.disableGestures = disableGestures;
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
         * @param activity Current activity.
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
         * @param brightness Brightness (from 0.0 to 1.0).
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
        return (int) player.getCurrentPosition();
    }

    @Override
    public boolean isPlaying() {
        return videoIsPlaying;
    }

    @Override
    public void pause() {
        player.setPlayWhenReady(false);
    }

    @Override
    public void play() {
        player.setPlayWhenReady(true);
    }

    @Override
    public void setPlaybackStateListener(final PlaybackStateListener listener) {
        playbackStateListener = listener;
    }

}
