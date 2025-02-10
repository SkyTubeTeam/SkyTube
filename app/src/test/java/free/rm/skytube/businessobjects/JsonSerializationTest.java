/*
 * SkyTube
 * Copyright (C) 2025  Zsombor Gegesy
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
package free.rm.skytube.businessobjects;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import free.rm.skytube.businessobjects.Sponsorblock.SBSegment;
import free.rm.skytube.businessobjects.Sponsorblock.SBVideoInfo;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;
import free.rm.skytube.businessobjects.YouTube.newpipe.ChannelId;
import free.rm.skytube.businessobjects.YouTube.newpipe.VideoId;

class JsonSerializationTest {

    private final String VIDEO_ID = "abc12345";
    private final String TITLE = "myTitle";
    private final String DESCRIPTION = "description";
    private final long DURATION_IN_SECS = 1234L;
    private final String CHANNEL_TITLE = "ChannelTitle";
    private final String CHANNEL_ID = "xyz32412";
    private final YouTubeChannel channel = new YouTubeChannel(CHANNEL_ID, CHANNEL_TITLE);
    private final long VIEW_COUNT = 9_876_654_322L;
    private final long PUBLISH_DATE_TIMESTAMP = 1_000_000_000_00L;
    private final Instant PUBLISH_DATE = Instant.ofEpochMilli(PUBLISH_DATE_TIMESTAMP);
    private final boolean PUBLISH_DATE_IS_EXACT = true;
    private final String THUMBNAIL_URL = "http://something.com";

    private final String ORIGINAL_FORMAT = "{\"channel\":{\"subscriberCount\":0,\"isUserSubscribed\":false,\"lastVisitTime\":0,\"lastCheckTime\":0,\"lastVideoTime\":0,\"youTubeVideos\":[],\"tags\":[],\"id\":\"xyz32412\",\"title\":\"ChannelTitle\",\"publishTimestampExact\":false},\"thumbsUpPercentage\":-1,\"duration\":\"20:34\",\"durationInSeconds\":1234,\"viewsCountInt\":9876654322,\"thumbnailMaxResUrl\":\"http://something.com\",\"isLiveStream\":false,\"id\":\"abc12345\",\"title\":\"myTitle\",\"description\":\"description\",\"publishTimestamp\":100000000000,\"publishTimestampExact\":true,\"thumbnailUrl\":\"http://something.com\"}";
    private final String ARCHAIC_FORMAT = "{\"channelId\":\"xyz32412\", \"channelName\":\"ChannelTitle\",\"thumbsUpPercentage\":-1,\"duration\":\"20:34\",\"durationInSeconds\":1234,\"viewsCountInt\":9876654322,\"thumbnailMaxResUrl\":\"http://something.com\",\"isLiveStream\":false,\"id\":\"abc12345\",\"title\":\"myTitle\",\"description\":\"description\",\"publishTimestamp\":100000000000,\"publishTimestampExact\":true,\"thumbnailUrl\":\"http://something.com\"}";
    private final String MISSING_CHANNEL = "{\"thumbsUpPercentage\":-1,\"duration\":\"20:34\",\"durationInSeconds\":1234,\"viewsCountInt\":9876654322,\"thumbnailMaxResUrl\":\"http://something.com\",\"isLiveStream\":false,\"id\":\"abc12345\",\"title\":\"myTitle\",\"description\":\"description\",\"publishTimestamp\":100000000000,\"publishTimestampExact\":true,\"thumbnailUrl\":\"http://something.com\"}";

    private final String SB_JSON = "{\"videoDuration\":1.02334566E7,\"segments\":[{\"category\":\"cat1\",\"startPos\":2000.0,\"endPos\":4000.0},{\"category\":\"cat2\",\"startPos\":6000.0,\"endPos\":7000.0}]}";
    JsonSerializer serializer;

    YouTubeVideo video;
    SBVideoInfo sbVideoInfo;

    @BeforeEach
    void setup() {
        serializer = new JsonSerializer();
        video = new YouTubeVideo(VIDEO_ID, TITLE, DESCRIPTION, DURATION_IN_SECS, channel, VIEW_COUNT, PUBLISH_DATE, PUBLISH_DATE_IS_EXACT, THUMBNAIL_URL);
        sbVideoInfo = new SBVideoInfo(10_233_456.6);
        sbVideoInfo.getSegments().add(new SBSegment("cat1", 2000.0, 4000.0));
        sbVideoInfo.getSegments().add(new SBSegment("cat2", 6000.0, 7000.0));
    }

    @Test
    void serializationWorks() {
        String string = serializer.toPersistedVideoJson(video);

        Assertions.assertEquals(ORIGINAL_FORMAT, string);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void deserializationWorks(boolean fromString) {
        YouTubeVideo newVideo = fromJson(fromString, ORIGINAL_FORMAT);

        verifyVideoProperties(newVideo);
        verifyChannel(newVideo);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void deserializationWorksForArchaicFormat(boolean fromString) {
        YouTubeVideo newVideo = fromJson(fromString, ARCHAIC_FORMAT);

        verifyVideoProperties(newVideo);
        verifyChannel(newVideo);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void missingChannelParsed(boolean fromString) {
        YouTubeVideo newVideo = fromJson(fromString, MISSING_CHANNEL);

        verifyVideoProperties(newVideo);
        Assertions.assertNull(newVideo.getChannel());
    }

    @Test
    void handleNullInputs() {
        Assertions.assertNull(serializer.fromPersistedVideoJson((String) null));
        Assertions.assertNull(serializer.fromPersistedVideoJson((byte[]) null));
        Assertions.assertEquals("null", serializer.toPersistedVideoJson((YouTubeVideo) null));
    }

    private YouTubeVideo fromJson(boolean fromString, String jsonString) {
        return fromString ? serializer.fromPersistedVideoJson(jsonString) : serializer.fromPersistedVideoJson(jsonString.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void sponsorBlockSerialization() {
        String result = serializer.toPersistedSponsorBlockJson(sbVideoInfo);
        Assertions.assertEquals(SB_JSON, result);
    }

    @Test
    void sponsorBlockDeserialization() {
        SBVideoInfo result = serializer.fromSponsorBlockJson(SB_JSON);
        Assertions.assertEquals(10_233_456.6, result.getVideoDuration());
        Assertions.assertEquals(2, result.getSegments().size());
        SBSegment seg1 = result.getSegments().get(0);
        Assertions.assertEquals("cat1", seg1.getCategory());
        Assertions.assertEquals(2000.0, seg1.getStartPos());
        Assertions.assertEquals(4000.0, seg1.getEndPos());
        SBSegment seg2 = result.getSegments().get(1);
        Assertions.assertEquals("cat2", seg2.getCategory());
        Assertions.assertEquals(6000.0, seg2.getStartPos());
        Assertions.assertEquals(7000.0, seg2.getEndPos());
    }

    private void verifyVideoProperties(YouTubeVideo newVideo) {
        Assertions.assertEquals(VIDEO_ID, newVideo.getId());
        Assertions.assertEquals(TITLE, newVideo.getTitle());
        Assertions.assertEquals(DESCRIPTION, newVideo.getDescription());
        Assertions.assertEquals(DURATION_IN_SECS, newVideo.getDurationInSeconds());
        Assertions.assertEquals(VIEW_COUNT, newVideo.getViewsCountInt());
        Assertions.assertEquals(PUBLISH_DATE_TIMESTAMP, newVideo.getPublishTimestamp());
        Assertions.assertEquals(PUBLISH_DATE_IS_EXACT, newVideo.getPublishTimestampExact());
        Assertions.assertEquals(THUMBNAIL_URL, newVideo.getThumbnailUrl());
        Assertions.assertEquals("https://youtu.be/abc12345", newVideo.getVideoUrl());
        Assertions.assertEquals(VideoId.create(VIDEO_ID), newVideo.getVideoId());
    }

    private void verifyChannel(YouTubeVideo newVideo) {
        Assertions.assertEquals(CHANNEL_TITLE, newVideo.getChannelName());
        Assertions.assertEquals(new ChannelId(CHANNEL_ID), newVideo.getChannelId());
        Assertions.assertNotNull(newVideo.getChannel());
        Assertions.assertEquals(new ChannelId(CHANNEL_ID), newVideo.getChannel().getChannelId());
        Assertions.assertEquals(CHANNEL_TITLE, newVideo.getChannel().getTitle());
    }

}
