package free.rm.skytube.businessobjects;

import android.content.Context;
import android.content.Intent;

import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;

import java.io.IOException;

import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.app.Utils;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;
import free.rm.skytube.businessobjects.YouTube.newpipe.ContentId;
import free.rm.skytube.businessobjects.YouTube.newpipe.NewPipeService;
import free.rm.skytube.gui.businessobjects.YouTubeVideoListener;

/**
 * An asynchronous task that will, from the given video URL, get the details of the video (e.g. video name,
 * likes ...etc).
 */
public class GetVideoDetailsTask extends AsyncTaskParallel<Void, Void, YouTubeVideo> {

	private final Context context;
	private final ContentId content;
	private final YouTubeVideoListener youTubeVideoListener;

	public GetVideoDetailsTask(Context context, ContentId content, YouTubeVideoListener youTubeVideoListener) {
		this.context = context;
		this.content = content;
		this.youTubeVideoListener = youTubeVideoListener;
	}

	public GetVideoDetailsTask(Context context, Intent intent, YouTubeVideoListener youTubeVideoListener) {
		this.context = context;
		this.content = SkyTubeApp.getUrlFromIntent(context, intent);
		Utils.isTrue(content.getType() == StreamingService.LinkType.STREAM, "Content is a video:"+content);

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
		if(youTubeVideoListener != null) {
			youTubeVideoListener.onYouTubeVideo(content, youTubeVideo);
		}
		super.onPostExecute(youTubeVideo);
	}

	@Override
	protected void showErrorToUi() {
		SkyTubeApp.notifyUserOnError(context, lastException);
	}
}