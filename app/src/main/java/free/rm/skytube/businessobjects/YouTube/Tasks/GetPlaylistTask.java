package free.rm.skytube.businessobjects.YouTube.Tasks;

import android.content.Context;

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Playlist;
import com.google.api.services.youtube.model.PlaylistListResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import free.rm.skytube.businessobjects.AsyncTaskParallel;
import free.rm.skytube.businessobjects.Logger;
import free.rm.skytube.businessobjects.YouTube.GetChannelsDetails;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeAPI;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeAPIKey;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubePlaylist;
import free.rm.skytube.businessobjects.db.Tasks.GetChannelInfo;
import free.rm.skytube.gui.businessobjects.YouTubePlaylistListener;

/**
 * An asynchronous task that will retrieve a YouTube playlist for a specified playlist URL.
 */
public class GetPlaylistTask extends AsyncTaskParallel<Void, Void, YouTubePlaylist> {
    private final String playlistId;
    private final YouTubePlaylistListener playlistListener;
    private final GetChannelInfo channelInfo;

    protected static final Long MAX_RESULTS = 45L;

    public GetPlaylistTask(Context context, String playlistId, YouTubePlaylistListener playlistClickListener) {
        this.channelInfo = new GetChannelInfo(context, true);
        this.playlistId = playlistId;
        this.playlistListener = playlistClickListener;
    }

    @Override
    protected YouTubePlaylist doInBackground(Void... voids) {
        try {
            YouTube.Playlists.List playlistList = YouTubeAPI.create().playlists().list("id, snippet, contentDetails");
            playlistList.setKey(YouTubeAPIKey.get().getYouTubeAPIKey());
            playlistList.setFields("items(id, snippet/title, snippet/description, snippet/thumbnails, snippet/publishedAt, contentDetails/itemCount, snippet/channelId)," +
                    "nextPageToken");
            playlistList.setMaxResults(MAX_RESULTS);
            playlistList.setId(playlistId);

            return getFirstPlaylist(playlistList);
        } catch (IOException e) {
            Logger.e(this, "Couldn't initialize GetPlaylist");
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(YouTubePlaylist youTubePlaylist) {
        channelInfo.showErrorToUi();
        if (playlistListener != null) {
            playlistListener.onYouTubePlaylist(youTubePlaylist);
        }
    }


    private YouTubePlaylist getFirstPlaylist(YouTube.Playlists.List api) {
        List<Playlist> playlistList = null;
        try {
            // communicate with YouTube
            PlaylistListResponse listResponse = api.execute();

            // get playlists
            playlistList = listResponse.getItems();

            if (!playlistList.isEmpty()) {
                Playlist playlist = playlistList.get(0);
                YouTubeChannel channel = channelInfo.getChannelInfoSync(playlist.getSnippet().getChannelId(), null);
                return new YouTubePlaylist(playlist, channel);
            }
            // set the next page token

        } catch (IOException ex) {
            Logger.d(this, ex.getLocalizedMessage());
        }

        return null;
    }


}
