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

package free.rm.skytube.gui.businessobjects.adapters;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.List;

import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.YouTube.GetCommentThreads;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeComment;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeCommentThread;
import free.rm.skytube.businessobjects.YouTube.newpipe.NewPipeService;
import free.rm.skytube.businessobjects.YouTube.newpipe.PagerBackend;
import free.rm.skytube.databinding.CommentBinding;

/**
 * An adapter that will display comments in an {@link ExpandableListView}.
 */
public class CommentsAdapter extends BaseExpandableListAdapter {

	private PagerBackend<YouTubeCommentThread> commentThreadPager;
	private List<YouTubeCommentThread>	commentThreadsList = new ArrayList<>();
	private GetCommentsTask				getCommentsTask = null;
	private ExpandableListView			expandableListView;
	private View						commentsProgressBar;
	private View						noVideoCommentsView;
	private LayoutInflater				layoutInflater;
	private Context 					context;

	private static final String TAG = CommentsAdapter.class.getSimpleName();


	public CommentsAdapter(Context context, String videoId, ExpandableListView expandableListView, View commentsProgressBar, View noVideoCommentsView) {
		this.context = context;
		this.expandableListView = expandableListView;
		this.expandableListView.setAdapter(this);
		this.expandableListView.setOnGroupClickListener((parent, v, groupPosition, id) -> true);
		this.commentsProgressBar = commentsProgressBar;
		this.noVideoCommentsView = noVideoCommentsView;
		this.layoutInflater = LayoutInflater.from(expandableListView.getContext());
		try {
			this.commentThreadPager = NewPipeService.isPreferred() ? NewPipeService.get().getCommentPager(videoId) : new GetCommentThreads(videoId);
			this.getCommentsTask = new GetCommentsTask();
			this.getCommentsTask.execute();
		} catch (Exception e) {
			SkyTubeApp.notifyUserOnError(context, e);
		}
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
			viewHolder = new CommentViewHolder(CommentBinding.bind(row));
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

			viewHolder.updateInfo(comment, getParentView, groupPosition);
		}

		// if it reached the bottom of the list, then try to get the next page of videos
		if (getParentView  &&  groupPosition == getGroupCount() - 1) {
			synchronized (this) {
				if (this.getCommentsTask == null) {
					Log.w(TAG, "Getting next page of comments...");
					this.getCommentsTask = new GetCommentsTask();
					this.getCommentsTask.execute();
				}
			}
		}

		return row;
	}


	////////////

	private class CommentViewHolder {
		private final CommentBinding binding;

		protected CommentViewHolder(CommentBinding binding) {
			this.binding = binding;
		}

		protected void updateInfo(final YouTubeComment comment, boolean isTopLevelComment, final int groupPosition) {
			binding.commentPaddingView.setVisibility(isTopLevelComment ? View.GONE : View.VISIBLE);
			binding.authorTextView.setText(comment.getAuthor());
			binding.commentTextView.setText(comment.getComment());
			binding.commentDateTextView.setText(comment.getDatePublished());
			binding.commentUpvotesTextView.setText(String.valueOf(comment.getLikeCount()));
			Glide.with(context)
					.load(comment.getThumbnailUrl())
					.apply(new RequestOptions().placeholder(R.drawable.channel_thumbnail_default))
					.into(binding.commentThumbnailImageView);

			binding.commentThumbnailImageView.setOnClickListener(view -> {
				if(comment.getAuthorChannelId() != null) {
					SkyTubeApp.launchChannel(comment.getAuthorChannelId(), context);
				}
			});

			// change the width dimensions depending on whether the comment is a top level or a child
			ViewGroup.LayoutParams lp = binding.commentThumbnailImageView.getLayoutParams();
			lp.width = (int) SkyTubeApp.getDimension(isTopLevelComment  ?  R.dimen.top_level_comment_thumbnail_width  :  R.dimen.child_comment_thumbnail_width);

			if (isTopLevelComment  &&  getChildrenCount(groupPosition) > 0) {
				binding.viewAllRepliesTextView.setVisibility(View.VISIBLE);

				// on click, hide/show the comment replies
				binding.getRoot().setOnClickListener(viewReplies -> {
					if (expandableListView.isGroupExpanded(groupPosition)) {
						binding.viewAllRepliesTextView.setText(R.string.view_replies);
						expandableListView.collapseGroup(groupPosition);
					} else {
						binding.viewAllRepliesTextView.setText(R.string.hide_replies);
						expandableListView.expandGroup(groupPosition);
					}
				});
			} else {
				binding.viewAllRepliesTextView.setVisibility(View.GONE);
			}
		}
	}

	////////////////////////////////////////////////////////////////////////////////////////////////

	private class GetCommentsTask extends AsyncTask<Void, Void, List<YouTubeCommentThread>> {

		@Override
		protected void onPreExecute() {
			commentsProgressBar.setVisibility(View.VISIBLE);
			noVideoCommentsView.setVisibility(View.GONE);
		}

		@Override
		protected  List<YouTubeCommentThread> doInBackground(Void... params) {
			return commentThreadPager.getSafeNextPage();
		}

		@Override
		protected void onPostExecute(List<YouTubeCommentThread> newComments) {
			SkyTubeApp.notifyUserOnError(expandableListView.getContext(), commentThreadPager.getLastException());

			if (newComments != null) {
				if (newComments.size() > 0) {
					CommentsAdapter.this.commentThreadsList.addAll(newComments);
					CommentsAdapter.this.notifyDataSetChanged();
				}
				if (commentThreadsList.isEmpty()) {
                    noVideoCommentsView.setVisibility(View.VISIBLE);
                }
			}

			commentsProgressBar.setVisibility(View.GONE);
			getCommentsTask = null;
		}

	}

}
