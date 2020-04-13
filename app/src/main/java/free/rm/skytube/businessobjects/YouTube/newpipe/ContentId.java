package free.rm.skytube.businessobjects.YouTube.newpipe;

import org.schabi.newpipe.extractor.StreamingService.LinkType;
import free.rm.skytube.app.Utils;

public class ContentId {
    final String id;
    final String canonicalUrl;
    final LinkType type;

    public ContentId(String id, String canonicalUrl, LinkType type) {
        Utils.requireNonNull(id, "id");
        Utils.requireNonNull(canonicalUrl, "canonicalUrl");
        Utils.requireNonNull(type, "type");
        if (type == LinkType.NONE) {
            throw new IllegalArgumentException("LinkType.NONE for id=" + id + ", url=" + canonicalUrl);
        }
        this.id = id;
        this.canonicalUrl = canonicalUrl;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public String getCanonicalUrl() {
        return canonicalUrl;
    }

    public LinkType getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContentId contentId = (ContentId) o;
        return id.equals(contentId.id) &&
                canonicalUrl.equals(contentId.canonicalUrl) &&
                type == contentId.type;
    }

    @Override
    public int hashCode() {
        return Utils.hash(id, canonicalUrl, type);
    }
}
