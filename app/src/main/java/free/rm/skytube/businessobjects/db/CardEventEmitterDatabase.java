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
package free.rm.skytube.businessobjects.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import free.rm.skytube.app.utils.WeakList;
import free.rm.skytube.businessobjects.YouTube.POJOs.CardData;
import free.rm.skytube.businessobjects.YouTube.newpipe.ContentId;
import free.rm.skytube.businessobjects.interfaces.CardListener;

abstract class CardEventEmitterDatabase extends SQLiteOpenHelperEx {

    private final WeakList<CardListener> listeners = new WeakList<>();

    CardEventEmitterDatabase(final Context context, final String name, final SQLiteDatabase.CursorFactory factory, final int version) {
        super(context, name, factory, version);
    }

    /**
     * Add a Listener that will be notified when a Card is added or removed from the database. This will
     * allow the Video Grid to be redrawn in order to remove the video from display.
     *
     * @param listener The Listener (which implements CardListener) to add.
     */
    public void registerListener(CardListener listener) {
        listeners.add(listener);
    }

    /**
     * Remove the Listener
     *
     * @param listener The Listener (which implements CardListener) to remove.
     */
    public void unregisterListener(CardListener listener) {
        listeners.remove(listener);
    }

    void notifyCardAdded(CardData cardData) {
        listeners.forEach(cardListener -> cardListener.onCardAdded(cardData));
    }

    void notifyCardDeleted(ContentId contentId) {
        listeners.forEach(cardListener -> cardListener.onCardDeleted(contentId));
    }
}
