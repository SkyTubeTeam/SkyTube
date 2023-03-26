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

import org.schabi.newpipe.extractor.StreamingService;

public final class VideoId extends ContentId {
    final Integer timestamp;

    public VideoId(String id, String canonicalUrl, Integer timestamp) {
        super(id, canonicalUrl, StreamingService.LinkType.STREAM);
        this.timestamp = timestamp;
    }

    public Integer getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "VideoId{" +
                "id='" + id + '\'' +
                ", canonicalUrl='" + canonicalUrl + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }

    public static VideoId create(String id) {
        return new VideoId(id, String.format("https://youtu.be/%s", id), null);
    }
}
