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

import static free.rm.skytube.app.SkyTubeApp.getContext;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.util.Log;
import android.widget.Toast;
import free.rm.skytube.R;
import free.rm.skytube.businessobjects.AsyncTaskParallel;
import free.rm.skytube.businessobjects.YouTube.GetChannelVideosFull;
import free.rm.skytube.businessobjects.YouTube.GetChannelVideosInterface;
import free.rm.skytube.businessobjects.YouTube.GetChannelVideosLite;
import free.rm.skytube.businessobjects.YouTube.GetYouTubeVideos;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeAPIKey;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;
import free.rm.skytube.businessobjects.db.SubscriptionsDb;

/**
 * Task to asynchronously get videos for a specific channel.
 */
public class GetChannelVideosTask extends AsyncTaskParallel<Void, Void, List<YouTubeVideo>> {
	private static final String TAG = GetChannelVideosTask.class.getSimpleName();

	private final GetYouTubeVideos getChannelVideos;
	private final YouTubeChannel channel;
	private GetChannelVideosTaskInterface getChannelVideosTaskInterface;


	public GetChannelVideosTask(YouTubeChannel channel) {
		this.getChannelVideos = createChannelVideosFetcher();
		this.channel = channel;
		try {
			getChannelVideos.init();
			setPublishedAfter(getOneMonthAgo());
			getChannelVideos.setQuery(channel.getId());
		} catch (IOException e) {
			e.printStackTrace();
			Toast.makeText(getContext(),
				String.format(getContext().getString(R.string.could_not_get_videos), channel.getTitle()),
				Toast.LENGTH_LONG).show();
		}
	}

	/**
	 * Create an appropriate class to get videos of a channel. 
	 * The channel ID is specified by calling {@link #setQuery(String)}.
	 *
	 * <p>This class will detect if the user is using his own YouTube API key or not... if they are, then
	 * we are going to use {@link GetChannelVideosFull}; otherwise we are going to use
	 * {@link GetChannelVideosLite}.</p>
	 */
	public static GetYouTubeVideos createChannelVideosFetcher() {
		if (YouTubeAPIKey.get().isUserApiKeySet()) {
			Log.d(TAG, "Using GetChannelVideosFull...");
			return new GetChannelVideosFull();
		} else {
			Log.d(TAG, "Using GetChannelVideosLite...");
			return new GetChannelVideosLite();
		}

	}
	/**
	 * Once set, this class will only return videos published after the specified date.  If the date
	 * is set to null, then the class will return videos that are less than one month old.
	 */
	public GetChannelVideosTask setPublishedAfter(Long timeInMs) {
		((GetChannelVideosInterface)getChannelVideos).setPublishedAfter(timeInMs != null ? timeInMs : getOneMonthAgo());
		return this;
	}

	public GetChannelVideosTask setGetChannelVideosTaskInterface(GetChannelVideosTaskInterface getChannelVideosTaskInterface) {
		this.getChannelVideosTaskInterface = getChannelVideosTaskInterface;
		return this;
	}

	@Override
	protected List<YouTubeVideo> doInBackground(Void... voids) {
		List<YouTubeVideo> videos = null;

		if (!isCancelled()) {
			videos = getChannelVideos.getNextVideos();
		}

		if(videos != null) {
			for (YouTubeVideo video : videos)
				channel.addYouTubeVideo(video);
			if(channel.isUserSubscribed()) {
				SubscriptionsDb.getSubscriptionsDb().saveChannelVideos(channel);
			}
		}
		return videos;
	}


	@Override
	protected void onPostExecute(List<YouTubeVideo> youTubeVideos) {
		if(getChannelVideosTaskInterface != null)
			getChannelVideosTaskInterface.onGetVideos(youTubeVideos);
	}


	private long getOneMonthAgo() {
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.MONTH, -1);
		Date date = calendar.getTime();
		return date.getTime();
	}

}