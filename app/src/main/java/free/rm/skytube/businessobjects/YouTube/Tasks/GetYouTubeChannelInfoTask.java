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

import android.content.Context;
import android.widget.Toast;

import java.io.IOException;

import free.rm.skytube.R;
import free.rm.skytube.businessobjects.AsyncTaskParallel;
import free.rm.skytube.businessobjects.Logger;
import free.rm.skytube.businessobjects.YouTube.GetChannelsDetails;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannelInterface;

/**
 * A task that given a channel ID it will try to initialize and return {@link YouTubeChannel}.
 */
public class GetYouTubeChannelInfoTask extends AsyncTaskParallel<String, Void, YouTubeChannel> {

	private YouTubeChannelInterface youTubeChannelInterface;
	private Context context;
	private boolean usingUsername = false;

	public GetYouTubeChannelInfoTask(Context context, YouTubeChannelInterface youTubeChannelInterface) {
		this.context = context;
		this.youTubeChannelInterface = youTubeChannelInterface;
	}

	/**
	 * Set the flag that the execution of this task is passing a username, not an id.
	 * @return this object, for chaining
	 */
	public GetYouTubeChannelInfoTask setUsingUsername() {
		usingUsername = true;
		return this;
	}


	@Override
	protected YouTubeChannel doInBackground(String... channelId) {
		YouTubeChannel channel;

		try {
			if(usingUsername)
				channel = new GetChannelsDetails().getYouTubeChannelFromUsername(channelId[0]);
			else
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
			if (youTubeChannel != null) {
				youTubeChannelInterface.onGetYouTubeChannel(youTubeChannel);
			} else {
				showError();
			}
		}
	}

	protected void showError() {
		Toast.makeText(context,
				context.getString(R.string.could_not_get_channel),
				Toast.LENGTH_LONG).show();
	}

}