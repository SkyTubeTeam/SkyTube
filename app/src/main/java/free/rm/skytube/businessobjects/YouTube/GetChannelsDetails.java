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

package free.rm.skytube.businessobjects.YouTube;

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.ChannelListResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.Logger;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeAPI;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeAPIKey;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;

/**
 * Returns the details/information about YouTube channel(s).
 */
public class GetChannelsDetails {

	private static final int    CHANNELS_PER_QUERY = 50;
	private static final Long   MAX_RESULTS = (long) CHANNELS_PER_QUERY;


	/**
	 * Retrieve a list of {@link YouTubeChannel} by communicating with YouTube.
	 *
	 * This should not be called from the main thread.
	 *
	 * @param channelIdsList	List of channel IDs
	 * @param isUserSubscribed	if set to true, then it means the user is subscribed to this channel;
	 *                          otherwise it means that we currently do not know if the user is
	 *                          subbed or not (hence we need to check).
	 *
	 * @return List of {@link YouTubeChannel}.
	 * @throws IOException
	 */
	public List<YouTubeChannel> getYouTubeChannels(List<String> channelIdsList, boolean isUserSubscribed, boolean shouldCheckForNewVideos) throws IOException {
		String  bannerType = SkyTubeApp.isTablet() ? "bannerTabletHdImageUrl" : "bannerMobileHdImageUrl";

		List<YouTubeChannel> youTubeChannelsList = new ArrayList<>();

		// YouTube can only return information about 50 (or so) channels at a time.  Hence we need
		// to divide the given channelIdsList into smaller lists... then we need to regroup them
		// into youTubeChannelsList.
		List<List<String>> dividedChannelIdsLists = divideList(channelIdsList);
		for (List<String> subChannelIdsList : dividedChannelIdsLists) {
			YouTube youtube = YouTubeAPI.create();
			YouTube.Channels.List channelInfo = youtube.channels().list("snippet, statistics, brandingSettings");
			channelInfo.setFields("items(id, snippet/title, snippet/description, snippet/thumbnails/default," +
								"statistics/subscriberCount, brandingSettings/image/" + bannerType + ")," +
								"nextPageToken")
					.setKey(YouTubeAPIKey.get().getYouTubeAPIKey())
					.setId(convertListToCSV(subChannelIdsList))
					.setMaxResults(MAX_RESULTS);

			// get channels' info from the remote YouTube server
			youTubeChannelsList.addAll( getYouTubeChannels(channelInfo, isUserSubscribed, shouldCheckForNewVideos) );
		}

		// There is currently a bug in the YouTube API in the sense that the order of channels is
		// not maintained.  Hence we currently need to manually sort the channels (to maintain the
		// order listed in the DB) until YouTube fix their bug.
		return sortYouTubeChannelsList(channelIdsList, youTubeChannelsList);
	}


	/**
	 * Retrieve a list of {@link YouTubeChannel} by communicating with YouTube.
	 *
	 * This should not be called from the main thread.
	 *
	 * @param channelId     Channel ID.
	 *
	 * @return YouTubeChannel
	 */
	public YouTubeChannel getYouTubeChannel(final String channelId) throws IOException {
		List<String> c = new ArrayList<>();
		c.add(channelId);

		List<YouTubeChannel> channelList = getYouTubeChannels(c, false, true);

		return (channelList != null  &&  channelList.size() > 0)  ?  channelList.get(0)  :  null;
	}

	/**
	 * Return a YouTubeChannel object from the passed username.
	 *
	 * This should not be called from the main thread.
	 *
	 * @param username	The YouTube username
	 *
	 * @return	YouTubeChannel
	 */
	public YouTubeChannel getYouTubeChannelFromUsername(String username) throws IOException {

		String  bannerType = SkyTubeApp.isTablet() ? "bannerTabletHdImageUrl" : "bannerMobileHdImageUrl";

		YouTube youtube = YouTubeAPI.create();
		YouTube.Channels.List channelInfo = youtube.channels().list("snippet, statistics, brandingSettings");

		channelInfo.setForUsername(username);
		channelInfo.setFields("items(id, snippet/title, snippet/description, snippet/thumbnails/default," +
						"statistics/subscriberCount, brandingSettings/image/" + bannerType + ")," +
						"nextPageToken")
						.setKey(YouTubeAPIKey.get().getYouTubeAPIKey());

		ChannelListResponse response = channelInfo.execute();
		List<Channel> channelList = response.getItems();

		if(channelList != null && channelList.size() > 0) {
			YouTubeChannel youTubeChannel = new YouTubeChannel();
			youTubeChannel.init(channelList.get(0), false, false);
			return youTubeChannel;
		}

		return null;
	}


	/**
	 * Divides the given list into a list of lists in which each list is made up of no more than
	 * {@link #CHANNELS_PER_QUERY} elements.
	 *
	 * @param l list to divide.
	 *
	 * @return  A list of lists.
	 */
	private static List<List<String>> divideList(List<String> l) {
		final int elements   = CHANNELS_PER_QUERY;
		final int totalLists = (int) Math.ceil((double)l.size() / (double)elements);
		List<List<String>> lists = new ArrayList<>();
		int toRange;

		for (int i = 0;  i < totalLists;  i++) {
			toRange = (i+1) * elements;

			lists.add(l.subList(i * elements,  toRange < l.size() ? toRange : l.size()));
		}

		return lists;
	}


	/**
	 * Converts a list into a comma-separated string.
	 *
	 * @param list List.
	 *
	 * @return CSV.
	 */
	private String convertListToCSV(List<String> list) {
		StringBuilder str = new StringBuilder();

		if (list.size() > 0) {
			for (int i = 0;  i < list.size() - 1;  i++) {
				str.append(list.get(i));
				str.append(',');
			}

			str.append(list.get(list.size() - 1));
		}

		return str.toString();
	}


	/**
	 * Get the channels info from the remote YouTube server and then return a list of
	 * {@link YouTubeChannel}.
	 *
	 * @param channelInfo
	 * @param isUserSubscribed	        if set to true, then it means the user is subscribed to this
	 *                                  channel;  otherwise it means that we currently do not know
	 *                                  if the user is subbed or not (hence we need to check).
	 * @param shouldCheckForNewVideos   if set to true it will check with the database whether new
	 *                                  videos have been published since last visit.
	 *
	 * @return A list of {@link YouTubeChannel}.
	 */
	private List<YouTubeChannel> getYouTubeChannels(YouTube.Channels.List channelInfo, boolean isUserSubscribed, boolean shouldCheckForNewVideos) {
		List<YouTubeChannel>    youTubeChannelList = new ArrayList<>();

		try {
			// communicate with YouTube
			ChannelListResponse response = channelInfo.execute();

			// get channel
			List<Channel> channelList = response.getItems();

			YouTubeChannel youTubeChannel;

			// set the instance variables
			for (Channel channel : channelList) {
				try {
					youTubeChannel = new YouTubeChannel();
					youTubeChannel.init(channel, isUserSubscribed, shouldCheckForNewVideos);
					youTubeChannelList.add(youTubeChannel);
				} catch (Throwable tr) {
					Logger.e(this, "Error has occurred while getting channel info for ChannelID=" + channel.getId(), tr);
				}
			}
		} catch (Throwable tr) {
			Logger.e(this, "Error has occurred while getting channels info", tr);
		}

		return youTubeChannelList;
	}


	/**
	 * Sort the order of the given youTubeChannelsList such that it is the same as that of channelIdsList.
	 *
	 * @param channelIdsList        Original list of channel IDs -- retrieved from the DB (i.e.
	 *                              correct ordering).
	 * @param youTubeChannelsList   YouTube channels list as given by the YouTube servers.
	 *
	 * @return  Sorted list of YouTube Channels.
	 */
	private List<YouTubeChannel> sortYouTubeChannelsList(List<String> channelIdsList, List<YouTubeChannel> youTubeChannelsList) {
		List<YouTubeChannel> sortedList = new ArrayList<>(youTubeChannelsList.size());
		boolean channelFound = false;

		for (String channelId : channelIdsList) {
			for (YouTubeChannel channel : youTubeChannelsList) {
				if (channel.getId().equals(channelId)) {
					sortedList.add(channel);
					channelFound = true;
					break;
				}
			}

			if (!channelFound) {
				Logger.d(this, "Channel id=%s was not found in the youTubeChannelsList", channelId);
			}

			channelFound = false;
		}

		return sortedList;
	}

}
