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
package free.rm.skytube.businessobjects.YouTube.POJOs;

import free.rm.skytube.businessobjects.YouTube.newpipe.ChannelId;

public class ChannelView {
    private final ChannelId id;
    private final String title;
    private final String thumbnailUrl;
    private boolean newVideosSinceLastVisit;

    public ChannelView(ChannelId id, String title, String thumbnailUrl, boolean newVideosSinceLastVisit) {
        this.id = id;
        this.title = title;
        this.thumbnailUrl = thumbnailUrl;
        this.newVideosSinceLastVisit = newVideosSinceLastVisit;
    }

    public ChannelId getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public boolean isNewVideosSinceLastVisit() {
        return newVideosSinceLastVisit;
    }

    public void setNewVideosSinceLastVisit(boolean newVideosSinceLastVisit) {
        this.newVideosSinceLastVisit = newVideosSinceLastVisit;
    }
}
