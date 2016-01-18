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

package free.rm.skytube.gui.businessobjects;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;

/**
 * An extension of {@link Fragment} in which a fragment instance is retained across Activity
 * re-creation (e.g. after device rotation EditText content is not lost).
 */
public class FragmentEx extends Fragment {

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setRetainInstance(true);
	}


	/**
	 * @return Instance of {@link ActionBar}.
	 */
	protected ActionBar getActionBar() {
		// The Fragment might not always get completely destroyed after Activity.finish(), hence
		// this code might get called after the hosting activity is destroyed.  Therefore we need
		// to handle getActivity() properly.  Refer to:  http://stackoverflow.com/a/21886594/3132935
		Activity activity = getActivity();
		return (activity != null ? activity.getActionBar() : null);
	}

}
