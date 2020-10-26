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

package free.rm.skytube.businessobjects.YouTube.newpipe;

import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.ListExtractor;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.playlist.PlaylistExtractor;

import java.util.List;

import free.rm.skytube.businessobjects.YouTube.POJOs.CardData;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubePlaylist;

public class PlaylistPager extends VideoPager {
    private YouTubePlaylist playlist;
    private final PlaylistExtractor playlistExtractor;
    public PlaylistPager(StreamingService streamingService, PlaylistExtractor playlistExtractor) {
        super(streamingService, (ListExtractor) playlistExtractor);
        this.playlistExtractor = playlistExtractor;
    }

    @Override
    protected List<CardData> extract(ListExtractor.InfoItemsPage<InfoItem> page) throws NewPipeException {
        if (playlist == null) {
            try {
                String uploaderUrl = playlistExtractor.getUploaderUrl();
                String channelId = uploaderUrl != null ? getStreamingService().getChannelLHFactory().fromUrl(uploaderUrl).getId() : null;
                playlist = new YouTubePlaylist(
                        playlistExtractor.getId(),
                        playlistExtractor.getName(),
                        "" /* description */,
                        null /* publishDate */,
                        playlistExtractor.getStreamCount(),
                        playlistExtractor.getThumbnailUrl(),
                        new YouTubeChannel(channelId, playlistExtractor.getUploaderName())
                );
            } catch (ParsingException e) {
                e.printStackTrace();
            }
        }
        return super.extract(page);
    }

    public YouTubePlaylist getPlaylist() {
        return playlist;
    }
}
