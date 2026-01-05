/*
 * SkyTube
 * Copyright (C) 2026  SkyTube Team
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

import org.schabi.newpipe.extractor.comments.CommentsInfoItem;

/**
 * Interface for CommentPager to make it more testable.
 */
public interface CommentPagerInterface extends PagerBackend<CommentsInfoItem> {

    /**
     * Returns the comment at the specified index or null if not found
     */
    CommentsInfoItem getComment(int idx);

    /**
     * Returns whether comments are disabled for this video.
     */
    boolean isCommentsDisabled();

    /**
     * Returns the total number of comments available (not necessarily loaded).
     */
    int getCommentCount();

    /**
     * Returns the number of comments that have actually been loaded so far.
     * This is different from getCommentCount() which returns the total available.
     *
     * @return the number of loaded comments
     */
    int getLoadedCount();

}