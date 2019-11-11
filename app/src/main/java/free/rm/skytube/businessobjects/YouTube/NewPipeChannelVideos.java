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

import free.rm.skytube.businessobjects.YouTube.VideoStream.NewPipeService;
import free.rm.skytube.businessobjects.YouTube.VideoStream.Pager;

/**
 * Adapter class to get list of videos from a channel.
 */
public class NewPipeChannelVideos extends NewPipeVideos<StreamInfoItem> implements GetChannelVideosInterface {

    private String channelId;

    @Override
    public void setChannelQuery(String channelId, boolean filterSubscribedVideos) {
        this.channelId = channelId;
    }

    @Override
    public void setPublishedAfter(long timeInMs) {

    }

    @Override
    protected Pager<StreamInfoItem> createNewPager() throws ExtractionException, IOException {
        return NewPipeService.get().getChannelPager(channelId);
    }
}
