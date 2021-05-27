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

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MergingMediaSource;
import com.google.android.exoplayer2.source.SingleSampleMediaSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultLoadErrorHandlingPolicy;

import org.schabi.newpipe.extractor.MediaFormat;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.SubtitlesStream;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import free.rm.skytube.businessobjects.Logger;

public class DatasourceBuilder {
    private final Context context;
    private final ExoPlayer player;

    private final DefaultDataSourceFactory dataSourceFactory;
    private final SingleSampleMediaSource.Factory singleSampleSourceFactory;
    private final ExtractorMediaSource.Factory extMediaSourceFactory;

    private final static int MINIMUM_LOADABLE_RETRY_COUNT = 10;

    public DatasourceBuilder(Context context, ExoPlayer player) {
        this.context = context;
        this.player = player;
        dataSourceFactory =  new DefaultDataSourceFactory(context, "ST. Agent", new DefaultBandwidthMeter());
        singleSampleSourceFactory = new SingleSampleMediaSource.Factory(dataSourceFactory);

        extMediaSourceFactory = new ExtractorMediaSource.Factory(dataSourceFactory).setLoadErrorHandlingPolicy(
                new DefaultLoadErrorHandlingPolicy(MINIMUM_LOADABLE_RETRY_COUNT));
    }

    public void play(Uri videoUri, Uri audioUri) {
        preparePlayer(createSources(videoUri, audioUri, null));
    }

    public void play(Uri videoUri, Uri audioUri, StreamInfo streamInfo) {
        final List<MediaSource> titles;
        if (streamInfo != null && streamInfo.getSubtitles() != null) {
            titles = streamInfo.getSubtitles().stream().map( this::convert).collect(Collectors.toList());
        } else {
            titles = null;
        }
        List<MediaSource> sources = createSources(videoUri, audioUri, titles);
        preparePlayer(sources);
    }

    private MediaSource convert(SubtitlesStream subtitlesStream) {
        MediaFormat format = subtitlesStream.getFormat();
        String language = subtitlesStream.getLocale().getLanguage();
        Logger.i(this, "convert %s -> %s %s", subtitlesStream.getUrl(), format, language);
        Format exoFormat = Format.createTextSampleFormat(null, format.getMimeType(), C.SELECTION_FLAG_AUTOSELECT, language);
        return singleSampleSourceFactory.createMediaSource(
                Uri.parse(subtitlesStream.getUrl()), exoFormat,
                C.TIME_UNSET);
    }

    private List<MediaSource> createSources(Uri videoUri, Uri audioUri, List<MediaSource> subtitles) {
        Objects.requireNonNull(videoUri, "videoUri is required");
        Logger.i(this, "Create datasources for video=%s \n\taudio= %s and %s subtitles", videoUri, audioUri, subtitles);
        List<MediaSource> sources = new ArrayList<MediaSource>();

        sources.add(createSource(videoUri));
        if (audioUri != null) {
            sources.add(createSource(audioUri));
        }
        if (subtitles != null) {
            sources.addAll(subtitles);
        }
        return sources;
    }

    private ExtractorMediaSource createSource(Uri uri) {
        return extMediaSourceFactory.createMediaSource(uri);
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
            MergingMediaSource merged = new MergingMediaSource(sources.toArray(new MediaSource[sources.size()]));
            player.prepare(merged);
        }
    }
}
