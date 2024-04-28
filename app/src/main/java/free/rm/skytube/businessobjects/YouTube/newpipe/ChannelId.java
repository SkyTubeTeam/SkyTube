/*
 * SkyTube
 * Copyright (C) 2023  Zsombor Gegesy
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

import java.util.Objects;

import free.rm.skytube.app.Utils;

public class ChannelId {
    private final String id;
    public ChannelId(String id) {
        this.id = Utils.removeChannelIdPrefix(Objects.requireNonNull(id, "rawId is null"));
    }

    public String getRawId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChannelId channelId = (ChannelId) o;
        return Objects.equals(id, channelId.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ChannelId{'" + id + '\'' + '}';
    }
}
