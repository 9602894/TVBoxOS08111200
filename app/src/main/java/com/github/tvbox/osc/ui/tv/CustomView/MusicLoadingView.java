package com.github.tvbox.osc.ui.tv.CustomView;

import android.content.Context;
import android.graphics.Rect;
import android.text.TextUtils;
import android.util.AttributeSet;

public class MarqueeTextView extends androidx.appcompat.widget.AppCompatTextView {
    public MarqueeTextView(Context context) { this(context, null); }
    public MarqueeTextView(Context context, AttributeSet attrs) { this(context, attrs, 0); }
    public MarqueeTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setEllipsize(TextUtils.TruncateAt.MARQUEE);
        setMarqueeRepeatLimit(-1);
        setSingleLine(true);
        setFocusable(true);
        setFocusableInTouchMode(true);
        setHorizontallyScrolling(true);
    }
    @Override public boolean isFocused() { return true; }
    @Override protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        if (focused) super.onFocusChanged(focused, direction, previouslyFocusedRect);
    }
    @Override public void onWindowFocusChanged(boolean hasWindowFocus) {
        if (hasWindowFocus) super.onWindowFocusChanged(hasWindowFocus);
    }
}
