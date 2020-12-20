package free.rm.skytube.businessobjects.YouTube.POJOs;

import org.ocpsoft.prettytime.PrettyTime;

import java.io.Serializable;
import java.time.Instant;

public class CardData implements Serializable {
    protected String              id;
    protected String              title;
    protected String              description;
    protected Long                publishTimestamp;
    protected boolean             publishTimestampExact;
    protected String              thumbnailUrl;

    /**
     * The video/playlist/etc publish date in pretty format (e.g. "17 hours ago").
     */
    private transient String      publishDatePretty;
    /**
     * The time when the publishDatePretty was calculated.
     */
    private transient long        publishDatePrettyCalculationTime;

    /** publishDate will remain valid for 1 hour. */
    private final static long     PUBLISH_DATE_VALIDITY_TIME = 60 * 60 * 1000L;

    public final String getId() {
        return id;
    }

    public final void setId(String id) {
        this.id = id;
    }

    public final String getTitle() {
        return title;
    }

    public final void setTitle(String title) {
        this.title = title;
    }

    public final String getDescription() {
        return description;
    }

    public final void setDescription(String description) {
        this.description = description;
    }

    public void setPublishTimestampExact(boolean publishTimestampExact) {
        this.publishTimestampExact = publishTimestampExact;
    }

    public boolean getPublishTimestampExact() {
        return publishTimestampExact;
    }

    public final Long getPublishTimestamp() {
        return publishTimestamp;
    }

    public final void setPublishTimestamp(Long publishTimestamp) {
        this.publishTimestamp = publishTimestamp;
        forceRefreshPublishDatePretty();
    }

    public final String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public final void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    /**
     * Gets the {@link #publishTimestamp} as a pretty string.
     */
    public final String getPublishDatePretty() {
        long now = System.currentTimeMillis();
        // if pretty is not yet calculated, or the publish date was generated more than (1 hour) PUBLISH_DATE_VALIDITY_TIME ago...
        if (publishTimestamp != null && (publishDatePretty == null ||
                PUBLISH_DATE_VALIDITY_TIME < now - publishDatePrettyCalculationTime)) {
            this.publishDatePretty = new PrettyTime().format(Instant.ofEpochMilli(publishTimestamp));
            this.publishDatePrettyCalculationTime = now;
        }
        return publishDatePretty != null ? publishDatePretty : "???";
    }

    /**
     * Given that {@link #publishDatePretty} is being cached once generated, this method will allow
     * you to regenerate and reset the {@link #publishDatePretty}.
     */
    public final void forceRefreshPublishDatePretty() {
        // Will force the publishDatePretty to be regenerated.  Refer to getPublishDatePretty()
        if (publishTimestamp != null) {
            // only,when publishDate is set - so if only the 'pretty' is available, no refresh will occur.
            this.publishDatePretty = null;
        }
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + '{' +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
