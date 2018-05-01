package free.rm.skytube.businessobjects.db.Tasks;

import android.view.Menu;

import free.rm.skytube.R;
import free.rm.skytube.businessobjects.AsyncTaskParallel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;
import free.rm.skytube.businessobjects.db.PlaybackStatusDb;

/**
 * A Task that checks if the passed {@link YouTubeVideo} is marked as watched, to update the passed {@link Menu} accordingly.
 */
public class IsVideoWatchedTask extends AsyncTaskParallel<Void, Void, Boolean> {
	private Menu menu;
	private YouTubeVideo youTubeVideo;

	public IsVideoWatchedTask(YouTubeVideo youTubeVideo, Menu menu) {
		this.youTubeVideo = youTubeVideo;
		this.menu = menu;
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		return PlaybackStatusDb.getVideoDownloadsDb().getVideoWatchedStatus(youTubeVideo).watched;
	}

	@Override
	protected void onPostExecute(Boolean videoIsWatched) {
		// if this video has been watched, hide the set watched option and show the set unwatched option.
		menu.findItem(R.id.mark_watched).setVisible(!videoIsWatched);
		menu.findItem(R.id.mark_unwatched).setVisible(videoIsWatched);
	}
}
