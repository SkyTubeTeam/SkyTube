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

package free.rm.skytube.gui.businessobjects;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

/**
 * Detects user's swipe motions.
 */
public abstract class OnSwipeTouchListener implements View.OnTouchListener {

	private final GestureDetector gestureDetector;


	protected OnSwipeTouchListener(Context context){
		gestureDetector = new GestureDetector(context, new GestureListener());
	}


	@Override
	public boolean onTouch(View v, MotionEvent event) {
		return gestureDetector.onTouchEvent(event);
	}


	/**
	 * User swiped to the right.
	 */
	public abstract void onSwipeRight();

	/**
	 * User swiped to the left.
	 */
	public abstract void onSwipeLeft();

	/**
	 * User swiped to the top.
	 */
	public abstract void onSwipeTop();

	/**
	 * User swiped to the bottom.
	 */
	public abstract void onSwipeBottom();



	////////////////////////////////////////////////////////////////////////////////////////////////

	private final class GestureListener extends GestureDetector.SimpleOnGestureListener {

		private static final int SWIPE_THRESHOLD = 100;
		private static final int SWIPE_VELOCITY_THRESHOLD = 100;

		@Override
		public boolean onDown(MotionEvent e) {
			return false;   // do not process/handle taps/clicks...
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
			boolean eventConsumed = false;  // set to true if the event is consumed

			try {
				float diffY = e2.getY() - e1.getY();
				float diffX = e2.getX() - e1.getX();

				if (Math.abs(diffX) > Math.abs(diffY)) {
					if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
						if (diffX > 0) {
							onSwipeRight();
						} else {
							onSwipeLeft();
						}
						eventConsumed = true;
					}
				} else if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
					if (diffY > 0) {
						onSwipeBottom();
					} else {
						onSwipeTop();
					}
					eventConsumed = true;
				}
			} catch (Exception exception) {
				Logger.e(this, "onFling() exception caught", exception);
			}

			return eventConsumed;
		}
	}

}