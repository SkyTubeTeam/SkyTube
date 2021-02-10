/*
 * SkyTube
 * Copyright (C) 2021  Zsombor Gegesy
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

package free.rm.skytube.app;

import free.rm.skytube.app.utils.WeakList;
import free.rm.skytube.gui.fragments.MainFragment;

public class EventBus {
    public enum SettingChange {
        HIDE_TABS,
        CONTENT_COUNTRY
    }
    private static EventBus instance;

    private WeakList<MainFragment> mainFragments = new WeakList<>();

    public static synchronized EventBus getInstance() {
        if (instance == null) {
            instance = new EventBus();
        }
        return instance;
    }

    public void registerMainFragment(MainFragment mainFragment) {
        this.mainFragments.add(mainFragment);
    }

    public void notifyMainTabChanged(SettingChange settingChange) {
        this.mainFragments.forEach(main -> main.refreshTabs(settingChange));
    }
}
