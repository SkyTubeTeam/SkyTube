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
        mission.title = "Test video";
        mission.notificationGroupId = "group-1";
        mission.notificationGroupTitle = "Group title";
        mission.notificationGroupSize = 2;
        mission.length = 1024;
        mission.done = 512;

        File metadata = new File(tempDir, "mission.meta");
        DownloadUtils.writeToFile(metadata, mission);

        DownloadMission restored = DownloadUtils.readFromFile(metadata);

        assertEquals(mission.id, restored.id);
        assertEquals(target, restored.targetFile);
        assertEquals("Test video", restored.title);
        assertEquals("group-1", restored.notificationGroupId);
        assertEquals("Group title", restored.notificationGroupTitle);
        assertEquals(2, restored.notificationGroupSize);
        assertEquals(1024, restored.length);
        assertEquals(512, restored.done);
        assertEquals("https://example.com/video.mp4", restored.urls[0]);
        assertNotNull(restored.threads);
        assertEquals(0, restored.threads.length);
        assertNull(restored.init);
        assertEquals(3, restored.maxRetry);
        assertFalse(restored.running);
    }

    @Test
    void progressIsCalculatedFromLength() {
        File target = new File(tempDir, "video.mp4");
        DownloadMission mission = new DownloadMission(new String[]{"https://example.com"}, target);

        mission.length = 1000;
        mission.done = 250;

        assertEquals(25, mission.getProgress());
        assertEquals(new File(tempDir, "video.mp4.part"), mission.getPartFile());
    }

    @Test
    void progressFallsBackToNearLength() {
        File target = new File(tempDir, "video.mp4");
        DownloadMission mission = new DownloadMission(new String[]{"https://example.com"}, target);

        mission.nearLength = 2000;
        mission.done = 500;

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
            mission.threadCount = 3;

            mission.start();

            long deadline = System.currentTimeMillis() + 30_000;
            while (!target.isFile() && System.currentTimeMillis() < deadline) {
                Thread.sleep(50);
            }

            assertTrue(target.isFile(), "The download did not finish in time");
            assertArrayEquals(content, Files.readAllBytes(target.toPath()));
            assertFalse(new File(tempDir, "file.bin.part").exists());
            assertEquals(content.length, mission.done);
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
            mission.threadCount = 3;

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
        video.notificationGroupId = "group-1";
        video.notificationGroupTitle = "Video";
        video.notificationGroupSize = 2;
        DownloadMission audio = new DownloadMission(
                new String[]{"https://example.com/audio"}, audioTarget);
        audio.notificationGroupId = "group-1";
        audio.notificationGroupTitle = "Video";
        audio.notificationGroupSize = 2;

        DownloadNotification notification = new DownloadNotification();
        notification.addMission(video);

        assertEquals(2, notification.expectedTasks());
        assertEquals(1, notification.seenTasks());
        assertFalse(notification.isTerminal());
        assertFalse(notification.hasFullLength());
        assertEquals(-1, notification.getProgress());

        video.length = 1000;
        video.done = 500;
        notification.addMission(video);
        assertEquals(500, notification.totalDone());
        assertEquals(1000, notification.totalLength());
        assertFalse(notification.hasFullLength());

        video.done = 1000;
        notification.markFinished(video);
        assertFalse(notification.isComplete());
        assertFalse(notification.isTerminal());

        audio.length = 1000;
        audio.done = 250;
        notification.addMission(audio);
        assertEquals(1250, notification.totalDone());
        assertEquals(2000, notification.totalLength());
        assertTrue(notification.hasFullLength());
        assertEquals(62, notification.getProgress());

        audio.done = 1000;
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
        mission.notificationGroupId = "group-1";
        mission.notificationGroupTitle = "Video";
        mission.notificationGroupSize = 1;

        DownloadNotification notification = new DownloadNotification();
        notification.addMission(mission);
        mission.length = 100;
        mission.done = 10;
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
        first.notificationGroupId = "group-1";
        first.notificationGroupTitle = "First";
        first.notificationGroupSize = 1;
        first.length = 100;
        first.done = 50;

        DownloadMission second = new DownloadMission(
                new String[]{"https://example.com/second"}, new File(tempDir, "second.mp4"));
        second.notificationGroupId = "group-2";
        second.notificationGroupTitle = "Second";
        second.notificationGroupSize = 1;
        second.length = 100;
        second.done = 25;

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
        video.notificationGroupId = "group-1";
        video.notificationGroupTitle = "Video";
        video.notificationGroupSize = 2;
        DownloadMission audio = new DownloadMission(
                new String[]{"https://example.com/audio"}, audioTarget);
        audio.notificationGroupId = "group-1";
        audio.notificationGroupTitle = "Video";
        audio.notificationGroupSize = 2;

        DownloadNotification notification = new DownloadNotification();
        notification.addMission(video);

        video.length = 1000;
        video.done = 1000;
        notification.markFinished(video);

        // the group is not complete as long as the second task did not finish
        assertFalse(notification.isComplete());
        assertFalse(notification.isTerminal());
        assertTrue(notification.hasIncompleteGroups());

        // the same must hold when the group size is re-reported by a late task
        audio.length = 1000;
        audio.done = 100;
        notification.addMission(audio);
        assertFalse(notification.isComplete());
        assertFalse(notification.isTerminal());
        assertTrue(notification.hasIncompleteGroups());

        audio.done = 1000;
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
        video.notificationGroupId = "group-1";
        video.notificationGroupTitle = "Video";
        video.notificationGroupSize = 2;
        DownloadMission audio = new DownloadMission(
                new String[]{"https://example.com/audio"}, audioTarget);
        audio.notificationGroupId = "group-1";
        audio.notificationGroupTitle = "Video";
        audio.notificationGroupSize = 2;

        DownloadNotification notification = new DownloadNotification();
        notification.addMission(video);

        video.length = 1000;
        video.done = 10;
        notification.markFailed(video);
        assertTrue(notification.hasFailure());
        assertTrue(notification.isTerminal());

        // a later task must not flip the failed group back to "complete"
        audio.length = 1000;
        audio.done = 100;
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
        video.notificationGroupId = "group-1";
        video.notificationGroupTitle = "Video";
        video.notificationGroupSize = 2;

        DownloadNotification notification = new DownloadNotification();
        notification.addMission(video);
        video.length = 1000;
        video.done = 1000;
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
        failed.notificationGroupId = "group-2";
        failed.notificationGroupTitle = "Faulty";
        failed.notificationGroupSize = 1;
        notification.addMission(failed);
        failed.length = 100;
        failed.done = 10;
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
        video.notificationGroupId = "group-1";
        video.notificationGroupTitle = "Video";
        video.notificationGroupSize = 2;
        DownloadMission audio = new DownloadMission(
                new String[]{"https://example.com/audio"}, audioTarget);
        audio.notificationGroupId = "group-1";
        audio.notificationGroupTitle = "Video";
        audio.notificationGroupSize = 2;
        DownloadMission other = new DownloadMission(
                new String[]{"https://example.com/other"}, new File(tempDir, "other.mp4"));
        other.notificationGroupId = "group-2";
        other.notificationGroupTitle = "Other";
        other.notificationGroupSize = 1;

        DownloadNotification notification = new DownloadNotification();
        notification.addMission(video);
        notification.addMission(audio);
        notification.addMission(other);
        video.length = 1000;
        video.done = 1000;
        audio.length = 1000;
        audio.done = 1000;
        other.length = 400;
        other.done = 0;
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

        mission.length = 100;
        mission.done = 100;
        notification.markFinished(mission);

        assertTrue(notification.isComplete());
        assertTrue(notification.isTerminal());
        assertEquals(1, notification.getGroups().size());
        assertEquals(mission.id, notification.getGroups().get(0).id);
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
