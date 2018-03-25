/*
 * SkyTube
 * Copyright (C) 2018  Ramon Mifsud
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

import android.support.v4.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.List;

import free.rm.skytube.businessobjects.AsyncTaskParallel;
import free.rm.skytube.businessobjects.Logger;
import free.rm.skytube.businessobjects.YouTube.GetYouTubeVideos;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;
import free.rm.skytube.businessobjects.db.BlockedChannelsDb;
import free.rm.skytube.businessobjects.db.SubscriptionsDb;
import free.rm.skytube.gui.businessobjects.adapters.VideoGridAdapter;


/**
 * An asynchronous task that will retrieve YouTube videos and displays them in the supplied Adapter.
 */
public class GetYouTubeVideosTask extends AsyncTaskParallel<Void, Void, List<YouTubeVideo>> {

    /** Object used to retrieve the desired YouTube videos. */
    private GetYouTubeVideos getYouTubeVideos;

    /** The Adapter where the retrieved videos will be displayed. */
    private VideoGridAdapter	videoGridAdapter;

    /** SwipeRefreshLayout will be used to display the progress bar */
    private SwipeRefreshLayout  swipeRefreshLayout;

    private YouTubeChannel channel;


    /**
     * Constructor to get youtube videos as part of a swipe to refresh. Since this functionality has its own progress bar, we'll
     * skip showing our own.
     *
     * @param getYouTubeVideos The object that does the actual fetching of videos.
     * @param videoGridAdapter The grid adapter the videos will be added to.
     */
    public GetYouTubeVideosTask(GetYouTubeVideos getYouTubeVideos, VideoGridAdapter videoGridAdapter, SwipeRefreshLayout swipeRefreshLayout) {
        this.getYouTubeVideos = getYouTubeVideos;
        this.videoGridAdapter = videoGridAdapter;
        this.swipeRefreshLayout = swipeRefreshLayout;
    }


    @Override
    protected void onPreExecute() {
        // if this task is being called by ChannelBrowserFragment, then get the channel the user is browsing
        channel = videoGridAdapter.getYouTubeChannel();

        if (swipeRefreshLayout != null)
            swipeRefreshLayout.setRefreshing(true);
    }


    @Override
    protected List<YouTubeVideo> doInBackground(Void... params) {
        List<YouTubeVideo> videosList = null;
        ArrayList<YouTubeVideo> youTubeVideoList = new ArrayList<>();
        List<String> blockedChannelIds = new ArrayList<>();
        final BlockedChannelsDb blockedChannelsDb = BlockedChannelsDb.getBlockedChannelsDb();

        if (!isCancelled()) {
            // get videos from YouTube
            videosList = getYouTubeVideos.getNextVideos();

            if (videosList != null && channel != null && channel.isUserSubscribed()) {
                for (YouTubeVideo video : videosList) {
                    channel.addYouTubeVideo(video);
                }
                SubscriptionsDb.getSubscriptionsDb().saveChannelVideos(channel);
            }

            try {
                //get the IDs of blocked channels
                for (String channelIds : blockedChannelsDb.getBlockedChannelsListId()) {
                    blockedChannelIds.add(channelIds);
                }

                //filtering system to get the videos that are not blocked
                //videos are checked by their IDs - if their IDs are blocked they are not loaded.

                // videoList needs to be null checked - if there is not connection we get null point exception
                if (videosList != null) {
                    for (YouTubeVideo video : videosList) {
                        if (!blockedChannelIds.contains(video.getChannelId())) {
                            youTubeVideoList.add(video);
                        }
                    }
                }
            } catch (Exception e) {
                Logger.e(this, "Error occurred while checking blocked channels", e);
            }
        }
        
        return youTubeVideoList;
    }


    @Override
    protected void onPostExecute(List<YouTubeVideo> videosList) {
        videoGridAdapter.appendList(videosList);

        if(swipeRefreshLayout != null)
            swipeRefreshLayout.setRefreshing(false);
    }


    @Override
    protected void onCancelled() {
        if(swipeRefreshLayout != null)
            swipeRefreshLayout.setRefreshing(false);
    }

}
