/*
 * SkyTube
 * Copyright (C) 2020  Zsombor Gegesy
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
package free.rm.skytube.gui.activities;

import androidx.annotation.StringRes;

import free.rm.skytube.R;
import free.rm.skytube.businessobjects.YouTube.YouTubeTasks;
import free.rm.skytube.businessobjects.YouTube.newpipe.ContentId;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

/**
 * Activity that receives an intent from other apps in order to bookmark a video from another app.
 * This Activity uses a transparent theme, and finishes right away, so as not to take focus from the sharing app.s
 */
public class ShareBookmarkActivity extends ExternalUrlActivity {
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    @Override
    void handleVideoContent(ContentId contentId) {
        compositeDisposable.add(YouTubeTasks.getVideoDetails(this, contentId)
                .flatMapCompletable(video -> {
                    if (video != null) {
                        return video.bookmarkVideo(free.rm.skytube.gui.activities.ShareBookmarkActivity.this).ignoreElement();
                    } else {
                        invalidUrlError();
                    }
                    finish();
                    return Completable.complete();
                }).subscribe());
    }

    @Override
    protected void onDestroy() {
        compositeDisposable.clear();
        super.onDestroy();
    }

    @StringRes
    int invalidUrlErrorMessage() {
        return R.string.bookmark_share_invalid_url;
    }
}
