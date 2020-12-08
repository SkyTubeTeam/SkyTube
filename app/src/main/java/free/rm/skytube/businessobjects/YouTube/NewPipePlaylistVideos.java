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

package free.rm.skytube.businessobjects.YouTube;

import org.schabi.newpipe.extractor.stream.StreamInfoItem;

import java.util.Objects;

import free.rm.skytube.businessobjects.YouTube.newpipe.NewPipeException;
import free.rm.skytube.businessobjects.YouTube.newpipe.NewPipeService;
import free.rm.skytube.businessobjects.YouTube.newpipe.VideoPager;

public class NewPipePlaylistVideos extends NewPipeVideos<StreamInfoItem>  {

    private String playlistId;

    // Important, this is called from the channel tab
    @Override
    public void setQuery(String query) {
        this.playlistId = Objects.requireNonNull(query, "query missing");
    }

    @Override
    protected VideoPager createNewPager() throws NewPipeException {
        return NewPipeService.get().getPlaylistPager(Objects.requireNonNull(playlistId, "playlistId missing"));
    }
}
