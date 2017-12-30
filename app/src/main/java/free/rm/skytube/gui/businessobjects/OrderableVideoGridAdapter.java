package free.rm.skytube.gui.businessobjects;

import android.content.Context;

import java.util.Collections;

import free.rm.skytube.businessobjects.interfaces.OrderableDatabase;

/**
 * Subclass of VideoGridAdapter that supports drag & drop reordering of the items in the grid.
 */
public class OrderableVideoGridAdapter extends VideoGridAdapter implements ItemTouchHelperAdapter {
	private OrderableDatabase database = null;

	public OrderableVideoGridAdapter(Context context, OrderableDatabase database) {
		super(context);
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
