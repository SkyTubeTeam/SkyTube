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
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.List;
import java.util.Set;

import free.rm.skytube.R;
import free.rm.skytube.gui.businessobjects.adapters.MultiSelectListPreferenceAdapter;

/**
 * A dialog builder that displays a lists items, allows to user to select multiple items and can
 * select/deselect all items.
 */
public class MultiSelectListPreferenceDialog extends SkyTubeMaterialDialog {

	private MultiSelectListPreferenceAdapter listAdapter;


	public MultiSelectListPreferenceDialog(@NonNull Context context, List<MultiSelectListPreferenceItem> items) {
		super(context);
		listAdapter = new MultiSelectListPreferenceAdapter(items);
		customView(R.layout.subs_youtube_import_dialog_list, false);
	}


	@Override
	public MaterialDialog build() {
		MaterialDialog materialDialog = super.build();

		RecyclerView list = materialDialog.getCustomView().findViewById(R.id.channel_list);
		list.setAdapter(listAdapter);
		list.setLayoutManager(new LinearLayoutManager(materialDialog.getContext()));

		Button selectAllButton = materialDialog.getCustomView().findViewById(R.id.select_all_button);
		selectAllButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				listAdapter.selectAll();
			}
		});
		Button selectNoneButton = materialDialog.getCustomView().findViewById(R.id.select_none_button);
		selectNoneButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				listAdapter.selectNone();
			}
		});

		return materialDialog;
	}


	/**
	 * @return A set of items that are selected/checked by the user.
	 */
	public Set<String> getSelectedItemsIds() {
		return listAdapter.getSelectedItemsIds();
	}

}
