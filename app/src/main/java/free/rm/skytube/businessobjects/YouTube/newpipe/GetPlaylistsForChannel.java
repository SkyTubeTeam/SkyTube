/*
 * SkyTube
 * Copyright (C) 2023  Zsombor Gegesy
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

import org.schabi.newpipe.extractor.ListExtractor;
import org.schabi.newpipe.extractor.Page;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubePlaylist;
import free.rm.skytube.businessobjects.YouTube.YouTubeTasks;

public class GetPlaylistsForChannel implements YouTubeTasks.ChannelPlaylistFetcher {
    static class Paging {
        private final YouTubeChannel channel;
        private final ListExtractor<PlaylistInfoItem> extractor;
        private Page nextPage;
        private boolean firstPage = false;
        Paging(YouTubeChannel channel, ListExtractor<PlaylistInfoItem> extractor) {
            this.channel = channel;
            this.extractor = extractor;
        }

        private synchronized List<PlaylistInfoItem> getNextPage() throws ExtractionException, IOException {
            extractor.fetchPage();
            if (firstPage) {
                if (Page.isValid(nextPage)) {
                    ListExtractor.InfoItemsPage<PlaylistInfoItem> res = extractor.getPage(nextPage);
                    nextPage = res.getNextPage();
                    return res.getItems();
                } else {
                    return Collections.emptyList();
                }
            } else {
                ListExtractor.InfoItemsPage<PlaylistInfoItem> res = extractor.getInitialPage();
                firstPage = true;
                nextPage = res.getNextPage();
                return res.getItems();
            }
        }

        private List<YouTubePlaylist> getNextPlaylists() throws ExtractionException, IOException {
            List<PlaylistInfoItem> infoItems = getNextPage();
            return infoItems.stream().map(item ->
                    new YouTubePlaylist(item.getUrl(), item.getName(), "", null, item.getStreamCount(), item.getThumbnailUrl(),
                            channel)
            ).collect(Collectors.toList());
        }

    }

    private final YouTubeChannel channel;

    private Paging paging;
    public GetPlaylistsForChannel(YouTubeChannel channel) {
        this.channel = channel;
    }
    @Override
    public void reset() {
        paging = null;
    }

    @Override
    public YouTubeChannel getChannel() {
        return channel;
    }

    @Override
    public List<YouTubePlaylist> getNextPlaylists() throws IOException, ExtractionException, NewPipeException {
        return getPaging().getNextPlaylists();
    }

    private synchronized Paging getPaging() throws NewPipeException, ParsingException {
        SkyTubeApp.nonUiThread();
        if (paging == null) {
            NewPipeService.ChannelWithExtractor cwe = NewPipeService.get().getChannelWithExtractor(channel.getId());
            paging = new Paging(cwe.channel, cwe.extractor.getPlaylists());
        }
        return paging;
    }

}
