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

import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.VideoStream;

import java.util.Collection;

import free.rm.skytube.R;
import free.rm.skytube.businessobjects.YouTube.VideoStream.VideoQuality;
import free.rm.skytube.businessobjects.YouTube.VideoStream.VideoResolution;

public class StreamSelectionPolicy {
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
            if (videoStreamWithResolution.videoStream.isVideoOnly) {
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
        AudioStream best = null;
        for (AudioStream audioStream : streamInfo.getAudioStreams()) {
            if (isBetter(best, audioStream)) {
                best = audioStream;
            }
        }
        return best;
    }

    private boolean isBetter(AudioStream best, AudioStream other) {
        if (best == null) {
            return true;
        }
        switch (videoQuality) {
            case LEAST_BANDWITH: return other.average_bitrate < best.average_bitrate;
            case BEST_QUALITY: return best.average_bitrate < other.average_bitrate;
        }
        throw new IllegalStateException("Unexpected videoQuality:" + videoQuality);
    }

    private VideoStreamWithResolution pickVideo(StreamInfo streamInfo) {
        VideoStreamWithResolution videoStream = pick(streamInfo.getVideoStreams());
        if (allowVideoOnly) {
            VideoStreamWithResolution videoOnlyStream = pick(streamInfo.getVideoOnlyStreams());
            if (videoOnlyStream != null && videoOnlyStream.isBetterQualityThan(videoStream)) {
                return videoOnlyStream;
            }
        }
        return videoStream;
    }

    private VideoStreamWithResolution pick(Collection<VideoStream> streams) {
        VideoStreamWithResolution best = null;
        for (VideoStream stream: streams) {
            VideoStreamWithResolution videoStream = new VideoStreamWithResolution(stream);
            if (isAllowed(videoStream.resolution)) {
                switch (videoQuality) {
                    case BEST_QUALITY:
                        if (videoStream.isBetterQualityThan(best)) {
                            best = videoStream;
                        }
                        break;
                    case LEAST_BANDWITH:
                        if (videoStream.isLessNetworkUsageThan(best)) {
                            best = videoStream;
                        }
                        break;
                }
            }
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

    private static class VideoStreamWithResolution {
        final VideoStream videoStream;
        final VideoResolution resolution;

        VideoStreamWithResolution(VideoStream videoStream) {
            this.videoStream = videoStream;
            this.resolution = VideoResolution.resolutionToVideoResolution(videoStream.getResolution());
        }

        boolean isBetterQualityThan(VideoStreamWithResolution other) {
            return other == null || resolution.isBetterQualityThan(other.resolution);
        }

        boolean isLessNetworkUsageThan(VideoStreamWithResolution other) {
            return other == null || resolution.isLessNetworkUsageThan(other.resolution);
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
            return Uri.parse(videoStream.getUrl());
        }

        public Uri getAudioStreamUri() {
            return audioStream != null && audioStream.url != null ? Uri.parse(audioStream.getUrl()) : null;
        }

        public VideoResolution getResolution() {
            return resolution;
        }

        public AudioStream getAudioStream() {
            return audioStream;
        }
    }
}
