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
import free.rm.skytube.businessobjects.YouTube.VideoStream.VideoResolution;

public class StreamSelectionPolicy {
    private final boolean allowVideoOnly;
    private final VideoResolution maxResolution;
    private final VideoResolution minResolution;

    public StreamSelectionPolicy(boolean allowVideoOnly, VideoResolution maxResolution, VideoResolution minResolution) {
        this.allowVideoOnly = allowVideoOnly;
        this.maxResolution = maxResolution;
        this.minResolution = minResolution;
    }

    public StreamSelectionPolicy withAllowVideoOnly(boolean newValue) {
        return new StreamSelectionPolicy(newValue, maxResolution, minResolution);
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
            if (best == null || audioStream.average_bitrate < best.average_bitrate) {
                best = audioStream;
            }
        }
        return best;
    }

    private VideoStreamWithResolution pickVideo(StreamInfo streamInfo) {
        VideoStreamWithResolution videoStream = pick(streamInfo.getVideoStreams());
        if (allowVideoOnly) {
            VideoStreamWithResolution videoOnlyStream = pick(streamInfo.getVideoOnlyStreams());
            if (videoOnlyStream != null && videoOnlyStream.isBiggerThan(videoStream)) {
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
                if (videoStream.isBiggerThan(best)) {
                    best = videoStream;
                }
            }
        }
        return best;
    }

    private boolean isAllowed(VideoResolution resolution) {
        if (minResolution != null && minResolution.isBiggerThan(resolution)) {
            return false;
        }

        if (maxResolution != null && resolution.isBiggerThan(maxResolution)) {
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

        boolean isBiggerThan(VideoStreamWithResolution other) {
            return other == null || resolution.isBiggerThan(other.resolution);
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
