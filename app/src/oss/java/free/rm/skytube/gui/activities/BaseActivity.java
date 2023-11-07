/*
 * SkyTube
 * Copyright (C) 2015  Ramon Mifsud
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

import android.view.Menu;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.YouTube.newpipe.ChannelId;
import free.rm.skytube.databinding.ActivityMainBinding;
import free.rm.skytube.gui.businessobjects.MainActivityListener;

/**
 * Base Activity that any Activity that needs to support Chromecast functionality must extend. This OSS version just
 * contains a bunch of mostly no-op methods.
 */
public abstract class BaseActivity extends AppCompatActivity implements MainActivityListener {
	protected ActivityMainBinding binding;

	// No-op methods that aren't necessarily needed by all classes that extend this one
	protected void onOptionsMenuCreated(Menu menu) {}
	public void onLayoutSet() {}
	@Override
	public void onChannelClick(ChannelId channelId) {}
	public void redrawPanel() {}
	protected boolean isLocalPlayer() {
		return false;
	}
	protected void returnToMainAndResume() {}

	/**
	 * The extra variant needs to collapse the Chromecast Controller if it is visible and expanded,
	 * so its BaseActivity will check for that in its version of this method (and not do anything else).
	 * For the OSS variant, we simply return true, which will allow MainActivity.onBackPressed to check
	 * if mainFragment is visible, so it can return to the homescreen without exiting.
	 * @return true
	 */
	public boolean shouldMinimizeOnBack() {
		return true;
	}

	public void onSessionStarting() {}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
    }
}
