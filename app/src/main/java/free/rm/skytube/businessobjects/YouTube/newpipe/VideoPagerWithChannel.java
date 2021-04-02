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
import org.schabi.newpipe.extractor.stream.StreamInfoItem;

import free.rm.skytube.businessobjects.Logger;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;

public class VideoPagerWithChannel extends VideoPager {
    private final YouTubeChannel channel;

    public VideoPagerWithChannel(StreamingService streamingService, ListExtractor<InfoItem> channelExtractor, YouTubeChannel channel) {
        super(streamingService, channelExtractor);
        this.channel = channel;
    }

    public YouTubeChannel getChannel() {
        return channel;
    }

    @Override
    protected YouTubeVideo convert(StreamInfoItem item, String id) {
        NewPipeService.DateInfo date = new NewPipeService.DateInfo(item.getUploadDate());
        if (NewPipeService.DEBUG_LOG) {
            Logger.d(this, "item %s, title=%s at %s", id, item.getName(), date);
        }
        YouTubeChannel ch = channel != null ? channel : new YouTubeChannel(item.getUploaderUrl(), item.getUploaderName());
        return new YouTubeVideo(id, item.getName(), null, item.getDuration(), ch,
                item.getViewCount(), date.instant, date.exact, NewPipeService.getThumbnailUrl(id));
    }

}
