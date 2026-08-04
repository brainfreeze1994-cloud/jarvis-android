package com.jarvis.ai;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

/**
 * Scrolling HUD ticker — "◈ H.E.N.R.Y ONLINE ◈ ALL SYSTEMS OPERATIONAL …"
 */
public class HudTickerView extends View {

    private static final String TEXT =
        "◈ H.E.N.R.Y ONLINE   ◈ ALL SYSTEMS OPERATIONAL   " +
        "◈ STARK INDUSTRIES   ◈ NEURAL CORE ACTIVE   " +
        "◈ REASONING ENGINE READY   ◈ VOICE INTERFACE STANDING BY   " +
        "◈ COMPOUND-BETA SEARCH ENABLED   ◈ LIVE DATA CONNECTED   ";

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float offsetX = 0f;
    private float textWidth = 0f;
    private ValueAnimator anim;

    public HudTickerView(Context ctx) { super(ctx); init(); }
    public HudTickerView(Context ctx, AttributeSet a) { super(ctx, a); init(); }
    public HudTickerView(Context ctx, AttributeSet a, int s) { super(ctx, a, s); init(); }

    private void init() {
        paint.setTextSize(20f);   // pixels — scaled in onSizeChanged
        paint.setColor(0xFF1E4A66);
        paint.setLetterSpacing(0.14f);
        setLayerType(LAYER_TYPE_HARDWARE, null);
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        // Scale text to ~38% of view height
        paint.setTextSize(Math.max(18f, h * 0.55f));
        paint.setLetterSpacing(0.14f);
        textWidth = paint.measureText(TEXT);
        startScroll(w);
    }

    private void startScroll(float viewWidth) {
        if (anim != null) anim.cancel();
        float totalDistance = viewWidth + textWidth;
        long duration = (long)(totalDistance / 55f * 1000L); // 55 px/sec
        anim = ValueAnimator.ofFloat(viewWidth, -textWidth);
        anim.setDuration(Math.max(duration, 12000));
        anim.setRepeatCount(ValueAnimator.INFINITE);
        anim.setInterpolator(new LinearInterpolator());
        anim.addUpdateListener(a -> { offsetX = (float) a.getAnimatedValue(); invalidate(); });
        anim.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float y = getHeight() * 0.78f;
        canvas.drawText(TEXT, offsetX, y, paint);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (anim != null) { anim.cancel(); anim = null; }
    }
}
