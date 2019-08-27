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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.ListExtractor;
import org.schabi.newpipe.extractor.ListExtractor.InfoItemsPage;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.LinkHandlerFactory;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;

import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;

/**
 * Class to parse a channel screen for searching for videos and returning as pages with {@link #getVideos()}.
 * 
 * @author zsombor
 *
 */
public class Pager<I extends InfoItem> {
    private final StreamingService streamingService;
    private final ListExtractor<I> channelExtractor;
    private final YouTubeChannel channel;
    private String nextPageUrl;
    private boolean hasNextPage = true;

    Pager(StreamingService streamingService, ListExtractor<I> channelExtractor, YouTubeChannel channel) {
        this.streamingService = streamingService;
        this.channelExtractor = channelExtractor;
        this.channel = channel;
    }

    public YouTubeChannel getChannel() {
        return channel;
    }

    /**
     * @return true, if there could be more videos available in the next page.
     */
    public boolean isHasNextPage() {
        return hasNextPage;
    }

    /**
     * @return the next page of videos.
     * @throws ParsingException
     * @throws IOException
     * @throws ExtractionException
     */
    public List<YouTubeVideo> getVideos() throws ParsingException, IOException, ExtractionException {
        if (!hasNextPage) {
            return Collections.emptyList();
        }
        if (nextPageUrl == null) {
            return process(channelExtractor.getInitialPage());
        } else {
            return process(channelExtractor.getPage(nextPageUrl));
        }
    }

    private List<YouTubeVideo> process(InfoItemsPage<I> page) throws ParsingException {
        nextPageUrl = page.getNextPageUrl();
        hasNextPage = nextPageUrl != null;
        return extract(page);
    }

    private List<YouTubeVideo> extract(InfoItemsPage<I> page) throws ParsingException {
        List<YouTubeVideo> result = new ArrayList<>(page.getItems().size());
        LinkHandlerFactory linkHandlerFactory = streamingService.getStreamLHFactory();
        for (I infoItem : page.getItems()) {
            if (infoItem instanceof StreamInfoItem) {
                result.add(convert((StreamInfoItem) infoItem, linkHandlerFactory));
            }
        }
        return result;
    }

    private YouTubeVideo convert(StreamInfoItem item, LinkHandlerFactory linkHandlerFactory) throws ParsingException {
        String id = linkHandlerFactory.getId(item.getUrl());
        Long publishDate = NewPipeService.getPublishDateSafe(item.getUploadDate(), id);
        YouTubeChannel ch = channel != null ? channel : new YouTubeChannel(item.getUploaderUrl(), item.getUploaderName());
        return new YouTubeVideo(id, item.getName(), null, item.getDuration(), ch,
                item.getViewCount(), publishDate, item.getUploadDate(), NewPipeService.getThumbnailUrl(id));
    }

}
