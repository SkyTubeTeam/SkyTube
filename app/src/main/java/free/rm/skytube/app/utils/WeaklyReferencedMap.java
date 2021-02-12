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
package free.rm.skytube.app.utils;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

/**
 * Not thread-safe Map-like structure, where the value can be safely GC-d. Can be used for caching.
 * @param <Key>
 * @param <Value>
 */
public class WeaklyReferencedMap<Key, Value> {
    private final Map<Key, WeakReference<Value>> internal = new HashMap<>();

    public Value get(Key key) {
        if (key == null) {
            return null;
        }
        WeakReference<Value> ref = internal.get(key);
        if (ref != null) {
            Value result = ref.get();
            if (result == null) {
                internal.remove(key);
            }
            return result;
        }
        return null;
    }

    public void put(Key key, Value value) {
        if (key != null && value != null) {
            internal.put(key, new WeakReference<>(value));
        }
    }

    public Value remove(Key key) {
        WeakReference<Value> ref = internal.remove(key);
        if (ref != null) {
            return ref.get();
        }
        return null;
    }

    public synchronized void clear() {
        internal.clear();
    }
}
