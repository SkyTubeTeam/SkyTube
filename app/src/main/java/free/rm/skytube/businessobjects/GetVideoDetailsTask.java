package free.rm.skytube.businessobjects;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;

import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.exceptions.ParsingException;

import free.rm.skytube.businessobjects.YouTube.GetVideosDetailsByIDs;
import free.rm.skytube.businessobjects.YouTube.POJOs.CardData;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;
import free.rm.skytube.businessobjects.YouTube.newpipe.NewPipeService;
import free.rm.skytube.businessobjects.YouTube.newpipe.VideoId;
import free.rm.skytube.gui.businessobjects.YouTubeVideoListener;

/**
 * An asynchronous task that will, from the given video URL, get the details of the video (e.g. video name,
 * likes ...etc).
 */
public class GetVideoDetailsTask extends AsyncTaskParallel<Void, Void, YouTubeVideo> {

	private String videoUrl = null;
	private YouTubeVideoListener youTubeVideoListener;
	private Exception lastException;

	public GetVideoDetailsTask(String videoUrl, YouTubeVideoListener youTubeVideoListener) {
		this.videoUrl = videoUrl;
		this.youTubeVideoListener = youTubeVideoListener;
	}

	@Override
	protected void onPreExecute() {
		try {
			// YouTube sends subscriptions updates email in which its videos' URL are encoded...
			// Hence we need to decode them first...
			videoUrl = URLDecoder.decode(videoUrl, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			Logger.e(this, "UnsupportedEncodingException on " + videoUrl + " encoding = UTF-8", e);
		}
	}


	/**
	 * Returns an instance of {@link YouTubeVideo} from the given {@link #videoUrl}.
	 *
	 * @return {@link YouTubeVideo}; null if an error has occurred.
	 */
	@Override
	protected YouTubeVideo doInBackground(Void... params) {
	    try {
			VideoId videoId = NewPipeService.get().getVideoId(videoUrl);
			try {
				return NewPipeService.get().getDetails(videoId.getId());
			} catch (ExtractionException | IOException e) {
				Logger.e(this, "Unable to get video details, where id=" + videoId, e);
				this.lastException = e;
			}
		} catch (ParsingException videoUrlParsing) {
			Logger.e(this, "Unable to get video details, where id=" + videoUrl, videoUrlParsing);
			this.lastException = videoUrlParsing;
		}

		return null;
	}

	@Override
	protected void onPostExecute(YouTubeVideo youTubeVideo) {
		if(youTubeVideoListener != null) {
			youTubeVideoListener.onYouTubeVideo(videoUrl, youTubeVideo);
		}
	}
}