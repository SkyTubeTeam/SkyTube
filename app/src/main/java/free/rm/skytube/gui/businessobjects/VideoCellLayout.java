package free.rm.skytube.gui.businessobjects;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

/** A RelativeLayout that will always have an aspect ratio of 4:3 */
public class VideoCellLayout extends RelativeLayout {
	private static final double WIDTH_RATIO = 4;
	private static final double HEIGHT_RATIO = 3;

	public VideoCellLayout(Context context) {
		super(context);
	}

	public VideoCellLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public VideoCellLayout(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public VideoCellLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int widthSize = MeasureSpec.getSize(widthMeasureSpec);
		int heightSize = (int) (HEIGHT_RATIO / WIDTH_RATIO * widthSize);
		int newHeightSpec = MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY);
		super.onMeasure(widthMeasureSpec, newHeightSpec);
	}
}
