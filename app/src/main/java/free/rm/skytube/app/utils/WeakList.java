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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

public class WeakList<X> {
    private List<WeakReference<X>> values = new ArrayList<>();

    public void add(X object) {
        cleanup(null, null);
        values.add(new WeakReference<>(object));
    }

    public void forEach(Consumer<X> consumer) {
        cleanup(consumer, null);
    }

    public void remove(X object) {
        cleanup(null, object);
    }

    private void cleanup(Consumer<X> consumer, X valueToRemove) {
        for (Iterator<WeakReference<X>> iter = values.iterator(); iter.hasNext(); ) {
            WeakReference<X> ref = iter.next();
            X value = ref.get();
            if (value == null || value == valueToRemove) {
                iter.remove();
            } else {
                if (consumer != null) {
                    consumer.accept(value);
                }
            }
        }
    }
}