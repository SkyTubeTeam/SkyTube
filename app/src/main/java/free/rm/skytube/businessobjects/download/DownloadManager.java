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

import android.app.DownloadManager.Request;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import free.rm.skytube.businessobjects.Logger;

/**
 * Keeps track of the pending downloads, restores them after a process restart
 * and decides when a mission is allowed to run.
 *
 * <p>Ported from NewPipe's {@code us.shandian.giga.service.DownloadManager},
 * adapted to SkyTube's plain file based storage.</p>
 */
public class DownloadManager {
    private static final String TAG = "DownloadManager";

    private static final String DOWNLOADS_METADATA_FOLDER = "pending_downloads";

    private final Context context;
    private final File pendingMissionsDir;
    private final DownloadMission.HandlerThreadFactory handlerFactory;
    private final DownloadMission.Listener missionListener;

    private final ArrayList<DownloadMission> missionsPending = new ArrayList<>();
    private final ArrayList<FinishedMission> missionsFinished = new ArrayList<>();

    private int maxRetry = 3;

    DownloadManager(@NonNull Context context,
                    @NonNull DownloadMission.HandlerThreadFactory handlerFactory,
                    @NonNull DownloadMission.Listener missionListener) {
        this.context = context.getApplicationContext();
        this.handlerFactory = handlerFactory;
        this.missionListener = missionListener;
        this.pendingMissionsDir = getPendingDir(this.context);

        loadPendingMissions();
    }

    /**
     * Returns (and creates if needed) the directory that stores the pending
     * download metadata.
     */
    @NonNull
    public static File getPendingDir(@NonNull Context context) {
        File dir = context.getExternalFilesDir(DOWNLOADS_METADATA_FOLDER);
        if (dir != null && DownloadUtils.mkdir(dir, true)) {
            return dir;
        }

        dir = new File(context.getFilesDir(), DOWNLOADS_METADATA_FOLDER);
        if (DownloadUtils.mkdir(dir, true)) {
            return dir;
        }

        throw new RuntimeException("Path to pending downloads is not accessible");
    }

    /**
     * Checks whether there is a resumable (not finished) mission for the given
     * target file. This is used by {@code FileDownloader} to avoid treating a
     * partially downloaded file as complete.
     */
    public static boolean hasPendingMission(@NonNull Context context, @NonNull File targetFile) {
        try {
            File metadata = DownloadUtils.getMetadataFile(getPendingDir(context), targetFile);
            DownloadMission mission = DownloadUtils.readFromFile(metadata);
            if (mission == null) {
                DownloadUtils.deleteQuietly(metadata);
                return false;
            }

            if (mission.isFinished()
                    || (mission.getTargetFile() != null && mission.getTargetFile().exists()
                            && !mission.getPartFile().exists())) {
                DownloadUtils.deleteQuietly(metadata);
                return false;
            }

            return true;
        } catch (RuntimeException e) {
            Logger.e(TAG, "Cannot check the pending download state", e);
            return false;
        }
    }

    private void loadPendingMissions() {
        File[] subs = pendingMissionsDir.listFiles();
        if (subs == null) {
            return;
        }

        for (File sub : subs) {
            if (!sub.isFile() || !sub.getName().endsWith(".meta")) {
                continue;
            }

            DownloadMission mission = DownloadUtils.readFromFile(sub);
            if (mission == null) {
                DownloadUtils.deleteQuietly(sub);
                continue;
            }

            if (!prepareLoadedMission(mission, sub)) {
                continue;
            }

            missionsPending.add(mission);
        }

        if (missionsPending.size() > 1) {
            Collections.sort(missionsPending, Comparator.comparingLong(Mission::getTimestamp));
        }
    }

    /**
     * Start a new download mission, or resume an existing one for the same
     * target file.
     */
    synchronized void startMission(@NonNull DownloadMission mission) {
        DownloadMission existing = findPendingByTarget(mission.getTargetFile());
        if (existing == null) {
            existing = loadPendingFromMetadata(mission.getTargetFile());
        }

        if (existing != null) {
            // Use the freshly extracted URL, in case the previous one expired.
            boolean reset = existing.getUrls() == null || existing.getUrls().length != mission.getUrls().length;
            // A mission that already finished (or that the engine considers
            // finished) cannot be restarted as is: forget its progress so it
            // can be downloaded from the beginning.
            reset = reset || existing.isFinished();
            if (reset) {
                existing.setUrls(mission.getUrls());
                existing.setOffsets(new long[mission.getUrls().length]);
                existing.resetState(true, false, DownloadMission.ERROR_NOTHING);
            } else {
                existing.setUrls(mission.getUrls());
            }
            existing.setTitle(mission.getTitle());
            if (mission.getNotificationGroupId() != null) {
                existing.setNotificationGroupId(mission.getNotificationGroupId());
            }
            if (mission.getNotificationGroupTitle() != null) {
                existing.setNotificationGroupTitle(mission.getNotificationGroupTitle());
            }
            if (mission.getNotificationGroupSize() > 0) {
                existing.setNotificationGroupSize(mission.getNotificationGroupSize());
            }
            existing.setThreadCount(mission.getThreadCount());
            existing.setKind(mission.getKind());
            existing.setAllowedOverRoaming(mission.getAllowedOverRoaming());
            existing.setAllowedNetworkTypes(mission.getAllowedNetworkTypes());
            if (mission.getNearLength() > 0) {
                existing.setNearLength(mission.getNearLength());
            }
            existing.setMaxRetry(maxRetry);
            existing.setHandler(handlerFactory);
            existing.setListener(missionListener);
            existing.setEnqueued(true);

            if (!existing.getRunning() && canDownloadInCurrentNetwork(existing)) {
                existing.start();
            } else {
                existing.writeThisToFile();
            }
            return;
        }

        mission.setTimestamp(System.currentTimeMillis());
        mission.setMaxRetry(maxRetry);
        mission.setHandler(handlerFactory);
        mission.setListener(missionListener);

        File metadata = DownloadUtils.getMetadataFile(pendingMissionsDir, mission.getTargetFile());
        if (!DownloadUtils.mkdir(metadata.getParentFile(), true)) {
            mission.notifyError(DownloadMission.ERROR_PATH_CREATION, null);
            return;
        }
        mission.setMetadata(metadata);

        missionsPending.add(mission);

        // Persist before starting, so an unexpected process death does not
        // forget the mission.
        DownloadUtils.writeToFile(metadata, mission);

        if (canDownloadInCurrentNetwork(mission)) {
            mission.start();
        }
    }

    @Nullable
    private DownloadMission loadPendingFromMetadata(@NonNull File targetFile) {
        File metadata = DownloadUtils.getMetadataFile(pendingMissionsDir, targetFile);
        DownloadMission mission = DownloadUtils.readFromFile(metadata);
        if (mission == null) {
            return null;
        }

        if (!prepareLoadedMission(mission, metadata)) {
            return null;
        }

        missionsPending.add(mission);
        return mission;
    }

    /**
     * Validates a mission restored from disk and repairs its state when the
     * partial file disappeared.
     *
     * @return true if the mission should be kept, false if it is already
     * finished or cannot be used anymore
     */
    private boolean prepareLoadedMission(@NonNull DownloadMission mission, @NonNull File metadata) {
        if (mission.getTargetFile() == null) {
            DownloadUtils.deleteQuietly(metadata);
            return false;
        }

        File partFile = mission.getPartFile();
        if (!partFile.exists() && mission.getTargetFile().exists()) {
            // The file was completed, but the metadata was not removed (e.g.
            // the process died right after the rename).
            DownloadUtils.deleteQuietly(metadata);
            return false;
        }

        if (mission.isFinished()) {
            DownloadUtils.deleteQuietly(metadata);
            return false;
        }

        if (mission.isInitialized() && !partFile.exists()) {
            // The progress was lost; start this mission from scratch.
            mission.resetState(true, false, DownloadMission.ERROR_PROGRESS_LOST);
        }

        mission.setThreads(new Thread[0]);
        mission.setMetadata(metadata);
        mission.setMaxRetry(maxRetry);
        mission.setHandler(handlerFactory);
        mission.setListener(missionListener);

        return true;
    }

    @Nullable
    private DownloadMission findPendingByTarget(@NonNull File targetFile) {
        for (DownloadMission mission : missionsPending) {
            if (mission.getTargetFile() != null && mission.getTargetFile().equals(targetFile)) {
                return mission;
            }
        }
        return null;
    }

    void resumeMission(@NonNull DownloadMission mission) {
        if (!mission.getRunning() && canDownloadInCurrentNetwork(mission)) {
            mission.setEnqueued(true);
            mission.start();
        }
    }

    void pauseMission(@NonNull DownloadMission mission) {
        if (mission.getRunning()) {
            mission.setEnqueued(false);
            mission.pause();
        }
    }

    synchronized void deleteMission(@NonNull Mission mission) {
        if (mission instanceof DownloadMission) {
            missionsPending.remove(mission);
        } else if (mission instanceof FinishedMission) {
            missionsFinished.remove(mission);
        }

        mission.delete();
    }

    synchronized void setFinished(@NonNull DownloadMission mission) {
        missionsPending.remove(mission);
        missionsFinished.add(0, new FinishedMission(mission));
    }

    /**
     * Removes a failed mission from the in-memory queue, while keeping its
     * metadata so a later attempt can resume it.
     */
    synchronized void removePending(@NonNull DownloadMission mission) {
        missionsPending.remove(mission);
        mission.writeThisToFile();
    }

    synchronized int getRunningMissionsCount() {
        int count = 0;
        for (DownloadMission mission : missionsPending) {
            if (mission.getRunning() && !mission.isFinished()) {
                count++;
            }
        }
        return count;
    }

    synchronized void pauseAllMissions(boolean force) {
        for (DownloadMission mission : missionsPending) {
            if (!mission.getRunning() || mission.isFinished()) {
                if (mission.getMetadata() != null) {
                    mission.writeThisToFile();
                }
                continue;
            }

            if (force) {
                // avoid waiting for threads
                mission.setInit(null);
                mission.setThreads(new Thread[0]);
            }

            mission.pause();
        }
    }

    synchronized void startAllMissions() {
        for (DownloadMission mission : missionsPending) {
            if (mission.getRunning() || mission.isCorrupt() || !mission.getEnqueued()) {
                continue;
            }
            if (canDownloadInCurrentNetwork(mission)) {
                mission.start();
            }
        }
    }

    /**
     * Called when the active network changes. Pauses downloads that are no
     * longer allowed and resumes queued ones.
     */
    synchronized void onNetworkChanged() {
        for (DownloadMission mission : missionsPending) {
            if (mission.isFinished()) {
                continue;
            }

            boolean allowed = canDownloadInCurrentNetwork(mission);

            if (mission.getRunning() && !allowed) {
                mission.pause();
            } else if (!mission.getRunning() && mission.getEnqueued() && allowed) {
                mission.start();
            }
        }
    }

    /**
     * @return true if the mission can be retried silently after a network
     * change, false if the user has to start it again.
     */
    boolean canRetry(@NonNull DownloadMission mission) {
        switch (mission.getErrCode()) {
            case DownloadMission.ERROR_SSL_EXCEPTION:
            case DownloadMission.ERROR_UNKNOWN_HOST:
            case DownloadMission.ERROR_CONNECT_HOST:
            case DownloadMission.ERROR_TIMEOUT:
                return true;
            default:
                return false;
        }
    }

    /**
     * Checks whether the active network is allowed for the given mission.
     */
    boolean canDownloadInCurrentNetwork(@NonNull DownloadMission mission) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return false;
        }

        NetworkInfo info = connectivityManager.getActiveNetworkInfo();
        if (info == null || !info.isConnected()) {
            return false;
        }

        if (info.isRoaming() && !mission.getAllowedOverRoaming()) {
            return false;
        }

        int type = info.getType();
        if (type == ConnectivityManager.TYPE_WIFI
                && (mission.getAllowedNetworkTypes() & Request.NETWORK_WIFI) == 0) {
            return false;
        }
        if (type == ConnectivityManager.TYPE_MOBILE
                && (mission.getAllowedNetworkTypes() & Request.NETWORK_MOBILE) == 0) {
            return false;
        }

        return true;
    }

    synchronized void updateMaximumAttempts(int maxRetry) {
        this.maxRetry = maxRetry;
        for (DownloadMission mission : missionsPending) {
            mission.setMaxRetry(maxRetry);
        }
    }

    synchronized boolean hasPendingMissions() {
        return !missionsPending.isEmpty();
    }

    @NonNull
    synchronized List<DownloadMission> getPendingMissions() {
        return new ArrayList<>(missionsPending);
    }
}
