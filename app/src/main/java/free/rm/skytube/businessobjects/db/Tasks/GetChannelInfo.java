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
import free.rm.skytube.businessobjects.AsyncTaskParallel;
import free.rm.skytube.businessobjects.Logger;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannelInterface;
import free.rm.skytube.businessobjects.YouTube.VideoStream.NewPipeService;
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

    public GetChannelInfo(Context context, YouTubeChannelInterface channelReceiver) {
        NewPipeService.requireNonNull(context, "context missing");
        NewPipeService.requireNonNull(channelReceiver, "channelReceiver missing");
        this.context = context;
        this.channelReceiver = channelReceiver;
    }

    @Override
    protected YouTubeChannel doInBackground(String... params) {
        String channelId = params[0];
        final SubscriptionsDb db = SubscriptionsDb.getSubscriptionsDb();
        YouTubeChannel channel = db.getCachedChannel(channelId);
        if (channel == null || channel.getLastCheckTime() < System.currentTimeMillis() - CHANNEL_INFO_VALIDITY) {
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
