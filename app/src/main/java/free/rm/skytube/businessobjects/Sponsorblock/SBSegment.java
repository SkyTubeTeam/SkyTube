package free.rm.skytube.businessobjects.Sponsorblock;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;

public class SBSegment {
    private final String category;
    private final double startPos;
    private final double endPos;

    public SBSegment(String category, double startPos, double endPos) {
        this.category = category;
        this.startPos = startPos;
        this.endPos = endPos;
    }

    public SBSegment(JsonObject jsonSegment) {
        category = jsonSegment.getString("category");

        JsonArray segmentTimes = jsonSegment.getArray("segment");
        startPos = segmentTimes.getDouble(0);
        endPos = segmentTimes.getDouble(1);
    }

    public String getCategory() {
        return category;
    }

    public double getStartPos() {
        return startPos;
    }

    public double getEndPos() {
        return endPos;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("SBSegment{");
        sb.append("category='").append(category).append('\'');
        sb.append(", startPos=").append(startPos);
        sb.append(", endPos=").append(endPos);
        sb.append('}');
        return sb.toString();
    }
}
