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

import androidx.annotation.NonNull;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document.OutputSettings;
import org.jsoup.safety.Whitelist;
import org.schabi.newpipe.extractor.ListExtractor;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.channel.ChannelExtractor;
import org.schabi.newpipe.extractor.comments.CommentsExtractor;
import org.schabi.newpipe.extractor.exceptions.ContentNotAvailableException;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.exceptions.FoundAdException;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.feed.FeedExtractor;
import org.schabi.newpipe.extractor.linkhandler.LinkHandler;
import org.schabi.newpipe.extractor.linkhandler.LinkHandlerFactory;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandlerFactory;
import org.schabi.newpipe.extractor.localization.DateWrapper;
import org.schabi.newpipe.extractor.localization.Localization;
import org.schabi.newpipe.extractor.playlist.PlaylistExtractor;
import org.schabi.newpipe.extractor.search.SearchExtractor;
import org.schabi.newpipe.extractor.stream.Description;
import org.schabi.newpipe.extractor.stream.StreamExtractor;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.VideoStream;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.app.Utils;
import free.rm.skytube.businessobjects.Logger;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;
import free.rm.skytube.businessobjects.YouTube.VideoStream.HttpDownloader;
import free.rm.skytube.businessobjects.YouTube.VideoStream.StreamMetaData;
import free.rm.skytube.businessobjects.YouTube.VideoStream.StreamMetaDataList;

/**
 * Service to interact with remote video services, using the NewPipeExtractor backend.
 */
public class NewPipeService {
    // TODO: remove this singleton
    private static NewPipeService instance;

    private final StreamingService streamingService;
    private final static boolean DEBUG_LOG = false;

    public NewPipeService(StreamingService streamingService) {
        this.streamingService = streamingService;
    }

    /**
     * Returns a list of video/stream meta-data that is supported by this app.
     *
     * @return List of {@link StreamMetaData}.
     */
    public StreamMetaDataList getStreamMetaDataListByUrl(String videoUrl) {
        StreamMetaDataList list = new StreamMetaDataList();

        try {

            // actual extraction
            StreamInfo streamInfo = StreamInfo.getInfo(streamingService, videoUrl);

            // now print the stream url and we are done
            for(VideoStream stream : streamInfo.getVideoStreams()) {
                list.add( new StreamMetaData(stream) );
            }
        } catch (ContentNotAvailableException exception) {
            list = new StreamMetaDataList(exception.getMessage());
        } catch (Throwable tr) {
            Logger.e(this, "An error has occurred while getting streams metadata.  URL=" + videoUrl, tr);
            list = new StreamMetaDataList(R.string.error_video_streams);
        }

        return list;
    }

    public ContentId getVideoId(String url) throws ParsingException {
        if (url == null) {
            return null;
        }
        return parse(streamingService.getStreamLHFactory(), url, StreamingService.LinkType.STREAM);
    }

    public ContentId getContentId(String url) throws FoundAdException {
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

    private ContentId parse(LinkHandlerFactory handlerFactory, String url, StreamingService.LinkType type) throws FoundAdException {
        if (handlerFactory != null) {
            try {
                String id = handlerFactory.getId(url);
                return new ContentId(id, handlerFactory.getUrl(id), type);
            } catch (FoundAdException fa) {
                throw fa;
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
    public StreamMetaDataList getStreamMetaDataList(String videoId) {
        try {
            return getStreamMetaDataListByUrl(getVideoUrl(videoId));
        } catch (ParsingException e) {
            return new StreamMetaDataList(e.getMessage());
        }
    }

    /**
     * Return the most recent videos for the given channel
     * @param channelId the id of the channel
     * @return list of recent {@link YouTubeVideo}.
     * @throws ExtractionException
     * @throws IOException
     */
    private List<YouTubeVideo> getChannelVideos(String channelId) throws NewPipeException {
        VideoPager pager = getChannelPager(channelId);
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
        final String url = getListLinkHandler(channelId).getUrl();
        final FeedExtractor feedExtractor = streamingService.getFeedExtractor(url);
        if (feedExtractor == null) {
            Logger.i(this, "getFeedExtractor doesn't return anything for %s -> %s", channelId, url);
            return null;
        }
        feedExtractor.fetchPage();
        return new VideoPager(streamingService, (ListExtractor)feedExtractor, createInternalChannelFromFeed(feedExtractor)).getNextPageAsVideos();
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
            List<YouTubeVideo> videos = getFeedVideos(channelId);
            if (videos != null) {
                return videos;
            }
        } catch (IOException | ExtractionException | RuntimeException | NewPipeException e) {
            Logger.e(this, "Unable to get videos from a feed " + channelId + " : "+ e.getMessage(), e);
        }
        return getChannelVideos(channelId);
    }

    public VideoPager getChannelPager(String channelId) throws NewPipeException {
        try {
            ChannelExtractor channelExtractor = getChannelExtractor(channelId);

            YouTubeChannel channel = createInternalChannel(channelExtractor);
            return new VideoPager(streamingService, (ListExtractor) channelExtractor, channel);
        } catch (ExtractionException | IOException | RuntimeException e) {
            throw new NewPipeException("Getting videos for " + channelId + " fails:" + e.getMessage(), e);
        }
    }

    public PlaylistPager getPlaylistPager(String channelId) throws NewPipeException {
        try {
            ListLinkHandler channelList = getListLinkHandler(channelId);
            PlaylistExtractor playlistExtractor = streamingService.getPlaylistExtractor(channelList);
            playlistExtractor.fetchPage();
            return new PlaylistPager(streamingService, playlistExtractor);
        } catch (ExtractionException | IOException | RuntimeException e) {
            throw new NewPipeException("Getting playlists for " + channelId + " fails:" + e.getMessage(), e);
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
        Utils.requireNonNull(channelId, "channelId");
        VideoPager pager = getChannelPager(channelId);
        // get the channel, and add all the videos from the first page
        pager.getChannel().getYouTubeVideos().addAll(pager.getNextPageAsVideos());
        return pager.getChannel();
    }

    private YouTubeChannel createInternalChannelFromFeed(FeedExtractor extractor) throws ParsingException {
        return new YouTubeChannel(extractor.getId(), extractor.getName(), null,
                null, null, -1, false, 0, System.currentTimeMillis());
    }

    private YouTubeChannel createInternalChannel(ChannelExtractor extractor) throws ParsingException {
        return new YouTubeChannel(extractor.getId(), extractor.getName(), filterHtml(extractor.getDescription()),
                extractor.getAvatarUrl(), extractor.getBannerUrl(), getSubscriberCount(extractor), false, 0, System.currentTimeMillis());
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
        Utils.requireNonNull(channelId, "channelId");
        // Extract from it
        ChannelExtractor channelExtractor = streamingService.getChannelExtractor(getListLinkHandler(channelId));
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

    /**
     * Return detailed information about a video from it's id.
     * @param videoId the id of the video.
     * @return a {@link YouTubeVideo}
     * @throws ExtractionException
     * @throws IOException
     */
    public YouTubeVideo getDetails(String videoId) throws ExtractionException, IOException {
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

        YouTubeVideo video = new YouTubeVideo(extractor.getId(), extractor.getName(), filterHtml(extractor.getDescription()),
                extractor.getLength(), new YouTubeChannel(extractor.getUploaderUrl(), extractor.getUploaderName()),
                viewCount, uploadDate.timestamp, uploadDate.exact, extractor.getThumbnailUrl());
        try {
            video.setLikeDislikeCount(extractor.getLikeCount(), extractor.getDislikeCount());
        } catch (ParsingException pe) {
            Logger.e(this, "Unable get like count for " + url.getUrl() + ", created at " + uploadDate + ", error:" + pe.getMessage(), pe);
            video.setLikeDislikeCount(null, null);
        }
        video.setRetrievalTimestamp(System.currentTimeMillis());
        // Logger.i(this, " -> publishDate is %s, pretty: %s - orig value: %s", video.getPublishDate(),video.getPublishDatePretty(), uploadDate);
        return video;
    }

    static class DateInfo {
        boolean exact;
        Long timestamp;

        public DateInfo(DateWrapper uploadDate) {
            if (uploadDate != null) {
                timestamp = uploadDate.date().getTimeInMillis();
                exact = !uploadDate.isApproximation();
            } else {
                timestamp = System.currentTimeMillis();
                exact = false;
            }

        }

        static final SimpleDateFormat sdf= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        @NonNull
        @Override
        public String toString() {
            try {
                return "[time= " + sdf.format(new Date(timestamp)) + ",exact=" + exact + ']';
            } catch (Exception e){
                return "[incorrect time= "+timestamp+" ,exact=" + exact + ']';
            }
        }
    }

    static String getThumbnailUrl(String id) {
        // Logger.d(NewPipeService.class, "getThumbnailUrl  %s", id);
        return "https://i.ytimg.com/vi/" + id + "/hqdefault.jpg";
    }

    private String filterHtml(String content) {
        return Jsoup.clean(content, "", Whitelist.basic(), new OutputSettings().prettyPrint(false));
    }

    private String filterHtml(Description description) {
        String result;
        if (description.getType() == Description.HTML) {
            result = filterHtml(description.getContent());
        } else {
            result = description.getContent();
        }
        if (DEBUG_LOG) {
            Logger.d(this, "filterHtml %s -> %s", description, result);
        }
        return result;
    }

    public VideoPager getSearchResult(String query) throws NewPipeException {
        try {
            SearchExtractor extractor = streamingService.getSearchExtractor(query);
            extractor.fetchPage();
            return new VideoPager(streamingService, extractor, null);
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

    /**
     * Initialize NewPipe with a custom HttpDownloader.
     */
    public static void initNewPipe() {
        if (NewPipe.getDownloader() == null) {
            NewPipe.init(new HttpDownloader(), new Localization("GB", "en"));
        }
    }

    /**
     * @return true, if it's the preferred backend API
     */
    public static boolean isPreferred() {
        return SkyTubeApp.getPreferenceManager().getBoolean(SkyTubeApp.getStr(R.string.pref_use_default_newpipe_backend), true);
    }
}
