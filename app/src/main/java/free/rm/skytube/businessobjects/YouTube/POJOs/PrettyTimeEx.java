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

import com.google.api.client.util.DateTime;

import org.ocpsoft.prettytime.PrettyTime;

import java.util.Date;

/**
 * An extension of {@link PrettyTime}.
 */
public class PrettyTimeEx extends PrettyTime {

	/**
	 * It will convert {@link DateTime} to a pretty string.
	 *
	 * @see PrettyTime#format(Date)
	 */
	public String format(DateTime dateTime) {
		long unixEpoch = dateTime.getValue();
		Date date = new Date(unixEpoch);
		return format(date);
	}

	/**
	 * It will convert the timestamp - since unix epoch to a pretty string.
	 *
	 * @see PrettyTime#format(Date)
	 */
	public String format(long unixEpoch) {
		return format(new Date(unixEpoch));
	}

}
