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

package free.rm.skytube.gui.businessobjects.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;

import free.rm.skytube.R;
import free.rm.skytube.businessobjects.YouTube.POJOs.PersistentChannel;
import free.rm.skytube.businessobjects.YouTube.newpipe.ChannelId;
import free.rm.skytube.businessobjects.db.DatabaseTasks;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

/**
 * The (channel) subscribe button.
 */
@RemoteViews.RemoteView
public class SubscribeButton extends MaterialButton implements View.OnClickListener, ChannelSubscriber {

	/** Is user subscribed to a channel? */
	private boolean isUserSubscribed = false;

	private ChannelId channelId;
	private OnClickListener externalClickListener = null;

	private final CompositeDisposable compositeDisposable = new CompositeDisposable();

	public SubscribeButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		super.setOnClickListener(this);
	}

	@Override
	public void onClick(View view) {
		// Need to handle externalClickListener first, in case this Button is used by the ChannelBrowserFragment,
		// which will save the channel's videos to the channel object from the video grid. The channels will then be saved
		// by the SubscribeToChannelTask.
		if(externalClickListener != null) {
			externalClickListener.onClick(SubscribeButton.this);
		}
		if(channelId != null) {
			compositeDisposable.add(DatabaseTasks.subscribeToChannel(!isUserSubscribed,
					this, getContext(), channelId, true).subscribe());
		}
	}

	@Override
	public void setOnClickListener(@Nullable OnClickListener l) {
		externalClickListener = l;
		super.setOnClickListener(this);
	}

	public void clearBackgroundTasks() {
		compositeDisposable.clear();
	}

	public void setChannelInfo(@NonNull PersistentChannel persistentChannel) {
		this.channelId = persistentChannel.getChannelId();
		setSubscribedState(persistentChannel.isSubscribed());
	}

	/**
	 * Set the button's state to subscribe or unsubscribe (i.e. once clicked, the user indicates that he wants to
	 * unsubscribe).
	 */
	@Override
	public void setSubscribedState(boolean subscribed) {
		isUserSubscribed = subscribed;
		if (subscribed) {
			// the user is subscribed currently
			setText(R.string.unsubscribe);
		} else {
			// the user is currently NOT subscribed
			setText(R.string.subscribe);
		}
	}
}
