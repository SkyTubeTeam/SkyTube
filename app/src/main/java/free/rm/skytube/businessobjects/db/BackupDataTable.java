package free.rm.skytube.businessobjects.db;

public class BackupDataTable {

    public static final String TABLE_NAME = "Backup";
    public static final String COL_BACKUP_ID = "backup_id";
    public static final String COL_YOUTUBE_API_KEY = "youtube_api_key";
    public static final String COL_DEFAULT_TAB_NAME = "default_tab_name";
    public static final String COL_SORT_CHANNELS = "sort_channels";
    public static final String COL_HIDE_TABS = "hide_tabs";
    public static final String COL_USE_NEWPIPE_BACKEND = "use_newpipe_backend";

    public static String getCreateStatement() {
        return "CREATE TABLE " + TABLE_NAME + " (" +
                COL_BACKUP_ID + " INTEGER , " +
                COL_YOUTUBE_API_KEY + " TEXT, " +
                COL_DEFAULT_TAB_NAME + " TEXT, " +
                COL_SORT_CHANNELS + " TEXT, " +
                COL_HIDE_TABS + " TEXT, " +
                COL_USE_NEWPIPE_BACKEND + " TEXT " +
                " )";
    }
}
