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

import android.util.Log;

import com.google.api.client.util.DateTime;

import java.io.IOException;
import java.util.List;

import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeAPIKey;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;

/**
 * Returns the videos of a channel.  The channel is specified by calling {@link #setQuery(String)}.
 *
 * <p>This class will detect if the user is using his own YouTube API key or not... if he is, then
 * we are going to use {@link GetChannelVideosFull}; otherwise we are going to use
 * {@link GetChannelVideosLite}.</p>
 */
public class GetChannelVideos extends GetYouTubeVideos implements GetChannelVideosInterface {

	private GetYouTubeVideos getChannelVideos;
	private static final String TAG = GetChannelVideos.class.getSimpleName();

	@Override
	public void init() throws IOException {
		if (YouTubeAPIKey.get().isUserApiKeySet()) {
			getChannelVideos = new GetChannelVideosFull();
			Log.d(TAG, "Using GetChannelVideosFull...");
		} else {
			getChannelVideos = new GetChannelVideosLite();
			Log.d(TAG, "Using GetChannelVideosLite...");
		}

		getChannelVideos.init();
	}

	/**
	 * Since this GetYouTubeVideos class uses its own instance of GetYouTubeVideos (getChannelVideos),
	 * when reset() is called on this instance, it must call reset on getChannelVideos. Otherwise,
	 * when a refresh is called via the Channel Browser, the next page of videos will be shown, instead
	 * of the first page.
	 */
	@Override
	public void reset() {
		super.reset();
		getChannelVideos.reset();
	}

	@Override
	public List<YouTubeVideo> getNextVideos() {
		return getChannelVideos.getNextVideos();
	}

	@Override
	public boolean noMoreVideoPages() {
		return getChannelVideos.noMoreVideoPages();
	}

	/**
	 * Set the channel id.
	 *
	 * @param channelId	Channel ID.
	 */
	@Override
	public void setQuery(String channelId) {
		getChannelVideos.setQuery(channelId);
	}

	@Override
	public void setPublishedAfter(DateTime dateTime) {
		((GetChannelVideosInterface) getChannelVideos).setPublishedAfter(dateTime);
	}

}
