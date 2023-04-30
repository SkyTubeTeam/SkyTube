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
import org.schabi.newpipe.extractor.comments.CommentsExtractor;
import org.schabi.newpipe.extractor.comments.CommentsInfoItem;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommentPager extends Pager<CommentsInfoItem, CommentsInfoItem> {

    private Boolean disabledComments;
    private final List<CommentsInfoItem> allComments = new ArrayList<>();
    private final CommentsExtractor commentsExtractor;

    private Integer commentCount;

    CommentPager(StreamingService streamingService, CommentsExtractor commentsExtractor) throws ExtractionException, IOException {
        super(streamingService, commentsExtractor);
        this.commentsExtractor = commentsExtractor;
    }

    @Override
    protected List<CommentsInfoItem> process(ListExtractor.InfoItemsPage<? extends CommentsInfoItem> page) throws NewPipeException, ExtractionException {
        this.commentCount = commentsExtractor.getCommentsCount();
        this.disabledComments = commentsExtractor.isCommentsDisabled();
        List<CommentsInfoItem> result = super.process(page);
        allComments.addAll(result);
        return new ArrayList<>(result);
    }

    @Override
    protected List<CommentsInfoItem> extract(ListExtractor.InfoItemsPage<? extends CommentsInfoItem> page) throws ExtractionException {
        return (List<CommentsInfoItem>) page.getItems();
    }

    public CommentsInfoItem getComment(int idx) {
        return 0 <= idx && idx < allComments.size() ? allComments.get(idx) : null;
    }

    public boolean isCommentsDisabled() {
        return disabledComments != null ? disabledComments : false;
    }

    public int getCommentCount() {
        return commentCount != null ? commentCount.intValue() : -1;
    }

}
