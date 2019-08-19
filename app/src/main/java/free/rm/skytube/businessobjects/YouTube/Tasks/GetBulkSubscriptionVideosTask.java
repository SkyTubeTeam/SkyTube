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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import free.rm.skytube.businessobjects.AsyncTaskParallel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.db.SubscriptionsDb;

/**
 * A task that returns the videos of channel the user has subscribed too. Used
 * to detect if new videos have been published since last time the user used the
 * app.
 */
public class GetBulkSubscriptionVideosTask extends AsyncTaskParallel<Void, GetBulkSubscriptionVideosTask.Progress, Void> {

    static class Progress {
        YouTubeChannel dbChannel;
        int newVideos;
        public Progress(YouTubeChannel dbChannel, int newVideos) {
            this.dbChannel = dbChannel;
            this.newVideos = newVideos;
        }
        
    }
    private final GetSubscriptionVideosTaskListener listener;
    private final List<YouTubeChannel> channels;

    private List<GetBulkChannelVideosTask> tasks = new ArrayList<>();
    private int numTasksLeft = 0;
    private int numTasksFinished = 0;

    public GetBulkSubscriptionVideosTask(List<YouTubeChannel> channels, GetSubscriptionVideosTaskListener listener) {
        this.listener = listener;
        this.channels = channels;
    }

    @Override
    protected Void doInBackground(Void... params) {
        AtomicBoolean changed = new AtomicBoolean(false);

        for(final YouTubeChannel channel : channels) {
            tasks.add(new GetBulkChannelVideosTask(channel)
                    .setGetChannelVideosTaskInterface(videos -> {
                        numTasksFinished++;
                        boolean videosDeleted = false;
                        int numberOfVideos = videos != null ? videos.size() : 0;
                        if (numberOfVideos > 0) {
                            changed.compareAndSet(false, true);
                        }
                        if(numTasksFinished < numTasksLeft) {
                            if(tasks.size() > 0) {
                                // More channels to fetch videos from
                                tasks.get(0).executeInParallel();
                                tasks.remove(0);
                            }
                            publishProgress(new Progress(channel, videos.size()));
                        } else {
                            videosDeleted = SubscriptionsDb.getSubscriptionsDb().trimSubscriptionVideos();

                            // All channels have finished querying. Update the last time this refresh was done.
                            GetSubscriptionVideosTask.updateFeedsLastUpdateTime();

                            publishProgress(new Progress(channel, videos.size()));

                            if (listener != null) {
                                listener.onAllChannelVideosFetched(videosDeleted || changed.get());
                            }
                        }
                    })

            );

        }

        numTasksLeft = tasks.size();

        int numToStart = tasks.size() >= 4 ? 4 : tasks.size();

        // Start fetching videos for up to 4 channels simultaneously.
        for(int i=0;i<numToStart;i++) {
            tasks.get(0).executeInParallel();
            tasks.remove(0);
        }

        return null;
    }

    @Override
    protected void onProgressUpdate(Progress... values) {
        for (Progress p : values) {
            listener.onChannelVideosFetched(p.dbChannel, p.newVideos, false);
        }
    }




}
