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
import android.view.View;
import android.widget.Filter;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.cursoradapter.widget.SimpleCursorAdapter;

import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.Logger;
import free.rm.skytube.businessobjects.db.SearchHistoryDb;
import free.rm.skytube.businessobjects.db.SearchHistoryTable;
import free.rm.skytube.businessobjects.interfaces.SearchHistoryClickListener;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

/**
 * A SimpleCursorAdapter that will display search suggestions based on what the user has previously
 * searched for.
 */
public class SearchHistoryCursorAdapter extends SimpleCursorAdapter {

    /** The current string that the user typed in the search bar */
    private String searchBarString = "";
    private SearchHistoryClickListener searchHistoryClickListener;

    public SearchHistoryCursorAdapter(Context context, int layout, String[] from, int[] to, int flags) {
        super(context, layout, null, from, to, flags);
    }

    public void setSearchHistoryClickListener(SearchHistoryClickListener searchHistoryClickListener) {
        this.searchHistoryClickListener = searchHistoryClickListener;
    }

    /**
     * Sets the current search string.
     */
    public void setSearchBarString(String searchBarString) {
        this.searchBarString = searchBarString;
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
        textView.setOnClickListener(v -> {
            if(searchHistoryClickListener != null)
                searchHistoryClickListener.onClick(textView.getText().toString());
            SearchHistoryDb.getSearchHistoryDb()
                    .updateSearchTextTimestamp(textView.getText().toString())
                    .subscribe();
        });
        deleteButton.setOnClickListener(v -> {
            // delete the previous search from the search database, re-execute the query, and update the UI
            SearchHistoryDb.getSearchHistoryDb().deleteAndSearchAgain(textView.getText().toString(), searchBarString)
                .doOnSuccess( newCursor -> changeCursor(newCursor))
                    .subscribe();
        });
    }

    @Override
    public Cursor runQueryOnBackgroundThread(final CharSequence constraint) {
        return SearchHistoryDb.getSearchHistoryDb().executeSearch(constraint.toString());
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }


}
