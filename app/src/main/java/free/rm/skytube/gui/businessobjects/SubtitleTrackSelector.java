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
import android.widget.PopupMenu;
import android.widget.TextView;

import java.util.List;

import free.rm.skytube.R;

public class SubtitleTrackSelector extends TrackSelector<TrackSelectionManager.SubtitleTrackInfo> {
    private static final int OFF_ITEM_ID = 0;
    private static final int SUBTITLE_OFFSET = 100;

    public SubtitleTrackSelector(Context context, TextView displayTextView, TrackSelectionManager trackSelectionManager) {
        super(context, displayTextView, trackSelectionManager, 200, null);
    }

    @Override
    protected int getEmptyLabelResId() {
        return R.string.subtitle_selector_label;
    }

    @Override
    protected String getNoTracksMessage() {
        return resources.getString(R.string.no_subtitle_tracks);
    }

    protected List<TrackSelectionManager.SubtitleTrackInfo> getTracks() {
        return trackSelectionManager.getAvailableSubtitleTracks();
    }

    protected int getCurrentIndex() {
        return trackSelectionManager.getCurrentSubtitleTrackIndex();
    }

    protected void addMenuItems() {
        popupMenu.getMenu().add(groupId, OFF_ITEM_ID, Menu.NONE, resources.getString(R.string.track_off));
        for (int i = 0; i < currentTracks.size(); i++) {
            TrackSelectionManager.SubtitleTrackInfo track = currentTracks.get(i);
            String label = getTrackLabel(track);
            popupMenu.getMenu().add(groupId, i + SUBTITLE_OFFSET, Menu.NONE, label);
        }
    }

    private String getTrackLabel(TrackSelectionManager.SubtitleTrackInfo track) {
        String language = track.getDisplayName();
        if (language == null || language.equals("Unknown")) {
            language = track.getLanguage() != null ? track.getLanguage() : "und";
        }
        return language;
    }

    @Override
    public boolean onMenuItemClick(android.view.MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == OFF_ITEM_ID) {
            if (trackSelectionManager.disableSubtitles()) {
                currentIndex = -1;
                updateDisplayText();
            }
        } else {
            int index = itemId - SUBTITLE_OFFSET;
            if (index >= 0 && index < currentTracks.size()) {
                if (trackSelectionManager.setSubtitleTrack(index)) {
                    currentIndex = index;
                    updateDisplayText();
                }
            }
        }
        return true;
    }

    @Override
    public void onDismiss(PopupMenu menu) {}
}
