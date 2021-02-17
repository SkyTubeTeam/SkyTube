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

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;

import free.rm.skytube.R;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.YouTube.YouTubeTasks;
import free.rm.skytube.businessobjects.db.DatabaseTasks;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

/**
 * The (channel) subscribe button.
 */
@RemoteViews.RemoteView
public class SubscribeButton extends AppCompatButton implements View.OnClickListener {

	/** Is user subscribed to a channel? */
	private boolean isUserSubscribed = false;

	private YouTubeChannel channel;
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
		if(channel != null) {
			// Only fetch videos for this channel if fetchChannelVideosOnSubscribe is true AND the channel is not subscribed to yet.
			if (!isUserSubscribed) {
				compositeDisposable.add(YouTubeTasks.refreshSubscribedChannel(channel.getId(), null).subscribe());
			}
			compositeDisposable.add(DatabaseTasks.subscribeToChannel(!isUserSubscribed,
					this, getContext(), channel, true).subscribe());
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

	public void setChannel(YouTubeChannel channel) {
		this.channel = channel;
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
