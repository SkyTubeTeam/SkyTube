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

package free.rm.skytube.gui.businessobjects;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;
import android.widget.RemoteViews;

import free.rm.skytube.R;

/**
 * The (channel) subscribe button.
 */
@RemoteViews.RemoteView
public class SubscribeButton extends Button {

	/** Is user subscribed to a channel? */
	private boolean isUserSubscribed = false;


	public SubscribeButton(Context context, AttributeSet attrs) {
		super(context, attrs);
	}


	public boolean isUserSubscribed() {
		return isUserSubscribed;
	}


	/**
	 * Set the button's state to subscribe (i.e. once clicked, the user indicates that he wants to
	 * subscribe).
	 */
	public void setSubscribeState() {
		setText(R.string.subscribe);
		isUserSubscribed = false;	// the user is currently NOT subscribed
	}


	/**
	 * Set the button's state to unsubscribe (i.e. once clicked, the user indicates that he wants to
	 * unsubscribe).
	 */
	public void setUnsubscribeState() {
		setText(R.string.unsubscribe);
		isUserSubscribed = true;
	}

}
