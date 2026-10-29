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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DownloadEngineTest {

    @TempDir
    File tempDir;

    @Test
    void getFileExt() {
        assertEquals(".mp4", DownloadUtils.getFileExt("https://example.com/video.mp4?token=abc"));
        assertEquals(".webm", DownloadUtils.getFileExt("https://example.com/video.webm"));
        assertNull(DownloadUtils.getFileExt("https://example.com/video"));
    }

    @Test
    void metadataFileIsDeterministic() {
        File target = new File(tempDir, "video.mp4");
        File first = DownloadUtils.getMetadataFile(tempDir, target);
        File second = DownloadUtils.getMetadataFile(tempDir, target);

        assertEquals(first, second);
        assertTrue(first.getName().endsWith(".meta"));
    }

    @Test
    void missionCanBeSerializedAndRestored() {
        File target = new File(tempDir, "video.mp4");
        DownloadMission mission = new DownloadMission(
                new String[]{"https://example.com/video.mp4"}, target);
        mission.setTitle("Test video");
        mission.setNotificationGroupId("group-1");
        mission.setNotificationGroupTitle("Group title");
        mission.setNotificationGroupSize(2);
        mission.setLength(1024);
        mission.setDone(512);

        File metadata = new File(tempDir, "mission.meta");
        DownloadUtils.writeToFile(metadata, mission);

        DownloadMission restored = DownloadUtils.readFromFile(metadata);

        assertEquals(mission.getId(), restored.getId());
        assertEquals(target, restored.getTargetFile());
        assertEquals("Test video", restored.getTitle());
        assertEquals("group-1", restored.getNotificationGroupId());
        assertEquals("Group title", restored.getNotificationGroupTitle());
        assertEquals(2, restored.getNotificationGroupSize());
        assertEquals(1024, restored.getLength());
        assertEquals(512, restored.getDone());
        assertEquals("https://example.com/video.mp4", restored.getUrls()[0]);
        assertNotNull(restored.getThreads());
        assertEquals(0, restored.getThreads().length);
        assertNull(restored.getInit());
        assertEquals(3, restored.getMaxRetry());
        assertFalse(restored.getRunning());
    }

    @Test
    void progressIsCalculatedFromLength() {
        File target = new File(tempDir, "video.mp4");
        DownloadMission mission = new DownloadMission(new String[]{"https://example.com"}, target);

        mission.setLength(1000);
        mission.setDone(250);

        assertEquals(25, mission.getProgress());
        assertEquals(new File(tempDir, "video.mp4.part"), mission.getPartFile());
    }

    @Test
    void progressFallsBackToNearLength() {
        File target = new File(tempDir, "video.mp4");
        DownloadMission mission = new DownloadMission(new String[]{"https://example.com"}, target);

        mission.setNearLength(2000);
        mission.setDone(500);

        assertEquals(25, mission.getProgress());
    }

    @Test
    void moveCompletedFileRenamesAndReplaces() throws Exception {
        File source = new File(tempDir, "video.mp4.part");
        File target = new File(tempDir, "video.mp4");
        Files.write(source.toPath(), "new".getBytes(StandardCharsets.UTF_8));
        Files.write(target.toPath(), "old".getBytes(StandardCharsets.UTF_8));

        assertTrue(DownloadUtils.moveCompletedFile(source, target));
        assertFalse(source.exists());
        assertEquals("new", new String(Files.readAllBytes(target.toPath()), StandardCharsets.UTF_8));
    }

    @Test
    void fileStreamSupportsSeekAndTruncate() throws Exception {
        File file = new File(tempDir, "stream.bin");

        try (FileStream stream = new FileStream(file)) {
            stream.write("hello world".getBytes(StandardCharsets.UTF_8));
            stream.seek(6);
            byte[] buffer = new byte[5];
            assertEquals(5, stream.read(buffer));
            assertEquals("world", new String(buffer, StandardCharsets.UTF_8));

            stream.setLength(5);
            assertEquals(5, stream.length());
        }
    }

    @Test
    void downloadsFileUsingRangeRequests() throws Exception {
        byte[] content = new byte[2 * 1024 * 1024 + 123];
        for (int i = 0; i < content.length; i++) {
            content[i] = (byte) (i * 31);
        }

        try (RangeServer server = new RangeServer(content)) {
            File target = new File(tempDir, "file.bin");
            String url = "http://127.0.0.1:" + server.getPort() + "/file.bin";
            DownloadMission mission = new DownloadMission(new String[]{url}, target);
            mission.setThreadCount(3);

            mission.start();

            long deadline = System.currentTimeMillis() + 30_000;
            while (!target.isFile() && System.currentTimeMillis() < deadline) {
                Thread.sleep(50);
            }

            assertTrue(target.isFile(), "The download did not finish in time");
            assertArrayEquals(content, Files.readAllBytes(target.toPath()));
            assertFalse(new File(tempDir, "file.bin.part").exists());
            assertEquals(content.length, mission.getDone());
        }
    }

    @Test
    void downloadsFileWhenServerIgnoresRanges() throws Exception {
        byte[] content = new byte[1024 * 1024 + 17];
        for (int i = 0; i < content.length; i++) {
            content[i] = (byte) (i * 17);
        }

        try (RangeServer server = new RangeServer(content, false)) {
            File target = new File(tempDir, "fallback.bin");
            String url = "http://127.0.0.1:" + server.getPort() + "/file.bin";
            DownloadMission mission = new DownloadMission(new String[]{url}, target);
            mission.setThreadCount(3);

            mission.start();

            long deadline = System.currentTimeMillis() + 30_000;
            while (!target.isFile() && System.currentTimeMillis() < deadline) {
                Thread.sleep(50);
            }

            assertTrue(target.isFile(), "The fallback download did not finish in time");
            assertArrayEquals(content, Files.readAllBytes(target.toPath()));
            assertFalse(new File(tempDir, "fallback.bin.part").exists());
        }
    }

    @Test
    void downloadNotificationAggregatesTasks() {
        File videoTarget = new File(tempDir, "video.mp4");
        File audioTarget = new File(tempDir, "audio.webm");
        DownloadMission video = new DownloadMission(
                new String[]{"https://example.com/video"}, videoTarget);
        video.setNotificationGroupId("group-1");
        video.setNotificationGroupTitle("Video");
        video.setNotificationGroupSize(2);
        DownloadMission audio = new DownloadMission(
                new String[]{"https://example.com/audio"}, audioTarget);
        audio.setNotificationGroupId("group-1");
        audio.setNotificationGroupTitle("Video");
        audio.setNotificationGroupSize(2);

        DownloadNotification notification = new DownloadNotification();
        notification.addMission(video);

        assertEquals(2, notification.expectedTasks());
        assertEquals(1, notification.seenTasks());
        assertFalse(notification.isTerminal());
        assertFalse(notification.hasFullLength());
        assertEquals(-1, notification.getProgress());

        video.setLength(1000);
        video.setDone(500);
        notification.addMission(video);
        assertEquals(500, notification.totalDone());
        assertEquals(1000, notification.totalLength());
        assertFalse(notification.hasFullLength());

        video.setDone(1000);
        notification.markFinished(video);
        assertFalse(notification.isComplete());
        assertFalse(notification.isTerminal());

        audio.setLength(1000);
        audio.setDone(250);
        notification.addMission(audio);
        assertEquals(1250, notification.totalDone());
        assertEquals(2000, notification.totalLength());
        assertTrue(notification.hasFullLength());
        assertEquals(62, notification.getProgress());

        audio.setDone(1000);
        notification.markFinished(audio);
        assertTrue(notification.isComplete());
        assertTrue(notification.isTerminal());
        assertEquals(100, notification.getProgress());
        assertEquals("Video", notification.getTitle());
    }

    @Test
    void downloadNotificationReportsFailure() {
        File target = new File(tempDir, "video.mp4");
        DownloadMission mission = new DownloadMission(
                new String[]{"https://example.com/video"}, target);
        mission.setNotificationGroupId("group-1");
        mission.setNotificationGroupTitle("Video");
        mission.setNotificationGroupSize(1);

        DownloadNotification notification = new DownloadNotification();
        notification.addMission(mission);
        mission.setLength(100);
        mission.setDone(10);
        notification.markFailed(mission);

        assertTrue(notification.hasFailure());
        assertTrue(notification.isTerminal());
        assertFalse(notification.isComplete());

        notification.markAllFailed();
        assertTrue(notification.hasFailure());

        notification.clear();
        assertTrue(notification.isEmpty());
        assertFalse(notification.hasFailure());
    }

    @Test
    void downloadNotificationAggregatesMultipleGroups() {
        DownloadNotification notification = new DownloadNotification();

        DownloadMission first = new DownloadMission(
                new String[]{"https://example.com/first"}, new File(tempDir, "first.mp4"));
        first.setNotificationGroupId("group-1");
        first.setNotificationGroupTitle("First");
        first.setNotificationGroupSize(1);
        first.setLength(100);
        first.setDone(50);

        DownloadMission second = new DownloadMission(
                new String[]{"https://example.com/second"}, new File(tempDir, "second.mp4"));
        second.setNotificationGroupId("group-2");
        second.setNotificationGroupTitle("Second");
        second.setNotificationGroupSize(1);
        second.setLength(100);
        second.setDone(25);

        notification.addMission(first);
        notification.addMission(second);

        assertEquals(2, notification.expectedTasks());
        assertEquals(75, notification.totalDone());
        assertEquals(200, notification.totalLength());
        assertEquals(37, notification.getProgress());
        assertNull(notification.getTitle());

        notification.markFinished(first);
        notification.markFinished(second);
        assertTrue(notification.isComplete());
    }

    @Test
    void downloadNotificationStaysIncompleteUntilAllTasksFinish() {
        File videoTarget = new File(tempDir, "video.mp4");
        File audioTarget = new File(tempDir, "audio.webm");
        DownloadMission video = new DownloadMission(
                new String[]{"https://example.com/video"}, videoTarget);
        video.setNotificationGroupId("group-1");
        video.setNotificationGroupTitle("Video");
        video.setNotificationGroupSize(2);
        DownloadMission audio = new DownloadMission(
                new String[]{"https://example.com/audio"}, audioTarget);
        audio.setNotificationGroupId("group-1");
        audio.setNotificationGroupTitle("Video");
        audio.setNotificationGroupSize(2);

        DownloadNotification notification = new DownloadNotification();
        notification.addMission(video);

        video.setLength(1000);
        video.setDone(1000);
        notification.markFinished(video);

        // the group is not complete as long as the second task did not finish
        assertFalse(notification.isComplete());
        assertFalse(notification.isTerminal());
        assertTrue(notification.hasIncompleteGroups());

        // the same must hold when the group size is re-reported by a late task
        audio.setLength(1000);
        audio.setDone(100);
        notification.addMission(audio);
        assertFalse(notification.isComplete());
        assertFalse(notification.isTerminal());
        assertTrue(notification.hasIncompleteGroups());

        audio.setDone(1000);
        notification.markFinished(audio);
        assertTrue(notification.isComplete());
        assertTrue(notification.isTerminal());
        assertFalse(notification.hasIncompleteGroups());
    }

    @Test
    void downloadNotificationRecoversWhenGroupGrowsAfterFailure() {
        File videoTarget = new File(tempDir, "video.mp4");
        File audioTarget = new File(tempDir, "audio.webm");
        DownloadMission video = new DownloadMission(
                new String[]{"https://example.com/video"}, videoTarget);
        video.setNotificationGroupId("group-1");
        video.setNotificationGroupTitle("Video");
        video.setNotificationGroupSize(2);
        DownloadMission audio = new DownloadMission(
                new String[]{"https://example.com/audio"}, audioTarget);
        audio.setNotificationGroupId("group-1");
        audio.setNotificationGroupTitle("Video");
        audio.setNotificationGroupSize(2);

        DownloadNotification notification = new DownloadNotification();
        notification.addMission(video);

        video.setLength(1000);
        video.setDone(10);
        notification.markFailed(video);
        assertTrue(notification.hasFailure());
        assertTrue(notification.isTerminal());

        // a later task must not flip the failed group back to "complete"
        audio.setLength(1000);
        audio.setDone(100);
        notification.addMission(audio);
        assertTrue(notification.hasFailure());
        assertTrue(notification.isTerminal());
        assertFalse(notification.isComplete());
    }

    @Test
    void downloadNotificationResolvesGroupWithoutAllTasks() {
        File videoTarget = new File(tempDir, "video.mp4");
        File audioTarget = new File(tempDir, "audio.webm");
        DownloadMission video = new DownloadMission(
                new String[]{"https://example.com/video"}, videoTarget);
        video.setNotificationGroupId("group-1");
        video.setNotificationGroupTitle("Video");
        video.setNotificationGroupSize(2);

        DownloadNotification notification = new DownloadNotification();
        notification.addMission(video);
        video.setLength(1000);
        video.setDone(1000);
        notification.markFinished(video);

        // the audio file of the pair is no longer tracked (e.g. the process
        // was restarted): nothing can make progress for the group any more
        assertTrue(notification.hasIncompleteGroups());
        notification.markGroupComplete("group-1");
        assertTrue(notification.isComplete());
        assertTrue(notification.isTerminal());
        assertFalse(notification.hasIncompleteGroups());

        // a failed group must not be flipped to complete
        DownloadMission failed = new DownloadMission(
                new String[]{"https://example.com/faulty"}, new File(tempDir, "faulty.mp4"));
        failed.setNotificationGroupId("group-2");
        failed.setNotificationGroupTitle("Faulty");
        failed.setNotificationGroupSize(1);
        notification.addMission(failed);
        failed.setLength(100);
        failed.setDone(10);
        notification.markFailed(failed);
        notification.markGroupComplete("group-2");
        DownloadNotification.Group group = notification.getGroup("group-2");
        assertNotNull(group);
        assertTrue(group.isFailed());
        assertTrue(group.isTerminal());
        assertFalse(group.isComplete());
    }

    @Test
    void downloadNotificationRemovesTerminalGroup() {
        File videoTarget = new File(tempDir, "video.mp4");
        File audioTarget = new File(tempDir, "audio.webm");
        DownloadMission video = new DownloadMission(
                new String[]{"https://example.com/video"}, videoTarget);
        video.setNotificationGroupId("group-1");
        video.setNotificationGroupTitle("Video");
        video.setNotificationGroupSize(2);
        DownloadMission audio = new DownloadMission(
                new String[]{"https://example.com/audio"}, audioTarget);
        audio.setNotificationGroupId("group-1");
        audio.setNotificationGroupTitle("Video");
        audio.setNotificationGroupSize(2);
        DownloadMission other = new DownloadMission(
                new String[]{"https://example.com/other"}, new File(tempDir, "other.mp4"));
        other.setNotificationGroupId("group-2");
        other.setNotificationGroupTitle("Other");
        other.setNotificationGroupSize(1);

        DownloadNotification notification = new DownloadNotification();
        notification.addMission(video);
        notification.addMission(audio);
        notification.addMission(other);
        video.setLength(1000);
        video.setDone(1000);
        audio.setLength(1000);
        audio.setDone(1000);
        other.setLength(400);
        other.setDone(0);
        notification.markFinished(video);
        notification.markFinished(audio);

        DownloadNotification.Group group = notification.getGroup("group-1");
        assertNotNull(group);
        assertTrue(group.isComplete());
        assertFalse(group.isFailed());
        assertEquals("Video", group.getTitle());
        assertEquals(2, group.getExpected());
        assertEquals(2, group.getFinishedCount());
        assertEquals(0, group.getFailedCount());

        // posting the final notification of the finished group removes it
        // from the aggregate; the other group keeps tracking progress
        notification.removeGroup("group-1");
        assertNull(notification.getGroup("group-1"));
        assertEquals(1, notification.getGroups().size());
        assertEquals(400, notification.totalLength());
        assertEquals(0, notification.totalDone());
        assertEquals(0, notification.getProgress());
        assertEquals("Other", notification.getTitle());

        notification.removeGroup("group-2");
        assertTrue(notification.isEmpty());
        assertTrue(notification.getMissions().isEmpty());
    }

    @Test
    void downloadNotificationTreatsUnrelatedMissionsAsOwnGroup() {
        DownloadMission mission = new DownloadMission(
                new String[]{"https://example.com/solo"}, new File(tempDir, "solo.mp4"));
        // no notificationGroupId: the mission forms a group of its own

        DownloadNotification notification = new DownloadNotification();
        notification.addMission(mission);

        mission.setLength(100);
        mission.setDone(100);
        notification.markFinished(mission);

        assertTrue(notification.isComplete());
        assertTrue(notification.isTerminal());
        assertEquals(1, notification.getGroups().size());
        assertEquals(mission.getId(), notification.getGroups().get(0).id);
    }

    @Test
    void downloadNotificationDoesNotResurrectResolvedGroup() {
        DownloadMission mission = new DownloadMission(
                new String[]{"https://example.com/video"}, new File(tempDir, "video.mp4"));
        mission.setNotificationGroupId("group-1");
        mission.setNotificationGroupSize(1);
        mission.setLength(100);
        mission.setDone(100);

        DownloadNotification notification = new DownloadNotification();
        notification.addMission(mission);
        notification.markFinished(mission);
        assertTrue(notification.isTerminal());

        // posting the final notification removes the group
        notification.removeGroup("group-1");
        assertTrue(notification.isEmpty());

        // late events of the same download must not re-create the group,
        // otherwise the aggregate notification would never go away
        notification.addMission(mission);
        notification.markFinished(mission);
        notification.markFailed(mission);
        assertTrue(notification.isEmpty());
        assertFalse(notification.hasFailure());
        assertFalse(notification.hasIncompleteGroups());
    }

    /**
     * Minimal HTTP server that supports HEAD and Range requests, so the
     * multi-threaded engine can be exercised without external dependencies.
     */
    private static class RangeServer implements AutoCloseable {
        private final byte[] content;
        private final boolean supportRanges;
        private final ServerSocket serverSocket;
        private volatile boolean running = true;

        RangeServer(byte[] content) throws Exception {
            this(content, true);
        }

        RangeServer(byte[] content, boolean supportRanges) throws Exception {
            this.content = content;
            this.supportRanges = supportRanges;
            this.serverSocket = new ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"));

            Thread acceptThread = new Thread(this::acceptLoop, "test-http-server");
            acceptThread.setDaemon(true);
            acceptThread.start();
        }

        int getPort() {
            return serverSocket.getLocalPort();
        }

        private void acceptLoop() {
            while (running) {
                try {
                    Socket socket = serverSocket.accept();
                    Thread connection = new Thread(() -> handle(socket), "test-http-connection");
                    connection.setDaemon(true);
                    connection.start();
                } catch (Exception e) {
                    if (running) {
                        e.printStackTrace();
                    }
                    return;
                }
            }
        }

        private void handle(Socket socket) {
            try (Socket s = socket) {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(s.getInputStream(), StandardCharsets.ISO_8859_1));
                String requestLine = reader.readLine();
                if (requestLine == null) {
                    return;
                }

                boolean head = requestLine.startsWith("HEAD ");
                String range = null;
                String line;
                while ((line = reader.readLine()) != null && !line.isEmpty()) {
                    int colon = line.indexOf(':');
                    if (colon > 0 && "Range".equalsIgnoreCase(line.substring(0, colon).trim())) {
                        range = line.substring(colon + 1).trim();
                    }
                }

                OutputStream out = s.getOutputStream();
                if (!supportRanges || range == null || !range.startsWith("bytes=")) {
                    writeHeaders(out, "200 OK", content.length, null);
                    if (!head) {
                        out.write(content);
                    }
                    out.flush();
                    return;
                }

                String spec = range.substring("bytes=".length());
                String[] parts = spec.split("-", -1);
                int start = parts[0].isEmpty() ? 0 : Integer.parseInt(parts[0]);
                int end = parts.length > 1 && !parts[1].isEmpty()
                        ? Integer.parseInt(parts[1])
                        : content.length - 1;
                end = Math.min(end, content.length - 1);
                if (start > end) {
                    writeHeaders(out, "416 Range Not Satisfiable", 0, null);
                    out.flush();
                    return;
                }

                int length = end - start + 1;
                writeHeaders(out, "206 Partial Content", length,
                        "bytes " + start + "-" + end + "/" + content.length);
                if (!head) {
                    out.write(Arrays.copyOfRange(content, start, end + 1));
                }
                out.flush();
            } catch (Exception e) {
                if (running) {
                    e.printStackTrace();
                }
            }
        }

        private static void writeHeaders(OutputStream out, String status, int length,
                                         String contentRange) throws Exception {
            StringBuilder headers = new StringBuilder()
                    .append("HTTP/1.1 ").append(status).append("\r\n")
                    .append("Accept-Ranges: bytes\r\n")
                    .append("Content-Length: ").append(length).append("\r\n")
                    .append("Connection: close\r\n");
            if (contentRange != null) {
                headers.append("Content-Range: ").append(contentRange).append("\r\n");
            }
            headers.append("\r\n");
            out.write(headers.toString().getBytes(StandardCharsets.ISO_8859_1));
        }

        @Override
        public void close() throws Exception {
            running = false;
            serverSocket.close();
        }
    }
}
