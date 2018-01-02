/*
 * SkyTube
 * Copyright (C) 2017  Ramon Mifsud
 *
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
 *
 *
 * Parts of the code below were written by Christian Schabesberger.
 *
 * Copyright (C) Christian Schabesberger 2015 <chris.schabesberger@mailbox.org>
 * Code written by Schabesberger is licensed under GPL version 3 of the License, or (at your
 * option) any later version.
 */

package free.rm.skytube.businessobjects.YouTube.VideoStream;

import android.util.Log;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * A parser for the class {@link ParseStreamMetaData}.
 */
class StreamMetaDataParser {

	private static final String TAG = StreamMetaDataParser.class.getSimpleName();


	public static String matchGroup1(String pattern, String input) throws RegexException {
		return matchGroup(pattern, input, 1);
	}

	public static String matchGroup(String pattern, String input, int group) throws RegexException {
		Pattern pat = Pattern.compile(pattern);
		Matcher mat = pat.matcher(input);
		boolean foundMatch = mat.find();
		if (foundMatch) {
			return mat.group(group);
		}
		else {
			Log.w(TAG, "failed to find pattern \""+pattern+"\" inside of \""+input+"\"");
			new Exception("failed to find pattern \""+pattern+"\"").printStackTrace();
			return "";
		}
	}


	/**
	 * Exception that shows that a problem occurred when parsing.
	 */
	public static class RegexException extends Exception {
		public RegexException(String message) {
			super(message);
		}
	}
}
