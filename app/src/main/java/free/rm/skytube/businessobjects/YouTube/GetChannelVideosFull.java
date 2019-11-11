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

package free.rm.skytube.businessobjects.YouTube;

import com.google.api.client.util.DateTime;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;
import free.rm.skytube.businessobjects.db.SubscriptionsDb;

/**
 * Returns the videos of a channel.
 *
 * <p>The is the "full" edition (as opposed to {@link GetChannelVideosLite}) which is used for users
 * that ARE using their own YouTube API key.  This is as YouTube.Search.List consumes 100 unit and
 * the results are accurate.
 */
public class GetChannelVideosFull extends GetYouTubeVideoBySearch implements GetChannelVideosInterface {

	private String channelId;

	@Override
	public void init() throws IOException {
		super.init();
		videosList.setOrder("date");
	}


	/**
	 * Set the channel id.
	 *
	 * @param channelId	Channel ID.
	 */
	@Override
	public void setQuery(String channelId) {
		this.channelId = channelId;
		if (videosList != null) {
			videosList.setChannelId(channelId);
		}
	}


	@Override
	public void setPublishedAfter(long timeInMs) {
		if (videosList != null) {
			videosList.setPublishedAfter(new DateTime(timeInMs));
		}
	}

	@Override
	protected List<YouTubeVideo> getVideoListFromIds(List<String> videoIds) throws IOException {
		if (videoIds != null && !videoIds.isEmpty() && channelId != null) {
			final Set<String> videosByChannel = SubscriptionsDb.getSubscriptionsDb().getSubscribedChannelVideosByChannel(channelId);
			videoIds.removeAll(videosByChannel);
		}
		return super.getVideoListFromIds(videoIds);
	}
}
