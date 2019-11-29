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
 package free.rm.skytube.businessobjects.YouTube;

import java.io.IOException;

import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;

import free.rm.skytube.businessobjects.YouTube.newpipe.NewPipeService;
import free.rm.skytube.businessobjects.YouTube.newpipe.VideoPager;

/**
 * Adapter class to get list of videos from a channel.
 */
public class NewPipeChannelVideos extends NewPipeVideos<StreamInfoItem> implements GetChannelVideosInterface {

    private String channelId;

    @Override
    public void setChannelQuery(String channelId, boolean filterSubscribedVideos) {
        NewPipeService.requireNonNull(channelId, "channelId missing");
        this.channelId = channelId;
    }

    // Important, this is called from the channel tab
    @Override
    public void setQuery(String query) {
        NewPipeService.requireNonNull(query, "query missing");
        this.channelId = query;
    }

    @Override
    public void setPublishedAfter(long timeInMs) {

    }

    @Override
    protected VideoPager createNewPager() throws ExtractionException, IOException {
        NewPipeService.requireNonNull(channelId, "channelId missing");
        return NewPipeService.get().getChannelPager(channelId);
    }
}
