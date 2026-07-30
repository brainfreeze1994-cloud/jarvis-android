package com.jarvis.ai;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
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

    private final Paint paintCore  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintRing1 = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintRing2 = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintSpin  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintPulse = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintInner = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintDot   = new Paint(Paint.ANTI_ALIAS_FLAG);

    public OrbView(Context ctx) { super(ctx); init(); }
    public OrbView(Context ctx, AttributeSet a) { super(ctx, a); init(); }
    public OrbView(Context ctx, AttributeSet a, int s) { super(ctx, a, s); init(); }

    private void init() {
        paintCore.setStyle(Paint.Style.FILL);
        paintRing1.setStyle(Paint.Style.STROKE); paintRing1.setStrokeWidth(2f); paintRing1.setColor(Color.parseColor("#1e3a5f"));
        paintRing2.setStyle(Paint.Style.STROKE); paintRing2.setStrokeWidth(2f); paintRing2.setColor(Color.parseColor("#1e3a5f"));
        paintSpin.setStyle(Paint.Style.STROKE);  paintSpin.setStrokeWidth(2f);  paintSpin.setColor(Color.parseColor("#3b82f6")); paintSpin.setAlpha(150);
        paintPulse.setStyle(Paint.Style.STROKE); paintPulse.setStrokeWidth(3f); paintPulse.setColor(Color.parseColor("#3b82f6"));
        paintInner.setStyle(Paint.Style.STROKE); paintInner.setStrokeWidth(2f); paintInner.setColor(Color.parseColor("#93c5fd")); paintInner.setAlpha(180);
        paintDot.setStyle(Paint.Style.FILL); paintDot.setColor(Color.parseColor("#bfdbfe"));
        setState(OrbState.IDLE);
    }

    public void setState(OrbState newState) {
        this.state = newState;
        stopAnimations();
        applyStateColor();
        switch (newState) {
            case LISTENING: startPulse(1200); break;
            case THINKING:  startSpin(2000); break;
            case SPEAKING:  startSpin(1200); startPulse(800); break;
            case WAKE:      startPulse(3000); startSpin(8000); break;
            default: break;
        }
        invalidate();
    }

    private void applyStateColor() {
        int color;
        switch (state) {
            case LISTENING: color = Color.parseColor("#2563eb"); break;
            case THINKING:  color = Color.parseColor("#7c3aed"); break;
            case SPEAKING:  color = Color.parseColor("#059669"); break;
            case WAKE:      color = Color.parseColor("#0f2a4a"); break;
            default:        color = Color.parseColor("#1d4ed8"); break;
        }
        paintCore.setColor(color);
    }

    private void startPulse(long duration) {
        pulseAnim = ValueAnimator.ofFloat(0f, 1f);
        pulseAnim.setDuration(duration);
        pulseAnim.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnim.setInterpolator(new LinearInterpolator());
        pulseAnim.addUpdateListener(a -> {
            float t = (float) a.getAnimatedValue();
            pulseRadius = t; pulseAlpha = 1f - t; invalidate();
        });
        pulseAnim.start();
    }

    private void startSpin(long duration) {
        spinAnim = ValueAnimator.ofFloat(0f, 360f);
        spinAnim.setDuration(duration);
        spinAnim.setRepeatCount(ValueAnimator.INFINITE);
        spinAnim.setInterpolator(new LinearInterpolator());
        spinAnim.addUpdateListener(a -> { spinAngle = (float) a.getAnimatedValue(); invalidate(); });
        spinAnim.start();
    }

    private void stopAnimations() {
        if (pulseAnim != null) { pulseAnim.cancel(); pulseAnim = null; }
        if (spinAnim  != null) { spinAnim.cancel();  spinAnim  = null; }
        pulseRadius = 0f; pulseAlpha = 0f; spinAngle = 0f;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float r  = Math.min(cx, cy) * 0.92f;

        if (pulseAlpha > 0) {
            paintPulse.setAlpha((int)(pulseAlpha * 200));
            canvas.drawCircle(cx, cy, r * (0.85f + pulseRadius * 0.4f), paintPulse);
        }
        canvas.drawCircle(cx, cy, r * 0.88f, paintRing1);
        canvas.save();
        canvas.rotate(spinAngle, cx, cy);
        paintSpin.setPathEffect(new DashPathEffect(new float[]{12f, 9f}, 0f));
        canvas.drawCircle(cx, cy, r * 0.72f, paintSpin);
        canvas.restore();
        canvas.drawCircle(cx, cy, r * 0.58f, paintRing2);
        paintRing2.setAlpha(90);
        float lo = r * 0.58f;
        canvas.drawLine(cx, cy - lo, cx, cy + lo, paintRing2);
        canvas.drawLine(cx - lo*0.866f, cy - lo*0.5f, cx + lo*0.866f, cy + lo*0.5f, paintRing2);
        canvas.drawLine(cx - lo*0.866f, cy + lo*0.5f, cx + lo*0.866f, cy - lo*0.5f, paintRing2);
        paintRing2.setAlpha(255);
        canvas.drawCircle(cx, cy, r * 0.42f, paintCore);
        canvas.drawCircle(cx, cy, r * 0.28f, paintInner);
        canvas.drawCircle(cx, cy, r * 0.09f, paintDot);
    }
}
