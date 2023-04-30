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

package free.rm.skytube.businessobjects.YouTube;

import androidx.core.util.Pair;

import java.util.ArrayList;
import java.util.List;

import free.rm.skytube.app.Utils;
import free.rm.skytube.businessobjects.YouTube.POJOs.CardData;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;
import free.rm.skytube.businessobjects.db.BookmarksDb;


/**
 * Get user's bookmarked videos.
 */
public class GetBookmarksVideos extends GetYouTubeVideos {
    private Integer minOrder;

    @Override
    public void init() {
        reset();
    }

    @Override
    public void reset() {
        minOrder = null;
        noMoreVideoPages = false;
    }

    @Override
    public List<CardData> getNextVideos() {
        if (!noMoreVideoPages()) {
            BookmarksDb db = BookmarksDb.getBookmarksDb();
            Pair<List<YouTubeVideo>, Integer> bookmarkedVideos = db.getBookmarkedVideos(20, minOrder);
            if (!bookmarkedVideos.first.isEmpty()) {
                minOrder = Utils.min(bookmarkedVideos.second, minOrder);
                noMoreVideoPages = bookmarkedVideos.first.isEmpty();
            } else {
                noMoreVideoPages = true;
            }
            return new ArrayList<>(bookmarkedVideos.first);
        }

        return null;
    }

}
