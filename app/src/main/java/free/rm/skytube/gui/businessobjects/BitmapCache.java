/*
 * SkyTube
 * Copyright (C) 2015  Ramon Mifsud
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

package free.rm.skytube.gui.businessobjects;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.LruCache;

public class BitmapCache {

	/** Actual cache. */
	private LruCache<String, Bitmap> cache;


	public BitmapCache(Context context) {
		cache = new LruCache<String, Bitmap>(calculateMaxCacheSize(context)) {
			@Override
			protected int sizeOf(String key, Bitmap value) {
				return value.getByteCount();
			}
		};
	}


	/**
	 * Calculated the maximum size (in bytes) this cache is allowed to grow to.
	 *
	 * @param context	Context instance.
	 * @return Maximum cache size in bytes.
	 */
	private int calculateMaxCacheSize(Context context) {
		// get the approximate memory (in bytes) this app is assigned to
		final int maxMem = ((ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass();

		// use 1/8th of the available memory for this memory cache
		return (maxMem * 1024 * 1024) / 8;
	}


	/**
	 * Adds a {@link Bitmap} to cache.
	 *
	 * @param bitmapID The bitmap ID (e.g. URL).
	 * @param bitmap Bitmap instance.
	 */
	public void add(String bitmapID, Bitmap bitmap) {
		// TODO:  if bitmap is null, then add a default thumbnail image...

		if (bitmapID != null  &&  bitmap != null)
			cache.put(bitmapID, bitmap);
	}


	/**
	 * Searches for the bitmap stored in this cache whose ID is equal to bitmapID.
	 *
	 * @param bitmapID Bitmap ID
	 * @return Bitmap instance if found; null otherwise.
	 */
	public Bitmap get(String bitmapID) {
		return cache.get(bitmapID);
	}

}
