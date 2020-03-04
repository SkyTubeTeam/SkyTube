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

import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.downloader.Request;
import org.schabi.newpipe.extractor.downloader.Response;
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException;
import org.schabi.newpipe.extractor.localization.Localization;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;


/**
 * Downloads HTTP content.
 */
public class HttpDownloader extends Downloader {

	/** Mimic the Mozilla user agent */
	private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:68.0) Gecko/20100101 Firefox/68.0";

	@Override
	public Response execute(Request request) throws IOException, ReCaptchaException {
		final String httpMethod = request.httpMethod();
		final String url = request.url();
		final Map<String, List<String>> headers = request.headers();
		final Localization localization = request.localization();

		final HttpsURLConnection connection = (HttpsURLConnection) new URL(url).openConnection();

		connection.setConnectTimeout(30 * 1000); // 30s
		connection.setReadTimeout(30 * 1000); // 30s
		connection.setRequestMethod(httpMethod);

		connection.setRequestProperty("User-Agent", USER_AGENT);
		connection.setRequestProperty("Accept-Language", "en");

		for (Map.Entry<String, List<String>> pair : headers.entrySet()) {
			final String headerName = pair.getKey();
			final List<String> headerValueList = pair.getValue();

			if (headerValueList.size() > 1) {
				connection.setRequestProperty(headerName, null);
				for (String headerValue : headerValueList) {
					connection.addRequestProperty(headerName, headerValue);
				}
			} else if (headerValueList.size() == 1) {
				connection.setRequestProperty(headerName, headerValueList.get(0));
			}
		}

		try(OutputStream outputStream = sendOutput(request, connection)) {

			final String response = readResponse(connection);

			final int responseCode = connection.getResponseCode();
			final String responseMessage = connection.getResponseMessage();
			final Map<String, List<String>> responseHeaders = connection.getHeaderFields();
			final URL latestUrl = connection.getURL();
			return new Response(responseCode, responseMessage, responseHeaders, response, latestUrl.toString());
		} catch (Exception e) {
			/*
			 * HTTP 429 == Too Many Request
			 * Receive from Youtube.com = ReCaptcha challenge request
			 * See : https://github.com/rg3/youtube-dl/issues/5138
			 */
			if (connection.getResponseCode() == 429) {
				throw new ReCaptchaException("reCaptcha Challenge requested", url);
			}

			throw new IOException(connection.getResponseCode() + " " + connection.getResponseMessage(), e);
		}
	}

	private OutputStream sendOutput(Request request, HttpsURLConnection connection) throws IOException {
		final byte[] dataToSend = request.dataToSend();
		if (dataToSend != null && dataToSend.length > 0) {
			connection.setDoOutput(true);
			connection.setRequestProperty("Content-Length", dataToSend.length + "");
			OutputStream outputStream = connection.getOutputStream();
			outputStream.write(dataToSend);
			return outputStream;
		}
		return null;
	}

	private String readResponse(HttpsURLConnection connection) throws IOException {
		try (InputStreamReader input = new InputStreamReader(connection.getInputStream())) {
			final StringBuilder response = new StringBuilder();

			int readCount;
			char[] buffer = new char[32 * 1024];
			while ((readCount = input.read(buffer)) != -1) {
				response.append(buffer, 0, readCount);
			}
			return response.toString();
		}
	}

}
