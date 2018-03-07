package free.rm.skytube.businessobjects.db.Tasks;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;

import java.io.IOException;
import java.util.List;

import free.rm.skytube.businessobjects.Logger;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.db.SubscriptionsDb;
import free.rm.skytube.gui.businessobjects.adapters.SubsAdapter;

/**
 * An Asynctask class that unsubscribes user from all the channels at once.
 */
public class UnsubscribeFromAllChannelsTask extends AsyncTask<YouTubeChannel, Void, Void> {

    Handler handler = new Handler();

    private Context context;

    SubsAdapter subsAdapter = SubsAdapter.get(context);

    public UnsubscribeFromAllChannelsTask(Context context) {
        this.context = context;
    }
    @Override
    protected Void doInBackground(YouTubeChannel... youTubeChannels) {
        try {
            List<YouTubeChannel> channelList = SubscriptionsDb.getSubscriptionsDb().getSubscribedChannels();
            for (final YouTubeChannel youTubeChannel : channelList) {
                SubscriptionsDb.getSubscriptionsDb().unsubscribe(youTubeChannel);

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        subsAdapter.removeChannel(youTubeChannel);
                    }
                });
            }
        } catch (IOException e) {
            Logger.e(this,"Error while unsubscribing from all channels" + e);
        }
        return null;
    }
}
