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

import com.google.api.client.util.DateTime;
import com.google.api.services.youtube.model.Playlist;
import com.google.api.services.youtube.model.Thumbnail;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A POJO class to store a YouTube Playlist.
 */
public class YouTubePlaylist implements Serializable {

	private String              id;
	private String              title;
	private String              description;
	private Long                publishDate;
	private int                 videoCount = 0;
	private String              thumbnailUrl;

	/** The YouTube Channel object that this playlist belongs to. */
	private YouTubeChannel channel;

	public YouTubePlaylist(String id, String title, String description, Long publishDate, int videoCount, String thumbnailUrl, YouTubeChannel channel) {
		this.id = id;
		this.title = title;
		this.description = description;
		this.publishDate = publishDate;
		this.videoCount = videoCount;
		this.thumbnailUrl = thumbnailUrl;
		this.channel = channel;
	}

	public YouTubePlaylist(Playlist playlist, YouTubeChannel channel) {
		id = playlist.getId();
		this.channel = channel;

		if(playlist.getSnippet() != null) {
			title = playlist.getSnippet().getTitle();
			description = playlist.getSnippet().getDescription();
			DateTime dt = playlist.getSnippet().getPublishedAt();
			publishDate = dt != null ? dt.getValue() : null;

			if(playlist.getSnippet().getThumbnails() != null) {
				Thumbnail thumbnail = playlist.getSnippet().getThumbnails().getHigh();
				if(thumbnail != null)
					thumbnailUrl = thumbnail.getUrl();
			}
		}

		if(playlist.getContentDetails() != null) {
			videoCount = playlist.getContentDetails().getItemCount().intValue();
		}
	}

	public String getThumbnailUrl() {
		return thumbnailUrl;
	}

	public String getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public int getVideoCount() {
		return videoCount;
	}

	public String getBannerUrl() {
		return channel != null ? channel.getBannerUrl() : null;
	}

	public String getChannelTitle() {
		return channel != null ? channel.getTitle() : null;
	}

	/**
	 * Gets the {@link #publishDate} as a pretty string.
	 */
	public String getPublishDatePretty() {
		return (publishDate != null)
						? new PrettyTimeEx().format(publishDate)
						: "???";
	}
}
