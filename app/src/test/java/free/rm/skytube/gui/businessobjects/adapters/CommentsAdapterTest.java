/*
 * SkyTube
 * Copyright (C) 2024  SkyTube Team
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ExpandableListView;

import com.mikepenz.iconics.IconicsDrawable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.schabi.newpipe.extractor.comments.CommentsInfoItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import free.rm.skytube.businessobjects.YouTube.newpipe.CommentPagerInterface;
import free.rm.skytube.gui.businessobjects.views.Linker;

@ExtendWith(MockitoExtension.class)
class CommentsAdapterTest {

    @Mock
    private Context mockContext;

    @Mock
    private Linker.CurrentActivity mockCurrentActivity;

    @Mock
    private CommentPagerInterface mockCommentPager;

    @Mock
    private ExpandableListView mockExpandableListView;

    @Mock
    private View mockCommentsProgressBar;

    @Mock
    private View mockNoVideoCommentsView;

    @Mock
    private View mockDisabledCommentsView;

    @Mock
    private LayoutInflater mockLayoutInflater;

    @Mock
    private IconicsDrawable mockHeartedIcon;

    @Mock
    private IconicsDrawable mockPinnedIcon;

    private CommentsAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new CommentsAdapter(
                mockContext,
                mockCurrentActivity,
                mockCommentPager,
                mockExpandableListView,
                mockCommentsProgressBar,
                mockNoVideoCommentsView,
                mockDisabledCommentsView,
                mockLayoutInflater,
                mockHeartedIcon,
                mockPinnedIcon
        );
    }

    @Test
    void testWithNullPager() {
        assertThrows(NullPointerException.class, () ->
                new CommentsAdapter(
                        mockContext,
                        mockCurrentActivity,
                        /* commentPager */null,
                        mockExpandableListView,
                        mockCommentsProgressBar,
                        mockNoVideoCommentsView,
                        mockDisabledCommentsView,
                        mockLayoutInflater,
                        mockHeartedIcon,
                        mockPinnedIcon
                ));
    }

    @Test
    void testGetGroupCountUsesLoadedCount() {
        // This tests the key fix: using getLoadedCount() instead of getCommentCount()
        when(mockCommentPager.getLoadedCount()).thenReturn(5);

        assertEquals(5, adapter.getGroupCount(), "Should use loaded count, not total count");
    }

    @Test
    void testGetGroupCountWithDifferentLoadedCounts() {
        // Test various loaded count scenarios
        when(mockCommentPager.getLoadedCount()).thenReturn(0);
        assertEquals(0, adapter.getGroupCount(), "Should return 0 when no comments loaded");

        when(mockCommentPager.getLoadedCount()).thenReturn(1);
        assertEquals(1, adapter.getGroupCount(), "Should return 1 when 1 comment loaded");

        when(mockCommentPager.getLoadedCount()).thenReturn(25);
        assertEquals(25, adapter.getGroupCount(), "Should return 25 when 25 comments loaded");
    }

    @Test
    void testGetGroupReturnsCommentFromPager() {
        CommentsInfoItem mockComment = mock(CommentsInfoItem.class);
        when(mockCommentPager.getComment(0)).thenReturn(mockComment);

        Object result = adapter.getGroup(0);
        assertSame(mockComment, result, "Should return comment from pager");

        verify(mockCommentPager, times(1)).getComment(0);
    }

    @Test
    void testGetGroupReturnsNullWhenPagerReturnsNull() {
        when(mockCommentPager.getComment(0)).thenReturn(null);

        Object result = adapter.getGroup(0);
        assertNull(result, "Should return null when pager returns null");
    }

    @Test
    void testGetChildIdCalculation() {
        // Test the child ID calculation: (groupPosition * 1024) + childPosition
        long childId1 = adapter.getChildId(0, 0);
        assertEquals(0, childId1, "Child ID for (0,0) should be 0");

        long childId2 = adapter.getChildId(2, 3);
        assertEquals(2051, childId2, "Child ID for (2,3) should be 2051");

        long childId3 = adapter.getChildId(5, 10);
        assertEquals(5130, childId3, "Child ID for (5,10) should be 5130");
    }

    @Test
    void testGetGroupId() {
        assertEquals(0, adapter.getGroupId(0), "Group ID should match position");
        assertEquals(5, adapter.getGroupId(5), "Group ID should match position");
        assertEquals(100, adapter.getGroupId(100), "Group ID should match position");
    }

    @Test
    void testHasStableIds() {
        assertTrue(adapter.hasStableIds(), "Adapter should have stable IDs");
    }

    @Test
    void testIsChildSelectable() {
        assertFalse(adapter.isChildSelectable(0, 0), "Child items should not be selectable");
        assertFalse(adapter.isChildSelectable(5, 10), "Child items should not be selectable");
    }

    @Test
    void testGetChildrenCountWithCommentsNotFetched() {
        CommentsInfoItem mockComment = mock(CommentsInfoItem.class);
        when(mockComment.getReplyCount()).thenReturn(3);
        when(mockCommentPager.getComment(0)).thenReturn(mockComment);

        int childrenCount = adapter.getChildrenCount(0);
        assertEquals(0, childrenCount, "Should return 0 reply count when comments hasn't fetched");
    }

    @Test
    void testGetChildrenCountWithCommentHavingNoReplies() {
        CommentsInfoItem mockComment = mock(CommentsInfoItem.class);
        when(mockComment.getReplyCount()).thenReturn(0);
        when(mockCommentPager.getComment(0)).thenReturn(mockComment);

        int childrenCount = adapter.getChildrenCount(0);
        assertEquals(0, childrenCount, "Should return 0 when comment has no replies");
    }

    @Test
    void testGetChildrenCountWithNullComment() {
        when(mockCommentPager.getComment(0)).thenReturn(null);

        assertEquals(0, adapter.getChildrenCount(0), "Should return 0 when comment is null");
    }

    @Test
    void testAdapterUsesInjectedPager() {
        // Test that the adapter properly uses the injected pager
        // We can verify this by checking that getGroupCount() uses the pager's getLoadedCount()
        when(mockCommentPager.getLoadedCount()).thenReturn(42);

        assertEquals(42, adapter.getGroupCount(), "Should use the injected pager's loaded count");
    }

    @Test
    void testPaginationLogic() {
        // Test the key insight: loaded count vs total count
        when(mockCommentPager.getLoadedCount()).thenReturn(10);

        assertEquals(10, adapter.getGroupCount(), "Should only show loaded comments");
    }

    @Test
    void testEdgeCaseLargeLoadedCount() {
        when(mockCommentPager.getLoadedCount()).thenReturn(1000);

        assertEquals(1000, adapter.getGroupCount(), "Should handle large loaded counts");
    }

    @Test
    void testEdgeCaseZeroLoadedCount() {
        when(mockCommentPager.getLoadedCount()).thenReturn(0);

        assertEquals(0, adapter.getGroupCount(), "Should handle zero loaded count");
        assertEquals(0, adapter.getChildrenCount(0), "Should handle zero children count");
    }

    //=========================================================================
    // Reply Testing - New tests for reply functionality
    //=========================================================================

    @Test
    void testAddRepliesAddsToReplyMap() {
        // Create a mock parent comment
        CommentsInfoItem parentComment = mock(CommentsInfoItem.class);
        when(parentComment.getCommentId()).thenReturn("parent_123");
        when(parentComment.getReplyCount()).thenReturn(2);

        // Create mock replies
        List<CommentsInfoItem> mockReplies = new ArrayList<>();
        CommentsInfoItem reply1 = mock(CommentsInfoItem.class);
        CommentsInfoItem reply2 = mock(CommentsInfoItem.class);
        mockReplies.add(reply1);
        mockReplies.add(reply2);

        // Mock the pager to return our parent comment at position 0
        when(mockCommentPager.getComment(0)).thenReturn(parentComment);

        // Call addReplies
        adapter.addReplies(parentComment, mockReplies);

        // Verify replies were added by checking children count
        int childrenCount = adapter.getChildrenCount(0);
        assertEquals(2, childrenCount, "Should have 2 replies stored");

        // Verify we can get the child items
        Object child1 = adapter.getChild(0, 0);
        Object child2 = adapter.getChild(0, 1);
        assertSame(reply1, child1, "First child should be the first reply");
        assertSame(reply2, child2, "Second child should be the second reply");
    }

    @Test
    void testAddRepliesHandlesMultipleCallsForSameComment() {
        // Create a mock parent comment
        CommentsInfoItem parentComment = mock(CommentsInfoItem.class);
        when(parentComment.getCommentId()).thenReturn("parent_456");
        when(parentComment.getReplyCount()).thenReturn(5);

        // Mock the pager to return our parent comment at position 0
        when(mockCommentPager.getComment(0)).thenReturn(parentComment);

        // First batch of replies
        List<CommentsInfoItem> firstBatch = new ArrayList<>();
        firstBatch.add(mock(CommentsInfoItem.class));
        firstBatch.add(mock(CommentsInfoItem.class));

        // Second batch of replies
        List<CommentsInfoItem> secondBatch = new ArrayList<>();
        secondBatch.add(mock(CommentsInfoItem.class));
        secondBatch.add(mock(CommentsInfoItem.class));
        secondBatch.add(mock(CommentsInfoItem.class));

        // Add both batches
        adapter.addReplies(parentComment, firstBatch);
        adapter.addReplies(parentComment, secondBatch);

        // Verify all replies were added by checking children count
        int childrenCount = adapter.getChildrenCount(0);
        assertEquals(5, childrenCount, "Should have 5 replies total after two batches");
    }

    @Test
    void testAddRepliesHandlesEmptyReplyList() {
        // Create a mock parent comment
        CommentsInfoItem parentComment = mock(CommentsInfoItem.class);
        when(parentComment.getCommentId()).thenReturn("parent_789");
        when(parentComment.getReplyCount()).thenReturn(0);

        // Mock the pager to return our parent comment at position 0
        when(mockCommentPager.getComment(0)).thenReturn(parentComment);

        // Try to add empty list
        adapter.addReplies(parentComment, Collections.emptyList());

        // Verify no crash by checking children count
        int childrenCount = adapter.getChildrenCount(0);
        assertEquals(0, childrenCount, "Null replies list should result in 0 children");
    }

    @Test
    void testGetChildrenCountUsesReplyMap() {
        // Create a mock parent comment with replies
        CommentsInfoItem parentComment = mock(CommentsInfoItem.class);
        when(parentComment.getCommentId()).thenReturn("parent_with_replies");
        when(parentComment.getReplyCount()).thenReturn(3);

        // Mock the pager to return our parent comment
        when(mockCommentPager.getComment(0)).thenReturn(parentComment);

        // Add some replies to the map
        List<CommentsInfoItem> replies = new ArrayList<>();
        replies.add(mock(CommentsInfoItem.class));
        replies.add(mock(CommentsInfoItem.class));
        adapter.addReplies(parentComment, replies);

        // Test getChildrenCount
        int childrenCount = adapter.getChildrenCount(0);

        // Should return the number of replies we added (2), not the replyCount from the comment (3)
        assertEquals(2, childrenCount, "Should return actual number of loaded replies");
    }

    @Test
    void testGetChildrenCountReturnsZeroForCommentWithNoRepliesInMap() {
        // Create a mock parent comment with reply count but no replies in map
        CommentsInfoItem parentComment = mock(CommentsInfoItem.class);
        when(parentComment.getCommentId()).thenReturn("parent_no_replies");
        when(parentComment.getReplyCount()).thenReturn(5); // Claims to have 5 replies

        // Mock the pager to return our parent comment
        when(mockCommentPager.getComment(0)).thenReturn(parentComment);

        // Don't add any replies to the map

        // Test getChildrenCount
        int childrenCount = adapter.getChildrenCount(0);

        // Should return 0 since no replies are actually loaded
        assertEquals(0, childrenCount, "Should return 0 when no replies are loaded");
    }

    @Test
    void testGetChildrenCountReturnsZeroForNullComment() {
        // Mock the pager to return null
        when(mockCommentPager.getComment(0)).thenReturn(null);

        // Test getChildrenCount
        int childrenCount = adapter.getChildrenCount(0);

        // Should return 0 for null comment
        assertEquals(0, childrenCount, "Should return 0 for null comment");
    }

    @Test
    void testAddRepliesIsThreadSafe() throws InterruptedException {
        // Create a mock parent comment
        CommentsInfoItem parentComment = mock(CommentsInfoItem.class);
        when(parentComment.getCommentId()).thenReturn("thread_safe_parent");
        when(parentComment.getReplyCount()).thenReturn(10);

        // Mock the pager to return our parent comment at position 0
        when(mockCommentPager.getComment(0)).thenReturn(parentComment);

        // Create multiple threads that add replies simultaneously
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Runnable> tasks = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            tasks.add(() -> {
                List<CommentsInfoItem> replies = new ArrayList<>();
                CommentsInfoItem reply = mock(CommentsInfoItem.class);
                replies.add(reply);
                adapter.addReplies(parentComment, replies);
            });
        }

        // Execute all tasks
        for (Runnable task : tasks) {
            executor.execute(task);
        }

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // Verify all replies were added correctly by checking children count
        int childrenCount = adapter.getChildrenCount(0);
        assertEquals(threadCount, childrenCount, "Should have added replies from all threads");
    }
}