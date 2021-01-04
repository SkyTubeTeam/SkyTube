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

import androidx.viewpager.widget.ViewPager;

/**
 * A fragment that will act as a tab.
 *
 * <p>It will be placed inside a {@link ViewPager}.</p>
 */
public abstract class TabFragment extends FragmentEx {

	/** True indicates that this fragment is selected and can be used by the user;  false means that
	 *  this fragment is hidden and cannot be used. */
	private boolean         isFragmentSelected = false;


	/**
	 * @return The fragment/tab name/title.
	 */
	public abstract String getFragmentName();


	/**
	 * Will be called when the user selects this fragment/tab.  This super method should ALWAYS be
	 * called when inherited.
	 */
	public void onFragmentSelected() {
		isFragmentSelected = true;
	}


	/**
	 * Will be called when the user selects another fragment and hence this fragment is no longer
	 * selected.
	 */
	public void onFragmentUnselected() {
		isFragmentSelected = false;
	}


	protected boolean isFragmentSelected() {
		return isFragmentSelected;
	}

}
