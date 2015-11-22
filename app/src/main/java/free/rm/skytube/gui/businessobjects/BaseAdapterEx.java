/*
 * SkyTube
 * Copyright (C) 2015  Ramon Mifsud
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
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
import android.view.LayoutInflater;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * An extended class of {@link BaseAdapter} that accepts a context and a list of items.
 *
 * <p>Similar to {@link ArrayAdapter}, however this class is simpler to setup and does
 * not perform stuff behind your back (e.g. ArrayAdapter tends to fill in the first TextView for
 * you when getView() is called -- this might not be always convenient and might be seen as a waste
 * of processing power.</p>
 */
public abstract class BaseAdapterEx<T> extends BaseAdapter {

	private Context context;
	private LayoutInflater inflater;
	private List<T> list;

	public BaseAdapterEx(Context context) {
		this(context, new ArrayList<T>());
	}

	public BaseAdapterEx(Context context, List<T> list) {
		this.context  = context;
		this.inflater = LayoutInflater.from(context);
		this.list     = list;
	}

	protected Context getContext() {
		return context;
	}

	/**
	 * @return An instance of {@link LayoutInflater}.
	 */
	protected LayoutInflater getLayoutInflater() {
		return inflater;
	}


	/**
	 * Append the given items to the Adapter's list.
	 *
	 * @param l The items to append.
	 */
	public void appendList(List<T> l) {
		if (l != null) {
			this.list.addAll(l);
			this.notifyDataSetChanged();
		}
	}


	/**
	 * Clear all items that are in the list.
	 */
	public void clearList() {
		this.list.clear();
	}


	@Override
	public int getCount() {
		return list.size();
	}

	@Override
	public Object getItem(int position) {
		return list.get(position);
	}

	protected T get(int position) {
		return list.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

}
