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

import java.io.IOException;

import free.rm.skytube.businessobjects.AsyncTaskParallel;
import free.rm.skytube.businessobjects.YouTube.GetChannelsDetails;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannelInterface;
import free.rm.skytube.businessobjects.Logger;

/**
 * A task that given a channel ID it will try to initialize and return {@link YouTubeChannel}.
 */
public class GetYouTubeChannelInfoTask extends AsyncTaskParallel<String, Void, YouTubeChannel> {

	private YouTubeChannelInterface youTubeChannelInterface;


	public GetYouTubeChannelInfoTask(YouTubeChannelInterface youTubeChannelInterface) {
		this.youTubeChannelInterface = youTubeChannelInterface;
	}


	@Override
	protected YouTubeChannel doInBackground(String... channelId) {
		YouTubeChannel channel;

		try {
			channel = new GetChannelsDetails().getYouTubeChannel(channelId[0]);
		} catch (IOException e) {
			Logger.e(this, "Unable to get channel info.  ChannelID=" + channelId[0], e);
			channel = null;
		}

		return channel;
	}


	@Override
	protected void onPostExecute(YouTubeChannel youTubeChannel) {
		if(youTubeChannelInterface != null) {
			youTubeChannelInterface.onGetYouTubeChannel(youTubeChannel);
		}
	}

}