package free.rm.skytube.gui.businessobjects;

import android.content.Context;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatTextView;

import java.util.concurrent.TimeUnit;

/**
 * Custom View class to display a video's RunTime. If the video is less than one hour long,
 * the hours will not show.
 */
public class RuntimeView extends AppCompatTextView {
	private int milliseconds;

	public RuntimeView(Context context) {
		super(context);
	}

	public RuntimeView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public RuntimeView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public void setMilliseconds(int milliseconds) {
		this.milliseconds = milliseconds;
		setText(getTimeDisplay(milliseconds));
	}

	public void setSeconds(int seconds) {
		setText(getTimeDisplay(seconds * 1000));
	}

	public int getMilliseconds() {
		return milliseconds;
	}

	public int getSeconds() {
		return milliseconds / 1000;
	}

	private String getTimeDisplay(int milliseconds) {
		long hours = TimeUnit.MILLISECONDS.toHours(milliseconds);
		long minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds);
		if(minutes > 59)
			minutes = minutes % 60;
		long seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds);
		if(seconds > 59)
			seconds = seconds % 60;

		String out;
		if(hours == 0)
			out = String.format("%02d:%02d", minutes, seconds);
		else
			out = String.format("%02d:%02d:%02d", hours, minutes, seconds);
		return out;
	}
}
