/*
 * SkyTube
 * Copyright (C) 2020  Zsombor Gegesy
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
package free.rm.skytube.businessobjects.YouTube.VideoStream;

import androidx.preference.ListPreference;

import free.rm.skytube.R;

public enum VideoQuality {
    LEAST_BANDWITH, BEST_QUALITY;

    /**
     * Configures the given preference to list all video resolution.
     *
     * @param preference
     */
    public static void setupListPreferences(ListPreference preference) {
        preference.setEntries(R.array.pref_network_usage_display);
        preference.setEntryValues(R.array.pref_network_usage);
    }

}
