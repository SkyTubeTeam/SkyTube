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

package free.rm.skytube.gui.activities;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;

import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.gui.businessobjects.BackButtonActivity;

/**
 * An {@link Activity} that contains an instance of
 * {@link free.rm.skytube.gui.fragments.YouTubePlayerFragment}.
 */
public class YouTubePlayerActivity extends BackButtonActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_video_player);
	}

	@Override
	protected void onStart() {
		super.onStart();
		String str = SkyTubeApp.getPreferenceManager().getString(getString(R.string.pref_key_screen_orientation), "auto");
		int orientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
		if("landscape".equals(str)) orientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
		if("portrait".equals(str)) orientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT;
		if("sensor".equals(str)) orientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR;
		setRequestedOrientation(orientation);
	}

	@Override
	protected void onStop() {
		super.onStop();
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
	}

}
