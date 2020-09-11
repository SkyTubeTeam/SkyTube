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

package free.rm.skytube.businessobjects.YouTube.POJOs;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A class that converts ISO 8601 Durations into a human readable strings.
 *
 * <p>This implementation is not intended to be 100% compliant with the ISO specification, nor is it
 * intended to fool-proof.  It assumes that YouTube will supply correct format; however it should
 * decently handle incorrect formats.
 */
public class VideoDuration {

	/**
	 * Converts number of seconds to a human readable string.  If the supplied
	 * duration is greater than 1 day, then it will return "inf.".
	 *
	 * @param numberOfSeconds number of seconds to convert.
	 * @return Human readable string
	 */
	public static String toHumanReadableString(int numberOfSeconds) {
		StringBuilder s = new StringBuilder();
		final int hours = numberOfSeconds / 3600;
		if (hours >= 24) {
			return "inf.";
		}
		numberOfSeconds -= (hours * 3600);
		final int minutes = numberOfSeconds / 60;
		final int seconds = numberOfSeconds % 60;
		if (hours > 0) {
			s.append(hours).append(':');
			if (minutes < 10) {
				s.append('0');
			}
		}
		s.append(minutes);
		s.append(':');
		if (seconds < 10) {
			s.append('0');
		}
		s.append(seconds);
		return s.toString();
	}

	/**
	 * Converts the supplies ISO 8601 duration into a human readable string.  If the supplied
	 * duration is greater than 1 day, then it will return "inf.".
	 *
	 * @param iso8601Duration ISO 8601 duration as defined <a href="https://en.wikipedia.org/wiki/ISO_8601#Durations">here</a>.
	 * @return Human readable string
	 */
	public static String toHumanReadableString(final String iso8601Duration) {
		/** Human readable duration string */
		String str = "";

		// if there are Year (represented by 'Y'), Month or Day fields, then stop parsing (as the
		// duration is highly unlikely for a YouTube Video)
		if (iso8601Duration.matches("P\\d+[YMD].+")) {
			str = "inf.";
		} else {
			// extract hours, minutes and seconds from the iso8601 string
			String	hours   = getHours(iso8601Duration),		// can be null
					minutes = getMinutes(iso8601Duration),		// cannot be null
					seconds = getSeconds(iso8601Duration);		// cannot be null

			// construct the human readable duration string
			if (hours != null) {
				str = hours;

				// since hours is present, then make sure that the minutes is at least 2 digits long
				if (minutes != null  &&  minutes.length() == 1) {
					minutes = "0" + minutes;	// append a '0' to the minutes
				}
			}

			// append minutes
			if (str.isEmpty())
				str = minutes;
			else
				str += ":" + minutes;

			// append seconds
			if (str.isEmpty())
				str = seconds;
			else
				str += ":" + seconds;
		}

		return str;
	}


	private static String getHours(String iso8601Duration) {
		return get(iso8601Duration, 'H');
	}


	private static String getMinutes(String iso8601Duration) {
		String minutes = get(iso8601Duration, 'M');
		return minutes != null ? minutes : "0";
	}


	private static String getSeconds(String iso8601Duration) {
		String seconds = get(iso8601Duration, 'S');

		// always assure that the seconds are made up of at least two digits
		if (seconds != null) {
			if (seconds.length() == 1) {
				seconds = "0" + seconds;
			}
		} else {
			seconds = "00";
		}

		return seconds;
	}


	/**
	 * Extracts hours, minutes and seconds from the given iso8601Duration string.
	 *
	 * @param iso8601Duration ISO duration string.
	 * @param which	A character which specifies which information are we going to extract,
	 *              i.e. 'H' for hours, 'M' for minutes and 'S' for seconds.
	 * @return Extracted information as string or null.
	 */
	private static String get(String iso8601Duration, char which) {
		if (which != 'H'  &&  which != 'M'  &&  which != 'S')
			throw new IllegalArgumentException("Expecting 'H'/'M'/'S'; got " + which);

		Matcher m = Pattern.compile("(\\d+)" + which).matcher(iso8601Duration);
		String	res = null;

		if (m.find()) {
			res = m.group(1);
		}

		return res;
	}
}
