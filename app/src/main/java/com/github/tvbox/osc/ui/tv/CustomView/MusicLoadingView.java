package com.github.tvbox.osc.ui.tv.CustomView;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;

public class MusicLoadingView extends View {
    private Paint mPaint;
    private float mWidth = 0f;
    private float mPadding = 0f;
    private float mStartAngle = 0f;
    private RectF mRectF;
    private RotateAnimation mAnimation;

    public MusicLoadingView(Context context) {
        this(context, null);
    }

    public MusicLoadingView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MusicLoadingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initPaint();
    }

    private void initPaint() {
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(Color.WHITE);
        mPaint.setStrokeWidth(4f);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mWidth = w;
        mPadding = 5;
        mRectF = new RectF(mPadding, mPadding, mWidth - mPadding, mWidth - mPadding);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mRectF == null) return;
        canvas.drawArc(mRectF, mStartAngle, 100, false, mPaint);
    }

    public void startAnim() {
        stopAnim();
        mAnimation = new RotateAnimation(0f, 360f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        mAnimation.setDuration(1000);
        mAnimation.setInterpolator(new LinearInterpolator());
        mAnimation.setRepeatCount(Animation.INFINITE);
        mAnimation.setFillAfter(true);
        startAnimation(mAnimation);
    }

    public void stopAnim() {
        if (mAnimation != null) {
            clearAnimation();
            mAnimation = null;
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startAnim();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopAnim();
    }
}
