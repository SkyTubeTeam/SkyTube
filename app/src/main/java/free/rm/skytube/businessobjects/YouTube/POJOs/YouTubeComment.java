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

package free.rm.skytube.businessobjects.YouTube.POJOs;

import com.google.api.client.util.ArrayMap;
import com.google.api.client.util.DateTime;

import org.ocpsoft.prettytime.PrettyTime;

import java.time.Instant;

/**
 * A YouTube comment.
 */
public class YouTubeComment {

	private String author;
	private String comment;
	private String datePublished;
	private Long likeCount;
	private String thumbnailUrl;
	private String authorChannelId;
	private boolean pinned;
	private boolean heartedByUploader;

	public YouTubeComment(com.google.api.services.youtube.model.Comment comment) {
		if (comment.getSnippet() != null) {
			this.author = comment.getSnippet().getAuthorDisplayName();
			ArrayMap<String, String> channelIdMap = (ArrayMap<String, String>)comment.getSnippet().getAuthorChannelId();
			if(channelIdMap != null)
				this.authorChannelId = channelIdMap.get("value");
			this.comment = comment.getSnippet().getTextDisplay();
			final DateTime publishedAt = comment.getSnippet().getPublishedAt();
			this.datePublished = new PrettyTime().format(Instant.ofEpochMilli(publishedAt.getValue()));
			this.likeCount = comment.getSnippet().getLikeCount();
			this.thumbnailUrl = comment.getSnippet().getAuthorProfileImageUrl();
		}
	}

	public YouTubeComment(String authorChannelId, String author, String thumbnailUrl, String comment, String datePublished, long likeCount, boolean pinned, boolean heartedByUploader) {
		this.author = author;
		this.comment = comment;
		this.datePublished = datePublished;
		this.likeCount = (0 <= likeCount ? Long.valueOf(likeCount) : null);
		this.thumbnailUrl = thumbnailUrl;
		this.authorChannelId = authorChannelId;
		this.pinned = pinned;
		this.heartedByUploader = heartedByUploader;
	}

	public String getAuthor() {
		return author;
	}

	public String getComment() {
		return comment;
	}

	public String getDatePublished() {
		return datePublished;
	}

	public Long getLikeCount() {
		return likeCount;
	}

	public String getThumbnailUrl() {
		return thumbnailUrl;
	}

	public String getAuthorChannelId() { return authorChannelId; }

	public boolean isPinned() {
		return pinned;
	}

	public boolean isHeartedByUploader() {
		return heartedByUploader;
	}
}
