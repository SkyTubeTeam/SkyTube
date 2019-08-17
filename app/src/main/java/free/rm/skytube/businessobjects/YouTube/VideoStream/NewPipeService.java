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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document.OutputSettings;
import org.jsoup.safety.Whitelist;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.ListExtractor.InfoItemsPage;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.channel.ChannelExtractor;
import org.schabi.newpipe.extractor.exceptions.ContentNotAvailableException;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.LinkHandler;
import org.schabi.newpipe.extractor.linkhandler.LinkHandlerFactory;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler;
import org.schabi.newpipe.extractor.search.SearchExtractor;
import org.schabi.newpipe.extractor.stream.StreamExtractor;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.extractor.stream.VideoStream;

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
        ChannelExtractor channelExtractor = getChannelExtractor(channelId);

        InfoItemsPage<StreamInfoItem> initialPage = channelExtractor.getInitialPage();
        YouTubeChannel channel = createInternalChannel(channelExtractor);

        List<YouTubeVideo> result = extract(channel, initialPage);
        Logger.i(this, "getChannelVideos for %s(%s)  -> %s videos", channel.getTitle(), channelId, result.size());
        return result;
    }

    /**
     * Return detailed information for a channel from it's id.
     * @param channelId
     * @return the {@link YouTubeChannel}, with a list of recent videos.
     * @throws ExtractionException
     * @throws IOException
     */
    public YouTubeChannel getChannelDetails(String channelId) throws ExtractionException, IOException {
        ChannelExtractor extractor = getChannelExtractor(channelId);
        YouTubeChannel channel = createInternalChannel(extractor);
        channel.getYouTubeVideos().addAll(extract(channel, extractor.getInitialPage()));
        return channel;
    }

    private YouTubeChannel createInternalChannel(ChannelExtractor extractor) throws ParsingException {
        return new YouTubeChannel(extractor.getId(), extractor.getName(), filterHtml(extractor.getDescription()),
                extractor.getAvatarUrl(), extractor.getBannerUrl(), extractor.getSubscriberCount(), false, 0);
    }

    private ChannelExtractor getChannelExtractor(String channelId)
            throws ParsingException, ExtractionException, IOException {
        // Get channel LinkHandler
        ListLinkHandler channelLink = streamingService.getChannelLHFactory().fromId("channel/" + channelId);

        // Extract from it
        ChannelExtractor channelExtractor = streamingService.getChannelExtractor(channelLink);
        channelExtractor.fetchPage();
        return channelExtractor;
    }

    private List<YouTubeVideo> extract(YouTubeChannel channel, InfoItemsPage<StreamInfoItem> initialPage)
            throws ParsingException {
        List<YouTubeVideo> result = new ArrayList<>(initialPage.getItems().size());
        LinkHandlerFactory linkHandlerFactory = streamingService.getStreamLHFactory();
        for (StreamInfoItem item:initialPage.getItems()) {
            String id = linkHandlerFactory.getId(item.getUrl());
            Long publishDate = null;
            try {
                publishDate = getPublishDate(item.getUploadDate());
            } catch (ParseException e) {
                Logger.i(this, "Unable parse publish date %s(%s)  -> %s", channel.getTitle(), channel.getId(), item.getUploadDate());
            }
            YouTubeVideo video = new YouTubeVideo(id, item.getName(), null, item.getDuration(), item.getViewCount(), publishDate, item.getThumbnailUrl());
            video.setChannel(channel);
            result.add(video);
        }
        return result;
    }

    /**
     * Return detailed information about a video from it's id.
     * @param videoId the id of the video.
     * @return a {@link YoutTubeVideo}
     * @throws ExtractionException
     * @throws IOException
     */
    public YouTubeVideo getDetails(String videoId) throws ExtractionException, IOException {
        LinkHandler url = streamingService.getStreamLHFactory().fromId(videoId);
        StreamExtractor extractor = streamingService.getStreamExtractor(url);
        extractor.fetchPage();

        String dateStr = extractor.getUploadDate();
        try {
            return new YouTubeVideo(extractor.getId(), extractor.getName(), filterHtml(extractor.getDescription()),
                    extractor.getLength(),
                    extractor.getViewCount(), getPublishDate(dateStr), extractor.getThumbnailUrl()).setLikeDislikeCount(extractor.getLikeCount(), extractor.getDislikeCount());
        } catch (ParseException e) {
            throw new ExtractionException("Unable parse publish date:" + dateStr + " for video=" + videoId, e);
        }
    }

    private long getPublishDate(String dateStr) throws ParseException {
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

    private String filterHtml(String htmlContent) {
        String result = Jsoup.clean(htmlContent, "", Whitelist.none(), new OutputSettings().prettyPrint(false));
        Logger.d(this, "filterHtml %s -> %s", htmlContent, result);
        return result;
    }

    public List<YouTubeVideo> getSearchResult(String query, String token) throws ExtractionException, IOException {
        SearchExtractor extractor = streamingService.getSearchExtractor(query);
        extractor.fetchPage();
        return extractSearchResult(extractor.getInitialPage());
    }

    private List<YouTubeVideo> extractSearchResult(InfoItemsPage<InfoItem> initialPage)
            throws ParsingException {
        final List<YouTubeVideo> result = new ArrayList<>(initialPage.getItems().size());
        LinkHandlerFactory linkHandlerFactory = streamingService.getStreamLHFactory();
        for (InfoItem item:initialPage.getItems()) {
            if (item instanceof StreamInfoItem) {
                StreamInfoItem si = (StreamInfoItem) item;
                String id = linkHandlerFactory.getId(item.getUrl());
                result.add(new YouTubeVideo(id, item, si.getUploaderUrl(), si.getUploaderName(), si.getViewCount(), si.getDuration()));
            }
        }
        return result;
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
            SkyTubeApp.initNewPipe();
        }
        return instance;
    }

    /**
     * @return true, if it's the preferred backend API
     */
    public static boolean isPreferred() {
        return SkyTubeApp.getPreferenceManager().getBoolean(SkyTubeApp.getStr(R.string.pref_use_newpipe_backend), false);
    }
}
