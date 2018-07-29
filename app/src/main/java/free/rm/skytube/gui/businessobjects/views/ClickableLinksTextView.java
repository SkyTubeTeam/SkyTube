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
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AlertDialog;
import android.util.AttributeSet;
import android.widget.Toast;

import com.klinker.android.link_builder.Link;
import com.klinker.android.link_builder.LinkBuilder;
import com.klinker.android.link_builder.LinkConsumableTextView;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import free.rm.skytube.R;
import free.rm.skytube.businessobjects.GetPlaylistTask;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannelInterface;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubePlaylist;
import free.rm.skytube.businessobjects.YouTube.Tasks.GetYouTubeChannelInfoTask;
import free.rm.skytube.gui.activities.MainActivity;
import free.rm.skytube.gui.businessobjects.PlaylistClickListener;
import free.rm.skytube.gui.businessobjects.YouTubePlayer;
import free.rm.skytube.gui.fragments.ChannelBrowserFragment;
import free.rm.skytube.gui.fragments.PlaylistVideosFragment;

/**
 * A {@link android.widget.TextView} which is able to handle clicks on links within the set text.
 * YouTube related links are handled by the app itself, while other links are handled by third-
 * party Android apps.
 */
public class ClickableLinksTextView extends LinkConsumableTextView {

	public ClickableLinksTextView(Context context) {
		super(context);
	}


	public ClickableLinksTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}


	public ClickableLinksTextView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}


	/**
	 * Sets the text to be displayed and ensure that any links (in the text) are clickable.
	 *
	 * @param text
	 */
	public void setTextAndLinkify(CharSequence text) {
		setText(text);
		linkify();
	}


	/**
	 * Linkify the text inside this TextView.  Certain kinds of URLs are to be opened from within
	 * the app itself (i.e. YouTube video urls, playlist urls, etc); all other URLs are to be opened
	 * by using third party Android apps.
	 *
	 * Furthermore, all long clicks are captured to show a menu to open, copy, or share the url.
	 */
	private void linkify() {
		Link link = new Link(android.util.Patterns.WEB_URL);
		final Pattern videoPattern = Pattern.compile("http(?:s?):\\/\\/(?:www\\.)?youtu(?:be\\.com\\/watch\\?v=|\\.be\\/)([\\w\\-\\_]*)(&(amp;)?[\\w\\?=\\.]*)?");
		final Pattern playlistPattern = Pattern.compile("^.*(youtu.be\\/|list=)([^#\\&\\?]*).*");
		final Pattern channelPattern = Pattern.compile("(?:https|http)\\:\\/\\/(?:[\\w]+\\.)?youtube\\.com\\/(?:c\\/|channel\\/|user\\/)?([a-zA-Z0-9\\-]{1,})");

		// set the on click listener
		link.setOnClickListener(new Link.OnClickListener() {
			@Override
			public void onClick(String clickedText) {
				final Matcher playlistMatcher = playlistPattern.matcher(clickedText);
				final Matcher channelMatcher = channelPattern.matcher(clickedText);

				if(videoPattern.matcher(clickedText).matches()) {
					YouTubePlayer.launch(clickedText, getContext());
				} else if(playlistMatcher.matches()) {
					String playlistId = playlistMatcher.group(2);
					// Retrieve the playlist from the playlist ID that was in the url the user clicked on
					new GetPlaylistTask(playlistId, new PlaylistClickListener() {
						@Override
						public void onClickPlaylist(YouTubePlaylist playlist) {
							// Pass the clicked playlist to PlaylistVideosFragment.
							Intent playlistIntent = new Intent(getContext(), MainActivity.class);
							playlistIntent.setAction(MainActivity.ACTION_VIEW_PLAYLIST);
							playlistIntent.putExtra(PlaylistVideosFragment.PLAYLIST_OBJ, playlist);
							getContext().startActivity(playlistIntent);
						}
					}).executeInParallel();
				} else if(channelMatcher.matches()) {
					String username = channelMatcher.group(1);
					new GetYouTubeChannelInfoTask(getContext(), new YouTubeChannelInterface() {
						@Override
						public void onGetYouTubeChannel(YouTubeChannel youTubeChannel) {
							Intent channelIntent = new Intent(getContext(), MainActivity.class);
							channelIntent.setAction(MainActivity.ACTION_VIEW_CHANNEL);
							channelIntent.putExtra(ChannelBrowserFragment.CHANNEL_OBJ, youTubeChannel);
							getContext().startActivity(channelIntent);
						}
					}).setUsingUsername().executeInParallel(username);
				} else {
					Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(clickedText));
					browserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					getContext().startActivity(browserIntent);
				}
			}
		});

		// Handle long click by showing a dialog allowing the user to open the link in a browser, copy the url, or share it.
		link.setOnLongClickListener(new Link.OnLongClickListener() {
			@Override
			public void onLongClick(final String clickedText) {
				AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
						.setTitle(clickedText)
						.setItems(new CharSequence[]
										{getContext().getString(R.string.open_in_browser), getContext().getString(R.string.copy_url), getContext().getString(R.string.share_via)},
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										switch (which) {
											case 0:
												Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(clickedText));
												browserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
												getContext().startActivity(browserIntent);
												break;
											case 1:
												ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
												ClipData clip = ClipData.newPlainText("URL", clickedText);
												clipboard.setPrimaryClip(clip);
												Toast.makeText(getContext(), R.string.url_copied_to_clipboard, Toast.LENGTH_SHORT).show();
												break;
											case 2:
												Intent intent = new Intent(android.content.Intent.ACTION_SEND);
												intent.setType("text/plain");
												intent.putExtra(android.content.Intent.EXTRA_TEXT, clickedText);
												getContext().startActivity(Intent.createChooser(intent, getContext().getString(R.string.share_via)));
												break;
										}
									}
								});
				builder.create().show();
			}
		});

		LinkBuilder.on(this)
				.addLink(link)
				.build();
	}

}
