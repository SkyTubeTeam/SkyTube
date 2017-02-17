/*
 * SkyTube
 * Copyright (C) 2016  Ramon Mifsud
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

package free.rm.skytube.gui.businessobjects;

import android.content.Context;

import java.util.Collections;

import free.rm.skytube.businessobjects.db.BookmarksDb;

/**
 * Subclass of VideoGridAdapter that supports drag & drop reordering of the items in the grid.
 */
public class bookmarksGridAdapter extends VideoGridAdapter implements ItemTouchHelperAdapter {

	public bookmarksGridAdapter(Context context) {
		super(context);
	}

	@Override
	public boolean onItemMove(int fromPosition, int toPosition) {
		if (fromPosition < toPosition) {
			for (int i = fromPosition; i < toPosition; i++) {
				Collections.swap(list, i, i + 1);
			}
		} else {
			for (int i = fromPosition; i > toPosition; i--) {
				Collections.swap(list, i, i - 1);
			}
		}
		notifyItemMoved(fromPosition, toPosition);

		// Update the database since the order has changed
		BookmarksDb.getBookmarksDb().updateOrder(list);

		return true;
	}


}
