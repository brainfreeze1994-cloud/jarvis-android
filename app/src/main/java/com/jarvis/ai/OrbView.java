package com.jarvis.ai;

import android.animation.ArgbEvaluator;
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

    private OrbState state   = OrbState.IDLE;
    private float pulseRadius = 0f;
    private float pulseAlpha  = 0f;
    private float spinAngle   = 0f;

    private ValueAnimator pulseAnim;
    private ValueAnimator spinAnim;
    private ValueAnimator colorAnim;

    // ── Emotion colour palette ────────────────────────────────────────────────
    // Each emotion gets: accentColor (ring/triangle/dot), coreColor (fill), dimColor (spin ring)
    private int currentAccent  = 0xFFC9A84C;   // gold  — neutral default
    private int currentCore    = 0xFF1A1508;
    private int currentDim     = 0xFF5C4A22;

    // emotion → [accent, core, dim]
    private static int[][] EMOTION_PALETTE = {
        // neutral  — gold
        { 0xFFC9A84C, 0xFF1A1508, 0xFF5C4A22 },
        // warm     — soft rose
        { 0xFFE07898, 0xFF1A0812, 0xFF6B3045 },
        // concerned — amber-orange
        { 0xFFE09040, 0xFF1A0E04, 0xFF6B4018 },
        // excited  — electric cyan
        { 0xFF30D0E8, 0xFF041618, 0xFF185868 },
        // amused   — lime-green
        { 0xFF90CC30, 0xFF0A1204, 0xFF3A5210 },
        // serious  — crimson
        { 0xFFCC3030, 0xFF180404, 0xFF5C1010 },
        // proud    — violet
        { 0xFF9060D0, 0xFF100818, 0xFF3C2060 },
    };
    private static final String[] EMOTION_KEYS = {
        "neutral","warm","concerned","excited","amused","serious","proud"
    };

    private final Paint paintCore   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintRing1  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintSpin   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintPulse  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintTri    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintDot    = new Paint(Paint.ANTI_ALIAS_FLAG);

    public OrbView(Context ctx) { super(ctx); init(); }
    public OrbView(Context ctx, AttributeSet a) { super(ctx, a); init(); }
    public OrbView(Context ctx, AttributeSet a, int s) { super(ctx, a, s); init(); }

    private void init() {
        paintCore.setStyle(Paint.Style.FILL);
        paintCore.setColor(currentCore);

        paintRing1.setStyle(Paint.Style.STROKE);
        paintRing1.setStrokeWidth(2f);
        paintRing1.setColor(currentAccent);

        paintSpin.setStyle(Paint.Style.STROKE);
        paintSpin.setStrokeWidth(1.5f);
        paintSpin.setColor(currentDim);
        paintSpin.setAlpha(160);

        paintPulse.setStyle(Paint.Style.STROKE);
        paintPulse.setStrokeWidth(2f);
        paintPulse.setColor(currentAccent);

        paintTri.setStyle(Paint.Style.STROKE);
        paintTri.setStrokeWidth(2f);
        paintTri.setColor(currentAccent);
        paintTri.setAlpha(220);

        paintDot.setStyle(Paint.Style.FILL);
        paintDot.setColor(currentAccent);

        setState(OrbState.IDLE);
    }

    // ── Emotion colour change (animated) ─────────────────────────────────────
    public void setEmotion(String emotion) {
        int idx = 0;
        for (int i = 0; i < EMOTION_KEYS.length; i++) {
            if (EMOTION_KEYS[i].equalsIgnoreCase(emotion)) { idx = i; break; }
        }
        final int[] palette = EMOTION_PALETTE[idx];
        final int toAccent  = palette[0];
        final int toCore    = palette[1];
        final int toDim     = palette[2];

        if (colorAnim != null) colorAnim.cancel();
        colorAnim = ValueAnimator.ofFloat(0f, 1f);
        colorAnim.setDuration(600);
        final int fromAccent = currentAccent;
        final int fromCore   = currentCore;
        final int fromDim    = currentDim;
        colorAnim.addUpdateListener(a -> {
            float t = (float) a.getAnimatedValue();
            currentAccent = blendColor(fromAccent, toAccent, t);
            currentCore   = blendColor(fromCore,   toCore,   t);
            currentDim    = blendColor(fromDim,    toDim,    t);
            applyColors();
            invalidate();
        });
        colorAnim.start();
    }

    private int blendColor(int from, int to, float t) {
        int fa = (from >> 24) & 0xFF, fr = (from >> 16) & 0xFF,
            fg = (from >>  8) & 0xFF, fb =  from        & 0xFF;
        int ta = (to   >> 24) & 0xFF, tr = (to   >> 16) & 0xFF,
            tg = (to   >>  8) & 0xFF, tb =  to          & 0xFF;
        return Color.argb(
            (int)(fa + (ta - fa) * t),
            (int)(fr + (tr - fr) * t),
            (int)(fg + (tg - fg) * t),
            (int)(fb + (tb - fb) * t)
        );
    }

    private void applyColors() {
        paintCore.setColor(currentCore);
        paintRing1.setColor(currentAccent);
        paintSpin.setColor(currentDim);
        paintPulse.setColor(currentAccent);
        paintTri.setColor(currentAccent);
        paintDot.setColor(currentAccent);
    }

    // ── State (controls animation, not colour) ────────────────────────────────
    public void setState(OrbState newState) {
        this.state = newState;
        stopStateAnimations();

        switch (newState) {
            case LISTENING:
                startPulse(1100);
                break;
            case THINKING:
                startSpin(2000);
                break;
            case SPEAKING:
                startSpin(1200);
                startPulse(800);
                break;
            case WAKE:
                startPulse(2500);
                startSpin(7000);
                break;
            default: // IDLE — no animation
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

    private void stopStateAnimations() {
        if (pulseAnim != null) { pulseAnim.cancel(); pulseAnim = null; }
        if (spinAnim  != null) { spinAnim.cancel();  spinAnim  = null; }
        pulseRadius = 0f; pulseAlpha = 0f; spinAngle = 0f;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float cx = getWidth()  / 2f;
        float cy = getHeight() / 2f;
        float r  = Math.min(cx, cy) * 0.90f;

        // Pulse ring
        if (pulseAlpha > 0) {
            paintPulse.setAlpha((int)(pulseAlpha * 180));
            canvas.drawCircle(cx, cy, r * (0.80f + pulseRadius * 0.45f), paintPulse);
        }

        // Outer ring
        paintRing1.setAlpha(255);
        canvas.drawCircle(cx, cy, r * 0.88f, paintRing1);

        // Spinning dashed ring
        canvas.save();
        canvas.rotate(spinAngle, cx, cy);
        paintSpin.setAlpha(160);
        paintSpin.setPathEffect(new DashPathEffect(new float[]{10f, 8f}, 0f));
        canvas.drawCircle(cx, cy, r * 0.70f, paintSpin);
        canvas.restore();

        // Core fill circle
        canvas.drawCircle(cx, cy, r * 0.55f, paintCore);

        // Triangle in center
        float th = r * 0.38f;
        float tw = th * 1.10f;
        Path tri = new Path();
        tri.moveTo(cx,      cy - th * 0.80f);
        tri.lineTo(cx - tw, cy + th * 0.60f);
        tri.lineTo(cx + tw, cy + th * 0.60f);
        tri.close();
        paintTri.setAlpha(220);
        canvas.drawPath(tri, paintTri);

        // Center dot
        paintDot.setAlpha(200);
        canvas.drawCircle(cx, cy, r * 0.055f, paintDot);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopStateAnimations();
        if (colorAnim != null) { colorAnim.cancel(); colorAnim = null; }
    }
}
