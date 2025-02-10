/*
 * SkyTube
 * Copyright (C) 2016  Ramon Mifsud
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation (version 3 of the License).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package free.rm.skytube.businessobjects.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;

import com.github.skytube.components.utils.SQLiteHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.app.Utils;
import free.rm.skytube.businessobjects.JsonSerializer;
import free.rm.skytube.businessobjects.Logger;
import free.rm.skytube.businessobjects.YouTube.POJOs.CardData;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;
import free.rm.skytube.businessobjects.YouTube.newpipe.VideoId;
import free.rm.skytube.businessobjects.interfaces.OrderableDatabase;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * A database (DB) that stores user's bookmarked videos.
 */
public class BookmarksDb extends CardEventEmitterDatabase implements OrderableDatabase {
	private static volatile BookmarksDb bookmarksDb = null;

	private static final int DATABASE_VERSION = 1;
	private static final String DATABASE_NAME = "bookmarks.db";
    private final JsonSerializer jsonSerializer = new JsonSerializer();

	private BookmarksDb(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}


	public static synchronized BookmarksDb getBookmarksDb() {
		if (bookmarksDb == null) {
			bookmarksDb = new BookmarksDb(SkyTubeApp.getContext());
		}

		return bookmarksDb;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(BookmarksTable.getCreateStatement());
	}


	@Override
	public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
	}

	/**
	 * A task that checks if this video is bookmarked or not. If it is bookmarked, then it will hide
	 * the menu option to bookmark the video; otherwise it will hide the option to unbookmark the
	 * video.
	 */
	public Single<Boolean> isVideoBookmarked(@NonNull String videoId) {
		return Single.fromCallable(() -> isBookmarked(videoId))
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread());
	}


        /**
         * Add the specified video to the list of bookmarked videos. The video will appear at the
         * top of the list (when displayed in the grid, videos will be ordered by the Order
         * field, descending.
         *
         * @param video Video to add
         *
         * @return True if the video was successfully saved/bookmarked to the DB.
         */
    private DatabaseResult add(YouTubeVideo video) {
		ContentValues values = new ContentValues();
		values.put(BookmarksTable.COL_YOUTUBE_VIDEO_ID, video.getId());
		values.put(BookmarksTable.COL_YOUTUBE_VIDEO, jsonSerializer.toPersistedVideoJson(video).getBytes());

		int order = getMaximumOrderNumber();
		order++;
		values.put(BookmarksTable.COL_ORDER, order);

		try {
			long result = getWritableDatabase().insertWithOnConflict(BookmarksTable.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_IGNORE);
			Logger.i(this, "Result for adding "+ video+ " IS "+ result);
			if (result >= 1) {
				return DatabaseResult.SUCCESS;
			}
			if (isBookmarked(video.getId())) {
				return DatabaseResult.NOT_MODIFIED;
			} else {
				return DatabaseResult.ERROR;
			}
		} catch (SQLException e) {
			Logger.e(this, "Unexpected error in bookmark creation :"+ video+" - error:"+e.getMessage(), e);
			return DatabaseResult.ERROR;
		}
	}

    public Single<DatabaseResult> bookmarkAsync(YouTubeVideo video) {
        return Single.fromSupplier(() -> add(video))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess(status -> {
                    if (status == DatabaseResult.SUCCESS) {
                        notifyCardAdded(video);
                    }
                });
    }

    public Single<DatabaseResult> unbookmarkAsync(VideoId video) {
        return Single.fromSupplier(() -> remove(video))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess(status -> {
                    if (status == DatabaseResult.SUCCESS) {
                        notifyCardDeleted(video);
                    }
                });
    }

	/**
	 * Remove the specified video from the list of bookmarked videos.
	 *
	 * @param video Video to remove.
	 *
	 * @return True if the video has been unbookmarked; false otherwise.
	 */
	private DatabaseResult remove(VideoId video) {
		try {
			int rowsDeleted = getWritableDatabase().delete(BookmarksTable.TABLE_NAME,
					BookmarksTable.COL_YOUTUBE_VIDEO_ID + " = ?",
					new String[]{video.getId()});

			if (rowsDeleted > 0) {
				// Since we've removed a video, we will need to update the order column for all the videos.
				int order = 1;
				try (Cursor cursor = getReadableDatabase().query(
						BookmarksTable.TABLE_NAME,
						new String[]{BookmarksTable.COL_YOUTUBE_VIDEO, BookmarksTable.COL_ORDER},
						null,
						null, null, null, BookmarksTable.COL_ORDER + " ASC")) {
                    int blobCol = cursor.getColumnIndexOrThrow(BookmarksTable.COL_YOUTUBE_VIDEO);
                    while (cursor.moveToNext()) {
                        byte[] blob = cursor.getBlob(blobCol);
                        YouTubeVideo uvideo = jsonSerializer.fromPersistedVideoJson(blob).updatePublishTimestampFromDate();
                        ContentValues contentValues = new ContentValues();
                        contentValues.put(BookmarksTable.COL_ORDER, order++);

                        getWritableDatabase().update(BookmarksTable.TABLE_NAME, contentValues, BookmarksTable.COL_YOUTUBE_VIDEO_ID + " = ?",
                                new String[]{uvideo.getId()});
                    }
                }
                return DatabaseResult.SUCCESS;
            }
            return DatabaseResult.NOT_MODIFIED;
		} catch (SQLException e) {
			Logger.e(this, "Database error: " + e.getMessage(), e);
			return DatabaseResult.ERROR;
		}
	}


	/**
	 * When a Video in the Bookmarks tab is drag & dropped to a new position, this will be
	 * called with the new updated list of videos. Since the videos are displayed in descending order,
	 * the first video in the list will have the highest number.
	 *
	 * @param videos List of Videos to update their order.
	 */
	@Override
	public void updateOrder(List<CardData> videos) {
		int order = getMaximumOrderNumber();

		for(CardData video : videos) {
			ContentValues cv = new ContentValues();
			cv.put(BookmarksTable.COL_ORDER, order--);
			getWritableDatabase().update(BookmarksTable.TABLE_NAME, cv, BookmarksTable.COL_YOUTUBE_VIDEO_ID + " = ?", new String[]{video.getId()});
		}
	}

	/**
	 * Check if the specified Video has been bookmarked.
	 *
	 * @param videoId Video to check
	 *
	 * @return True if it has been bookmarked, false if not.
	 */
	public boolean isBookmarked(String videoId) {
		SkyTubeApp.nonUiThread();
		return SQLiteHelper.executeQueryForInteger(getReadableDatabase(), BookmarksTable.IS_BOOKMARKED_QUERY, new String[]{videoId}, 0) > 0;
	}

    /**
     * @return The total number of bookmarked videos.
     */
    public Single<Integer> getTotalBookmarkCount() {
        return Single.fromCallable(() ->
                SQLiteHelper.executeQueryForInteger(getReadableDatabase(), BookmarksTable.COUNT_ALL_BOOKMARKS, 0)
        ).subscribeOn(Schedulers.io());
    }

	/**
	 * @return The maximum of the order number - which could be different from the number of bookmarked videos, in case some of them are deleted.
	 */
	public int getMaximumOrderNumber() {
		SkyTubeApp.nonUiThread();
		return SQLiteHelper.executeQueryForInteger(getReadableDatabase(), BookmarksTable.MAXIMUM_ORDER_QUERY, 0);
	}


	/**
	 * Get the list of Videos that have been bookmarked.
	 *
	 * @return List of Videos
	 */
	public @NonNull Pair<List<YouTubeVideo>, Integer> getBookmarkedVideos(int limit, Integer maxOrderLimit) {
        //Logger.i(this, "getBookmarkedVideos " + limit + ',' + maxOrderLimit +
        //        " - " + (maxOrderLimit != null ? BookmarksTable.PAGED_QUERY : BookmarksTable.PAGED_QUERY_UNBOUNDED));
		SkyTubeApp.nonUiThread();

        SQLiteDatabase db = getReadableDatabase();
        Cursor	cursor = maxOrderLimit != null ?
                db.rawQuery(
                        BookmarksTable.PAGED_QUERY, new String[] { String.valueOf(maxOrderLimit), String.valueOf(limit)}) :
                db.rawQuery(
                        BookmarksTable.PAGED_QUERY_UNBOUNDED, new String[] { String.valueOf(limit)});

		List<YouTubeVideo> videos = new ArrayList<>();

		Integer minOrder = null;
		if(cursor.moveToNext()) {
			final int colOrder = cursor.getColumnIndex(BookmarksTable.COL_ORDER);
			final int colVideo = cursor.getColumnIndex(BookmarksTable.COL_YOUTUBE_VIDEO);
			do {
				final byte[] blob = cursor.getBlob(colVideo);
				final int currentOrder = cursor.getInt(colOrder);

                minOrder = Utils.min(currentOrder, minOrder);

				// convert JSON into YouTubeVideo
				YouTubeVideo video = jsonSerializer.fromPersistedVideoJson(blob);

				// add the video to the list
				videos.add(video);
			} while(cursor.moveToNext());
		}

		cursor.close();
		return Pair.create(videos, minOrder);
	}

	/**
	 *
	 * @return all the bookmarked video's id.
	 */
	public @NonNull Set<VideoId> getAllBookmarkedVideoIds() {
		SkyTubeApp.nonUiThread();

		SQLiteDatabase db = getReadableDatabase();
		Set<VideoId> results = new HashSet<>();
		try (Cursor	cursor = db.rawQuery(BookmarksTable.QUERY_ALL_IDS, new String[0] )) {
			while(cursor.moveToNext()) {
				String id = cursor.getString(0);
				results.add(VideoId.create(id));
			}
		}
		return results;
	}

}
