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

package free.rm.skytube.businessobjects.YouTube.POJOs;

import android.widget.Toast;

import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.ChannelBrandingSettings;
import com.google.api.services.youtube.model.ChannelSnippet;
import com.google.api.services.youtube.model.ChannelStatistics;
import com.google.api.services.youtube.model.ThumbnailDetails;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.Logger;
import free.rm.skytube.businessobjects.YouTube.VideoBlocker;
import free.rm.skytube.businessobjects.db.ChannelFilteringDb;
import free.rm.skytube.businessobjects.db.SubscriptionsDb;
import free.rm.skytube.businessobjects.db.Tasks.SubscribeToChannelTask;

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


	public YouTubeChannel() {

	}


	public YouTubeChannel(String id, String title) {
		this.id = id;
		this.title = title;
	}


	/**
	 * Initialise this object.
	 *
	 * @param channel
	 * @param isUserSubscribed	        if set to true, then it means the user is subscribed to this
	 *                                  channel;  otherwise it means that we currently do not know
	 *                                  if the user is subbed or not (hence we need to check).
	 * @param shouldCheckForNewVideos   if set to true it will check with the database whether new
	 *                                  videos have been published since last visit.
	 */
	public void init(Channel channel, boolean isUserSubscribed, boolean shouldCheckForNewVideos) {
		parse(channel);

		// get any channel info that is stored in the database
		getChannelInfoFromDB(isUserSubscribed);

		// if the user has subbed to this channel, then check if videos have been publish since
		// the last visit to this channel
		if (this.isUserSubscribed && shouldCheckForNewVideos) {
			newVideosSinceLastVisit = SubscriptionsDb.getSubscriptionsDb().channelHasNewVideos(this);
		}
	}


	/**
	 * Parses a {@link Channel} and then sets the instance variables accordingly.
	 *
	 * @param channel
	 */
	private void parse(Channel channel) {
		this.id = channel.getId();

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
			this.bannerUrl = SkyTubeApp.isTablet() ? branding.getImage().getBannerTabletHdImageUrl() : branding.getImage().getBannerMobileHdImageUrl();

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
				Logger.e(this, "Unable to check if user has subscribed to channel id=" + id, tr);
				this.isUserSubscribed = false;
			}
		} else {
			this.isUserSubscribed = true;
		}

		// get the last time the user has visited this channel
		this.lastVisitTime = SubscriptionsDb.getSubscriptionsDb().getLastVisitTime(this);
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
			Logger.e(this, "Unable to update channel's last visit time.  ChannelID=" + id);
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


	/**
	 * Block a channel.  This operation depends on what filtering method was enabled by the user:
	 * i.e. either channel blacklisting or whitelisting.
	 */
	public void blockChannel() {
		blockChannel(true);
	}


	/**
	 * Block a channel.  This operation depends on what filtering method was enabled by the user:
	 * i.e. either channel blacklisting or whitelisting.
	 *
	 * @param displayToastMessage Set to true to display toast message when the operation is carried
	 *                            out.
	 */
	public boolean blockChannel(boolean displayToastMessage) {
		boolean success;

		// if user is subscribed to the channel, then unsubscribe first...
		if (SubscriptionsDb.getSubscriptionsDb().isUserSubscribedToChannel(getId())) {
			new SubscribeToChannelTask(this).executeInParallel();
		}

		if (VideoBlocker.isChannelBlacklistEnabled()) {
			success = blacklistChannel(displayToastMessage);
		} else {
			success = unwhitelistChannel(displayToastMessage);
		}

		return success;
	}


	/**
	 * Blacklist the channel.
	 *
	 * @param displayToastMessage   Set to true to display toast message when the operation is carried
	 *                              out.
	 *
	 * @return True if successful.
	 */
	private boolean blacklistChannel(boolean displayToastMessage) {
		boolean success = ChannelFilteringDb.getChannelFilteringDb().blacklist(this.getId(), this.getTitle());

		if (displayToastMessage) {
			Toast.makeText(SkyTubeApp.getContext(),
					success ? R.string.channel_blacklisted : R.string.channel_blacklist_error,
					Toast.LENGTH_LONG).show();
		}

		return success;
	}


	/**
	 * Whitelist the channel.
	 *
	 * @param displayToastMessage   Set to true to display toast message when the operation is carried
	 *                              out.
	 *
	 * @return True if successful.
	 */
	private boolean unwhitelistChannel(boolean displayToastMessage) {
		boolean success = ChannelFilteringDb.getChannelFilteringDb().unwhitelist(this.getId());

		if (displayToastMessage) {
			Toast.makeText(SkyTubeApp.getContext(),
					success ? R.string.channel_unwhitelist_success : R.string.channel_unwhitelist_error,
					Toast.LENGTH_LONG).show();
		}

		return success;
	}

}
