/*
 * SkyTube
 * Copyright (C) 2016  Ramon Mifsud
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

package free.rm.skytube.businessobjects;

import android.util.Log;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.ChannelBrandingSettings;
import com.google.api.services.youtube.model.ChannelListResponse;
import com.google.api.services.youtube.model.ChannelSnippet;
import com.google.api.services.youtube.model.ChannelStatistics;
import com.google.api.services.youtube.model.ThumbnailDetails;

import java.io.IOException;
import java.util.List;

import free.rm.skytube.R;
import free.rm.skytube.gui.app.SkyTubeApp;

/**
 * Represents a YouTube Channel.
 *
 * <p>This class has the ability to query channel info by using the given channel ID.</p>
 */
public class YouTubeChannel {

	private String id;
	private String title;
	private String description;
	private String thumbnailHdUrl;
	private String thumbnailNormalUrl;
	private String bannerUrl;
	private String totalSubscribers;

	private static final String	TAG = YouTubeChannel.class.getSimpleName();


	public void init(String channelId) throws IOException {
		HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
		JsonFactory jsonFactory = com.google.api.client.extensions.android.json.AndroidJsonFactory.getDefaultInstance();
		YouTube			youtube = new YouTube.Builder(httpTransport, jsonFactory, null /*timeout here?*/).build();

		YouTube.Channels.List channelInfo = youtube.channels().list("snippet, statistics, brandingSettings");
		channelInfo.setFields("items(id, snippet/title, snippet/description, snippet/thumbnails/high, snippet/thumbnails/default," +
				"statistics/subscriberCount, brandingSettings/image/bannerTabletHdImageUrl)," +
				"nextPageToken");
		channelInfo.setKey(SkyTubeApp.getStr(R.string.API_KEY));
		channelInfo.setId(channelId);

		// get this channel's info from the remote YouTube server
		if (getChannelInfo(channelInfo)) {
			this.id = channelId;
		}
	}


	private boolean getChannelInfo(YouTube.Channels.List channelInfo) {
		List<Channel>	channelList = null;
		boolean			successful = false;

		try {
			// communicate with YouTube
			ChannelListResponse response = channelInfo.execute();

			// get channel
			channelList = response.getItems();
		} catch (IOException e) {
			Log.e(TAG, "Error has occurred while getting Featured Videos.", e);
		}


		if (channelList.size() <= 0)
			Log.e(TAG, "channelList is empty");
		else {
			parse(channelList.get(0));
			successful = true;
		}

		return successful;
	}


	private void parse(Channel channel) {
		ChannelSnippet snippet = channel.getSnippet();
		if (snippet != null) {
			this.title = snippet.getTitle();
			this.description = snippet.getDescription();

			ThumbnailDetails thumbnail = snippet.getThumbnails();
			if (thumbnail != null) {
				this.thumbnailHdUrl = snippet.getThumbnails().getHigh().getUrl();
				this.thumbnailNormalUrl = snippet.getThumbnails().getDefault().getUrl();
			}
		}

		ChannelBrandingSettings branding = channel.getBrandingSettings();
		if (branding != null)
			this.bannerUrl = branding.getImage().getBannerTabletHdImageUrl();

		ChannelStatistics statistics = channel.getStatistics();
		if (statistics != null) {
			this.totalSubscribers = String.format(SkyTubeApp.getStr(R.string.total_subscribers),
																	statistics.getSubscriberCount());
		}
	}


	public String getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public String getThumbnailHdUrl() {
		return thumbnailHdUrl;
	}

	public String getThumbnailNormalUrl() {
		return thumbnailNormalUrl;
	}

	public String getBannerUrl() {
		return bannerUrl;
	}

	public String getTotalSubscribers() {
		return totalSubscribers;
	}

}
