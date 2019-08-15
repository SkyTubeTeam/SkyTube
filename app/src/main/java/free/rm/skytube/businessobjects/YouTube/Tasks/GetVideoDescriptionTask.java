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

package free.rm.skytube.businessobjects.YouTube.Tasks;

import java.io.IOException;
import java.util.List;

import org.schabi.newpipe.extractor.exceptions.ExtractionException;

import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.AsyncTaskParallel;
import free.rm.skytube.businessobjects.Logger;
import free.rm.skytube.businessobjects.YouTube.GetVideoDescription;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;
import free.rm.skytube.businessobjects.YouTube.VideoStream.NewPipeService;

/**
 * Get the video's description.
 */
public class GetVideoDescriptionTask extends AsyncTaskParallel<Void, Void, String> {
	private YouTubeVideo youTubeVideo;
	private GetVideoDescriptionTaskListener listener;

	public interface GetVideoDescriptionTaskListener {
		void onFinished(String description);
	}

	public GetVideoDescriptionTask(YouTubeVideo youTubeVideo, GetVideoDescriptionTaskListener listener) {
		this.youTubeVideo = youTubeVideo;
		this.listener = listener;
	}

	@Override
	protected String doInBackground(Void... params) {
		if (youTubeVideo.getDescription() != null) {
			return youTubeVideo.getDescription();
		}
		String description = getDescription();
		if (description != null) {
			this.youTubeVideo.setDescription(description);
			return description;
		}

		return getErrorMessage();
	}

	private String getErrorMessage() {
		return SkyTubeApp.getStr(R.string.error_get_video_desc);
	}

	private String getDescription() {
		if (NewPipeService.isPreferred()) {
			try {
				YouTubeVideo details = NewPipeService.get().getDetails(youTubeVideo.getId());
				return details != null ? details.getDescription() : null;
			} catch (ExtractionException | IOException e) {
				Logger.e(this, "Unable to get video details, where id=" + youTubeVideo.getId(), e);
				return null;
			}
		} else {
			return getDescriptionFromAPI();
		}
	}

	private String getDescriptionFromAPI() {
		GetVideoDescription getVideoDescription = new GetVideoDescription();
		
		try {
			getVideoDescription.init(youTubeVideo.getId());
			List<YouTubeVideo> list = getVideoDescription.getNextVideos();
			if (!list.isEmpty()) {
				return list.get(0).getDescription();
			}
		} catch (IOException e) {
			Logger.e(this, "error_get_video_desc - id=" + youTubeVideo.getId(), e);
		}
		return null;
	}

	@Override
	protected void onPostExecute(String description) {
		if(listener instanceof GetVideoDescriptionTaskListener) {
			listener.onFinished(description);
		}
	}

}