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

package free.rm.skytube.gui.businessobjects.fragments;

import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

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


	protected AppCompatActivity getAppCompatActivity() {
		return (AppCompatActivity) getActivity();
	}


	/**
	 * Close the parent activity.
	 */
	protected void closeActivity() {
		AppCompatActivity activity = getAppCompatActivity();

		if (activity != null) {
			activity.finish();
		}
	}


	/**
	 * @return Instance of {@link ActionBar}.
	 */
	protected ActionBar getSupportActionBar() {
		// The Fragment might not always get completely destroyed after Activity.finish(), hence
		// this code might get called after the hosting activity is destroyed.  Therefore we need
		// to handle getActivity() properly.  Refer to:  http://stackoverflow.com/a/21886594/3132935
		AppCompatActivity activity = getAppCompatActivity();
		return (activity != null ? activity.getSupportActionBar() : null);
	}


	/**
	 * Set a {@link Toolbar} to act as the {@link ActionBar}.
	 *
	 * @param toolbar	Toolbar to set as the Activity's action bar, or null to clear it.
	 */
	protected void setSupportActionBar(Toolbar toolbar) {
		AppCompatActivity activity = getAppCompatActivity();

		if (activity != null) {
			activity.setSupportActionBar(toolbar);
		}
	}

}
