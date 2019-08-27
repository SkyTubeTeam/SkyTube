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
import java.util.Collections;
import java.util.List;

import org.schabi.newpipe.extractor.exceptions.ExtractionException;

import free.rm.skytube.businessobjects.Logger;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;
import free.rm.skytube.businessobjects.YouTube.VideoStream.NewPipeService;
import free.rm.skytube.businessobjects.YouTube.VideoStream.Pager;

public class NewPipeChannelVideos extends GetYouTubeVideos implements GetChannelVideosInterface {

    private String channelId;
    private Pager pager;

    @Override
    public void setQuery(String channelId) {
        this.channelId = channelId;
    }

    @Override
    public void setPublishedAfter(long timeInMs) {

    }

    @Override
    public void init() throws IOException {
    }

    @Override
    public List<YouTubeVideo> getNextVideos() {
        if (pager == null) {
            try {
                pager = NewPipeService.get().getChannelPager(channelId);
            } catch (ExtractionException | IOException e) {
                Logger.e(this, "An error has occurred while getting videos:" + e.getMessage(), e);
                return Collections.emptyList();
            }
        }
        try {
            return pager.getVideos();
        } catch (IOException | ExtractionException e) {
            Logger.e(this, "An error has occurred while getting videos:" + e.getMessage(), e);
            return Collections.emptyList();
        } finally {
            noMoreVideoPages = !pager.isHasNextPage();
        }
    }

    @Override
    public void reset() {
        noMoreVideoPages = false;
        pager = null;
    }
}
