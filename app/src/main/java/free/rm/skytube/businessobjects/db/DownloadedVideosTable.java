package free.rm.skytube.businessobjects.db;

/**
 * Downloaded Videos Table
 */
public class DownloadedVideosTable {
	public static final String TABLE_NAME = "DownloadedVideos";
	public static final String COL_YOUTUBE_VIDEO_ID = "YouTube_Video_Id";
	public static final String COL_YOUTUBE_VIDEO = "YouTube_Video";
	public static final String COL_FILE_URI = "File_URI";
	public static final String COL_AUDIO_FILE_URI = "Audio_URI";
	public static final String COL_ORDER = "Order_Index";

	static final String MAXIMUM_ORDER_QUERY = String.format("SELECT MAX(%s) FROM %s", COL_ORDER, TABLE_NAME);
	static final String COUNT_ALL = String.format("SELECT COUNT(*) FROM %s", TABLE_NAME);

	static final String PAGED_QUERY = String.format("SELECT %1$s,%2$s FROM %3$s WHERE %2$s > ? ORDER BY %2$s DESC LIMIT ?", COL_YOUTUBE_VIDEO, COL_ORDER, TABLE_NAME);

	private static final String ADD_COLUMN = "ALTER TABLE " + TABLE_NAME + " ADD COLUMN ";

	public static String getCreateStatement() {
		return "CREATE TABLE " + TABLE_NAME + " (" +
						COL_YOUTUBE_VIDEO_ID + " TEXT PRIMARY KEY NOT NULL, " +
						COL_YOUTUBE_VIDEO + " BLOB, " +
						COL_FILE_URI + " TEXT, " +
						COL_AUDIO_FILE_URI + " TEXT, " +
						COL_ORDER + " INTEGER " +
						" )";
	}

	public static String getAddAudioUriColumn() {
		return ADD_COLUMN + COL_AUDIO_FILE_URI + " TEXT";
	}
}
