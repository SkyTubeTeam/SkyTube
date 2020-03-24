package free.rm.skytube.businessobjects.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.google.gson.Gson;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import free.rm.skytube.app.SkyTubeApp;

public class BackupDataDb extends SQLiteOpenHelperEx {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "backup.db";
    private static volatile BackupDataDb backupDataDb = null;

    private BackupDataDb(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static synchronized BackupDataDb getBackupDataDbDb() {
        if (backupDataDb == null) {
            backupDataDb = new BackupDataDb(SkyTubeApp.getContext());
        }

        return backupDataDb;
    }

    @Override
    protected void clearDatabaseInstance() {

    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(BackupDataTable.getCreateStatement());
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }


    public long insertBackupData(String youtubeApiKey, String defaultTab, String sortChannelsAlphabetically) {
        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(BackupDataTable.COL_BACKUP_ID, 1);
        if (youtubeApiKey != null){
            values.put(BackupDataTable.COL_YOUTUBE_API_KEY, youtubeApiKey);
        }
        if (defaultTab != null){
            values.put(BackupDataTable.COL_DEFAULT_TAB, defaultTab);
        }
        if (defaultTab != null){
            values.put(BackupDataTable.COL_DEFAULT_TAB, defaultTab);
        }

        long isInsertSuccesfull = getWritableDatabase().insert(BackupDataTable.TABLE_NAME, null, values);
        getWritableDatabase().close();
        return isInsertSuccesfull;
    }

    public void getBackupData(){
        Cursor cursor = getWritableDatabase().query(
                BackupDataTable.TABLE_NAME,   // The table to query
                new String[]{BackupDataTable.COL_BACKUP_ID,BackupDataTable.COL_YOUTUBE_API_KEY},             // The array of columns to return (pass null to get all)
                null,              // The columns for the WHERE clause
                null,          // The values for the WHERE clause
                null,                   // don't group the rows
                null,                   // don't filter by row groups
                null               // The sort order
        );

        String[] backupArray = new String[1];
        while(cursor.moveToNext()) {
            long backupId = cursor.getLong(cursor.getColumnIndexOrThrow(BackupDataTable.COL_BACKUP_ID));
            String youtubeApiKey = cursor.getString(cursor.getColumnIndexOrThrow(BackupDataTable.COL_YOUTUBE_API_KEY));
            backupArray = new String[] {String.valueOf(backupId),youtubeApiKey};
        }
        cursor.close();
        getWritableDatabase().close();
        System.out.println("Backup data " + Arrays.toString(backupArray));
        }
    }

