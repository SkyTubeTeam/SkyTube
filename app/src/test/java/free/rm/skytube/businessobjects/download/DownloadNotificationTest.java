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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the bookkeeping behind the aggregate download notification.
 */
public class DownloadNotificationTest {

    @TempDir
    File tempDir;

    @Test
    void addMissionIsIdempotent() {
        DownloadNotification notification = new DownloadNotification();
        DownloadMission mission = mission("group-1", "video.mp4", 2);

        notification.addMission(mission);
        notification.addMission(mission);

        assertEquals(1, notification.seenTasks());
        assertEquals(2, notification.expectedTasks());
    }

    @Test
    void markFinishedRegistersAnUntrackedMission() {
        DownloadNotification notification = new DownloadNotification();
        DownloadMission mission = mission("group-1", "video.mp4", 1);
        mission.setLength(100);
        mission.setDone(100);

        // a completion may arrive before the first progress event
        notification.markFinished(mission);

        assertEquals(1, notification.seenTasks());
        assertTrue(notification.isComplete());
        assertTrue(notification.isTerminal());
    }

    @Test
    void markFailedRegistersAnUntrackedMission() {
        DownloadNotification notification = new DownloadNotification();
        DownloadMission mission = mission("group-1", "video.mp4", 1);

        notification.markFailed(mission);

        assertEquals(1, notification.seenTasks());
        assertTrue(notification.hasFailure());
        assertTrue(notification.isTerminal());
        assertFalse(notification.isComplete());
    }

    @Test
    void expectedTasksGrowsWhenALaterMissionReportsABiggerGroup() {
        DownloadNotification notification = new DownloadNotification();
        DownloadMission first = mission("group-1", "video.mp4", 1);
        DownloadMission second = mission("group-1", "audio.webm", 3);

        notification.addMission(first);
        assertEquals(1, notification.expectedTasks());

        notification.addMission(second);
        assertEquals(3, notification.expectedTasks());

        notification.markFinished(first);
        notification.markFinished(second);
        assertFalse(notification.isComplete());
        assertTrue(notification.hasIncompleteGroups());
    }

    @Test
    void expectedTasksIsTheSumOfAllGroups() {
        DownloadNotification notification = new DownloadNotification();
        notification.addMission(mission("group-1", "video.mp4", 2));
        notification.addMission(mission("group-2", "other.mp4", 3));

        assertEquals(5, notification.expectedTasks());
        assertEquals(2, notification.getGroups().size());
    }

    @Test
    void progressIsUnknownUntilEveryTaskHasALength() {
        DownloadNotification notification = new DownloadNotification();
        DownloadMission first = mission("group-1", "video.mp4", 2);
        DownloadMission second = mission("group-1", "audio.webm", 2);

        first.setLength(100);
        first.setDone(50);
        notification.addMission(first);
        notification.addMission(second);

        // the second task has no length yet
        assertFalse(notification.hasFullLength());
        assertEquals(-1, notification.getProgress());

        second.setLength(100);
        second.setDone(0);

        assertTrue(notification.hasFullLength());
        assertEquals(25, notification.getProgress());
    }

    @Test
    void progressIgnoresNegativeValuesAndIsClamped() {
        DownloadNotification notification = new DownloadNotification();
        DownloadMission mission = mission("group-1", "video.mp4", 1);
        mission.setLength(100);
        mission.setDone(150);
        notification.addMission(mission);

        assertEquals(100, notification.getProgress());

        mission.setDone(-50);
        assertEquals(0, notification.totalDone());
        assertEquals(0, notification.getProgress());
    }

    @Test
    void progressClampsToHundredWhenDoneExceedsLength() {
        DownloadNotification notification = new DownloadNotification();
        DownloadMission mission = mission("group-1", "video.mp4", 1);
        mission.setLength(1000);
        mission.setDone(1001);
        notification.addMission(mission);

        assertTrue(notification.getProgress() <= 100);
        assertEquals(100, notification.getProgress());
    }

    @Test
    void titleFallsBackToTheMissionTitle() {
        DownloadNotification notification = new DownloadNotification();
        DownloadMission mission = mission("group-1", "video.mp4", 1);
        mission.setTitle("Video title");
        mission.setNotificationGroupTitle("   ");
        notification.addMission(mission);

        assertEquals("Video title", notification.getTitle());
    }

    @Test
    void titleFallsBackToTheFileName() {
        DownloadNotification notification = new DownloadNotification();
        DownloadMission mission = mission("group-1", "fallback.mp4", 1);
        notification.addMission(mission);

        assertEquals("fallback.mp4", notification.getTitle());
    }

    @Test
    void titleIsOnlyKnownForASingleGroup() {
        DownloadNotification notification = new DownloadNotification();
        notification.addMission(mission("group-1", "video.mp4", 1));
        notification.addMission(mission("group-2", "other.mp4", 1));

        assertNull(notification.getTitle());

        notification.removeGroup("group-2");
        assertEquals("video.mp4", notification.getTitle());
    }

    @Test
    void aLaterMissionUpdatesTheGroupTitle() {
        DownloadNotification notification = new DownloadNotification();
        DownloadMission first = mission("group-1", "video.mp4", 2);
        first.setNotificationGroupTitle(null);
        notification.addMission(first);
        assertEquals("video.mp4", notification.getTitle());

        DownloadMission second = mission("group-1", "audio.webm", 2);
        second.setNotificationGroupTitle("Real title");
        notification.addMission(second);

        assertEquals("Real title", notification.getTitle());
    }

    @Test
    void removingAnUnfinishedTaskShrinksTheGroup() {
        DownloadNotification notification = new DownloadNotification();
        DownloadMission video = mission("group-1", "video.mp4", 2);
        DownloadMission audio = mission("group-1", "audio.webm", 2);

        notification.addMission(video);
        notification.addMission(audio);
        notification.markFinished(video);
        assertFalse(notification.isComplete());

        // the user deleted the audio download: the video alone is the group now
        notification.removeMission(audio);

        DownloadNotification.Group group = notification.getGroup("group-1");
        assertNotNull(group);
        assertEquals(1, group.getExpected());
        assertTrue(group.isComplete());
        assertTrue(notification.isComplete());
        assertEquals(1, notification.seenTasks());
    }

    @Test
    void removingTheLastTaskClearsTheNotification() {
        DownloadNotification notification = new DownloadNotification();
        DownloadMission mission = mission("group-1", "video.mp4", 1);
        notification.addMission(mission);

        notification.removeMission(mission);

        assertTrue(notification.isEmpty());
        assertTrue(notification.getMissions().isEmpty());
        assertFalse(notification.hasFailure());
        assertFalse(notification.hasIncompleteGroups());
    }

    @Test
    void removingAnUnknownTaskIsANoOp() {
        DownloadNotification notification = new DownloadNotification();
        DownloadMission mission = mission("group-1", "video.mp4", 1);
        notification.addMission(mission);

        notification.removeMission(mission("group-2", "other.mp4", 1));

        assertEquals(1, notification.seenTasks());
        assertNotNull(notification.getGroup("group-1"));
    }

    @Test
    void removingAnUnknownGroupIsANoOp() {
        DownloadNotification notification = new DownloadNotification();
        notification.addMission(mission("group-1", "video.mp4", 1));

        notification.removeGroup("unknown");
        assertNotNull(notification.getGroup("group-1"));
        assertEquals(1, notification.getGroups().size());

        // removing the same group twice must be safe
        notification.removeGroup("group-1");
        notification.removeGroup("group-1");
        assertTrue(notification.isEmpty());
    }

    @Test
    void markGroupCompleteOnAnUnknownGroupIsANoOp() {
        DownloadNotification notification = new DownloadNotification();
        notification.markGroupComplete("unknown");

        assertTrue(notification.isEmpty());
        assertFalse(notification.isTerminal());
    }

    @Test
    void failureIsStickyWhenALateTaskFinishes() {
        DownloadNotification notification = new DownloadNotification();
        DownloadMission mission = mission("group-1", "video.mp4", 1);
        notification.addMission(mission);

        notification.markFailed(mission);
        notification.markFinished(mission);

        assertTrue(notification.hasFailure());
        assertFalse(notification.isComplete());
        assertTrue(notification.isTerminal());
        assertTrue(notification.getGroup("group-1").isFailed());
    }

    @Test
    void groupsAreResolvedIndependently() {
        DownloadNotification notification = new DownloadNotification();
        DownloadMission first = mission("group-1", "first.mp4", 1);
        DownloadMission second = mission("group-2", "second.mp4", 1);
        first.setLength(100);
        first.setDone(100);
        second.setLength(100);
        second.setDone(10);

        notification.addMission(first);
        notification.addMission(second);
        notification.markFinished(first);

        assertTrue(notification.hasIncompleteGroups());
        assertFalse(notification.isTerminal());

        notification.markFinished(second);
        assertFalse(notification.hasIncompleteGroups());
        assertTrue(notification.isTerminal());
        assertTrue(notification.isComplete());
    }

    @Test
    void getGroupsReturnsASnapshotThatIsSafeToIterateWhileRemoving() {
        DownloadNotification notification = new DownloadNotification();
        DownloadMission first = mission("group-1", "first.mp4", 1);
        DownloadMission second = mission("group-2", "second.mp4", 1);
        notification.addMission(first);
        notification.addMission(second);

        // this is how the service posts the final notifications
        for (DownloadNotification.Group group : notification.getGroups()) {
            notification.removeGroup(group.id);
        }

        assertTrue(notification.isEmpty());
        assertTrue(notification.getMissions().isEmpty());
    }

    @Test
    void resolvingOneGroupDoesNotBlockANewOne() {        DownloadNotification notification = new DownloadNotification();
        DownloadMission first = mission("group-1", "first.mp4", 1);
        first.setLength(100);
        first.setDone(100);
        notification.addMission(first);
        notification.markFinished(first);
        notification.removeGroup("group-1");
        assertTrue(notification.isEmpty());

        DownloadMission second = mission("group-2", "second.mp4", 1);
        notification.addMission(second);

        assertFalse(notification.isEmpty());
        assertEquals(1, notification.getGroups().size());
        assertTrue(notification.hasIncompleteGroups());
        assertFalse(notification.isTerminal());
    }

    @Test
    void getMissionsReturnsASnapshot() {
        DownloadNotification notification = new DownloadNotification();
        DownloadMission mission = mission("group-1", "video.mp4", 1);
        notification.addMission(mission);

        List<DownloadMission> snapshot = notification.getMissions();
        notification.removeMission(mission);

        assertEquals(1, snapshot.size());
        assertSame(mission, snapshot.get(0));
        assertTrue(notification.getMissions().isEmpty());
    }

    @NonNull
    private DownloadMission mission(@Nullable String groupId,
                                    @NonNull String fileName,
                                    int groupSize) {
        DownloadMission mission = new DownloadMission(
                new String[]{"https://example.com/" + fileName},
                new File(tempDir, fileName));
        if (groupId != null) {
            mission.setNotificationGroupId(groupId);
        }
        mission.setNotificationGroupTitle(fileName);
        mission.setNotificationGroupSize(groupSize);
        return mission;
    }
}
