package com.jarvis.ai;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

public class OrbView extends View {

    public enum OrbState { IDLE, LISTENING, THINKING, SPEAKING, WAKE }

    private OrbState state = OrbState.IDLE;
    private float pulseRadius = 0f;
    private float pulseAlpha  = 0f;
    private float spinAngle   = 0f;

    private ValueAnimator pulseAnim;
    private ValueAnimator spinAnim;

    private static final int GOLD      = 0xFFC9A84C;
    private static final int GOLD_DIM  = 0xFF3A2E18;
    private static final int GOLD_MED  = 0xFF5C4A22;
    private static final int BG_DARK   = 0xFF0F0E0A;
    private static final int GOLD_FILL = 0xFF1A1508;

    private final Paint paintCore  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintRing1 = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintSpin  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintPulse = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintTri   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintDot   = new Paint(Paint.ANTI_ALIAS_FLAG);

    public OrbView(Context ctx) { super(ctx); init(); }
    public OrbView(Context ctx, AttributeSet a) { super(ctx, a); init(); }
    public OrbView(Context ctx, AttributeSet a, int s) { super(ctx, a, s); init(); }

    private void init() {
        paintCore.setStyle(Paint.Style.FILL);
        paintCore.setColor(GOLD_FILL);

        paintRing1.setStyle(Paint.Style.STROKE);
        paintRing1.setStrokeWidth(2f);
        paintRing1.setColor(GOLD);

        paintSpin.setStyle(Paint.Style.STROKE);
        paintSpin.setStrokeWidth(1.5f);
        paintSpin.setColor(GOLD_MED);
        paintSpin.setAlpha(160);

        paintPulse.setStyle(Paint.Style.STROKE);
        paintPulse.setStrokeWidth(2f);
        paintPulse.setColor(GOLD);

        paintTri.setStyle(Paint.Style.STROKE);
        paintTri.setStrokeWidth(2f);
        paintTri.setColor(GOLD);
        paintTri.setAlpha(220);

        paintDot.setStyle(Paint.Style.FILL);
        paintDot.setColor(GOLD);

        setState(OrbState.IDLE);
    }

    public void setState(OrbState newState) {
        this.state = newState;
        stopAnimations();
        switch (newState) {
            case LISTENING:
                paintCore.setColor(Color.parseColor("#1A1200"));
                paintRing1.setColor(GOLD);
                startPulse(1100);
                break;
            case THINKING:
                paintCore.setColor(Color.parseColor("#180E1A"));
                paintRing1.setColor(Color.parseColor("#8B6914"));
                startSpin(2000);
                break;
            case SPEAKING:
                paintCore.setColor(Color.parseColor("#0A1808"));
                paintRing1.setColor(Color.parseColor("#A8862E"));
                startSpin(1200);
                startPulse(800);
                break;
            case WAKE:
                paintCore.setColor(GOLD_FILL);
                paintRing1.setColor(GOLD);
                startPulse(2500);
                startSpin(7000);
                break;
            default:
                paintCore.setColor(GOLD_FILL);
                paintRing1.setColor(GOLD);
                break;
        }
        invalidate();
    }

    private void startPulse(long duration) {
        pulseAnim = ValueAnimator.ofFloat(0f, 1f);
        pulseAnim.setDuration(duration);
        pulseAnim.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnim.setInterpolator(new LinearInterpolator());
        pulseAnim.addUpdateListener(a -> {
            float t = (float) a.getAnimatedValue();
            pulseRadius = t;
            pulseAlpha  = (1f - t);
            invalidate();
        });
        pulseAnim.start();
    }

    private void startSpin(long duration) {
        spinAnim = ValueAnimator.ofFloat(0f, 360f);
        spinAnim.setDuration(duration);
        spinAnim.setRepeatCount(ValueAnimator.INFINITE);
        spinAnim.setInterpolator(new LinearInterpolator());
        spinAnim.addUpdateListener(a -> {
            spinAngle = (float) a.getAnimatedValue();
            invalidate();
        });
        spinAnim.start();
    }

    private void stopAnimations() {
        if (pulseAnim != null) { pulseAnim.cancel(); pulseAnim = null; }
        if (spinAnim  != null) { spinAnim.cancel();  spinAnim  = null; }
        pulseRadius = 0f; pulseAlpha = 0f; spinAngle = 0f;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float cx = getWidth()  / 2f;
        float cy = getHeight() / 2f;
        float r  = Math.min(cx, cy) * 0.90f;

        if (pulseAlpha > 0) {
            paintPulse.setAlpha((int)(pulseAlpha * 180));
            canvas.drawCircle(cx, cy, r * (0.80f + pulseRadius * 0.45f), paintPulse);
        }

        canvas.drawCircle(cx, cy, r * 0.88f, paintRing1);

        canvas.save();
        canvas.rotate(spinAngle, cx, cy);
        paintSpin.setPathEffect(new DashPathEffect(new float[]{10f, 8f}, 0f));
        canvas.drawCircle(cx, cy, r * 0.70f, paintSpin);
        canvas.restore();

        canvas.drawCircle(cx, cy, r * 0.55f, paintCore);

        float th = r * 0.38f;
        float tw = th * 1.10f;
        Path tri = new Path();
        tri.moveTo(cx,       cy - th * 0.80f);
        tri.lineTo(cx - tw,  cy + th * 0.60f);
        tri.lineTo(cx + tw,  cy + th * 0.60f);
        tri.close();
        canvas.drawPath(tri, paintTri);

        paintDot.setAlpha(200);
        canvas.drawCircle(cx, cy, r * 0.055f, paintDot);
    }
}
