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

import android.content.Context;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

/**
 * An extended class of {@link RecyclerView.Adapter} that accepts a context and a list of items.
 */
public abstract class RecyclerViewAdapterEx<T, HolderType extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<HolderType> {

	private Context context;
	protected final List<T> list = new ArrayList<>();

	public RecyclerViewAdapterEx() {
	}

	public RecyclerViewAdapterEx(Context context) {
		this.context  = context;
	}

	public Context getContext() {
		return context;
	}

	public void setContext(Context context) {
		this.context = context;
	}


	/**
	 * Clears the list and copy list l to the Adapter's list.
	 *
	 * @param l List to set
	 */
	public void setList(List<T> l) {
		clearList();
		appendList(l);
	}


	/**
	 * Append the given items to the Adapter's list.
	 *
	 * @param l The items to append.
	 */
	public void appendList(List<T> l) {
		if (l != null  && !l.isEmpty()) {
			list.addAll(l);
			notifyDataSetChanged();
		}
	}

	public void prepend(T item) {
		if (item != null) {
			this.list.add(0, item);
			this.notifyItemInserted(0);
		}
	}

	/**
	 * Append the given item to the Adapter's list.
	 *
	 * @param item The item to append.
	 */
	protected void append(T item) {
		if (item != null) {
			this.list.add(item);
			this.notifyDataSetChanged();
		}
	}


	/**
	 * Remove an item from the Adapter's list.
	 *
	 * @param itemPosition	Item's position/index to remove.
	 */
	protected void remove(int itemPosition) {
		if (itemPosition >= 0  &&  itemPosition < getItemCount()) {
			list.remove(itemPosition);
			this.notifyDataSetChanged();
		}
	}


	public void remove(Predicate<T> predicate) {
		for (int i=0;i<list.size();i++) {
			T item = list.get(i);
			if (predicate.test(item)) {
				list.remove(i);
				this.notifyItemRemoved(i);
				i--;
			}
		}
	}

	/**
	 * Clear all items that are in the list.
	 */
	public void clearList() {
		int listSize = getItemCount();

		this.list.clear();
		notifyItemRangeRemoved(0, listSize);
	}

    protected void notifyItemChanged(T item) {
        int idx = this.list.indexOf(item);
        if (idx >= 0) {
            this.notifyItemChanged(idx);
        }
    }

    /**
     * Replace the first matching item with the given item
     * @return the index of the new match, or -1 if none found 
     */
    protected int replace(Predicate<T> predicate, T newItem) {
        for (int i=0;i<list.size();i++) {
            T item = list.get(i);
            if (predicate.test(item)) {
                list.set(i, newItem);
                this.notifyItemChanged(i);
                return i;
            }
        }
        return -1;
    }

    public Iterator<T> getIterator() {
        return this.list.iterator();
    }

	@Override
	public int getItemCount() {
		return list.size();
	}


	protected T get(int position) {
		return list.get(position);
	}


}
