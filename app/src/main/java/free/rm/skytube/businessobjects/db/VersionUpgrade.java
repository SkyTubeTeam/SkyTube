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


/**
 * This could be a record, if we were not living in 2010
 */
public class VersionUpgrade {
    private final int oldVersion;
    private final int newVersion;

    public VersionUpgrade(int oldVersion, int newVersion) {
        this.oldVersion = oldVersion;
        this.newVersion = newVersion;
    }

    public boolean executeStep(int upgradeTo) {
        return upgradeTo > 1 && oldVersion <= upgradeTo - 1 && upgradeTo <= newVersion;
    }
}
