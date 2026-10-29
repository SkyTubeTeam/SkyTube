package free.rm.skytube.app;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.schabi.newpipe.extractor.MediaFormat;
import org.schabi.newpipe.extractor.stream.DeliveryMethod;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.StreamType;
import org.schabi.newpipe.extractor.stream.VideoStream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import free.rm.skytube.businessobjects.YouTube.VideoStream.VideoQuality;
import free.rm.skytube.businessobjects.YouTube.VideoStream.VideoResolution;

public class StreamSelectionPolicyTest {

    private static final String HLS_URL = "https://example.com/manifest.m3u8";

    @Test
    void testBestVideoQualitySelection() {
        StreamSelectionPolicy policy = new StreamSelectionPolicy(false, VideoResolution.RES_1080P, VideoResolution.RES_480P, VideoQuality.BEST_QUALITY);
        test(policy, "1080P", "480P", "720P", "1080P", "1440P");
        test(policy, "1080P", "480P", "720P", "1080P");
        test(policy, "720P", "480P", "720P");
        test(policy, null, "144P", "1440P");
    }

    @Test
    void testLeastNetworkUsageSelection() {
        StreamSelectionPolicy policy = new StreamSelectionPolicy(false, VideoResolution.RES_1080P, VideoResolution.RES_480P, VideoQuality.LEAST_BANDWIDTH);
        test(policy, "480P", "144P", "480P", "720P", "1080P", "1440P");
        test(policy, "480P", "480P", "720P", "1080P");
        test(policy, "720P", "720P", "1080P");
        test(policy, null, "144P", "1440P");
    }

    @Test
    void testBetterFormatSelection() {
        StreamSelectionPolicy policy = new StreamSelectionPolicy(false, VideoResolution.RES_1080P, VideoResolution.RES_480P, VideoQuality.BEST_QUALITY);

        test(policy, MediaFormat.MPEG_4, MediaFormat.v3GPP, MediaFormat.MPEG_4);
        test(policy, MediaFormat.WEBM, MediaFormat.v3GPP, MediaFormat.WEBM, MediaFormat.MPEG_4);
        test(policy, MediaFormat.MPEG_4, MediaFormat.VTT, MediaFormat.MPEG_4);
    }

    @Test
    void testBetterFormatSelectionForLeastBandwith() {
        StreamSelectionPolicy policy = new StreamSelectionPolicy(false, VideoResolution.RES_1080P, VideoResolution.RES_480P, VideoQuality.LEAST_BANDWIDTH);

        test(policy, MediaFormat.MPEG_4, MediaFormat.v3GPP, MediaFormat.MPEG_4);
        test(policy, MediaFormat.WEBM, MediaFormat.v3GPP, MediaFormat.WEBM, MediaFormat.MPEG_4);
        test(policy, MediaFormat.MPEG_4, MediaFormat.VTT, MediaFormat.MPEG_4);
    }

    @Test
    void testGetAvailableResolutionsOfEmptyStreamInfo() {
        StreamSelectionPolicy policy = policy(false);

        Assertions.assertTrue(policy.getAvailableResolutions(streamInfo()).isEmpty());
    }

    @Test
    void testGetAvailableResolutionsAreDeduplicatedAndSortedHighestFirst() {
        StreamSelectionPolicy policy = policy(false);
        StreamInfo streamInfo = streamInfo(
                videoStream("720P", MediaFormat.WEBM, true),
                videoStream("1080P", MediaFormat.WEBM, true),
                videoStream("480P", MediaFormat.WEBM, true),
                videoStream("720P", MediaFormat.MPEG_4, true));

        Assertions.assertEquals(
                Arrays.asList(VideoResolution.RES_1080P, VideoResolution.RES_720P, VideoResolution.RES_480P),
                policy.getAvailableResolutions(streamInfo));
    }

    @Test
    void testGetAvailableResolutionsSkipsUnsupportedFormats() {
        StreamSelectionPolicy policy = policy(false);
        StreamInfo streamInfo = streamInfo(
                videoStream("1080P", MediaFormat.WEBM, true),
                videoStream("720P", MediaFormat.VTT, true));

        Assertions.assertEquals(
                Collections.singletonList(VideoResolution.RES_1080P),
                policy.getAvailableResolutions(streamInfo));
    }

    @Test
    void testGetAvailableResolutionsSkipsNonUrlAndUnknownResolutions() {
        StreamSelectionPolicy policy = policy(false);
        StreamInfo streamInfo = streamInfo(
                videoStream("1080P", MediaFormat.WEBM, true),
                videoStream("720P", MediaFormat.WEBM, false),
                videoStream("999P", MediaFormat.WEBM, true));

        Assertions.assertEquals(
                Collections.singletonList(VideoResolution.RES_1080P),
                policy.getAvailableResolutions(streamInfo));
    }

    @Test
    void testGetAvailableResolutionsExcludesVideoOnlyWhenNotAllowed() {
        StreamSelectionPolicy policy = policy(false);
        StreamInfo streamInfo = streamInfo(
                Collections.singletonList(videoStream("1080P", MediaFormat.WEBM, true)),
                Collections.singletonList(videoStream("2160P", MediaFormat.WEBM, true, true)));

        Assertions.assertEquals(
                Collections.singletonList(VideoResolution.RES_1080P),
                policy.getAvailableResolutions(streamInfo));
    }

    @Test
    void testGetAvailableResolutionsIncludesVideoOnlyWhenAllowed() {
        StreamSelectionPolicy policy = policy(true);
        StreamInfo streamInfo = streamInfo(
                Collections.singletonList(videoStream("1080P", MediaFormat.WEBM, true)),
                Collections.singletonList(videoStream("2160P", MediaFormat.WEBM, true, true)));

        Assertions.assertEquals(
                Arrays.asList(VideoResolution.RES_2160P, VideoResolution.RES_1080P),
                policy.getAvailableResolutions(streamInfo));
    }

    @Test
    void testHlsFallbackIsUsedWhenNoStreamsAreAvailable() {
        StreamInfo streamInfo = streamInfo();
        streamInfo.setHlsUrl(HLS_URL);

        Assertions.assertNull(policy(true).select(streamInfo),
                "Without the HLS fallback nothing should be selected");

        StreamSelectionPolicy.StreamSelection selection =
                policy(true).withHlsFallback(true).select(streamInfo);
        Assertions.assertNotNull(selection);
        Assertions.assertTrue(selection.isHls());
        Assertions.assertEquals(HLS_URL, selection.getHlsUrl());
        Assertions.assertNull(selection.getVideoStream());
        Assertions.assertNull(selection.getAudioStream());
    }

    @Test
    void testHlsFallbackIsUsedWhenOnlyVideoOnlyStreamsAreAvailable() {
        StreamInfo streamInfo = streamInfo(
                Collections.emptyList(),
                Collections.singletonList(videoStream("1080P", MediaFormat.WEBM, true, true)));
        streamInfo.setHlsUrl(HLS_URL);

        Assertions.assertNull(policy(false).select(streamInfo),
                "Video-only streams are not allowed by the policy");

        StreamSelectionPolicy.StreamSelection selection =
                policy(false).withHlsFallback(true).select(streamInfo);
        Assertions.assertNotNull(selection);
        Assertions.assertTrue(selection.isHls());
    }

    @Test
    void testHlsFallbackIsUsedWhenVideoOnlyStreamHasNoAudio() {
        StreamInfo streamInfo = streamInfo(
                Collections.emptyList(),
                Collections.singletonList(videoStream("1080P", MediaFormat.WEBM, true, true)));
        streamInfo.setHlsUrl(HLS_URL);

        Assertions.assertNull(policy(true).select(streamInfo),
                "A video-only stream without audio is not playable");

        StreamSelectionPolicy.StreamSelection selection =
                policy(true).withHlsFallback(true).select(streamInfo);
        Assertions.assertNotNull(selection);
        Assertions.assertTrue(selection.isHls());
    }

    @Test
    void testStreamsArePreferredOverHls() {
        StreamInfo streamInfo = streamInfo(videoStream("720P", MediaFormat.MPEG_4, true));
        streamInfo.setHlsUrl(HLS_URL);

        StreamSelectionPolicy.StreamSelection selection =
                policy(false).withHlsFallback(true).select(streamInfo);
        Assertions.assertNotNull(selection);
        Assertions.assertFalse(selection.isHls());
        Assertions.assertEquals("720P", selection.getVideoStream().resolution);
    }

    @Test
    void testNoHlsSelectionWhenThereIsNoManifest() {
        StreamInfo streamInfo = streamInfo();

        Assertions.assertNull(policy(true).withHlsFallback(true).select(streamInfo));
    }

    @Test
    void testHlsFallbackIsPreservedByPolicyCopies() {
        StreamInfo streamInfo = streamInfo();
        streamInfo.setHlsUrl(HLS_URL);

        StreamSelectionPolicy.StreamSelection selection = policy(true)
                .withAllowVideoOnly(false)
                .withResolution(VideoResolution.RES_720P)
                .withHlsFallback(true)
                .select(streamInfo);
        Assertions.assertNotNull(selection);
        Assertions.assertTrue(selection.isHls());
    }

    private void test(StreamSelectionPolicy policy, String expectedResolution, String... resolutions) {
        StreamInfo streamInfo = createStreams(resolutions);
        StreamSelectionPolicy.StreamSelection selection = policy.select(streamInfo);
        if (expectedResolution != null) {
            Assertions.assertEquals(expectedResolution, selection.getVideoStream().resolution, "Expecting " + policy + ", available resolutions: " + Arrays.asList(resolutions));
        } else {
            Assertions.assertNull(selection, "Nothing should match");
        }
    }

    private void test(StreamSelectionPolicy policy, MediaFormat expectedFormat, MediaFormat... mediaFormats) {
        StreamInfo streamInfo = createStreams(mediaFormats);
        StreamSelectionPolicy.StreamSelection selection = policy.select(streamInfo);
        if (expectedFormat != null) {
            Assertions.assertEquals(expectedFormat, selection.getVideoStream().getFormat(), "Expecting " + policy + ", available formats: " + Arrays.asList(mediaFormats));
        } else {
            Assertions.assertNull(selection, "Nothing should match");
        }
    }

    private StreamInfo createStreams(MediaFormat... mediaFormats) {
        List<VideoStream> streams = new ArrayList<>();
        for (MediaFormat mediaFormat : mediaFormats) {
            streams.add(
                new VideoStream.Builder()
                    .setId("id-"+mediaFormat.hashCode())
                        .setContent("url/" + mediaFormat, true)
                        .setMediaFormat(mediaFormat)
                        .setResolution("1080P")
                        .setDeliveryMethod(DeliveryMethod.PROGRESSIVE_HTTP)
                        .setIsVideoOnly(false)
                        .build());
        }
        return createStreams(streams);
    }

    private StreamInfo createStreams(String... resolutions) {
        List<VideoStream> streams = new ArrayList<>();
        for (String resolution : resolutions) {
            streams.add(
                new VideoStream.Builder()
                    .setId("id-"+resolution.hashCode())
                    .setContent("url/" + resolution, true)
                    .setMediaFormat(MediaFormat.WEBM)
                    .setResolution(resolution)
                    .setIsVideoOnly(false)
                    .setDeliveryMethod(DeliveryMethod.DASH)
                    .build());
        }
        return createStreams(streams);
    }

    private StreamInfo createStreams(List<VideoStream> streams) {
        StreamInfo si = createStreamInfo();
        si.setVideoStreams(streams);
        return si;
    }

    private StreamInfo createStreamInfo() {
        return new StreamInfo(0, "url", "originalUrl", StreamType.VIDEO_STREAM, "id", "name", -1);
    }

    private static StreamSelectionPolicy policy(boolean allowVideoOnly) {
        return new StreamSelectionPolicy(allowVideoOnly, VideoResolution.RES_UNKNOWN, VideoResolution.RES_UNKNOWN, VideoQuality.BEST_QUALITY);
    }

    private StreamInfo streamInfo(VideoStream... videoStreams) {
        return streamInfo(Arrays.asList(videoStreams), Collections.emptyList());
    }

    private StreamInfo streamInfo(List<VideoStream> videoStreams, List<VideoStream> videoOnlyStreams) {
        StreamInfo streamInfo = createStreamInfo();
        streamInfo.setVideoStreams(videoStreams);
        streamInfo.setVideoOnlyStreams(videoOnlyStreams);
        return streamInfo;
    }

    private static VideoStream videoStream(String resolution, MediaFormat format, boolean isUrl) {
        return videoStream(resolution, format, isUrl, false);
    }

    private static VideoStream videoStream(String resolution, MediaFormat format, boolean isUrl, boolean videoOnly) {
        return new VideoStream.Builder()
                .setId("id-" + resolution + "-" + format + "-" + isUrl + "-" + videoOnly)
                .setContent("url/" + resolution, isUrl)
                .setMediaFormat(format)
                .setResolution(resolution)
                .setDeliveryMethod(DeliveryMethod.DASH)
                .setIsVideoOnly(videoOnly)
                .build();
    }
}
