package free.rm.skytube.businessobjects.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

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


    public long insertBackupData(String defaultTab, String hiddenTabs, String youtubeApiKey, String isNewPipePreferredBackend, String sortChannels) {
        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(BackupDataTable.COL_BACKUP_ID, 1);
        if (defaultTab != null){
            values.put(BackupDataTable.COL_DEFAULT_TAB, defaultTab);
        }
        if (youtubeApiKey != null){
            values.put(BackupDataTable.COL_YOUTUBE_API_KEY, youtubeApiKey);
        }
        if (isNewPipePreferredBackend != null){
            values.put(BackupDataTable.COL_PREFERRED_BACKEND, isNewPipePreferredBackend);
        }
        if (hiddenTabs != null){
            values.put(BackupDataTable.COL_HIDDEN_TABS, hiddenTabs);
        }
        if (sortChannels != null){
            values.put(BackupDataTable.COL_SORT_CHANNELS, sortChannels);
        }
        long isInsertSuccesfull = getWritableDatabase().insert(BackupDataTable.TABLE_NAME, null, values);
        getWritableDatabase().close();
        return isInsertSuccesfull;
    }

    public void getBackupData(){
        Cursor cursor = getWritableDatabase().query(
                BackupDataTable.TABLE_NAME,   // The table to query
                null,             // The array of columns to return (pass null to get all)
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
            String defaultTab = cursor.getString(cursor.getColumnIndexOrThrow(BackupDataTable.COL_DEFAULT_TAB));
            String sortChannels = cursor.getString(cursor.getColumnIndexOrThrow(BackupDataTable.COL_SORT_CHANNELS));
            String hiddenTabs = cursor.getString(cursor.getColumnIndexOrThrow(BackupDataTable.COL_HIDDEN_TABS));
            String preferredBackend = cursor.getString(cursor.getColumnIndexOrThrow(BackupDataTable.COL_PREFERRED_BACKEND));
            backupArray = new String[] {String.valueOf(backupId),youtubeApiKey,defaultTab,sortChannels,hiddenTabs,preferredBackend};
        }
        cursor.close();
        getWritableDatabase().close();
        System.out.println("Backup data " + Arrays.toString(backupArray));
        }
    }

