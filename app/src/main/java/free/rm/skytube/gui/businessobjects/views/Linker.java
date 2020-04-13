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

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.appcompat.app.AlertDialog;

import android.os.Build;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.widget.TextView;
import android.widget.Toast;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import free.rm.skytube.R;
import free.rm.skytube.businessobjects.Logger;
import free.rm.skytube.businessobjects.YouTube.Tasks.GetPlaylistTask;
import free.rm.skytube.businessobjects.YouTube.Tasks.GetYouTubeChannelInfoTask;
import free.rm.skytube.gui.activities.MainActivity;
import free.rm.skytube.gui.businessobjects.YouTubePlayer;
import free.rm.skytube.gui.fragments.PlaylistVideosFragment;

/**
 * A {@link android.widget.TextView} which is able to handle clicks on links within the set text.
 * YouTube related links are handled by the app itself, while other links are handled by third-
 * party Android apps.
 */
public class Linker {
	private static final Pattern videoPattern = Pattern.compile("http(?:s?):\\/\\/(?:www\\.)?youtu(?:be\\.com\\/watch\\?v=|\\.be\\/)([\\w\\-\\_]*)(&(amp;)?[\\w\\?=\\.]*)?");
	private static final Pattern playlistPattern = Pattern.compile("^.*(youtu.be\\/|list=)([^#\\&\\?]*).*");
	private static final Pattern channelPattern = Pattern.compile("(?:https|http)\\:\\/\\/(?:[\\w]+\\.)?youtube\\.com\\/(?:c\\/|channel\\/|user\\/)?([a-zA-Z0-9\\-]{1,})");

	public static void configure(TextView textView) {
		textView.setAutoLinkMask(0);
		textView.setMovementMethod(new TouchableMovementMethod(new LinkListener(textView.getContext())));
	}
	/**
	 * Sets the text to be displayed and ensure that any links (in the text) are clickable.
	 *
	 * @param text
	 */
	public static void setTextAndLinkify(TextView textView, String text) {
		Logger.i(Linker.class.getSimpleName(), "setText: %s", text);
		Spanned spanns = span(text);
		textView.setText(spanns);
	}

	private static Spanned span(String text) {
		if (isText(text)) {
			return spanText(text);
		} else {
			return spanHtml(text);
		}
	}

	private static boolean isText(String text) {
		String lower = text.toLowerCase();
		return (!lower.contains("<a ") && !lower.contains("<br"));
	}

	private static Spanned spanText(String text) {
		SpannableStringBuilder spanner = new SpannableStringBuilder(text);
		Linkify.addLinks(spanner, Linkify.WEB_URLS);
		return spanner;
	}

	private static Spanned spanHtml(String content) {
		if (Build.VERSION.SDK_INT >= 24) {
			return Html.fromHtml(content, 0);
		} else {
			//noinspection deprecation
			return Html.fromHtml(content);
		}
	}

	/**
	 * Certain kinds of URLs are to be opened from within
	 * the app itself (i.e. YouTube video urls, playlist urls, etc); all other URLs are to be opened
	 * by using third party Android apps.
	 *
	 * Furthermore, all long clicks are captured to show a menu to open, copy, or share the url.
	 */
	static class LinkListener implements TouchableMovementMethod.URLSpanClickListener {
		private final Context ctx;
		LinkListener(Context ctx) {
			this.ctx = ctx;
		}

		@Override
		public void onClick(URLSpan span, boolean longClick) {
			Logger.i(Linker.class.getSimpleName(), "onClick: %s, longClick= %s", span.getURL(), longClick);
			if (longClick) {
				longClick(span.getURL());
			} else {
				shortClick(span.getURL());
			}
		}

		private void shortClick(String clickedText) {
			final Matcher playlistMatcher = playlistPattern.matcher(clickedText);
			final Matcher channelMatcher = channelPattern.matcher(clickedText);

			if(videoPattern.matcher(clickedText).matches()) {
				YouTubePlayer.launch(clickedText, ctx);
			} else if(playlistMatcher.matches()) {
				String playlistId = playlistMatcher.group(2);
				// Retrieve the playlist from the playlist ID that was in the url the user clicked on
				new GetPlaylistTask(ctx, playlistId, playlist -> {
					// Pass the clicked playlist to PlaylistVideosFragment.
					Intent playlistIntent = new Intent(ctx, MainActivity.class);
					playlistIntent.setAction(MainActivity.ACTION_VIEW_PLAYLIST);
					playlistIntent.putExtra(PlaylistVideosFragment.PLAYLIST_OBJ, playlist);
					ctx.startActivity(playlistIntent);
				}).executeInParallel();
			} else if(channelMatcher.matches()) {
				String username = channelMatcher.group(1);
				new GetYouTubeChannelInfoTask(ctx, youTubeChannel -> {
					YouTubePlayer.launchChannel(youTubeChannel, ctx);
				}).setUsingUsername().executeInParallel(username);
			} else {
				Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(clickedText));
				browserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				ctx.startActivity(browserIntent);
			}
		}

		private void longClick(String clickedText) {
			AlertDialog.Builder builder = new AlertDialog.Builder(ctx)
					.setTitle(clickedText)
					.setItems(new CharSequence[] {
									ctx.getString(R.string.open_in_browser),
									ctx.getString(R.string.copy_url),
									ctx.getString(R.string.share_via)},
							(dialog, which) -> {
								switch (which) {
									case 0:
										Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(clickedText));
										browserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
										ctx.startActivity(browserIntent);
										break;
									case 1:
										ClipboardManager clipboard = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
										ClipData clip = ClipData.newPlainText("URL", clickedText);
										clipboard.setPrimaryClip(clip);
										Toast.makeText(ctx, R.string.url_copied_to_clipboard, Toast.LENGTH_SHORT).show();
										break;
									case 2:
										Intent intent = new Intent(Intent.ACTION_SEND);
										intent.setType("text/plain");
										intent.putExtra(Intent.EXTRA_TEXT, clickedText);
										ctx.startActivity(Intent.createChooser(intent, ctx.getString(R.string.share_via)));
										break;
								}
							});
			builder.create().show();
		}
	}

}
