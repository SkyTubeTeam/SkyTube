package free.rm.skytube.businessobjects.YouTube.Tasks;

import org.schabi.newpipe.extractor.exceptions.ExtractionException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import free.rm.skytube.businessobjects.AsyncTaskParallel;
import free.rm.skytube.businessobjects.Logger;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;
import free.rm.skytube.businessobjects.YouTube.VideoStream.NewPipeService;
import free.rm.skytube.businessobjects.db.SubscriptionsDb;
import free.rm.skytube.businessobjects.utils.Predicate;

public class GetBulkChannelVideosTask extends AsyncTaskParallel<Void, Void, List<YouTubeVideo>> {
    private GetChannelVideosTaskInterface getChannelVideosTaskInterface;
    private YouTubeChannel channel;

    public GetBulkChannelVideosTask(YouTubeChannel channel) {
        this.channel = channel;
    }

    @Override
    protected List<YouTubeVideo> doInBackground(Void... voids) {
        Set<String> alreadyKnownVideos = SubscriptionsDb.getSubscriptionsDb().getSubscribedChannelVideos();
        List<YouTubeVideo> newVideos = fetchVideos(alreadyKnownVideos, channel);
        List<YouTubeVideo> detailedList = new ArrayList<>();
        Logger.d(this, "666 Fetching videos for %s", channel.getTitle());
        if (!newVideos.isEmpty()) {
            for (YouTubeVideo vid:newVideos) {
                YouTubeVideo details;
                Logger.d(this, "666 %s got %d videos", channel.getTitle(), newVideos.size());
                try {
                    details = NewPipeService.get().getDetails(vid.getId());
                    details.setChannel(vid.getChannel());
                    detailedList.add(details);
                    channel.addYouTubeVideo(details);
                    if(channel.isUserSubscribed())
                        SubscriptionsDb.getSubscriptionsDb().saveChannelVideos(channel);
                } catch (ExtractionException | IOException e) {
                    Logger.e(this, "Error during parsing video page for " + vid.getId() + ",msg:" + e.getMessage(), e);
                    e.printStackTrace();
                }
            }
        }
        return detailedList;
    }

    @Override
    protected void onPostExecute(List<YouTubeVideo> videos) {
        getChannelVideosTaskInterface.onGetVideos(videos);
    }


    public GetBulkChannelVideosTask setGetChannelVideosTaskInterface(GetChannelVideosTaskInterface getChannelVideosTaskInterface) {
        this.getChannelVideosTaskInterface = getChannelVideosTaskInterface;
        return this;
    }

    private List<YouTubeVideo> fetchVideos(Set<String> alreadyKnownVideos, YouTubeChannel dbChannel) {
        try {
            List<YouTubeVideo> videos = NewPipeService.get().getChannelVideos(dbChannel.getId());
            Predicate<YouTubeVideo> predicate = video -> alreadyKnownVideos.contains(video.getId());
            // If we found a video which is already added to the db, no need to check the videos after,
            // assume, they are older, and already seen
            Logger.d(this, "666 %s has %d videos", channel.getTitle(), videos.size());
            predicate.removeAfter(videos);
            return videos;
        } catch (ExtractionException | IOException e) {
            Logger.e(this, "Error during fetching channel page for " + dbChannel + ",msg:" + e.getMessage(), e);
            e.printStackTrace();
            return Collections.EMPTY_LIST;
        }
    }
}
