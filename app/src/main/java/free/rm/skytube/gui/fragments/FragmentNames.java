/*
 * SkyTube
 * Copyright (C) 2020  Zsombor Gegesy
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

package free.rm.skytube.gui.fragments;

import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;

import static free.rm.skytube.gui.fragments.MainFragment.*;

public class FragmentNames {

    private final SkyTubeApp app;

    public FragmentNames(SkyTubeApp app) {
        this.app = app;
    }

    public String getName(String key) {
        if (MOST_POPULAR_VIDEOS_FRAGMENT.equals(key)) {
            return app.getString(R.string.trending_in, app.getAppSettings().getPreferredContentCountry());
        }
        if (FEATURED_VIDEOS_FRAGMENT.equals(key)) {
            return app.getString(R.string.featured);
        }
        if (SUBSCRIPTIONS_FEED_FRAGMENT.equals(key)) {
            return app.getString(R.string.feed);
        }
        if (BOOKMARKS_FRAGMENT.equals(key)) {
            return app.getString(R.string.bookmarks);
        }
        if (DOWNLOADED_VIDEOS_FRAGMENT.equals(key)) {
            return app.getString(R.string.downloads);
        }
        return null;
    }

    public String[] getAllNames(String[] tabListValues) {
        String[] result = new String[tabListValues.length];
        for (int i = 0; i < tabListValues.length; i++) {
            result[i] = getName(tabListValues[i]);
        }
        return result;
    }
}
