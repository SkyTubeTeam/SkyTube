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
package free.rm.skytube.gui.businessobjects;

import android.content.Context;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.SimpleExoPlayer;

import java.util.Objects;

import free.rm.skytube.app.Utils;

public class PlaybackSpeedController implements PopupMenu.OnMenuItemClickListener, PopupMenu.OnDismissListener, View.OnClickListener {

    private final static float[] PLAYBACK_SPEEDS = {0.6f, 0.8f, 1f, 1.2f, 1.4f, 1.6f, 1.8f, 2f, 2.5f, 3f, 3.5f, 4f, 4.5f, 5f, 5.5f, 6f};
    private final static int GROUP_ID = 999;

    private final PopupMenu playbackSpeedPopupMenu;
    private final TextView playbackSpeedTextView;
    private final SimpleExoPlayer player;

    public PlaybackSpeedController(Context context, TextView playbackSpeedTextView, SimpleExoPlayer player) {
        this.player = Objects.requireNonNull(player, "SimpleExoPlayer");
        this.playbackSpeedTextView = Objects.requireNonNull(playbackSpeedTextView, "playbackSpeedTextView");
        this.playbackSpeedPopupMenu = new PopupMenu(context, playbackSpeedTextView);
        this.playbackSpeedTextView.setOnClickListener(this);
    }

    public void updateMenu() {

        playbackSpeedPopupMenu.getMenu().removeGroup(GROUP_ID);
        for (int i = 0; i < PLAYBACK_SPEEDS.length; i++) {
            playbackSpeedPopupMenu.getMenu().add(GROUP_ID, i, Menu.NONE, Utils.formatSpeed(PLAYBACK_SPEEDS[i]));
        }
        playbackSpeedTextView.setText(Utils.formatSpeed(getPlaybackSpeed()));
        playbackSpeedPopupMenu.setOnMenuItemClickListener(this);
        playbackSpeedPopupMenu.setOnDismissListener(this);
    }

    public float getPlaybackSpeed() {
        return getPlaybackParameters().speed;
    }

    public float getPlaybackPitch() {
        return getPlaybackParameters().pitch;
    }

    public boolean getPlaybackSkipSilence() {
        return getPlaybackParameters().skipSilence;
    }

    public void setPlaybackSpeed(float speed) {
        setPlaybackParameters(speed, getPlaybackPitch(), getPlaybackSkipSilence());
    }

    public PlaybackParameters getPlaybackParameters() {
        if (player == null) return PlaybackParameters.DEFAULT;
        final PlaybackParameters parameters = player.getPlaybackParameters();
        return parameters == null ? PlaybackParameters.DEFAULT : parameters;
    }

    public void setPlaybackParameters(float speed, float pitch, boolean skipSilence) {
        player.setPlaybackParameters(new PlaybackParameters(speed, pitch, skipSilence));
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        int speedIndex = item.getItemId();
        float speed = PLAYBACK_SPEEDS[speedIndex];

        setPlaybackSpeed(speed);
        playbackSpeedTextView.setText(Utils.formatSpeed(speed));

        return false;
    }

    @Override
    public void onDismiss(PopupMenu menu) {

    }

    @Override
    public void onClick(View v) {
        playbackSpeedPopupMenu.show();
    }
}
