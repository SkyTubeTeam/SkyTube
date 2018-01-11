package free.rm.skytube.businessobjects.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;
import free.rm.skytube.businessobjects.interfaces.OrderableDatabase;

/**
 * Created by Okan Kaya on 6.12.2017.
 */

/**
 * A database (DB) that stores user's blocked channels.
 */

public class BlockedChannelsDb extends SQLiteOpenHelperEx {
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "blockedchannels.db";
    private static volatile BlockedChannelsDb blockedChannelsDb = null;
    private static boolean hasUpdated = false;
    private List<BlockedChannelsDbListener> listeners = new ArrayList<>();

    public BlockedChannelsDb(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);

    }

    public static synchronized BlockedChannelsDb getBlockedChannelsDb() {
        if (blockedChannelsDb == null) {
            blockedChannelsDb = new BlockedChannelsDb(SkyTubeApp.getContext());
        }

    return blockedChannelsDb;
    }

    @Override
    protected void clearDatabaseInstance() {

        blockedChannelsDb = null;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
    sqLiteDatabase.execSQL(BlockedChannelsTable.getCreateStatement());
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }

    /**
     * Add the specified channel to the list of blocked channels.
     *
     * @param video channel to add
     * @return True if the channel was successfully saved/blocked to the DB.
     */
    public boolean add(YouTubeVideo video) {

        Gson gson = new Gson();
        ContentValues contentValues = new ContentValues();

        contentValues.put(BlockedChannelsTable.COL_CHANNEL_ID, video.getChannelId());
     //   contentValues.put(BlockedChannelsTable.COL_YOUTUBE_CHANNEL_NAME, gson.toJson(video.getChannelName()));

        boolean addSuccesful = getWritableDatabase().insert(BlockedChannelsTable.TABLE_NAME, null, contentValues) != -1;
        onUpdated();

        Log.d("", "BLOCK CHANNEL add: number " + this.getBlockedChannels());
        return addSuccesful;
    }



    /**
     * Remove the specified channel from the list of blocked channels.
     *
     * @param video channel to remove.
     * @return True if the channel has been unblocked; false otherwise.
     */
    public boolean remove(YouTubeVideo video) {
        getWritableDatabase().delete(BlockedChannelsTable.TABLE_NAME,
                BlockedChannelsTable.COL_CHANNEL_ID + " = ?",
                new String[]{video.getChannelId()});

        int rowsDeleted = getWritableDatabase().delete(BlockedChannelsTable.TABLE_NAME,
                BlockedChannelsTable.COL_CHANNEL_ID + " = ?",
                new String[]{video.getChannelId()});

        return rowsDeleted >= 0;

    }

    private void onUpdated() {
        hasUpdated = true;

        for (BlockedChannelsDb.BlockedChannelsDbListener listener : listeners)
            listener.onBlockedChannelsDbUpdated();
    }

    public int getBlockedChannels() {
        String query = String.format("SELECT COUNT(*) FROM %s", BlockedChannelsTable.TABLE_NAME);
        Cursor cursor = BlockedChannelsDb.getBlockedChannelsDb().getReadableDatabase().rawQuery(query, null);
        int totalBlockedChannels = 0;

        if (cursor.moveToFirst()) {
            totalBlockedChannels = cursor.getInt(0);
        }

        cursor.close();
        return totalBlockedChannels;
    }

    public List<String> getBlockedChannelsList(){

        List<String> videos = new ArrayList<>();

        String query = "SELECT * FROM " + BlockedChannelsTable.TABLE_NAME;
        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
        Cursor cursor = sqLiteDatabase.rawQuery(query,null);
/*
        String query = "SELECT * FROM " + TABLE_IMAGES;
        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
        Cursor cursor = sqLiteDatabase.rawQuery(query, null);*/

     /*   Cursor	cursor = getReadableDatabase().query(
                BlockedChannelsTable.TABLE_NAME,
                new String[]{BlockedChannelsTable.COL_CHANNEL_ID},
                null,
                null, null, null, null);
*/
       // List<YouTubeVideo> videos = new ArrayList<>();

        if(cursor.moveToFirst()) {
            do {

                String channelId = cursor.getString(0);

                 /*byte[] blob = cursor.getBlob(cursor.getColumnIndex(BlockedChannelsTable.COL_ID));
                YouTubeVideo video = new Gson().fromJson(new String(blob), new TypeToken<YouTubeVideo>(){}.getType());
                */
                 videos.add(channelId);
            } while(cursor.moveToNext());
        }
        cursor.close();

        return videos;

    }

    public interface BlockedChannelsDbListener {
        /**
         * Will be called once the blocked channels DB is updated (by either a blocked channel insertion or
         * deletion).
         */
        void onBlockedChannelsDbUpdated();
    }
}
