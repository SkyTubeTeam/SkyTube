/*
 * SkyTube
 * Copyright (C) 2021  Zsombor Gegesy
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

package free.rm.skytube.businessobjects.db;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import java.util.Collections;
import java.util.List;

import free.rm.skytube.R;
import free.rm.skytube.app.EventBus;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.YouTube.POJOs.ChannelView;
import free.rm.skytube.businessobjects.YouTube.POJOs.PersistentChannel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;
import free.rm.skytube.businessobjects.YouTube.VideoBlocker;
import free.rm.skytube.businessobjects.YouTube.newpipe.ChannelId;
import free.rm.skytube.businessobjects.YouTube.newpipe.NewPipeException;
import free.rm.skytube.businessobjects.YouTube.newpipe.NewPipeService;
import free.rm.skytube.businessobjects.model.Status;
import free.rm.skytube.gui.businessobjects.views.ChannelSubscriber;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * Contains database-related tasks to be carried out asynchronously.
 */
public class DatabaseTasks {
    private static final String TAG = DatabaseTasks.class.getSimpleName();

    private DatabaseTasks() {}

    /**
     * Task to retrieve channel information - from the local cache, or from the remote service if the
     * value is old or doesn't exist.
     */
    public static Maybe<PersistentChannel> getChannelInfo(@NonNull Context context,
                                                          @NonNull ChannelId channelId,
                                                          boolean staleAcceptable) {
        return Maybe.fromCallable(() -> getChannelOrRefresh(context, channelId, staleAcceptable))
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(throwable -> {
                    Log.e(TAG, "Error: " + throwable.getMessage(), throwable);
                    final String msg = (throwable.getCause() != null ? throwable.getCause() : throwable).getMessage();
                    final String toastMsg = msg != null ?
                            context.getString(R.string.could_not_get_channel_detailed, msg) :
                            context.getString(R.string.could_not_get_channel);
                    Toast.makeText(context, toastMsg, Toast.LENGTH_LONG).show();
                })
                .subscribeOn(Schedulers.io());
    }

    /**
     * Returns the cached information about the channel, or tries to retrieve it from the network.
     */
    public static PersistentChannel getChannelOrRefresh(Context context, ChannelId channelId, boolean staleAcceptable) throws NewPipeException {
        SkyTubeApp.nonUiThread();

        final SubscriptionsDb db = SubscriptionsDb.getSubscriptionsDb();
        PersistentChannel persistentChannel = db.getCachedChannel(channelId);
        final boolean needsRefresh;
        if (persistentChannel == null || TextUtils.isEmpty(persistentChannel.channel().getTitle())) {
            needsRefresh = true;
        } else if (staleAcceptable) {
            needsRefresh = false;
        } else {
            needsRefresh = persistentChannel.channel().getLastCheckTime() < System.currentTimeMillis() - (24 * 60 * 60 * 1000L);
        }
        if (needsRefresh && SkyTubeApp.isConnected(context)) {
            try {
                return NewPipeService.get().getChannelDetails(channelId, persistentChannel);
            } catch (NewPipeException newPipeException) {
                if (persistentChannel != null && persistentChannel.status() != Status.OK) {
                    Log.e(TAG, "Channel is blocked/terminated - and kept that way: "+ persistentChannel+", message:"+newPipeException.getMessage());
                    return persistentChannel;
                }
                throw newPipeException;
            }
        }
        return persistentChannel;
    }

    public static Single<List<ChannelView>> getSubscribedChannelView(Context context, @Nullable View progressBar,
                                                                     @Nullable String searchText) {
        final boolean sortChannelsAlphabetically = SkyTubeApp.getPreferenceManager()
                .getBoolean(SkyTubeApp.getStr(R.string.pref_key_subscriptions_alphabetical_order), false);
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

        // filter out for any whitelisted/blacklisted channels
        return Single.fromCallable(() -> new VideoBlocker().filterChannels(SubscriptionsDb.getSubscriptionsDb()
                .getSubscribedChannelsByText(searchText, sortChannelsAlphabetically)))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess(list -> {
                    if (progressBar != null) {
                        progressBar.setVisibility(View.INVISIBLE);
                    }
                }).onErrorReturn(error -> {
                    Log.e(TAG, "Error: " + error.getMessage(), error);
                    String msg = context.getString(R.string.could_not_get_channel_detailed, error.getMessage());
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
                    return Collections.emptyList();
                });
    }

    /**
     * A task that checks if this video is bookmarked or not. If it is bookmarked, then it will hide
     * the menu option to bookmark the video; otherwise it will hide the option to unbookmark the
     * video.
     */
    public static Disposable isVideoBookmarked(@NonNull String videoId, @NonNull Menu menu) {
        return BookmarksDb.getBookmarksDb().isVideoBookmarked(videoId)
                .subscribe(videoIsBookmarked -> {
                    // if this video has been bookmarked, hide the bookmark option and show the unbookmark option.
                    menu.findItem(R.id.bookmark_video).setVisible(!videoIsBookmarked);
                    menu.findItem(R.id.unbookmark_video).setVisible(videoIsBookmarked);
                });
    }

    public static void updateDownloadedVideoMenu(@NonNull YouTubeVideo video, @NonNull Menu menu) {
        final MenuItem downloadVideo = menu.findItem(R.id.download_video);
        downloadVideo.setVisible(false);
        if (video != null) {
            DownloadedVideosDb.getVideoDownloadsDb().isVideoDownloaded(video.getVideoId()).subscribe(isDownloaded -> {
                SkyTubeApp.uiThread();
                if (!isDownloaded) {
                    downloadVideo.setVisible(true);
                }
            });
        }
    }

    /**
     * A task that checks if the passed {@link YouTubeVideo} is marked as watched, to update the passed {@link Menu} accordingly.
     */
    public static Disposable isVideoWatched(@NonNull String videoId, @NonNull Menu menu) {
        return PlaybackStatusDb.getPlaybackStatusDb().getVideoWatchedStatusAsync(videoId)
                .subscribe(videoStatus -> {
                    boolean videoIsWatched = videoStatus != null && videoStatus.isFullyWatched();
                    // if this video has been watched, hide the set watched option and show the set unwatched option.
                    menu.findItem(R.id.mark_watched).setVisible(!videoIsWatched);
                    menu.findItem(R.id.mark_unwatched).setVisible(videoIsWatched);
                });
    }

    /**
     * A task that subscribes to / unsubscribes from a YouTube channel.
     *
     * @param subscribeToChannel  Whether the channel should be subscribed to.
     * @param subscribeButton	  The subscribe button that the user has just clicked.
     * @param context             The context to be used to show the toast, if necessary.
     * @param channelId			  The channel id the user wants to subscribe / unsubscribe.
     * @param displayToastMessage Whether or not a toast should be shown.
     */
    public static Single<Pair<PersistentChannel, DatabaseResult>> subscribeToChannel(boolean subscribeToChannel,
                                                            @Nullable ChannelSubscriber subscribeButton,
                                                            @NonNull Context context,
                                                            @NonNull ChannelId channelId,
                                                            boolean displayToastMessage) {
        return Single.fromCallable(() -> {
            PersistentChannel channel = DatabaseTasks.getChannelOrRefresh(context, channelId, true);
            SubscriptionsDb db = SubscriptionsDb.getSubscriptionsDb();
            final DatabaseResult result;
            if (subscribeToChannel) {
                result = db.subscribe(channel, channel.channel().getYouTubeVideos());
            } else {
                result = db.unsubscribe(channel);
            }
            return Pair.create(channel, result);
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess(databaseResultPair -> {
                    YouTubeChannel channel = databaseResultPair.first.channel();
                    if (databaseResultPair.second == DatabaseResult.SUCCESS) {
                        // we need to refresh the Feed tab so it shows videos from the newly subscribed (or
                        // unsubscribed) channels
                        SkyTubeApp.getSettings().setRefreshSubsFeedFromCache(true);

                        if (subscribeToChannel) {
                            // change the state of the button
                            if (subscribeButton != null)
                                subscribeButton.setSubscribedState(true);
                            // Also change the subscription state of the channel
                            channel.setUserSubscribed(true);

                            // notify about the subscription list change
                            EventBus.getInstance().notifyMainTabChanged(EventBus.SettingChange.SUBSCRIPTION_LIST_CHANGED);

                            if (displayToastMessage) {
                                Toast.makeText(context, R.string.subscribed, Toast.LENGTH_LONG).show();
                            }
                        } else {
                            // change the state of the button
                            if (subscribeButton != null)
                                subscribeButton.setSubscribedState(false);
                            // Also change the subscription state of the channel
                            channel.setUserSubscribed(false);

                            // remove the channel from the channels subscriptions list/drawer
                            EventBus.getInstance().notifyChannelRemoved(channel.getChannelId());

                            if (displayToastMessage) {
                                Toast.makeText(context, R.string.unsubscribed, Toast.LENGTH_LONG).show();
                            }
                        }
                    } else if (databaseResultPair.second == DatabaseResult.NOT_MODIFIED) {
                        if (subscribeToChannel) {
                            Toast.makeText(context, R.string.channel_already_subscribed, Toast.LENGTH_LONG).show();
                        }
                    } else {
                        String err = String.format(SkyTubeApp.getStr(R.string.error_unable_to_subscribe), channel.getId());
                        Toast.makeText(context, err, Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * A task that unsubscribes the user from all the channels at once.
     */
    public static Completable completableUnsubscribeFromAllChannels() {
        return Completable.fromAction(() ->
                SubscriptionsDb.getSubscriptionsDb().unsubscribeFromAllChannels())
                .subscribeOn(Schedulers.io());
    }
}
