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

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import free.rm.skytube.databinding.MultiSelectListDialogBinding;
import free.rm.skytube.gui.businessobjects.MultiSelectListPreferenceItem;

/**
 * Subclass of {@link RecyclerView.Adapter} to list the items to be displayed inside a
 * {@link free.rm.skytube.gui.businessobjects.MultiSelectListPreferenceDialog}.
 */
public class MultiSelectListPreferenceAdapter extends RecyclerView.Adapter<MultiSelectListPreferenceAdapter.ViewHolder> {

	private List<MultiSelectListPreferenceItem> items;


	public MultiSelectListPreferenceAdapter(List<MultiSelectListPreferenceItem> items) {
		this.items = items;
	}


	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		MultiSelectListDialogBinding binding = MultiSelectListDialogBinding.inflate(
				LayoutInflater.from(parent.getContext()));
		return new ViewHolder(binding);
	}


	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
		MultiSelectListPreferenceItem item = items.get(position);
		holder.binding.channelName.setText(item.text);
		holder.binding.checkBox.setChecked(item.isChecked);
		holder.binding.checkBox.setTag(item);
		holder.binding.checkBox.setOnClickListener(view -> {
			CheckBox cb = (CheckBox)view;
			MultiSelectListPreferenceItem ch = (MultiSelectListPreferenceItem)cb.getTag();
			ch.isChecked = cb.isChecked();
			items.get(position).isChecked = cb.isChecked();
		});
	}


	@Override
	public int getItemCount() {
		return items.size();
	}


	/**
	 * Add an item to this adapter.  Will fail if the item has already been added.
	 *
	 * @param item  Item to add.
	 *
	 * @return True if successful; false if the item is already stored in this adapter.
	 */
	public boolean addItem(MultiSelectListPreferenceItem item) {
		for (MultiSelectListPreferenceItem i : items) {
			if (i.id.equals(item.id))
				return false;
		}

		items.add(item);
		notifyDataSetChanged();
		return true;
	}


	/**
	 * @return A list of items that are selected/checked by the user.
	 */
	public List<MultiSelectListPreferenceItem> getSelectedItems() {
		List<MultiSelectListPreferenceItem> selectedItems = new ArrayList<>();

		for (MultiSelectListPreferenceItem item : items) {
			if (item.isChecked) {
				selectedItems.add(item);
			}
		}

		return selectedItems;
	}


	/**
	 * @return A set of IDs that are selected/checked by the user.
	 */
	public Set<String> getSelectedItemsIds() {
		Set<String> selectedItems = new HashSet<>();

		for (MultiSelectListPreferenceItem item : items) {
			if (item.isChecked) {
				selectedItems.add(item.id);
			}
		}

		return selectedItems;
	}


	/**
	 * Select/Check (the tickbox) for all items in this adapter.
	 */
	public void selectAll() {
		for(MultiSelectListPreferenceItem item : items) {
			item.isChecked = true;
		}
		notifyDataSetChanged();
	}


	/**
	 * Deselect/Uncheck (the tickbox) for all items in this adapter.
	 */
	public void selectNone() {
		for(MultiSelectListPreferenceItem item : items) {
			item.isChecked = false;
		}
		notifyDataSetChanged();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////

	static class ViewHolder extends RecyclerView.ViewHolder {
		MultiSelectListDialogBinding binding;

		ViewHolder(MultiSelectListDialogBinding binding) {
			super(binding.getRoot());
			this.binding = binding;
		}
	}
}
