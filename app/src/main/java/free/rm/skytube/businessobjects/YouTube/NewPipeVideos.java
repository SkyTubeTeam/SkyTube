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

import java.util.Collections;
import java.util.List;

import free.rm.skytube.businessobjects.Logger;
import free.rm.skytube.businessobjects.YouTube.POJOs.CardData;
import free.rm.skytube.businessobjects.YouTube.newpipe.NewPipeException;
import free.rm.skytube.businessobjects.YouTube.newpipe.VideoPager;

/**
 * Base class to adapt the UI to the NewPipe based paging.
 *
 */
public abstract class NewPipeVideos extends GetYouTubeVideos {

    private VideoPager pager;

    protected abstract VideoPager createNewPager() throws NewPipeException;

    @Override
    public void init() {
    }

    @Override
    public List<CardData> getNextVideos() {
        if (pager == null) {
            try {
                pager = createNewPager();
            } catch (Exception e) {
                Logger.e(this, "An error has occurred while getting videos:" + e.getMessage(), e);
                setLastException(e);
                return Collections.emptyList();
            }
        }
        try {
            return pager.getNextPage();
        } catch (Exception e) {
            Logger.e(this, "An error has occurred while getting videos:" + e.getMessage(), e);
            setLastException(e);
            return Collections.emptyList();
        } finally {
            noMoreVideoPages = !pager.isHasNextPage();
        }
    }

    @Override
    public void reset() {
         noMoreVideoPages = false;
         pager = null;
    }

}
