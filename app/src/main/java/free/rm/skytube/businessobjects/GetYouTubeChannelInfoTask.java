package free.rm.skytube.businessobjects;

import android.util.Log;

import java.io.IOException;

public class GetYouTubeChannelInfoTask extends AsyncTaskParallel<String, Void, YouTubeChannel> {

	private final String TAG = GetYouTubeChannelInfoTask.class.getSimpleName();
	private YouTubeChannelInterface youTubeChannelInterface;

	public GetYouTubeChannelInfoTask(YouTubeChannelInterface youTubeChannelInterface) {
		this.youTubeChannelInterface = youTubeChannelInterface;
	}

	@Override
	protected YouTubeChannel doInBackground(String... channelId) {
		YouTubeChannel chn = new YouTubeChannel();

		try {
			chn.init(channelId[0]);
		} catch (IOException e) {
			Log.e(TAG, "Unable to get channel info.  ChannelID=" + channelId[0], e);
			chn = null;
		}

		return chn;
	}

	@Override
	protected void onPostExecute(YouTubeChannel youTubeChannel) {
		if(youTubeChannelInterface != null) {
			youTubeChannelInterface.onGetYouTubeChannel(youTubeChannel);
		}
	}

}