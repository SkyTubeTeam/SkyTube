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

import android.content.Context;

import androidx.annotation.NonNull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the queue management of the download engine.
 *
 * <p>The {@link Context} is a mock that points the metadata directory to a
 * temporary folder, and it has no connectivity: missions are persisted and
 * queued, but never started, so the tests stay deterministic.</p>
 */
public class DownloadManagerTest {

    @TempDir
    File tempDir;

    private Context context;
    private File pendingDir;
    private final List<DownloadManager> managers = new ArrayList<>();

    @BeforeEach
    void setUp() {
        context = mock(Context.class);
        when(context.getApplicationContext()).thenReturn(context);
        when(context.getFilesDir()).thenReturn(new File(tempDir, "files"));
        when(context.getExternalFilesDir(any())).thenReturn(new File(tempDir, "external"));
        pendingDir = DownloadManager.getPendingDir(context);
    }

    /**
     * Some manager operations persist the metadata from a short-lived
     * background thread.  Wait for those writers before JUnit removes the
     * temporary directory, otherwise the cleanup races with them.
     */
    @AfterEach
    void waitForMetadataWrites() throws InterruptedException {
        for (DownloadManager manager : managers) {
            for (DownloadMission mission : manager.getPendingMissions()) {
                mission.awaitPendingWrite(5_000);
            }
        }
    }

    @Test
    void startMissionPersistsMetadataAndKeepsTheMissionPending() {
        DownloadManager manager = newManager();
        File target = new File(tempDir, "video.mp4");
        DownloadMission mission = mission(target);

        manager.startMission(mission);

        assertEquals(1, manager.getPendingMissions().size());
        assertSame(mission, manager.getPendingMissions().get(0));
        assertFalse(mission.getRunning(), "a mission must not start without network");
        assertEquals(3, mission.getMaxRetry());
        assertNotNull(mission.getMetadata());
        assertTrue(mission.getMetadata().isFile());

        DownloadMission restored = DownloadUtils.readFromFile(mission.getMetadata());
        assertNotNull(restored);
        assertEquals(mission.getId(), restored.getId());
        assertEquals(target, restored.getTargetFile());
    }

    @Test
    void startMissionReusesAnExistingMissionForTheSameTarget() {
        DownloadManager manager = newManager();
        File target = new File(tempDir, "video.mp4");
        DownloadMission first = mission(target, "https://example.com/old.mp4");
        first.setTitle("Old");
        manager.startMission(first);

        DownloadMission second = mission(target, "https://example.com/new.mp4");
        second.setTitle("New");
        second.setNotificationGroupId("group-1");
        second.setNotificationGroupSize(2);
        manager.startMission(second);

        List<DownloadMission> pending = manager.getPendingMissions();
        assertEquals(1, pending.size(), "a target file must only be tracked once");
        assertSame(first, pending.get(0));

        // the fresh request wins: URLs expire, the metadata is kept
        assertEquals("https://example.com/new.mp4", first.getUrls()[0]);
        assertEquals("New", first.getTitle());
        assertEquals("group-1", first.getNotificationGroupId());
        assertEquals(2, first.getNotificationGroupSize());
    }

    @Test
    void startMissionLoadsPersistedMissionMetadata() throws Exception {
        DownloadManager manager = newManager();
        File target = new File(tempDir, "video.mp4");
        File part = new File(target.getPath() + ".part");
        Files.write(part.toPath(), new byte[]{1, 2, 3});

        DownloadMission stored = mission(target);
        stored.setDone(500);
        stored.setLength(1000);
        DownloadUtils.writeToFile(DownloadUtils.getMetadataFile(pendingDir, target), stored);

        DownloadMission request = mission(target, "https://example.com/fresh.mp4");
        manager.startMission(request);

        assertEquals(1, manager.getPendingMissions().size());
        DownloadMission loaded = manager.getPendingMissions().get(0);
        assertEquals(stored.getId(), loaded.getId(), "the resumable mission must be reused");
        assertEquals("https://example.com/fresh.mp4", loaded.getUrls()[0]);
        assertEquals(500, loaded.getDone(), "the progress must be kept");
    }

    @Test
    void startMissionRestartsAFinishedMissionFromScratch() throws Exception {
        DownloadManager manager = newManager();
        File target = new File(tempDir, "video.mp4");
        DownloadMission mission = completedMission(target, "https://example.com/a.mp4");
        manager.startMission(mission);

        // the file is there, so the engine considers the mission finished
        Files.write(target.toPath(), new byte[]{1});
        assertTrue(mission.isFinished());

        manager.startMission(mission(target, "https://example.com/b.mp4"));

        assertSame(mission, manager.getPendingMissions().get(0));
        assertEquals(0, mission.getDone(), "a finished mission must restart from zero");
        assertEquals("https://example.com/b.mp4", mission.getUrls()[0]);
        assertEquals(0, mission.getOffsets()[0]);
    }

    @Test
    void loadPendingMissionsRestoresMissionsSortedByTimestamp() {
        File firstTarget = new File(tempDir, "first.mp4");
        File secondTarget = new File(tempDir, "second.mp4");
        persist(firstTarget, 1000L);
        persist(secondTarget, 2000L);

        DownloadManager manager = newManager();

        List<DownloadMission> pending = manager.getPendingMissions();
        assertEquals(2, pending.size());
        assertEquals(firstTarget, pending.get(0).getTargetFile());
        assertEquals(secondTarget, pending.get(1).getTargetFile());
    }

    @Test
    void loadPendingMissionsSkipsCompletedMissionsAndDeletesTheirMetadata() throws Exception {
        File target = new File(tempDir, "done.mp4");
        Files.write(target.toPath(), new byte[]{1});
        File metadata = persist(target, 1000L);

        DownloadManager manager = newManager();

        assertTrue(manager.getPendingMissions().isEmpty());
        assertFalse(metadata.exists());
    }

    @Test
    void loadPendingMissionsDiscardsUnreadableMetadata() throws Exception {
        File metadata = new File(pendingDir, "broken.meta");
        Files.write(metadata.toPath(), "not a mission".getBytes(StandardCharsets.UTF_8));

        DownloadManager manager = newManager();

        assertTrue(manager.getPendingMissions().isEmpty());
        assertFalse(metadata.exists());
    }

    @Test
    void loadPendingMissionsIgnoresUnrelatedFiles() throws Exception {
        File notes = new File(pendingDir, "notes.txt");
        Files.write(notes.toPath(), "hello".getBytes(StandardCharsets.UTF_8));

        DownloadManager manager = newManager();

        assertTrue(manager.getPendingMissions().isEmpty());
        assertTrue(notes.isFile());
    }

    @Test
    void loadPendingMissionsResetsMissionWhenThePartialFileIsGone() {
        File target = new File(tempDir, "video.mp4");
        DownloadMission stored = mission(target);
        stored.setBlocks(new int[]{-1});
        stored.setDone(500);
        stored.setLength(1000);
        DownloadUtils.writeToFile(DownloadUtils.getMetadataFile(pendingDir, target), stored);

        DownloadManager manager = newManager();

        DownloadMission loaded = manager.getPendingMissions().get(0);
        assertEquals(0, loaded.getDone());
        assertEquals(DownloadMission.ERROR_PROGRESS_LOST, loaded.getErrCode());
        assertNull(loaded.getBlocks());
    }

    @Test
    void setFinishedMovesTheMissionToTheFinishedListOnlyOnce() {
        DownloadManager manager = newManager();
        DownloadMission first = mission(new File(tempDir, "first.mp4"));
        DownloadMission second = mission(new File(tempDir, "second.mp4"));

        manager.startMission(first);
        manager.startMission(second);

        manager.setFinished(first);
        manager.setFinished(first); // a delayed duplicate event

        assertEquals(1, manager.getPendingMissions().size());
        assertSame(second, manager.getPendingMissions().get(0));
        assertEquals(1, manager.getFinishedMissions().size());

        manager.setFinished(second);
        assertTrue(manager.getPendingMissions().isEmpty());
        List<FinishedMission> finished = manager.getFinishedMissions();
        assertEquals(2, finished.size());
        assertEquals(second.getId(), finished.get(0).getId(), "most recent first");
        assertEquals(first.getId(), finished.get(1).getId());
    }

    @Test
    void setFinishedIgnoresMissionsThatAreNotPending() {
        DownloadManager manager = newManager();

        manager.setFinished(mission(new File(tempDir, "unknown.mp4")));

        assertTrue(manager.getPendingMissions().isEmpty());
        assertTrue(manager.getFinishedMissions().isEmpty());
    }

    @Test
    void getPendingMissionsReturnsACopy() {
        DownloadManager manager = newManager();
        manager.startMission(mission(new File(tempDir, "video.mp4")));

        manager.getPendingMissions().clear();
        manager.getFinishedMissions().clear();

        assertEquals(1, manager.getPendingMissions().size());
    }

    @Test
    void hasActiveMissionsTracksRunningAndQueuedMissions() throws InterruptedException {
        DownloadManager manager = newManager();
        DownloadMission mission = mission(new File(tempDir, "video.mp4"));
        manager.startMission(mission);

        // a new mission is queued
        assertTrue(mission.getEnqueued());
        assertTrue(manager.hasActiveMissions());

        mission.setEnqueued(false);
        mission.awaitPendingWrite(5_000);
        assertFalse(manager.hasActiveMissions(), "a paused mission must not keep the service alive");

        mission.setRunning(true);
        assertTrue(manager.hasActiveMissions());

        mission.setRunning(false);
        assertFalse(manager.hasActiveMissions());

        mission.setEnqueued(true);
        mission.awaitPendingWrite(5_000);
        mission.setRunning(true);
        manager.setFinished(mission);
        assertFalse(manager.hasActiveMissions());
    }

    @Test
    void getRunningMissionsCountOnlyCountsRunningMissions() {
        DownloadManager manager = newManager();
        DownloadMission mission = mission(new File(tempDir, "video.mp4"));
        manager.startMission(mission);

        assertEquals(0, manager.getRunningMissionsCount());

        mission.setRunning(true);
        assertEquals(1, manager.getRunningMissionsCount());

        mission.setRunning(false);
        assertEquals(0, manager.getRunningMissionsCount());
    }

    @Test
    void deleteMissionRemovesTheFilesAndTheMetadata() throws Exception {
        DownloadManager manager = newManager();
        File target = new File(tempDir, "video.mp4");
        File part = new File(target.getPath() + ".part");
        DownloadMission mission = mission(target);
        manager.startMission(mission);
        File metadata = mission.getMetadata();

        Files.write(part.toPath(), new byte[]{1, 2});
        Files.write(target.toPath(), new byte[]{1});

        manager.deleteMission(mission);

        assertTrue(manager.getPendingMissions().isEmpty());
        assertFalse(target.exists());
        assertFalse(part.exists());
        assertFalse(metadata.exists());
    }

    @Test
    void removePendingKeepsTheMetadataForAResume() throws Exception {
        DownloadManager manager = newManager();
        File target = new File(tempDir, "video.mp4");
        File part = new File(target.getPath() + ".part");
        DownloadMission mission = mission(target);
        manager.startMission(mission);
        File metadata = mission.getMetadata();
        Files.write(part.toPath(), new byte[]{1, 2});

        manager.removePending(mission);

        assertTrue(manager.getPendingMissions().isEmpty());
        assertTrue(metadata.isFile(), "the metadata is needed to resume the download");
        assertTrue(part.isFile());
    }

    @Test
    void updateMaximumAttemptsAppliesToExistingAndNewMissions() {
        DownloadManager manager = newManager();
        DownloadMission first = mission(new File(tempDir, "first.mp4"));
        manager.startMission(first);

        manager.updateMaximumAttempts(7);
        assertEquals(7, first.getMaxRetry());

        DownloadMission second = mission(new File(tempDir, "second.mp4"));
        manager.startMission(second);
        assertEquals(7, second.getMaxRetry());
    }

    @Test
    void canRetryOnlyForTransientErrors() {
        DownloadManager manager = newManager();
        DownloadMission mission = mission(new File(tempDir, "video.mp4"));

        int[] retryable = {
                DownloadMission.ERROR_SSL_EXCEPTION,
                DownloadMission.ERROR_UNKNOWN_HOST,
                DownloadMission.ERROR_CONNECT_HOST,
                DownloadMission.ERROR_TIMEOUT,
        };
        for (int code : retryable) {
            mission.notifyError(code, null);
            assertTrue(manager.canRetry(mission), "error code " + code + " should be retryable");
        }

        int[] fatal = {
                DownloadMission.ERROR_NOTHING,
                DownloadMission.ERROR_FILE_CREATION,
                DownloadMission.ERROR_PROGRESS_LOST,
                404,
                500,
        };
        for (int code : fatal) {
            mission.notifyError(code, null);
            assertFalse(manager.canRetry(mission), "error code " + code + " should not be retryable");
        }
    }

    @Test
    void hasPendingMissionDetectsResumableDownloads() throws Exception {
        File target = new File(tempDir, "video.mp4");
        File part = new File(target.getPath() + ".part");
        File metadata = DownloadUtils.getMetadataFile(pendingDir, target);

        assertFalse(DownloadManager.hasPendingMission(context, target));

        DownloadMission mission = mission(target);
        mission.setDone(10);
        mission.setLength(100);
        DownloadUtils.writeToFile(metadata, mission);
        assertTrue(DownloadManager.hasPendingMission(context, target),
                "a tracked mission without a partial file is still pending");

        Files.write(part.toPath(), new byte[]{1, 2, 3});
        assertTrue(DownloadManager.hasPendingMission(context, target),
                "a partial file must be resumed");

        // the partial file was renamed: the download is complete
        Files.delete(part.toPath());
        Files.write(target.toPath(), new byte[]{1});
        assertFalse(DownloadManager.hasPendingMission(context, target));
        assertFalse(metadata.exists());

        Files.write(metadata.toPath(), "garbage".getBytes(StandardCharsets.UTF_8));
        assertFalse(DownloadManager.hasPendingMission(context, target));
        assertFalse(metadata.exists());
    }

    @Test
    void nothingStartsWithoutConnectivity() {
        DownloadManager manager = newManager();
        DownloadMission mission = mission(new File(tempDir, "video.mp4"));
        manager.startMission(mission);

        manager.startAllMissions();
        manager.onNetworkChanged();
        manager.pauseAllMissions(false);

        assertFalse(mission.getRunning());
        assertTrue(manager.getPendingMissions().contains(mission));
    }

    @NonNull
    private DownloadManager newManager() {
        DownloadManager manager = new DownloadManager(context, (what, mission) -> {
        }, new DownloadMission.Listener() {
            @Override
            public void onProgress(DownloadMission mission) {
            }

            @Override
            public void onFinished(DownloadMission mission) {
            }

            @Override
            public void onError(DownloadMission mission) {
            }
        });
        managers.add(manager);
        return manager;
    }

    @NonNull
    private DownloadMission mission(@NonNull File target) {
        return mission(target, "https://example.com/" + target.getName());
    }

    @NonNull
    private DownloadMission mission(@NonNull File target, @NonNull String url) {
        return new DownloadMission(new String[]{url}, target);
    }

    /**
     * A mission whose sub-downloads are already done ({@code current} reached
     * the number of URLs), as left behind by a completion that could not be
     * reported to the manager.
     */
    @NonNull
    private DownloadMission completedMission(@NonNull File target, @NonNull String url) {
        return new DownloadMission(
                "finished-" + target.getName(),
                url,
                1000,
                System.currentTimeMillis(),
                '?',
                target,
                new String[]{url},
                1000,       // done
                false,      // unknown length
                new long[]{0},
                1,          // current == urls.length
                0,          // near length
                null,       // blocks
                0,          // fallback resume offset
                3,          // thread count
                "Finished",
                null,
                null,
                1,
                true,
                android.app.DownloadManager.Request.NETWORK_WIFI,
                false,
                DownloadMission.ERROR_NOTHING);
    }

    @NonNull
    private File persist(@NonNull File target, long timestamp) {
        DownloadMission mission = mission(target);
        mission.setTimestamp(timestamp);
        File metadata = DownloadUtils.getMetadataFile(pendingDir, target);
        DownloadUtils.writeToFile(metadata, mission);
        return metadata;
    }
}
