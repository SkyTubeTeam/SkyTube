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

package free.rm.skytube.gui.businessobjects;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import free.rm.skytube.R;

/**
 * An {@link ImageView} can load images/pictures located on the internet asynchronously.
 */
public class InternetImageView extends ImageView {

	/**
	 * The default image to be used (before the specified image in
	 * {@link #setImageAsync(String)} is loaded)
	 */
	protected final int defaultImageRes;


	public InternetImageView(Context context, AttributeSet attrs) {
		super(context, attrs);

		TypedArray a = context.getTheme().obtainStyledAttributes(
							attrs,
							R.styleable.InternetImageView,
							0, 0);

		try {
			// get the value of "custom:defaultImage" (this holds the drawable resource that will be
			// used before the proper image is downloaded and loaded)
			defaultImageRes = a.getResourceId(R.styleable.InternetImageView_defaultImage, 0);
		} finally {
			a.recycle();
		}
	}


	/**
	 * Set a remote image as the content of this {@link InternetImageView}.  The remote image will
	 * be stored in bitmapCache.
	 *
	 * @param url	URL of the remote image.
	 */
	public void setImageAsync(String url) {
		new DownloadImageTask().execute(url);
	}


	////////////////////////////

	protected class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {

		private static final String TAG = "DownloadImageTask";

		@Override
		protected void onPreExecute() {
			InternetImageView.this.setImageResource(defaultImageRes);
		}

		@Override
		protected Bitmap doInBackground(String... param) {
			BitmapCache bitmapCache = BitmapCache.get();
			String url = param[0];

			// try to get the bitmap from the cache
			Bitmap bitmap = (bitmapCache != null) ? bitmapCache.get(url) : null;

			// if the bitmap was located in cache...
			if (bitmap != null) {
				Log.i(TAG, "Cache hit:  " + url);
			} else if (url != null) {
				// bitmap was not located in cache:  therefore download it
				bitmap = downloadBitmap(url);

				// adds the downloaded image to the cache
				if (bitmapCache != null  &&  bitmap != null)
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
				bitmap = null;
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
			if (bitmap != null)
				InternetImageView.this.setImageBitmap(bitmap);
		}
	}

}
