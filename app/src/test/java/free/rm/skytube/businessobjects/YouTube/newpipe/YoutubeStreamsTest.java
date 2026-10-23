package free.rm.skytube.businessobjects.YouTube.newpipe;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.github.skytube.components.httpclient.OkHttpDownloader;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.VideoStream;

import free.rm.skytube.app.StreamSelectionPolicy;
import free.rm.skytube.businessobjects.YouTube.VideoStream.VideoQuality;
import free.rm.skytube.businessobjects.YouTube.VideoStream.VideoResolution;

/**
 * Loads all the streams of a real video to inspect what the extractor returns.
 * Hits the network, so it is meant for manual troubleshooting.
 */
public class YoutubeStreamsTest {

    private static final String VIDEO_URL = "https://youtu.be/v3PDXW-jzwM";

    @BeforeAll
    public static void init() {
        NewPipe.init(OkHttpDownloader.init(new okhttp3.OkHttpClient.Builder()));
    }

    @Test
    public void printAllStreams() throws Exception {
        StreamInfo info = StreamInfo.getInfo(ServiceList.YouTube, VIDEO_URL);
        assertNotNull(info);

        System.out.println("=== " + VIDEO_URL + " ===");
        System.out.println("progressive (getVideoStreams): " + info.getVideoStreams().size());
        for (VideoStream vs : info.getVideoStreams()) {
            System.out.println("  " + describe(vs));
        }
        System.out.println("video-only (getVideoOnlyStreams): " + info.getVideoOnlyStreams().size());
        for (VideoStream vs : info.getVideoOnlyStreams()) {
            System.out.println("  " + describe(vs));
        }
        System.out.println("audio (getAudioStreams): " + info.getAudioStreams().size());
        for (AudioStream as : info.getAudioStreams()) {
            System.out.println("  AUDIO bitrate=" + as.getAverageBitrate()
                    + " format=" + as.getFormat() + " isUrl=" + as.isUrl());
        }

        assertFalse(info.getVideoStreams().isEmpty() && info.getVideoOnlyStreams().isEmpty(),
                "the extractor returned no video streams at all");

        // Mirror the download selection: prefer a muxed stream, fall back to video-only + audio.
        StreamSelectionPolicy downloadPolicy = new StreamSelectionPolicy(true,
                VideoResolution.RES_1080P, VideoResolution.RES_UNKNOWN, VideoQuality.BEST_QUALITY);
        StreamSelectionPolicy.StreamSelection muxed = downloadPolicy.withAllowVideoOnly(false).select(info);
        StreamSelectionPolicy.StreamSelection selection = muxed != null ? muxed : downloadPolicy.select(info);
        System.out.println("muxed selection -> " + muxed);
        System.out.println("download selection -> " + selection);
        System.out.println("=== END ===");
        assertNotNull(selection, "the download selection should not be null");
        assertNotNull(selection.getVideoStream(), "the download selection must contain a video stream");
    }

    private static String describe(VideoStream vs) {
        return "res=" + vs.getResolution() + " format=" + vs.getFormat()
                + " videoOnly=" + vs.isVideoOnly() + " isUrl=" + vs.isUrl()
                + " delivery=" + vs.getDeliveryMethod();
    }
}
