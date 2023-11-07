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
import android.widget.Toast;

import androidx.core.util.Pair;

import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeChannelLinkHandlerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import free.rm.skytube.R;
import free.rm.skytube.app.EventBus;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.Logger;
import free.rm.skytube.businessobjects.YouTube.VideoBlocker;
import free.rm.skytube.businessobjects.YouTube.newpipe.ChannelId;
import free.rm.skytube.businessobjects.db.ChannelFilteringDb;
import free.rm.skytube.businessobjects.db.DatabaseTasks;
import free.rm.skytube.businessobjects.db.SubscriptionsDb;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

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
	private Integer categoryId;
	private final List<YouTubeVideo> youTubeVideos = new ArrayList<>();
	private final List<String> tags;

	public YouTubeChannel() {
		tags = Collections.emptyList();
	}

	public YouTubeChannel(String id, String title) {
		this();
		this.id = id;
		this.title = title;
	}

	public YouTubeChannel(String id, String title, String description, String thumbnailUrl,
						  String bannerUrl, long subscriberCount, boolean isUserSubscribed, long lastVisitTime, long lastCheckTime,
						  Integer categoryId, List<String> tags) {
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
		this.categoryId = categoryId;
		this.tags = tags;
	}

	private static String getFormattedSubscribers(long subscriberCount) {
		return String.format(SkyTubeApp.getStr(R.string.total_subscribers),subscriberCount);
	}

	public ChannelId getChannelId() {
		return new ChannelId(id);
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

	public Integer getCategoryId() {
		return categoryId;
	}

	public void setCategoryId(Integer categoryId) {
		this.categoryId = categoryId;
	}

	public void setUserSubscribed(boolean userSubscribed) {
		isUserSubscribed = userSubscribed;
	}

    public Disposable updateLastVisitTime() {
        return SubscriptionsDb.getSubscriptionsDb().updateLastVisitTimeAsync(id).subscribe(timestamp -> {
            lastVisitTime = timestamp;
            if (lastVisitTime < 0) {
                Logger.e(YouTubeChannel.this, "Unable to update channel's last visit time.  ChannelID=" + id);
            }
        });
    }

	public long getLastVisitTime() {
		return lastVisitTime;
	}

	public long getLastVideoTime() {
		return lastVideoTime;
	}

	public List<String> getTags() {
		return tags;
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
	public Single<Boolean> blockChannel() {
		return blockChannel(true);
	}

	/**
	 * Block a channel.  This operation depends on what filtering method was enabled by the user:
	 * i.e. either channel blacklisting or whitelisting.
	 *
	 * @param displayToastMessage Set to true to display toast message when the operation is carried
	 *                            out.
	 */
	public Single<Boolean> blockChannel(boolean displayToastMessage) {
		return SubscriptionsDb.getSubscriptionsDb().getUserSubscribedToChannel(getChannelId())
				.flatMap(isSubscribed -> DatabaseTasks.subscribeToChannel(false,
						null, SkyTubeApp.getContext(), this, false))
				.map(result -> SkyTubeApp.getSettings().isChannelDenyListEnabled())
				.flatMap(isDenyListEnabled -> {
					if (isDenyListEnabled) {
						return dennyChannel(displayToastMessage);
					} else {
						return removeAllowedChannel(displayToastMessage);
					}
				})
				.observeOn(AndroidSchedulers.mainThread());
	}

	/**
	 * Block a channel.  This operation depends on what filtering method was enabled by the user:
	 * i.e. either channel blacklisting or whitelisting.
	 */
	public Single<Boolean> unblockChannel() {
		return unblockChannel(true);
	}

	public Single<Boolean> unblockChannel(boolean displayToastMessage) {
		return Single.fromCallable(() -> SkyTubeApp.getSettings().isChannelDenyListEnabled())
				.flatMap(isDenyListEnabled -> {
					if (isDenyListEnabled) {
						return removeDeniedChannel(displayToastMessage);
					} else {
						return allowChannel(displayToastMessage);
					}
				})
				.observeOn(AndroidSchedulers.mainThread());
	}

	/**
	 * Denny the channel.
	 *
	 * @param displayToastMessage   Set to true to display toast message when the operation is carried
	 *                              out.
	 *
	 * @return True if successful.
	 */
	private Single<Boolean> dennyChannel(boolean displayToastMessage) {
		return Single.fromCallable(() -> ChannelFilteringDb.getChannelFilteringDb().denyChannel(id, title))
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.doOnSuccess(success -> {
					if (displayToastMessage) {
						Toast.makeText(SkyTubeApp.getContext(),
								success ? R.string.channel_blacklisted : R.string.channel_blacklist_error,
								Toast.LENGTH_LONG).show();
					}
				});
	}

	/**
	 * Denny the channel.
	 *
	 * @param displayToastMessage   Set to true to display toast message when the operation is carried
	 *                              out.
	 *
	 * @return True if successful.
	 */
	private Single<Boolean> allowChannel(boolean displayToastMessage) {
		return Single.fromCallable(() -> ChannelFilteringDb.getChannelFilteringDb().allowChannel(id, title))
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.doOnSuccess(success -> {
					if (displayToastMessage) {
						Toast.makeText(SkyTubeApp.getContext(),
								success ? R.string.channel_blacklisted : R.string.channel_blacklist_error,
								Toast.LENGTH_LONG).show();
					}
				});
	}

	/**
	 * Whitelist the channel.
	 *
	 * @param displayToastMessage   Set to true to display toast message when the operation is carried
	 *                              out.
	 *
	 * @return True if successful.
	 */
	private Single<Boolean> removeAllowedChannel(boolean displayToastMessage) {
		return Single.fromCallable(() -> ChannelFilteringDb.getChannelFilteringDb().removeAllowList(id))
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.doOnSuccess(success -> {
					if (displayToastMessage) {
						Toast.makeText(SkyTubeApp.getContext(),
								success ? R.string.channel_unwhitelist_success : R.string.channel_unwhitelist_error,
								Toast.LENGTH_LONG).show();
					}
				});
	}

	/**
	 * Remove channel from the deny list.
	 *
	 * @param displayToastMessage   Set to true to display toast message when the operation is carried
	 *                              out.
	 *
	 * @return True if successful.
	 */
	private Single<Boolean> removeDeniedChannel(boolean displayToastMessage) {
		return Single.fromCallable(() -> ChannelFilteringDb.getChannelFilteringDb().removeDenyList(id))
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.doOnSuccess(success -> {
					if (displayToastMessage) {
						Toast.makeText(SkyTubeApp.getContext(),
								success ? R.string.channel_blacklist_updated : R.string.channel_blacklist_update_failure,
								Toast.LENGTH_LONG).show();
					}
				});
	}

	public static Disposable subscribeChannel(final Context context, final ChannelId channelId) {
		if (channelId != null) {
			return DatabaseTasks.getChannelInfo(context, channelId, false)
					.observeOn(Schedulers.io())
					.map(youTubeChannel ->
						new Pair<>(youTubeChannel, SubscriptionsDb.getSubscriptionsDb().subscribe(youTubeChannel))
					)
					.observeOn(AndroidSchedulers.mainThread())
					.subscribe(youTubeChannelWithResult -> {
						switch(youTubeChannelWithResult.second) {
							case SUCCESS: {
								youTubeChannelWithResult.first.setUserSubscribed(true);
								EventBus.getInstance().notifyMainTabChanged(EventBus.SettingChange.SUBSCRIPTION_LIST_CHANGED);
								SkyTubeApp.getSettings().setRefreshSubsFeedFromCache(true);
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
					});
		} else {
			Toast.makeText(context, "Channel is not specified", Toast.LENGTH_LONG).show();
			return Disposable.empty();
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
