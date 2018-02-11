package free.rm.skytube.businessobjects.db;

public class BlockedChannelsTable {
    public static final String TABLE_NAME = "BlockedChannels";
    public static final String COL_CHANNEL_ID = "_Id";
    public static final String COL_YOUTUBE_CHANNEL_NAME = "Youtube_Channel_Name";
    public static final String COL_ID = "Id";

    public static String getCreateStatement() {
        return "CREATE TABLE " + TABLE_NAME + " (" +
                COL_CHANNEL_ID + " TEXT , " +
                COL_YOUTUBE_CHANNEL_NAME + " TEXT, " +
                COL_ID + " INTEGER PRIMARY KEY ASC " +
                ")";
    }
}
