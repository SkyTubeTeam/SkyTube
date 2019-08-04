/*
 * SkyTube
 * Copyright (C) 2019 Zsombor Gegesy
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

package free.rm.skytube.businessobjects.YouTube.Tasks;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.schabi.newpipe.extractor.exceptions.ExtractionException;

import free.rm.skytube.businessobjects.AsyncTaskParallel;
import free.rm.skytube.businessobjects.Logger;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;
import free.rm.skytube.businessobjects.YouTube.VideoStream.NewPipeService;
import free.rm.skytube.businessobjects.db.SubscriptionsDb;

/**
 * A task that returns the videos of channel the user has subscribed too. Used
 * to detect if new videos have been published since last time the user used the
 * app.
 */
public class GetBulkSubscriptionVideosTask extends AsyncTaskParallel<Void, Void, Void> {

    private final GetSubscriptionVideosTaskListener listener;
    private final List<YouTubeChannel> channels;

    public GetBulkSubscriptionVideosTask(List<YouTubeChannel> channels, GetSubscriptionVideosTaskListener listener) {
        this.listener = listener;
        this.channels = channels;
    }

    @Override
    protected Void doInBackground(Void... params) {
        Set<String> alreadyKnownVideos = SubscriptionsDb.getSubscriptionsDb().getSubscribedChannelVideos();
        for (YouTubeChannel dbChannel : channels) {
            List<YouTubeVideo> newVideos = fetchVideos(alreadyKnownVideos, dbChannel);
            listener.onChannelVideosFetched(dbChannel, newVideos, false);
        }
        return null;
    }

    private List<YouTubeVideo> fetchVideos(Set<String> alreadyKnownVideos, YouTubeChannel dbChannel) {
        try {
            List<YouTubeVideo> videos = NewPipeService.get().getChannelVideos(dbChannel.getId());
            videos.removeIf(video -> alreadyKnownVideos.contains(video.getId()));
            return videos;
        } catch (ExtractionException | IOException e) {
            Logger.e(this, "Error during fetching channel page for " + dbChannel + ",msg:" + e.getMessage(), e);
            e.printStackTrace();
            return Collections.EMPTY_LIST;
        }
    }

}
