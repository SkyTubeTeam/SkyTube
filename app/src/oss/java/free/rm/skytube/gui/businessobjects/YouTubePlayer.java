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
import android.content.Intent;
import android.net.Uri;

import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubePlaylist;
import free.rm.skytube.businessobjects.YouTube.newpipe.ContentId;
import free.rm.skytube.gui.activities.MainActivity;
import free.rm.skytube.gui.activities.YouTubePlayerActivity;
import free.rm.skytube.gui.fragments.ChannelBrowserFragment;
import free.rm.skytube.gui.fragments.PlaylistVideosFragment;

import static free.rm.skytube.gui.activities.YouTubePlayerActivity.YOUTUBE_VIDEO_OBJ;

/**
 * Launches YouTube player.
 */
public class YouTubePlayer {

	/**
	 * Launches the custom-made YouTube player so that the user can view the selected video.
	 *
	 * @param youTubeVideo Video to be viewed.
	 */
	public static void launch(YouTubeVideo youTubeVideo, Context context) {
		Intent i = new Intent(context, YouTubePlayerActivity.class);
		i.putExtra(YOUTUBE_VIDEO_OBJ, youTubeVideo);
		context.startActivity(i);
	}


	/**
	 * Launches the custom-made YouTube player so that the user can view the selected video.
	 *
	 * @param videoId ContentId of the video to be watched.
	 */
	public static void launch(ContentId videoId, Context context) {
		Intent i = new Intent(context, YouTubePlayerActivity.class);
		i.setAction(Intent.ACTION_VIEW);
		i.setData(Uri.parse(videoId.getCanonicalUrl()));
		context.startActivity(i);
	}

	/**
	 * Launches the channel view, so the user can see all the videos from a channel.
	 *
	 * @param youTubeChannel the channel to be displayed.
	 */
	public static void launchChannel(YouTubeChannel youTubeChannel, Context context) {
		Intent i = new Intent(context, MainActivity.class);
		i.setAction(MainActivity.ACTION_VIEW_CHANNEL);
		i.putExtra(ChannelBrowserFragment.CHANNEL_OBJ, youTubeChannel);
		context.startActivity(i);
	}

	/**
	 * Launch the {@link PlaylistVideosFragment}
	 * @param playlist the playlist to display
	 */
	public static void launchPlaylist(final YouTubePlaylist playlist, final Context context) {
		Intent playlistIntent = new Intent(context, MainActivity.class);
		playlistIntent.setAction(MainActivity.ACTION_VIEW_PLAYLIST);
		playlistIntent.putExtra(PlaylistVideosFragment.PLAYLIST_OBJ, playlist);
		context.startActivity(playlistIntent);
	}

	/**
	 * Launch an external activity to actually open the given URL
	 * @param url
	 */
	public static void viewInBrowser(String url, final Context context) {
		Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
		browserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(browserIntent);
	}


}
