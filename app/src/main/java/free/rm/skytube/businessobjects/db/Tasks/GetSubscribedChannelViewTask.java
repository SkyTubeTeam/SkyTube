/*
 * SkyTube
 * Copyright (C) 2020  Zsombor Gegesy
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

import android.view.View;

import androidx.core.util.Consumer;

import java.util.List;

import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.AsyncTaskParallel;
import free.rm.skytube.businessobjects.YouTube.POJOs.ChannelView;
import free.rm.skytube.businessobjects.YouTube.VideoBlocker;
import free.rm.skytube.businessobjects.db.SubscriptionsDb;

public class GetSubscribedChannelViewTask extends AsyncTaskParallel<Void, Void, List<ChannelView>> {

    private final String searchText; //when it is null user is just opening subscription drawer - when it is not user is searching for subs
    /** Set to true if the user wants channels to be sorted alphabetically (as set in the
     * preference). */
    private final boolean sortChannelsAlphabetically;

    private final View progressBar;
    private final Consumer<List<ChannelView>> callback;

    public GetSubscribedChannelViewTask(String searchText, View progressBar, Consumer<List<ChannelView>> callback) {
        this.searchText = searchText;
        this.progressBar = progressBar;
        this.callback = callback;
        this.sortChannelsAlphabetically = SkyTubeApp.getPreferenceManager().getBoolean(SkyTubeApp.getStr(R.string.pref_key_subscriptions_alphabetical_order), false);
    }

    @Override
    protected void onPreExecute() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected List<ChannelView> doInBackground(Void... params) {

        List<ChannelView>  channelViews = SubscriptionsDb.getSubscriptionsDb().getSubscribedChannelsByText(searchText, sortChannelsAlphabetically);

        // filter out for any whitelisted/blacklisted channels
        return new VideoBlocker().filterChannels(channelViews);
    }

    @Override
    protected void onPostExecute(List<ChannelView> subbedChannelsList) {
        if (progressBar != null) {
            progressBar.setVisibility(View.INVISIBLE);
        }

        callback.accept(subbedChannelsList);
    }

}
