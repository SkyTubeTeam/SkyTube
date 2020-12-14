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

package free.rm.skytube.gui.businessobjects;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.List;
import java.util.Set;

import free.rm.skytube.R;
import free.rm.skytube.databinding.SubsYoutubeImportDialogListBinding;
import free.rm.skytube.gui.businessobjects.adapters.MultiSelectListPreferenceAdapter;

/**
 * A dialog builder that displays a lists items, allows to user to select multiple items and can
 * select/deselect all items.
 */
public class MultiSelectListPreferenceDialog extends SkyTubeMaterialDialog {

	private MultiSelectListPreferenceAdapter listAdapter;


	public MultiSelectListPreferenceDialog(@NonNull Context context) {
		super(context);

		// set the custom view to be placed inside this dialog
		customView(R.layout.subs_youtube_import_dialog_list, false);
	}


	public MultiSelectListPreferenceDialog(@NonNull Context context, List<MultiSelectListPreferenceItem> items) {
		this(context);
		setItems(items);
	}


	/**
	 * Set items to be displayed in this dialog.
	 *
	 * @param items A list of items to be displayed.
	 */
	public void setItems(List<MultiSelectListPreferenceItem> items) {
		listAdapter = new MultiSelectListPreferenceAdapter(items);
	}


	/**
	 * Add an item to this adapter.  Will fail if the item has already been added.
	 *
	 * @param item  Item to add.
	 *
	 * @return True if successful; false if the item is already stored in this adapter.
	 */
	public boolean addItem(MultiSelectListPreferenceItem item) {
		return listAdapter.addItem(item);
	}


	@Override
	public MaterialDialog build() {
		MaterialDialog materialDialog = super.build();
		SubsYoutubeImportDialogListBinding binding = SubsYoutubeImportDialogListBinding
				.bind(materialDialog.getCustomView());

		binding.channelList.setAdapter(listAdapter);
		binding.channelList.setLayoutManager(new LinearLayoutManager(materialDialog.getContext()));
		binding.selectAllButton.setOnClickListener(view -> listAdapter.selectAll());
		binding.selectNoneButton.setOnClickListener(view -> listAdapter.selectNone());

		return materialDialog;
	}


	/**
	 * @return A set of items that are selected/checked by the user.
	 */
	public List<MultiSelectListPreferenceItem> getSelectedItems() {
		return listAdapter.getSelectedItems();
	}


	/**
	 * @return A set of items that are selected/checked by the user.
	 */
	public Set<String> getSelectedItemsIds() {
		return listAdapter.getSelectedItemsIds();
	}

}
