package free.rm.skytube.app;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.schabi.newpipe.extractor.MediaFormat;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.StreamType;
import org.schabi.newpipe.extractor.stream.VideoStream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import free.rm.skytube.businessobjects.YouTube.VideoStream.VideoQuality;
import free.rm.skytube.businessobjects.YouTube.VideoStream.VideoResolution;

public class StreamSelectionPolicyTest {

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
        StreamSelectionPolicy policy = new StreamSelectionPolicy(false, VideoResolution.RES_1080P, VideoResolution.RES_480P, VideoQuality.LEAST_BANDWITH);
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
        StreamSelectionPolicy policy = new StreamSelectionPolicy(false, VideoResolution.RES_1080P, VideoResolution.RES_480P, VideoQuality.LEAST_BANDWITH);

        test(policy, MediaFormat.MPEG_4, MediaFormat.v3GPP, MediaFormat.MPEG_4);
        test(policy, MediaFormat.WEBM, MediaFormat.v3GPP, MediaFormat.WEBM, MediaFormat.MPEG_4);
        test(policy, MediaFormat.MPEG_4, MediaFormat.VTT, MediaFormat.MPEG_4);
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
            streams.add(new VideoStream("url/" + mediaFormat, mediaFormat, "1080P"));
        }
        return createStreams(streams);
    }

    private StreamInfo createStreams(String... resolutions) {
        List<VideoStream> streams = new ArrayList<>();
        for (String resolution : resolutions) {
            streams.add(new VideoStream("url/" + resolution, MediaFormat.WEBM, resolution));
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
}
