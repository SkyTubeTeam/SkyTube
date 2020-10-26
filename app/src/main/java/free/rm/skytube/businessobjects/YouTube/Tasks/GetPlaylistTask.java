package free.rm.skytube.businessobjects.YouTube.Tasks;

import android.content.Context;

import java.util.List;

import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.AsyncTaskParallel;
import free.rm.skytube.businessobjects.Logger;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubePlaylist;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;
import free.rm.skytube.businessobjects.YouTube.newpipe.NewPipeException;
import free.rm.skytube.businessobjects.YouTube.newpipe.NewPipeService;
import free.rm.skytube.businessobjects.YouTube.newpipe.PlaylistPager;
import free.rm.skytube.gui.businessobjects.YouTubePlaylistListener;

/**
 * An asynchronous task that will retrieve a YouTube playlist for a specified playlist URL.
 */
public class GetPlaylistTask extends AsyncTaskParallel<Void, Void, YouTubePlaylist> {
    private final String playlistId;
    private final YouTubePlaylistListener playlistListener;
    private final Context context;

    protected static final Long MAX_RESULTS = 45L;

    public GetPlaylistTask(Context context, String playlistId, YouTubePlaylistListener playlistClickListener) {
        this.context = context;
        this.playlistId = playlistId;
        this.playlistListener = playlistClickListener;
    }

    @Override
    protected YouTubePlaylist doInBackground(Void... voids) {
        try {
            final PlaylistPager pager = NewPipeService.get().getPlaylistPager(playlistId);
            final List<YouTubeVideo> firstPage = pager.getNextPageAsVideos();
            return pager.getPlaylist();
        } catch (NewPipeException e) {
            Logger.e(this, "Couldn't load playlist", e);
            lastException = e;
        }
        return null;
    }

    @Override
    protected void onPostExecute(YouTubePlaylist youTubePlaylist) {
        if (playlistListener != null && youTubePlaylist != null) {
            playlistListener.onYouTubePlaylist(youTubePlaylist);
        }
        super.onPostExecute(youTubePlaylist);
    }

    @Override
    protected void showErrorToUi() {
        SkyTubeApp.notifyUserOnError(context, lastException);
    }

}
