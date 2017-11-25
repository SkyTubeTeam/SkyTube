package free.rm.skytube.gui.businessobjects;

import android.util.Log;

public class Logger {

	public static void i(Object obj, String format, Object ... args) {
		String msg = String.format(format, args);
		Log.i(obj.getClass().getSimpleName(), msg);
	}

	public static void d(Object obj, String format, Object ... args) {
		String msg = String.format(format, args);
		Log.d(obj.getClass().getSimpleName(), msg);
	}

	public static void w(Object obj, String format, Object ... args) {
		String msg = String.format(format, args);
		Log.w(obj.getClass().getSimpleName(), msg);
	}

	public static void e(Object obj, String msg, Throwable tr) {
		Log.e(obj.getClass().getSimpleName(), msg, tr);
	}

	public static void e(Object obj, String format, Object ... args) {
		String msg = String.format(format, args);
		Log.e(obj.getClass().getSimpleName(), msg);
	}

}