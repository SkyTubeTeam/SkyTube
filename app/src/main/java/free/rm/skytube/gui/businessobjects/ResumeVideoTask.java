/*
 * SkyTube
 * Copyright (C) 2018  Zsombor Gegesy
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
import android.support.annotation.NonNull;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;
import free.rm.skytube.businessobjects.db.PlaybackStatusDb;

/**
 * Class to encapsulate the functionality to ask the user, if they want to resume playing
 * the video.
 */
public class ResumeVideoTask {

    public static interface Callback {
        void loadVideo(int position);
    }

    final Context context;
    final Callback callback;
    final YouTubeVideo youTubeVideo;

    public ResumeVideoTask(Context context, YouTubeVideo youTubeVideo, Callback callback) {
        this.context = context;
        this.youTubeVideo = youTubeVideo;
        this.callback = callback;
    }

    /**
     * Ask the user if he wants to resume playing this video
     * (if he has played it in the past...)
     *
     */
    public void ask() {
        if(!SkyTubeApp.getPreferenceManager().getBoolean(context.getString(R.string.pref_key_disable_playback_status), false)) {
            final PlaybackStatusDb.VideoWatchedStatus watchStatus = PlaybackStatusDb.getVideoDownloadsDb().getVideoWatchedStatus(youTubeVideo);
            if (watchStatus.getPosition() > 0) {
                new SkyTubeMaterialDialog(context)
                        .content(R.string.should_resume)
                        .positiveText(R.string.resume)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                callback.loadVideo((int) watchStatus.getPosition());
                            }
                        })
                        .negativeText(R.string.no)
                        .onNegative(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                callback.loadVideo(0);
                            }
                        })
                        .show();
            } else {
                callback.loadVideo(0);
            }
        } else {
            callback.loadVideo(0);
        }

    }
}
