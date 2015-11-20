/*
 * SkyTube
 * Copyright (C) 2015  Ramon Mifsud
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package free.rm.skytube.gui.businessobjects;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * SkyTube
 * <br>Copyright (C) 2015  Ramon Mifsud</br>
 *
 * <p>This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.</p>
 *
 * <p>This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.</p>
 *
 * <p>You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <a href="http://www.gnu.org/licenses/">http://www.gnu.org/licenses/</a>.</p>
 */
public class InternetImageView extends ImageView {

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public InternetImageView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}

	public InternetImageView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public InternetImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public InternetImageView(Context context) {
		super(context);
	}

	public void setImageAsync(BitmapCache bitmapCache, String url) {
		new DownloadImageTask(bitmapCache).execute(url);
	}


	////////////////////////////

	protected class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {

		private BitmapCache bitmapCache;
		private static final String TAG = "DownloadImageTask";

		public DownloadImageTask(BitmapCache bitmapCache) {
			this.bitmapCache = bitmapCache;
		}

		@Override
		protected void onPreExecute() {
			InternetImageView.this.setImageBitmap(null);
		}

		@Override
		protected Bitmap doInBackground(String... param) {
			String url = param[0];

			// try to get the bitmap from the cache
			Bitmap bitmap = bitmapCache.get(url);

			// if the bitmap was located in cache...
			if (bitmap != null) {
				Log.i(TAG, "Cache hit:  " + url);
			} else {
				// bitmap was not located in cache:  therefore download it
				bitmap = downloadBitmap(url);

				// adds the downloaded image to the cache
				bitmapCache.add(url, bitmap);
			}

			return bitmap;
		}


		/**
		 * Downloads a bitmap from the Internet.
		 *
		 * @param url Bitmap's URL.
		 * @return Bitmap instance if successful; null otherwise.
		 */
		private Bitmap downloadBitmap(String url) {
			HttpURLConnection urlConnection = null;
			Bitmap				bitmap = null;

			try {
				// download the image from the internet and construct its bitmap
				Log.i(TAG, "Downloading:  " + url);

				urlConnection = (HttpURLConnection) new URL(url).openConnection();
				int responseCode = urlConnection.getResponseCode();

				if (responseCode < 0) {
					Log.e(TAG, "Image not found.  Response code = " + responseCode);
				} else {
					InputStream inputStream = urlConnection.getInputStream();
					if (inputStream != null) {
						bitmap = BitmapFactory.decodeStream(inputStream);
					}
				}
			} catch (Exception e) {
				urlConnection.disconnect();
				Log.e(TAG, "Error has occurred while downloading image from " + url, e);
			} finally {
				if (urlConnection != null) {
					urlConnection.disconnect();
				}
			}

			return bitmap;
		}


		@Override
		protected void onPostExecute(Bitmap bitmap) {
			InternetImageView.this.setImageBitmap(bitmap);
		}
	}

}
