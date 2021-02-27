/*
 * SkyTube
 * Copyright (C) 2021  Zsombor Gegesy
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
package free.rm.skytube.businessobjects.interfaces;

import free.rm.skytube.businessobjects.YouTube.POJOs.CardData;
import free.rm.skytube.businessobjects.YouTube.newpipe.ContentId;

public interface CardListener {
    /**
     * Will be called once the DB is updated - by a video insertion.
     */
    void onCardAdded(CardData card);

    /**
     * Will be called once the DB is updated - by a video deletion.
     */
    void onCardDeleted(ContentId card);

}
