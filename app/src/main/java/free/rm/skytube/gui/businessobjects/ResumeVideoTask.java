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

import androidx.annotation.NonNull;

import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.db.PlaybackStatusDb;

/**
 * Class to encapsulate the functionality to ask the user, if they want to resume playing
 * the video.
 */
public class ResumeVideoTask {

    public interface Callback {
        void loadVideo(int position);
    }

    private final @NonNull Context context;
    private final @NonNull Callback callback;
    private final @NonNull String videoId;

    public ResumeVideoTask(@NonNull Context context, @NonNull String videoId, @NonNull Callback callback) {
        this.context = context;
        this.videoId = videoId;
        this.callback = callback;
    }

    /**
     * Ask the user if he wants to resume playing this video
     * (if he has played it in the past...)
     *
     */
    public void ask() {
        if (SkyTubeApp.getSettings().isPlaybackStatusEnabled()) {
            final PlaybackStatusDb.VideoWatchedStatus watchStatus = PlaybackStatusDb.getPlaybackStatusDb().getVideoWatchedStatus(videoId);
            if (watchStatus.getPosition() > 0) {
                new SkyTubeMaterialDialog(context)
                        .onNegativeOrCancel((dialog) -> callback.loadVideo(0))
                        .content(R.string.should_resume)
                        .positiveText(R.string.resume)
                        .onPositive((dialog, which) -> callback.loadVideo((int) watchStatus.getPosition()))
                        .negativeText(R.string.no)
                        .show();
            } else {
                callback.loadVideo(0);
            }
        } else {
            callback.loadVideo(0);
        }

    }
}
