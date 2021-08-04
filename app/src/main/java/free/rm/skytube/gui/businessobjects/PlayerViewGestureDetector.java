/*
 * SkyTube
 * Copyright (C) 2018  Ramon Mifsud
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

import androidx.core.view.GestureDetectorCompat;

import free.rm.skytube.app.Settings;

/**
 * Gesture detector for a PlayerView.  It will detect gestures that may result in showing comments,
 * video description, change in volume or brightness.
 */
public abstract class PlayerViewGestureDetector implements View.OnTouchListener {

	private final GestureDetectorCompat     gestureDetector;
	private final PlayerViewGestureListener playerViewGestureListener;
	private final Settings 					settings;


	public PlayerViewGestureDetector(Context context, Settings settings) {
		this.settings = settings;
		playerViewGestureListener = new PlayerViewGestureListener();
		gestureDetector = new GestureDetectorCompat(context, playerViewGestureListener);
	}


	@Override
	public boolean onTouch(View v, MotionEvent event) {
		gestureDetector.onTouchEvent(event);

		if (event.getAction() == MotionEvent.ACTION_UP) {
			playerViewGestureListener.onSwipeGestureDone();
			onGestureDone();
		}

		return true;
	}


	/**
	 * Called when the user single taps.
	 *
	 * @return  True if the event was consumed; false otherwise.
	 */
	public abstract boolean onSingleTap();


	/**
	 * Called when the user double taps.
	 */
	public abstract void onDoubleTap();


	/**
	 * Called when user wants to view video's comments.
	 */
	public abstract void onCommentsGesture();


	/**
	 * Called when user wants to view video's description.
	 */
	public abstract void onVideoDescriptionGesture();


	/**
	 * Called every time any gesture is ended.
	 */
	public abstract void onGestureDone();


	/**
	 * User swiped from top to bottom or from bottom to top at the left side of the view.
	 */
	public abstract void adjustBrightness(double adjustPercent);


	/**
	 * User swiped from top to bottom or from bottom to top at the left side of the view.
	 */
	public abstract void adjustVolumeLevel(double adjustPercent);


	/**
	 * User swiped from left to right or from right to left at any place of the view except 20% from the right.
	 */
	public abstract void adjustVideoPosition(double adjustPercent, boolean forwardDirection);


	/**
	 * Returns the PlayerView's Rect instance.
	 *
	 * @return PlayerView's rect.
	 */
	public abstract Rect getPlayerViewRect();


	////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Gesture type.
	 */
	private enum SwipeGestureType {
		NONE,
		BRIGHTNESS,
		VOLUME,
		SEEK,
		COMMENTS,
		DESCRIPTION
	}


	/**
	 * Class that listen to events and classifies them accordingly.  Once an event is classified,
	 * it will call the respective (abstract) method.
	 */
	private class PlayerViewGestureListener extends GestureDetector.SimpleOnGestureListener {

		/** The current swipe gesture type being performed by the user (if any). */
		SwipeGestureType currentGestureEvent = SwipeGestureType.NONE;

		private static final int SWIPE_THRESHOLD = 50;


		@Override
		public boolean onSingleTapConfirmed(MotionEvent e) {
			return onSingleTap();
		}


		@Override
		public boolean onDoubleTap(MotionEvent e) {
			PlayerViewGestureDetector.this.onDoubleTap();
			return false;
		}


		@Override
		public boolean onScroll(MotionEvent startEvent, MotionEvent endEvent, float distanceX, float distanceY) {
			// detect swipe event type
			currentGestureEvent = getSwipeGestureType(startEvent, endEvent);

			if (currentGestureEvent != SwipeGestureType.NONE) {
				double  yDistance = endEvent.getY() - startEvent.getY();
				double  xDistance = endEvent.getX() - startEvent.getX();
				Rect    playerViewRect = getPlayerViewRect();

				if (currentGestureEvent == SwipeGestureType.COMMENTS) {
					onCommentsGesture();
				} else if (currentGestureEvent == SwipeGestureType.DESCRIPTION) {
					onVideoDescriptionGesture();
				} else if (currentGestureEvent == SwipeGestureType.BRIGHTNESS) {
					// use half of BrightnessRect's height to calculate percent.
					double percent = yDistance / (getBrightnessRect(playerViewRect).height() / 2f) * -1f;
					adjustBrightness(percent);
				} else if (currentGestureEvent == SwipeGestureType.VOLUME) {
					// use half of volumeRect's height to calculate percent.
					double percent = yDistance / (getVolumeRect(playerViewRect).height() / 2f) * -1f;
					adjustVolumeLevel(percent);
				} else if (currentGestureEvent == SwipeGestureType.SEEK) {
					double percent = xDistance / getPlayerViewRect().width();
					adjustVideoPosition(percent, distanceX < 0);
				}
			}

			return false;   // event not consumed -- the event might need to be consumed by the Video Player
		}


		/**
		 * To be called when the user is done swiping.
		 */
		void onSwipeGestureDone() {
			currentGestureEvent = SwipeGestureType.NONE;
		}


		/**
		 * Detect swipe gesture type.
		 *
		 * @param startEvent    The start event.
		 * @param currentEvent  The current event.
		 *
		 * @return The detected {@link SwipeGestureType}.
		 */
		private SwipeGestureType getSwipeGestureType(MotionEvent startEvent, MotionEvent currentEvent) {
			if (currentGestureEvent != SwipeGestureType.NONE) {
				return currentGestureEvent;
			}

			final float startX = startEvent.getX();
			final float startY = startEvent.getY();
			final float currentX = currentEvent.getX();
			final float currentY = currentEvent.getY();
			final float diffY = startY - currentY;
			final float diffX = startX - currentX;
			final Rect  playerViewRect = getPlayerViewRect();

			if (Math.abs(currentX - startX) >= SWIPE_THRESHOLD && Math.abs(currentX - startX) > Math.abs(currentY - startY)) {
				if (getCommentsRect(playerViewRect).contains((int) startX, (int) startY) && diffX > 0) {
					return SwipeGestureType.COMMENTS;
				} else {
					return SwipeGestureType.SEEK;
				}
			} else if (Math.abs(currentY - startY) >= SWIPE_THRESHOLD) {
				if (getDescriptionRect(playerViewRect).contains((int) startX, (int) startY) && diffY > 0) {
					return SwipeGestureType.DESCRIPTION;
				} else if (getBrightnessRect(playerViewRect).contains((int) startX, (int) startY)) {
					return settings.isSwitchVolumeAndBrightness() ? SwipeGestureType.VOLUME : SwipeGestureType.BRIGHTNESS;
				} else if (getVolumeRect(playerViewRect).contains((int) startX, (int) startY)) {
					return settings.isSwitchVolumeAndBrightness() ? SwipeGestureType.BRIGHTNESS : SwipeGestureType.VOLUME;
				}
			}

			return SwipeGestureType.NONE;
		}


		/**
		 * Here we decide in what place of the screen user should swipe to get a new brightness value.
		 */
		private Rect getBrightnessRect(final Rect playerViewRect) {
			return new Rect(playerViewRect.right / 2, (int)(playerViewRect.bottom * 0.2),   // top (X, Y) coordinates
					playerViewRect.right, playerViewRect.bottom);                                // bottom (X, Y) coordinates
		}


		/**
		 * Here we decide in what place of the screen user should swipe to get a new volume value.
		 */
		private Rect getVolumeRect(final Rect playerViewRect) {
			return new Rect(0, (int)(playerViewRect.bottom * 0.2),
					playerViewRect.right / 2, playerViewRect.bottom);
		}


		/**
		 * Here we choose a rect for swipe which then will be used to open the comments view.
		 */
		private Rect getCommentsRect(final Rect playerViewRect) {
			// 20% from right side will trigger comments view
			return new Rect((int) (playerViewRect.right - (playerViewRect.right * 0.2)), 0,
					playerViewRect.right, playerViewRect.bottom);
		}

		/**
		 * Here we choose a rect for swipe which then will be used to open the description view.
		 */
		private Rect getDescriptionRect(final Rect playerViewRect) {
			// 20% from bottom side will trigger description view
			return new Rect(0, (int) (playerViewRect.bottom - (playerViewRect.bottom * 0.2)),
					playerViewRect.right, playerViewRect.bottom);
		}

	}

}
