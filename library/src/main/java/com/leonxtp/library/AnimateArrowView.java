package com.leonxtp.library;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

/**
 * Created by leonxtp on 2018/1/2.
 * Modified by leonxtp on 2018/1/2
 */

public class AnimateArrowView extends View {

    private final String TAG = this.getClass().getSimpleName();

    /**
     * 除去padding之外的宽高
     */
    private int mViewWidth;
    /**
     * 箭头宽度
     */
    private int mArrowHeight;
    /**
     * 箭头移动的距离
     */
    private int mTranslateRange;
    /**
     * 箭头移动的活动范围与箭头显示高度的比例，在宽高都没有指定的时候工作
     */
    private float mArrowHeightRatio = 2.0f;
    /**
     * 箭头drawable距离view活动范围顶部的距离
     */
    private int mArrowTopMargin;
    /**
     * 箭头缩放比例
     */
    private float mViewScale = 1.0f;

    private Drawable mArrowDrawable;

    private int mArrowAlpha = 100;

    private AnimatorSet animatorSet;

    private RefreshProgressRunnable mRefreshProgressRunnable;

    private int mAlphaInDuration = 400, mAlphaHoldOnDuration = 400, mAlphaOutDuration = 400;

    private boolean isAnimatorCancelled = true;

    public AnimateArrowView(Context context) {
        this(context, null);
    }

    public AnimateArrowView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AnimateArrowView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {

        mArrowDrawable = ContextCompat.getDrawable(context, R.drawable.icon_arrow_double);

        TypedArray ta = attrs == null ? null : getContext().obtainStyledAttributes(attrs,
                R.styleable.AnimateArrowView);

        if (ta != null) {
            mArrowHeightRatio = ta.getFloat(
                    R.styleable.AnimateArrowView_arrowHeightRatio, 2.0f);
            int animDuration = ta.getInteger(R.styleable.AnimateArrowView_arrowAnimDuration, 0);
            if (animDuration != 0) {
                mAlphaInDuration = animDuration / 3;
                mAlphaHoldOnDuration = mAlphaInDuration;
                mAlphaOutDuration = mAlphaInDuration;
            } else {
                mAlphaInDuration = ta.getInteger(R.styleable.AnimateArrowView_arrowAnimDuration, 400);
                mAlphaHoldOnDuration = ta.getInteger(R.styleable.AnimateArrowView_arrowAlphaHoldOnDuration, 400);
                mAlphaOutDuration = ta.getInteger(R.styleable.AnimateArrowView_arrowAlphaOutDuration, 400);

            }

            ta.recycle();
        }

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        int measuredWidth, measureHeight;

        int intrinsicWidth = mArrowDrawable.getIntrinsicWidth();
        int intrinsicHeight = mArrowDrawable.getIntrinsicHeight();

        int viewHeight;

        if (widthMode == MeasureSpec.EXACTLY && heightMode == MeasureSpec.EXACTLY) {

            mViewWidth = widthSize - getPaddingLeft() - getPaddingRight();
            viewHeight = heightSize - getPaddingTop() - getPaddingBottom();

            measuredWidth = widthSize;
            measureHeight = heightSize;

        } else if (widthMode == MeasureSpec.EXACTLY) {

            mViewWidth = widthSize - getPaddingLeft() - getPaddingRight();
            viewHeight = (int) (mViewWidth * mArrowHeightRatio);

            measuredWidth = widthSize;
            measureHeight = viewHeight + getPaddingTop() + getPaddingBottom();

        } else if (heightMode == MeasureSpec.EXACTLY) {

            viewHeight = heightSize - getPaddingTop() - getPaddingBottom();
            mViewWidth = (int) (viewHeight / mArrowHeightRatio);

            measuredWidth = mViewWidth + getPaddingLeft() + getPaddingRight();
            measureHeight = heightSize;

        } else {

            mViewWidth = intrinsicWidth + getPaddingLeft() + getPaddingRight();
            viewHeight = (int) (intrinsicHeight * mArrowHeightRatio);

            measuredWidth = mViewWidth + getPaddingLeft() + getPaddingRight();
            measureHeight = viewHeight + getPaddingTop() + getPaddingBottom();
        }

        mArrowHeight = (int) (viewHeight / mArrowHeightRatio);
        mTranslateRange = viewHeight - mArrowHeight;

        if (animatorSet == null) {
            initAnimator();
        }

        setMeasuredDimension(measuredWidth, measureHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int left = (int) ((1 - mViewScale) * mViewWidth / 2);
        int top = (int) (mViewScale * mArrowTopMargin);
        mArrowDrawable.setBounds(left, top,
                (int) (left + mViewWidth * mViewScale),
                (int) (top + mArrowHeight * mViewScale));
        mArrowDrawable.setAlpha(mArrowAlpha);

        mArrowDrawable.draw(canvas);
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);

        if (animatorSet == null) {
            return;
        }
        if (View.GONE == visibility) {
            stopAnim();
        } else {
            startAnim();
        }
    }

    private void startAnim() {
        if (!animatorSet.isRunning()) {
            isAnimatorCancelled = false;
            animatorSet.start();
        }

        removeCallbacks(mRefreshProgressRunnable);
        mRefreshProgressRunnable = new RefreshProgressRunnable();
        post(mRefreshProgressRunnable);
    }

    private void stopAnim() {
        if (animatorSet.isRunning()) {
            isAnimatorCancelled = true;
            animatorSet.cancel();
        }

        removeCallbacks(mRefreshProgressRunnable);
    }

    private void initAnimator() {

        //渐入
        ValueAnimator animatorAlphaIn = ValueAnimator.ofInt(0, 255);
        animatorAlphaIn.setDuration(mAlphaInDuration);
        animatorAlphaIn.setInterpolator(null);
        animatorAlphaIn.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mArrowAlpha = (int) animation.getAnimatedValue();
            }
        });

        //渐出
        ValueAnimator animatorAlphaOut = ValueAnimator.ofInt(255, 0);
        animatorAlphaOut.setDuration(mAlphaOutDuration);
        //中间保持
        animatorAlphaOut.setStartDelay(mAlphaHoldOnDuration);
        animatorAlphaOut.setInterpolator(null);
        animatorAlphaOut.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mArrowAlpha = (int) animation.getAnimatedValue();
            }
        });

        ValueAnimator animatorY = ValueAnimator.ofInt(mTranslateRange, 0);
        animatorY.setDuration(mAlphaInDuration + mAlphaHoldOnDuration + mAlphaOutDuration);
        animatorY.setInterpolator(null);
        animatorY.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mArrowTopMargin = (int) animation.getAnimatedValue();
//                Log.e(TAG, "onAnimationUpdate:" + mArrowTopMargin);
            }
        });

        animatorSet = new AnimatorSet();

        AnimatorSet animatorSetAlpha = new AnimatorSet();
        animatorSetAlpha.play(animatorAlphaIn).before(animatorAlphaOut);

        animatorSet.play(animatorSetAlpha).with(animatorY);
        animatorSet.addListener(new AnimatorListenerAdapter() {

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!isAnimatorCancelled) {
                    animatorSet.start();
                }
            }

        });

        startAnim();
    }

    /**
     * 如果需要此箭头随着上滑，并且此View逐渐变小，而且动画仍然继续，
     * 设置此缩放比例
     *
     * @param scale 更新此View的缩放比例
     */
    public void updateScale(float scale) {
        this.mViewScale = scale;
    }

    private class RefreshProgressRunnable implements Runnable {

        @Override
        public void run() {
            synchronized (AnimateArrowView.this) {
                long start = System.currentTimeMillis();

                invalidate();

                long gap = 16 - (System.currentTimeMillis() - start);
                postDelayed(this, gap < 0 ? 0 : gap);
            }
        }
    }

}
