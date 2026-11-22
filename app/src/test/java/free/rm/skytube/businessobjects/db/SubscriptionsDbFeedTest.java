/*
 * SkyTube
 * Copyright (C) 2026  SkyTube contributors
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
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import java.util.Collections;

import free.rm.skytube.app.Settings;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.YouTube.POJOs.PersistentChannel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;
import free.rm.skytube.businessobjects.model.Status;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * Tests that feed videos are always tied to the subscription they belong to.
 */
public class SubscriptionsDbFeedTest {
    private static final String CHANNEL_ID = "UCabcdefghijklmnopqrstuv";
    private static final long CHANNEL_PK = 7L;
    private static final long SUBSCRIPTION_PK = 42L;

    private SubscriptionsDb db;
    private SQLiteDatabase writableDb;
    private SQLiteDatabase readableDb;

    @BeforeEach
    void setUp() {
        db = mock(SubscriptionsDb.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
        writableDb = mock(SQLiteDatabase.class);
        readableDb = mock(SQLiteDatabase.class);
        when(db.getWritableDatabase()).thenReturn(writableDb);
        when(db.getReadableDatabase()).thenReturn(readableDb);
    }

    @Test
    void subscribeSavesVideosWithTheNewSubscriptionId() {
        when(writableDb.insertWithOnConflict(eq(SubscriptionsTable.TABLE_NAME), isNull(),
                any(ContentValues.class), eq(SQLiteDatabase.CONFLICT_IGNORE))).thenReturn(SUBSCRIPTION_PK);
        when(readableDb.rawQuery(anyString(), any())).thenAnswer(invocation -> {
            Cursor cursor = mock(Cursor.class);
            if (invocation.<String>getArgument(0).contains("FROM " + SubscriptionsTable.TABLE_NAME)) {
                when(cursor.moveToFirst()).thenReturn(true);
                when(cursor.getLong(0)).thenReturn(SUBSCRIPTION_PK);
            }
            return cursor;
        });

        try (MockedConstruction<ContentValues> valuesConstruction = mockConstruction(ContentValues.class)) {
            assertEquals(DatabaseResult.SUCCESS, db.subscribe(channel(null), Collections.singletonList(video())));

            InOrder order = inOrder(writableDb);
            order.verify(writableDb).insertWithOnConflict(eq(SubscriptionsTable.TABLE_NAME), isNull(),
                    any(ContentValues.class), eq(SQLiteDatabase.CONFLICT_IGNORE));
            ArgumentCaptor<ContentValues> videoValues = ArgumentCaptor.forClass(ContentValues.class);
            order.verify(writableDb).insert(eq(SubscriptionsVideosTable.TABLE_NAME_V2), isNull(), videoValues.capture());

            assertTrue(valuesConstruction.constructed().contains(videoValues.getValue()));
            verify(videoValues.getValue()).put(eq(SubscriptionsVideosTable.COL_SUBS_ID.name()), eq(SUBSCRIPTION_PK));
        }
    }

    @Test
    void videosAreNotSavedWithoutSubscriptionId() {
        db.saveChannelVideos(Collections.singletonList(video()), channel(null), false);

        verify(writableDb, never()).insert(eq(SubscriptionsVideosTable.TABLE_NAME_V2), isNull(), any(ContentValues.class));
    }

    @Test
    void latestVideoTimestampIsStoredAgainstTheChannel() {
        LocalChannelTable.updateLatestVideoTimestamp(writableDb, channel(SUBSCRIPTION_PK), 1_000L);

        ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
        verify(writableDb).execSQL(anyString(), args.capture());
        assertEquals(CHANNEL_PK, args.getValue()[1]);
    }

    @Test
    void unsubscribeDeletesVideosWithoutSubscriptionId() {
        Settings settings = mock(Settings.class);
        try (MockedStatic<SkyTubeApp> app = mockStatic(SkyTubeApp.class)) {
            app.when(SkyTubeApp::getSettings).thenReturn(settings);

            assertEquals(DatabaseResult.SUCCESS, db.unsubscribe(channel(null)));

            ArgumentCaptor<String> selection = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String[]> args = ArgumentCaptor.forClass(String[].class);
            verify(writableDb).delete(eq(SubscriptionsVideosTable.TABLE_NAME_V2), selection.capture(), args.capture());
            assertTrue(selection.getValue().contains(SubscriptionsVideosTable.COL_CHANNEL_ID_V2.name()),
                    "Deletion must also match videos saved with a NULL subs_id");
            assertEquals(CHANNEL_ID, args.getValue()[2]);
        }
    }

    private static PersistentChannel channel(Long subscriptionPk) {
        return new PersistentChannel(new YouTubeChannel(CHANNEL_ID, "Channel"), CHANNEL_PK, subscriptionPk, Status.OK);
    }

    private static YouTubeVideo video() {
        YouTubeVideo video = new YouTubeVideo();
        video.setId("video-1");
        video.setPublishTimestamp(1_000L);
        return video;
    }
}
