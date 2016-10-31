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

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.ChannelBrandingSettings;
import com.google.api.services.youtube.model.ChannelListResponse;
import com.google.api.services.youtube.model.ChannelSnippet;
import com.google.api.services.youtube.model.ChannelStatistics;
import com.google.api.services.youtube.model.ThumbnailDetails;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import free.rm.skytube.BuildConfig;
import free.rm.skytube.R;
import free.rm.skytube.businessobjects.db.SubscriptionsDb;
import free.rm.skytube.gui.app.SkyTubeApp;

/**
 * Represents a YouTube Channel.
 *
 * <p>This class has the ability to query channel info by using the given channel ID.</p>
 */
public class YouTubeChannel implements Serializable {

	private String id;
	private String title;
	private String description;
	private String thumbnailNormalUrl;
	private String bannerUrl;
	private String totalSubscribers;
	private boolean isUserSubscribed;
	private long	lastVisitTime;
	private boolean	newVideosSinceLastVisit = false;
	private List<YouTubeVideo> youTubeVideos = new ArrayList<>();

	private static final String	TAG = YouTubeChannel.class.getSimpleName();


	/**
	 * Initialise this object.  This should not be called from the main thread.
	 *
	 * @param channelId	Channel ID
	 */
	public void init(String channelId) throws IOException {
		init(channelId, false, true);
	}


	/**
	 * Initialise this object.  This should not be called from the main thread.
	 *
	 * @param channelId			Channel ID
	 * @param isUserSubscribed	if set to true, then it means the user is subscribed to this channel;
	 *                          otherwise it means that we currently do not know if the user is
	 *                          subbed or not (hence we need to check).
	 * @throws IOException
	 */
	public void init(String channelId, boolean isUserSubscribed, boolean shouldCheckActivity) throws IOException {
		YouTube youtube = YouTubeAPI.create();
		YouTube.Channels.List channelInfo = youtube.channels().list("snippet, statistics, brandingSettings");
		channelInfo.setFields("items(id, snippet/title, snippet/description, snippet/thumbnails/default," +
				"statistics/subscriberCount, brandingSettings/image/bannerTabletHdImageUrl)," +
				"nextPageToken");
		channelInfo.setKey(BuildConfig.YOUTUBE_API_KEY);
		channelInfo.setId(channelId);

		// get this channel's info from the remote YouTube server
		if (getChannelInfo(channelInfo)) {
			this.id = channelId;

			// get any channel info that is stored in the database
			getChannelInfoFromDB(isUserSubscribed);

			// if the user has subbed to this channel, then check if videos have been publish since
			// the last visit to this channel
			if (isUserSubscribed && shouldCheckActivity) {
				// TODO : Optimise!
				Log.d(TAG, "Moo: " + title);
				CheckChannelActivity checkActivity = new CheckChannelActivity();
				checkActivity.init();
				newVideosSinceLastVisit = checkActivity.checkIfVideosBeenPublishedSinceLastVisit(this);
			}
		}
	}


	/**
	 * Get this channel's info from the remote YouTube server.
	 *
	 * @param channelInfo
	 *
	 * @return True if successful; false otherwise.
	 */
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
				this.thumbnailNormalUrl = snippet.getThumbnails().getDefault().getUrl();

				// YouTube Bug:  channels with no thumbnail/avatar will return a link to the default
				// thumbnail that does NOT start with "http" or "https", but rather it starts with
				// "//s.ytimg.com/...".  So in this case, we just add "https:" in front.
				String thumbnailUrlLowerCase = this.thumbnailNormalUrl.toLowerCase();
				if ( !(thumbnailUrlLowerCase.startsWith("http://")  ||  thumbnailUrlLowerCase.startsWith("https://")) )
					this.thumbnailNormalUrl = "https:" + this.thumbnailNormalUrl;
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


	/**
	 * Get any channel info that is stored in the database (locally).
	 *
	 * @param isUserSubscribed	if set to true, then it means the user is subscribed to this channel;
	 *                          otherwise it means that we currently do not know if the user is
	 *                          subbed or not (hence we need to check).
	 */
	private void getChannelInfoFromDB(boolean isUserSubscribed) {
		// check if the user is subscribed to this channel or not
		if (!isUserSubscribed) {
			try {
				this.isUserSubscribed = SubscriptionsDb.getSubscriptionsDb().isUserSubscribedToChannel(id);
			} catch (Throwable tr) {
				Log.e(TAG, "Unable to check if user has subscribed to channel id=" + id, tr);
				this.isUserSubscribed = false;
			}
		} else {
			this.isUserSubscribed = true;
		}

		// get the last time the user has visited this channel
		this.lastVisitTime = SubscriptionsDb.getSubscriptionsDb().getLastVisitTime(id);
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

	public String getThumbnailNormalUrl() {
		return thumbnailNormalUrl;
	}

	public String getBannerUrl() {
		return bannerUrl;
	}

	public String getTotalSubscribers() {
		return totalSubscribers;
	}

	public boolean isUserSubscribed() {
		return isUserSubscribed;
	}

	public void setUserSubscribed(boolean userSubscribed) {
		isUserSubscribed = userSubscribed;
	}

	public void updateLastVisitTime() {
		lastVisitTime = SubscriptionsDb.getSubscriptionsDb().updateLastVisitTime(id);

		if (lastVisitTime < 0) {
			Log.e(TAG, "Unable to update channel's last visit time.  ChannelID=" + id);
		}
	}

	public long getLastVisitTime() {
		return lastVisitTime;
	}

	public boolean newVideosSinceLastVisit() {
		return newVideosSinceLastVisit;
	}

	public void setNewVideosSinceLastVisit(boolean newVideos) {
		this.newVideosSinceLastVisit = newVideos;
	}

	public void addYouTubeVideo(YouTubeVideo video) {
		if(!youTubeVideos.contains(video)) {
			youTubeVideos.add(video);
		}
	}

	public List<YouTubeVideo> getYouTubeVideos() {
		return youTubeVideos;
	}
}
