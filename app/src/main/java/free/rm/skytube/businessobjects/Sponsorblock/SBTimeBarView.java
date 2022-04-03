package free.rm.skytube.businessobjects.Sponsorblock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.HashMap;

import free.rm.skytube.R;

public class SBTimeBarView extends View {
    private static final String TAG = SBTimeBarView.class.getSimpleName();
    SBVideoInfo SBVideoInfo = new SBVideoInfo();
    HashMap<String, Paint> categoryPaint = new HashMap<String, Paint>();

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
        Paint categorySponsor = new Paint();
        Paint categorySelfPromo = new Paint();
        Paint categoryInteraction = new Paint();
        Paint categoryMusicOfftopic = new Paint();
        Paint categoryIntro = new Paint();
        Paint categoryOutro = new Paint();
        Paint categoryPreview = new Paint();
        Paint categoryFiller = new Paint();

        categorySponsor.setColor(getResources().getColor(R.color.sponsorblock_category_sponsor));
        categorySelfPromo.setColor(getResources().getColor(R.color.sponsorblock_category_selfpromo));
        categoryInteraction.setColor(getResources().getColor(R.color.sponsorblock_category_interaction));
        categoryMusicOfftopic.setColor(getResources().getColor(R.color.sponsorblock_category_music_offtopic));
        categoryIntro.setColor(getResources().getColor(R.color.sponsorblock_category_intro));
        categoryOutro.setColor(getResources().getColor(R.color.sponsorblock_category_outro));
        categoryPreview.setColor(getResources().getColor(R.color.sponsorblock_category_preview));
        categoryFiller.setColor(getResources().getColor(R.color.sponsorblock_category_filler));

        categoryPaint.clear();
        categoryPaint.put("sponsor", categorySponsor);
        categoryPaint.put("selfpromo", categorySelfPromo);
        categoryPaint.put("interaction", categoryInteraction);
        categoryPaint.put("music_offtopic", categoryMusicOfftopic);
        categoryPaint.put("intro", categoryIntro);
        categoryPaint.put("outro", categoryOutro);
        categoryPaint.put("preview", categoryPreview);
        categoryPaint.put("filler", categoryFiller);
    }

    public void setSegments(SBVideoInfo SBVideoInfo) {
        this.SBVideoInfo = SBVideoInfo;

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(!SBVideoInfo.isLoaded()) {
            Log.d(TAG, "SBInfo not loaded yet");
            return;
        }
        Log.d(TAG, "SBInfo has loaded now for "+SBVideoInfo.getSegments().size()+" segments");

        int viewWidth = canvas.getWidth();
        int viewHeight = canvas.getHeight();
        double duration = SBVideoInfo.getVideoDuration();
        double pxToS = viewWidth / duration;

        int i = 0;
        for (SBSegment segment : SBVideoInfo.getSegments()) {
            i++;

            float startX = Double.valueOf (segment.getStartPos() * pxToS).floatValue();
            float endX = Double.valueOf(segment.getEndPos() * pxToS).floatValue();
            Paint segmentPaint = categoryPaint.containsKey(segment.getCategory()) ? categoryPaint.get(segment.getCategory()) : categoryPaint.get("sponsor");

            Log.d(TAG, "Segment "+i+" / "+segment.getCategory()+" from "+startX+" to "+endX);
            canvas.drawRect(startX, 0, endX, viewHeight, segmentPaint);
        }
    }
}
