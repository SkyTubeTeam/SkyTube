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
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatButton;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RemoteViews;

import free.rm.skytube.R;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.YouTube.Tasks.GetChannelVideosTask;
import free.rm.skytube.businessobjects.db.Tasks.SubscribeToChannelTask;

/**
 * The (channel) subscribe button.
 */
@RemoteViews.RemoteView
public class SubscribeButton extends AppCompatButton implements View.OnClickListener {

	/** Is user subscribed to a channel? */
	private boolean isUserSubscribed = false;

	private YouTubeChannel channel;
	private boolean fetchChannelVideosOnSubscribe = true;
	private OnClickListener externalClickListener = null;


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
		// Only fetch videos for this channel if fetchChannelVideosOnSubscribe is true AND the channel is not subscribed to yet.
		if(fetchChannelVideosOnSubscribe && !isUserSubscribed) {
			new GetChannelVideosTask(channel).executeInParallel();
		}
		if(channel != null)
			new SubscribeToChannelTask(SubscribeButton.this, channel).executeInParallel();
	}

	@Override
	public void setOnClickListener(@Nullable OnClickListener l) {
		externalClickListener = l;
		super.setOnClickListener(this);
	}

	public void setChannel(YouTubeChannel channel) {
		this.channel = channel;
	}

	public void setFetchChannelVideosOnSubscribe(boolean fetchChannelVideosOnSubscribe) {
		this.fetchChannelVideosOnSubscribe = fetchChannelVideosOnSubscribe;
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
