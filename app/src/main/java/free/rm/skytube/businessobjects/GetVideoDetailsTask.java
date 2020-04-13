package free.rm.skytube.businessobjects;

import android.content.Context;
import android.content.Intent;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.exceptions.ParsingException;

import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;
import free.rm.skytube.businessobjects.YouTube.newpipe.ContentId;
import free.rm.skytube.businessobjects.YouTube.newpipe.NewPipeService;
import free.rm.skytube.businessobjects.YouTube.newpipe.VideoId;
import free.rm.skytube.gui.businessobjects.YouTubeVideoListener;

/**
 * An asynchronous task that will, from the given video URL, get the details of the video (e.g. video name,
 * likes ...etc).
 */
public class GetVideoDetailsTask extends AsyncTaskParallel<Void, Void, YouTubeVideo> {

	private final Context context;
	private final ContentId content;
	private final YouTubeVideoListener youTubeVideoListener;
	private Exception lastException;

	public GetVideoDetailsTask(Context context, ContentId content, YouTubeVideoListener youTubeVideoListener) {
		this.context = context;
		this.content = content;
		this.youTubeVideoListener = youTubeVideoListener;
	}

	public GetVideoDetailsTask(Context context, Intent intent, YouTubeVideoListener youTubeVideoListener) {
		this.context = context;
		this.content = SkyTubeApp.getUrlFromIntent(context, intent);
		this.youTubeVideoListener = youTubeVideoListener;
	}

	/**
	 * Returns an instance of {@link YouTubeVideo} from the given {@link #content}.
	 *
	 * @return {@link YouTubeVideo}; null if an error has occurred.
	 */
	@Override
	protected YouTubeVideo doInBackground(Void... params) {
		try {
			return NewPipeService.get().getDetails(content.getId());
		} catch (ExtractionException | IOException e) {
			Logger.e(this, "Unable to get video details, where id=" + content, e);
			this.lastException = e;
		}

		return null;
	}

	@Override
	protected void onPostExecute(YouTubeVideo youTubeVideo) {
		SkyTubeApp.notifyUserOnError(context, lastException);

		if(youTubeVideoListener != null) {
			youTubeVideoListener.onYouTubeVideo(content, youTubeVideo);
		}
	}
}