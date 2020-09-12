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

import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import free.rm.skytube.R;
import free.rm.skytube.app.Utils;
import free.rm.skytube.businessobjects.AsyncTaskParallel;
import free.rm.skytube.businessobjects.VideoCategory;
import free.rm.skytube.businessobjects.YouTube.GetChannelVideosFull;
import free.rm.skytube.businessobjects.YouTube.GetChannelVideosInterface;
import free.rm.skytube.businessobjects.YouTube.GetChannelVideosLite;
import free.rm.skytube.businessobjects.YouTube.POJOs.CardData;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;
import free.rm.skytube.businessobjects.db.SubscriptionsDb;

import static free.rm.skytube.app.SkyTubeApp.getContext;

/**
 * Task to asynchronously get videos for a specific channel.
 */
public class GetChannelVideosTask extends AsyncTaskParallel<Void, Void, List<CardData>> {
	private static final String TAG = GetChannelVideosTask.class.getSimpleName();

	private final GetChannelVideosInterface getChannelVideos;
	private final String channelId;
	private final boolean filterSubscribedVideos;
	private YouTubeChannel channel;
	private final GetChannelVideosTaskInterface getChannelVideosTaskInterface;
	private final Long publishedAfter;

	/**
	 * Create an appropriate class to get videos of a channel.
	 *
	 * <p>This class will detect if the user is using his own YouTube API key or not... if they are, then
	 * we are going to use {@link GetChannelVideosFull}; otherwise we are going to use
	 * {@link GetChannelVideosLite}.</p>
	 */
	public GetChannelVideosTask(String channelId, Long publishedAfter, boolean filterSubscribedVideos,
								GetChannelVideosTaskInterface getChannelVideosTaskInterface) {
		Utils.requireNonNull(channelId, "channelId missing");
		this.getChannelVideos = VideoCategory.createChannelVideosFetcher();
		this.filterSubscribedVideos = filterSubscribedVideos;
		this.channelId = channelId;
		this.publishedAfter = publishedAfter;
		this.getChannelVideosTaskInterface = getChannelVideosTaskInterface;
	}


	@Override
	protected List<CardData> doInBackground(Void... voids) {
		List<CardData> videos = null;

		final SubscriptionsDb db = SubscriptionsDb.getSubscriptionsDb();
		if (!isCancelled()) {
			try {
				getChannelVideos.init();
				getChannelVideos.setPublishedAfter(publishedAfter != null ? publishedAfter : getOneMonthAgo());
				getChannelVideos.setChannelQuery(channelId, filterSubscribedVideos);
				videos = getChannelVideos.getNextVideos();
			} catch (IOException e) {
				lastException = e;
				channel = db.getCachedChannel(channelId);
			}
		}

		if(videos != null && !videos.isEmpty()) {
			if (db.isUserSubscribedToChannel(channelId)) {
				List<YouTubeVideo> realVideos = new ArrayList<>(videos.size());
				for (CardData cd : videos) {
					if (cd instanceof YouTubeVideo) {
						realVideos.add((YouTubeVideo) cd);
					}
				}
				db.saveVideos(realVideos, channelId);
			}
		}
		return videos;
	}

	@Override
	protected void onPostExecute(List<CardData> youTubeVideos) {
		if(getChannelVideosTaskInterface != null) {
			getChannelVideosTaskInterface.onGetVideos(youTubeVideos);
		}
		super.onPostExecute(youTubeVideos);
	}

	@Override
	protected void showErrorToUi() {
		if (lastException != null && channel != null) {
			Toast.makeText(getContext(),
					String.format(getContext().getString(R.string.could_not_get_videos), channel.getTitle()),
					Toast.LENGTH_LONG).show();
		}
	}

	private long getOneMonthAgo() {
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.MONTH, -1);
		Date date = calendar.getTime();
		return date.getTime();
	}

}