package free.rm.skytube.businessobjects.Sponsorblock;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class SBVideoInfo {
    private final double videoDuration;
    private final List<SBSegment> segments = new ArrayList<SBSegment>();

    public SBVideoInfo(double videoDuration) {
        this.videoDuration = videoDuration;
    }

    public SBVideoInfo(JsonArray sponsorblockInfo) {
        double firstDuration = 0;
        for (int i = 0; i < sponsorblockInfo.size(); i++) {
            JsonObject segmentDetails = sponsorblockInfo.getObject(i);
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

    @Override
    public String toString() {
        return "SBVideoInfo{" +
                "videoDuration=" + videoDuration +
                ", segments=" + segments +
                '}';
    }
}
