/*
 * SkyTube
 * Copyright (C) 2023  Zsombor Gegesy
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

import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class ChannelActionHandler {
    private CompositeDisposable compositeDisposable;

    public ChannelActionHandler(CompositeDisposable compositeDisposable) {
        this.compositeDisposable = compositeDisposable;
    }

    public boolean handleChannelActions(Context context, YouTubeChannel channel, int itemId) {
        switch (itemId) {
            case R.id.subscribe_channel:
                compositeDisposable.add(YouTubeChannel.subscribeChannel(context, channel.getId()));
                return true;
            case R.id.open_channel:
                SkyTubeApp.launchChannel(channel.getId(), context);
                return true;
            case R.id.block_channel:
                compositeDisposable.add(channel.blockChannel().subscribe());
                return true;
            case R.id.unblock_channel:
                compositeDisposable.add(channel.unblockChannel().subscribe());
                return true;
        }
        return false;
    }
}
