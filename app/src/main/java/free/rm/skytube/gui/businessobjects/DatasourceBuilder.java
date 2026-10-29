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
import java.util.List;
import java.util.Objects;

import free.rm.skytube.app.StreamSelectionPolicy;
import free.rm.skytube.businessobjects.Logger;

public class DatasourceBuilder {
    private final ExoPlayer player;

    private final PlayerDataSource playerDataSource;
    private final SingleSampleMediaSource.Factory singleSampleSourceFactory;

    public DatasourceBuilder(Context context, ExoPlayer player) {
        this.player = player;
        DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter.Builder(context).build();
        playerDataSource = new PlayerDataSource(context, bandwidthMeter);
        singleSampleSourceFactory = playerDataSource.getSingleSampleMediaSourceFactory();
    }

    public void play(StreamSelectionPolicy.StreamSelection selection, @Nullable StreamInfo streamInfo) {
        Objects.requireNonNull(selection, "selection");
        final String cacheKey = streamInfo != null ? streamInfo.getId() : null;
        final List<MediaSource> sources = new ArrayList<>();

        if (selection.isHls()) {
            sources.add(buildHlsMediaSource(selection.getHlsUrl(), streamInfo, cacheKey));
        } else {
            sources.add(buildMediaSource(selection.getVideoStream(), streamInfo, cacheKey));
            if (selection.getAudioStream() != null) {
                sources.add(buildMediaSource(selection.getAudioStream(), streamInfo, cacheKey));
            }
        }
        if (streamInfo != null) {
            addSubtitles(sources, streamInfo);
        }
        preparePlayer(sources);
    }

    public void play(Uri videoUri, @Nullable Uri audioUri, @Nullable StreamInfo streamInfo) {
        Objects.requireNonNull(videoUri, "videoUri is required");
        final List<MediaSource> sources = new ArrayList<>();
        sources.add(createLocalSource(videoUri));
        if (audioUri != null) {
            sources.add(createLocalSource(audioUri));
        }
        if (streamInfo != null) {
            addSubtitles(sources, streamInfo);
        }
        preparePlayer(sources);
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

    private void addSubtitles(List<MediaSource> sources, StreamInfo streamInfo) {
        final List<SubtitlesStream> subtitles = streamInfo.getSubtitles();
        if (subtitles == null) {
            return;
        }
        for (SubtitlesStream subtitle : subtitles) {
            if (!subtitle.isUrl()) {
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
        }
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
