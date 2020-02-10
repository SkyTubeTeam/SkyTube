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

import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.ListExtractor;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.LinkHandlerFactory;
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import free.rm.skytube.businessobjects.Logger;
import free.rm.skytube.businessobjects.YouTube.POJOs.CardData;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubePlaylist;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;

public class VideoPager extends Pager<InfoItem, CardData> {
    private final YouTubeChannel channel;
    private final Set<String> seenVideos = new HashSet<>();

    public VideoPager(StreamingService streamingService, ListExtractor<InfoItem> channelExtractor, YouTubeChannel channel) {
        super(streamingService, channelExtractor);
        this.channel = channel;
    }

    public YouTubeChannel getChannel() {
        return channel;
    }


    @Override
    protected List<CardData> extract(ListExtractor.InfoItemsPage<InfoItem> page) throws ParsingException {
        List<CardData> result = new ArrayList<>(page.getItems().size());
        Logger.i(this, "extract from %s, items: %s", page, page.getItems().size());
        int repeatCounter = 0;
        int unexpected = 0;
        LinkHandlerFactory linkHandlerFactory = getStreamingService().getStreamLHFactory();
        for (InfoItem infoItem : page.getItems()) {
            String id = linkHandlerFactory.getId(infoItem.getUrl());
            if (infoItem instanceof StreamInfoItem) {
                StreamInfoItem streamInfo = (StreamInfoItem) infoItem;
                if (seenVideos.contains(id)) {
                    repeatCounter++;
                } else {
                    seenVideos.add(id);
                    result.add(convert(streamInfo, id));
                }
            } else if (infoItem instanceof PlaylistInfoItem) {
                PlaylistInfoItem playlistInfoItem = (PlaylistInfoItem) infoItem;
                result.add(convert(playlistInfoItem, id));
            } else {
                Logger.i(this, "Unexpected item %s, type:%s", infoItem, infoItem.getClass());
                unexpected ++;
            }
        }
        Logger.i(this, "From the requested %s, number of duplicates: %s, wrong types: %s", page.getItems().size(), repeatCounter, unexpected);
        return result;
    }

    public List<YouTubeVideo> getNextPageAsVideos() throws ParsingException, IOException, ExtractionException {
        List<CardData> cards = getNextPage();
        List<YouTubeVideo> result = new ArrayList<>(cards.size());
        for (CardData cardData: cards) {
            if (cardData instanceof YouTubeVideo) {
                result.add((YouTubeVideo) cardData);
            }
        }
        return result;
    }

    private YouTubeVideo convert(StreamInfoItem item, String id) throws ParsingException {
        Long publishDate = NewPipeService.getPublishDate(item.getUploadDate());
        YouTubeChannel ch = channel != null ? channel : new YouTubeChannel(item.getUploaderUrl(), item.getUploaderName());
        return new YouTubeVideo(id, item.getName(), null, item.getDuration(), ch,
                item.getViewCount(), publishDate, NewPipeService.getThumbnailUrl(id));
    }

    private CardData convert(PlaylistInfoItem playlistInfoItem, String id) {
        return new YouTubePlaylist(id, playlistInfoItem.getName(), "", null, playlistInfoItem.getStreamCount(), playlistInfoItem.getThumbnailUrl(),
                null);
    }


}
