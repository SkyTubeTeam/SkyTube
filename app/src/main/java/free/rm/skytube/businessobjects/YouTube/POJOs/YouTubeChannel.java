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

import android.content.Context;
import android.view.Menu;
import android.widget.Toast;

import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.ChannelBrandingSettings;
import com.google.api.services.youtube.model.ChannelSnippet;
import com.google.api.services.youtube.model.ChannelStatistics;
import com.google.api.services.youtube.model.ThumbnailDetails;

import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeChannelLinkHandlerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.app.Utils;
import free.rm.skytube.businessobjects.Logger;
import free.rm.skytube.businessobjects.YouTube.VideoBlocker;
import free.rm.skytube.businessobjects.db.ChannelFilteringDb;
import free.rm.skytube.businessobjects.db.DatabaseResult;
import free.rm.skytube.businessobjects.db.SubscriptionsDb;
import free.rm.skytube.businessobjects.db.Tasks.GetChannelInfo;
import free.rm.skytube.businessobjects.db.Tasks.SubscribeToChannelTask;
import free.rm.skytube.gui.businessobjects.adapters.SubsAdapter;
import free.rm.skytube.gui.fragments.SubscriptionsFeedFragment;

/**
 * Represents a YouTube Channel.
 *
 * <p>This class has the ability to query channel info by using the given channel ID.</p>
 */
public class YouTubeChannel extends CardData implements Serializable {

	private String bannerUrl;
	private String totalSubscribers;
	private long subscriberCount;
	private boolean isUserSubscribed;
	private long	lastVisitTime;
	private long    lastCheckTime;
	private long    lastVideoTime;
	private boolean	newVideosSinceLastVisit = false;
	private List<YouTubeVideo> youTubeVideos = new ArrayList<>();


	public YouTubeChannel() {

	}


	public YouTubeChannel(String id, String title) {
		this.id = id;
		this.title = title;
	}

	public YouTubeChannel(String id, String title, String description, String thumbnailUrl,
						  String bannerUrl, long subscriberCount, boolean isUserSubscribed, long lastVisitTime, long lastCheckTime) {
		this.id = id;
		this.title = title;
		this.description = description;
		this.thumbnailUrl = thumbnailUrl;
		this.bannerUrl = bannerUrl;
		this.subscriberCount = subscriberCount;
		this.totalSubscribers = getFormattedSubscribers(subscriberCount);
		this.isUserSubscribed = isUserSubscribed;
		this.lastVisitTime = lastVisitTime;
		this.lastCheckTime = lastCheckTime;
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
	 * @return true if the values are different than the stored data.
	 */
	public boolean init(Channel channel, boolean isUserSubscribed, boolean shouldCheckForNewVideos) {
		boolean ret = parse(channel);

		// if the user has subbed to this channel, then check if videos have been publish since
		// the last visit to this channel
		if (this.isUserSubscribed && shouldCheckForNewVideos) {
			newVideosSinceLastVisit = SubscriptionsDb.getSubscriptionsDb().channelHasNewVideos(this);
		}
		return ret;
	}


	/**
	 * Parses a {@link Channel} and then sets the instance variables accordingly.
	 *
	 * @param channel
	 */
	private boolean parse(Channel channel) {
		this.id = channel.getId();
		boolean ret = false;
		ChannelSnippet snippet = channel.getSnippet();
		if (snippet != null) {
			ret |= Utils.equals(this.title, snippet.getTitle());
			this.title = snippet.getTitle();
			ret |= Utils.equals(this.description, snippet.getDescription());
			this.description = snippet.getDescription();

			ThumbnailDetails thumbnail = snippet.getThumbnails();
			if (thumbnail != null) {
				String thmbNormalUrl = snippet.getThumbnails().getDefault().getUrl();

				// YouTube Bug:  channels with no thumbnail/avatar will return a link to the default
				// thumbnail that does NOT start with "http" or "https", but rather it starts with
				// "//s.ytimg.com/...".  So in this case, we just add "https:" in front.
				String thumbnailUrlLowerCase = thmbNormalUrl.toLowerCase();
				if ( !(thumbnailUrlLowerCase.startsWith("http://")  ||  thumbnailUrlLowerCase.startsWith("https://")) ) {
					thmbNormalUrl = "https:" + thmbNormalUrl;
				}
				ret |= Utils.equals(this.thumbnailUrl, thmbNormalUrl);
				this.thumbnailUrl = thmbNormalUrl;
			}
		}

		ChannelBrandingSettings branding = channel.getBrandingSettings();
		if (branding != null) {
			String bannerUrl = SkyTubeApp.isTablet() ? branding.getImage().getBannerTabletHdImageUrl() : branding.getImage().getBannerMobileHdImageUrl();
			ret |= Utils.equals(this.bannerUrl, bannerUrl);
			this.bannerUrl = bannerUrl;
		}

		ChannelStatistics statistics = channel.getStatistics();
		if (statistics != null) {
			long count = statistics.getSubscriberCount().longValue();
			ret |= this.subscriberCount != count;
			this.subscriberCount = count;
			this.totalSubscribers = getFormattedSubscribers(statistics.getSubscriberCount().longValue());
		}
		return ret;
	}

	private static String getFormattedSubscribers(long subscriberCount) {
		return String.format(SkyTubeApp.getStr(R.string.total_subscribers),subscriberCount);
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

	public long getSubscriberCount() {
		return subscriberCount;
	}

	public long getLastCheckTime() { return lastCheckTime; }

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

	public long getLastVideoTime() {
		return lastVideoTime;
	}

	public void setLastVideoTime(long lastVideoTime) {
		this.lastVideoTime = lastVideoTime;
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

	public static void subscribeChannel(final Context context, final Menu menu, final String channelId) {
		if (channelId != null) {
			new GetChannelInfo(context, youTubeChannel -> {
				DatabaseResult result = SubscriptionsDb.getSubscriptionsDb().subscribe(youTubeChannel);
				switch(result) {
					case SUCCESS: {
						youTubeChannel.setUserSubscribed(true);
						SubsAdapter.get(context).refreshSubsList();
						SubscriptionsFeedFragment.refreshSubsFeedFromCache();
						Toast.makeText(context, R.string.channel_subscribed, Toast.LENGTH_LONG).show();
						break;
					}
					case NOT_MODIFIED: {
						Toast.makeText(context, R.string.channel_already_subscribed, Toast.LENGTH_LONG).show();
						break;
					}
					default: {
						Toast.makeText(context, R.string.channel_subscribe_failed, Toast.LENGTH_LONG).show();
						break;
					}
				}
			}).executeInParallel(channelId);

		} else {
			Toast.makeText(context, "Channel is not specified", Toast.LENGTH_LONG).show();
		}
	}

	public String getChannelUrl() {
		try {
			return YoutubeChannelLinkHandlerFactory.getInstance().getUrl(getId());
		} catch (ParsingException p) {
			Logger.e(this, "getChannel URL for " + getId() + ", error:" + p.getMessage(), p);
			return id;
		}
	}
}
