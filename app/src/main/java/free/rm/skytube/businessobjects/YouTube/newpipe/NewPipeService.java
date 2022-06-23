/*
 * SkyTube
 * Copyright (C) 2019  Zsombor Gegesy
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
package free.rm.skytube.businessobjects.YouTube.newpipe;

import android.util.Log;

import androidx.annotation.NonNull;

import com.github.skytube.components.httpclient.OkHttpDownloader;

import org.schabi.newpipe.extractor.ListExtractor;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.channel.ChannelExtractor;
import org.schabi.newpipe.extractor.comments.CommentsExtractor;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.exceptions.FoundAdException;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.feed.FeedExtractor;
import org.schabi.newpipe.extractor.kiosk.KioskExtractor;
import org.schabi.newpipe.extractor.kiosk.KioskList;
import org.schabi.newpipe.extractor.linkhandler.LinkHandler;
import org.schabi.newpipe.extractor.linkhandler.LinkHandlerFactory;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandlerFactory;
import org.schabi.newpipe.extractor.localization.ContentCountry;
import org.schabi.newpipe.extractor.localization.DateWrapper;
import org.schabi.newpipe.extractor.localization.Localization;
import org.schabi.newpipe.extractor.playlist.PlaylistExtractor;
import org.schabi.newpipe.extractor.search.SearchExtractor;
import org.schabi.newpipe.extractor.stream.StreamExtractor;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.subscription.SubscriptionExtractor;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.Logger;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;
import free.rm.skytube.businessobjects.YouTube.VideoStream.StreamMetaData;

/**
 * Service to interact with remote video services, using the NewPipeExtractor backend.
 */
public class NewPipeService {
    // TODO: remove this singleton
    private static NewPipeService instance;

    private final StreamingService streamingService;
    final static boolean DEBUG_LOG = false;

    public NewPipeService(StreamingService streamingService) {
        this.streamingService = streamingService;
    }

    /**
     * Returns a list of video/stream meta-data that is supported by this app.
     *
     * @return The {@link StreamInfo}.
     */
    public StreamInfo getStreamInfoByUrl(String videoUrl) throws IOException, ExtractionException {
        // actual extraction
        return StreamInfo.getInfo(streamingService, videoUrl);
    }

    public ContentId getVideoId(String url) throws ParsingException {
        if (url == null) {
            return null;
        }
        return parse(streamingService.getStreamLHFactory(), url, StreamingService.LinkType.STREAM);
    }

    public ContentId getContentId(String url) {
        if (url == null) {
            return null;
        }
        ContentId id;
        id = parse(streamingService.getStreamLHFactory(), url, StreamingService.LinkType.STREAM);
        if (id != null) {
            return id;
        }
        id = parse(streamingService.getChannelLHFactory(), url, StreamingService.LinkType.CHANNEL);
        if (id != null) {
            return id;
        }
        id = parse(streamingService.getPlaylistLHFactory(), url, StreamingService.LinkType.PLAYLIST);
        return id;
    }

    private ContentId parse(LinkHandlerFactory handlerFactory, String url, StreamingService.LinkType type) {
        if (handlerFactory != null) {
            try {
                String id = handlerFactory.getId(url);
                return new ContentId(id, handlerFactory.getUrl(id), type);
            } catch (ParsingException pe) {
                return null;
            }
        }
        return null;
    }

    /**
     * Returns a list of video/stream meta-data that is supported by this app for this video ID.
     *
     * @param videoId the id of the video.
     * @return List of {@link StreamMetaData}.
     */
    public StreamInfo getStreamInfoByVideoId(String videoId) throws ExtractionException, IOException {
        return getStreamInfoByUrl(getVideoUrl(videoId));
    }

    /**
     * Return the most recent videos for the given channel
     * @param channelId the id of the channel
     * @return list of recent {@link YouTubeVideo}.
     * @throws ExtractionException
     * @throws IOException
     */
    private List<YouTubeVideo> getChannelVideos(String channelId) throws NewPipeException {
        SkyTubeApp.nonUiThread();
        VideoPagerWithChannel pager = getChannelPager(channelId);
        List<YouTubeVideo> result = pager.getNextPageAsVideos();
        Logger.i(this, "getChannelVideos for %s(%s)  -> %s videos", pager.getChannel().getTitle(), channelId, result.size());
        return result;
    }

    /**
     * Return the most recent videos for the given channel from a dedicated feed (with a {@link FeedExtractor}).
     * @param channelId the id of the channel
     * @return list of recent {@link YouTubeVideo}, or null, if there is no feed.
     * @throws ExtractionException
     * @throws IOException
     */
    private List<YouTubeVideo> getFeedVideos(String channelId) throws ExtractionException, IOException, NewPipeException {
        SkyTubeApp.nonUiThread();
        final String url = getListLinkHandler(channelId).getUrl();
        final FeedExtractor feedExtractor = streamingService.getFeedExtractor(url);
        if (feedExtractor == null) {
            Logger.i(this, "getFeedExtractor doesn't return anything for %s -> %s", channelId, url);
            return null;
        }
        feedExtractor.fetchPage();
        return new VideoPagerWithChannel(streamingService, (ListExtractor)feedExtractor, createInternalChannelFromFeed(feedExtractor)).getNextPageAsVideos();
    }

    /**
     * Return the most recent videos for the given channel, either from a dedicated feed (with a {@link FeedExtractor} or from
     * the generic {@link ChannelExtractor}.
     * @param channelId the id of the channel
     * @return list of recent {@link YouTubeVideo}.
     * @throws ExtractionException
     * @throws IOException
     */
    public List<YouTubeVideo> getVideosFromFeedOrFromChannel(String channelId) throws NewPipeException {
        try {
            SkyTubeApp.nonUiThread();

            List<YouTubeVideo> videos = getFeedVideos(channelId);
            if (videos != null) {
                return videos;
            }
        } catch (IOException | ExtractionException | RuntimeException | NewPipeException e) {
            Logger.e(this, "Unable to get videos from a feed " + channelId + " : "+ e.getMessage(), e);
        }
        return getChannelVideos(channelId);
    }

    public VideoPager getTrending() throws NewPipeException {
        try {
            KioskList kiosks = streamingService.getKioskList();
            KioskExtractor kex = kiosks.getDefaultKioskExtractor();
            kex.fetchPage();
            return new VideoPager(streamingService, kex);
        } catch (ExtractionException | IOException e) {
            throw new NewPipeException("Unable to get 'trending' list:" + e.getMessage(), e);
        }
    }

    public VideoPagerWithChannel getChannelPager(String channelId) throws NewPipeException {
        try {
            ChannelExtractor channelExtractor = getChannelExtractor(channelId);

            YouTubeChannel channel = createInternalChannel(channelExtractor);
            return new VideoPagerWithChannel(streamingService, (ListExtractor) channelExtractor, channel);
        } catch (ExtractionException | IOException | RuntimeException e) {
            throw new NewPipeException("Getting videos for " + channelId + " fails:" + e.getMessage(), e);
        }
    }

    public PlaylistPager getPlaylistPager(String playlistId) throws NewPipeException {
        try {
            ListLinkHandler playlistLinkHandler = getPlaylistHandler(playlistId);
            PlaylistExtractor playlistExtractor = streamingService.getPlaylistExtractor(playlistLinkHandler);
            playlistExtractor.fetchPage();
            return new PlaylistPager(streamingService, playlistExtractor);
        } catch (ExtractionException | IOException | RuntimeException e) {
            throw new NewPipeException("Getting playlists for " + playlistId + " fails:" + e.getMessage(), e);
        }
    }

    public CommentPager getCommentPager(String videoId) throws NewPipeException {
        try {
            final ListLinkHandler linkHandler = streamingService.getCommentsLHFactory().fromId(videoId);
            final CommentsExtractor commentsExtractor = streamingService.getCommentsExtractor(linkHandler);
            return new CommentPager(streamingService, commentsExtractor);
        } catch (ExtractionException | RuntimeException e) {
            throw new NewPipeException("Getting comments for " + videoId + " fails:" + e.getMessage(), e);
        }
    }

    /**
     * Return detailed information for a channel from it's id.
     * @param channelId
     * @return the {@link YouTubeChannel}, with a list of recent videos.
     * @throws ExtractionException
     * @throws IOException
     */
    public YouTubeChannel getChannelDetails(String channelId) throws NewPipeException {
        VideoPagerWithChannel pager = getChannelPager(Objects.requireNonNull(channelId, "channelId"));
        // get the channel, and add all the videos from the first page
        YouTubeChannel channel = pager.getChannel();
        try {
            channel.getYouTubeVideos().addAll(pager.getNextPageAsVideos());
        } catch (NewPipeException e) {
            Logger.e(this, "Unable to retrieve videos for "+channelId+", error: "+e.getMessage(), e);
        }
        return channel;
    }

    private YouTubeChannel createInternalChannelFromFeed(FeedExtractor extractor) throws ParsingException {
        return new YouTubeChannel(extractor.getId(), extractor.getName(), null,
                null, null, -1, false, 0, System.currentTimeMillis(), null);
    }

    private YouTubeChannel createInternalChannel(ChannelExtractor extractor) throws ParsingException {
        return new YouTubeChannel(extractor.getId(), extractor.getName(), NewPipeUtils.filterHtml(extractor.getDescription()),
                extractor.getAvatarUrl(), extractor.getBannerUrl(), getSubscriberCount(extractor), false, 0, System.currentTimeMillis(), null);
    }

    /**
     * @param extractor
     * @return the subscriber count, or -1 if it's not available.
     */
    private long getSubscriberCount(ChannelExtractor extractor) {
        try {
            return extractor.getSubscriberCount();
        } catch (NullPointerException | ParsingException  npe) {
            Logger.e(this, "Unable to get subscriber count for " + extractor.getLinkHandler().getUrl() + " : "+ npe.getMessage(), npe);
            return -1L;
        }
    }

    private ChannelExtractor getChannelExtractor(String channelId)
            throws ParsingException, ExtractionException, IOException {
        // Extract from it
        ChannelExtractor channelExtractor = streamingService
                .getChannelExtractor(getListLinkHandler(Objects.requireNonNull(channelId, "channelId")));
        channelExtractor.fetchPage();
        return channelExtractor;
    }

    private ListLinkHandler getListLinkHandler(String channelId) throws ParsingException {
        // Get channel LinkHandler, handle three cases:
        // 1, channelId=UCbx1TZgxfIauUZyPuBzEwZg
        // 2, channelId=https://www.youtube.com/channel/UCbx1TZgxfIauUZyPuBzEwZg
        // 3, channelId=channel/UCbx1TZgxfIauUZyPuBzEwZg
        ListLinkHandlerFactory channelLHFactory = streamingService.getChannelLHFactory();
        try {
            return channelLHFactory.fromUrl(channelId);
        } catch (ParsingException p) {
            if (DEBUG_LOG) {
                Logger.d(this, "Unable to parse channel url=%s", channelId);
            }
        }
        if (channelId.startsWith("channel/") || channelId.startsWith("c/") || channelId.startsWith("user/")) {
            return channelLHFactory.fromId(channelId);
        }
        return channelLHFactory.fromId("channel/" + channelId);
    }

    private ListLinkHandler getPlaylistHandler(String playlistId) throws ParsingException {
        return streamingService.getPlaylistLHFactory().fromId(playlistId);
    }

    /**
     * Return detailed information about a video from it's id.
     * @param videoId the id of the video.
     * @return a {@link YouTubeVideo}
     * @throws ExtractionException
     * @throws IOException
     */
    public YouTubeVideo getDetails(String videoId) throws ExtractionException, IOException {
        SkyTubeApp.nonUiThread();
        LinkHandler url = streamingService.getStreamLHFactory().fromId(videoId);
        StreamExtractor extractor = streamingService.getStreamExtractor(url);
        extractor.fetchPage();

        DateInfo uploadDate = new DateInfo(extractor.getUploadDate());
        Logger.i(this, "getDetails for %s -> %s %s", videoId, url.getUrl(), uploadDate);

        long viewCount;
        try {
            viewCount = extractor.getViewCount();
        } catch (NumberFormatException|ParsingException e) {
            Logger.e(this, "Unable to get view count for " + url.getUrl()+", error: "+e.getMessage(), e);
            viewCount = 0;
        }

        YouTubeVideo video = new YouTubeVideo(extractor.getId(), extractor.getName(), NewPipeUtils.filterHtml(extractor.getDescription()),
                extractor.getLength(), new YouTubeChannel(extractor.getUploaderUrl(), extractor.getUploaderName()),
                viewCount, uploadDate.instant, uploadDate.exact, extractor.getThumbnailUrl());
        try {
            video.setLikeDislikeCount(extractor.getLikeCount(), extractor.getDislikeCount());
        } catch (ParsingException pe) {
            Logger.e(this, "Unable get like count for " + url.getUrl() + ", created at " + uploadDate + ", error:" + pe.getMessage(), pe);
            video.setLikeDislikeCount(null, null);
        }
        // Logger.i(this, " -> publishDate is %s, pretty: %s - orig value: %s", video.getPublishDate(),video.getPublishDatePretty(), uploadDate);
        return video;
    }

    static class DateInfo {
        boolean exact;
        Instant instant;

        public DateInfo(DateWrapper uploadDate) {
            if (uploadDate != null) {
                instant = uploadDate.offsetDateTime().toInstant();
                exact = !uploadDate.isApproximation();
            } else {
                instant = null;
                exact = false;
            }
        }

        private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        @NonNull
        @Override
        public String toString() {
            try {
                return "[time= " + dtf.format(instant) + ",exact=" + exact + ']';
            } catch (Exception e){
                return "[incorrect time= " + instant + " ,exact=" + exact + ']';
            }
        }
    }

    static String getThumbnailUrl(String id) {
        // Logger.d(NewPipeService.class, "getThumbnailUrl  %s", id);
        return "https://i.ytimg.com/vi/" + id + "/hqdefault.jpg";
    }


    public VideoPager getSearchResult(String query) throws NewPipeException {
        SkyTubeApp.nonUiThread();
        try {
            SearchExtractor extractor = streamingService.getSearchExtractor(query);
            extractor.fetchPage();
            return new VideoPager(streamingService, extractor);
        } catch (ExtractionException | IOException | RuntimeException e) {
            throw new NewPipeException("Getting search result for " + query + " fails:" + e.getMessage(), e);
        }
    }

    /**
     * Given video ID it will return the video's page URL.
     *
     * @param videoId       The ID of the video.
     * @throws ParsingException 
     */
    private String getVideoUrl(String videoId) throws ParsingException {
        return streamingService.getStreamLHFactory().getUrl(videoId);
    }

    public synchronized static NewPipeService get() {
        if (instance == null) {
            instance = new NewPipeService(ServiceList.YouTube);
            initNewPipe();
        }
        return instance;
    }

    public SubscriptionExtractor createSubscriptionExtractor() {
        return streamingService.getSubscriptionExtractor();
    }
    /**
     * Initialize NewPipe with a custom HttpDownloader.
     */
    public static void initNewPipe() {
        if (NewPipe.getDownloader() == null) {
            NewPipe.init(OkHttpDownloader.getInstance(), Localization.DEFAULT, toContentCountry(SkyTubeApp.getSettings().getPreferredContentCountry()));
        }
    }

    private static ContentCountry toContentCountry(String countryCode){
        if (countryCode == null || countryCode.isEmpty()) {
            return ContentCountry.DEFAULT;
        } else {
            return new ContentCountry(countryCode);
        }
    }

    public static void setCountry(String countryCodeStr) {
        initNewPipe();
        final ContentCountry contentCountry = toContentCountry(countryCodeStr);
        Log.i("NewPipeService", "set preferred content country to " + contentCountry);
        NewPipe.setPreferredContentCountry(contentCountry);
    }

    /**
     * @return true, if it's the preferred backend API
     */
    public static boolean isPreferred() {
        return SkyTubeApp.getPreferenceManager().getBoolean(SkyTubeApp.getStr(R.string.pref_use_default_newpipe_backend), true);
    }
}
