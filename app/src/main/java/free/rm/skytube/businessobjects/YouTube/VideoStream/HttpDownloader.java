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

package free.rm.skytube.businessobjects.YouTube.VideoStream;

import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.downloader.Request;
import org.schabi.newpipe.extractor.downloader.Response;
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.UnknownHostException;

import javax.annotation.Nonnull;
import javax.net.ssl.HttpsURLConnection;


/**
 * Downloads HTTP content.
 */
public class HttpDownloader extends Downloader {

	/** Mimic the Mozilla user agent */
	private static final String USER_AGENT = "Mozilla/5.0";

	@Override
	public Response execute(@Nonnull Request request) throws IOException, ReCaptchaException {
		URL url = new URL(request.url());
		HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
		BufferedReader in = null;
		StringBuilder response = new StringBuilder();

		try {
			con.setRequestMethod("GET");
			con.setRequestProperty("User-Agent", USER_AGENT);

			in = new BufferedReader(new InputStreamReader(con.getInputStream()));
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
				throw new ReCaptchaException("reCaptcha Challenge requested", url.toString());
			}
			throw new IOException(e);
		} finally {
			if(in != null) {
				in.close();
			}
		}

		return new Response(con.getResponseCode(), con.getResponseMessage(), con.getHeaderFields(), response.toString());
	}

}
