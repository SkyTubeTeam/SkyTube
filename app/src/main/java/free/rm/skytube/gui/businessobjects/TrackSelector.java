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
import android.content.res.Resources;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public abstract class TrackSelector<T extends TrackSelectionManager.SelectableTrackInfo>
        implements PopupMenu.OnMenuItemClickListener, PopupMenu.OnDismissListener, View.OnClickListener {
    protected final PopupMenu popupMenu;
    protected final TextView displayTextView;
    protected TrackSelectionManager trackSelectionManager;
    protected final Runnable onSelectionChanged;
    protected List<T> currentTracks;
    protected int currentIndex;

    protected final int groupId;
    protected final Resources resources;

    public TrackSelector(Context context, TextView displayTextView, TrackSelectionManager trackSelectionManager,
                     int groupId, Runnable onSelectionChanged) {
        this.displayTextView = displayTextView;
        this.trackSelectionManager = trackSelectionManager;
        this.onSelectionChanged = onSelectionChanged;
        this.popupMenu = new PopupMenu(context, displayTextView);
        this.groupId = groupId;
        this.resources = context.getResources();
        this.displayTextView.setOnClickListener(this);
    }

    /**
     * Every playback starts with a freshly built manager, so an already created selector must be
     * pointed at the new manager before its menu is refreshed.
     */
    public void setTrackSelectionManager(TrackSelectionManager trackSelectionManager) {
        this.trackSelectionManager = trackSelectionManager;
    }

    protected void notifySelectionChanged() {
        if (onSelectionChanged != null) {
            onSelectionChanged.run();
        }
    }

    protected abstract int getEmptyLabelResId();

    protected abstract String getNoTracksMessage();

    protected abstract List<T> getTracks();

    protected abstract int getCurrentIndex();

    protected abstract void addMenuItems();

    public void updateMenu() {
        if (trackSelectionManager == null) {
            displayTextView.setText(getEmptyLabel());
            return;
        }

        currentTracks = getTracks();
        popupMenu.getMenu().clear();

        if (currentTracks.isEmpty()) {
            displayTextView.setText(getEmptyLabel());
            return;
        }

        addMenuItems();

        currentIndex = getCurrentIndex();
        updateDisplayText();
        popupMenu.setOnMenuItemClickListener(this);
        popupMenu.setOnDismissListener(this);
    }

    protected String getEmptyLabel() {
        return resources.getString(getEmptyLabelResId());
    }

    protected void updateDisplayText() {
        if (currentIndex >= 0 && currentIndex < currentTracks.size()) {
            T track = currentTracks.get(currentIndex);
            String display = track.getDisplayName() != null ? track.getDisplayName() : track.getLanguage();
            displayTextView.setText(display);
        } else {
            displayTextView.setText(getEmptyLabel());
        }
    }

    @Override
    public void onClick(View v) {
        if (trackSelectionManager == null) {
            Toast.makeText(displayTextView.getContext(),
                    resources.getString(getNoTrackManagerMessageResId()),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        updateMenu();
        if (currentTracks != null && !currentTracks.isEmpty()) {
            popupMenu.show();
        } else {
            Toast.makeText(displayTextView.getContext(), getNoTracksMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    protected int getNoTrackManagerMessageResId() {
        return free.rm.skytube.R.string.no_track_manager;
    }
}