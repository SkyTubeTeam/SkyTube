package free.rm.skytube.businessobjects.db;

public class SubscriptionsVideosTable {
	public static final String TABLE_NAME = "SubsVideos";
	public static final String COL_CHANNEL_ID = "Channel_Id";
	public static final String COL_YOUTUBE_VIDEO_ID = "YouTube_Video_Id";
	public static final String COL_YOUTUBE_VIDEO = "YouTube_Video";
	public static final String COL_YOUTUBE_VIDEO_DATE = "YouTube_Video_Date";

	public static String getCreateStatement() {
		return "CREATE TABLE " + TABLE_NAME + " (" +
						COL_YOUTUBE_VIDEO_ID + " TEXT PRIMARY KEY NOT NULL, " +
						COL_CHANNEL_ID + " TEXT NOT NULL, " +
						COL_YOUTUBE_VIDEO + " BLOB, " +
						COL_YOUTUBE_VIDEO_DATE + " TIMESTAMP DEFAULT (strftime('%s', 'now')) " +
						" )";
	}
}
