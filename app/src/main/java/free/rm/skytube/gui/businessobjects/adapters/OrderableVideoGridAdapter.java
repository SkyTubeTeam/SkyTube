/*
 * SkyTube
 * Copyright (C) 2018  Ramon Mifsud
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

package free.rm.skytube.gui.businessobjects.adapters;

import java.util.Collections;

import free.rm.skytube.businessobjects.interfaces.OrderableDatabase;

/**
 * Subclass of VideoGridAdapter that supports drag & drop reordering of the items in the grid.
 */
public class OrderableVideoGridAdapter extends VideoGridAdapter implements ItemTouchHelperAdapter {
	private final OrderableDatabase database;

	public OrderableVideoGridAdapter(OrderableDatabase database) {
		this.database = database;
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

		if(database != null)
			database.updateOrder(list);

		return true;
	}
}
