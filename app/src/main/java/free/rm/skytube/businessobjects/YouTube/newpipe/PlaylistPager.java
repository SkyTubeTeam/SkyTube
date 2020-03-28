package free.rm.skytube.businessobjects.YouTube.newpipe;

import org.schabi.newpipe.extractor.ListExtractor;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.localization.DateWrapper;
import org.schabi.newpipe.extractor.playlist.PlaylistExtractor;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;

import java.util.ArrayList;
import java.util.List;

import free.rm.skytube.businessobjects.Logger;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubePlaylist;

class PlaylistPager extends Pager<StreamInfoItem, YouTubePlaylist>  {
    public PlaylistPager(StreamingService streamingService, PlaylistExtractor playlistExtractor) {
        super(streamingService, playlistExtractor);
    }

    @Override
    protected List<YouTubePlaylist> extract(ListExtractor.InfoItemsPage<StreamInfoItem> page) {
        List<YouTubePlaylist> result = new ArrayList<>(page.getItems().size());
        Logger.i(this, "extract from %s, items: %s", page, page.getItems().size());
        for (StreamInfoItem item: page.getItems()) {
            Logger.i(this, "item is %s - uploader: %s / %s", item, item.getUploaderUrl(), item.getUploaderName());
            DateWrapper dw = item.getUploadDate();
            Long publishTimestamp = dw != null ? dw.date().getTimeInMillis() : null;
            YouTubePlaylist playlist = new YouTubePlaylist(item.getUrl(), item.getName(), "", publishTimestamp, 1, item.getThumbnailUrl(),  null);
            result.add(playlist);
        }
        return result;
    }
}
