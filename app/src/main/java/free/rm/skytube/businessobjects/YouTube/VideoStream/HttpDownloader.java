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

package free.rm.skytube.businessobjects.YouTube.VideoStream;

import org.schabi.newpipe.extractor.Downloader;
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;


/**
 * Downloads HTTP content.
 */
public class HttpDownloader implements Downloader {

	/** Mimic the Mozilla user agent */
	private static final String USER_AGENT = "Mozilla/5.0";


	@Override
	public String download(String siteUrl, String language) throws IOException, ReCaptchaException {
		Map<String, String> requestProperties = new HashMap<>();
		requestProperties.put("Accept-Language", language);
		return download(siteUrl, requestProperties);
	}

	@Override
	public String download(String siteUrl, Map<String, String> customProperties) throws IOException, ReCaptchaException {
		URL url = new URL(siteUrl);
		HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
		BufferedReader in = null;
		StringBuilder response = new StringBuilder();

		try {
			con.setRequestMethod("GET");
			con.setRequestProperty("User-Agent", USER_AGENT);

			in = new BufferedReader(
					new InputStreamReader(con.getInputStream()));
			String inputLine;

			while((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
		} catch(UnknownHostException uhe) {//thrown when there's no internet connection
			throw new IOException("unknown host or no network", uhe);
		} catch(Exception e) {
            /*
             * HTTP 429 == Too Many Request
             * Receive from Youtube.com = ReCaptcha challenge request
             * See : https://github.com/rg3/youtube-dl/issues/5138
             */
			if (con.getResponseCode() == 429) {
				throw new ReCaptchaException("reCaptcha Challenge requested");
			}
			throw new IOException(e);
		} finally {
			if(in != null) {
				in.close();
			}
		}

		return response.toString();
	}

	@Override
	public String download(String siteUrl) throws IOException, ReCaptchaException {
		Map<String, String> requestProperties = new HashMap<>();
		return download(siteUrl, requestProperties);
	}

}
