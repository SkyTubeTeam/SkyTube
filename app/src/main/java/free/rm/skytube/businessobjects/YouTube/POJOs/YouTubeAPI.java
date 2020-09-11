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

import android.content.pm.PackageManager;
import android.content.pm.Signature;

import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.common.io.BaseEncoding;

import java.io.IOException;
import java.security.MessageDigest;

import free.rm.skytube.BuildConfig;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.Logger;


/**
 * Represents YouTube API service.
 */
public class YouTubeAPI {

	/**
	 * Returns a new instance of {@link YouTube}.
	 *
	 * @return {@link YouTube}
	 */
	public static YouTube create() {
		HttpTransport httpTransport = new NetHttpTransport();
		JsonFactory jsonFactory = AndroidJsonFactory.getDefaultInstance();
		return new YouTube.Builder(httpTransport, jsonFactory, new HttpRequestInitializer() {
			private String getSha1() {
				String sha1 = null;
				try {
					Signature[] signatures = SkyTubeApp.getContext().getPackageManager().getPackageInfo(BuildConfig.APPLICATION_ID, PackageManager.GET_SIGNATURES).signatures;
					for (Signature signature: signatures) {
						MessageDigest md = MessageDigest.getInstance("SHA-1");

						md.update(signature.toByteArray());
						sha1 = BaseEncoding.base16().encode(md.digest());
					}
				} catch (Throwable tr) {
					Logger.e(this, "...", tr);
				}
				return sha1;
			}
			@Override
			public void initialize(HttpRequest request) throws IOException {
				request.getHeaders().set("X-Android-Package", BuildConfig.APPLICATION_ID);
				request.getHeaders().set("X-Android-Cert", getSha1());
			}
		}).setApplicationName("+").build();
	}

}
