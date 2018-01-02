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

import free.rm.skytube.businessobjects.Logger;

/**
 * Detects user's swipe motions and taps.
 */
public abstract class OnSwipeTouchListener implements View.OnTouchListener {

	private final GestureDetector gestureDetector;


	protected OnSwipeTouchListener(Context context){
		gestureDetector = new GestureDetector(context, new GestureListener());
	}


	@Override
	public boolean onTouch(View v, MotionEvent event) {
		gestureDetector.onTouchEvent(event);
		return true;
	}


	/**
	 * User swiped to the right.
	 *
	 * @return True if the event was consumed.
	 */
	public boolean onSwipeRight() {
		return false;
	}


	/**
	 * User swiped to the left.
	 *
	 * @return True if the event was consumed.
	 */
	public boolean onSwipeLeft() {
		return false;
	}


	/**
	 * User swiped to the top.
	 *
	 * @return True if the event was consumed.
	 */
	public boolean onSwipeTop() {
		return false;
	}


	/**
	 * User swiped to the bottom.
	 *
	 * @return True if the event was consumed.
	 */
	public boolean onSwipeBottom() {
		return false;
	}


	/**
	 * User double tapped.
	 *
	 * @return True if the event was consumed.
	 */
	public boolean onDoubleTap() {
		return false;
	}


	/**
	 * User single tapped.
	 *
	 * @return True if the event was consumed.
	 */
	public boolean onSingleTap() {
		return false;
	}



	////////////////////////////////////////////////////////////////////////////////////////////////

	private final class GestureListener extends GestureDetector.SimpleOnGestureListener {

		private static final int SWIPE_THRESHOLD = 100;
		private static final int SWIPE_VELOCITY_THRESHOLD = 100;


		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
			try {
				float diffY = e2.getY() - e1.getY();
				float diffX = e2.getX() - e1.getX();

				if (Math.abs(diffX) > Math.abs(diffY)) {
					if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
						if (diffX > 0) {
							return onSwipeRight();
						} else {
							return onSwipeLeft();
						}
					}
				} else if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
					if (diffY > 0) {
						return onSwipeBottom();
					} else {
						return onSwipeTop();
					}
				}
			} catch (Exception exception) {
				Logger.e(this, "onFling() exception caught", exception);
			}

			return false;   // event not consumed
		}


		@Override
		public boolean onDoubleTap(MotionEvent e) {
			return OnSwipeTouchListener.this.onDoubleTap();
		}


		@Override
		public boolean onSingleTapConfirmed(MotionEvent e) {
			return onSingleTap();
		}
	}

}