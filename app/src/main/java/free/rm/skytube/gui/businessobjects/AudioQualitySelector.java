/*
 * SkyTube
 * Copyright (C) 2026  SkyTube Project
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
import android.widget.PopupMenu;
import android.widget.TextView;

import java.util.List;

import free.rm.skytube.R;

/**
 * Lets the user choose the audio quality for the currently selected language.
 */
public class AudioQualitySelector extends TrackSelector<TrackSelectionManager.AudioTrackInfo> {
    public AudioQualitySelector(Context context, TextView displayTextView,
                                TrackSelectionManager trackSelectionManager) {
        super(context, displayTextView, trackSelectionManager, 400, null);
    }

    @Override
    protected int getEmptyLabelResId() {
        return R.string.audio_quality_selector_label;
    }

    @Override
    protected String getNoTracksMessage() {
        return resources.getString(R.string.no_audio_qualities);
    }

    @Override
    protected List<TrackSelectionManager.AudioTrackInfo> getTracks() {
        return trackSelectionManager.getAvailableAudioQualities();
    }

    @Override
    protected int getCurrentIndex() {
        return trackSelectionManager.getCurrentAudioQualityIndex();
    }

    @Override
    protected void addMenuItems() {
        for (int i = 0; i < currentTracks.size(); i++) {
            popupMenu.getMenu().add(groupId, i, Menu.NONE, currentTracks.get(i).getDisplayName());
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        final int index = item.getItemId();
        if (index >= 0 && index < currentTracks.size()
                && trackSelectionManager.setAudioQuality(index)) {
            currentIndex = index;
            updateDisplayText();
        }
        return true;
    }

    @Override
    public void onDismiss(PopupMenu menu) {}
}
