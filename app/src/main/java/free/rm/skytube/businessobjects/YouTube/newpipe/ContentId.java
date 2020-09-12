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

package free.rm.skytube.businessobjects.YouTube.newpipe;

import org.schabi.newpipe.extractor.StreamingService.LinkType;

import free.rm.skytube.app.Utils;

public class ContentId {
    final String id;
    final String canonicalUrl;
    final LinkType type;

    public ContentId(String id, String canonicalUrl, LinkType type) {
        Utils.requireNonNull(id, "id");
        Utils.requireNonNull(canonicalUrl, "canonicalUrl");
        Utils.requireNonNull(type, "type");
        if (type == LinkType.NONE) {
            throw new IllegalArgumentException("LinkType.NONE for id=" + id + ", url=" + canonicalUrl);
        }
        this.id = id;
        this.canonicalUrl = canonicalUrl;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public String getCanonicalUrl() {
        return canonicalUrl;
    }

    public LinkType getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContentId contentId = (ContentId) o;
        return id.equals(contentId.id) &&
                canonicalUrl.equals(contentId.canonicalUrl) &&
                type == contentId.type;
    }

    @Override
    public String toString() {
        return "ContentId{" +
                "id='" + id + '\'' +
                ", canonicalUrl='" + canonicalUrl + '\'' +
                ", type=" + type +
                '}';
    }

    @Override
    public int hashCode() {
        return Utils.hash(id, canonicalUrl, type);
    }
}
