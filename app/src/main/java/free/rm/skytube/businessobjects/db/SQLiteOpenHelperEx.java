/*
 * SkyTube
 * Copyright (C) 2017  Ramon Mifsud
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

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.File;

import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.Logger;

/**
 * An extended {@link SQLiteOpenHelper} with extra goodies.
 *
 * <p>Class assumes that sub-classes shall make use of the singleton design pattern.</p>
 */
public abstract class SQLiteOpenHelperEx extends SQLiteOpenHelper {

	public SQLiteOpenHelperEx(Context context, String name, android.database.sqlite.SQLiteDatabase.CursorFactory factory, int version) {
		super(context, name, factory, version);
	}


//	/**
//	 * Closes the database and clears any singleton instances.
//	 */
//	@Override
//	public synchronized void close() {
//		super.close();
//		clearDatabaseInstance();
//	}


	/**
	 * @return The database (full) path.
	 */
	public String getDatabasePath() {
		return SkyTubeApp.getContext().getDatabasePath(getDatabaseName()).getPath();
	}


	/**
	 * @return The database directory (as a {@link File}).
	 */
	public File getDatabaseDirectory() {
		return SkyTubeApp.getContext().getDatabasePath(getDatabaseName()).getParentFile();
	}

	/**
	 * Execute a <b>constant</b> query, and return the number in the first row, first column.
	 *
	 * @param query the query to execute
	 * @param selectionArgs the arguments for the query.
	 * @return a number.
	 */
	public Integer executeQueryForInteger(String query, String[] selectionArgs, Integer defaultValue) {
		return executeQueryForInteger(getReadableDatabase(), query, selectionArgs, defaultValue);
	}

	/**
	 * Execute a <b>constant</b> query, and return the number in the first row, first column.
	 *
	 * @param query the query to execute
	 * @return a number.
	 */
	public Integer executeQueryForInteger(String query, Integer defaultValue) {
		return executeQueryForInteger(getReadableDatabase(), query, null, defaultValue);
	}

	/**
	 * Execute a <b>constant</b> query, and return the number in the first row, first column.
	 *
	 * @param db the database to execute on.
	 * @param query the query to execute
	 * @param selectionArgs the arguments for the query.
	 * @return a number.
	 */
	public static Integer executeQueryForInteger(SQLiteDatabase db, String query, String[] selectionArgs, Integer defaultValue) {
		try (Cursor cursor = db.rawQuery(query, selectionArgs)) {
			if (cursor.moveToFirst()) {
				return cursor.getInt(0);
			}
		}
		return defaultValue;
	}

	/**
	 * Execute a <b>constant</b> query, and return the number in the first row, first column.
	 *
	 * @param query the query to execute
	 * @return a number.
	 */
	public static Integer executeQueryForInteger(SQLiteDatabase db, String query, Integer defaultValue) {
		return executeQueryForInteger(db, query, null, defaultValue);
	}

    /**
     * Execute the given sql updates, one-by-one. Throws an exception if any of them fails.
     */
    public static void execSQLUpdates(SQLiteDatabase db, String[] sqlUpdates) {
        for (String sqlUpdate : sqlUpdates) {
            db.execSQL(sqlUpdate);
        }
    }

    public static void continueOnError(SQLiteDatabase db, String update) {
        try {
            db.execSQL(update);
        } catch (SQLiteException e) {
            Logger.e(db, e,"Unable to execute %s , because: %s", update, e.getMessage());
        }
    }

    public static void addColumn(SQLiteDatabase db, String tableName, Column column) {
        try {
            db.execSQL("ALTER TABLE " + tableName + " ADD COLUMN " + column.format());
        } catch (SQLiteException e) {
            Logger.e(db, e,"Unable to add '%s'  '%s' to table: '%s', because: %s", column.name, column.type, tableName, e.getMessage());
        }
    }

    public static void createIndex(SQLiteDatabase db, String indexName, String tableName, Column... columns) {
        db.execSQL("CREATE INDEX "+indexName+" ON "+tableName + "("+listColumns(true, columns)+")");
    }

    public static void createTable(SQLiteDatabase db, String tableName, Column... columns) {
        try {
            db.execSQL("CREATE TABLE " + tableName + " (" + listColumns(false, columns) + ")");
        } catch (SQLiteException e) {
            Logger.e(db, e,"Unable to create table: '%s', because: %s", tableName, e.getMessage());
            throw e;
        }
    }

    private static String listColumns(boolean justNames, final Column[] columns) {
        boolean first = true;
        final StringBuilder sql = new StringBuilder();
        for (Column col : columns) {
            if (first) {
                first = false;
            } else {
                sql.append(", ");
            }
            if (justNames) {
                sql.append(col.name);
            } else {
                sql.append(col.format());
            }
        }
        return sql.toString();
    }

}