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
import org.schabi.newpipe.extractor.channel.ChannelInfoItem;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.LinkHandlerFactory;
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import free.rm.skytube.businessobjects.Logger;
import free.rm.skytube.businessobjects.YouTube.POJOs.CardData;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubePlaylist;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;

public class VideoPager extends Pager<InfoItem, CardData> {
    private final Set<String> seenVideos = new HashSet<>();

    public VideoPager(StreamingService streamingService, ListExtractor<? extends InfoItem> channelExtractor) {
        super(streamingService, channelExtractor);
    }

    @Override
    protected List<CardData> extract(ListExtractor.InfoItemsPage<? extends InfoItem> page) throws NewPipeException {
        List<CardData> result = new ArrayList<>(page.getItems().size());
        if (NewPipeService.DEBUG_LOG) {
            Logger.d(this, "extract from %s, items: %s", page, page.getItems().size());
        }
        int repeatCounter = 0;
        int unexpected = 0;

        for (InfoItem infoItem : page.getItems()) {
            if (infoItem instanceof StreamInfoItem) {
                String id = getId(streamLinkHandler, infoItem.getUrl());
                StreamInfoItem streamInfo = (StreamInfoItem) infoItem;
                if (seenVideos.contains(id)) {
                    repeatCounter++;
                } else {
                    seenVideos.add(id);
                    result.add(convert(streamInfo, id));
                }
            } else if (infoItem instanceof PlaylistInfoItem) {
                PlaylistInfoItem playlistInfoItem = (PlaylistInfoItem) infoItem;
                result.add(convert(playlistInfoItem, getId(playlistLinkHandler, infoItem.getUrl())));
            } else if (infoItem instanceof ChannelInfoItem) {
                ChannelInfoItem channelInfoItem = (ChannelInfoItem) infoItem;
                result.add(convert(channelInfoItem));
            } else {
                Logger.i(this, "Unexpected item %s, type:%s", infoItem, infoItem.getClass());
                unexpected ++;
            }
        }
        if (NewPipeService.DEBUG_LOG) {
            Logger.d(this, "From the requested %s, number of duplicates: %s, wrong types: %s", page.getItems().size(), repeatCounter, unexpected);
        }
        return result;
    }

    private String getId(LinkHandlerFactory handler, String url) throws NewPipeException {
        try {
            return handler.getId(url);
        } catch (ParsingException e) {
            throw new NewPipeException("Unable to convert " + url + " with " + handler, e);
        }
    }

    public List<YouTubeVideo> getNextPageAsVideos() throws NewPipeException {
        List<CardData> cards = getNextPage();
        List<YouTubeVideo> result = new ArrayList<>(cards.size());
        for (CardData cardData: cards) {
            if (cardData instanceof YouTubeVideo) {
                result.add((YouTubeVideo) cardData);
            }
        }
        return result;
    }

    protected YouTubeVideo convert(StreamInfoItem item, String id) {
        NewPipeService.DateInfo date = new NewPipeService.DateInfo(item.getUploadDate());
        if (NewPipeService.DEBUG_LOG) {
            Logger.i(this, "item %s, title=%s at %s", id, item.getName(), date);
        }
        YouTubeChannel ch = new YouTubeChannel(item.getUploaderUrl(), item.getUploaderName());
        return new YouTubeVideo(id, item.getName(), null, item.getDuration(), ch,
                item.getViewCount(), date.instant, date.exact, NewPipeService.getThumbnailUrl(id));
    }

    private CardData convert(PlaylistInfoItem playlistInfoItem, String id) {
        return new YouTubePlaylist(id, playlistInfoItem.getName(), "", null, playlistInfoItem.getStreamCount(), playlistInfoItem.getThumbnailUrl(),
                null);
    }

    private CardData convert(ChannelInfoItem channelInfoItem) {
        String url = channelInfoItem.getUrl();
        String id = getId(url);
        return new YouTubeChannel(id, channelInfoItem.getName(), channelInfoItem.getDescription(), channelInfoItem.getThumbnailUrl(), null,
                channelInfoItem.getSubscriberCount(), false, -1, System.currentTimeMillis(), null, Collections.emptyList());
    }

    private String getId(String url) {
        try {
            return channelLinkHandler.getId(url);
        } catch (ParsingException p) {
            Logger.e(this, "Unable to parse channel url "+ url, p);
            return url;
        }
    }

}
