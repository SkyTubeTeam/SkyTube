package free.rm.skytube.businessobjects;

import java.io.IOException;

import free.rm.skytube.gui.businessobjects.Logger;

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
			channel = new GetChannelsDetails().getYouTubeChannels(channelId[0]);
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