/*
 * SkyTube
 * Copyright (C) 2020  Zsombor Gegesy
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation (version 3 of the License).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package free.rm.skytube.gui.businessobjects.views;

import android.os.Handler;
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.widget.TextView;

import java.util.HashSet;
import java.util.Set;

class TouchableMovementMethod extends LinkMovementMethod {
    interface URLSpanClickListener {
        void onClick(URLSpan span, boolean longClick);
    }

    final Object lock = new Object();
    URLSpan pressedSpan;
    Set<URLSpan> touchedSpans = new HashSet<>();
    boolean touched;
    URLSpanClickListener listener;

    TouchableMovementMethod(URLSpanClickListener listener) {
        this.listener = listener;
    }
    /**
     * Manages the touches to find the link that was clicked and highlight it
     *
     * @param textView  view the user clicked
     * @param spannable spannable string inside the clicked view
     * @param event     motion event that occurred
     * @return
     */
    @Override
    public boolean onTouchEvent(TextView textView, Spannable spannable, MotionEvent event) {
        synchronized (lock) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN: {
                    handleActionDown(textView, spannable, event);
                    break;
                }
                case MotionEvent.ACTION_MOVE: {
                    handleActionMove(textView, spannable, event);
                    break;
                }
                case MotionEvent.ACTION_UP: {
                    handleActionUp(spannable);
                    break;
                }
                default:
                    handleOther(textView, spannable, event);
            }
        }

        return true;
    }

    private void handleOther(TextView textView, Spannable spannable, MotionEvent event) {
        if (pressedSpan != null) {
            touchedSpans.remove(pressedSpan);
            touched = false;

            super.onTouchEvent(textView, spannable, event);
        }

        pressedSpan = null;

        Selection.removeSelection(spannable);
    }

    private void handleActionUp(Spannable spannable) {
        if (pressedSpan != null) {
            listener.onClick(pressedSpan, false);
            touchedSpans.remove(pressedSpan);
            pressedSpan = null;

            Selection.removeSelection(spannable);
        }
    }

    private void handleActionMove(TextView textView, Spannable spannable, MotionEvent event) {
        URLSpan touchedSpan = getPressedSpan(textView, spannable, event);

        if (pressedSpan != null && pressedSpan != touchedSpan) {
            touchedSpans.remove(pressedSpan);
            pressedSpan = null;
            touched = false;

            Selection.removeSelection(spannable);
        }
    }

    private void handleActionDown(TextView textView, Spannable spannable, MotionEvent event) {
        pressedSpan = getPressedSpan(textView, spannable, event);

        if (pressedSpan != null) {
            touchedSpans.add(pressedSpan);
            touched = true;

            new Handler().postDelayed(() -> {
                synchronized (lock) {

                    if (touched && pressedSpan != null) {
                        if (textView.isHapticFeedbackEnabled()) {
                            textView.setHapticFeedbackEnabled(false);
                        }
                        textView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                        touchedSpans.remove(pressedSpan);
                        listener.onClick(pressedSpan, true);
                        pressedSpan = null;

                        Selection.removeSelection(spannable);
                    }
                }
            }, 500);

            Selection.setSelection(spannable, spannable.getSpanStart(pressedSpan),
                    spannable.getSpanEnd(pressedSpan));
        }
    }

    /**
     * Find the span that was clicked
     *
     * @param widget    view the user clicked
     * @param spannable spannable string inside the clicked view
     * @param event     motion event that occurred
     * @return the touchable span that was pressed
     */
    private URLSpan getPressedSpan(TextView widget, Spannable spannable, MotionEvent event) {
        int x = Math.round(event.getX());
        int y = Math.round(event.getY());

        x -= widget.getTotalPaddingLeft();
        y -= widget.getTotalPaddingTop();

        x += widget.getScrollX();
        y += widget.getScrollY();

        Layout layout = widget.getLayout();
        int line = layout.getLineForVertical(y);

        int off;
        try {
            off = layout.getOffsetForHorizontal(line, (float) x);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }

        int end = layout.getLineEnd(line);

        // offset seems like it can be one off in some cases
        // Could be what was causing issue 7 in the first place:
        // https://github.com/klinker24/Android-TextView-LinkBuilder/issues/7
        if (off != end && off != end - 1) {
            URLSpan[] links = spannable.getSpans(off, off, URLSpan.class);

            if (links.length > 0) {
                return links[0];
            }
        }

        return null;
    }

}
