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

package free.rm.skytube.businessobjects.db.Tasks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import free.rm.skytube.businessobjects.YouTube.GetYouTubeVideos;
import free.rm.skytube.businessobjects.YouTube.POJOs.CardData;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;
import free.rm.skytube.businessobjects.db.SubscriptionsDb;


/**
 * Get user's subscriptions video feed from the user's local database.
 */
public class GetSubscriptionsVideosFromDb extends GetYouTubeVideos {

    private String lastVideoId;
    private long lastVideoPublishTimestamp;

	@Override
	public synchronized void init() throws IOException {
		noMoreVideoPages = false;
	}


	@Override
	public synchronized List<CardData> getNextVideos() {
		if (!noMoreVideoPages()) {
			List<YouTubeVideo> result = SubscriptionsDb.getSubscriptionsDb().getSubscriptionVideoPage(20, lastVideoId, lastVideoPublishTimestamp);
			if (result.isEmpty()) {
				noMoreVideoPages = true;
				lastVideoId = null;
			} else {
				YouTubeVideo last = result.get(result.size() -1);
				lastVideoId = last.getId();
				lastVideoPublishTimestamp = last.getPublishTimestamp();
			}

			return new ArrayList<>(result);
		}

		return null;
	}


	@Override
	public synchronized void reset() {
		super.reset();
		lastVideoId = null;
		lastVideoPublishTimestamp = System.currentTimeMillis();
	}
}
