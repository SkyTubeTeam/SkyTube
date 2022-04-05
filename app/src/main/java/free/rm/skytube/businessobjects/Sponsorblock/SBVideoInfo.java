package free.rm.skytube.businessobjects.Sponsorblock;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SBVideoInfo {
    private final double videoDuration;
    private final List<SBSegment> segments = new ArrayList<SBSegment>();

    public SBVideoInfo(JSONArray sponsorblockInfo) throws JSONException {
        double firstDuration = 0;
        for (int i = 0; i < sponsorblockInfo.length(); i++) {
            JSONObject segmentDetails = sponsorblockInfo.getJSONObject(i);
            SBSegment thisSegment = new SBSegment(segmentDetails);
            this.segments.add(thisSegment);

            if(i == 0) {
                firstDuration = segmentDetails.getDouble("videoDuration");
            }
        }
        this.videoDuration = firstDuration;
    }

    public double getVideoDuration() {
        return videoDuration;
    }

    public List<SBSegment> getSegments() {
        return segments;
    }
}
