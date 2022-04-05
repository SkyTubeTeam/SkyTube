package free.rm.skytube.businessobjects.Sponsorblock;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.HashMap;
import java.util.Map;

public class SBTimeBarView extends View {
    private static final String TAG = SBTimeBarView.class.getSimpleName();
    private SBVideoInfo videoInfo;
    private final HashMap<String, Paint> categoryPaint = new HashMap<String, Paint>();

    public SBTimeBarView(Context context) {
        super(context);
        initDraw();
    }

    public SBTimeBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initDraw();
    }

    public SBTimeBarView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initDraw();
    }

    protected void initDraw() {
        Resources res = getResources();
        for (Map.Entry<String, SBTasks.LabelAndColor> entry : SBTasks.getAllCategories()) {
            Paint paint = new Paint();
            paint.setColor(res.getColor(entry.getValue().color));
            categoryPaint.put(entry.getKey(), paint);
        }
    }

    public void setSegments(SBVideoInfo videoInfo) {
        this.videoInfo = videoInfo;

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(videoInfo == null) {
            Log.d(TAG, "SBInfo not loaded yet");
            return;
        }
        Log.d(TAG, "SBInfo has loaded now for " + videoInfo.getSegments().size() + " segments");

        int viewWidth = canvas.getWidth();
        int viewHeight = canvas.getHeight();
        double duration = videoInfo.getVideoDuration();
        double pxToS = viewWidth / duration;

        int i = 0;
        for (SBSegment segment : videoInfo.getSegments()) {
            i++;

            float startX = Double.valueOf (segment.getStartPos() * pxToS).floatValue();
            float endX = Double.valueOf(segment.getEndPos() * pxToS).floatValue();
            Paint segmentPaint = categoryPaint.containsKey(segment.getCategory()) ? categoryPaint.get(segment.getCategory()) : categoryPaint.get("sponsor");

            Log.d(TAG, "Segment "+i+" / "+segment.getCategory()+" from "+startX+" to "+endX);
            canvas.drawRect(startX, 0, endX, viewHeight, segmentPaint);
        }
    }
}
