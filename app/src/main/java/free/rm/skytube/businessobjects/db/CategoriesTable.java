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

/**
 * A table that stores the categories.
 */
public class CategoriesTable {
    public static final String TABLE_NAME = "Categories";
    public static final String COL_ID = "id";
    public static final String COL_BUILTIN = "builtin";
    public static final String COL_ENABLED = "enabled";
    public static final String COL_PRIORITY = "priority";
    public static final String COL_LABEL = "label";
    public static final String COL_ICON = "icon";

    static final String[] ALL_COLUMNS_FOR_EXTRACT = new String[] {
            COL_ID,
            COL_BUILTIN,
            COL_ENABLED,
            COL_PRIORITY,
            COL_LABEL,
            COL_ICON
    };

    static final String MAXIMUM_PRIORITY_QUERY = String.format("SELECT MAX(%s) FROM %s", COL_PRIORITY, TABLE_NAME);

    static final String COUNT_BY_LABEL_QUERY = String.format("SELECT count(%s) FROM %s WHERE %s = ?", COL_ID, TABLE_NAME, COL_LABEL);

    public static String getCreateStatement() {
        return "CREATE TABLE " + TABLE_NAME + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                COL_LABEL + " TEXT NOT NULL, " +
                COL_ICON + " TEXT, " +
                COL_BUILTIN + " INTEGER, " +
                COL_ENABLED + " INTEGER, " +
                COL_PRIORITY + " INTEGER" +
                " )";
    }

}
