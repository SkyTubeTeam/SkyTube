/*
 * SkyTube
 * Copyright (C) 2026  SkyTube contributors
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

package free.rm.skytube.businessobjects.download;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.ServiceCompat;
import androidx.core.content.ContextCompat;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import free.rm.skytube.R;
import free.rm.skytube.businessobjects.Logger;

/**
 * Foreground service that runs the ported NewPipe download engine.
 *
 * <p>The service owns a {@link DownloadManager} and shows one aggregate
 * notification per logical download (a video and its audio streams form one
 * logical download).  While the files are being downloaded the notification
 * shows the combined progress; once every file of the logical download has
 * finished (or one of them failed) a final, dismissible "Download complete"
 * or "Download failed" notification is posted.</p>
 */
public class DownloadService extends Service implements Handler.Callback {

    private static final String TAG = "DownloadService";

    public static final int MESSAGE_RUNNING = 0;
    public static final int MESSAGE_PAUSED = 1;
    public static final int MESSAGE_FINISHED = 2;
    public static final int MESSAGE_ERROR = 3;
    public static final int MESSAGE_DELETED = 4;
    public static final int MESSAGE_PROGRESS = 5;

    private static final String ACTION_RUN = "free.rm.skytube.businessobjects.download.RUN";
    private static final String ACTION_TASK_FINISHED =
            "free.rm.skytube.businessobjects.download.TASK_FINISHED";
    private static final String ACTION_TASK_FAILED =
            "free.rm.skytube.businessobjects.download.TASK_FAILED";
    private static final String EXTRA_MISSION = "DownloadService.extra.mission";

    private static final String NOTIFICATION_CHANNEL = "downloads";
    private static final int FOREGROUND_NOTIFICATION_ID = 2000;
    /** First id of the final (dismissible) notifications, one per logical download. */
    private static final int FIRST_GROUP_NOTIFICATION_ID = 2100;
    /**
     * How long a group whose files cannot be downloaded any more (no mission
     * running or queued for it) is given to reach its end state before it is
     * resolved, so the notification never stays "in progress" forever.
     */
    private static final long STALL_CHECK_DELAY_MS = 10_000;

    private static final Map<String, MissionCompletionListener> completionListeners =
            new ConcurrentHashMap<>();

    private static volatile boolean serviceRunning = false;

    /**
     * Logical downloads that already got their final notification in this
     * process, so a restored group is not announced twice.
     */
    private static final Set<String> notifiedGroups = ConcurrentHashMap.newKeySet();
    private static final AtomicInteger nextGroupNotificationId =
            new AtomicInteger(FIRST_GROUP_NOTIFICATION_ID);

    private DownloadManager manager;
    private Handler handler;
    private NotificationManager notificationManager;
    private PowerManager.WakeLock wakeLock;
    private WifiManager.WifiLock wifiLock;
    private boolean foreground = false;
    private long lastNotificationUpdate = 0;
    private DownloadNotification notification;
    /** Pending "resolve this group" checks, one per group id. */
    private final Map<String, Runnable> groupStallChecks = new HashMap<>();

    private final BroadcastReceiver connectivityReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (manager != null) {
                manager.onNetworkChanged();
                syncState();
            }
        }
    };

    /**
     * Starts downloading the given mission and invokes the listener once the
     * download completes or fails.
     */
    public static void startDownload(@NonNull Context context,
                                     @NonNull DownloadMission mission,
                                     @Nullable MissionCompletionListener listener) {
        if (listener != null && mission.targetFile != null) {
            completionListeners.put(mission.targetFile.getAbsolutePath(), listener);
        }

        Intent intent = new Intent(context, DownloadService.class)
                .setAction(ACTION_RUN)
                .putExtra(EXTRA_MISSION, mission);

        try {
            if (serviceRunning) {
                context.startService(intent);
            } else {
                ContextCompat.startForegroundService(context, intent);
            }
        } catch (RuntimeException e) {
            if (mission.targetFile != null) {
                completionListeners.remove(mission.targetFile.getAbsolutePath());
            }
            throw e;
        }
    }

    /**
     * Reports the outcome of a download task that did not run through the
     * download engine (e.g. the file was already on disk, or starting the
     * download failed) so that the aggregate notification of the related
     * download can reach its final state.
     *
     * <p>The completion listener of the task is not involved: the caller
     * already handled the outcome.</p>
     */
    public static void reportTaskResult(@NonNull Context context,
                                        @NonNull DownloadMission mission,
                                        boolean finished) {
        Intent intent = new Intent(context, DownloadService.class)
                .setAction(finished ? ACTION_TASK_FINISHED : ACTION_TASK_FAILED)
                .putExtra(EXTRA_MISSION, mission);

        try {
            if (serviceRunning) {
                context.startService(intent);
            } else {
                ContextCompat.startForegroundService(context, intent);
            }
        } catch (RuntimeException e) {
            Logger.e(TAG, "Cannot report the download task result", e);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        handler = new Handler(Looper.getMainLooper(), this);
        notificationManager = ContextCompat.getSystemService(getApplicationContext(), NotificationManager.class);

        createNotificationChannel();

        manager = new DownloadManager(this,
                (what, mission) -> {
                    Handler currentHandler = handler;
                    if (currentHandler != null) {
                        currentHandler.obtainMessage(what, mission).sendToTarget();
                    }
                },
                new DownloadMission.Listener() {
                    @Override
                    public void onProgress(DownloadMission mission) {
                        Handler currentHandler = handler;
                        if (currentHandler != null) {
                            currentHandler.obtainMessage(MESSAGE_PROGRESS, mission).sendToTarget();
                        }
                    }

                    @Override
                    public void onFinished(DownloadMission mission) {
                        // handled by the MESSAGE_FINISHED message
                    }

                    @Override
                    public void onError(DownloadMission mission) {
                        // handled by the MESSAGE_ERROR message
                    }
                });

        notification = new DownloadNotification();
        restorePendingMissions();
        serviceRunning = true;

        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        ContextCompat.registerReceiver(this, connectivityReceiver, filter,
                ContextCompat.RECEIVER_EXPORTED);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // A service that was started with startForegroundService (or that is
        // restarted after the process died) must re-enter the foreground
        // state within a few seconds, otherwise Android kills the process.
        // syncState() below may immediately leave the foreground again when
        // there is nothing left to download.
        if (!foreground) {
            startForeground(FOREGROUND_NOTIFICATION_ID, buildNotification());
            foreground = true;
            lastNotificationUpdate = SystemClock.elapsedRealtime();
        }

        if (intent == null) {
            // The process was killed (or the system restarted the service):
            // resume everything that is still pending.
            if (manager != null) {
                manager.startAllMissions();
            }
            restorePendingMissions();
            syncState();
            return START_NOT_STICKY;
        }

        if (ACTION_RUN.equals(intent.getAction())) {
            DownloadMission mission = getMission(intent);
            if (mission != null) {
                notification.addMission(mission);
                manager.startMission(mission);
                manager.startAllMissions();
            }
            syncState();
            return START_NOT_STICKY;
        }

        if (ACTION_TASK_FINISHED.equals(intent.getAction())
                || ACTION_TASK_FAILED.equals(intent.getAction())) {
            DownloadMission mission = getMission(intent);
            if (mission != null) {
                if (ACTION_TASK_FAILED.equals(intent.getAction())) {
                    notification.markFailed(mission);
                } else {
                    notification.markFinished(mission);
                }
            }
            syncState();
        }

        return START_NOT_STICKY;
    }

    @SuppressWarnings("deprecation")
    @Nullable
    private DownloadMission getMission(Intent intent) {
        try {
            return (DownloadMission) intent.getSerializableExtra(EXTRA_MISSION);
        } catch (Exception e) {
            Logger.e(TAG, "Cannot read the download mission from the intent", e);
            return null;
        }
    }

    @Override
    public void onDestroy() {
        try {
            unregisterReceiver(connectivityReceiver);
        } catch (Exception e) {
            // already unregistered
        }

        if (handler != null) {
            for (Runnable check : groupStallChecks.values()) {
                handler.removeCallbacks(check);
            }
            handler.removeCallbacksAndMessages(null);
        }
        groupStallChecks.clear();

        if (manager != null) {
            manager.pauseAllMissions(true);
            for (DownloadMission mission : manager.getPendingMissions()) {
                mission.handler = null;
                mission.setListener(null);
            }
            manager = null;
        }

        if (notification != null) {
            notification.clear();
            notification = null;
        }

        releaseLocks();
        wakeLock = null;
        wifiLock = null;
        notificationManager = null;

        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE);
        foreground = false;
        serviceRunning = false;

        // Drop any completion listeners that are still registered: the
        // service is going away and will not be able to report their outcome.
        completionListeners.clear();

        handler = null;

        super.onDestroy();
    }

    /**
     * Brings the service lifecycle and the notifications in sync with the
     * state of the download engine:
     *
     * <ul>
     *   <li>tasks whose file is already on disk count as finished, even if
     *       the engine did not report the completion,</li>
     *   <li>every logical download that reached its end state (all files
     *       downloaded, or a failure) gets its final, dismissible
     *       notification and is removed from the aggregate one,</li>
     *   <li>groups that can no longer make progress are resolved after a
     *       grace period, so the notification never stays "in progress"
     *       forever,</li>
     *   <li>the service stays in the foreground as long as something is
     *       still being downloaded.</li>
     * </ul>
     */
    private void syncState() {
        if (manager == null || notification == null) {
            stopSelf();
            return;
        }

        markFinishedOnDisk();
        postTerminalGroupNotifications();

        boolean active = hasActiveMissions()
                || notification.hasIncompleteGroups();
        updateForegroundState(active);
        updateNotification();
        updateLocks();
        scheduleGroupStallChecks();

        if (!active) {
            stopSelf();
        }
    }

    /**
     * Treats a task as finished when its file is already on disk, even if
     * the engine did not report the completion (e.g. the process died right
     * after the file was renamed, or the file was present before the
     * download was requested).
     */
    private void markFinishedOnDisk() {
        for (DownloadMission mission : notification.getMissions()) {
            if (mission.targetFile == null || mission.getPartFile().exists()) {
                continue;
            }
            if (mission.targetFile.exists()) {
                notification.markFinished(mission);
            }
        }
    }

    /**
     * Posts a dismissible final notification for every logical download that
     * reached its end state, and removes it from the aggregate notification.
     */
    private void postTerminalGroupNotifications() {
        if (notificationManager == null) {
            return;
        }

        for (DownloadNotification.Group group : notification.getGroups()) {
            if (!group.isTerminal() || notifiedGroups.contains(group.id)) {
                continue;
            }
            notifiedGroups.add(group.id);

            Context appContext = getApplicationContext();
            String title = group.getTitle().trim().isEmpty()
                    ? appContext.getString(R.string.downloads)
                    : group.getTitle();
            Notification groupNotification = new NotificationCompat.Builder(appContext, NOTIFICATION_CHANNEL)
                    .setSmallIcon(R.drawable.ic_notification_icon)
                    .setContentTitle(title)
                    .setContentText(appContext.getString(group.isFailed()
                            ? R.string.download_notification_failed
                            : R.string.download_notification_complete))
                    .setAutoCancel(true)
                    .build();

            notificationManager.notify(nextGroupNotificationId.getAndIncrement(), groupNotification);
            notification.removeGroup(group.id);
        }
    }

    /**
     * A group whose files cannot be downloaded any more (no mission is
     * running or queued for it) will never reach its end state on its own -
     * e.g. the process was restarted and one file of a video/audio pair is
     * no longer tracked.  Resolve such groups after a grace period, so the
     * notification does not stay "in progress" forever.
     */
    private void scheduleGroupStallChecks() {
        if (handler == null) {
            return;
        }
        for (DownloadNotification.Group group : notification.getGroups()) {
            if (group.isTerminal()
                    || hasActiveMissionsFor(group.id)
                    || groupStallChecks.containsKey(group.id)) {
                continue;
            }

            Runnable check = () -> {
                groupStallChecks.remove(group.id);
                DownloadNotification.Group current = notification.getGroup(group.id);
                if (current != null && !current.isTerminal()
                        && !hasActiveMissionsFor(group.id)) {
                    // Nothing is running or queued for this group any more,
                    // the missing files will not arrive on their own.  The
                    // files that are on disk are usable, so treat the group
                    // as done.
                    notification.markGroupComplete(group.id);
                }
                syncState();
            };
            groupStallChecks.put(group.id, check);
            handler.postDelayed(check, STALL_CHECK_DELAY_MS);
        }
    }

    /**
     * @return true if at least one mission of the given group is running or
     *         queued in the download engine.
     */
    private boolean hasActiveMissionsFor(@NonNull String groupId) {
        for (DownloadMission mission : manager.getPendingMissions()) {
            if (groupId.equals(mission.notificationGroupId)
                    && (mission.running || mission.enqueued)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public boolean handleMessage(@NonNull Message msg) {
        if (msg.obj == null) {
            syncState();
            return true;
        }
        DownloadMission mission = (DownloadMission) msg.obj;

        switch (msg.what) {
            case MESSAGE_PROGRESS:
                notification.addMission(mission);
                break;
            case MESSAGE_RUNNING:
                notification.addMission(mission);
                break;
            case MESSAGE_PAUSED:
                break;
            case MESSAGE_FINISHED:
                manager.setFinished(mission);
                notification.markFinished(mission);
                notifyCompletionListener(mission, null);
                break;
            case MESSAGE_ERROR:
                if (manager.canRetry(mission)) {
                    mission.enqueued = true;
                    handler.postDelayed(() -> manager.resumeMission(mission), 2000);
                } else {
                    notification.markFailed(mission);
                    notifyCompletionListener(mission, mission.errObject != null
                            ? mission.errObject
                            : new RuntimeException("Download failed with error code " + mission.errCode));
                    manager.removePending(mission);
                }
                break;
            case MESSAGE_DELETED:
                notification.removeMission(mission);
                break;
            default:
                break;
        }

        syncState();

        return true;
    }

    /**
     * Keeps the CPU and Wi-Fi awake while at least one mission is running.
     */
    private void updateLocks() {
        if (manager != null && manager.getRunningMissionsCount() > 0) {
            acquireLocks();
        } else {
            releaseLocks();
        }
    }

    private void acquireLocks() {
        if (wakeLock == null) {
            PowerManager powerManager =
                    (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
            if (powerManager != null) {
                wakeLock = powerManager.newWakeLock(
                        PowerManager.PARTIAL_WAKE_LOCK, TAG + ":download");
                wakeLock.setReferenceCounted(false);
            }
        }
        if (wakeLock != null && !wakeLock.isHeld()) {
            wakeLock.acquire();
        }

        if (wifiLock == null) {
            WifiManager wifiManager =
                    (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                wifiLock = wifiManager.createWifiLock(
                        WifiManager.WIFI_MODE_FULL_HIGH_PERF, TAG + ":download");
                wifiLock.setReferenceCounted(false);
            }
        }
        if (wifiLock != null && !wifiLock.isHeld()) {
            wifiLock.acquire();
        }
    }

    private void releaseLocks() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        if (wifiLock != null && wifiLock.isHeld()) {
            wifiLock.release();
        }
    }

    private void notifyCompletionListener(@NonNull DownloadMission mission, @Nullable Exception error) {
        if (mission.targetFile == null) {
            return;
        }

        MissionCompletionListener listener =
                completionListeners.remove(mission.targetFile.getAbsolutePath());
        if (listener == null) {
            return;
        }

        if (error == null) {
            listener.onCompleted(mission);
        } else {
            listener.onFailed(mission, error);
        }
    }

    private void createNotificationChannel() {
        Context appContext = getApplicationContext();
        NotificationChannelCompat channel = new NotificationChannelCompat
                .Builder(NOTIFICATION_CHANNEL, NotificationManagerCompat.IMPORTANCE_LOW)
                .setName(appContext.getString(R.string.download_notification_channel_name))
                .setDescription(appContext.getString(R.string.download_notification_channel_description))
                .build();

        NotificationManagerCompat.from(appContext).createNotificationChannel(channel);
    }

    private void updateForegroundState(boolean state) {
        if (state == foreground) {
            if (state) {
                updateNotification();
            }
            return;
        }

        if (state) {
            startForeground(FOREGROUND_NOTIFICATION_ID, buildNotification());
            foreground = true;
        } else {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE);
            foreground = false;
        }
    }

    private void updateNotification() {
        if (!foreground || notificationManager == null) {
            return;
        }

        long now = SystemClock.elapsedRealtime();
        if (now - lastNotificationUpdate < 500) {
            return;
        }
        lastNotificationUpdate = now;

        notificationManager.notify(FOREGROUND_NOTIFICATION_ID, buildNotification());
    }

    @NonNull
    private Notification buildNotification() {
        Context appContext = getApplicationContext();
        String title = notification.getTitle();
        if (title == null || title.trim().isEmpty()) {
            title = appContext.getString(R.string.downloads);
        }

        int progress = notification.getProgress();
        String text;
        if (progress >= 0) {
            text = appContext.getString(R.string.download_notification_progress_aggregate,
                    DownloadUtils.formatBytes(notification.totalDone()),
                    DownloadUtils.formatBytes(notification.totalLength()),
                    progress);
        } else if (notification.totalDone() > 0) {
            text = appContext.getString(R.string.download_notification_downloaded,
                    DownloadUtils.formatBytes(notification.totalDone()));
        } else {
            text = appContext.getString(R.string.download_notification_waiting);
        }

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(appContext, NOTIFICATION_CHANNEL)
                        .setSmallIcon(R.drawable.ic_notification_icon)
                        .setContentTitle(title)
                        .setContentText(text)
                        .setOngoing(true)
                        .setOnlyAlertOnce(true);

        if (progress >= 0) {
            builder.setProgress(100, progress, false);
        } else {
            builder.setProgress(100, 0, true);
        }

        return builder.build();
    }

    private boolean hasActiveMissions() {
        return manager != null
                && (manager.getRunningMissionsCount() > 0 || manager.hasPendingMissions());
    }

    private void restorePendingMissions() {
        if (manager == null || notification == null) {
            return;
        }
        for (DownloadMission mission : manager.getPendingMissions()) {
            notification.addMission(mission);
        }
    }

    /**
     * Callback invoked when a mission started through
     * {@link #startDownload(Context, DownloadMission, MissionCompletionListener)}
     * finishes.
     */
    public interface MissionCompletionListener {
        void onCompleted(@NonNull DownloadMission mission);

        void onFailed(@NonNull DownloadMission mission, @NonNull Exception error);
    }
}
