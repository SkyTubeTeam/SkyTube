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

import android.database.sqlite.SQLiteDatabase;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.withSettings;

/**
 * Tests for making subscription_videos.subs_id mandatory.
 */
public class SubscriptionsDbMigrationTest {

    @Test
    void newFeedTableHasMandatorySubsId() {
        SQLiteDatabase sqlite = mock(SQLiteDatabase.class);

        SubscriptionsVideosTable.addNewFlatTable(sqlite, false, true);

        verify(sqlite).execSQL(argThat(sql -> sql.contains("CREATE TABLE subscription_videos")
                && sql.contains("subs_id integer not null")));
    }

    @Test
    void upgradeTo19BackfillsSubsIdBeforeEnforcingIt() {
        SubscriptionsDb db = mock(SubscriptionsDb.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
        SQLiteDatabase sqlite = mock(SQLiteDatabase.class);

        db.onUpgrade(sqlite, 18, 19);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(sqlite, atLeastOnce()).execSQL(sql.capture());
        List<String> statements = sql.getAllValues();
        int backfillByChannelPk = find(statements, "update subscription_videos set subs_id", "channel_pk");
        int backfillByChannelId = find(statements, "update subscription_videos set subs_id", "Channel_Id");
        int deleteOrphans = find(statements, "delete from subscription_videos where subs_id is null");
        int rebuild = find(statements, "CREATE TABLE subscription_videos", "subs_id integer not null");

        assertTrue(backfillByChannelPk >= 0, "Missing subs_id backfill from channel_pk: " + statements);
        assertTrue(backfillByChannelId >= 0, "Missing fallback backfill from channel id: " + statements);
        assertTrue(deleteOrphans > backfillByChannelId, "Orphaned feed videos must be removed after backfill");
        assertTrue(rebuild > deleteOrphans, "Cleanup must happen before the table is rebuilt");
    }

    private static int find(List<String> statements, String... parts) {
        for (int i = 0; i < statements.size(); i++) {
            String statement = statements.get(i);
            boolean matches = true;
            for (String part : parts) {
                matches &= statement.contains(part);
            }
            if (matches) {
                return i;
            }
        }
        return -1;
    }
}
