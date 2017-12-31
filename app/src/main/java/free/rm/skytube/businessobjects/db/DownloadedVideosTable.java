package free.rm.skytube.businessobjects.db;

/**
 * Downloaded Videos Table
 */
public class DownloadedVideosTable {
	public static final String TABLE_NAME = "DownloadedVideos";
	public static final String COL_YOUTUBE_VIDEO_ID = "YouTube_Video_Id";
	public static final String COL_YOUTUBE_VIDEO = "YouTube_Video";
	public static final String COL_FILE_URI = "File_URI";
	public static final String COL_ORDER = "Order_Index";


	public static String getCreateStatement() {
		return "CREATE TABLE " + TABLE_NAME + " (" +
						COL_YOUTUBE_VIDEO_ID + " TEXT PRIMARY KEY NOT NULL, " +
						COL_YOUTUBE_VIDEO + " BLOB, " +
						COL_FILE_URI + " TEXT, " +
						COL_ORDER + " INTEGER " +
						" )";
	}

}
