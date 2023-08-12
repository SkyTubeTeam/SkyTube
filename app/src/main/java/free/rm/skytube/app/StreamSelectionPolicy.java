/*
 * SkyTube
 * Copyright (C) 2020  Zsombor Gegesy
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

package free.rm.skytube.app;

import android.content.Context;
import android.net.Uri;

import org.schabi.newpipe.extractor.MediaFormat;
import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.VideoStream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import free.rm.skytube.R;
import free.rm.skytube.BuildConfig;
import free.rm.skytube.businessobjects.Logger;
import free.rm.skytube.businessobjects.YouTube.VideoStream.VideoQuality;
import free.rm.skytube.businessobjects.YouTube.VideoStream.VideoResolution;

public class StreamSelectionPolicy {
    private final static List<MediaFormat> VIDEO_FORMAT_QUALITY = Arrays.asList(MediaFormat.WEBM, MediaFormat.MPEG_4, MediaFormat.v3GPP);

    private final boolean allowVideoOnly;
    private final VideoResolution maxResolution;
    private final VideoResolution minResolution;
    private final VideoQuality videoQuality;

    public StreamSelectionPolicy(boolean allowVideoOnly, VideoResolution maxResolution, VideoResolution minResolution, VideoQuality videoQuality) {
        this.allowVideoOnly = allowVideoOnly;
        this.maxResolution = maxResolution != VideoResolution.RES_UNKNOWN ? maxResolution : null;
        this.minResolution = minResolution != VideoResolution.RES_UNKNOWN ? minResolution : null;
        this.videoQuality = videoQuality;
    }

    public StreamSelectionPolicy withAllowVideoOnly(boolean newValue) {
        return new StreamSelectionPolicy(newValue, maxResolution, minResolution, videoQuality);
    }

    public StreamSelection select(StreamInfo streamInfo) {
        VideoStreamWithResolution videoStreamWithResolution = pickVideo(streamInfo);
        if (videoStreamWithResolution != null) {
            if (videoStreamWithResolution.videoStream.isVideoOnly()) {
                AudioStream audioStream = pickAudio(streamInfo);
                if (audioStream != null) {
                    return new StreamSelection(videoStreamWithResolution.videoStream, videoStreamWithResolution.resolution, audioStream);
                }
            } else {
                return new StreamSelection(videoStreamWithResolution.videoStream, videoStreamWithResolution.resolution, null);
            }
        }
        return null;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("StreamSelectionPolicy{");
        sb.append("allowVideoOnly=").append(allowVideoOnly);
        if (maxResolution != null) {
            sb.append(", maxResolution=").append(maxResolution);
        }
        if (minResolution != null) {
            sb.append(", minResolution=").append(minResolution);
        }
        sb.append(", videoQuality=").append(videoQuality);
        sb.append('}');
        return sb.toString();
    }

    public String getErrorMessage(Context context) {
        String min = "*";
        String max = "*";
        if (minResolution != null) {
            min = minResolution.name();
        }
        if (maxResolution != null) {
            max = maxResolution.name();
        }
        return context.getString(R.string.video_stream_not_found_with_request_resolution, min, max);
    }

    private AudioStream pickAudio(StreamInfo streamInfo) {
        if (BuildConfig.DEBUG) {
            for (AudioStream vs : streamInfo.getAudioStreams()) {
                Logger.d(this, "AudioStream %s", toHumanReadable(vs));
            }
        }
        AudioStream best = null;
        for (AudioStream audioStream : streamInfo.getAudioStreams()) {
            if (isBetter(best, audioStream)) {
                if (BuildConfig.DEBUG) {
                    Logger.d(this, "better %s -> %s", toHumanReadable(best), toHumanReadable(audioStream));
                }
                best = audioStream;
            }
        }
        if (BuildConfig.DEBUG) {
            Logger.d(this, "best %s", toHumanReadable(best));
        }
        return best;
    }

    private static String toHumanReadable(AudioStream as) {
        return as != null ? "AudioStream(" + as.getAverageBitrate() + ", " + as.getFormat() + ", codec=" + as.getCodec() + ", q=" + as.getQuality() + ", isUrl=" + as.isUrl() + ",delivery=" + as.getDeliveryMethod() + ")" : "NULL";
    }

    private boolean isBetter(AudioStream best, AudioStream other) {
        if (best == null) {
            return true;
        }
        switch (videoQuality) {
            case LEAST_BANDWITH:
                return other.getAverageBitrate() < best.getAverageBitrate();
            case BEST_QUALITY:
                return best.getAverageBitrate() < other.getAverageBitrate();
        }
        throw new IllegalStateException("Unexpected videoQuality:" + videoQuality);
    }

    private static boolean isSecondBetterFormat(VideoStream stream1, VideoStream stream2) {
        final int format1Idx = VIDEO_FORMAT_QUALITY.indexOf(stream1.getFormat());
        final int format2Idx = VIDEO_FORMAT_QUALITY.indexOf(stream2.getFormat());
        if (format2Idx < 0) {
            return false;
        }
        if (format1Idx < 0) {
            return true;
        }
        return (format2Idx < format1Idx);
    }

    private VideoStreamWithResolution pickVideo(StreamInfo streamInfo) {
        List<VideoStream> streams = streamInfo.getVideoStreams();
        if (allowVideoOnly) {
            streams = new ArrayList<>(streams);
            streams.addAll(streamInfo.getVideoOnlyStreams());
        }
        if (BuildConfig.DEBUG) {
            for (VideoStream vs : streams) {
                Logger.d(this, "found %s", VideoStreamWithResolution.toHumanReadable(vs));
            }
        }
        return pick(streams);
    }

    private VideoStreamWithResolution pick(Collection<VideoStream> streams) {
        VideoStreamWithResolution best = null;
        for (VideoStream stream : streams) {
            VideoStreamWithResolution videoStream = new VideoStreamWithResolution(stream);
            if (isAllowed(videoStream.resolution) && isAllowedVideoFormat(videoStream.videoStream.getFormat()) && stream.isUrl()) {
                switch (videoQuality) {
                    case BEST_QUALITY:
                        if (videoStream.isBetterQualityThan(best)) {
                            if (BuildConfig.DEBUG) {
                                Logger.d(this, "better quality %s -> %s", VideoStreamWithResolution.toHumanReadable(best), VideoStreamWithResolution.toHumanReadable(videoStream));
                            }
                            best = videoStream;
                        }
                        break;
                    case LEAST_BANDWITH:
                        if (videoStream.isLessNetworkUsageThan(best)) {
                            if (BuildConfig.DEBUG) {
                                Logger.d(this, "less network %s -> %s", VideoStreamWithResolution.toHumanReadable(best), VideoStreamWithResolution.toHumanReadable(videoStream));
                            }
                            best = videoStream;
                        }
                        break;
                }
            }
        }
        if (BuildConfig.DEBUG) {
            Logger.d(this, "best -> %s", VideoStreamWithResolution.toHumanReadable(best));
        }
        return best;
    }

    private boolean isAllowed(VideoResolution resolution) {
        if (minResolution != null && minResolution.isBetterQualityThan(resolution)) {
            return false;
        }

        if (maxResolution != null && resolution.isBetterQualityThan(maxResolution)) {
            return false;
        }

        return true;
    }

    private boolean isAllowedVideoFormat(MediaFormat format) {
        return VIDEO_FORMAT_QUALITY.contains(format);
    }

    private static class VideoStreamWithResolution {
        final VideoStream videoStream;
        final VideoResolution resolution;

        VideoStreamWithResolution(VideoStream videoStream) {
            this.videoStream = videoStream;
            this.resolution = VideoResolution.resolutionToVideoResolution(videoStream.getResolution());
        }

        boolean isBetterQualityThan(VideoStreamWithResolution other) {
            return other == null || resolution.isBetterQualityThan(other.resolution) || (resolution == other.resolution && isSecondBetterFormat(other.videoStream, videoStream));
        }

        boolean isLessNetworkUsageThan(VideoStreamWithResolution other) {
            return other == null || resolution.isLessNetworkUsageThan(other.resolution) || (resolution == other.resolution && isSecondBetterFormat(other.videoStream, videoStream));
        }

        private String toHumanReadable() {
            return "VideoStream(" + resolution.name() +
                    ", format=" + videoStream.getFormat() +
                    ", codec=" + videoStream.getCodec() +
                    ", quality=" + videoStream.getQuality() +
                    ",videoOnly=" + videoStream.isVideoOnly() +
                    ",isUrl=" + videoStream.isUrl() +
                    ",delivery=" + videoStream.getDeliveryMethod() + ")";
        }

        private static String toHumanReadable(VideoStreamWithResolution v) {
            return v != null ? v.toHumanReadable() : "NULL";
        }

        private static String toHumanReadable(VideoStream v) {
            return v != null ? new VideoStreamWithResolution(v).toHumanReadable() : "NULL";
        }
    }

    public static class StreamSelection {
        final VideoStream videoStream;
        final VideoResolution resolution;
        final AudioStream audioStream;

        StreamSelection(VideoStream videoStream, VideoResolution resolution, AudioStream audioStream) {
            this.videoStream = videoStream;
            this.resolution = resolution;
            this.audioStream = audioStream;
        }

        public VideoStream getVideoStream() {
            return videoStream;
        }

        public Uri getVideoStreamUri() {
            return videoStream.isUrl() ? Uri.parse(videoStream.getContent()) : null;
        }

        public Uri getAudioStreamUri() {
            return audioStream != null && audioStream.isUrl() ? Uri.parse(audioStream.getContent()) : null;
        }

        public VideoResolution getResolution() {
            return resolution;
        }

        public AudioStream getAudioStream() {
            return audioStream;
        }
    }
}
