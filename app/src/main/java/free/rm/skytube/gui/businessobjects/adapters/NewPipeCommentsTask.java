package free.rm.skytube.gui.businessobjects.adapters;

import java.util.List;

import free.rm.skytube.businessobjects.AsyncTaskParallel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeCommentThread;

public class NewPipeCommentsTask extends AsyncTaskParallel<Void, Void, List<YouTubeCommentThread>> {

    private String nextPageUrl;
    private boolean hasNextPage = true;

    @Override
    protected List<YouTubeCommentThread> doInBackground(Void... voids) {
        return null;
    }
}
