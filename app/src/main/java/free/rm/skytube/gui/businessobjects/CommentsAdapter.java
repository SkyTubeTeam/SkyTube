/*
 * SkyTube
 * Copyright (C) 2016  Ramon Mifsud
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

package free.rm.skytube.gui.businessobjects;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import java.util.List;

import free.rm.skytube.R;
import free.rm.skytube.businessobjects.YouTubeComment;
import free.rm.skytube.businessobjects.YouTubeCommentThread;
import free.rm.skytube.gui.app.SkyTubeApp;

/**
 *
 */
public class CommentsAdapter extends BaseExpandableListAdapter {

	private List<YouTubeCommentThread>	commentThreadsList;
	private LayoutInflater				layoutInflater;

	private static final String TAG = CommentsAdapter.class.getSimpleName();


	public CommentsAdapter(List<YouTubeCommentThread> commentThreadsList, Context context) {
		this.commentThreadsList = commentThreadsList;
		this.layoutInflater = LayoutInflater.from(context);
	}

	@Override
	public int getGroupCount() {
		return commentThreadsList.size();
	}

	@Override
	public int getChildrenCount(int groupPosition) {
		return commentThreadsList.get(groupPosition).getTotalReplies();
	}

	@Override
	public Object getGroup(int groupPosition) {
		return commentThreadsList.get(groupPosition);
	}

	@Override
	public Object getChild(int groupPosition, int childPosition) {
		return commentThreadsList.get(groupPosition).getRepliesList().get(childPosition);
	}

	@Override
	public long getGroupId(int groupPosition) {
		return groupPosition;
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		return (groupPosition * 1024) + childPosition;
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
		return getParentOrChildView(true, groupPosition, 0 /*ignore this*/, convertView, parent);
	}

	@Override
	public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
		return getParentOrChildView(false, groupPosition, childPosition, convertView, parent);
	}

	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return false;
	}


	private View getParentOrChildView(boolean getParentView, int groupPosition, int childPosition, View convertView, ViewGroup parent) {
		View row;
		CommentViewHolder viewHolder;

		if (convertView == null) {
			row = layoutInflater.inflate(R.layout.comment, parent, false);
			viewHolder = new CommentViewHolder(row);
			row.setTag(viewHolder);
		} else {
			row = convertView;
			viewHolder = (CommentViewHolder) row.getTag();
		}

		if (viewHolder != null) {
			YouTubeComment comment;

			if (getParentView)
				comment = ((YouTubeCommentThread)getGroup(groupPosition)).getTopLevelComment();
			else
				comment = (YouTubeComment)getChild(groupPosition, childPosition);

			viewHolder.updateInfo(comment, getParentView);
		}

		// if it reached the bottom of the list, then try to get the next page of videos
		if (getParentView  &&  groupPosition == getGroupCount() - 1) {
			Log.w(TAG, "BOTTOM REACHED!!!");
			//new GetYouTubeVideosTask(getYouTubeVideos, this).execute();
		}

		return row;
	}


	////////////

	private static class CommentViewHolder {
		private View		paddingView;
		private TextView	authorTextView,
							commentTextView,
							dateTextView,
							upvotesTextView;
		private InternetImageView	thumbnailImageView;

		protected CommentViewHolder(View commentView) {
			paddingView		= commentView.findViewById(R.id.comment_padding_view);
			authorTextView	= (TextView) commentView.findViewById(R.id.author_text_view);
			commentTextView	= (TextView) commentView.findViewById(R.id.comment_text_view);
			dateTextView	= (TextView) commentView.findViewById(R.id.comment_date_text_view);
			upvotesTextView	= (TextView) commentView.findViewById(R.id.comment_upvotes_text_view);
			thumbnailImageView = (InternetImageView) commentView.findViewById(R.id.comment_thumbnail_image_view);
		}


		protected void updateInfo(YouTubeComment comment, boolean isTopLevelComment) {
			paddingView.setVisibility(isTopLevelComment ? View.GONE : View.VISIBLE);
			authorTextView.setText(comment.getAuthor());
			commentTextView.setText(comment.getComment());
			dateTextView.setText(comment.getDatePublished());
			upvotesTextView.setText(comment.getLikeCount());

			// change the width dimensions depending on whether the comment is a top level or a child
			ViewGroup.LayoutParams lp = thumbnailImageView.getLayoutParams();
			lp.width = (int) SkyTubeApp.getDimension(isTopLevelComment  ?  R.dimen.top_level_comment_thumbnail_width  :  R.dimen.child_comment_thumbnail_width);
		}
	}

}
