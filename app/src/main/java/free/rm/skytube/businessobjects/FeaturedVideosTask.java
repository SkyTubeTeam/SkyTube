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

package free.rm.skytube.businessobjects;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Video;

import java.io.IOException;
import java.util.List;

import free.rm.skytube.R;
import free.rm.skytube.gui.businessobjects.GridAdapter;

/**
 *
 */
public class FeaturedVideosTask extends AsyncTask<Void, Void, List<Video>> {

	private FeaturedVideos	featuredVideos;
	private Context			context;
	private GridAdapter		gridAdapter;

	private static final String TAG = FeaturedVideosTask.class.getSimpleName();


	public FeaturedVideosTask(Context context, GridAdapter gridAdapter) {
		this.featuredVideos = new FeaturedVideos();
		this.context = context;
		this.gridAdapter = gridAdapter;
	}


	@Override
	protected void onPreExecute() {
		try {
			featuredVideos.init(context);
		} catch (IOException e) {
			Log.e(TAG, "Could not init featured videos...", e);
			Toast.makeText(context, context.getString(R.string.could_not_get_videos), Toast.LENGTH_LONG).show();
			cancel(true);
		}
	}


	@Override
	protected List<Video> doInBackground(Void... params) {
		List<Video> videos = null;

		if (!isCancelled()) {
			videos = featuredVideos.getNextVideos();
		}

		return videos;
	}


	@Override
	protected void onPostExecute(List<Video> videosList) {
		gridAdapter.appendList(videosList);
	}
}
