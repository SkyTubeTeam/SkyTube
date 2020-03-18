/*
 * SkyTube
 * Copyright (C) 2019  Zsombor Gegesy
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
package free.rm.skytube.businessobjects.utils;

import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;

/**
 * TODO, remove when old Android support can be dropped!
 */
public interface Predicate<E> {
    boolean test(E object);

    default void removeIf(Collection<E> collection) {
        Objects.requireNonNull(collection);
        final Iterator<E> each = collection.iterator();
        while (each.hasNext()) {
            if (test(each.next())) {
                each.remove();
            }
        }
    }

    /**
     * Remove everything after the predicate returns true.
     * @param collection
     */
    default void removeAfter(Collection<E> collection) {
        Objects.requireNonNull(collection);
        final Iterator<E> each = collection.iterator();
        boolean after = false;
        while (each.hasNext()) {
            E object = each.next();
            if (after || test(object)) {
                each.remove();
                after = true;
            }
        }
    }

}
