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

import org.schabi.newpipe.extractor.exceptions.ExtractionException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.AsyncTaskParallel;
import free.rm.skytube.businessobjects.Logger;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;
import free.rm.skytube.businessobjects.YouTube.newpipe.NewPipeException;
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
    private final List<String> channelIds;
    private final SubscriptionsDb subscriptionsDb;

    public GetBulkSubscriptionVideosTask(String channelId, GetSubscriptionVideosTaskListener listener) {
        this(Collections.singletonList(channelId), listener);
    }

    public GetBulkSubscriptionVideosTask(List<String> channelIds, GetSubscriptionVideosTaskListener listener) {
        this.listener = listener;
        this.channelIds = channelIds;
        this.subscriptionsDb = SubscriptionsDb.getSubscriptionsDb();
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        AtomicBoolean changed = new AtomicBoolean(false);
        CountDownLatch countDown = new CountDownLatch(channelIds.size());

        Semaphore semaphore = new Semaphore(4);

        for (String channelId : channelIds) {
            try {
                semaphore.acquire();
                NetworkAccess.runOnNetworkPool(new AsyncTaskParallel<Void, Void, Integer>() {

                    @Override
                    protected Integer doInBackground(Void... voids) {
                        Map<String, Long> alreadyKnownVideos = subscriptionsDb.getSubscribedChannelVideosByChannelToTimestamp(channelId);
                        List<YouTubeVideo> newVideos = fetchVideos(alreadyKnownVideos, channelId);
                        List<YouTubeVideo> detailedList = new ArrayList<>();
                        if (!newVideos.isEmpty()) {
                            YouTubeChannel dbChannel = subscriptionsDb.getCachedSubscribedChannel(channelId);
                            for (YouTubeVideo vid : newVideos) {
                                YouTubeVideo details;
                                try {
                                    details = NewPipeService.get().getDetails(vid.getId());
                                    if (Boolean.TRUE.equals(vid.getPublishTimestampExact())) {
                                        Logger.i(this, "updating %s with %s from %s", vid.getTitle(),
                                                new Date(vid.getPublishTimestamp()),
                                                new Date(details.getPublishTimestamp()));
                                        details.setPublishTimestamp(vid.getPublishTimestamp());
                                        details.setPublishTimestampExact(vid.getPublishTimestampExact());
                                    }
                                    details.setChannel(dbChannel);
                                    detailedList.add(details);
                                } catch (ExtractionException | IOException e) {
                                    Logger.e(this, "Error during parsing video page for " + vid.getId() + ",msg:" + e.getMessage(), e);
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
                            listener.onChannelVideosFetched(channelId, newYouTubeVideos, false);
                        }
                        countDown.countDown();
                    }
                });
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
        super.onPostExecute(changed);
    }

    @Override
    protected void onProgressUpdate(Progress... values) {
        if (listener != null) {
            for (Progress p : values) {
                listener.onChannelVideosFetched(p.dbChannel, p.newVideos, false);
            }
        }
    }

    private List<YouTubeVideo> fetchVideos(Map<String, Long> alreadyKnownVideos, String channelId) {
        try {
            List<YouTubeVideo> videos = NewPipeService.get().getVideosFromFeedOrFromChannel(channelId);
            Predicate<YouTubeVideo> predicate = video -> {
                Long storedTs = alreadyKnownVideos.get(video.getId());
                if (storedTs != null && Boolean.TRUE.equals(video.getPublishTimestampExact()) && !storedTs.equals(video.getPublishTimestamp())) {
                    // the freshly retrieved video contains an exact, and different publish timestamp
                    int result = subscriptionsDb.setPublishTimestamp(video);
                    Logger.i(this, "Updating publish timestamp for %s - %s with %s",
                            video.getId(), video.getTitle(), new Date(video.getPublishTimestamp()));
                }
                return storedTs != null;
            };
            // If we found a video which is already added to the db, no need to check the videos after,
            // assume, they are older, and already seen
            predicate.removeIf(videos);
            return videos;
        } catch (NewPipeException e) {
            Logger.e(this, "Error during fetching channel page for " + channelId + ",msg:" + e.getMessage(), e);
            return Collections.EMPTY_LIST;
        }
    }


}
