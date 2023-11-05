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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializer;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.app.Utils;
import free.rm.skytube.businessobjects.Logger;
import free.rm.skytube.businessobjects.YouTube.POJOs.ChannelView;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * A database (DB) that stores user subscriptions (with respect to YouTube channels).
 */
public class SubscriptionsDb extends SQLiteOpenHelperEx {
    private static final String HAS_VIDEO_QUERY = String.format("SELECT COUNT(*) FROM %s WHERE %s = ?", SubscriptionsVideosTable.TABLE_NAME_V2, SubscriptionsVideosTable.COL_YOUTUBE_VIDEO_ID);
    private static final String GET_VIDEO_IDS_BY_CHANNEL_TO_PUBLISH_TS = String.format("SELECT %s,%s FROM %s WHERE %s = ?",
            SubscriptionsVideosTable.COL_YOUTUBE_VIDEO_ID, SubscriptionsVideosTable.COL_PUBLISH_TIME.name, SubscriptionsVideosTable.TABLE_NAME_V2, SubscriptionsVideosTable.COL_CHANNEL_ID);
    private static final String GET_VIDEO_IDS_BY_CHANNEL = String.format("SELECT %s FROM %s WHERE %s = ?",
            SubscriptionsVideosTable.COL_YOUTUBE_VIDEO_ID, SubscriptionsVideosTable.TABLE_NAME_V2, SubscriptionsVideosTable.COL_CHANNEL_ID);
    private static final String FIND_EMPTY_RETRIEVAL_TS = String.format("SELECT %s,%s FROM %s WHERE %s IS NULL",
			SubscriptionsVideosTable.COL_YOUTUBE_VIDEO_ID, SubscriptionsVideosTable.COL_YOUTUBE_VIDEO, SubscriptionsVideosTable.TABLE_NAME, SubscriptionsVideosTable.COL_RETRIEVAL_TS);
	private static final String sortChannelsASC = "LOWER(" + SubscriptionsTable.COL_TITLE + ") ASC ";

	private static final String SUBSCRIBED_CHANNEL_INFO = String.format("SELECT %1$s,%2$s,%3$s,%4$s,(select max(%6$s) from %7$s videos where videos.%8$s = subs.%1$s) as latest_video_ts FROM %5$s subs",
			SubscriptionsTable.COL_CHANNEL_ID, SubscriptionsTable.COL_TITLE, SubscriptionsTable.COL_THUMBNAIL_NORMAL_URL, SubscriptionsTable.COL_LAST_VISIT_TIME,
			SubscriptionsTable.TABLE_NAME,
			SubscriptionsVideosTable.COL_PUBLISH_TIME.name, SubscriptionsVideosTable.TABLE_NAME_V2, SubscriptionsVideosTable.COL_CHANNEL_ID);
	private static final String SUBSCRIBED_CHANNEL_INFO_ORDER_BY = " ORDER BY "+sortChannelsASC;
	private static final String SUBSCRIBED_CHANNEL_LIMIT_BY_TITLE = " WHERE LOWER(" +SubscriptionsTable.COL_TITLE + ") like ?";

	private static final String IS_SUBSCRIBED_QUERY = String.format("SELECT EXISTS(SELECT %s FROM %s WHERE %s =?) AS VAL ", SubscriptionsTable.COL_ID, SubscriptionsTable.TABLE_NAME, SubscriptionsTable.COL_CHANNEL_ID);
	private static volatile SubscriptionsDb subscriptionsDb = null;

    private static final int DATABASE_VERSION = 9;

	private static final String DATABASE_NAME = "subs.db";

	private Gson gson;

	private SubscriptionsDb(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		this.gson = createGson();
	}


	public static synchronized SubscriptionsDb getSubscriptionsDb() {
		if (subscriptionsDb == null) {
			subscriptionsDb = new SubscriptionsDb(SkyTubeApp.getContext());
		}

		return subscriptionsDb;
	}

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SubscriptionsTable.getCreateStatement());
        SubscriptionsVideosTable.addNewFlatTable(db);
        db.execSQL(LocalChannelTable.getCreateStatement());
        db.execSQL(CategoriesTable.getCreateStatement());
        new CategoryManagement(db).setupDefaultCategories();
    }

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// Version 2 of the database introduces the SubscriptionsVideosTable, which stores videos found in each subscribed channel
		if(oldVersion == 1 && newVersion >= 2) {
			db.execSQL(SubscriptionsVideosTable.getCreateStatement());
		}
        if (oldVersion <= 2 && newVersion >= 3) {
            execSQLUpdates(db, SubscriptionsTable.getAddColumns());
        }
        if (oldVersion <= 3 && newVersion >= 4) {
            execSQLUpdates(db, SubscriptionsVideosTable.getAddTimestampColumns());
            setupRetrievalTimestamp(db);
        }
        if (oldVersion <= 4 && newVersion >= 5) {
            execSQLUpdates(db, SubscriptionsTable.getLastCheckTimeColumn());
			db.execSQL(LocalChannelTable.getCreateStatement());
			try {
				for (YouTubeChannel channel : getSubscribedChannels(db)) {
					if (!Utils.isEmpty(channel.getId()) &&
							!Utils.isEmpty(channel.getTitle()) &&
							!Utils.isEmpty(channel.getBannerUrl()) &&
							!Utils.isEmpty(channel.getThumbnailUrl()) &&
							!Utils.isEmpty(channel.getDescription())) {
						cacheChannel(db, channel);
					}
				}
			} catch (IOException ex) {
				Logger.e(this, "Unable to load subscribed channels to populate cache:" + ex.getMessage(), ex);
			}
		}
        if (oldVersion <= 6 && newVersion >= 7) {
            db.execSQL(SubscriptionsVideosTable.getIndexOnVideos());
        }
        if (oldVersion <= 7 && newVersion >= 8) {
            continueOnError(db, CategoriesTable.getCreateStatement());
            new CategoryManagement(db).setupDefaultCategories();
            SubscriptionsTable.addCategoryColumn(db);
        }
        if (oldVersion <= 8 && newVersion >= 9) {
            SubscriptionsVideosTable.addNewFlatTable(db);
            migrateFromJsonColumn(db);
        }
//        migrateFromJsonColumn(db);
    }

    private void migrateFromJsonColumn(final SQLiteDatabase db) {
        int fullCount = 0;
        while (true) {
            int success = migrateFromJsonColumnBlock(db);
            if (success > 0) {
                fullCount += success;
            } else {
                Logger.w(this, "Migrated " + fullCount + " videos");
                return;
            }
        }
    }

    private int migrateFromJsonColumnBlock(final SQLiteDatabase db) {
        int counter = 0;
        int success = 0;
        try (Cursor cursor = db.query(SubscriptionsVideosTable.TABLE_NAME,
                new String[] { SubscriptionsVideosTable.COL_YOUTUBE_VIDEO_ID, SubscriptionsVideosTable.COL_YOUTUBE_VIDEO},
                null,
                null,null, null, null, "100")) {
            while (cursor.moveToNext()) {
                counter ++;
                String id = cursor.getString(0);
                final byte[] blob = cursor.getBlob(1);
                final String videoJson = new String(blob);

                // convert JSON into YouTubeVideo
                YouTubeVideo video = gson.fromJson(videoJson, YouTubeVideo.class);
                ContentValues values = convertToContentValues(video, null);
                long rowId = db.insert(SubscriptionsVideosTable.TABLE_NAME_V2, null, values);
                if (rowId > 0) {
                    success ++;
                }
            }
        }
        Logger.w(this, "Loaded " + counter + " videos, updated " + success + " videos successfully");
        return success;
    }

	private void setupRetrievalTimestamp(SQLiteDatabase db) {
        List<YouTubeVideo> videos = extractVideos(db.rawQuery(FIND_EMPTY_RETRIEVAL_TS, null), false);
        int count = 0;
        for (YouTubeVideo video : videos) {
            final Long publishTimestamp = video.getPublishTimestamp();
            if (publishTimestamp != null) {
                ContentValues values = new ContentValues();
                values.put(SubscriptionsVideosTable.COL_PUBLISH_TS, publishTimestamp);
                values.put(SubscriptionsVideosTable.COL_RETRIEVAL_TS, publishTimestamp);
                int updateCount = db.update(
                        SubscriptionsVideosTable.TABLE_NAME,
                        values,
                        SubscriptionsVideosTable.COL_YOUTUBE_VIDEO_ID_EQUALS_TO,
                        new String[]{video.getId()});
                Logger.i(this,"updating " + video.getId() + " with publish date:" + ZonedDateTime.ofInstant(Instant.ofEpochMilli(publishTimestamp), ZoneId.systemDefault()) + " -> " + updateCount);
                count += updateCount;
            }
        }
        Logger.i(this, "From " + videos.size() + ", retrieval timestamp filled for " + count);
    }

	/**
	 * Saves the given channel into the subscriptions DB.
	 *
	 * @param channel Channel the user wants to subscribe to.
	 *
	 * @return DatabaseResult.SUCCESS if the operation was successful; NOT_MODIFIED or ERROR otherwise.
	 */
	public DatabaseResult subscribe(YouTubeChannel channel) {
		SkyTubeApp.nonUiThread();
		saveChannelVideos(channel.getYouTubeVideos(), channel.getId());

		return saveSubscription(channel);
	}

	/**
	 * Saves the given channel into the subscriptions DB.
	 *
	 * @param channel The channel the user wants to subscribe to.
	 *
	 * @return True if the operation was successful; false otherwise.
	 */
	private DatabaseResult saveSubscription(YouTubeChannel channel) {
		SkyTubeApp.nonUiThread();

		ContentValues values = new ContentValues();
		values.put(SubscriptionsTable.COL_CHANNEL_ID, channel.getId());
		values.put(SubscriptionsTable.COL_LAST_VISIT_TIME, channel.getLastVisitTime());
		values.put(SubscriptionsTable.COL_TITLE, channel.getTitle());
		values.put(SubscriptionsTable.COL_DESCRIPTION, channel.getDescription());
		values.put(SubscriptionsTable.COL_BANNER_URL, channel.getBannerUrl());
		values.put(SubscriptionsTable.COL_THUMBNAIL_NORMAL_URL, channel.getThumbnailUrl());
		values.put(SubscriptionsTable.COL_CATEGORY_ID.name, channel.getCategoryId());
		values.put(SubscriptionsTable.COL_SUBSCRIBER_COUNT, channel.getSubscriberCount());

		SQLiteDatabase db = getWritableDatabase();
		try {
			long result = db.insertWithOnConflict(SubscriptionsTable.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_IGNORE);
			if (result > 0) {
				return DatabaseResult.SUCCESS;
			}
			if (isUserSubscribedToChannel(channel.getId())) {
				Logger.i(this, "Already subscribed to " + channel.getId());
				return DatabaseResult.NOT_MODIFIED;
			}
			Logger.i(this, "Unable to subscribe to " + channel.getId());
			return DatabaseResult.ERROR;
		} catch (SQLException e) {
			Logger.e(this, "Error during subscribing: " + e.getMessage(), e);
			return DatabaseResult.ERROR;
		}
	}

	/**
	 * Removes the given channel from the subscriptions DB.
	 *
	 * @param channelId id of the channel the user wants to unsubscribe to.
	 *
	 * @return True if the operation was successful; false otherwise.
	 */
	public DatabaseResult unsubscribe(String channelId) {
		SkyTubeApp.nonUiThread();

		// delete any feed videos pertaining to this channel
		getWritableDatabase().delete(SubscriptionsVideosTable.TABLE_NAME_V2,
				SubscriptionsVideosTable.COL_CHANNEL_ID + " = ?",
				new String[]{channelId});

		// remove this channel from the subscriptions DB
		int rowsDeleted = getWritableDatabase().delete(SubscriptionsTable.TABLE_NAME,
				SubscriptionsTable.COL_CHANNEL_ID + " = ?",
				new String[]{channelId});

		// Need to make sure when we come back to MainActivity, that we refresh the Feed tab so it hides videos from the newly unsubscribed
		SkyTubeApp.getSettings().setRefreshSubsFeedFromCache(true);

		return (rowsDeleted >= 0) ? DatabaseResult.SUCCESS : DatabaseResult.NOT_MODIFIED;
	}

	public void unsubscribeFromAllChannels() {
		getWritableDatabase().delete(SubscriptionsVideosTable.TABLE_NAME_V2,null,null);
		getWritableDatabase().delete(SubscriptionsTable.TABLE_NAME,null,null);
	}

	/**
	 * @param channelId the id of the channel
	 * @return all the video ids for the subscribed channels from the database.
	 */
	public Set<String> getSubscribedChannelVideosByChannel(String channelId) {
		SkyTubeApp.nonUiThread();
		try(Cursor cursor = getReadableDatabase().rawQuery(GET_VIDEO_IDS_BY_CHANNEL, new String[] { channelId})) {
			Set<String> result = new HashSet<>();
			while(cursor.moveToNext()) {
				result.add(cursor.getString(0));
			}
			return result;
		}
	}

    /**
     * @param channelId the id of the channel
     * @return all the video ids for the subscribed channels from the database, mapped to publication times
     */
    public Map<String, Long> getSubscribedChannelVideosByChannelToTimestamp(String channelId) {
        SkyTubeApp.nonUiThread();
        try(Cursor cursor = getReadableDatabase().rawQuery(GET_VIDEO_IDS_BY_CHANNEL_TO_PUBLISH_TS, new String[] { channelId})) {
            Map<String, Long> result = new HashMap<>();
            while(cursor.moveToNext()) {
                result.put(cursor.getString(0), cursor.getLong(1));
            }
            return result;
        }
    }

    public void updateVideo(YouTubeVideo video) {
        ContentValues values = convertToContentValues(video, null);
        getWritableDatabase().update(
                SubscriptionsVideosTable.TABLE_NAME_V2,
                values,
                SubscriptionsVideosTable.COL_YOUTUBE_VIDEO_ID_EQUALS_TO,
                new String[] { video.getId() });
    }

    private ContentValues convertToContentValues(final YouTubeVideo video, String channelId) {
        ContentValues values = new ContentValues();
        values.put(SubscriptionsVideosTable.COL_CHANNEL_ID_V2.name, channelId != null ? channelId : video.getChannelId());
        values.put(SubscriptionsVideosTable.COL_CHANNEL_TITLE.name, video.getSafeChannelName());
        values.put(SubscriptionsVideosTable.COL_YOUTUBE_VIDEO_ID_V2.name, video.getId());
        values.put(SubscriptionsVideosTable.COL_CATEGORY_ID.name, video.getCategoryId());
        values.put(SubscriptionsVideosTable.COL_PUBLISH_TIME_EXACT.name, video.getPublishTimestampExact());
        values.put(SubscriptionsVideosTable.COL_PUBLISH_TIME.name, video.getPublishTimestamp());
        if (video.getLikeCountNumber() != null) {
            values.put(SubscriptionsVideosTable.COL_LIKES.name, video.getLikeCountNumber());
        }
        if (video.getDislikeCountNumber() != null) {
            values.put(SubscriptionsVideosTable.COL_DISLIKES.name, video.getDislikeCountNumber());
        }
        if (video.getViewsCountInt() != null) {
            values.put(SubscriptionsVideosTable.COL_VIEWS.name, video.getViewsCountInt().longValue());
        }
        if (video.getTitle() != null) {
            values.put(SubscriptionsVideosTable.COL_TITLE.name, video.getTitle());
        }
        if (video.getDescription() != null) {
            values.put(SubscriptionsVideosTable.COL_DESCRIPTION.name, video.getDescription());
        }
        values.put(SubscriptionsVideosTable.COL_DURATION.name, video.getDurationInSeconds());
        if (video.getThumbnailUrl() != null) {
            values.put(SubscriptionsVideosTable.COL_THUMBNAIL_URL.name, video.getThumbnailUrl());
        }
        return values;
    }

	public int setPublishTimestamp(YouTubeVideo video) {
        ContentValues values = new ContentValues();
        values.put(SubscriptionsVideosTable.COL_PUBLISH_TIME.name, video.getPublishTimestamp());

        return getWritableDatabase().update(
                SubscriptionsVideosTable.TABLE_NAME_V2,
                values,
                SubscriptionsVideosTable.COL_YOUTUBE_VIDEO_ID_EQUALS_TO,
                new String[] { video.getId() });
    }

	private List<String> getSubscribedChannelIds() {
		SkyTubeApp.nonUiThread();
		try (Cursor cursor = getReadableDatabase().rawQuery("SELECT "+SubscriptionsTable.COL_CHANNEL_ID + " FROM "+SubscriptionsTable.TABLE_NAME,null)) {
			List<String> result = new ArrayList<>();
			while(cursor.moveToNext()) {
				result.add(cursor.getString(0));
			}
			return result;
		}
	}

	public Single<List<String>> getSubscribedChannelIdsAsync() {
		return Single.fromCallable(() -> getSubscribedChannelIds())
				.subscribeOn(Schedulers.io());
	}

	/**
	 * Returns a list of channels that the user subscribed to, without accessing the network
	 *
	 *
	 * @return A list of channels that the user subscribed to.
	 * @throws IOException
	 */
	private List<YouTubeChannel> getSubscribedChannels(SQLiteDatabase db) throws IOException {
		SkyTubeApp.nonUiThread();

		try (Cursor cursor = db.query(SubscriptionsTable.TABLE_NAME,
				SubscriptionsTable.ALL_COLUMNS,
				null, null,
				null, null,
				SubscriptionsTable.COL_ID + " ASC")) {

			List<YouTubeChannel> subsChannels = new ArrayList<>();

			if (cursor.moveToNext()) {
				final int colChannelIdNum = cursor.getColumnIndexOrThrow(SubscriptionsTable.COL_CHANNEL_ID);
				final int colTitle = cursor.getColumnIndexOrThrow(SubscriptionsTable.COL_TITLE);
				final int colDescription = cursor.getColumnIndexOrThrow(SubscriptionsTable.COL_DESCRIPTION);
				final int colBanner = cursor.getColumnIndexOrThrow(SubscriptionsTable.COL_BANNER_URL);
				final int colThumbnail = cursor.getColumnIndexOrThrow(SubscriptionsTable.COL_THUMBNAIL_NORMAL_URL);
				final int colSubscribers = cursor.getColumnIndexOrThrow(SubscriptionsTable.COL_SUBSCRIBER_COUNT);
				final int colLastVisit = cursor.getColumnIndexOrThrow(SubscriptionsTable.COL_LAST_VISIT_TIME);
				final int colLastCheck = cursor.getColumnIndexOrThrow(SubscriptionsTable.COL_LAST_CHECK_TIME);
				final int colCategoryId = cursor.getColumnIndexOrThrow(SubscriptionsTable.COL_CATEGORY_ID.name);

				do {
					final String id = cursor.getString(colChannelIdNum);
					Integer categoryId = getInteger(cursor, colCategoryId);
					subsChannels.add(new YouTubeChannel(id, cursor.getString(colTitle),
							cursor.getString(colDescription), cursor.getString(colThumbnail),
							cursor.getString(colBanner), cursor.getLong(colSubscribers), true, cursor.getLong(colLastVisit),
							cursor.getLong(colLastCheck),
							categoryId, Collections.emptyList()));
				} while (cursor.moveToNext());
			}
			return subsChannels;
		}
	}

	private Integer getInteger(Cursor cursor, int colCategoryId) {
		return (colCategoryId < 0 || cursor.isNull(colCategoryId)) ? null : cursor.getInt(colCategoryId);
	}

	public YouTubeChannel getCachedSubscribedChannel(String channelId) {
		SkyTubeApp.nonUiThread();

		try (Cursor cursor = getReadableDatabase().query(SubscriptionsTable.TABLE_NAME,
				SubscriptionsTable.ALL_COLUMNS,
				SubscriptionsTable.COL_CHANNEL_ID + " = ?", new String[]{channelId},
				null, null, null)) {

			YouTubeChannel channel = null;

			if (cursor.moveToNext()) {
				final int	   colChannelIdNum = cursor.getColumnIndexOrThrow(SubscriptionsTable.COL_CHANNEL_ID);
				final int	   colTitle = cursor.getColumnIndexOrThrow(SubscriptionsTable.COL_TITLE);
				final int	   colDescription = cursor.getColumnIndexOrThrow(SubscriptionsTable.COL_DESCRIPTION);
				final int	   colBanner = cursor.getColumnIndexOrThrow(SubscriptionsTable.COL_BANNER_URL);
				final int	   colThumbnail = cursor.getColumnIndexOrThrow(SubscriptionsTable.COL_THUMBNAIL_NORMAL_URL);
				final int	   colSubscribers = cursor.getColumnIndexOrThrow(SubscriptionsTable.COL_SUBSCRIBER_COUNT);
				final int	   colLastVisit = cursor.getColumnIndexOrThrow(SubscriptionsTable.COL_LAST_VISIT_TIME);
				final int	   colLastCheck = cursor.getColumnIndexOrThrow(SubscriptionsTable.COL_LAST_CHECK_TIME);
				final int	   colCategoryId = cursor.getColumnIndexOrThrow(SubscriptionsTable.COL_CATEGORY_ID.name);

				final String id = cursor.getString(colChannelIdNum);
				Integer categoryId = getInteger(cursor, colCategoryId);

				channel = new YouTubeChannel(id, cursor.getString(colTitle),
						cursor.getString(colDescription), cursor.getString(colThumbnail),
						cursor.getString(colBanner), cursor.getLong(colSubscribers), true,
						cursor.getLong(colLastVisit), cursor.getLong(colLastCheck),
						categoryId, Collections.emptyList());

			}

			return channel;
		}
	}



	/**
	 * Checks if the user is subscribed to the given channel.
	 *
	 * @param channelId	Channel ID
	 * @return True if the user is subscribed; false otherwise.
	 * @throws IOException
	 */
	public boolean isUserSubscribedToChannel(String channelId) {
		SkyTubeApp.nonUiThread();
	    channelId = Utils.removeChannelIdPrefix(channelId);

		return executeQueryForInteger(IS_SUBSCRIBED_QUERY, new String[]{channelId}, 0) > 0;
	}


	public Single<Boolean> getUserSubscribedToChannel(String channelId) {
		return Single.fromCallable(() -> isUserSubscribedToChannel(channelId)).subscribeOn(Schedulers.io());
	}

    /**
     * Updates the given channel's last visit time.
     *
     * @param channelId	Channel ID
     *
     * @return	last visit time, if the update was successful;  -1 otherwise.
     */
    public Single<Long> updateLastVisitTimeAsync(String channelId) {
        return Single.fromCallable(() -> {
            SQLiteDatabase	db = getWritableDatabase();
            long			currentTime = System.currentTimeMillis();

            ContentValues values = new ContentValues();
            values.put(SubscriptionsTable.COL_LAST_VISIT_TIME, currentTime);

            int count = db.update(
                SubscriptionsTable.TABLE_NAME,
                values,
                SubscriptionsTable.COL_CHANNEL_ID + " = ?",
                new String[]{channelId});

            return (count > 0 ? currentTime : -1);
        }).subscribeOn(Schedulers.io());
    }
    
    /**
     * Updates the given channel's last visit time.
     *
     * @param channelId	Channel ID
     *
     * @return	last visit time, if the update was successful;  -1 otherwise.
     */
    public long updateLastCheckTime(String channelId) {
        SQLiteDatabase	db = getWritableDatabase();
        long			currentTime = System.currentTimeMillis();

        ContentValues values = new ContentValues();
        values.put(SubscriptionsTable.COL_LAST_CHECK_TIME, currentTime);

        int count = db.update(
                SubscriptionsTable.TABLE_NAME,
                values,
                SubscriptionsTable.COL_CHANNEL_ID + " = ?",
                new String[]{channelId});

        return (count > 0 ? currentTime : -1);
    }

	/**
	 * Update channel informations in the database from the Object.
	 *
	 * @param channel which contains all the recent informations.
	 * @return true, if the channel was inside the database.
	 */
	public boolean updateChannel(YouTubeChannel channel) {
		SQLiteDatabase	db = getWritableDatabase();

		ContentValues values = new ContentValues();
		values.put(SubscriptionsTable.COL_TITLE, channel.getTitle());
		values.put(SubscriptionsTable.COL_DESCRIPTION, channel.getDescription());
		values.put(SubscriptionsTable.COL_BANNER_URL, channel.getBannerUrl());
		values.put(SubscriptionsTable.COL_THUMBNAIL_NORMAL_URL, channel.getThumbnailUrl());
		values.put(SubscriptionsTable.COL_SUBSCRIBER_COUNT, channel.getSubscriberCount());
		values.put(SubscriptionsTable.COL_LAST_VISIT_TIME, channel.getLastVisitTime());
		values.put(SubscriptionsTable.COL_CATEGORY_ID.name, channel.getCategoryId());

		int count = db.update(
				SubscriptionsTable.TABLE_NAME,
				values,
				SubscriptionsTable.COL_CHANNEL_ID + " = ?",
				new String[]{channel.getId()});
		return count > 0;
	}

    private boolean hasVideo(YouTubeVideo video) {
        return executeQueryForInteger(HAS_VIDEO_QUERY, new String[]{video.getId()}, 0) > 0;
    }

    /**
     * Loop through each video saved in the passed {@link YouTubeChannel} and save it into the database, if it's not already been saved
     * @param videos the list of videos
     * @param channelId the channel id
     */
    public void saveChannelVideos(Collection<YouTubeVideo> videos, String channelId) {
        for (YouTubeVideo video : videos) {
            if(video.getPublishTimestamp() != null && !hasVideo(video)) {
                ContentValues values = createContentValues(video, channelId);
                getWritableDatabase().insert(SubscriptionsVideosTable.TABLE_NAME_V2, null, values);
            }
        }
    }

	/**
	 * Loop through each video saved in the passed {@link YouTubeChannel} and insert it into the database, or update it.
	 * @param videos the list of videos
	 * @param channelId the channel id
	 */
	public void saveVideos(List<YouTubeVideo> videos, String channelId) {
		SkyTubeApp.nonUiThread();
		SQLiteDatabase db = getWritableDatabase();
		for (YouTubeVideo video : videos) {
			if (video.getPublishTimestamp() != null) {
				ContentValues values = createContentValues(video, channelId);
				if (hasVideo(video)) {
					values.remove(SubscriptionsVideosTable.COL_YOUTUBE_VIDEO_ID);
					db.update(SubscriptionsVideosTable.TABLE_NAME_V2,
                            values,
                            SubscriptionsVideosTable.COL_YOUTUBE_VIDEO_ID_EQUALS_TO,
							new String[]{video.getId()});
				} else {
					db.insert(SubscriptionsVideosTable.TABLE_NAME_V2, null, values);
				}
			}
		}
	}

	/**
	 * Insert videos into the subscription video table.
	 * @param videos
	 */
	public void insertVideosForChannel(List<YouTubeVideo> videos, String channelId) {
		SkyTubeApp.nonUiThread();

		SQLiteDatabase db = getWritableDatabase();
		for (YouTubeVideo video : videos) {
			try {
				if (video.getPublishTimestamp() != null) {
					ContentValues values = createContentValues(video, channelId);
					db.insert(SubscriptionsVideosTable.TABLE_NAME_V2, null, values);
				}
			} catch (Exception e) {
				Logger.e(this, e, "Error inserting "+ videos + " - "+e.getMessage());
			}
		}
	}

    private ContentValues createContentValues(YouTubeVideo video, String channelId) {
		channelId = Utils.removeChannelIdPrefix(channelId);
        ContentValues values = convertToContentValues(video, channelId);
        values.put(SubscriptionsVideosTable.COL_CHANNEL_ID, channelId);
        values.put(SubscriptionsVideosTable.COL_YOUTUBE_VIDEO_ID, video.getId());
        // values.put(SubscriptionsVideosTable.COL_YOUTUBE_VIDEO, gson.toJson(video).getBytes());
        final long publishInstant = video.getPublishTimestamp();
        values.put(SubscriptionsVideosTable.COL_PUBLISH_TIME.name, publishInstant);
        values.put(SubscriptionsVideosTable.COL_CATEGORY_ID.name, video.getCategoryId());

        return values;
    }

    /**
     * Query the database to retrieve number of videos for subscribed channels starting from the given video.
     * @return a list of {@link YouTubeVideo}
     */
    public List<YouTubeVideo> getSubscriptionVideoPage(int limit, String videoId, long beforeTimestamp) {
        SkyTubeApp.nonUiThread();

        final String selection;
        final String sortingColumn = SubscriptionsVideosTable.COL_PUBLISH_TIME.name;
        final String[] selectionArguments;
        if (videoId != null) {
            selection = "(" + sortingColumn + " < ?) OR (" + sortingColumn + " = ? AND " + SubscriptionsVideosTable.COL_YOUTUBE_VIDEO_ID + " > ?)";
            String formatted = String.valueOf(beforeTimestamp);
            selectionArguments = new String[]{ formatted, formatted, videoId };
        } else {
            selection = null;
            selectionArguments = null;
        }
        Cursor	cursor = getReadableDatabase().query(
            SubscriptionsVideosTable.TABLE_NAME_V2,
            SubscriptionsVideosTable.ALL_COLUMNS_FOR_EXTRACT,
            selection, selectionArguments, null, null,
                sortingColumn + " DESC, " + SubscriptionsVideosTable.COL_YOUTUBE_VIDEO_ID + " ASC",
            String.valueOf(limit));
        return extractVideos(cursor, true);
    }

    private Gson createGson() {
    	return new GsonBuilder().registerTypeAdapter(YouTubeChannel.class, (JsonSerializer<YouTubeChannel>) (src, typeOfSrc, context) -> {
			JsonObject obj = new JsonObject();
			obj.addProperty("id", src.getId());
			obj.addProperty("title", src.getTitle());
			obj.addProperty("description", src.getDescription());
			obj.addProperty("thumbnailNormalUrl", src.getThumbnailUrl());
			obj.addProperty("bannerUrl", src.getBannerUrl());
			return obj;
		}).create();
	}

    /**
     * Load YouTubeVideo objects from a cursor, only SubscriptionsVideosTable.COL_YOUTUBE_VIDEO column is needed.
     * @param cursor the cursor to process
     * @param fullColumnList get all the columns, not just the JSON blob - set to false only for db
     *                       maintenance queries!
     */
    private List<YouTubeVideo> extractVideos(Cursor cursor, boolean fullColumnList) {
        SkyTubeApp.nonUiThread();
        List<YouTubeVideo> videos = new ArrayList<>();
        Set<String> invalidIds = new HashSet<>();
        try {

            if (cursor.moveToNext()) {
                final int idIdx = cursor.getColumnIndex(SubscriptionsVideosTable.COL_YOUTUBE_VIDEO_ID);
                final int categoryIdx = cursor.getColumnIndex(SubscriptionsVideosTable.COL_CATEGORY_ID.name);
                final int publishTsIdx = fullColumnList ? SubscriptionsVideosTable.COL_PUBLISH_TIME.getColumn(cursor) : -1;
                final int publishTsExactIdx = SubscriptionsVideosTable.COL_PUBLISH_TIME_EXACT.getColumn(cursor);
                final int titleColumn = SubscriptionsVideosTable.COL_TITLE.getColumn(cursor);
                final int descriptionColumn = SubscriptionsVideosTable.COL_DESCRIPTION.getColumn(cursor);
                final int durationColumn = SubscriptionsVideosTable.COL_DURATION.getColumn(cursor);
                final int viewsColumn = SubscriptionsVideosTable.COL_VIEWS.getColumn(cursor);
                final int likesColumn = SubscriptionsVideosTable.COL_LIKES.getColumn(cursor);
                final int dislikesColumn = SubscriptionsVideosTable.COL_DISLIKES.getColumn(cursor);
                final int thumbnailUrlColumn = SubscriptionsVideosTable.COL_THUMBNAIL_URL.getColumn(cursor);
                final int channelTitleColumn = SubscriptionsVideosTable.COL_CHANNEL_TITLE.getColumn(cursor);
                final int channelIdColumn = SubscriptionsVideosTable.COL_CHANNEL_ID_V2.getColumn(cursor);

                do {
                    final String id = cursor.getString(idIdx);

                    // String id, String title, String description, long durationInSeconds,
                    //                            YouTubeChannel channel, long viewCount, Instant publishDate,
                    //                            boolean publishDateExact, String thumbnailUrl
                    YouTubeVideo video = new YouTubeVideo(id, cursor.getString(titleColumn), cursor.getString(descriptionColumn),
                            cursor.getLong(durationColumn),
                            new YouTubeChannel(cursor.getString(channelIdColumn), cursor.getString(channelTitleColumn)),
                            cursor.getLong(viewsColumn),  
                            Instant.ofEpochMilli(cursor.getLong(publishTsIdx)), 
                            cursor.getInt(publishTsExactIdx) > 0,
                            cursor.getString(thumbnailUrlColumn));
                    video.setLikeDislikeCount(cursor.getLong(likesColumn), cursor.getLong(dislikesColumn));
                    video.setPublishTimestamp(cursor.getLong(publishTsIdx));
                    video.setCategoryId(getInteger(cursor, categoryIdx));
                    video.updatePublishTimestampFromDate();

                    // regenerate the video's PublishDatePretty (e.g. 5 hours ago)
                    video.forceRefreshPublishDatePretty();
                    // add the video to the list
                    videos.add(video);
                } while (cursor.moveToNext());
            }
        } finally {
            cursor.close();
        }
        if (!invalidIds.isEmpty()) {
            Logger.e(this, "Found videos without channel: {}", invalidIds);
//            deleteVideosByIds(invalidIds);
        }
        return videos;
    }

    private void deleteVideosByIds(Set<String> ids) {
        for (String id: ids) {
            Logger.w(this, "delete video by id: "+ id);
            int rowsDeleted = getWritableDatabase().delete(SubscriptionsVideosTable.TABLE_NAME_V2,
                        SubscriptionsVideosTable.COL_YOUTUBE_VIDEO_ID + " = ?",
                        new String[]{id});
            Logger.w(this, "result "+rowsDeleted+" deleted");
        }
    }

    // Generic channel caching

	/**
	 *
	 * @param channelId
	 * @return all the information stored in the local cache about the channel.
	 */
	public YouTubeChannel getCachedChannel(String channelId) {
		try (Cursor cursor = getReadableDatabase().query(LocalChannelTable.TABLE_NAME,
				LocalChannelTable.ALL_COLUMNS,
				LocalChannelTable.COL_CHANNEL_ID + " = ?", new String[] { channelId },
				null, null, null)) {
			if (cursor.moveToNext()) {
				String title = cursor.getString(cursor.getColumnIndexOrThrow(LocalChannelTable.COL_TITLE));
				String description = cursor.getString(cursor.getColumnIndexOrThrow(LocalChannelTable.COL_DESCRIPTION));
				String thumbnail = cursor.getString(cursor.getColumnIndexOrThrow(LocalChannelTable.COL_THUMBNAIL_NORMAL_URL));
				String banner = cursor.getString(cursor.getColumnIndexOrThrow(LocalChannelTable.COL_BANNER_URL));
				long subscriberCount = cursor.getLong(cursor.getColumnIndexOrThrow(LocalChannelTable.COL_SUBSCRIBER_COUNT));
				long lastCheckTs = cursor.getLong(cursor.getColumnIndexOrThrow(LocalChannelTable.COL_LAST_CHECK_TS));
				return new YouTubeChannel(channelId, title, description, thumbnail, banner, subscriberCount, false, -1, lastCheckTs, null, Collections.emptyList());
			}
		}
		return null;
	}

	/**
	 * Save channel informations in the database from the Object.
	 *
	 * @param channel which contains all the recent informations.
	 * @return true, if the channel was inside the database.
	 */
	public boolean cacheChannel(YouTubeChannel channel) {
		SQLiteDatabase db = getWritableDatabase();
		updateSubscribedChannelTable(db, channel);
		return cacheChannel(db, channel);
	}

	private boolean updateSubscribedChannelTable(SQLiteDatabase db, YouTubeChannel channel) {
		ContentValues values = new ContentValues();
		values.put(SubscriptionsTable.COL_TITLE, channel.getTitle());
		values.put(SubscriptionsTable.COL_DESCRIPTION, channel.getDescription());
		values.put(SubscriptionsTable.COL_BANNER_URL, channel.getBannerUrl());
		values.put(SubscriptionsTable.COL_THUMBNAIL_NORMAL_URL, channel.getThumbnailUrl());
		values.put(SubscriptionsTable.COL_SUBSCRIBER_COUNT, channel.getSubscriberCount());
		values.put(SubscriptionsTable.COL_LAST_CHECK_TIME, channel.getLastCheckTime());
		values.put(SubscriptionsTable.COL_CATEGORY_ID.name, channel.getCategoryId());

		int count = db.update(
				SubscriptionsTable.TABLE_NAME,
				values,
				SubscriptionsTable.COL_CHANNEL_ID + " = ?",
				new String[]{channel.getId()});
		return count > 0;
	}

	private boolean cacheChannel(SQLiteDatabase db, YouTubeChannel channel) {
		ContentValues values = new ContentValues();
		values.put(LocalChannelTable.COL_TITLE, channel.getTitle());
		values.put(LocalChannelTable.COL_DESCRIPTION, channel.getDescription());
		values.put(LocalChannelTable.COL_BANNER_URL, channel.getBannerUrl());
		values.put(LocalChannelTable.COL_THUMBNAIL_NORMAL_URL, channel.getThumbnailUrl());
		values.put(LocalChannelTable.COL_SUBSCRIBER_COUNT, channel.getSubscriberCount());
		if(channel.getLastVideoTime() > 0) {
			values.put(LocalChannelTable.COL_LAST_VIDEO_TS, channel.getLastVideoTime());
		}
		if (channel.getLastCheckTime() > 0) {
			values.put(LocalChannelTable.COL_LAST_CHECK_TS, channel.getLastCheckTime());
		}

		int count = db.update(
				LocalChannelTable.TABLE_NAME,
				values,
				LocalChannelTable.COL_CHANNEL_ID + " = ?",
				new String[]{channel.getId()});
		if (count > 0) {
			return true;
		} else if (count == 0) {
			values.put(LocalChannelTable.COL_CHANNEL_ID, channel.getId());
			return db.insert(LocalChannelTable.TABLE_NAME, null, values) > 0;
		}
		return false;
	}

	/**
	 * Removes the given channel from the local channel cache.
	 *
	 * @param channelId id of the channel.
	 *
	 * @return True if the operation was successful; false otherwise.
	 */
	public boolean removeCachedChannel(String channelId) {
		// remove this channel from the subscriptions DB
		int rowsDeleted = getWritableDatabase().delete(LocalChannelTable.TABLE_NAME,
				LocalChannelTable.COL_CHANNEL_ID + " = ?",
				new String[]{channelId});
		return (rowsDeleted >= 0);
	}

	public List<ChannelView> getSubscribedChannelsByText(String searchText, boolean sortChannelsAlphabetically) {
		List<ChannelView> result = new ArrayList<>();
		try (Cursor cursor = createSubscriptionCursor(searchText, sortChannelsAlphabetically)) {
			final int channelId = cursor.getColumnIndexOrThrow(SubscriptionsTable.COL_CHANNEL_ID);
			final int title = cursor.getColumnIndexOrThrow(SubscriptionsTable.COL_TITLE);
			final int thumbnail = cursor.getColumnIndexOrThrow(SubscriptionsTable.COL_THUMBNAIL_NORMAL_URL);
			final int colLastVisit = cursor.getColumnIndexOrThrow(SubscriptionsTable.COL_LAST_VISIT_TIME);
			final int colLatestVideoTs = cursor.getColumnIndexOrThrow("latest_video_ts");
			while(cursor.moveToNext()) {
				Long lastVisit = cursor.getLong(colLastVisit);
				Long latestVideoTs = cursor.getLong(colLatestVideoTs);
				boolean hasNew = (latestVideoTs != null && (lastVisit == null || latestVideoTs > lastVisit));
				result.add(new ChannelView(cursor.getString(channelId), cursor.getString(title), cursor.getString(thumbnail), hasNew));
			}
			return result;
		}
	}

    private Cursor createSubscriptionCursor(String searchText, boolean sortChannelsAlphabetically) {
        if (Utils.isEmpty(searchText)) {
            return getReadableDatabase().rawQuery(SUBSCRIBED_CHANNEL_INFO +
                    (sortChannelsAlphabetically ? SUBSCRIBED_CHANNEL_INFO_ORDER_BY : ""), null);
        } else {
            return getReadableDatabase().rawQuery(SUBSCRIBED_CHANNEL_INFO + SUBSCRIBED_CHANNEL_LIMIT_BY_TITLE +
                            (sortChannelsAlphabetically ? SUBSCRIBED_CHANNEL_INFO_ORDER_BY : ""),
                    new String[]{"%" + searchText.toLowerCase() + "%"});
        }
    }
}
