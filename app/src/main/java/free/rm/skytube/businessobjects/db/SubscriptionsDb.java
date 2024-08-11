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

import com.github.skytube.components.utils.SQLiteHelper;
import com.github.skytube.components.utils.Stopwatch;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializer;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import androidx.annotation.Nullable;

import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.app.Utils;
import free.rm.skytube.businessobjects.Logger;
import free.rm.skytube.businessobjects.YouTube.POJOs.ChannelView;
import free.rm.skytube.businessobjects.YouTube.POJOs.PersistentChannel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;
import free.rm.skytube.businessobjects.YouTube.newpipe.ChannelId;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * A database (DB) that stores user subscriptions (with respect to YouTube channels).
 */
public class SubscriptionsDb extends SQLiteOpenHelperEx {
    private static final String HAS_VIDEO_QUERY = String.format("SELECT COUNT(*) FROM %s WHERE %s = ?", SubscriptionsVideosTable.TABLE_NAME_V2, SubscriptionsVideosTable.COL_YOUTUBE_VIDEO_ID);
    private static final String GET_VIDEO_IDS_BY_CHANNEL_TO_PUBLISH_TS = String.format("SELECT %s,%s FROM %s WHERE %s = ?",
            SubscriptionsVideosTable.COL_YOUTUBE_VIDEO_ID, SubscriptionsVideosTable.COL_PUBLISH_TIME.name(), SubscriptionsVideosTable.TABLE_NAME_V2, SubscriptionsVideosTable.COL_CHANNEL_ID);
    private static final String GET_VIDEO_IDS_BY_CHANNEL = String.format("SELECT %s FROM %s WHERE %s = ?",
            SubscriptionsVideosTable.COL_YOUTUBE_VIDEO_ID, SubscriptionsVideosTable.TABLE_NAME_V2, SubscriptionsVideosTable.COL_CHANNEL_ID);
    private static final String FIND_EMPTY_RETRIEVAL_TS = String.format("SELECT %s,%s FROM %s WHERE %s IS NULL",
			SubscriptionsVideosTable.COL_YOUTUBE_VIDEO_ID, SubscriptionsVideosTable.COL_YOUTUBE_VIDEO, SubscriptionsVideosTable.TABLE_NAME, SubscriptionsVideosTable.COL_RETRIEVAL_TS);

    private static final String SUBSCRIBED_CHANNEL_INFO = "SELECT c.Channel_Id,c.Title,c.Thumbnail_Normal_Url,s.Last_Visit_Time,c.Last_Video_TS as latest_video_ts FROM Subs s,Channel c where s.channel_pk = c._Id ";
	private static final String SUBSCRIBED_CHANNEL_INFO_ORDER_BY = " ORDER BY LOWER(" + LocalChannelTable.COL_TITLE + ") ASC";
	private static final String SUBSCRIBED_CHANNEL_LIMIT_BY_TITLE = " and LOWER(c." +LocalChannelTable.COL_TITLE + ") like ?";

    private static final String GET_ALL_SUBSCRIBED_CHANNEL_ID = "SELECT "+SubscriptionsTable.COL_CHANNEL_ID + " FROM "+SubscriptionsTable.TABLE_NAME;
	private static final String IS_SUBSCRIBED_QUERY = String.format("SELECT EXISTS(SELECT %s FROM %s WHERE %s =?) AS VAL ", SubscriptionsTable.COL_ID, SubscriptionsTable.TABLE_NAME, SubscriptionsTable.COL_CHANNEL_ID);

    private static final String GET_PK_FROM_CHANNEL_ID = "SELECT " + SubscriptionsTable.COL_ID + " FROM " + SubscriptionsTable.TABLE_NAME + " WHERE " + SubscriptionsTable.COL_CHANNEL_ID + " = ?";

	private static volatile SubscriptionsDb subscriptionsDb = null;

    private static final int DATABASE_VERSION = 18;

    private static final String DATABASE_NAME = "subs.db";

    private SubscriptionsDb(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
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
        SubscriptionsVideosTable.addNewFlatTable(db, false);
        SubscriptionsVideosTable.addPublishTimeIndex(db);
        db.execSQL(LocalChannelTable.getCreateStatement(true));
        LocalChannelTable.addChannelIdIndex(db);
        db.execSQL(CategoriesTable.getCreateStatement());
        new CategoryManagement(db).setupDefaultCategories();
    }

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        VersionUpgrade upgrade = new VersionUpgrade(oldVersion, newVersion);
        // Version 2 of the database introduces the SubscriptionsVideosTable, which stores videos found in each subscribed channel
        if (upgrade.executeStep(2)) {
            db.execSQL(SubscriptionsVideosTable.getCreateStatement());
        }
        if (upgrade.executeStep(3)) {
            SQLiteHelper.execSQLUpdates(db, SubscriptionsTable.getAddColumns());
        }
        if (upgrade.executeStep(4)) {
            SQLiteHelper.execSQLUpdates(db, SubscriptionsVideosTable.getAddTimestampColumns());
            setupRetrievalTimestamp(db);
        }
        if (upgrade.executeStep(5)) {
            SQLiteHelper.execSQLUpdates(db, SubscriptionsTable.getLastCheckTimeColumn());
			db.execSQL(LocalChannelTable.getCreateStatement(false));
			try {
				for (YouTubeChannel channel : getSubscribedChannels(db)) {
					if (!Utils.isEmpty(channel.getId()) &&
							!Utils.isEmpty(channel.getTitle()) &&
							!Utils.isEmpty(channel.getBannerUrl()) &&
							!Utils.isEmpty(channel.getThumbnailUrl()) &&
							!Utils.isEmpty(channel.getDescription())) {
						cacheChannel(db, null, channel);
					}
				}
			} catch (IOException ex) {
				Logger.e(this, "Unable to load subscribed channels to populate cache:" + ex.getMessage(), ex);
			}
		}
        if (upgrade.executeStep(7)) {
            db.execSQL(SubscriptionsVideosTable.getIndexOnVideos());
        }
        if (upgrade.executeStep(8)) {
            SQLiteHelper.continueOnError(db, CategoriesTable.getCreateStatement());
            new CategoryManagement(db).setupDefaultCategories();
            SubscriptionsTable.addCategoryColumn(db);
        }
        if (upgrade.executeStep(9)) {
            SubscriptionsVideosTable.addNewFlatTable(db, true);
            migrateFromJsonColumn(db);
        }
        if (upgrade.executeStep(10)) {
            fixChannelIds(db);
        }
        if (upgrade.executeStep(11)) {
            dropOldSubsVideosTable(db);
        }
        if (upgrade.executeStep(12)) {
            normalizeSubscriptionVideosTable(db);
        }
        if (upgrade.executeStep(13)) {
            LocalChannelTable.addIdColumn(db);
        }
        if (upgrade.executeStep(14)) {
            normalizeSubscriptionVideosTableSecondStep(db);
        }
        if (upgrade.executeStep(15)) {
            SubscriptionsTable.cleanupTable(db);
        }
        if (upgrade.executeStep(17)) {
            Logger.w(this, "Remove channel title from subscription_videos table");
            SubscriptionsVideosTable.removeChannelTitle(db);
            SubscriptionsVideosTable.addPublishTimeIndex(db);
        }
        if (upgrade.executeStep(18)) {
            Logger.w(this, "Optimize Channel table");
            LocalChannelTable.addChannelIdIndex(db);
            SubscriptionsTable.addChannelIdColumn(db);
            LocalChannelTable.addStateColumn(db);
        }
    }

    private void normalizeSubscriptionVideosTable(final SQLiteDatabase db) {
        Logger.w(this, "Normalizing subscription_videos table");
        SubscriptionsVideosTable.addSubsIdColumn(db);
        Map<ChannelId, Long> channelIdLongMap = getChannelIdLongMap(db, SubscriptionsTable.GET_ID_AND_CHANNEL_ID);
        for (Map.Entry<ChannelId, Long> entry : channelIdLongMap.entrySet()) {
            db.execSQL("update subscription_videos set subs_id = ? where Channel_Id = ?",new Object[] {
                    entry.getValue(), entry.getKey().getRawId() });
        }
    }

    private void normalizeSubscriptionVideosTableSecondStep(final SQLiteDatabase db) {
        Logger.w(this, "Normalizing subscription_videos table - 2nd step");
        SubscriptionsVideosTable.addChannelPkColumn(db);
        Map<ChannelId, Long> channelIdLongMap = getChannelIdLongMap(db, LocalChannelTable.GET_ID_AND_CHANNEL_ID);
        for (Map.Entry<ChannelId, Long> entry : channelIdLongMap.entrySet()) {
            db.execSQL("update subscription_videos set channel_pk = ? where Channel_Id = ?",new Object[] {
                    entry.getValue(), entry.getKey().getRawId() });
        }
    }

    private static Map<ChannelId, Long> getChannelIdLongMap(final SQLiteDatabase db, final String query) {
        Map<ChannelId, Long> channelIdLongMap = new HashMap<>();
        try (Cursor cursor = db.rawQuery(query, null)) {
            final int _idIdx = cursor.getColumnIndex(SubscriptionsTable.COL_ID);
            final int channelIdx = cursor.getColumnIndex(SubscriptionsTable.COL_CHANNEL_ID);
            while (cursor.moveToNext()) {
                Long id = Objects.requireNonNull(cursor.getLong(_idIdx), "missing _id column");
                String channelIds = Objects.requireNonNull(cursor.getString(channelIdx), "missing channelIdx column");
                channelIdLongMap.put(new ChannelId(channelIds), id);
            }
        }
        return channelIdLongMap;
    }

    private void dropOldSubsVideosTable(final SQLiteDatabase db) {
        Logger.w(this, "Dropping old Subs table");
        SQLiteHelper.continueOnError(db, SubscriptionsVideosTable.getDropTableStatement());
    }

    private void fixChannelIds(final SQLiteDatabase db) {
        Logger.w(this, "Fixing channel_id in the subscription_videos table");
        db.execSQL("update subscription_videos set channel_id = substr(channel_id, 33) where channel_id like \"https://www.youtube.com/channel/%\"");
    }

    private void migrateFromJsonColumn(final SQLiteDatabase db) {
        int fullCount = 0;
        Gson gson = new GsonBuilder().registerTypeAdapter(YouTubeChannel.class, (JsonSerializer<YouTubeChannel>) (src, typeOfSrc, context) -> {
            JsonObject obj = new JsonObject();
            obj.addProperty("id", src.getId());
            obj.addProperty("title", src.getTitle());
            obj.addProperty("description", src.getDescription());
            obj.addProperty("thumbnailNormalUrl", src.getThumbnailUrl());
            obj.addProperty("bannerUrl", src.getBannerUrl());
            return obj;
        }).create();
        while (true) {
            int success = migrateFromJsonColumnBlock(db, gson);
            if (success > 0) {
                fullCount += success;
            } else {
                Logger.w(this, "Migrated " + fullCount + " videos");
                return;
            }
        }
    }

    private int migrateFromJsonColumnBlock(final SQLiteDatabase db, Gson gson) {
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
	 * @param persistentChannel Channel the user wants to subscribe to.
	 * @param videos the channel videos, which needs to be added to the database.
     *
	 * @return DatabaseResult.SUCCESS if the operation was successful; NOT_MODIFIED or ERROR otherwise.
	 */
	public DatabaseResult subscribe(PersistentChannel persistentChannel, Collection<YouTubeVideo> videos) {
		SkyTubeApp.nonUiThread();
		saveChannelVideos(videos, persistentChannel, false);

		return saveSubscription(persistentChannel.channelPk(), persistentChannel.channel().getChannelId());
	}

	/**
	 * Saves the given channel into the subscriptions DB.
	 *
	 * @param channelId The id of the channel the user wants to subscribe to.
	 *
	 * @return True if the operation was successful; false otherwise.
	 */
	private DatabaseResult saveSubscription(long channelPk, ChannelId channelId) {
		SkyTubeApp.nonUiThread();

		ContentValues values = new ContentValues();
		values.put(SubscriptionsTable.COL_CHANNEL_ID, channelId.getRawId());
        values.put(SubscriptionsTable.COL_CHANNEL_PK.name(), channelPk);

		SQLiteDatabase db = getWritableDatabase();
		try {
			long result = db.insertWithOnConflict(SubscriptionsTable.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_IGNORE);
			if (result > 0) {

				return DatabaseResult.SUCCESS;
			}
			if (isUserSubscribedToChannel(channelId)) {
				Logger.i(this, "Already subscribed to " + channelId);
				return DatabaseResult.NOT_MODIFIED;
			}
			Logger.i(this, "Unable to subscribe to " + channelId);
			return DatabaseResult.ERROR;
		} catch (SQLException e) {
			Logger.e(this, "Error during subscribing: " + e.getMessage(), e);
			return DatabaseResult.ERROR;
		}
	}

	/**
	 * Removes the given channel from the subscriptions DB.
	 *
	 * @param channel  the channel the user wants to unsubscribe from.
	 *
	 * @return True if the operation was successful; false otherwise.
	 */
	public DatabaseResult unsubscribe(PersistentChannel channel) {
		SkyTubeApp.nonUiThread();
        Logger.i(this, "unsubscribing subs_id= %s, channel_id = %s, channel_pk = %s", channel.subscriptionPk(), channel.channel().getChannelId(), channel.channelPk());
        // delete any feed videos pertaining to this channel
        getWritableDatabase().delete(SubscriptionsVideosTable.TABLE_NAME_V2,
                SubscriptionsVideosTable.COL_SUBS_ID.name() + " = ?",
                toArray(channel.subscriptionPk()));

        // remove this channel from the subscriptions DB
        int rowsDeleted = getWritableDatabase().delete(SubscriptionsTable.TABLE_NAME,
                SubscriptionsTable.COL_CHANNEL_ID + " = ?",
                toArray(channel.channel().getId()));

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
	public Set<String> getSubscribedChannelVideosByChannel(ChannelId channelId) {
		SkyTubeApp.nonUiThread();
        try (Cursor cursor = getReadableDatabase().rawQuery(GET_VIDEO_IDS_BY_CHANNEL, toArrayParam(channelId))) {
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
    public Map<String, Long> getSubscribedChannelVideosByChannelToTimestamp(ChannelId channelId) {
        SkyTubeApp.nonUiThread();
        try(Cursor cursor = getReadableDatabase().rawQuery(GET_VIDEO_IDS_BY_CHANNEL_TO_PUBLISH_TS, toArrayParam(channelId))) {
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

    private ContentValues convertToContentValues(final YouTubeVideo video, @Nullable PersistentChannel persistentChannel) {
        ContentValues values = new ContentValues();
        if (persistentChannel != null) {
            ChannelId chId = persistentChannel.channel().getChannelId();
            values.put(SubscriptionsVideosTable.COL_CHANNEL_ID_V2.name(), chId.getRawId());
            values.put(SubscriptionsVideosTable.COL_SUBS_ID.name(), persistentChannel.subscriptionPk());
            values.put(SubscriptionsVideosTable.COL_CHANNEL_PK.name(), persistentChannel.channelPk());
        }

        values.put(SubscriptionsVideosTable.COL_YOUTUBE_VIDEO_ID_V2.name(), video.getId());
        values.put(SubscriptionsVideosTable.COL_CATEGORY_ID.name(), video.getCategoryId());
        values.put(SubscriptionsVideosTable.COL_PUBLISH_TIME_EXACT.name(), video.getPublishTimestampExact());
        values.put(SubscriptionsVideosTable.COL_PUBLISH_TIME.name(), video.getPublishTimestamp());
        if (video.getLikeCountNumber() != null) {
            values.put(SubscriptionsVideosTable.COL_LIKES.name(), video.getLikeCountNumber());
        }
        if (video.getDislikeCountNumber() != null) {
            values.put(SubscriptionsVideosTable.COL_DISLIKES.name(), video.getDislikeCountNumber());
        }
        if (video.getViewsCountInt() != null) {
            values.put(SubscriptionsVideosTable.COL_VIEWS.name(), video.getViewsCountInt().longValue());
        }
        if (video.getTitle() != null) {
            values.put(SubscriptionsVideosTable.COL_TITLE.name(), video.getTitle());
        }
        if (video.getDescription() != null) {
            values.put(SubscriptionsVideosTable.COL_DESCRIPTION.name(), video.getDescription());
        }
        values.put(SubscriptionsVideosTable.COL_DURATION.name(), video.getDurationInSeconds());
        if (video.getThumbnailUrl() != null) {
            values.put(SubscriptionsVideosTable.COL_THUMBNAIL_URL.name(), video.getThumbnailUrl());
        }
        return values;
    }
    public int setPublishTimestamp(YouTubeVideo video) {
        ContentValues values = new ContentValues();
        values.put(SubscriptionsVideosTable.COL_PUBLISH_TIME.name(), video.getPublishTimestamp());

        return getWritableDatabase().update(
                SubscriptionsVideosTable.TABLE_NAME_V2,
                values,
                SubscriptionsVideosTable.COL_YOUTUBE_VIDEO_ID_EQUALS_TO,
                new String[] { video.getId() });
    }

	private List<ChannelId> getSubscribedChannelIds() {
		SkyTubeApp.nonUiThread();
		try (Cursor cursor = getReadableDatabase().rawQuery(GET_ALL_SUBSCRIBED_CHANNEL_ID,null)) {
			List<ChannelId> result = new ArrayList<>();
			while(cursor.moveToNext()) {
				result.add(new ChannelId(cursor.getString(0)));
			}
			return result;
		}
	}

	public Single<List<ChannelId>> getSubscribedChannelIdsAsync() {
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

		try (Cursor cursor = db.rawQuery("select s._id subs_id, s.category_id, s.Last_Visit_Time, c.* from Subs s join Channel c on s.Channel_Id = c.Channel_Id", null)) {

			List<YouTubeChannel> subsChannels = new ArrayList<>();

			if (cursor.moveToNext()) {
				final int colChannelIdNum = cursor.getColumnIndexOrThrow(LocalChannelTable.COL_CHANNEL_ID.name());
				final int colTitle = cursor.getColumnIndexOrThrow(LocalChannelTable.COL_TITLE);
				final int colDescription = cursor.getColumnIndexOrThrow(LocalChannelTable.COL_DESCRIPTION);
				final int colBanner = cursor.getColumnIndexOrThrow(LocalChannelTable.COL_BANNER_URL);
				final int colThumbnail = cursor.getColumnIndexOrThrow(LocalChannelTable.COL_THUMBNAIL_NORMAL_URL);
				final int colSubscribers = cursor.getColumnIndexOrThrow(LocalChannelTable.COL_SUBSCRIBER_COUNT);
				final int colLastCheck = cursor.getColumnIndexOrThrow(LocalChannelTable.COL_LAST_CHECK_TS);
                final int colLastVisit = cursor.getColumnIndexOrThrow(SubscriptionsTable.COL_LAST_VISIT_TIME);
				final int colCategoryId = cursor.getColumnIndexOrThrow(SubscriptionsTable.COL_CATEGORY_ID.name());

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

	/**
	 * Checks if the user is subscribed to the given channel.
	 *
	 * @param channelId	Channel ID
	 * @return True if the user is subscribed; false otherwise.
	 * @throws IOException
	 */
	public boolean isUserSubscribedToChannel(ChannelId channelId) {
		SkyTubeApp.nonUiThread();

        return SQLiteHelper.executeQueryForInteger(getReadableDatabase(), IS_SUBSCRIBED_QUERY, toArrayParam(channelId), 0) > 0;
	}

	public Single<Boolean> getUserSubscribedToChannel(ChannelId channelId) {
		return Single.fromCallable(() -> isUserSubscribedToChannel(channelId)).subscribeOn(Schedulers.io());
	}

    /**
     * Updates the given channel's last visit time.
     *
     * @param channelId	Channel ID
     *
     * @return	last visit time, if the update was successful;  -1 otherwise.
     */
    public Single<Long> updateLastVisitTimeAsync(ChannelId channelId) {
        return Single.fromCallable(() -> {
            SQLiteDatabase	db = getWritableDatabase();
            long			currentTime = System.currentTimeMillis();

            ContentValues values = new ContentValues();
            values.put(SubscriptionsTable.COL_LAST_VISIT_TIME, currentTime);

            int count = db.update(
                SubscriptionsTable.TABLE_NAME,
                values,
                SubscriptionsTable.COL_CHANNEL_ID + " = ?",
                toArrayParam(channelId));

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
    public long updateLastVideoFetch(ChannelId channelId) {
        SQLiteDatabase	db = getWritableDatabase();
        long			currentTime = System.currentTimeMillis();

        ContentValues values = new ContentValues();
        values.put(SubscriptionsTable.COL_LAST_VIDEO_FETCH, currentTime);

        int count = db.update(
                SubscriptionsTable.TABLE_NAME,
                values,
                SubscriptionsTable.COL_CHANNEL_ID + " = ?",
                new String[]{channelId.getRawId()});

        return (count > 0 ? currentTime : -1);
    }

    private boolean hasVideo(YouTubeVideo video) {
        return SQLiteHelper.executeQueryForInteger(getReadableDatabase(), HAS_VIDEO_QUERY, new String[]{video.getId()}, 0) > 0;
    }

	/**
	 * Loop through each video saved in the passed {@link YouTubeChannel} and insert it into the database, or update it.
	 * @param videos the list of videos
     * @param persistentChannel information about the persisted channel.
	 */
	public void saveChannelVideos(Collection<YouTubeVideo> videos, PersistentChannel persistentChannel, boolean doUpdate) {
        SkyTubeApp.nonUiThread();
        SQLiteDatabase db = getWritableDatabase();
        long latestPublishTimestamp = 0;

        for (YouTubeVideo video : videos) {
            if (video.getPublishTimestamp() != null) {
                latestPublishTimestamp = Math.max(latestPublishTimestamp, video.getPublishTimestamp());
                ContentValues values = createContentValues(video, persistentChannel);
                if (hasVideo(video)) {
                    if (doUpdate) {
                        values.remove(SubscriptionsVideosTable.COL_YOUTUBE_VIDEO_ID);
                        db.update(SubscriptionsVideosTable.TABLE_NAME_V2,
                                values,
                                SubscriptionsVideosTable.COL_YOUTUBE_VIDEO_ID_EQUALS_TO,
                                new String[]{video.getId()});
                    }
                } else {
                    db.insert(SubscriptionsVideosTable.TABLE_NAME_V2, null, values);
                }
            }
        }
        SubscriptionsTable.updateLastVideoFetchTimestamps(db, persistentChannel);
        LocalChannelTable.updateLatestVideoTimestamp(db, persistentChannel, latestPublishTimestamp);
    }

    private ContentValues createContentValues(YouTubeVideo video, PersistentChannel persistentChannel) {
        ContentValues values = convertToContentValues(video, persistentChannel);
        values.put(SubscriptionsVideosTable.COL_YOUTUBE_VIDEO_ID, video.getId());
        final long publishInstant = video.getPublishTimestamp();
        values.put(SubscriptionsVideosTable.COL_PUBLISH_TIME.name(), publishInstant);
        values.put(SubscriptionsVideosTable.COL_CATEGORY_ID.name(), video.getCategoryId());

        return values;
    }

    /**
     * Query the database to retrieve number of videos for subscribed channels starting from the given video.
     * @return a list of {@link YouTubeVideo}
     */
    public List<YouTubeVideo> getSubscriptionVideoPage(int limit, String videoId, long beforeTimestamp) {
        SkyTubeApp.nonUiThread();

        final String selection;
        final String sortingColumn = SubscriptionsVideosTable.COL_PUBLISH_TIME.name();
        final String[] selectionArguments;
        if (videoId != null) {
            selection = "WHERE (" + sortingColumn + " < ?) OR (" + sortingColumn + " = ? AND " + SubscriptionsVideosTable.COL_YOUTUBE_VIDEO_ID + " > ?)";
            String formatted = String.valueOf(beforeTimestamp);
            selectionArguments = new String[]{ formatted, formatted, videoId };
        } else {
            selection = "";
            selectionArguments = null;
        }
        final String sorting = " ORDER BY " + sortingColumn + " DESC, " + SubscriptionsVideosTable.COL_YOUTUBE_VIDEO_ID + " ASC limit "+ limit;
        String query = SubscriptionsVideosTable.BASE_QUERY + selection + sorting;
        try (Stopwatch s = new Stopwatch("getVideos " + query + ",limit=" + limit + ", beforeTimestamp=" + beforeTimestamp+" videoid="+videoId)) {
            Cursor cursor = getReadableDatabase().rawQuery(query, selectionArguments);
            return extractVideos(cursor, true);
        }
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
                final int categoryIdx = cursor.getColumnIndex(SubscriptionsVideosTable.COL_CATEGORY_ID.name());
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
        }
        return videos;
    }

    public Maybe<PersistentChannel> getChannel(ChannelId channelId) {
        return Maybe.fromCallable(() -> getCachedChannel(channelId));
    }

	/**
	 *
	 * @param channelId
	 * @return all the information stored in the local cache about the channel.
	 */
	public PersistentChannel getCachedChannel(ChannelId channelId) {
        SkyTubeApp.nonUiThread();
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "select s._id subs_id, c.* from  Channel c left outer Join Subs s on c.Channel_Id = s.Channel_Id where c.Channel_Id = ?",
                toArrayParam(channelId))) {
            if (cursor.moveToNext()) {
                Long subscriptionPk = SQLiteHelper.getOptionalLong(cursor, "subs_id");
                Long channelPk = SQLiteHelper.getLong(cursor, LocalChannelTable.COL_ID.name());

				String title = cursor.getString(cursor.getColumnIndexOrThrow(LocalChannelTable.COL_TITLE));
				String description = cursor.getString(cursor.getColumnIndexOrThrow(LocalChannelTable.COL_DESCRIPTION));
				String thumbnail = cursor.getString(cursor.getColumnIndexOrThrow(LocalChannelTable.COL_THUMBNAIL_NORMAL_URL));
				String banner = cursor.getString(cursor.getColumnIndexOrThrow(LocalChannelTable.COL_BANNER_URL));
                long subscriberCount = SQLiteHelper.getLong(cursor, LocalChannelTable.COL_SUBSCRIBER_COUNT);
                long lastCheckTs = SQLiteHelper.getLong(cursor, LocalChannelTable.COL_LAST_CHECK_TS);
                // TODO: use
                Long lastVideoTs = SQLiteHelper.getOptionalLong(cursor, LocalChannelTable.COL_LAST_VIDEO_TS);
                YouTubeChannel channel = new YouTubeChannel(channelId.getRawId(), title, description, thumbnail, banner, subscriberCount, subscriptionPk != null, -1, lastCheckTs, null, Collections.emptyList());
                return new PersistentChannel(channel, channelPk, subscriptionPk);
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
    public PersistentChannel cacheChannel(@Nullable PersistentChannel persistentChannel, YouTubeChannel channel) {
        SQLiteDatabase db = getWritableDatabase();
        return cacheChannel(db, persistentChannel, channel);
    }

    private String[] toArray(Object obj) {
        return new String[] { String.valueOf(obj)};
    }

    private String[] toArrayParam(ChannelId channelId) {
        return new String[] { channelId.getRawId() };
    }

    private @Nullable Long getChannelPk(SQLiteDatabase db, ChannelId channelId){
        try (Cursor cursor = db.rawQuery(GET_PK_FROM_CHANNEL_ID, toArrayParam(channelId))) {
            if (cursor.moveToNext()) {
                return cursor.getLong(0);
            }
        }
        return null;
    }

    private PersistentChannel cacheChannel(SQLiteDatabase db, @Nullable PersistentChannel persistentChannel, YouTubeChannel channel) {
        ContentValues values = toContentValues(channel);

        final Long channelPk;
        if (persistentChannel != null) {
            channelPk = persistentChannel.channelPk();
        } else {
            channelPk = getChannelPk(db, channel.getChannelId());
        }
        Long subPk = persistentChannel != null ? persistentChannel.subscriptionPk() : null;

        // If there is a persistentChannel info, we already have the channel in the db
        if (channelPk != null) {
            // Try to update it ...
            int count = db.update(
                    LocalChannelTable.TABLE_NAME,
                    values,
                    LocalChannelTable.COL_ID.name() + " = ?",
                    toArray(channelPk));
            if (count != 1) {
                throw new IllegalStateException("Unable to update channel " + channel + ", with pk= " + channelPk);
            }
            return new PersistentChannel(channel, channelPk, subPk);
        }
        values.put(LocalChannelTable.COL_CHANNEL_ID.name(), channel.getChannelId().getRawId());
        long newPk = db.insert(LocalChannelTable.TABLE_NAME, null, values);
        return new PersistentChannel(channel, newPk, subPk);
    }

    private static ContentValues toContentValues(YouTubeChannel channel) {
        ContentValues values = new ContentValues();
        values.put(LocalChannelTable.COL_TITLE, channel.getTitle());
        if (!Utils.isEmpty(channel.getDescription())) {
            values.put(LocalChannelTable.COL_DESCRIPTION, channel.getDescription());
        }
        if (!Utils.isEmpty(channel.getBannerUrl())) {
            values.put(LocalChannelTable.COL_BANNER_URL, channel.getBannerUrl());
        }
        if (!Utils.isEmpty(channel.getThumbnailUrl())) {
            values.put(LocalChannelTable.COL_THUMBNAIL_NORMAL_URL, channel.getThumbnailUrl());
        }
        if (channel.getSubscriberCount() > 0) {
            values.put(LocalChannelTable.COL_SUBSCRIBER_COUNT, channel.getSubscriberCount());
        }
        if(channel.getLastVideoTime() > 0) {
            values.put(LocalChannelTable.COL_LAST_VIDEO_TS, channel.getLastVideoTime());
        }
        if (channel.getLastCheckTime() > 0) {
            values.put(LocalChannelTable.COL_LAST_CHECK_TS, channel.getLastCheckTime());
        }
        return values;
    }

    public List<ChannelView> getSubscribedChannelsByText(String searchText, boolean sortChannelsAlphabetically) {
		List<ChannelView> result = new ArrayList<>();
		try (Cursor cursor = createSubscriptionCursor(searchText, sortChannelsAlphabetically); Stopwatch s = new Stopwatch("search for "+searchText)) {
			final int channelId = cursor.getColumnIndexOrThrow(SubscriptionsTable.COL_CHANNEL_ID);
			final int title = cursor.getColumnIndexOrThrow(LocalChannelTable.COL_TITLE);
			final int thumbnail = cursor.getColumnIndexOrThrow(LocalChannelTable.COL_THUMBNAIL_NORMAL_URL);
			final int colLastVisit = cursor.getColumnIndexOrThrow(SubscriptionsTable.COL_LAST_VISIT_TIME);
			final int colLatestVideoTs = cursor.getColumnIndexOrThrow("latest_video_ts");
			while(cursor.moveToNext()) {
				Long lastVisit = cursor.getLong(colLastVisit);
				Long latestVideoTs = cursor.getLong(colLatestVideoTs);
				boolean hasNew = (latestVideoTs != null && (lastVisit == null || latestVideoTs > lastVisit));
				result.add(new ChannelView(new ChannelId(cursor.getString(channelId)), cursor.getString(title), cursor.getString(thumbnail), hasNew));
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
