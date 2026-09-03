package com.jarvis.ai;

import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.OverScroller;
import androidx.core.view.ViewCompat;

/**
 * High-performance 2D bidirectional scroll view for the HENRY Periodic Matrix.
 * Allows effortless simultaneous horizontal, vertical, and diagonal panning and flinging.
 */
public class TwoDScrollView extends FrameLayout {
    private final OverScroller scroller;
    private final GestureDetector gestureDetector;
    private final int touchSlop;
    private boolean isDragging = false;
    private float lastTouchX;
    private float lastTouchY;

    public TwoDScrollView(Context context) {
        this(context, null);
    }

    public TwoDScrollView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TwoDScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        scroller = new OverScroller(context);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                if (!scroller.isFinished()) {
                    scroller.abortAnimation();
                }
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                scrollBy((int) distanceX, (int) distanceY);
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                int maxX = Math.max(0, getScrollRangeX());
                int maxY = Math.max(0, getScrollRangeY());
                scroller.fling(getScrollX(), getScrollY(), -(int) velocityX, -(int) velocityY, 0, maxX, 0, maxY);
                ViewCompat.postInvalidateOnAnimation(TwoDScrollView.this);
                return true;
            }
        });

        setHorizontalScrollBarEnabled(true);
        setVerticalScrollBarEnabled(true);
    }

    public int getScrollRangeX() {
        if (getChildCount() > 0) {
            View child = getChildAt(0);
            return Math.max(0, child.getWidth() - (getWidth() - getPaddingLeft() - getPaddingRight()));
        }
        return 0;
    }

    public int getScrollRangeY() {
        if (getChildCount() > 0) {
            View child = getChildAt(0);
            return Math.max(0, child.getHeight() - (getHeight() - getPaddingTop() - getPaddingBottom()));
        }
        return 0;
    }

    @Override
    public void scrollTo(int x, int y) {
        int maxX = getScrollRangeX();
        int maxY = getScrollRangeY();
        x = Math.max(0, Math.min(x, maxX));
        y = Math.max(0, Math.min(y, maxY));
        super.scrollTo(x, y);
    }

    @Override
    public void computeScroll() {
        if (scroller.computeScrollOffset()) {
            scrollTo(scroller.getCurrX(), scroller.getCurrY());
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                isDragging = false;
                lastTouchX = ev.getX();
                lastTouchY = ev.getY();
                gestureDetector.onTouchEvent(ev);
                break;
            case MotionEvent.ACTION_MOVE:
                float dx = Math.abs(ev.getX() - lastTouchX);
                float dy = Math.abs(ev.getY() - lastTouchY);
                if (dx > touchSlop || dy > touchSlop) {
                    isDragging = true;
                    // Request parent not to intercept touch
                    if (getParent() != null) {
                        getParent().requestDisallowInterceptTouchEvent(true);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isDragging = false;
                break;
        }
        return isDragging;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean handled = gestureDetector.onTouchEvent(event);
        if (event.getActionMasked() == MotionEvent.ACTION_UP && !scroller.isFinished()) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
        return handled || super.onTouchEvent(event);
    }

    @Override
    protected void measureChild(View child, int parentWidthMeasureSpec, int parentHeightMeasureSpec) {
        int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }

    @Override
    protected void measureChildWithMargins(View child, int parentWidthMeasureSpec, int widthUsed,
                                           int parentHeightMeasureSpec, int heightUsed) {
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) child.getLayoutParams();
        int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(lp.leftMargin + lp.rightMargin, MeasureSpec.UNSPECIFIED);
        int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(lp.topMargin + lp.bottomMargin, MeasureSpec.UNSPECIFIED);
        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }
}
