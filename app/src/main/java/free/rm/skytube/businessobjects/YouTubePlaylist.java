package free.rm.skytube.businessobjects;

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
	private static final String	TAG = YouTubePlaylist.class.getSimpleName();

	private String id;
	private String title;
	private String description;
	private DateTime publishDate;
	private int videoCount = 0;
	private String thumbnailUrl;
	private List<YouTubeVideo> videos = new ArrayList<>();

	// The YouTube Channel object that this playlist belongs to
	private YouTubeChannel channel;

	public YouTubePlaylist(Playlist playlist, YouTubeChannel channel) {
		id = playlist.getId();
		this.channel = channel;

		if(playlist.getSnippet() != null) {
			title = playlist.getSnippet().getTitle();
			description = playlist.getSnippet().getDescription();
			publishDate = playlist.getSnippet().getPublishedAt();

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

	public void setThumbnailUrl(String thumbnailUrl) {
		this.thumbnailUrl = thumbnailUrl;
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

	public List<YouTubeVideo> getVideos() {
		return videos;
	}

	public String getBannerUrl() {
		return channel.getBannerUrl();
	}

	public YouTubeChannel getChannel() {
		return channel;
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
