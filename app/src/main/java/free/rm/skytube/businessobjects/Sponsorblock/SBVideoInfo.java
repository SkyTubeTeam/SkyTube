package free.rm.skytube.businessobjects.Sponsorblock;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SBVideoInfo {
    protected boolean loaded = false;
    protected double videoDuration;
    protected List<SBSegment> segments = new ArrayList<SBSegment>();

    public SBVideoInfo() {
    }

    public SBVideoInfo(JSONArray sponsorblockInfo) throws JSONException {
        for (int i = 0; i < sponsorblockInfo.length(); i++) {
            JSONObject segmentDetails = sponsorblockInfo.getJSONObject(i);
            SBSegment thisSegment = new SBSegment(segmentDetails);
            this.segments.add(thisSegment);

            if(i == 0) {
                videoDuration = segmentDetails.getDouble("videoDuration");
            }
        }
        this.loaded = true;
    }

    public double getVideoDuration() {
        return videoDuration;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public void setLoaded(boolean loaded) {
        this.loaded = loaded;
    }

    public List<SBSegment> getSegments() {
        return segments;
    }
}
