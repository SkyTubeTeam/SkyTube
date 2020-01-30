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
import android.database.sqlite.SQLiteDatabase;

import com.google.api.client.util.DateTime;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.app.Utils;
import free.rm.skytube.businessobjects.Logger;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;
import free.rm.skytube.gui.fragments.SubscriptionsFeedFragment;

/**
 * A database (DB) that stores user subscriptions (with respect to YouTube channels).
 */
public class SubscriptionsDb extends SQLiteOpenHelperEx {
    private static final String CHANNEL_HAS_NEW_VIDEO_QUERY = String.format("SELECT COUNT(*) FROM %s WHERE %s = ? AND %s > ?", SubscriptionsVideosTable.TABLE_NAME, SubscriptionsVideosTable.COL_CHANNEL_ID, SubscriptionsVideosTable.COL_YOUTUBE_VIDEO_DATE);
    private static final String VIDEO_DATE_IS_OLDER_THAN_1_MONTH = String.format("%s < DATETIME('now', '-1 month')", SubscriptionsVideosTable.COL_YOUTUBE_VIDEO_DATE);
    private static final String SUBSCRIBED_NUMBER_OF_CHANNELS_QUERY = String.format("SELECT COUNT(*) FROM %s", SubscriptionsTable.TABLE_NAME);
    private static final String HAS_VIDEO_QUERY = String.format("SELECT COUNT(*) FROM %s WHERE %s = ?", SubscriptionsVideosTable.TABLE_NAME, SubscriptionsVideosTable.COL_YOUTUBE_VIDEO_ID);
	private static final String GET_VIDEO_IDS = String.format("SELECT %s FROM %s", SubscriptionsVideosTable.COL_YOUTUBE_VIDEO_ID, SubscriptionsVideosTable.TABLE_NAME);
	private static final String GET_VIDEO_IDS_BY_CHANNEL = String.format("SELECT %s FROM %s WHERE %s = ?",
			SubscriptionsVideosTable.COL_YOUTUBE_VIDEO_ID, SubscriptionsVideosTable.TABLE_NAME, SubscriptionsVideosTable.COL_CHANNEL_ID);
    private static final String FIND_EMPTY_RETRIEVAL_TS = String.format("SELECT %s FROM %s WHERE %s IS NULL",
            SubscriptionsVideosTable.COL_YOUTUBE_VIDEO, SubscriptionsVideosTable.TABLE_NAME, SubscriptionsVideosTable.COL_RETRIEVAL_TS);

    private static volatile SubscriptionsDb subscriptionsDb = null;

	private static final int DATABASE_VERSION = 5;
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
	protected void clearDatabaseInstance() {
		subscriptionsDb = null;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(SubscriptionsTable.getCreateStatement());
		db.execSQL(SubscriptionsVideosTable.getCreateStatement());
		db.execSQL(LocalChannelTable.getCreateStatement());
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
							!Utils.isEmpty(channel.getThumbnailNormalUrl()) &&
							!Utils.isEmpty(channel.getDescription())) {
						cacheChannel(db, channel);
					}
				}
			} catch (IOException ex) {
				Logger.e(this, "Unable to load subscribed channels to populate cache:" + ex.getMessage(), ex);
			}
		}
	}

	private static void execSQLUpdates(SQLiteDatabase db, String[] sqlUpdates) {
		for (String sqlUpdate : sqlUpdates) {
			db.execSQL(sqlUpdate);
		}
	}

	private void setupRetrievalTimestamp(SQLiteDatabase db) {
        List<YouTubeVideo> videos = extractVideos(db.rawQuery(FIND_EMPTY_RETRIEVAL_TS, null), false);
        int count = 0;
        for (YouTubeVideo video : videos) {
            DateTime dateTime = video.getPublishDate();
            if (dateTime != null) {
                ContentValues values = new ContentValues();
                values.put(SubscriptionsVideosTable.COL_PUBLISH_TS, dateTime.getValue());
                values.put(SubscriptionsVideosTable.COL_RETRIEVAL_TS, dateTime.getValue());
                int updateCount = db.update(
                        SubscriptionsVideosTable.TABLE_NAME,
                        values,
                        SubscriptionsVideosTable.COL_YOUTUBE_VIDEO_ID + " = ?",
                        new String[]{video.getId()});
                Logger.i(this,"updating " + video.getId() + " with publish date:" + dateTime + " -> " + updateCount);
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
	 * @return True if the operation was successful; false otherwise.
	 */
	public boolean subscribe(YouTubeChannel channel) {
		saveChannelVideos(channel);

		return saveSubscription(channel);
	}


	/**
	 * Saves the given channel into the subscriptions DB.
	 *
	 * @param channel The channel the user wants to subscribe to.
	 *
	 * @return True if the operation was successful; false otherwise.
	 */
	private boolean saveSubscription(YouTubeChannel channel) {
		ContentValues values = new ContentValues();
		values.put(SubscriptionsTable.COL_CHANNEL_ID, channel.getId());
		values.put(SubscriptionsTable.COL_LAST_VISIT_TIME, channel.getLastVisitTime());
		values.put(SubscriptionsTable.COL_TITLE, channel.getTitle());
		values.put(SubscriptionsTable.COL_DESCRIPTION, channel.getDescription());
		values.put(SubscriptionsTable.COL_BANNER_URL, channel.getBannerUrl());
		values.put(SubscriptionsTable.COL_THUMBNAIL_NORMAL_URL, channel.getThumbnailNormalUrl());
		values.put(SubscriptionsTable.COL_SUBSCRIBER_COUNT, channel.getSubscriberCount());

		return getWritableDatabase().insert(SubscriptionsTable.TABLE_NAME, null, values) != -1;
	}


	/**
	 * Removes the given channel from the subscriptions DB.
	 *
	 * @param channelId id of the channel the user wants to unsubscribe to.
	 *
	 * @return True if the operation was successful; false otherwise.
	 */
	public boolean unsubscribe(String channelId) {
		// delete any feed videos pertaining to this channel
		getWritableDatabase().delete(SubscriptionsVideosTable.TABLE_NAME,
				SubscriptionsVideosTable.COL_CHANNEL_ID + " = ?",
				new String[]{channelId});

		// remove this channel from the subscriptions DB
		int rowsDeleted = getWritableDatabase().delete(SubscriptionsTable.TABLE_NAME,
				SubscriptionsTable.COL_CHANNEL_ID + " = ?",
				new String[]{channelId});

		// Need to make sure when we come back to MainActivity, that we refresh the Feed tab so it hides videos from the newly unsubscribed
		SubscriptionsFeedFragment.setFlag(SubscriptionsFeedFragment.FLAG_REFRESH_FEED_FROM_CACHE);

		return (rowsDeleted >= 0);
	}

	/**
	 * @return all the video ids for the subscribed channels from the database.
	 */
	public Set<String> getSubscribedChannelVideos() {
		try(Cursor cursor = getReadableDatabase().rawQuery(GET_VIDEO_IDS, null)) {
			Set<String> result = new HashSet<>();
			while(cursor.moveToNext()) {
				result.add(cursor.getString(0));
			}
			return result;
		}
	}

	/**
	 * @param channelId the id of the channel
	 * @return all the video ids for the subscribed channels from the database.
	 */
	public Set<String> getSubscribedChannelVideosByChannel(String channelId) {
		try(Cursor cursor = getReadableDatabase().rawQuery(GET_VIDEO_IDS_BY_CHANNEL, new String[] { channelId})) {
			Set<String> result = new HashSet<>();
			while(cursor.moveToNext()) {
				result.add(cursor.getString(0));
			}
			return result;
		}
	}


	public List<String> getSubscribedChannelIds() {
		try (Cursor cursor = getReadableDatabase().query(SubscriptionsTable.TABLE_NAME, new String[] {SubscriptionsTable.COL_CHANNEL_ID}, null, null, null, null, null)) {
			List<String> result = new ArrayList<>();
			while(cursor.moveToNext()) {
				result.add(cursor.getString(0));
			}
			return result;
		}
	}

	/**
	 * Returns a list of channels that the user subscribed to, without accessing the network
	 *
	 *
	 * @return A list of channels that the user subscribed to.
	 * @throws IOException
	 */
	private List<YouTubeChannel> getSubscribedChannels(SQLiteDatabase db) throws IOException {
		Cursor cursor = null;
		try {
			cursor = db.query(SubscriptionsTable.TABLE_NAME,
					SubscriptionsTable.ALL_COLUMNS,
					null, null,
					null, null,
					SubscriptionsTable.COL_ID + " ASC");

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

				do {
					final String id = cursor.getString(colChannelIdNum);
					subsChannels.add(new YouTubeChannel(id, cursor.getString(colTitle),
							cursor.getString(colDescription), cursor.getString(colThumbnail),
							cursor.getString(colBanner), cursor.getLong(colSubscribers), true, cursor.getLong(colLastVisit),
							cursor.getLong(colLastCheck)));
				} while (cursor.moveToNext());

			}
			return subsChannels;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	public YouTubeChannel getCachedSubscribedChannel(String channelId) throws IOException {
		Cursor cursor = null;
		try {
			cursor = getReadableDatabase().query(SubscriptionsTable.TABLE_NAME,
					SubscriptionsTable.ALL_COLUMNS,
					SubscriptionsTable.COL_CHANNEL_ID + " = ?", new String[] { channelId },
					null, null,null);

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

				final String id = cursor.getString(colChannelIdNum);
				channel = new YouTubeChannel(id, cursor.getString(colTitle),
						cursor.getString(colDescription), cursor.getString(colThumbnail),
						cursor.getString(colBanner), cursor.getLong(colSubscribers), true,
						cursor.getLong(colLastVisit), cursor.getLong(colLastCheck));

			}

			return channel;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}


	/**
	 * @return The total number of subscribed channels.
	 */
	public int getTotalSubscribedChannels() {
		Cursor	cursor = getReadableDatabase().rawQuery(SUBSCRIBED_NUMBER_OF_CHANNELS_QUERY, null);
		int		totalSubbedChannels = 0;

		if (cursor.moveToFirst())
			totalSubbedChannels = cursor.getInt(0);

		cursor.close();
		return totalSubbedChannels;
	}


	/**
	 * Checks if the user is subscribed to the given channel.
	 *
	 * @param channelId	Channel ID
	 * @return True if the user is subscribed; false otherwise.
	 * @throws IOException
	 */
	public boolean isUserSubscribedToChannel(String channelId) {
		Cursor cursor = getReadableDatabase().query(
				SubscriptionsTable.TABLE_NAME,
				new String[]{SubscriptionsTable.COL_ID},
				SubscriptionsTable.COL_CHANNEL_ID + " = ?",
				new String[]{channelId}, null, null, null);
		boolean	isUserSubbed = cursor.moveToNext();

		cursor.close();
		return isUserSubbed;
	}


    /**
     * Updates the given channel's last visit time.
     *
     * @param channelId	Channel ID
     *
     * @return	last visit time, if the update was successful;  -1 otherwise.
     */
    public long updateLastVisitTime(String channelId) {
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
		values.put(SubscriptionsTable.COL_THUMBNAIL_NORMAL_URL, channel.getThumbnailNormalUrl());
		values.put(SubscriptionsTable.COL_SUBSCRIBER_COUNT, channel.getSubscriberCount());
		values.put(SubscriptionsTable.COL_LAST_VISIT_TIME, channel.getLastVisitTime());

		int count = db.update(
				SubscriptionsTable.TABLE_NAME,
				values,
				SubscriptionsTable.COL_CHANNEL_ID + " = ?",
				new String[]{channel.getId()});
		return count > 0;
	}

	/**
	 * Returns the last time the user has visited this channel.
	 *
	 * @param channel
	 *
	 * @return	last visit time, if the update was successful;  -1 otherwise.
	 * @throws IOException
	 */
	public long getLastVisitTime(YouTubeChannel channel) {
		Cursor	cursor = getReadableDatabase().query(
							SubscriptionsTable.TABLE_NAME,
							new String[]{SubscriptionsTable.COL_LAST_VISIT_TIME},
							SubscriptionsTable.COL_CHANNEL_ID + " = ?",
							new String[]{channel.getId()}, null, null, null);
		long	lastVisitTime = -1;

		if (cursor.moveToNext()) {
			int colLastVisitTIme = cursor.getColumnIndexOrThrow(SubscriptionsTable.COL_LAST_VISIT_TIME);
			lastVisitTime = cursor.getLong(colLastVisitTIme);
		}

		cursor.close();
		return lastVisitTime;
	}


	private boolean hasVideo(YouTubeVideo video) {
		Cursor cursor = null;
		try {
			cursor = getReadableDatabase().rawQuery(HAS_VIDEO_QUERY, new String[]{video.getId()});
			if (cursor.moveToFirst()) {
				return cursor.getInt(0) > 0;
			}
			return false;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}


	/**
	 * Check if the given channel has new videos (by looking into the {@link SubscriptionsVideosTable}
	 * [i.e. video cache table]).
	 *
	 * @param channel Channel to check.
	 *
	 * @return True if the user hasn't visited the channel and new videos have been uploaded in the
	 * meantime; false otherwise.
	 */
	public boolean channelHasNewVideos(YouTubeChannel channel) {
		//Unfortunately, this doesn't work, due to XXX is not supported on this API level
		// String formatted = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US).format(channel.getLastVisitTime());
		String formatted = new DateTime(channel.getLastVisitTime()).toString();
		Cursor cursor = getReadableDatabase().rawQuery(CHANNEL_HAS_NEW_VIDEO_QUERY,
							new String[]{channel.getId(), formatted});
		boolean channelHasNewVideos = false;

		if (cursor.moveToFirst()) {
			channelHasNewVideos = cursor.getInt(0) > 0;
		}

		cursor.close();
		return channelHasNewVideos;
	}

	/**
	 * Loop through each video saved in the passed {@link YouTubeChannel} and save it into the database, if it's not already been saved
	 * @param channel
	 */
	public void saveChannelVideos(YouTubeChannel channel) {
		for (YouTubeVideo video : channel.getYouTubeVideos()) {
			if(video.getPublishDate() != null && !hasVideo(video)) {
                ContentValues values = createContentValues(video, channel.getId());
                getWritableDatabase().insert(SubscriptionsVideosTable.TABLE_NAME, null, values);
			}
		}
	}

	/**
	 * Insert or update the list of videos
	 * @param videos
	 */
	public void saveVideos(List<YouTubeVideo> videos) {
		SQLiteDatabase db = getWritableDatabase();
		for (YouTubeVideo video : videos) {
			if (video.getPublishDate() != null) {
				ContentValues values = createContentValues(video, video.getChannelId());
				if (hasVideo(video)) {
					values.remove(SubscriptionsVideosTable.COL_YOUTUBE_VIDEO_ID);
					db.update(SubscriptionsVideosTable.TABLE_NAME, values,
							SubscriptionsVideosTable.COL_YOUTUBE_VIDEO_ID + " = ?",
							new String[]{video.getId()});
				} else {
					db.insert(SubscriptionsVideosTable.TABLE_NAME, null, values);
				}
			}
		}
	}

	/**
	 * Insert videos into the subscription video table.
	 * @param videos
	 */
	public void insertVideos(List<YouTubeVideo> videos) {
		SQLiteDatabase db = getWritableDatabase();
		for (YouTubeVideo video : videos) {
			if (video.getPublishDate() != null) {
				ContentValues values = createContentValues(video, video.getChannelId());
				db.insert(SubscriptionsVideosTable.TABLE_NAME, null, values);
			}
		}
	}

    private ContentValues createContentValues(YouTubeVideo video, String channelId) {
        ContentValues values = new ContentValues();
        values.put(SubscriptionsVideosTable.COL_CHANNEL_ID, channelId);
        values.put(SubscriptionsVideosTable.COL_YOUTUBE_VIDEO_ID, video.getId());
        values.put(SubscriptionsVideosTable.COL_YOUTUBE_VIDEO, gson.toJson(video).getBytes());
        DateTime publishDate = video.getPublishDate();
        values.put(SubscriptionsVideosTable.COL_YOUTUBE_VIDEO_DATE, publishDate.toString());
        long ts = video.getRetrievalTimestamp() != null ? video.getRetrievalTimestamp() : publishDate.getValue();
        values.put(SubscriptionsVideosTable.COL_RETRIEVAL_TS, ts);
        values.put(SubscriptionsVideosTable.COL_PUBLISH_TS, publishDate.getValue());

        return values;
    }

	/**
	 * Delete any videos stored in the database (for subscribed channels) that are over a month old.
	 * @return
	 */
	public boolean trimSubscriptionVideos() {
		int result = getWritableDatabase().delete(SubscriptionsVideosTable.TABLE_NAME, VIDEO_DATE_IS_OLDER_THAN_1_MONTH, null);
		return result > 0;
	}

	/**
	 * Query the database to retrieve all videos for subscribed channels.
	 * @return
	 */
	public List<YouTubeVideo> getSubscriptionVideos() {
		Cursor	cursor = getReadableDatabase().query(
							SubscriptionsVideosTable.TABLE_NAME,
							SubscriptionsVideosTable.ALL_COLUMNS_FOR_EXTRACT,
							null, null, null, null,
							SubscriptionsVideosTable.COL_YOUTUBE_VIDEO_DATE + " DESC");
		return extractVideos(cursor, true);
	}

    /**
     * Query the database to retrieve number of videos for subscribed channels starting from the given video.
     * @return a list of {@link YouTubeVideo}
     */
    public List<YouTubeVideo> getSubscriptionVideoPage(int limit, String videoId, long beforeTimestamp) {
        final String selection;
        final String[] selectionArguments;
        if (videoId != null) {
            selection = "(" + SubscriptionsVideosTable.COL_RETRIEVAL_TS + " < ?) OR (" + SubscriptionsVideosTable.COL_RETRIEVAL_TS + " = ? AND " + SubscriptionsVideosTable.COL_YOUTUBE_VIDEO_ID + " > ?)";
            String formatted = String.valueOf(beforeTimestamp);
            selectionArguments = new String[]{ formatted, formatted, videoId };
        } else {
            selection = null;
            selectionArguments = null;
        }
        Cursor	cursor = getReadableDatabase().query(
            SubscriptionsVideosTable.TABLE_NAME,
            SubscriptionsVideosTable.ALL_COLUMNS_FOR_EXTRACT,
            selection, selectionArguments, null, null,
            SubscriptionsVideosTable.COL_RETRIEVAL_TS + " DESC, " + SubscriptionsVideosTable.COL_YOUTUBE_VIDEO_ID + " ASC",
            String.valueOf(limit));
        return extractVideos(cursor, true);
    }

    private Gson createGson() {
    	return new GsonBuilder().registerTypeAdapter(YouTubeChannel.class, new JsonSerializer<YouTubeChannel>() {

			@Override
			public JsonElement serialize(YouTubeChannel src, Type typeOfSrc, JsonSerializationContext context) {
				JsonObject obj = new JsonObject();
				obj.addProperty("id", src.getId());
				obj.addProperty("title", src.getTitle());
				obj.addProperty("description", src.getDescription());
				obj.addProperty("thumbnailNormalUrl", src.getThumbnailNormalUrl());
				obj.addProperty("bannerUrl", src.getBannerUrl());
				return obj;
			}
		}).create();
	}

    /**
     * Load YouTubeVideo objects from a cursor, only SubscriptionsVideosTable.COL_YOUTUBE_VIDEO column is needed.
     * @param cursor the cursor to process
     * @param fullColumnList get all the columns, not just the JSON blob - set to false only for db
     *                       maintenance queries!
     */
    private List<YouTubeVideo> extractVideos(Cursor cursor, boolean fullColumnList) {
        List<YouTubeVideo> videos = new ArrayList<>();
        try {

            if (cursor.moveToNext()) {
                final int jsonIdx = cursor.getColumnIndex(SubscriptionsVideosTable.COL_YOUTUBE_VIDEO);
                final int retrievalIdx = fullColumnList ? cursor.getColumnIndex(SubscriptionsVideosTable.COL_RETRIEVAL_TS) : -1;
                final int publishTsIdx = fullColumnList ? cursor.getColumnIndex(SubscriptionsVideosTable.COL_PUBLISH_TS) : -1;

                do {
                    final byte[] blob = cursor.getBlob(jsonIdx);
                    final String videoJson = new String(blob);

                    // convert JSON into YouTubeVideo
                    YouTubeVideo video = gson.fromJson(videoJson, YouTubeVideo.class);
                    if (fullColumnList) {
                        video.setRetrievalTimestamp(cursor.getLong(retrievalIdx));
                        video.setPublishTimestamp(cursor.getLong(publishTsIdx));
                    }
                    video.updatePublishTimestampFromDate();

                    // due to upgrade to YouTubeVideo (by changing channel{Id,Name} to
                    // YouTubeChannel)
                    // from version 2.82 to 2.90
                    if (video.getChannel() == null) {
                        try {
                            JSONObject videoJsonObj = new JSONObject(videoJson);
                            final String channelId = videoJsonObj.get("channelId").toString();
                            final String channelName = videoJsonObj.get("channelName").toString();
                            video.setChannel(new YouTubeChannel(channelId, channelName));
                        } catch (JSONException e) {
                            Logger.e(this, "Error occurred while extracting channel{Id,Name} from JSON", e);
                        }
                    }
                    // regenerate the video's PublishDatePretty (e.g. 5 hours ago)
                    video.forceRefreshPublishDatePretty();
                    // add the video to the list
                    videos.add(video);
                } while (cursor.moveToNext());
            }
        } finally {
            cursor.close();
        }
        return videos;
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
				String thumnbail = cursor.getString(cursor.getColumnIndexOrThrow(LocalChannelTable.COL_THUMBNAIL_NORMAL_URL));
				String banner = cursor.getString(cursor.getColumnIndexOrThrow(LocalChannelTable.COL_BANNER_URL));
				long subscriberCount = cursor.getLong(cursor.getColumnIndexOrThrow(LocalChannelTable.COL_SUBSCRIBER_COUNT));
				long lastCheckTs = cursor.getLong(cursor.getColumnIndexOrThrow(LocalChannelTable.COL_LAST_CHECK_TS));
				return new YouTubeChannel(channelId, title, description, thumnbail, banner, subscriberCount, false, -1, lastCheckTs);
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
		return cacheChannel(getWritableDatabase(), channel);
	}

	private boolean cacheChannel(SQLiteDatabase db, YouTubeChannel channel) {
		ContentValues values = new ContentValues();
		values.put(LocalChannelTable.COL_TITLE, channel.getTitle());
		values.put(LocalChannelTable.COL_DESCRIPTION, channel.getDescription());
		values.put(LocalChannelTable.COL_BANNER_URL, channel.getBannerUrl());
		values.put(LocalChannelTable.COL_THUMBNAIL_NORMAL_URL, channel.getThumbnailNormalUrl());
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

}
