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

package free.rm.skytube.gui.businessobjects;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MergingMediaSource;
import com.google.android.exoplayer2.source.SingleSampleMediaSource;
import com.google.android.exoplayer2.source.dash.manifest.DashManifest;
import com.google.android.exoplayer2.source.dash.manifest.DashManifestParser;
import com.google.android.exoplayer2.source.smoothstreaming.manifest.SsManifest;
import com.google.android.exoplayer2.source.smoothstreaming.manifest.SsManifestParser;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;

import org.schabi.newpipe.extractor.MediaFormat;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.services.youtube.ItagItem;
import org.schabi.newpipe.extractor.services.youtube.dashmanifestcreators.CreationException;
import org.schabi.newpipe.extractor.services.youtube.dashmanifestcreators.YoutubeOtfDashManifestCreator;
import org.schabi.newpipe.extractor.services.youtube.dashmanifestcreators.YoutubePostLiveStreamDvrDashManifestCreator;
import org.schabi.newpipe.extractor.services.youtube.dashmanifestcreators.YoutubeProgressiveDashManifestCreator;
import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.DeliveryMethod;
import org.schabi.newpipe.extractor.stream.Stream;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.StreamType;
import org.schabi.newpipe.extractor.stream.SubtitlesStream;
import org.schabi.newpipe.extractor.stream.VideoStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import free.rm.skytube.app.StreamSelectionPolicy;
import free.rm.skytube.businessobjects.Logger;

public class DatasourceBuilder {
    private final ExoPlayer player;
    private final DefaultTrackSelector trackSelector;

    private final PlayerDataSource playerDataSource;
    private final SingleSampleMediaSource.Factory singleSampleSourceFactory;

    public DatasourceBuilder(Context context, ExoPlayer player, DefaultTrackSelector trackSelector) {
        this.player = player;
        this.trackSelector = Objects.requireNonNull(trackSelector, "trackSelector");
        DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter.Builder(context).build();
        playerDataSource = new PlayerDataSource(context, bandwidthMeter);
        singleSampleSourceFactory = playerDataSource.getSingleSampleMediaSourceFactory();
    }

    public TrackSelectionManager play(StreamSelectionPolicy.StreamSelection selection,
                                      @Nullable StreamInfo streamInfo) {
        Objects.requireNonNull(selection, "selection");
        final String cacheKey = streamInfo != null ? streamInfo.getId() : null;
        final List<MediaSource> sources = new ArrayList<>();
        final List<AudioStream> audioStreams;

        if (selection.isHls()) {
            sources.add(buildHlsMediaSource(selection.getHlsUrl(), streamInfo, cacheKey));
            audioStreams = addAudioSources(sources, null, streamInfo, cacheKey);
        } else {
            sources.add(buildMediaSource(selection.getVideoStream(), streamInfo, cacheKey));
            audioStreams = addAudioSources(sources, selection.getAudioStream(), streamInfo, cacheKey);
        }
        final List<SubtitlesStream> subtitles = addSubtitles(sources, streamInfo);
        preparePlayer(sources);
        return buildTrackSelectionManager(streamInfo, audioStreams, subtitles);
    }

    /**
     * Adds every playable audio stream as a separate media source and returns the streams that
     * were actually added. Those streams are the source of truth for the audio options exposed by
     * {@link TrackSelectionManager}.
     */
    private List<AudioStream> addAudioSources(List<MediaSource> sources, AudioStream selectedAudio,
                                              @Nullable StreamInfo streamInfo, @Nullable String cacheKey) {
        final Set<String> addedStreams = new LinkedHashSet<>();
        final List<AudioStream> result = new ArrayList<>();

        if (streamInfo != null && streamInfo.getAudioStreams() != null) {
            for (AudioStream audioStream : streamInfo.getAudioStreams()) {
                if (!isPlayable(audioStream) || !addedStreams.add(audioStreamKey(audioStream))) {
                    continue;
                }
                if (addAudioSource(sources, audioStream, streamInfo, cacheKey)) {
                    result.add(audioStream);
                }
            }
        }

        if (result.isEmpty() && isPlayable(selectedAudio)
                && addedStreams.add(audioStreamKey(selectedAudio))) {
            if (addAudioSource(sources, selectedAudio, streamInfo, cacheKey)) {
                result.add(selectedAudio);
            }
        }
        Logger.i(this, "Added %d playable audio sources (policy selected: %s)", result.size(),
                audioStreamKey(selectedAudio));
        return result;
    }

    private boolean addAudioSource(List<MediaSource> sources, AudioStream audioStream,
                                   @Nullable StreamInfo streamInfo, @Nullable String cacheKey) {
        try {
            sources.add(buildMediaSource(audioStream, streamInfo, cacheKey));
            return true;
        } catch (RuntimeException e) {
            Logger.w(this, "Could not add audio stream %s: %s", audioStreamKey(audioStream), e.getMessage());
            return false;
        }
    }

    private static boolean isPlayable(AudioStream audioStream) {
        return audioStream != null && audioStream.isUrl()
                && audioStream.getContent() != null && !audioStream.getContent().isEmpty();
    }

    private static String audioStreamKey(AudioStream audioStream) {
        if (audioStream == null) {
            return "null";
        }
        return audioStream.getContent() + "|" + audioStream.getFormatId();
    }

    public TrackSelectionManager play(Uri videoUri, @Nullable Uri audioUri, @Nullable StreamInfo streamInfo) {
        Objects.requireNonNull(videoUri, "videoUri is required");
        final List<MediaSource> sources = new ArrayList<>();
        sources.add(createLocalSource(videoUri));
        if (audioUri != null) {
            sources.add(createLocalSource(audioUri));
        }
        final List<SubtitlesStream> subtitles = addSubtitles(sources, streamInfo);
        preparePlayer(sources);
        // Local files never expose alternative audio tracks, so the manager is intentionally
        // bare-bone on the audio side even when the caller supplies subtitle metadata.
        return new TrackSelectionManager(trackSelector, Collections.emptyList(),
                buildSubtitleTracks(subtitles));
    }

    private MediaSource createLocalSource(Uri uri) {
        return playerDataSource.getCachelessProgressiveMediaSourceFactory()
                .createMediaSource(new MediaItem.Builder().setUri(uri).build());
    }

    /**
     * Builds a media source for an HLS manifest URL. YouTube manifests need the
     * YouTube specific HTTP data source, other services are played with the generic one.
     */
    private MediaSource buildHlsMediaSource(String hlsUrl, @Nullable StreamInfo streamInfo,
                                            @Nullable String cacheKey) {
        if (hlsUrl == null || hlsUrl.isEmpty()) {
            throw new IllegalArgumentException("HLS manifest URL is missing");
        }
        final MediaItem item = mediaItem(hlsUrl, cacheKey);
        if (streamInfo != null && streamInfo.getServiceId() == ServiceList.YouTube.getServiceId()) {
            return playerDataSource.getYoutubeHlsMediaSourceFactory().createMediaSource(item);
        }
        return playerDataSource.getHlsMediaSourceFactory(null).createMediaSource(item);
    }

    private MediaSource buildMediaSource(Stream stream, @Nullable StreamInfo streamInfo,
                                         @Nullable String cacheKey) {
        Objects.requireNonNull(stream, "stream");
        final String streamCacheKey = cacheKey != null
                ? cacheKey + " " + stream.getFormatId() + " " + stream.getId()
                : null;
        if (streamInfo != null && streamInfo.getServiceId() == ServiceList.YouTube.getServiceId()) {
            return buildYoutubeMediaSource(stream, streamInfo, streamCacheKey);
        }
        return buildGenericMediaSource(stream, streamCacheKey);
    }

    private MediaSource buildGenericMediaSource(Stream stream, @Nullable String cacheKey) {
        final DeliveryMethod deliveryMethod = stream.getDeliveryMethod();
        switch (deliveryMethod) {
            case PROGRESSIVE_HTTP:
                checkUrl(stream);
                return playerDataSource.getProgressiveMediaSourceFactory()
                        .createMediaSource(mediaItem(stream.getContent(), cacheKey));
            case DASH:
                if (stream.isUrl()) {
                    checkUrl(stream);
                    return playerDataSource.getDashMediaSourceFactory()
                            .createMediaSource(mediaItem(stream.getContent(), cacheKey));
                }
                try {
                    final DashManifest manifest = new DashManifestParser().parse(
                            manifestUri(stream),
                            new ByteArrayInputStream(stream.getContent().getBytes(StandardCharsets.UTF_8)));
                    return playerDataSource.getDashMediaSourceFactory()
                            .createMediaSource(manifest, mediaItem(stream.getManifestUrl(), cacheKey));
                } catch (IOException e) {
                    throw new IllegalArgumentException("Could not parse DASH manifest", e);
                }
            case HLS:
                if (stream.isUrl()) {
                    checkUrl(stream);
                    return playerDataSource.getHlsMediaSourceFactory(null)
                            .createMediaSource(mediaItem(stream.getContent(), cacheKey));
                }
                final NonUriHlsDataSourceFactory.Builder hlsBuilder =
                        new NonUriHlsDataSourceFactory.Builder();
                hlsBuilder.setPlaylistString(stream.getContent());
                return playerDataSource.getHlsMediaSourceFactory(hlsBuilder)
                        .createMediaSource(mediaItem(stream.getManifestUrl(), cacheKey));
            case SS:
                if (stream.isUrl()) {
                    checkUrl(stream);
                    return playerDataSource.getSSMediaSourceFactory()
                            .createMediaSource(mediaItem(stream.getContent(), cacheKey));
                }
                try {
                    final SsManifest manifest = new SsManifestParser().parse(
                            manifestUri(stream),
                            new ByteArrayInputStream(stream.getContent().getBytes(StandardCharsets.UTF_8)));
                    return playerDataSource.getSSMediaSourceFactory()
                            .createMediaSource(manifest, mediaItem(stream.getManifestUrl(), cacheKey));
                } catch (IOException e) {
                    throw new IllegalArgumentException("Could not parse SmoothStreaming manifest", e);
                }
            default:
                throw new IllegalArgumentException("Unsupported delivery method: " + deliveryMethod);
        }
    }

    private MediaSource buildYoutubeMediaSource(Stream stream, StreamInfo streamInfo,
                                                @Nullable String cacheKey) {
        if (!(stream instanceof AudioStream || stream instanceof VideoStream)) {
            throw new IllegalArgumentException("Unsupported YouTube stream type: "
                    + stream.getClass().getSimpleName());
        }

        if (streamInfo.getStreamType() == StreamType.POST_LIVE_STREAM) {
            try {
                final ItagItem itagItem = Objects.requireNonNull(stream.getItagItem());
                final String manifest = YoutubePostLiveStreamDvrDashManifestCreator
                        .fromPostLiveStreamDvrStreamingUrl(stream.getContent(), itagItem,
                                itagItem.getTargetDurationSec(), streamInfo.getDuration());
                return buildYoutubeDashMediaSource(manifest, stream, cacheKey);
            } catch (CreationException | IOException | NullPointerException e) {
                throw new IllegalArgumentException(
                        "Could not create DASH manifest for YouTube post-live stream", e);
            }
        }

        final DeliveryMethod deliveryMethod = stream.getDeliveryMethod();
        switch (deliveryMethod) {
            case PROGRESSIVE_HTTP:
                if ((stream instanceof VideoStream && ((VideoStream) stream).isVideoOnly())
                        || stream instanceof AudioStream) {
                    try {
                        final String manifest = YoutubeProgressiveDashManifestCreator
                                .fromProgressiveStreamingUrl(stream.getContent(),
                                        Objects.requireNonNull(stream.getItagItem()),
                                        streamInfo.getDuration());
                        return buildYoutubeDashMediaSource(manifest, stream, cacheKey);
                    } catch (CreationException | IOException | NullPointerException e) {
                        Logger.w(this, "Could not create DASH manifest for YouTube progressive "
                                + "stream, falling back to progressive: %s", e.getMessage());
                        return playerDataSource.getYoutubeProgressiveMediaSourceFactory()
                                .createMediaSource(mediaItem(stream.getContent(), cacheKey));
                    }
                }
                return playerDataSource.getYoutubeProgressiveMediaSourceFactory()
                        .createMediaSource(mediaItem(stream.getContent(), cacheKey));
            case DASH:
                try {
                    final String manifest = YoutubeOtfDashManifestCreator.fromOtfStreamingUrl(
                            stream.getContent(), Objects.requireNonNull(stream.getItagItem()),
                            streamInfo.getDuration());
                    return buildYoutubeDashMediaSource(manifest, stream, cacheKey);
                } catch (CreationException | IOException | NullPointerException e) {
                    throw new IllegalArgumentException(
                            "Could not create DASH manifest for YouTube OTF stream", e);
                }
            case HLS:
                return playerDataSource.getYoutubeHlsMediaSourceFactory()
                        .createMediaSource(mediaItem(stream.getContent(), cacheKey));
            default:
                throw new IllegalArgumentException("Unsupported YouTube delivery method: "
                        + deliveryMethod);
        }
    }

    private MediaSource buildYoutubeDashMediaSource(String manifestContent, Stream stream,
                                                    @Nullable String cacheKey) throws IOException {
        final DashManifest manifest = new DashManifestParser().parse(
                manifestUri(stream),
                new ByteArrayInputStream(manifestContent.getBytes(StandardCharsets.UTF_8)));
        return playerDataSource.getYoutubeDashMediaSourceFactory()
                .createMediaSource(manifest, mediaItem(stream.getContent(), cacheKey));
    }

    private List<SubtitlesStream> addSubtitles(List<MediaSource> sources,
                                               @Nullable StreamInfo streamInfo) {
        final List<SubtitlesStream> added = new ArrayList<>();
        if (streamInfo == null || streamInfo.getSubtitles() == null) {
            return added;
        }
        for (SubtitlesStream subtitle : streamInfo.getSubtitles()) {
            if (!subtitle.isUrl() || subtitle.getLocale() == null) {
                continue;
            }
            final MediaFormat format = subtitle.getFormat();
            if (format == null) {
                continue;
            }
            final int roleFlags = subtitle.isAutoGenerated()
                    ? C.ROLE_FLAG_DESCRIBES_MUSIC_AND_SOUND
                    : C.ROLE_FLAG_CAPTION;
            final MediaItem.SubtitleConfiguration configuration =
                    new MediaItem.SubtitleConfiguration.Builder(Uri.parse(subtitle.getContent()))
                            .setMimeType(format.getMimeType())
                            .setRoleFlags(roleFlags)
                            .setLanguage(subtitle.getLocale().getLanguage())
                            .build();
            sources.add(singleSampleSourceFactory.createMediaSource(configuration, C.TIME_UNSET));
            added.add(subtitle);
        }
        return added;
    }

    /**
     * Builds a manager entirely from the metadata of the streams that were just prepared, instead
     * of making the manager discover tracks from the player after the fact.
     */
    private TrackSelectionManager buildTrackSelectionManager(@Nullable StreamInfo streamInfo,
                                                             List<AudioStream> audioStreams,
                                                             List<SubtitlesStream> subtitles) {
        return new TrackSelectionManager(trackSelector, buildAudioTracks(streamInfo, audioStreams),
                buildSubtitleTracks(subtitles));
    }

    private List<TrackSelectionManager.AudioTrackInfo> buildAudioTracks(@Nullable StreamInfo streamInfo,
                                                  List<AudioStream> audioStreams) {
        if (audioStreams.isEmpty()) {
            return Collections.emptyList();
        }
        final Set<String> translatedLanguages =
                TrackSelectionManager.findAutoTranslatedAudioLanguages(streamInfo);
        final List<TrackSelectionManager.AudioTrackInfo> tracks = new ArrayList<>();
        for (AudioStream audioStream : audioStreams) {
            final String language = TrackSelectionManager.normalizeLanguage(audioLanguage(audioStream));
            final boolean translated = translatedLanguages.contains(language);
            final int bitrate = audioStream.getAverageBitrate() > 0
                    ? audioStream.getAverageBitrate() : TrackSelectionManager.NO_BITRATE;
            tracks.add(new TrackSelectionManager.AudioTrackInfo(language,
                    TrackSelectionManager.getLanguageDisplayName(language, translated),
                    bitrate, translated));
        }
        return tracks;
    }

    private static String audioLanguage(AudioStream audioStream) {
        if (audioStream.getAudioLocale() != null) {
            return audioStream.getAudioLocale().getLanguage();
        }
        final ItagItem itagItem = audioStream.getItagItem();
        return itagItem != null && itagItem.getAudioLocale() != null
                ? itagItem.getAudioLocale().getLanguage() : null;
    }

    private List<TrackSelectionManager.SubtitleTrackInfo> buildSubtitleTracks(List<SubtitlesStream> subtitles) {
        if (subtitles.isEmpty()) {
            return Collections.emptyList();
        }
        final List<TrackSelectionManager.SubtitleTrackInfo> tracks = new ArrayList<>();
        for (SubtitlesStream subtitle : subtitles) {
            final String language =
                    TrackSelectionManager.normalizeLanguage(subtitle.getLocale().getLanguage());
            tracks.add(new TrackSelectionManager.SubtitleTrackInfo(language,
                    TrackSelectionManager.getLanguageDisplayName(language)));
        }
        return tracks;
    }

    private MediaItem mediaItem(@Nullable String url, @Nullable String cacheKey) {
        final MediaItem.Builder builder = new MediaItem.Builder()
                .setUri(Uri.parse(url != null ? url : ""));
        if (cacheKey != null) {
            builder.setCustomCacheKey(cacheKey);
        }
        return builder.build();
    }

    private static Uri manifestUri(Stream stream) {
        final String manifestUrl = stream.getManifestUrl();
        return Uri.parse(manifestUrl != null ? manifestUrl : "");
    }

    private static void checkUrl(Stream stream) {
        final String url = stream.getContent();
        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("Stream URL is missing");
        }
    }

    private void preparePlayer(List<MediaSource> sources) {
        Objects.requireNonNull(sources, "sources");
        Logger.i(this, "Prepare player with sources: %s - raw: %s", sources.size(), sources);
        if (sources.isEmpty()) {
            return;
        }
        if (sources.size() == 1) {
            player.prepare(sources.get(0));
        } else {
            player.prepare(new MergingMediaSource(true, sources.toArray(new MediaSource[0])));
        }
    }
}
