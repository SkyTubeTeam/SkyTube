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

package free.rm.skytube.gui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import butterknife.BindView;
import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.VideoCategory;
import free.rm.skytube.businessobjects.YouTube.POJOs.CardData;
import free.rm.skytube.businessobjects.YouTube.newpipe.ContentId;
import free.rm.skytube.businessobjects.db.BookmarksDb;
import free.rm.skytube.businessobjects.interfaces.CardListener;
import free.rm.skytube.gui.businessobjects.adapters.OrderableVideoGridAdapter;
import free.rm.skytube.gui.businessobjects.fragments.OrderableVideosGridFragment;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;

/**
 * Fragment that displays bookmarked videos.
 */
public class BookmarksFragment extends OrderableVideosGridFragment implements CardListener {
	@BindView(R.id.noBookmarkedVideosText)
	View noBookmarkedVideosText;

	public BookmarksFragment() {
	}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, @Nullable Bundle savedInstanceState) {
        setVideoGridAdapter(new OrderableVideoGridAdapter(BookmarksDb.getBookmarksDb()));
        BookmarksDb.getBookmarksDb().registerListener(this);
        View view = super.onCreateView(inflater, container, savedInstanceState);
        setListVisible(false);
        swipeRefreshLayout.setEnabled(false);

        populateList();
        return view;
    }

    private void populateList() {
        BookmarksDb.getBookmarksDb().getTotalBookmarkCount()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess(numberOfBookmarks -> {
                    if (numberOfBookmarks > 0 && swipeRefreshLayout != null) {
                        setListVisible(true);
                        // swipeRefreshLayout.setRefreshing(true);
                    }
                }).subscribe();
    }

    @Override
    public void onCardAdded(final CardData card) {
        videoGridAdapter.onCardAdded(card);
        setListVisible(true);
    }

    @Override
    public void onCardDeleted(final ContentId contentId) {
        videoGridAdapter.onCardDeleted(contentId);
        if (videoGridAdapter.getItemCount() == 0) {
            setListVisible(false);
        }
    }

	@Override
	protected int getLayoutResource() {
		return R.layout.fragment_bookmarks;
	}
	

	@Override
	protected VideoCategory getVideoCategory() {
		return VideoCategory.BOOKMARKS_VIDEOS;
	}
	

	@Override
	public String getFragmentName() {
		return SkyTubeApp.getStr(R.string.bookmarks);
	}

	@Override
	public int getPriority() {
		return 3;
	}

	@Override
	public String getBundleKey() {
		return MainFragment.BOOKMARKS_FRAGMENT;
	}

    @Override
    public void onDestroyView() {
        BookmarksDb.getBookmarksDb().unregisterListener(this);
        super.onDestroyView();
    }

    private void setListVisible(boolean visible) {
        if (visible) {
            swipeRefreshLayout.setVisibility(View.VISIBLE);
            noBookmarkedVideosText.setVisibility(View.GONE);
        } else {
            swipeRefreshLayout.setVisibility(View.GONE);
            noBookmarkedVideosText.setVisibility(View.VISIBLE);
        }
    }

}
