package free.rm.skytube.businessobjects.YouTube;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import free.rm.skytube.R;
import free.rm.skytube.app.EventBus;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.app.Utils;
import free.rm.skytube.businessobjects.VideoCategory;
import free.rm.skytube.businessobjects.YouTube.POJOs.CardData;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeAPIKey;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubePlaylist;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;
import free.rm.skytube.businessobjects.YouTube.newpipe.ContentId;
import free.rm.skytube.businessobjects.YouTube.newpipe.NewPipeException;
import free.rm.skytube.businessobjects.YouTube.newpipe.NewPipeService;
import free.rm.skytube.businessobjects.YouTube.newpipe.PlaylistPager;
import free.rm.skytube.businessobjects.db.SubscriptionsDb;
import free.rm.skytube.businessobjects.interfaces.GetDesiredStreamListener;
import free.rm.skytube.gui.businessobjects.adapters.PlaylistsGridAdapter;
import free.rm.skytube.gui.businessobjects.adapters.VideoGridAdapter;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.CompletableSubject;

import static free.rm.skytube.app.SkyTubeApp.getContext;

/**
 * Contains YouTube-related tasks to be carried out asynchronously.
 */
public class YouTubeTasks {
    private static final String TAG = YouTubeTasks.class.getSimpleName();
    private static final Scheduler scheduler = Schedulers.from(Executors.newFixedThreadPool(4));

    private YouTubeTasks() { }

    public static Single<Integer> refreshAllSubscriptions(Context context, @Nullable Consumer<List<String>> subscriptionListConsumer, @Nullable Consumer<Integer> newVideosFound) {
        Single<List<String>>  subscriptionList = SubscriptionsDb.getSubscriptionsDb().getSubscribedChannelIdsAsync();
        if (subscriptionListConsumer!= null) {
            subscriptionList = subscriptionList.observeOn(AndroidSchedulers.mainThread())
                    .doOnSuccess(list -> subscriptionListConsumer.accept(list))
                    .observeOn(Schedulers.io());
        }
        return subscriptionList
                .flatMap(channelIds -> refreshSubscriptions(channelIds, newVideosFound))
                .doOnError(error -> {
                    SkyTubeApp.notifyUserOnError(context, error);
                })
                .doOnSuccess(changed -> {
                    Log.i("YouTubeTasks", "refreshAllSubscriptions: " + changed);
                    SkyTubeApp.getSettings().updateFeedsLastUpdateTime();
                });
    }

    public static Single<Integer> refreshSubscribedChannel(String channelId, @Nullable Consumer<Integer> newVideosFound) {
        if (NewPipeService.isPreferred() || !YouTubeAPIKey.get().isUserApiKeySet()) {
            return YouTubeTasks.getBulkSubscriptionVideos(Collections.singletonList(channelId), newVideosFound);
        } else {
            return YouTubeTasks.getChannelVideos(channelId, null, false, newVideosFound)
                    .map(items -> items.size());
        }
    }

    private static Single<Integer> refreshSubscriptions(@NonNull List<String> channelIds, @Nullable Consumer<Integer> newVideosFound) {
        if (NewPipeService.isPreferred() || !YouTubeAPIKey.get().isUserApiKeySet()) {
            return YouTubeTasks.getBulkSubscriptionVideos(channelIds, newVideosFound);
        } else {
            return YouTubeTasks.getSubscriptionVideos(channelIds, newVideosFound);
        }
    }

    /**
     * An asynchronous task that will retrieve YouTube playlists for a specific channel and display
     * them in the supplied adapter.
     */
    public static Maybe<List<YouTubePlaylist>> getChannelPlaylists(@NonNull Context ctx,
                                                                   @NonNull GetChannelPlaylists getChannelPlaylists,
                                                                   @NonNull PlaylistsGridAdapter playlistsGridAdapter,
                                                                   boolean shouldReset) {
        if (shouldReset) {
            getChannelPlaylists.reset();
            playlistsGridAdapter.clearList();
        }
        return Single.fromCallable(getChannelPlaylists::getNextPlaylists)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(throwable -> {
                    Log.e(TAG, "Error:" + throwable.getLocalizedMessage(), throwable);
                    SkyTubeApp.notifyUserOnError(ctx, throwable);
                })
                .doOnSuccess(playlistsGridAdapter::appendList)
                .onErrorComplete();
    }

    /**
     * A task that returns the videos of the channels the user has subscribed to. Used to detect if
     * new videos have been published since last time the user used the app.
     */
    private static Single<Integer> getBulkSubscriptionVideos(@NonNull List<String> channelIds, @Nullable Consumer<Integer> newVideosFound) {
        final SubscriptionsDb subscriptionsDb = SubscriptionsDb.getSubscriptionsDb();
        final AtomicBoolean changed = new AtomicBoolean(false);
        final AtomicReference<ReCaptchaException> recaptch = new AtomicReference<>();
        return Flowable.fromIterable(channelIds)
                .flatMapSingle(channelId ->
                        Single.fromCallable(() -> {
                            SkyTubeApp.nonUiThread();
                            if (recaptch.get() != null) {
                                Log.i(TAG, "Re-captcha needed, done for now");
                                return 0;
                            }
                            Map<String, Long> alreadyKnownVideos = subscriptionsDb.getSubscribedChannelVideosByChannelToTimestamp(channelId);
                            List<YouTubeVideo> newVideos = fetchVideos(subscriptionsDb, alreadyKnownVideos, channelId);
                            List<YouTubeVideo> detailedList = new ArrayList<>();
                            if (!newVideos.isEmpty()) {
                                YouTubeChannel dbChannel = subscriptionsDb.getCachedSubscribedChannel(channelId);
                                for (YouTubeVideo vid : newVideos) {
                                    YouTubeVideo details;
                                    try {
                                        details = NewPipeService.get().getDetails(vid.getId());
                                        if (vid.getPublishTimestampExact()) {
                                            details.setPublishTimestamp(vid.getPublishTimestamp());
                                            details.setPublishTimestampExact(vid.getPublishTimestampExact());
                                        }
                                        details.setChannel(dbChannel);
                                        detailedList.add(details);
                                    } catch (ReCaptchaException reCaptchaException) {
                                        recaptch.set(reCaptchaException);
                                        Log.e(TAG, String.format("ReCaptcha error: %s, open %s to solve", reCaptchaException.getMessage(), reCaptchaException.getUrl()));
                                        return 0;
                                    } catch (ExtractionException | IOException e) {
                                        String errorMsg = String.format("Error during parsing video page for id=%s, channel: %s - name: '%s' msg:%s", vid.getId(), vid.getSafeChannelId(), vid.getSafeChannelName(), e.getMessage());
                                        Log.e(TAG, errorMsg, e);
                                    }
                                }
                                changed.compareAndSet(false, true);
                                subscriptionsDb.insertVideosForChannel(detailedList, channelId);
                            }
                            return detailedList.size();
                        })
                                .subscribeOn(scheduler)
                                .observeOn(AndroidSchedulers.mainThread())
                                .doOnSuccess(newYouTubeVideos -> {
                                    if (newVideosFound != null) {
                                        newVideosFound.accept(newYouTubeVideos);
                                    }
                                    EventBus.getInstance().notifyChannelNewVideos(channelId, newYouTubeVideos);
                                })
                )
                .collect(Collectors.summingInt(Integer::intValue))
                .map(result -> {
                    ReCaptchaException reCaptchaException = recaptch.get();
                    if (reCaptchaException != null) {
                        throw reCaptchaException;
                    }
                    return result;
                }).subscribeOn(Schedulers.io());
    }

    private static List<YouTubeVideo> fetchVideos(@NonNull SubscriptionsDb subscriptionsDb,
                                                  @NonNull Map<String, Long> alreadyKnownVideos,
                                                  @NonNull String channelId) {
        try {
            List<YouTubeVideo> videos = NewPipeService.get().getVideosFromFeedOrFromChannel(channelId);
            // If we found a video which is already added to the db, no need to check the videos after,
            // assume, they are older, and already seen
            videos.removeIf(video -> {
                Long storedTs = alreadyKnownVideos.get(video.getId());
                if (storedTs != null && Boolean.TRUE.equals(video.getPublishTimestampExact()) && !storedTs.equals(video.getPublishTimestamp())) {
                    // the freshly retrieved video contains an exact, and different publish timestamp
                    subscriptionsDb.setPublishTimestamp(video);
                    Log.i(TAG, String.format("Updating publish timestamp for %s - %s with %s",
                            video.getId(), video.getTitle(), new Date(video.getPublishTimestamp())));
                }
                return storedTs != null;
            });
            return videos;
        } catch (NewPipeException e) {
            Log.e(TAG, "Error during fetching channel page for " + channelId + ",msg:" + e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Task to asynchronously get videos for a specific channel.
     */
    private static Single<List<YouTubeVideo>> getChannelVideos(@NonNull String channelId,
                                                            @Nullable Long publishedAfter,
                                                            boolean filterSubscribedVideos,
                                                            @Nullable Consumer<Integer> newVideosFound) {
        if (!YouTubeAPIKey.get().isUserApiKeySet()) {
            throw new IllegalStateException("Only valid if custom YouTube key is set!");
        }
        final SubscriptionsDb db = SubscriptionsDb.getSubscriptionsDb();
        return Single.fromCallable(() -> {
            final GetChannelVideosFull getChannelVideosInterface = new GetChannelVideosFull();
            getChannelVideosInterface.init();
            getChannelVideosInterface.setPublishedAfter(publishedAfter != null
                    ? publishedAfter : ZonedDateTime.now().minusMonths(1).toInstant().toEpochMilli());
            getChannelVideosInterface.setChannelQuery(channelId, filterSubscribedVideos);
            return getChannelVideosInterface.getNextVideos();
        })
                .onErrorReturnItem(Collections.emptyList())
                .map(videos -> {
                    List<YouTubeVideo> realVideos = new ArrayList<>(videos.size());
                    for (CardData cd : videos) {
                        if (cd instanceof YouTubeVideo) {
                            realVideos.add((YouTubeVideo) cd);
                        }
                    }
                    db.saveVideos(realVideos, channelId);
                    return realVideos;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess(newYouTubeVideos -> {
                    if (newVideosFound != null) {
                        newVideosFound.accept(newYouTubeVideos.size());
                    }
                    EventBus.getInstance().notifyChannelNewVideos(channelId, newYouTubeVideos.size());
                })
                .doOnError(throwable ->
                    Toast.makeText(getContext(),
                        String.format(getContext().getString(R.string.could_not_get_videos),
                        db.getCachedChannel(channelId).getTitle()),
                        Toast.LENGTH_LONG).show()
                );
    }

    /**
     * An asynchronous task that will retrieve a YouTube playlist for a specified playlist URL.
     */
    public static Single<YouTubePlaylist> getPlaylist(@NonNull Context context, @NonNull String playlistId) {
        return Single.fromCallable(() -> {
            final PlaylistPager pager = NewPipeService.get().getPlaylistPager(playlistId);
            final List<YouTubeVideo> firstPage = pager.getNextPageAsVideos();
            return pager.getPlaylist();
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(throwable -> SkyTubeApp.notifyUserOnError(context, throwable));
    }

    /**
     * A task that returns the videos of channel the user has subscribed too. Used to detect if new
     * videos have been published since last time the user used the app.
     */
    private static Single<Integer> getSubscriptionVideos(@NonNull List<String> channelIds, @Nullable Consumer<Integer> newVideosFound) {
        /*
         * Get the last time all subscriptions were updated, and only fetch videos that were published after this.
         * Any new channels that have been subscribed to since the last time this refresh was done will have any
         * videos published after the last published time stored in the database, so we don't need to worry about missing
         * any.
         */
        final Long publishedAfter = SkyTubeApp.getSettings().getFeedsLastUpdateTime();
        final AtomicBoolean changed = new AtomicBoolean(false);

        return Flowable.fromIterable(channelIds)
            .flatMapSingle(channelId ->
                YouTubeTasks.getChannelVideos(channelId, publishedAfter, true, newVideosFound)
                    .doOnSuccess(videos -> {
                        if (!videos.isEmpty()) {
                            changed.compareAndSet(false, true);
                        }
                        EventBus.getInstance().notifyChannelNewVideos(channelId, videos.size());
                    })
                    .doOnError(throwable ->
                        Log.e(TAG, "Interrupt in semaphore.acquire:" + throwable.getMessage(), throwable)
                    )
                )
                .collect(Collectors.summingInt(videos -> videos.size()))
                .subscribeOn(Schedulers.io());
    }

    /**
     * Task that gets a video's description.
     */
    public static Single<String> getVideoDescription(@NonNull YouTubeVideo youTubeVideo) {
        return Single.fromCallable(() -> {
            if (youTubeVideo.getDescription() != null) {
                return youTubeVideo.getDescription();
            }
            final YouTubeVideo freshDetails = NewPipeService.get().getDetails(youTubeVideo.getId());
            youTubeVideo.setDescription(freshDetails.getDescription());
            youTubeVideo.setLikeDislikeCount(freshDetails.getLikeCountNumber(), freshDetails.getDislikeCountNumber());
            return youTubeVideo.getDescription();
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .onErrorReturn(throwable -> {
                    Log.e(TAG, "Unable to get video details, where id=" + youTubeVideo.getId(), throwable);
                    return SkyTubeApp.getStr(R.string.error_get_video_desc);
                });
    }

    /**
     * An asynchronous task that will, from the given video URL, get the details of the video (e.g. video name,
     * likes, etc).
     */
    public static Maybe<YouTubeVideo> getVideoDetails(@NonNull Context context,
                                                      @NonNull ContentId content) {
        return Maybe.fromCallable(() -> NewPipeService.get().getDetails(content.getId()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(throwable -> {
                    Log.e(TAG, "Unable to get video details, where id=" + content, throwable);
                    SkyTubeApp.notifyUserOnError(context, throwable);
                })
                .onErrorComplete();
    }

    /**
     * An asynchronous task that will, from the given video URL, get the details of the video (e.g. video name,
     * likes, etc).
     */
    public static Maybe<YouTubeVideo> getVideoDetails(@NonNull Context context,
                                                      @NonNull Intent intent) {
        final ContentId content = SkyTubeApp.getUrlFromIntent(context, intent);
        Utils.isTrue(content.getType() == StreamingService.LinkType.STREAM, "Content is a video:"+content);
        return getVideoDetails(context, content);
    }

    /**
     * Task to setup the appropriate Uri for the streams for the given YouTube video.
     */
    public static Completable getDesiredStream(@NonNull YouTubeVideo youTubeVideo,
                                                    @NonNull GetDesiredStreamListener listener) {
        return Single.fromCallable(() -> NewPipeService.get().getStreamInfoByVideoId(youTubeVideo.getId()))
                .subscribeOn(Schedulers.io())
                .doOnError(listener::onGetDesiredStreamError)
                .onErrorComplete()
                .map(streamInfo -> {
                    youTubeVideo.updateFromStreamInfo(streamInfo);
                    return streamInfo;
                })
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapCompletable(streamInfo -> {
                    listener.onGetDesiredStream(streamInfo, youTubeVideo);
                    return CompletableSubject.create();
                });
    }

    /**
     * An asynchronous task that will retrieve YouTube videos and display them in the supplied Adapter.
     *
     * @param getYouTubeVideos The object that does the actual fetching of videos.
     * @param videoGridAdapter The grid adapter the videos will be added to.
     * @param swipeRefreshLayout The layout which shows animation about the refresh process.
     * @param clearList Clear the list before adding new values to it.
     * @param callback To notify the updated {@link VideoGridAdapter}
     */
    public static Maybe<List<CardData>> getYouTubeVideos(@NonNull GetYouTubeVideos getYouTubeVideos,
                                                         @NonNull VideoGridAdapter videoGridAdapter,
                                                         @Nullable SwipeRefreshLayout swipeRefreshLayout,
                                                         boolean clearList) {
        getYouTubeVideos.resetKey();
        final YouTubeChannel channel = videoGridAdapter.getYouTubeChannel();
        final Context context = videoGridAdapter.getContext();
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(true);
        }

        return Maybe.fromCallable(() -> {
            // get videos from YouTube or the database.
            List<CardData> videosList;

            if (clearList && videoGridAdapter.getCurrentVideoCategory() == VideoCategory.SUBSCRIPTIONS_FEED_VIDEOS) {
                final int currentSize = videoGridAdapter.getItemCount();
                List<CardData> result = new ArrayList<>(currentSize);
                boolean hasNew;
                do {
                    videosList = getYouTubeVideos.getNextVideos();
                    hasNew = !videosList.isEmpty();
                    result.addAll(videosList);
                } while(result.size() < currentSize && hasNew);
                videosList = result;
            } else {
                videosList = getYouTubeVideos.getNextVideos();
            }

            if (videosList != null) {
                // filter videos
                if (videoGridAdapter.getCurrentVideoCategory().isVideoFilteringEnabled()) {
                    videosList = new VideoBlocker().filter(videosList);
                }

                if (channel != null && channel.isUserSubscribed()) {
                    for (CardData video : videosList) {
                        if (video instanceof YouTubeVideo) {
                            channel.addYouTubeVideo((YouTubeVideo) video);
                        }
                    }
                    SubscriptionsDb.getSubscriptionsDb().saveChannelVideos(channel.getYouTubeVideos(), channel.getId());
                }
            }

            return videosList;
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(error -> {
                    SkyTubeApp.notifyUserOnError(context, error);
                })
                .doOnSuccess(videosList -> {
                    SkyTubeApp.notifyUserOnError(context, getYouTubeVideos.getLastException());

                    if (clearList) {
                        videoGridAdapter.clearList();
                    }
                    videoGridAdapter.appendList(videosList);
                    videoGridAdapter.notifyVideoGridUpdated();
                })
                .doOnTerminate(() -> {
                    if(swipeRefreshLayout != null) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
    }
}
