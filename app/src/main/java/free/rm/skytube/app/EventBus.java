/*
 * SkyTube
 * Copyright (C) 2021  Zsombor Gegesy
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

package free.rm.skytube.app;

import java.util.function.Consumer;

import free.rm.skytube.app.utils.WeakList;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;
import free.rm.skytube.businessobjects.YouTube.Tasks.GetSubscriptionVideosTaskListener;
import free.rm.skytube.gui.businessobjects.MainActivityListener;
import free.rm.skytube.gui.fragments.MainFragment;

public class EventBus {
    public enum SettingChange {
        HIDE_TABS,
        CONTENT_COUNTRY,
        SUBSCRIPTION_LIST_CHANGED
    }
    private static EventBus instance;

    private WeakList<MainFragment> mainFragments = new WeakList<>();
    private WeakList<MainActivityListener> mainActivityListeners = new WeakList<>();
    private WeakList<GetSubscriptionVideosTaskListener> subscriptionListeners = new WeakList<GetSubscriptionVideosTaskListener>();
    private WeakList<Consumer<YouTubeVideo>> videoDetailListeners = new WeakList<>();

    public static synchronized EventBus getInstance() {
        if (instance == null) {
            instance = new EventBus();
        }
        return instance;
    }

    public void registerMainFragment(MainFragment mainFragment) {
        this.mainFragments.add(mainFragment);
    }

    public void notifyMainTabChanged(SettingChange settingChange) {
        this.mainFragments.forEach(main -> main.refreshTabs(settingChange));
    }

    public void notifyChannelNewVideosStatus(String channelId, boolean newVideos) {
        this.mainFragments.forEach(main -> main.notifyChangeChannelNewVideosStatus(channelId, newVideos));
    }

    public void notifyChannelNewVideos(String channelId, int newVideos) {
        if (newVideos > 0) {
            this.mainFragments.forEach(main -> main.notifyChangeChannelNewVideosStatus(channelId, true));
        }
    }

    public void notifyVideoDetailFetched(YouTubeVideo video) {
        this.videoDetailListeners.forEach(listener -> listener.accept(video));
    }

    public void registerVideoDetailFetcher(Consumer<YouTubeVideo> videoListener) {
        this.videoDetailListeners.add(videoListener);
    }

    public void notifyChannelRemoved(String channelId) {
        this.mainFragments.forEach(main -> main.notifyChannelRemoved(channelId));
    }

    public void registerMainActivityListener(MainActivityListener listener) {
        this.mainActivityListeners.add(listener);
    }

    public void notifyMainActivities(Consumer<MainActivityListener> function) {
        this.mainActivityListeners.forEach(function);
    }

    public void registerSubscriptionListener(GetSubscriptionVideosTaskListener listener) {
        this.subscriptionListeners.add(listener);
    }

    public void unregisterSubscriptionListener(GetSubscriptionVideosTaskListener listener) {
        this.subscriptionListeners.remove(listener);
    }

    public void notifyChannelVideoFetchingFinished(boolean changes) {
        this.subscriptionListeners.forEach(listener -> listener.onChannelVideoFetchFinish(changes));
    }

    public void notifySubscriptionRefreshFinished() {
        this.subscriptionListeners.forEach(listener -> listener.onSubscriptionRefreshFinished());
    }

    public void notifyChannelsFound(boolean hasSubscriptions) {
        this.subscriptionListeners.forEach(listener -> listener.onChannelsFound(hasSubscriptions));
    }
}
