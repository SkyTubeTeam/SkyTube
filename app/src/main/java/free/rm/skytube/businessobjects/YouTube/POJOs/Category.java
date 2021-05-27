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

package free.rm.skytube.businessobjects.YouTube.POJOs;

import com.mikepenz.iconics.typeface.library.materialdesigniconic.MaterialDesignIconic;

public class Category {
    private final Long id;
    private final boolean enabled;
    private final boolean builtin;
    private final String label;
    private final MaterialDesignIconic.Icon icon;
    private final int priority;

    public Category(Long id, boolean enabled, boolean builtin, String label, MaterialDesignIconic.Icon icon, int priority) {
        this.id = id;
        this.enabled = enabled;
        this.builtin = builtin;
        this.label = label;
        this.icon = icon;
        this.priority = priority;
    }

    public Long getId() {
        return id;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isBuiltin() {
        return builtin;
    }

    public String getLabel() {
        return label;
    }

    public MaterialDesignIconic.Icon getIcon() {
        return icon;
    }

    public int getPriority() {
        return priority;
    }
}
