/*
 * SkyTube
 * Copyright (C) 2019  Zsombor Gegesy
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
package free.rm.skytube.businessobjects.YouTube.newpipe;

import org.schabi.newpipe.extractor.ListExtractor;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.comments.CommentsInfoItem;

import java.util.ArrayList;
import java.util.List;

import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeComment;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeCommentThread;

public class CommentPager extends Pager<CommentsInfoItem, YouTubeCommentThread> {
    CommentPager(StreamingService streamingService, ListExtractor<CommentsInfoItem> commentExtractor) {
        super(streamingService, commentExtractor);
    }

    @Override
    protected List<YouTubeCommentThread> extract(ListExtractor.InfoItemsPage<CommentsInfoItem> page) {
        List<YouTubeCommentThread> result = new ArrayList<>(page.getItems().size());
        for (CommentsInfoItem infoItem : page.getItems()) {
            YouTubeCommentThread thread = new YouTubeCommentThread(new YouTubeComment(
                    infoItem.getUploaderUrl(),
                    infoItem.getUploaderName(),
                    infoItem.getThumbnailUrl(),
                    infoItem.getCommentText(),
                    infoItem.getTextualUploadDate(),
                    infoItem.getLikeCount(),
                    infoItem.isPinned(),
                    infoItem.isHeartedByUploader()
                    ));
            result.add(thread);
        }
        return result;
    }
}
