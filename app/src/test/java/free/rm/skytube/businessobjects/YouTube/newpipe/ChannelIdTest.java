/*
 * SkyTube
 * Copyright (C) 2023  Zsombor Gegesy
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
package free.rm.skytube.businessobjects.YouTube.newpipe;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ChannelIdTest {

    @Test
    void testDifference() {
        ChannelId id = new ChannelId("UCzv_x7BkLO4uxOzPkuijTjA");
        ChannelId id2 = new ChannelId("UCF6BQExwkd4Fmk1Nx_G6Fmw");
        Assertions.assertNotEquals(id, id2, "ChannelId.equals works");
        Assertions.assertNotEquals(id.hashCode(), id2.hashCode(), "ChannelId.hashcode works");
        Assertions.assertNotEquals(id.getRawId(), id2.getRawId(), "ChannelId.getRawId works");
    }

    @Test
    void testEquality() {
        ChannelId id = new ChannelId("UCzv_x7BkLO4uxOzPkuijTjA");
        ChannelId id2 = new ChannelId("UCzv_x7BkLO4uxOzPkuijTjA");
        assertEquals(id, id2);
    }

    @Test
    void testConversionFromUrl() {
        ChannelId id = new ChannelId("https://www.youtube.com/channel/UCF6BQExwkd4Fmk1Nx_G6Fmw");
        ChannelId id2 = new ChannelId("UCF6BQExwkd4Fmk1Nx_G6Fmw");
        assertEquals(id, id2);
    }

    @Test
    void testConversion() {
        ChannelId id = new ChannelId("channel/UCbx1TZgxfIauUZyPuBzEwZg");
        ChannelId id2 = new ChannelId("UCbx1TZgxfIauUZyPuBzEwZg");
        assertEquals(id, id2);
    }

    private void assertEquals(ChannelId id, ChannelId id2) {
        Assertions.assertEquals(id, id2, "ChannelId.equals works");
        Assertions.assertEquals(id.hashCode(), id2.hashCode(), "ChannelId.hashcode works");
        Assertions.assertEquals(id.getRawId(), id2.getRawId(), "ChannelId.getRawId works");
    }
}
