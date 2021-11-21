/*
 * SkyTube
 * Copyright (C) 2015  Ramon Mifsud -
 * based on https://github.com/TeamNewPipe/NewPipe/blob/dev/app/src/main/java/org/schabi/newpipe/DownloaderImpl.java
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

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;


/**
 * Downloads HTTP content.
 */
public final class HttpDownloader extends Downloader {
	public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; rv:78.0) Gecko/20100101 Firefox/78.0";

	private final Map<String, String> mCookies;
	private final OkHttpClient client;

	public HttpDownloader() {
		this.client = new OkHttpClient.Builder()
				.readTimeout(30, TimeUnit.SECONDS)
				.build();
		this.mCookies = new HashMap<>();
	}

	@Override
	public Response execute(@NonNull final Request request) throws IOException, ReCaptchaException {
		final String httpMethod = request.httpMethod();
		final String url = request.url();
		final Map<String, List<String>> headers = request.headers();
		final byte[] dataToSend = request.dataToSend();

		RequestBody requestBody = null;
		if (dataToSend != null) {
			requestBody = RequestBody.create(null, dataToSend);
		}

		final okhttp3.Request.Builder requestBuilder = new okhttp3.Request.Builder()
				.method(httpMethod, requestBody).url(url)
				.addHeader("User-Agent", USER_AGENT);

		for (final Map.Entry<String, List<String>> pair : headers.entrySet()) {
			final String headerName = pair.getKey();
			final List<String> headerValueList = pair.getValue();

			if (headerValueList.size() > 1) {
				requestBuilder.removeHeader(headerName);
				for (final String headerValue : headerValueList) {
					requestBuilder.addHeader(headerName, headerValue);
				}
			} else if (headerValueList.size() == 1) {
				requestBuilder.header(headerName, headerValueList.get(0));
			}

		}

		final okhttp3.Response response = client.newCall(requestBuilder.build()).execute();

		if (response.code() == 429) {
			response.close();

			throw new ReCaptchaException("reCaptcha Challenge requested", url);
		}

		final ResponseBody body = response.body();
		String responseBodyToReturn = null;

		if (body != null) {
			responseBodyToReturn = body.string();
		}

		final String latestUrl = response.request().url().toString();
		return new Response(response.code(), response.message(), response.headers().toMultimap(),
				responseBodyToReturn, latestUrl);
	}
}
