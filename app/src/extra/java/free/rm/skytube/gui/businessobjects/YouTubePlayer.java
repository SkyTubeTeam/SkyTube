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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import free.rm.skytube.BuildConfig;
import free.rm.skytube.R;
import free.rm.skytube.businessobjects.YouTubeVideo;
import free.rm.skytube.gui.activities.YouTubePlayerActivity;

/**
 * Launches YouTube player.
 */
public class YouTubePlayer {

	private static final String TAG = YouTubePlayer.class.getSimpleName();

	/**
	 * Launches the YouTube player so that the user can view the selected video.
	 *
	 * @param youTubeVideo Video to be viewed.
	 */
	public static void launch(YouTubeVideo youTubeVideo, Context context) {
		if (youTubeVideo != null) {
			// if the user has selected to play the videos using the official YouTube player
			// (in the preferences/settings) ...
			if (useOfficialYouTubePlayer(context)) {
				launchOfficialYouTubePlayer(youTubeVideo.getId(), context);
			} else {
				launchCustomYouTubePlayer(youTubeVideo, context);
			}
		}
	}


	/**
	 * Read the user's preferences and determine if the user wants to use the official YouTube video
	 * player or not.
	 *
	 * @return True if the user wants to use the official player; false otherwise.
	 */
	private static boolean useOfficialYouTubePlayer(Context context) {
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
		return sharedPref.getBoolean(context.getString(R.string.pref_key_use_offical_player), false);
	}


	/**
	 * Launch the official (i.e. non-free) YouTube player.
	 */
	private static void launchOfficialYouTubePlayer(String videoId, Context context) {
		try {
			// try to start the YouTube standalone player
			Intent intent = com.google.android.youtube.player.YouTubeStandalonePlayer.createVideoIntent((Activity) context, BuildConfig.YOUTUBE_API_KEY, videoId);
			context.startActivity(intent);
		} catch (Exception e) {
			String errorMsg = context.getString(R.string.launch_offical_player_error);

			// log the error
			Log.e(TAG, errorMsg, e);

			// display the error in an AlertDialog
			new AlertDialog.Builder(context)
					.setTitle(R.string.error)
					.setMessage(errorMsg)
					.setIcon(android.R.drawable.ic_dialog_alert)
					.setNeutralButton(android.R.string.ok, null)
					.show();
		}
	}


	/**
	 * Launch the custom-made YouTube player.
	 */
	private static void launchCustomYouTubePlayer(YouTubeVideo youTubeVideo, Context context) {
		Intent i = new Intent(context, YouTubePlayerActivity.class);
		i.putExtra(YouTubePlayerActivity.YOUTUBE_VIDEO_OBJ, youTubeVideo);
		context.startActivity(i);
	}

}
