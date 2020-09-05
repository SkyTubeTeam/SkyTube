/*
 * SkyTube
 * Copyright (C) 2018  Ramon Mifsud
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

package free.rm.skytube.businessobjects.YouTube.POJOs;

import com.google.api.services.youtube.model.Comment;
import com.google.api.services.youtube.model.CommentThread;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A Thread of comments.  A thread is made up of a top-level comments and 0 or more reply comments.
 */
public class YouTubeCommentThread {

	/** Top-level comment. */
	private YouTubeComment			comment;
	/** Replies. */
	private List<YouTubeComment>	repliesList = new ArrayList<>();

	public YouTubeCommentThread(CommentThread commentThread) {
		if (isCommentThreadOkay(commentThread)) {
			this.comment = new YouTubeComment(commentThread.getSnippet().getTopLevelComment());

			if (hasAnyReplies(commentThread)) {
				List<Comment> commentRepliesList = commentThread.getReplies().getComments();
				Collections.reverse(commentRepliesList);	// reverse as the newest comments are put at the front of the list -- so we need to invert it

				for (Comment comment : commentRepliesList) {
					repliesList.add(new YouTubeComment(comment));
				}
			}
		}

	}

	public YouTubeCommentThread(YouTubeComment comment) {
		this.comment = comment;
	}

	private boolean isCommentThreadOkay(CommentThread commentThread) {
		return (commentThread.getSnippet() != null
				&& commentThread.getSnippet().getTopLevelComment() != null);
	}


	private boolean hasAnyReplies(CommentThread commentThread) {
		return (commentThread.getReplies() != null
				&& commentThread.getReplies().size() > 0);
	}


	public YouTubeComment getTopLevelComment() {
		return comment;
	}

	public List<YouTubeComment> getRepliesList() {
		return repliesList;
	}

	public int getTotalReplies() {
		return repliesList.size();
	}

}
