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

import android.content.res.Resources;
import android.os.Build;
import android.view.View;

import androidx.fragment.app.FragmentActivity;

import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.Logger;

/**
 * A fragment that can enables and disables the immersive mode (i.e. hides the navigation bar and
 * the status bar).
 */
public class ImmersiveModeFragment extends FragmentEx {

	/**
	 * Hide Android's bottom navigation bar.
	 */
	protected void hideNavigationBar() {
		if (isImmersiveModeEnabled()) {
			changeNavigationBarVisibility(false);
		}
	}

	protected void showNavigationBar() {
		changeNavigationBarVisibility(true);
	}


	/**
	 * Change the navigation bar's visibility status.
	 */
	private void changeNavigationBarVisibility(boolean setBarToVisible) {
		try {
			FragmentActivity activity = getActivity();
			if (activity == null) {
				Logger.e(this, "Activity is not available!");
				return;
			}
			final View decorView = activity.getWindow().getDecorView();
			int newUiOptions = decorView.getSystemUiVisibility();

			// navigation bar hiding:  backwards compatible to ICS.
			if (Build.VERSION.SDK_INT >= 14) {
				newUiOptions = setBarToVisible
						? newUiOptions & ~View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
						: newUiOptions | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
			}

			// status bar hiding:  backwards compatible to Jellybean
			if (Build.VERSION.SDK_INT >= 16) {
				newUiOptions = setBarToVisible
						? newUiOptions & ~View.SYSTEM_UI_FLAG_FULLSCREEN & ~View.SYSTEM_UI_FLAG_LAYOUT_STABLE & ~View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION & ~View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
						: newUiOptions | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
			}

			// immersive mode:  backward compatible to KitKat
			if (Build.VERSION.SDK_INT >= 19) {
				newUiOptions = setBarToVisible
						? newUiOptions & ~View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
						: newUiOptions | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
			}

			decorView.setSystemUiVisibility(newUiOptions);
		} catch (Throwable tr) {
			Logger.e(this, "Exception caught while trying to change the nav bar visibility...", tr);
		}
	}

	private boolean isImmersiveModeEnabled() {
		return SkyTubeApp.getPreferenceManager().getBoolean(SkyTubeApp.getStr(R.string.pref_key_enable_immersive_mode), true);
	}

	/**
	 * @return The navigation bar's height in pixels.  0 is the device does not have any nav bar.
	 */
	protected int getNavBarHeightInPixels() {
		Resources   resources  = getResources();
		int         resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");

		return (resourceId > 0)  ?  resources.getDimensionPixelSize(resourceId)  :  0;
	}

}