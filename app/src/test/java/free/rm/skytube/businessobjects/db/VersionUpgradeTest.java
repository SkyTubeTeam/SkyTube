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
package free.rm.skytube.businessobjects.db;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class VersionUpgradeTest {
    @Test
    void testUpgradeFrom1() {
        VersionUpgrade upgrade = new VersionUpgrade(1, 4);
        check(false, upgrade, 1);
        check(true, upgrade, 2);
        check(true, upgrade, 3);
        check(true, upgrade, 4);
        check(false, upgrade, 5);
    }

    @Test
    void testUpgradeFrom2() {
        VersionUpgrade upgrade = new VersionUpgrade(2, 4);
        check(false, upgrade, 1);
        check(false, upgrade, 2);
        check(true, upgrade, 3);
        check(true, upgrade, 4);
        check(false, upgrade, 5);
    }

    @Test
    void testUpgradeFrom3() {
        VersionUpgrade upgrade = new VersionUpgrade(3, 4);
        check(false, upgrade, 1);
        check(false, upgrade, 2);
        check(false, upgrade, 3);
        check(true, upgrade, 4);
        check(false, upgrade, 5);
    }

    void check(boolean expected, VersionUpgrade upgrade, int step) {
        Assertions.assertEquals(expected, upgrade.executeStep(step), String.format("Step %s expected to happen:%s", step, expected));
    }
}
