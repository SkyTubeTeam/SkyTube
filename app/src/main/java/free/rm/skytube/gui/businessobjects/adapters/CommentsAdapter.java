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
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.List;

import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.YouTube.GetCommentThreads;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannelInterface;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeComment;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeCommentThread;
import free.rm.skytube.businessobjects.YouTube.Tasks.GetYouTubeChannelInfoTask;
import free.rm.skytube.gui.activities.MainActivity;
import free.rm.skytube.gui.fragments.ChannelBrowserFragment;

/**
 * An adapter that will display comments in an {@link ExpandableListView}.
 */
public class CommentsAdapter extends BaseExpandableListAdapter {

	private String						videoId;
	private List<YouTubeCommentThread>	commentThreadsList = new ArrayList<>();
	private GetCommentsTask				getCommentsTask = null;
	private GetCommentThreads			getCommentThreads = null;
	private ExpandableListView			expandableListView;
	private View						commentsProgressBar;
	private View						noVideoCommentsView;
	private LayoutInflater				layoutInflater;
	private Context 					context;

	private static final String TAG = CommentsAdapter.class.getSimpleName();


	public CommentsAdapter(Context context, String videoId, ExpandableListView expandableListView, View commentsProgressBar, View noVideoCommentsView) {
		this.context = context;
		this.videoId = videoId;
		this.expandableListView = expandableListView;
		this.expandableListView.setAdapter(this);
		this.expandableListView.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
			@Override
			public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
				return true;
			}
		});
		this.commentsProgressBar = commentsProgressBar;
		this.noVideoCommentsView = noVideoCommentsView;
		this.layoutInflater = LayoutInflater.from(expandableListView.getContext());
		this.getCommentsTask = new GetCommentsTask(videoId);
		this.getCommentsTask.execute();
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

			viewHolder.updateInfo(comment, getParentView, groupPosition);
		}

		// if it reached the bottom of the list, then try to get the next page of videos
		if (getParentView  &&  groupPosition == getGroupCount() - 1) {
			if (this.getCommentsTask == null) {
				Log.w(TAG, "Getting next page of comments...");
				this.getCommentsTask = new GetCommentsTask(this.videoId);
				this.getCommentsTask.execute();
			}
		}

		return row;
	}


	////////////

	private class CommentViewHolder {
		private View		commentView,
							paddingView;
		private TextView	authorTextView,
							commentTextView,
							dateTextView,
							upvotesTextView,
							viewRepliesTextView;
		private ImageView	thumbnailImageView;

		protected CommentViewHolder(View commentView) {
			this.commentView = commentView;
			paddingView		= commentView.findViewById(R.id.comment_padding_view);
			authorTextView	= commentView.findViewById(R.id.author_text_view);
			commentTextView	= commentView.findViewById(R.id.comment_text_view);
			dateTextView	= commentView.findViewById(R.id.comment_date_text_view);
			upvotesTextView	= commentView.findViewById(R.id.comment_upvotes_text_view);
			viewRepliesTextView	= commentView.findViewById(R.id.view_all_replies_text_view);
			thumbnailImageView	= commentView.findViewById(R.id.comment_thumbnail_image_view);
		}


		protected void updateInfo(final YouTubeComment comment, boolean isTopLevelComment, final int groupPosition) {
			paddingView.setVisibility(isTopLevelComment ? View.GONE : View.VISIBLE);
			authorTextView.setText(comment.getAuthor());
			commentTextView.setText(comment.getComment());
			dateTextView.setText(comment.getDatePublished());
			upvotesTextView.setText(comment.getLikeCount());
			Glide.with(context)
					.load(comment.getThumbnailUrl())
					.apply(new RequestOptions().placeholder(R.drawable.channel_thumbnail_default))
					.into(thumbnailImageView);

			thumbnailImageView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					if(comment.getAuthorChannelId() != null) {
						new GetYouTubeChannelInfoTask(context, new YouTubeChannelInterface() {
							@Override
							public void onGetYouTubeChannel(YouTubeChannel youTubeChannel) {
								Intent i = new Intent(context, MainActivity.class);
								i.setAction(MainActivity.ACTION_VIEW_CHANNEL);
								i.putExtra(ChannelBrowserFragment.CHANNEL_OBJ, youTubeChannel);
								context.startActivity(i);
							}
						}).executeInParallel(comment.getAuthorChannelId());
					}
				}
			});

			// change the width dimensions depending on whether the comment is a top level or a child
			ViewGroup.LayoutParams lp = thumbnailImageView.getLayoutParams();
			lp.width = (int) SkyTubeApp.getDimension(isTopLevelComment  ?  R.dimen.top_level_comment_thumbnail_width  :  R.dimen.child_comment_thumbnail_width);

			if (isTopLevelComment  &&  getChildrenCount(groupPosition) > 0) {
				viewRepliesTextView.setVisibility(View.VISIBLE);

				// on click, hide/show the comment replies
				commentView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View viewReplies) {
						if (expandableListView.isGroupExpanded(groupPosition)) {
							viewRepliesTextView.setText(R.string.view_replies);
							expandableListView.collapseGroup(groupPosition);
						} else {
							viewRepliesTextView.setText(R.string.hide_replies);
							expandableListView.expandGroup(groupPosition);
						}
					}
				});
			} else {
				viewRepliesTextView.setVisibility(View.GONE);
			}
		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////

	private class GetCommentsTask extends AsyncTask<Void, Void, List<YouTubeCommentThread>> {

		protected GetCommentsTask(String videoId) {
			if (getCommentThreads == null) {
				getCommentThreads = new GetCommentThreads();
				try {
					getCommentThreads.init(videoId);
				} catch (Throwable tr) {
					Toast.makeText(expandableListView.getContext(), R.string.error_get_comments, Toast.LENGTH_LONG).show();
				}
			}
		}

		@Override
		protected void onPreExecute() {
			commentsProgressBar.setVisibility(View.VISIBLE);
			noVideoCommentsView.setVisibility(View.GONE);
		}

		@Override
		protected  List<YouTubeCommentThread> doInBackground(Void... params) {
			return getCommentThreads.get();
		}

		@Override
		protected void onPostExecute(List<YouTubeCommentThread> commentThreadsList) {
			if (commentThreadsList != null) {
				if (commentThreadsList.size() > 0) {
					CommentsAdapter.this.commentThreadsList.addAll(commentThreadsList);
					CommentsAdapter.this.notifyDataSetChanged();
				} else {
					noVideoCommentsView.setVisibility(View.VISIBLE);
				}
			}

			commentsProgressBar.setVisibility(View.GONE);
			getCommentsTask = null;
		}

	}

}
