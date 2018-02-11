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
import android.database.Cursor;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import free.rm.skytube.R;
import free.rm.skytube.businessobjects.db.SearchHistoryDb;
import free.rm.skytube.businessobjects.db.SearchHistoryTable;
import free.rm.skytube.businessobjects.interfaces.SearchHistoryClickListener;

/**
 * A SimpleCursorAdapter that will display search suggestions based on what the user has previously searched for.
 */
public class SearchHistoryCursorAdapter extends SimpleCursorAdapter {
	private Runnable onUpdate;
	private SearchHistoryClickListener searchHistoryClickListener;

	public SearchHistoryCursorAdapter(Context context, int layout, Cursor c,
																			 String[] from, int[] to, int flags) {
		super(context, layout, c, from, to, flags);
	}

	public void setSearchHistoryClickListener(SearchHistoryClickListener searchHistoryClickListener) {
		this.searchHistoryClickListener = searchHistoryClickListener;
	}

	public void setOnUpdate(Runnable onUpdate) {
		this.onUpdate = onUpdate;
	}

	@Override
	public CharSequence convertToString(Cursor cursor) {
		int indexColumnSuggestion = cursor.getColumnIndex(SearchHistoryTable.COL_SEARCH_TEXT);
		return cursor.getString(indexColumnSuggestion);
	}


	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		super.bindView(view, context, cursor);
		ImageButton deleteButton = view.findViewById(R.id.delete_button);
		final TextView textView = view.findViewById(android.R.id.text1);
		textView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(searchHistoryClickListener != null)
					searchHistoryClickListener.onClick(textView.getText().toString());
			}
		});
		deleteButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				SearchHistoryDb.getSearchHistoryDb().deleteSearchText(textView.getText().toString());
				if(onUpdate != null)
					onUpdate.run();
			}
		});
	}
}
