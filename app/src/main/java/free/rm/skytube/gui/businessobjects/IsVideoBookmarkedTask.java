package free.rm.skytube.gui.businessobjects;

import android.view.Menu;

import free.rm.skytube.R;
import free.rm.skytube.businessobjects.AsyncTaskParallel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;
import free.rm.skytube.businessobjects.db.BookmarksDb;

/**
 * A task that checks if this video is bookmarked or not.  If it is bookmarked, then it will hide
 * the menu option to bookmark the video; otherwise it will hide the option to unbookmark the
 * video.
 */
public class IsVideoBookmarkedTask extends AsyncTaskParallel<Void, Void, Boolean> {
	private Menu menu;
	private YouTubeVideo youTubeVideo;

	public IsVideoBookmarkedTask(YouTubeVideo youTubeVideo, Menu menu) {
		this.youTubeVideo = youTubeVideo;
		this.menu = menu;
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		return BookmarksDb.getBookmarksDb().isBookmarked(youTubeVideo);
	}

	@Override
	protected void onPostExecute(Boolean videoIsBookmarked) {
		// if this video has been bookmarked, hide the bookmark option and show the unbookmark option.
		menu.findItem(R.id.bookmark_video).setVisible(!videoIsBookmarked);
		menu.findItem(R.id.unbookmark_video).setVisible(videoIsBookmarked);
	}
}
