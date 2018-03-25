package free.rm.skytube.businessobjects.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;

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
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {}

    /**
     * Add the specified channel to the list of blocked channels.
     *
     * @param video channel to add
     * @return True if the channel was successfully saved/blocked to the DB.
     */
    public boolean add(YouTubeVideo video) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(BlockedChannelsTable.COL_CHANNEL_ID, video.getChannelId());
        contentValues.put(BlockedChannelsTable.COL_YOUTUBE_CHANNEL_NAME, video.getChannelName());
      
        boolean addSuccessful = getWritableDatabase().insert(BlockedChannelsTable.TABLE_NAME, null, contentValues) != -1;
        onUpdated();

        return addSuccessful;
    }

    /**
     * Remove the specified channel from the list of blocked channels.
     *
     * @param channelName channel to remove.
     * @return True if the channel has been unblocked; false otherwise.
     */
    public boolean remove(String channelName) {
        int rowsDeleted = getWritableDatabase().delete(BlockedChannelsTable.TABLE_NAME,
                BlockedChannelsTable.COL_YOUTUBE_CHANNEL_NAME + " = ?",
                new String[]{String.valueOf(channelName)});

        return rowsDeleted >= 0;
    }

    private void onUpdated() {
        hasUpdated = true;

        for (BlockedChannelsDb.BlockedChannelsDbListener listener : listeners)
            listener.onBlockedChannelsDbUpdated();
    }

    /**
     * Method to get number of blocked channels.
     * @return number of blocked channels.
     */
    public int getNumberOfBlockedChannels() {
        String query = String.format("SELECT COUNT(*) FROM %s", BlockedChannelsTable.TABLE_NAME);
        Cursor cursor = BlockedChannelsDb.getBlockedChannelsDb().getReadableDatabase().rawQuery(query, null);
        int totalBlockedChannels = 0;
        if (cursor.moveToFirst()) {
            totalBlockedChannels = cursor.getInt(0);
        }
        cursor.close();
        return totalBlockedChannels;
    }

    /**
     * Method for getting blocked channels' IDs as list.
     * @return list of blocked channel IDs.
     */
    public List<String> getBlockedChannelsListId() {
        List<String> videos = new ArrayList<>();
        String youtubeChannelId = "_Id";
        String query = "SELECT "+ youtubeChannelId +" FROM " + BlockedChannelsTable.TABLE_NAME;
        SQLiteDatabase sqLiteDatabase = getReadableDatabase();
        Cursor cursor = sqLiteDatabase.rawQuery(query, null);
        if (cursor.moveToFirst()) {
            do {
                String channelId = cursor.getString(0);
                videos.add(channelId);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return videos;
    }

    /**
     * Method for getting blocked channels' names as list.
     * @return list of blocked channel names.
     */
    public List<String> getBlockedChannelsListName() {
        List<String> videos = new ArrayList<>();
        String youtubeChannelName = "Youtube_Channel_Name";
        String query = "SELECT "+youtubeChannelName+" FROM " + BlockedChannelsTable.TABLE_NAME;
        SQLiteDatabase sqLiteDatabase = getReadableDatabase();
        Cursor cursor = sqLiteDatabase.rawQuery(query, null);
        if (cursor.moveToFirst()) {
            do {
                String channelId = cursor.getString(0);
                videos.add(channelId);
            } while (cursor.moveToNext());
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