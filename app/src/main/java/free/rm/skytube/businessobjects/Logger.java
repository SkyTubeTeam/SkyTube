/*
 * SkyTube
 * Copyright (C) 2018  Ramon Mifsud
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

package free.rm.skytube.businessobjects;

import android.util.Log;

public class Logger {

	public static void i(Object obj, String format, Object ... args) {
		Log.i(obj.getClass().getSimpleName(), format(format, args));
	}

	public static void d(Object obj, String format, Object ... args) {
		Log.d(obj.getClass().getSimpleName(), format(format, args));
	}

	public static void w(Object obj, String format, Object ... args) {
		Log.w(obj.getClass().getSimpleName(), format(format, args));
	}

	public static void e(Object obj, String msg, Throwable tr) {
		Log.e(obj.getClass().getSimpleName(), msg, tr);
	}

	public static void e(Object obj, String format, Object ... args) {
		Log.e(obj.getClass().getSimpleName(), format(format, args));
	}

	private static String format(String format, Object... args) {
		return args.length > 0 ? String.format(format, args) : format;
	}
}