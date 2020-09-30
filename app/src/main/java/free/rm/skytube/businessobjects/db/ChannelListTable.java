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

package free.rm.skytube.businessobjects.db;

/**
 * The enumeration of the blacklist and whitelist.
 */
public enum ChannelListTable {
	BLACKLIST("Blacklist"), WHITELIST("Whitelist");

	public static final String COL_ID  = "_id";
	public static final String COL_CHANNEL_ID = "Channel_Id";
	public static final String COL_CHANNEL_NAME = "Channel_Name";

	private final String tableName;
	ChannelListTable(String tableName) {
		this.tableName = tableName;
	}

	/**
	 * Returns the create table statement.
	 *
	 * @param tableName Table name.
	 *
	 * @return The create table statement.
	 */
	public static String getCreateStatement(final String tableName) {
		return "CREATE TABLE " + tableName + " (" +
				COL_ID + " INTEGER PRIMARY KEY ASC, " +
				COL_CHANNEL_ID + " TEXT UNIQUE NOT NULL, " +
				COL_CHANNEL_NAME + " TEXT NOT NULL " +
				")";
	}

	public String getCreateStatement() {
		return getCreateStatement(getTableName());
	}


	/**
	 * @return The table name.
	 */
	public String getTableName() {
		return tableName;
	}

}
