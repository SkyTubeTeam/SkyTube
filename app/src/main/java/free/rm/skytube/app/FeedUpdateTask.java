/*
 * SkyTube
 * Copyright (C) 2021 Zsombor Gegesy
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
package free.rm.skytube.app;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.List;

import free.rm.skytube.R;
import free.rm.skytube.businessobjects.YouTube.YouTubeTasks;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class FeedUpdateTask {
    private static final String NOTIFICATION_CHANNEL_NAME = "SkyTube";
    private static final String TAG = "SkyTubeApp";
    private static final String NOTIFICATION_CHANNEL_ID = "subscriptionChecking";
    private static final int NOTIFICATION_ID = 1;

    private static FeedUpdateTask instance;

    public synchronized static FeedUpdateTask getInstance() {
        if (instance == null) {
            instance = new FeedUpdateTask();
        }
        return instance;
    }

    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    private int numVideosFetched = 0;
    private int numChannelsFetched = 0;
    private int numChannelsSubscribed = 0;
    private boolean refreshInProgress = false;

    public boolean isRefreshInProgress() {
        return refreshInProgress;
    }

    public synchronized boolean start(Context context) {
        if (refreshInProgress) {
            return false;
        }
        createNotificationChannel(context);
        if (!SkyTubeApp.isConnected(context)) {
            return false;
        }
        SkyTubeApp.getSettings().setRefreshSubsFeedFull(false);
        refreshInProgress = true;

        compositeDisposable.add(YouTubeTasks.refreshAllSubscriptions(context, this::processChannelIds,
                newVideosFound -> {
                    numChannelsFetched++;
                    numVideosFetched += newVideosFound;
                    showNotification();
                })
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess(newVideos -> {
                    SkyTubeApp.uiThread();
                    Log.i(TAG, "Found new videos: " + newVideos);
                    EventBus.getInstance().notifyChannelVideoFetchingFinished(newVideos > 0);
                    if (newVideos > 0) {
                        Toast.makeText(context,
                                String.format(SkyTubeApp.getStr(R.string.notification_new_videos_found),
                                        numVideosFetched), Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(context, R.string.no_new_videos_found, Toast.LENGTH_LONG).show();
                    }
                })
                .doOnTerminate(() -> {
                    refreshInProgress = false;

                    NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID);

                    EventBus.getInstance().notifySubscriptionRefreshFinished();
                }).subscribe());
        return true;
    }

    private synchronized void processChannelIds(List<String> channelIds) {
        SkyTubeApp.uiThread();
        numVideosFetched      = 0;
        numChannelsFetched    = 0;
        numChannelsSubscribed = channelIds.size();

        boolean hasChannels = numChannelsSubscribed > 0;
        EventBus.getInstance().notifyChannelsFound(hasChannels);

        if (hasChannels) {
            showNotification();
        } else {
            refreshInProgress = false;
        }
    }

    private void createNotificationChannel(Context context) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is not in the Support Library.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.i(TAG, "Create notification channel: "+NOTIFICATION_CHANNEL_ID);
            final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            final NotificationChannelCompat channel = new NotificationChannelCompat.Builder(NOTIFICATION_CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                    .setName(NOTIFICATION_CHANNEL_NAME)
                    .setSound(null, null)
                    .build();
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void showNotification() {
        final Context context = SkyTubeApp.getContext();
        final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_icon)
                .setContentTitle(context.getString(R.string.fetching_subscription_videos))
                .setContentText(String.format(context.getString(R.string.fetched_videos_from_channels),
                        numVideosFetched, numChannelsFetched, numChannelsSubscribed));


        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Log.e(TAG, "Pending intent call?");
            PendingIntent pendingIntent = PendingIntent.getActivity(context,
                    1, new Intent(), PendingIntent.FLAG_UPDATE_CURRENT);
            builder.setPriority(NotificationCompat.FLAG_ONGOING_EVENT)
                    .setContentIntent(pendingIntent);
        }

        // Issue the notification.
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

}
