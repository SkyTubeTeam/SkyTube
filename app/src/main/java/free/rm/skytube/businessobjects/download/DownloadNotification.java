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
import androidx.annotation.VisibleForTesting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tracks the state of the downloads that are aggregated in one notification.
 *
 * <p>Several files can belong to a single logical download (e.g. the video
 * and the audio streams of a video that is not muxed).  Files that share a
 * notification group identifier are shown in one notification and the group
 * only reaches its end state (complete or failed) when every file of the
 * group has finished or failed.</p>
 */
public final class DownloadNotification {

    private final Map<String, DownloadMission> tasks = new HashMap<>();
    private final Map<String, Group> groups = new HashMap<>();
    /**
     * Groups whose final notification was already posted.  Their missions
     * must not be tracked again: late progress/finished events (or the
     * on-disk fallback in the service) would otherwise re-create the group and
     * leave the aggregate notification stuck in the foreground forever.
     */
    private final Set<String> resolvedGroups = new HashSet<>();
    private boolean failed;

    synchronized void addMission(@NonNull DownloadMission mission) {
        if (isResolved(mission)) {
            return;
        }
        registerTask(mission);
    }

    synchronized void markFinished(@NonNull DownloadMission mission) {
        if (isResolved(mission)) {
            return;
        }
        registerTask(mission);
        Group group = groupFor(mission);
        if (group.finishedIds.add(mission.getId())) {
            group.complete = group.finishedIds.size() >= group.expected;
        }
    }

    synchronized void markFailed(@NonNull DownloadMission mission) {
        if (isResolved(mission)) {
            return;
        }
        registerTask(mission);
        Group group = groupFor(mission);
        if (group.failedIds.add(mission.getId())) {
            group.failed = true;
            failed = true;
        }
    }

    synchronized void removeMission(@NonNull DownloadMission mission) {
        if (tasks.remove(mission.getId()) == null) {
            return;
        }

        Group group = groups.get(groupKey(mission));
        if (group != null) {
            boolean finished = group.finishedIds.remove(mission.getId());
            boolean failedTask = group.failedIds.remove(mission.getId());
            group.seenIds.remove(mission.getId());
            if (!finished && !failedTask) {
                group.expected = Math.max(1, group.expected - 1);
            }
            group.complete = group.finishedIds.size() >= group.expected;
            if (group.seenIds.isEmpty()
                    && group.finishedIds.isEmpty()
                    && group.failedIds.isEmpty()) {
                groups.remove(group.id);
            }
        }

        if (groups.isEmpty()) {
            clear();
        }
    }

    @VisibleForTesting
    void markAllFailed() {
        failed = true;
        for (Group group : groups.values()) {
            group.failed = true;
        }
    }

    /**
     * Marks a group as complete although not every file of the group was
     * reported finished.  Used when nothing can make progress for the group
     * any more (e.g. the process was restarted and one file of a pair is no
     * longer tracked) and the files that are on disk are considered usable.
     */
    synchronized void markGroupComplete(@NonNull String groupId) {
        Group group = groups.get(groupId);
        if (group != null && !group.failed) {
            group.complete = true;
        }
    }

    /**
     * Removes a group - and the missions that belong to it - from the
     * aggregate notification, e.g. after the final notification of the
     * group was posted.
     */
    synchronized void removeGroup(@NonNull String groupId) {
        groups.remove(groupId);
        resolvedGroups.add(groupId);
        tasks.values().removeIf(mission -> groupId.equals(groupKey(mission)));
        if (groups.isEmpty()) {
            clear();
        }
    }

    synchronized void clear() {
        tasks.clear();
        groups.clear();
        failed = false;
    }

    @VisibleForTesting
    synchronized boolean isEmpty() {
        return groups.isEmpty();
    }

    @VisibleForTesting
    synchronized boolean isTerminal() {
        if (groups.isEmpty()) {
            return false;
        }
        for (Group group : groups.values()) {
            if (!group.isTerminal()) {
                return false;
            }
        }
        return true;
    }

    @VisibleForTesting
    synchronized boolean isComplete() {
        if (groups.isEmpty() || failed) {
            return false;
        }
        for (Group group : groups.values()) {
            if (!group.complete) {
                return false;
            }
        }
        return true;
    }

    @VisibleForTesting
    boolean hasFailure() {
        return failed;
    }

    synchronized boolean hasIncompleteGroups() {
        for (Group group : groups.values()) {
            if (!group.isTerminal()) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    synchronized String getTitle() {
        if (groups.size() != 1) {
            return null;
        }
        return groups.values().iterator().next().title;
    }

    synchronized long totalDone() {
        long total = 0;
        for (DownloadMission mission : tasks.values()) {
            total += Math.max(0, mission.getDone());
        }
        return total;
    }

    synchronized long totalLength() {
        long total = 0;
        for (DownloadMission mission : tasks.values()) {
            total += Math.max(0, mission.getLength());
        }
        return total;
    }

    synchronized int expectedTasks() {
        int total = 0;
        for (Group group : groups.values()) {
            total += group.expected;
        }
        return total;
    }

    @VisibleForTesting
    synchronized int seenTasks() {
        return tasks.size();
    }

    synchronized boolean hasFullLength() {
        int expected = expectedTasks();
        return expected > 0
                && knownLengthTaskCount() == expected
                && totalLength() > 0;
    }

    synchronized int getProgress() {
        if (!hasFullLength()) {
            return -1;
        }

        long total = totalLength();
        if (total <= 0) {
            return -1;
        }

        long done = Math.min(totalDone(), total);
        int progress = (int) (done * 100L / total);
        if (progress < 0) {
            return 0;
        }
        if (progress > 100) {
            return 100;
        }
        return progress;
    }

    /** All the missions currently tracked by the aggregate notification. */
    @NonNull
    synchronized List<DownloadMission> getMissions() {
        return new ArrayList<>(tasks.values());
    }

    /** A snapshot of the groups, safe to iterate while mutating. */
    @NonNull
    synchronized List<Group> getGroups() {
        return new ArrayList<>(groups.values());
    }

    @Nullable
    synchronized Group getGroup(@NonNull String groupId) {
        return groups.get(groupId);
    }

    synchronized private void registerTask(@NonNull DownloadMission mission) {
        if (tasks.get(mission.getId()) == null) {
            tasks.put(mission.getId(), mission);
        }
        groupFor(mission).seenIds.add(mission.getId());
    }

    @NonNull
    private Group groupFor(@NonNull DownloadMission mission) {
        String groupId = groupKey(mission);
        Group group = groups.get(groupId);
        if (group == null) {
            group = new Group(groupId);
            group.expected = mission.getNotificationGroupSize() > 0
                    ? mission.getNotificationGroupSize()
                    : 1;
            group.title = titleFor(mission);
            groups.put(groupId, group);
            return group;
        }

        if (mission.getNotificationGroupSize() > 0
                && group.expected < mission.getNotificationGroupSize()) {
            group.expected = mission.getNotificationGroupSize();
            if (!group.failed) {
                group.complete = group.finishedIds.size() >= group.expected;
            }
        }
        String title = mission.getNotificationGroupTitle();
        if (title != null && !title.trim().isEmpty()) {
            group.title = title;
        }
        return group;
    }

    private int knownLengthTaskCount() {
        int count = 0;
        for (DownloadMission mission : tasks.values()) {
            if (mission.getLength() > 0) {
                count++;
            }
        }
        return count;
    }

    @NonNull
    private static String groupKey(@NonNull DownloadMission mission) {
        return mission.getNotificationGroupId() != null
                ? mission.getNotificationGroupId()
                : mission.getId();
    }

    private boolean isResolved(@NonNull DownloadMission mission) {
        return resolvedGroups.contains(groupKey(mission));
    }

    @NonNull
    private static String titleFor(@NonNull DownloadMission mission) {
        if (mission.getNotificationGroupTitle() != null
                && !mission.getNotificationGroupTitle().trim().isEmpty()) {
            return mission.getNotificationGroupTitle();
        }
        if (mission.getTitle() != null && !mission.getTitle().trim().isEmpty()) {
            return mission.getTitle();
        }
        return mission.getTargetFile() != null ? mission.getTargetFile().getName() : "";
    }

    /**
     * The state of one logical download: the set of files that make it up
     * and which of them already finished or failed.
     */
    public static final class Group {
        final String id;
        int expected = 1;
        boolean complete;
        boolean failed;
        String title = "";
        final Set<String> seenIds = new HashSet<>();
        final Set<String> finishedIds = new HashSet<>();
        final Set<String> failedIds = new HashSet<>();

        Group(@NonNull String id) {
            this.id = id;
        }

        boolean isTerminal() {
            return complete || failed;
        }

        boolean isComplete() {
            return complete;
        }

        boolean isFailed() {
            return failed;
        }

        @NonNull
        String getTitle() {
            return title;
        }

        int getExpected() {
            return expected;
        }

        public int getFinishedCount() {
            return finishedIds.size();
        }

        public int getFailedCount() {
            return failedIds.size();
        }
    }
}
