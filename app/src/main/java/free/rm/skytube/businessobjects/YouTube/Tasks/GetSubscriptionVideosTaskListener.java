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


/**
 * A listener that will be notified when a new YouTube video is published to a channel that the user
 * is subscribed to.
 */
public interface GetSubscriptionVideosTaskListener {
	void onChannelVideosFetched(String channelId, int videosFetched, boolean videosDeleted);
	void onAllChannelVideosFetched(boolean changed);
}
