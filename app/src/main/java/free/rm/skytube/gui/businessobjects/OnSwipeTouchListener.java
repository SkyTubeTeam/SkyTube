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
import android.graphics.Rect;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

/**
 * Detects user's swipe motions and taps.
 */
public abstract class OnSwipeTouchListener implements View.OnTouchListener {

	private final GestureDetector gestureDetector;
	private final GestureListener gestureListener;


	protected OnSwipeTouchListener(Context context){
		gestureListener = new GestureListener();
		gestureDetector = new GestureDetector(context, gestureListener);
	}


	@Override
	public boolean onTouch(View v, MotionEvent event) {
		gestureListener.onTouchEvent(event);
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

	public void onSeekStart() {

	}

    public void onSeekEnd() {

    }
	public void onGestureDone(boolean notStart) {

	}

	public void adjustBrightness(double adjustPercent) {

	}

	public void adjustVolumeLevel(double adjustPercent) {

	}

	public void adjustVideoPosition(double adjustPercent, boolean forwardDirection) {

	}

	public Rect viewRect() {
		// We need to get actual rect of view from top class
		return new Rect();
	}

	private enum Type {
		NONE,
		BRIGHTNESS,
		VOLUME,
		SEEK,
		COMMENTS,
		DESCRIPTION
	}

	////////////////////////////////////////////////////////////////////////////////////////////////

	private final class GestureListener extends GestureDetector.SimpleOnGestureListener {

		private static final int SWIPE_THRESHOLD = 50;
		private Type type = Type.NONE;
		private MotionEvent startEvent;

		@Override
		public boolean onDoubleTap(MotionEvent e) {
			return OnSwipeTouchListener.this.onDoubleTap();
		}


		@Override
		public boolean onSingleTapConfirmed(MotionEvent e) {
			return onSingleTap();
		}

		@Override
		public boolean onScroll(MotionEvent start, MotionEvent end,
								float distanceX, float distanceY) {
			// function depends on start position. (volume or brightness or play position.)
			double yDistance = end.getY() - start.getY();
			double xDistance = end.getX() - start.getX();

			if (type == Type.COMMENTS) {
				onSwipeLeft();
			} else if (type == Type.DESCRIPTION) {
				onSwipeTop();
			}
			if (type == Type.BRIGHTNESS) {
				// use half of BrightnessRect's height to calculate percent.
				double percent = yDistance / (getBrightnessRect().height() / 2.0f) * -1.0f;
				adjustBrightness(percent);
			} else if (type == Type.VOLUME) {
				// use half of volumeRect's height to calculate percent.
				double percent = yDistance / (getVolumeRect().height() / 2.0f) * -1.0f;
				adjustVolumeLevel(percent);
			} else if (type == Type.SEEK) {
				double percent = xDistance / viewRect().width();
				adjustVideoPosition(percent, distanceX < 0);
			}
			return true;
		}

		private boolean isEventValid(MotionEvent event) {
			// this gesture just worked on single pointer.
			return (gestureDetector != null && event.getPointerCount() == 1);
		}

		private void onTouchEvent(MotionEvent event) {
			if (!isEventValid(event)) {
				gestureDetector.onTouchEvent(event);
				return;
			}
			// decide which process is needed.
			switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					startEvent = MotionEvent.obtain(event);
					break;
				case MotionEvent.ACTION_MOVE:
					if (type == Type.NONE && startEvent != null) {
						type = whatTypeIsIt(startEvent, event);

						if (type == Type.SEEK)
							onSeekStart();
					}
					break;
				case MotionEvent.ACTION_UP:
					if (type == Type.NONE) {
						// It happens when user clicks inside the view
						onGestureDone(true);
					} else {
					    if(type == Type.SEEK)
                            onSeekEnd();

						onGestureDone(false);
					}
					type = Type.NONE;
					startEvent = null;
					break;
				default:
					break;
			}

			gestureDetector.onTouchEvent(event);
		}

		private Rect getBrightnessRect() {
			return new Rect(0, 0, viewRect().right / 2, viewRect().bottom);
		}

		private Rect getVolumeRect() {
			return new Rect(viewRect().right / 2, 0, viewRect().right, viewRect().bottom);
		}

		private Rect getCommentsRect() {
			// 20% from right side will trigger comments view
			return new Rect((int)(viewRect().right - Math.min(viewRect().bottom, viewRect().right) * 0.2), 0, viewRect().right, viewRect().bottom);
		}

		private Rect getDescriptionRect() {
			// 30% from bottom side will trigger description view
			return new Rect(0, (int)(viewRect().bottom - Math.min(viewRect().bottom, viewRect().right) * 0.3), viewRect().right, viewRect().bottom);
		}

		private Type whatTypeIsIt(MotionEvent startEvent, MotionEvent currentEvent) {
			float startX = startEvent.getX();
			float startY = startEvent.getY();
			float currentX = currentEvent.getX();
			float currentY = currentEvent.getY();
			float diffY = startY - currentY;
			float diffX = startX - currentX;
			if (Math.abs(currentX - startX) >= SWIPE_THRESHOLD && Math.abs(currentX - startX) > Math.abs(currentY - startY)) {
				if (getCommentsRect().contains((int) startX, (int) startY) && diffX > 0)
					return Type.COMMENTS;

				return Type.SEEK;
			} else if (Math.abs(currentY - startY) >= SWIPE_THRESHOLD) {
				if (getDescriptionRect().contains((int) startX, (int) startY) && diffY > 0) {
					return Type.DESCRIPTION;
				} else if (getBrightnessRect().contains((int) startX, (int) startY)) {
					return Type.BRIGHTNESS;
				} else if (getVolumeRect().contains((int) startX, (int) startY)) {
					return Type.VOLUME;
				}
			}
			return Type.NONE;
		}
	}

}