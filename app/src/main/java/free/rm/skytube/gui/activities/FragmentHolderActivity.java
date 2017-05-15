/*
 * SkyTube
 * Copyright (C) 2017  Ramon Mifsud
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
import android.os.Bundle;

import free.rm.skytube.R;
import free.rm.skytube.gui.businessobjects.BackButtonActivity;

/**
 * An {@link Activity} that contains an instance of
 * {@link free.rm.skytube.gui.fragments.ChannelBrowserFragment}.
 */
public class FragmentHolderActivity extends BackButtonActivity {

	public static final String FRAGMENT_HOLDER_CHANNEL_BROWSER = "FragmentHolderActivity.ChannelBrowser";



	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Bundle bundle = getIntent().getExtras();
		int fragmentResId = 0;

		if (bundle != null) {
			if (bundle.getBoolean(FRAGMENT_HOLDER_CHANNEL_BROWSER)) {
				fragmentResId = R.layout.activity_channel_browser;
			}
//			} else if (bundle.getBoolean(FRAGMENT_HOLDER_YOUTUBE_PLAYER)) {
//				fragmentResId = R.layout.activity_video_player;
//			}
		}

		if (fragmentResId == 0) {
			throw new IllegalStateException("fragmentResId == 0 which means that intent.putExtra(FRAGMENT_HOLDER_...) was not called.");
		}

		setContentView(fragmentResId);
	}

}
