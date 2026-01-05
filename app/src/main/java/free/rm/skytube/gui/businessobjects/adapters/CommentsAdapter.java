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
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.mikepenz.iconics.IconicsColor;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.iconics.IconicsSize;
import com.mikepenz.iconics.typeface.library.materialdesigniconic.MaterialDesignIconic;

import org.schabi.newpipe.extractor.comments.CommentsInfoItem;
import org.schabi.newpipe.extractor.stream.Description;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.AsyncTaskParallel;
import free.rm.skytube.businessobjects.YouTube.newpipe.ChannelId;
import free.rm.skytube.businessobjects.YouTube.newpipe.CommentPagerInterface;
import free.rm.skytube.businessobjects.YouTube.newpipe.NewPipeException;
import free.rm.skytube.businessobjects.YouTube.newpipe.NewPipeService;
import free.rm.skytube.businessobjects.YouTube.newpipe.NewPipeUtils;
import free.rm.skytube.databinding.CommentBinding;
import free.rm.skytube.gui.businessobjects.views.Linker;

/**
 * An adapter that will display comments in an {@link ExpandableListView}.
 */
public class CommentsAdapter extends BaseExpandableListAdapter {

    private final CommentPagerInterface commentThreadPager;
    private GetCommentsTask getCommentsTask = null;
    private final ExpandableListView expandableListView;
    private final View commentsProgressBar;
    private final View noVideoCommentsView;
    private final View disabledCommentsView;
    private final LayoutInflater layoutInflater;
    private final Context context;
    private final IconicsDrawable heartedIcon;
    private final IconicsDrawable pinnedIcon;

    private final Linker.CurrentActivity currentActivity;

    private final Map<String, List<CommentsInfoItem>> replyMap = new HashMap<>();
    private final Set<String> currentlyFetching = new HashSet<>();

    private static final String TAG = CommentsAdapter.class.getSimpleName();

    protected CommentsAdapter(Context context, Linker.CurrentActivity currentActivity, @NonNull CommentPagerInterface commentPager,
                              ExpandableListView expandableListView, View commentsProgressBar, View noVideoCommentsView,
                              View disabledCommentsView, LayoutInflater layoutInflater,
                              IconicsDrawable heartedIcon, IconicsDrawable pinnedIcon) {
        this.context = context;
        this.currentActivity = currentActivity;
        this.commentThreadPager = Objects.requireNonNull(commentPager, "commentPager");
        this.expandableListView = expandableListView;
        this.commentsProgressBar = commentsProgressBar;
        this.noVideoCommentsView = noVideoCommentsView;
        this.disabledCommentsView = disabledCommentsView;
        this.layoutInflater = layoutInflater;
        this.heartedIcon = heartedIcon;
        this.pinnedIcon = pinnedIcon;

        // Initialize UI components
        this.expandableListView.setAdapter(this);
        this.expandableListView.setOnGroupClickListener((parent, v, groupPosition, id) -> true);
    }

    /**
     * Initiate fetching of the comments if not already tried.
     */
    synchronized void fetchComments() {
        if (getCommentsTask == null) {
            Log.w(TAG, "Getting next page of comments...");
            getCommentsTask = new GetCommentsTask();
            getCommentsTask.execute();
        }
    }

    @Override
    public int getGroupCount() {
        // Return the number of comments that have actually been loaded, not the total
        // count. This prevents the adapter from trying to display positions that
        // haven't been loaded yet
        return commentThreadPager.getLoadedCount();
    }

    /**
     * @param groupPosition the position of the group for which the children
     *                      count should be returned
     * @return the number of replies, which are already loaded
     */
    @Override
    public int getChildrenCount(int groupPosition) {
        CommentsInfoItem comment = commentThreadPager.getComment(groupPosition);
        if (comment != null && comment.getReplyCount() > 0) {
            List<CommentsInfoItem> replies = replyMap.get(comment.getCommentId());
            return replies != null ? replies.size() : 0;
        }
        return 0;
    }

    @Override
    public Object getGroup(int groupPosition) {
        return commentThreadPager.getComment(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return getComment(groupPosition, childPosition);
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
        CommentsInfoItem comment = commentThreadPager.getComment(groupPosition);
        final CommentViewHolder viewHolder = getCommentViewHolder(convertView, parent);
        if (comment != null) {
            viewHolder.updateInfo(comment, true, groupPosition);
        }

        // if it reached near the bottom of the list, then try to get the next page of
        // videos
        // Trigger when we're within 5 positions of the end to ensure smooth loading
        if (groupPosition >= getGroupCount() - 5) {
            fetchComments();
        }
        if (isExpanded && comment != null) {
            ensureRepliesLoaded(comment);
        }
        return viewHolder.getView();
    }

    private synchronized void ensureRepliesLoaded(@NonNull CommentsInfoItem comment) {
        if (replyMap.get(comment.getCommentId()) == null && comment.getReplies() != null) {
            new GetReplies().executeInParallel(comment);
        }
    }

    /**
     * Adds replies to the reply map for a given comment.
     * Made protected for testing purposes.
     *
     * @param comment    The parent comment
     * @param newReplies The new replies to add
     */
    protected synchronized void addReplies(@NonNull CommentsInfoItem comment, @NonNull List<CommentsInfoItem> newReplies) {
        List<CommentsInfoItem> replies = replyMap.get(comment.getCommentId());
        if (replies == null) {
            replies = new ArrayList<>();
            replyMap.put(comment.getCommentId(), replies);
        }
        replies.addAll(newReplies);
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView,
                             ViewGroup parent) {
        Log.d(TAG, "getChildView " + groupPosition + " " + childPosition + " lastChild=" + isLastChild);
        CommentsInfoItem comment = getComment(groupPosition, childPosition);
        final CommentViewHolder viewHolder = getCommentViewHolder(convertView, parent);
        if (comment != null) {
            viewHolder.updateInfo(comment, false, groupPosition);
        }

        return viewHolder.getView();
    }

    private synchronized CommentsInfoItem getComment(int idx, int childIdx) {
        CommentsInfoItem parent = commentThreadPager.getComment(idx);
        if (parent == null) {
            return null;
        }
        List<CommentsInfoItem> replies = replyMap.get(parent.getCommentId());
        if (replies != null && 0 <= childIdx) {
            int currentReplyListSize = replies.size();
            // Only fetch more replies if we're near the end of the current list AND we
            // haven't already fetched all replies
            if (currentReplyListSize < parent.getReplyCount() && childIdx >= currentReplyListSize - 5
                    && childIdx < currentReplyListSize) {
                synchronized (currentlyFetching) {
                    if (parent.getReplies() != null && currentlyFetching.add(parent.getCommentId())) {
                        Log.i(TAG,
                                String.format(
                                        "Fetching more replies for %s - currentReplyListSize: %s, childIdx: %s - %s",
                                        parent.getCommentId(), currentReplyListSize, childIdx, parent.getReplyCount()));
                        new GetReplies().executeInParallel(parent);
                    } else {
                        Log.i(TAG, String.format("No reply fetch for %s - currentReplyListSize: %s, childIdx: %s",
                                parent.getCommentId(), currentReplyListSize, childIdx));
                    }
                }
            }
            if (childIdx < currentReplyListSize) {
                return replies.get(childIdx);
            }
        }
        return null;
    }

    protected void removeFromCurrentlyFetching(CommentsInfoItem item) {
        synchronized (currentlyFetching) {
            currentlyFetching.remove(item.getCommentId());
        }
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return false;
    }

    private @NonNull CommentViewHolder getCommentViewHolder(View convertView, ViewGroup parent) {
        if (convertView != null) {
            Object tag = convertView.getTag();
            if (tag instanceof CommentViewHolder) {
                return (CommentViewHolder) tag;
            }
        }
        final View row = layoutInflater.inflate(R.layout.comment, parent, false);
        final CommentViewHolder viewHolder = new CommentViewHolder(CommentBinding.bind(row));
        row.setTag(viewHolder);
        return viewHolder;
    }

    ////////////

    private class CommentViewHolder {
        private final CommentBinding binding;

        protected CommentViewHolder(CommentBinding binding) {
            this.binding = binding;
        }

        public View getView() {
            return binding.getRoot();
        }

        protected void updateInfo(final CommentsInfoItem comment, boolean isTopLevelComment, final int groupPosition) {
            binding.heartedView.setImageDrawable(heartedIcon);
            binding.pinnedView.setImageDrawable(pinnedIcon);
            binding.heartedView.setVisibility(comment.isHeartedByUploader() ? View.VISIBLE : View.GONE);
            binding.pinnedView.setVisibility(comment.isPinned() ? View.VISIBLE : View.GONE);
            binding.commentPaddingView.setVisibility(isTopLevelComment ? View.GONE : View.VISIBLE);
            binding.authorTextView.setText(comment.getUploaderName());
            Linker.configure(binding.commentTextView, currentActivity);
            Linker.setTextAndLinkify(binding.commentTextView, getCommentText(comment));
            binding.commentDateTextView.setText(comment.getTextualUploadDate());
            binding.commentUpvotesTextView.setText(String.valueOf(comment.getLikeCount()));
            Glide.with(context)
                    .load(NewPipeUtils.getThumbnailUrl(comment))
                    .apply(new RequestOptions().placeholder(R.drawable.channel_thumbnail_default))
                    .into(binding.commentThumbnailImageView);

            binding.commentThumbnailImageView.setOnClickListener(view -> {
                if (comment.getUploaderUrl() != null) {
                    SkyTubeApp.launchChannel(new ChannelId(comment.getUploaderUrl()), context);
                }
            });

            // change the width dimensions depending on whether the comment is a top level
            // or a child
            ViewGroup.LayoutParams lp = binding.commentThumbnailImageView.getLayoutParams();
            lp.width = (int) SkyTubeApp.getDimension(isTopLevelComment ? R.dimen.top_level_comment_thumbnail_width
                    : R.dimen.child_comment_thumbnail_width);

            if (isTopLevelComment && comment.getReplyCount() > 0) {
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

    private static String getCommentText(CommentsInfoItem infoItem) {
        Description description = infoItem.getCommentText();
        return description != null ? description.getContent() : "";
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    private class GetReplies extends AsyncTaskParallel<CommentsInfoItem, Void, List<String>> {

        @Override
        protected List<String> doInBackground(CommentsInfoItem... repliesFor) {
            List<String> ids = new ArrayList<>(repliesFor.length);
            for (CommentsInfoItem item : repliesFor) {
                try {
                    List<CommentsInfoItem> replies = commentThreadPager.getPageAndExtract(item.getReplies());
                    addReplies(item, replies);
                    if (commentThreadPager.isHasNextPage()) {
                        item.setReplies(commentThreadPager.getNextPageInfo());
                    } else {
                        item.setReplies(null);
                    }
                    ids.add(item.getCommentId());
                } catch (NewPipeException e) {
                    lastException = e;
                    Log.e(TAG, "Unable to get replies " + item + " -> " + e.getMessage(), e);
                } finally {
                    removeFromCurrentlyFetching(item);
                }
            }
            return ids;
        }

        @Override
        protected void onPostExecute(List<String> commentsInfoItems) {
            if (!commentsInfoItems.isEmpty()) {
                CommentsAdapter.this.notifyDataSetChanged();
            }
            super.onPostExecute(commentsInfoItems);
        }

        @Override
        protected void showErrorToUi() {
            if (lastException != null) {
                Toast.makeText(CommentsAdapter.this.context, lastException.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private class GetCommentsTask extends AsyncTask<Void, Void, List<CommentsInfoItem>> {

        @Override
        protected void onPreExecute() {
            commentsProgressBar.setVisibility(View.VISIBLE);
            noVideoCommentsView.setVisibility(View.GONE);
        }

        @Override
        protected List<CommentsInfoItem> doInBackground(Void... params) {
            return commentThreadPager.getSafeNextPage();
        }

        @Override
        protected void onPostExecute(List<CommentsInfoItem> newComments) {
            SkyTubeApp.notifyUserOnError(expandableListView.getContext(), commentThreadPager.getLastException());

            if (newComments != null) {
                if (newComments.size() > 0) {
                    CommentsAdapter.this.notifyDataSetChanged();
                }
                if (commentThreadPager.isCommentsDisabled() && disabledCommentsView != null) {
                    disabledCommentsView.setVisibility(View.VISIBLE);
                } else if (commentThreadPager.getCommentCount() == 0) {
                    noVideoCommentsView.setVisibility(View.VISIBLE);
                }
            }

            commentsProgressBar.setVisibility(View.GONE);
            getCommentsTask = null;
        }

    }

    /**
     * Creates an adapter for the given video ID.
     * This is the main factory method for production use.
     */
    public static @Nullable BaseExpandableListAdapter createAdapter(Context context, Linker.CurrentActivity currentActivity,
                                                                    String videoId, ExpandableListView expandableListView, View commentsProgressBar, View noVideoCommentsView,
                                                                    View disabledCommentsView) {
        if (SkyTubeApp.getSettings().isUseNewPipe()) {
            try {
                CommentPagerInterface commentPager = NewPipeService.get().getCommentPager(videoId);
                LayoutInflater layoutInflater = LayoutInflater.from(expandableListView.getContext());

                // Create icons for production use
                IconicsDrawable heartedIcon = new IconicsDrawable(context)
                        .icon(MaterialDesignIconic.Icon.gmi_favorite)
                        .color(IconicsColor.colorInt(Color.RED))
                        .size(IconicsSize.TOOLBAR_ICON_SIZE)
                        .padding(IconicsSize.TOOLBAR_ICON_PADDING);
                IconicsDrawable pinnedIcon = new IconicsDrawable(context)
                        .icon(MaterialDesignIconic.Icon.gmi_pin)
                        .color(IconicsColor.colorInt(Color.RED))
                        .size(IconicsSize.TOOLBAR_ICON_SIZE)
                        .padding(IconicsSize.TOOLBAR_ICON_PADDING);

                CommentsAdapter commentsAdapter = new CommentsAdapter(context, currentActivity, commentPager, expandableListView,
                        commentsProgressBar, noVideoCommentsView, disabledCommentsView, layoutInflater, heartedIcon,
                        pinnedIcon);
                commentsAdapter.fetchComments();
                return commentsAdapter;
            } catch (Exception e) {
                Log.e(TAG, "Failed to create CommentsAdapter: " + e.getMessage(), e);
                SkyTubeApp.notifyUserOnError(context, e);
                return null;
            }
        } else {
            return new LegacyCommentsAdapter(context, videoId, expandableListView, commentsProgressBar,
                    noVideoCommentsView);
        }
    }
}
