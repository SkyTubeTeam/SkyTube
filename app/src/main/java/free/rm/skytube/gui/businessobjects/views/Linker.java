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

import android.content.Context;
import android.content.Intent;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.text.HtmlCompat;
import androidx.core.text.util.LinkifyCompat;

import org.schabi.newpipe.extractor.StreamingService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.YouTube.YouTubeTasks;
import free.rm.skytube.businessobjects.YouTube.newpipe.ContentId;
import free.rm.skytube.businessobjects.db.BookmarksDb;

/**
 * A {@link android.widget.TextView} which is able to handle clicks on links within the set text.
 * YouTube related links are handled by the app itself, while other links are handled by third-
 * party Android apps.
 */
public class Linker {

	private final static String TAG = Linker.class.getSimpleName();

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
		Spanned spanns = span(text);
		textView.setText(spanns);
	}

	private static Spanned span(String text) {
		if (isText(text)) {
			return spanText(text);
		} else {
			return HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_LEGACY);
		}
	}

	private static boolean isText(String text) {
		String lower = text.toLowerCase();
		return (!lower.contains("<a ") && !lower.contains("<br"));
	}

	private static Spanned spanText(String text) {
		SpannableStringBuilder spanner = new SpannableStringBuilder(text.replaceAll("&nbsp;", " "));
		LinkifyCompat.addLinks(spanner, Linkify.WEB_URLS);
		return spanner;
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
			if (longClick) {
				longClick(span.getURL());
			} else {
				SkyTubeApp.openUrl(ctx, span.getURL(), true);
			}
		}

		private void longClick(String clickedText) {
			List<String> items = new ArrayList<>(Arrays.asList(ctx.getString(R.string.open_in_browser), ctx.getString(R.string.copy_url), ctx.getString(R.string.share_via)));
			ContentId content = SkyTubeApp.parseUrl(ctx, clickedText, false);
			final boolean isBookmarked;
			if(content != null && content.getType() == StreamingService.LinkType.STREAM) {
				isBookmarked = BookmarksDb.getBookmarksDb().isBookmarked(content.getId());
				if(isBookmarked) {
					items.add(ctx.getString(R.string.unbookmark));
				} else {
					items.add(ctx.getString(R.string.bookmark));
				}
			} else {
				isBookmarked = false;
			}
			new AlertDialog.Builder(ctx)
					.setTitle(clickedText)
					.setItems(items.toArray(new CharSequence[0]),
							(dialog, which) -> {
								switch (which) {
									case 0:
										SkyTubeApp.viewInBrowser(clickedText, ctx);
										break;
									case 1:
										SkyTubeApp.copyUrl(ctx, "URL", clickedText);
										break;
									case 2:
										Intent intent = new Intent(Intent.ACTION_SEND);
										intent.setType("text/plain");
										intent.putExtra(Intent.EXTRA_TEXT, clickedText);
										ctx.startActivity(Intent.createChooser(intent, ctx.getString(R.string.share_via)));
										break;
									case 3:
										YouTubeTasks.getVideoDetails(ctx, content)
												.flatMapSingle(video -> {
													if(!isBookmarked) {
														return video.bookmarkVideo(ctx);
													} else {
														return video.unbookmarkVideo(ctx, null);
													}
												}).subscribe();
										break;
								}
							})
					.show();
		}
	}

}
