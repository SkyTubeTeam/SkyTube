package free.rm.skytube.businessobjects.Sponsorblock;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SBSegment {
    private final String category;
    private final double startPos;
    private final double endPos;

    public SBSegment(JSONObject jsonSegment) throws JSONException {
        category = jsonSegment.getString("category");

        JSONArray segmentTimes = jsonSegment.getJSONArray("segment");
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
