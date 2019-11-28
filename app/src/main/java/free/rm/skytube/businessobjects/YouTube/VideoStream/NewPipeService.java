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
package free.rm.skytube.businessobjects.YouTube.VideoStream;

import java.io.IOException;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document.OutputSettings;
import org.jsoup.safety.Whitelist;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.channel.ChannelExtractor;
import org.schabi.newpipe.extractor.exceptions.ContentNotAvailableException;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.LinkHandler;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandlerFactory;
import org.schabi.newpipe.extractor.localization.DateWrapper;
import org.schabi.newpipe.extractor.search.SearchExtractor;
import org.schabi.newpipe.extractor.stream.StreamExtractor;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.extractor.stream.VideoStream;
import org.schabi.newpipe.extractor.localization.Localization;

import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.Logger;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;

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
     * Return the post recent videos for the given channel
     * @param channelId the id of the channel
     * @return list of recent {@link YouTubeVideo}.
     * @throws ExtractionException
     * @throws IOException
     */
    public List<YouTubeVideo> getChannelVideos(String channelId) throws ExtractionException, IOException {
        Pager<StreamInfoItem> pager = getChannelPager(channelId);
        List<YouTubeVideo> result = pager.getVideos();
        Logger.i(this, "getChannelVideos for %s(%s)  -> %s videos", pager.getChannel().getTitle(), channelId, result.size());
        return result;
    }

    public Pager<StreamInfoItem> getChannelPager(String channelId) throws ExtractionException, IOException {
        ChannelExtractor channelExtractor = getChannelExtractor(channelId);

        YouTubeChannel channel = createInternalChannel(channelExtractor);
        return new Pager<>(streamingService, channelExtractor, channel);
    }

    /**
     * Return detailed information for a channel from it's id.
     * @param channelId
     * @return the {@link YouTubeChannel}, with a list of recent videos.
     * @throws ExtractionException
     * @throws IOException
     */
    public YouTubeChannel getChannelDetails(String channelId) throws ExtractionException, IOException {
        requireNonNull(channelId, "channelId");
        Pager<StreamInfoItem> pager = getChannelPager(channelId);
        // get the channel, and add all the videos from the first page
        pager.getChannel().getYouTubeVideos().addAll(pager.getVideos());
        return pager.getChannel();
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
        requireNonNull(channelId, "channelId");
        // Extract from it
        ChannelExtractor channelExtractor = streamingService.getChannelExtractor(getListLinkHandler(channelId));
        channelExtractor.fetchPage();
        return channelExtractor;
    }

    private ListLinkHandler getListLinkHandler(String channelId) throws ParsingException {
        // Get channel LinkHandler, handle two cases:
        // 1, channelId=UCbx1TZgxfIauUZyPuBzEwZg
        // 2, channelId=https://www.youtube.com/channel/UCbx1TZgxfIauUZyPuBzEwZg
        ListLinkHandlerFactory channelLHFactory = streamingService.getChannelLHFactory();
        try {
            return channelLHFactory.fromUrl(channelId);
        } catch (ParsingException p) {
            if (DEBUG_LOG) {
                Logger.d(this, "Unable to parse channel url=%s", channelId);
            }
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

        DateWrapper uploadDate = extractor.getUploadDate();

        YouTubeVideo video = new YouTubeVideo(extractor.getId(), extractor.getName(), filterHtml(extractor.getDescription()),
                extractor.getLength(), new YouTubeChannel(extractor.getUploaderUrl(), extractor.getUploaderName()),
                extractor.getViewCount(), getPublishDate(uploadDate), extractor.getThumbnailUrl());
        video.setLikeDislikeCount(extractor.getLikeCount(), extractor.getDislikeCount());
        video.setRetrievalTimestamp(System.currentTimeMillis());
        // Logger.i(this, " -> publishDate is %s, pretty: %s - orig value: %s", video.getPublishDate(),video.getPublishDatePretty(), uploadDate);
        return video;
    }

    static Long getPublishDate(DateWrapper date) {
        if (date != null && date.date() != null) {
            return date.date().getTimeInMillis();
        } else {
            return System.currentTimeMillis();
        }
    }
/*
    static long getPublishDate(String dateStr) throws ParseException {
        Date publishDate = new SimpleDateFormat("yyyy-MM-dd").parse(dateStr);
        // TODO: publish date is not accurate - as only date precision is available
        // So it's more convenient, if the upload date happened in this day, we just assume, that it happened a minute
        // ago, so new videos appear in a better order in the Feed fragment.
        final long now = System.currentTimeMillis();
        if (publishDate.getTime() > (now - (24 * 3600 * 1000))) {
            return now - 60000;
        } else {
            return publishDate.getTime();
        }
    }
*/
    static String getThumbnailUrl(String id) {
        // Logger.d(NewPipeService.class, "getThumbnailUrl  %s", id);
        return "https://i.ytimg.com/vi/" + id + "/hqdefault.jpg";
    }

    private String filterHtml(String htmlContent) {
        String result = Jsoup.clean(htmlContent, "", Whitelist.none(), new OutputSettings().prettyPrint(false));
        if (DEBUG_LOG) {
            Logger.d(this, "filterHtml %s -> %s", htmlContent, result);
        }
        return result;
    }

    public Pager<InfoItem> getSearchResult(String query) throws ExtractionException, IOException {
        SearchExtractor extractor = streamingService.getSearchExtractor(query);
        extractor.fetchPage();

        return new Pager<>(streamingService, extractor, null);
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

    // TODO: Eliminate when API level 19 is used.
    public static void requireNonNull(Object obj, String message) {
        if (obj == null) {
            throw new NullPointerException(message);
        }
    }

    /**
     * @return true, if it's the preferred backend API
     */
    public static boolean isPreferred() {
        return SkyTubeApp.getPreferenceManager().getBoolean(SkyTubeApp.getStr(R.string.pref_use_newpipe_backend), false);
    }
}
