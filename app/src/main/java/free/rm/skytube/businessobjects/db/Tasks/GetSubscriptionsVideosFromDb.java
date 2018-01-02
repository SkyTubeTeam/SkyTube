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
import java.util.List;

import free.rm.skytube.businessobjects.YouTube.GetYouTubeVideos;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;
import free.rm.skytube.businessobjects.db.SubscriptionsDb;


/**
 * Get user's subscriptions video feed from the user's local database.
 */
public class GetSubscriptionsVideosFromDb extends GetYouTubeVideos {

	@Override
	public void init() throws IOException {
		noMoreVideoPages = false;
	}


	@Override
	public List<YouTubeVideo> getNextVideos() {
		if (!noMoreVideoPages()) {
			noMoreVideoPages = true;
			return SubscriptionsDb.getSubscriptionsDb().getSubscriptionVideos();
		}

		return null;
	}


	@Override
	public boolean noMoreVideoPages() {
		return noMoreVideoPages;
	}

}
