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

/*
 * Parts of the code below were written by Christian Schabesberger.
 *
 * Copyright (C) Christian Schabesberger 2015 <chris.schabesberger@mailbox.org>
 * Code written by Schabesberger are Licensed under GPL version 3 of the License, or (at your
 * option) any later version.
 */

package free.rm.skytube.businessobjects.VideoStream;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


/**
 * Downloads HTTP content.
 */
public class HttpDownloader {

	/** Mimic the Mozilla user agent */
	private static final String USER_AGENT = "Mozilla/5.0";


	/**
	 * Download (via HTTP) the text file located at the supplied URL, and return its contents.
	 * Primarily intended for downloading web pages.
	 *
	 * @param siteUrl	The URL of the text file to download.
	 *
	 * @return	The contents of the specified text file.
	 */
	public static String download(String siteUrl) throws Exception {
		URL					url = new URL(siteUrl);
		HttpURLConnection	con = (HttpURLConnection) url.openConnection();
		StringBuffer		response = new StringBuffer();
		BufferedReader		in = null;

		try {
			con.setRequestMethod("GET");
			con.setRequestProperty("User-Agent", USER_AGENT);

			in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;

			while((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
		} finally {
			if (in != null)
				in.close();

			if (con != null)
				con.disconnect();
		}

		return response.toString();
	}

}
