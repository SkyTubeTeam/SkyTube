/*
 * SkyTube
 * Copyright (C) 2020  Zsombor Gegesy
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

package free.rm.skytube.businessobjects.db;

import android.content.ContentValues;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.StringRes;

import com.mikepenz.iconics.typeface.library.materialdesigniconic.MaterialDesignIconic;

import org.apache.commons.codec.binary.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.Logger;
import free.rm.skytube.businessobjects.YouTube.POJOs.Category;

public class CategoryManagement {
    private SQLiteDatabase db;

    private static class KeyLabel {
        @StringRes int key;
        @StringRes int label;
        MaterialDesignIconic.Icon icon;
        KeyLabel(@StringRes int key, @StringRes int label, MaterialDesignIconic.Icon icon) {
            this.key = key;
            this.label = label;
            this.icon = icon;
        }
    }

    private final static List<KeyLabel> CATEGORIES = Arrays.asList (
            new KeyLabel(R.string.category_games, R.string.category_games_label, MaterialDesignIconic.Icon.gmi_bike),
            new KeyLabel(R.string.category_music, R.string.category_music_label, MaterialDesignIconic.Icon.gmi_audio),
            new KeyLabel(R.string.category_news, R.string.category_news_label, MaterialDesignIconic.Icon.gmi_compass),
            new KeyLabel(R.string.category_tutorials, R.string.category_tutorials_label, MaterialDesignIconic.Icon.gmi_collection_bookmark),
            new KeyLabel(R.string.category_youtuber, R.string.category_youtuber_label, MaterialDesignIconic.Icon.gmi_youtube_play),
            new KeyLabel(R.string.category_for_kids, R.string.category_for_kids_label, MaterialDesignIconic.Icon.gmi_cake)
    );

    CategoryManagement(SQLiteDatabase db) {
        this.db = db;
    }

    public void setupDefaultCategories() {
        final Resources resources = SkyTubeApp.getContext().getResources();
        for (KeyLabel keyLabel: CATEGORIES) {
            addNew(resources.getString(keyLabel.key),  keyLabel.icon, true);
        }
    }

    public List<Category> getCategories() {
        try (final Cursor query = db.query(CategoriesTable.TABLE_NAME, CategoriesTable.ALL_COLUMNS_FOR_EXTRACT,
                null,
                null,
                null,
                null,
                CategoriesTable.COL_PRIORITY + " ASC")) {
            final int colId = query.getColumnIndexOrThrow(CategoriesTable.COL_ID);
            final int colLabel = query.getColumnIndexOrThrow(CategoriesTable.COL_LABEL);
            final int colBuiltin = query.getColumnIndexOrThrow(CategoriesTable.COL_BUILTIN);
            final int colEnabled = query.getColumnIndexOrThrow(CategoriesTable.COL_ENABLED);
            final int colPriority = query.getColumnIndexOrThrow(CategoriesTable.COL_PRIORITY);
            final int colIcon = query.getColumnIndexOrThrow(CategoriesTable.COL_ICON);
            List<Category> result = new ArrayList<>();
            while(query.moveToNext()) {
                boolean builtin = query.getInt(colBuiltin) != 0;
                String label = query.getString(colLabel);
                if (builtin) {
                    label = translate(label);
                }
                String icon = query.getString(colIcon);
                MaterialDesignIconic.Icon materialIcon = null;
                if (icon != null && !icon.trim().isEmpty()) {
                    materialIcon = MaterialDesignIconic.Icon.valueOf(icon);
                }
                result.add(new Category(
                        query.getLong(colId),
                        query.getInt(colEnabled) != 0,
                        builtin,
                        label,
                        materialIcon,
                        query.getInt(colPriority)));
            }
            return result;
        }
    }

    Category addNew(String name, MaterialDesignIconic.Icon icon, boolean builtin) {
        int count = SQLiteOpenHelperEx.executeQueryForInteger(db, CategoriesTable.COUNT_BY_LABEL_QUERY,  new String[] { name }, 0);
        if (count > 0) {
            return null;
        }
        final int priority = SQLiteOpenHelperEx.executeQueryForInteger(db, CategoriesTable.MAXIMUM_PRIORITY_QUERY, 0) + 1;

        ContentValues values = new ContentValues();
        values.put(CategoriesTable.COL_ENABLED, 1);
        values.put(CategoriesTable.COL_PRIORITY, priority);
        values.put(CategoriesTable.COL_BUILTIN, builtin ? 1 : 0);
        values.put(CategoriesTable.COL_LABEL, name);
        values.put(CategoriesTable.COL_ICON, icon.name());
        long id = db.insert(
                CategoriesTable.TABLE_NAME,
                null,
                values);
        Logger.i(this, "Adding new category: %s (%s) [priority: %s] -> %s", name, builtin, priority, id);
        return new Category(id, true, builtin, name, icon, priority);
    }

    private String translate(String label) {
        final Resources resources = SkyTubeApp.getContext().getResources();
        for (KeyLabel keyLabel: CATEGORIES) {
            if (resources.getString(keyLabel.key).equals(label)) {
                return resources.getString(keyLabel.label);
            }
        }
        return label;
    }

}
