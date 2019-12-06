/*
 * SkyTube
 * Copyright (C) 2019  Zsombor Gegesy
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
package free.rm.skytube.businessobjects.db.Tasks;

import android.content.Context;
import android.widget.Toast;

import org.schabi.newpipe.extractor.exceptions.ExtractionException;

import java.io.IOException;

import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.AsyncTaskParallel;
import free.rm.skytube.businessobjects.Logger;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannelInterface;
import free.rm.skytube.businessobjects.YouTube.newpipe.NewPipeService;
import free.rm.skytube.businessobjects.db.SubscriptionsDb;

/**
 * Task to retrieve channel information - first from the local cache, and if the value is old, or not exists,
 * ask the remote service.
 */
public class GetChannelInfo extends AsyncTaskParallel<String, Void, YouTubeChannel> {

    private final long CHANNEL_INFO_VALIDITY = 24 * 60 * 60 * 1000L;
    private final Context context;
    private Exception exception;
    private final YouTubeChannelInterface channelReceiver;
    private final boolean staleAcceptable;

    public GetChannelInfo(Context context, YouTubeChannelInterface channelReceiver) {
        this(context, channelReceiver, false);
    }
    public GetChannelInfo(Context context, YouTubeChannelInterface channelReceiver, boolean staleAcceptable) {
        NewPipeService.requireNonNull(context, "context missing");
        NewPipeService.requireNonNull(channelReceiver, "channelReceiver missing");
        this.context = context;
        this.channelReceiver = channelReceiver;
        this.staleAcceptable = staleAcceptable;
    }

    @Override
    protected YouTubeChannel doInBackground(String... params) {
        String channelId = params[0];
        return getChannelInfoSync(channelId);
    }

    YouTubeChannel getChannelInfoSync(String channelId) {
        final SubscriptionsDb db = SubscriptionsDb.getSubscriptionsDb();
        YouTubeChannel channel = db.getCachedChannel(channelId);
        if (needRefresh(channel) && SkyTubeApp.isConnected(context)) {
            try {
                channel = NewPipeService.get().getChannelDetails(channelId);
                db.cacheChannel(channel);
            } catch (ExtractionException | IOException e) {
                exception = e;
            }
        }
        if (channel != null) {
            channel.setUserSubscribed(db.isUserSubscribedToChannel(channelId));
        }
        return channel;
    }

    private boolean needRefresh(YouTubeChannel channel) {
        if (channel == null) {
            return true;
        }
        if (staleAcceptable) {
            return false;
        }
        return (channel.getLastCheckTime() < System.currentTimeMillis() - CHANNEL_INFO_VALIDITY);
    }

    @Override
    protected void onPostExecute(YouTubeChannel youTubeChannel) {
        if (exception != null) {
            Logger.e(this, "Error: "+exception.getMessage(), exception);
            showError(exception.getMessage());
        }
        channelReceiver.onGetYouTubeChannel(youTubeChannel);
    }

    protected void showError(String msg) {
        if (msg != null){
            Toast.makeText(context,
                    context.getString(R.string.could_not_get_channel_detailed, msg),
                    Toast.LENGTH_LONG).show();

        } else {
            Toast.makeText(context,
                    context.getString(R.string.could_not_get_channel),
                    Toast.LENGTH_LONG).show();
        }
    }

}
