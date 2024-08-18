/*
 * SkyTube
 * Copyright (C) 2024  Zsombor Gegesy
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

import java.util.Objects;

import free.rm.skytube.businessobjects.YouTube.newpipe.ChannelId;
import free.rm.skytube.businessobjects.model.Status;

public final class PersistentChannel {
    final YouTubeChannel channel;
    final long channelPk;
    final Long subscriptionPk;
    final Status status;

    public PersistentChannel(YouTubeChannel channel, long channelPk, Long subscriptionPk, Status status) {
        this.channel = Objects.requireNonNull(channel, "channel");
        this.channelPk = channelPk;
        this.subscriptionPk = subscriptionPk;
        this.status = status;
    }

    public YouTubeChannel channel() {
        return channel;
    }

    public Status status() {
        return status;
    }

    public ChannelId getChannelId() {
        return channel.getChannelId();
    }

    public long channelPk() {
        return channelPk;
    }

    public Long subscriptionPk() {
        return subscriptionPk;
    }

    public boolean isSubscribed() {
        return subscriptionPk != null;
    }

    public PersistentChannel with(YouTubeChannel newInstance) {
        newInstance.setUserSubscribed(isSubscribed());
        return new PersistentChannel(newInstance, channelPk, subscriptionPk, status);
    }

    public PersistentChannel withSubscriptionPk(Long newSubscriptionPk) {
        return new PersistentChannel(channel, channelPk, newSubscriptionPk, status);
    }
}
