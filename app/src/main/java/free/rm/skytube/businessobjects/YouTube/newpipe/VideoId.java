/*
 * SkyTube
 * Copyright (C) 2019  Zsombor Gegesy
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

package free.rm.skytube.businessobjects.YouTube.newpipe;

import free.rm.skytube.app.Utils;

public final class VideoId {
    private final String id;
    private final String canonicalUrl;

    public VideoId(String id, String canonicalUrl) {
        this.id = id;
        this.canonicalUrl = canonicalUrl;
    }

    public String getId() {
        return id;
    }

    public String getCanonicalUrl() {
        return canonicalUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VideoId videoId = (VideoId) o;
        return Utils.equals(id, videoId.id) &&
                Utils.equals(canonicalUrl, videoId.canonicalUrl);
    }

    @Override
    public int hashCode() {
        return Utils.hash(id, canonicalUrl);
    }
}
