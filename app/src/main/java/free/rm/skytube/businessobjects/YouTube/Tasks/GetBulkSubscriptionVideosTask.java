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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import org.schabi.newpipe.extractor.exceptions.ExtractionException;

import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.AsyncTaskParallel;
import free.rm.skytube.businessobjects.Logger;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;
import free.rm.skytube.businessobjects.YouTube.newpipe.NewPipeService;
import free.rm.skytube.businessobjects.db.SubscriptionsDb;
import free.rm.skytube.businessobjects.utils.Predicate;

/**
 * A task that returns the videos of channel the user has subscribed too. Used
 * to detect if new videos have been published since last time the user used the
 * app.
 */
public class GetBulkSubscriptionVideosTask extends AsyncTaskParallel<Void, GetBulkSubscriptionVideosTask.Progress, Boolean> {

    static class Progress {
        String dbChannel;
        int newVideos;
        public Progress(String dbChannel, int newVideos) {
            this.dbChannel = dbChannel;
            this.newVideos = newVideos;
        }
    }

    private final GetSubscriptionVideosTaskListener listener;
    private final List<YouTubeChannel> channels;
    private final SubscriptionsDb subscriptionsDb;

    public GetBulkSubscriptionVideosTask(YouTubeChannel channel, GetSubscriptionVideosTaskListener listener) {
        this(Collections.singletonList(channel), listener);
    }

    public GetBulkSubscriptionVideosTask(List<YouTubeChannel> channels, GetSubscriptionVideosTaskListener listener) {
        this.listener = listener;
        this.channels = channels;
        this.subscriptionsDb = SubscriptionsDb.getSubscriptionsDb();
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        Set<String> alreadyKnownVideos = subscriptionsDb.getSubscribedChannelVideos();

        AtomicBoolean changed = new AtomicBoolean(false);
        CountDownLatch countDown = new CountDownLatch(channels.size());

        Semaphore semaphore = new Semaphore(4);

        for (YouTubeChannel dbChannel : channels) {
            try {
                semaphore.acquire();
                new AsyncTaskParallel<Void, Void, Integer>() {

                    @Override
                    protected Integer doInBackground(Void... voids) {
                        List<YouTubeVideo> newVideos = fetchVideos(alreadyKnownVideos, dbChannel);
                        List<YouTubeVideo> detailedList = new ArrayList<>();
                        if (!newVideos.isEmpty()) {
                            for (YouTubeVideo vid : newVideos) {
                                YouTubeVideo details;
                                try {
                                    details = NewPipeService.get().getDetails(vid.getId());
                                    details.setChannel(dbChannel);
                                    detailedList.add(details);
                                } catch (ExtractionException | IOException e) {
                                    Logger.e(this, "Error during parsing video page for " + vid.getId() + ",msg:" + e.getMessage(), e);
                                    e.printStackTrace();
                                }
                            }
                            changed.compareAndSet(false, true);
                            subscriptionsDb.insertVideos(detailedList);
                        }
                        semaphore.release();
                        return detailedList.size();
                    }

                    @Override
                    protected void onPostExecute(Integer newYouTubeVideos) {
                        if (listener != null) {
                            listener.onChannelVideosFetched(dbChannel.getId(), newYouTubeVideos, false);
                        }
                        countDown.countDown();
                    }
                }.executeInParallel();
            } catch (InterruptedException e) {
                Logger.e(this, "Interrupt in semaphore.acquire:"+ e.getMessage(), e);
            }
        }

        try {
            countDown.await();
        } catch (InterruptedException e) {
            Logger.e(this, "Interrupt in countDown.await:"+ e.getMessage(), e);
        }
        SkyTubeApp.getSettings().updateFeedsLastUpdateTime(System.currentTimeMillis());
        return changed.get();
    }

    @Override
    protected void onPostExecute(Boolean changed) {
        if (listener != null) {
            listener.onAllChannelVideosFetched(changed);
        }
    }

    @Override
    protected void onProgressUpdate(Progress... values) {
        if (listener != null) {
            for (Progress p : values) {
                listener.onChannelVideosFetched(p.dbChannel, p.newVideos, false);
            }
        }
    }

    private List<YouTubeVideo> fetchVideos(Set<String> alreadyKnownVideos, YouTubeChannel dbChannel) {
        try {
            List<YouTubeVideo> videos = NewPipeService.get().getVideosFromFeedOrFromChannel(dbChannel.getId());
            Predicate<YouTubeVideo> predicate = video -> alreadyKnownVideos.contains(video.getId());
            // If we found a video which is already added to the db, no need to check the videos after,
            // assume, they are older, and already seen
            predicate.removeAfter(videos);
            return videos;
        } catch (ExtractionException | IOException e) {
            Logger.e(this, "Error during fetching channel page for " + dbChannel + ",msg:" + e.getMessage(), e);
            e.printStackTrace();
            return Collections.EMPTY_LIST;
        }
    }


}
