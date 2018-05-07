package free.rm.skytube.businessobjects.db;

public class ChannelBlacklistTable {
	
	public static final String TABLE_NAME = "Blacklist";
	public static final String COL_ID  = "_id";
	public static final String COL_CHANNEL_ID = "Channel_Id";
	public static final String COL_CHANNEL_NAME = "Youtube_Channel_Name";

	public static String getCreateStatement() {
		return "CREATE TABLE " + TABLE_NAME + " (" +
				COL_ID + " INTEGER PRIMARY KEY ASC, " +
				COL_CHANNEL_ID + " TEXT, " +
				COL_CHANNEL_NAME + " TEXT NOT NULL " +
				")";
	}

}
