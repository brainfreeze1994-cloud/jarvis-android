package com.jarvis.ai;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

/**
 * H.E.N.R.Y OrbView v18 — Iron Man HUD animated orb.
 *
 * IDLE      — blue rotating dashed rings, tick marks, steady glow
 * LISTENING — gold audio bar animation bursting around the circle
 * THINKING  — purple orbiting dots + 3 spinning dashed rings
 * SPEAKING  — green ripple waves + waveform bars
 * WAKE      — dim slow pulsing
 */
public class OrbView extends View {

    public enum OrbState { IDLE, LISTENING, THINKING, SPEAKING, WAKE }

    private OrbState state = OrbState.IDLE;

    // Continuous time animator (drives every animation in onDraw)
    private ValueAnimator timeAnim;
    private float time = 0f; // 0 → 1000, loops infinitely

    // Emotion colour transition
    private ValueAnimator colorAnim;
    private int currentAccent = 0xFF00D4FF;
    private int currentCore   = 0xFF020C1B;
    private int currentDim    = 0xFF004466;

    private static final int[][] EMOTION_PALETTE = {
        { 0xFF00D4FF, 0xFF020C1B, 0xFF004466 }, // neutral  — electric blue
        { 0xFF40E0FF, 0xFF011520, 0xFF006680 }, // warm     — soft cyan
        { 0xFFE09040, 0xFF1A0E04, 0xFF6B4018 }, // concerned— amber
        { 0xFF80DFFF, 0xFF021A28, 0xFF0088BB }, // excited  — bright blue
        { 0xFF00E5CC, 0xFF001A18, 0xFF007060 }, // amused   — teal
        { 0xFFCC3030, 0xFF180404, 0xFF5C1010 }, // serious  — red
        { 0xFF6080FF, 0xFF080818, 0xFF203070 }, // proud    — violet
    };
    private static final String[] EMOTION_KEYS = {
        "neutral","warm","concerned","excited","amused","serious","proud"
    };

    // Shared paints
    private final Paint pStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pFill   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pDash   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pGlow   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pBar    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pTick   = new Paint(Paint.ANTI_ALIAS_FLAG);

    public OrbView(Context ctx) { super(ctx); init(); }
    public OrbView(Context ctx, AttributeSet a) { super(ctx, a); init(); }
    public OrbView(Context ctx, AttributeSet a, int s) { super(ctx, a, s); init(); }

    private void init() {
        pStroke.setStyle(Paint.Style.STROKE);
        pStroke.setStrokeWidth(2f);
        pFill.setStyle(Paint.Style.FILL);
        pDash.setStyle(Paint.Style.STROKE);
        pDash.setStrokeWidth(1.2f);
        pGlow.setStyle(Paint.Style.FILL);
        pBar.setStyle(Paint.Style.STROKE);
        pBar.setStrokeWidth(2.8f);
        pBar.setStrokeCap(Paint.Cap.ROUND);
        pTick.setStyle(Paint.Style.STROKE);
        pTick.setStrokeWidth(1.2f);
        pTick.setStrokeCap(Paint.Cap.SQUARE);
        startTimeAnimator();
    }

    // ── Time loop (drives all animations) ────────────────────────────────────
    private void startTimeAnimator() {
        if (timeAnim != null) timeAnim.cancel();
        timeAnim = ValueAnimator.ofFloat(0f, 1000f);
        timeAnim.setDuration(16000);
        timeAnim.setRepeatCount(ValueAnimator.INFINITE);
        timeAnim.setInterpolator(new LinearInterpolator());
        timeAnim.addUpdateListener(a -> {
            time = (float) a.getAnimatedValue();
            invalidate();
        });
        timeAnim.start();
    }

    // ── Public API ────────────────────────────────────────────────────────────
    public void setState(OrbState newState) {
        this.state = newState;
        invalidate();
    }

    public void setEmotion(String emotion) {
        int idx = 0;
        for (int i = 0; i < EMOTION_KEYS.length; i++) {
            if (EMOTION_KEYS[i].equalsIgnoreCase(emotion)) { idx = i; break; }
        }
        final int[] pal = EMOTION_PALETTE[idx];
        final int fromA = currentAccent, fromC = currentCore, fromD = currentDim;
        final int toA   = pal[0],        toC   = pal[1],      toD   = pal[2];
        if (colorAnim != null) colorAnim.cancel();
        colorAnim = ValueAnimator.ofFloat(0f, 1f);
        colorAnim.setDuration(600);
        colorAnim.addUpdateListener(a -> {
            float v = (float) a.getAnimatedValue();
            currentAccent = blend(fromA, toA, v);
            currentCore   = blend(fromC, toC, v);
            currentDim    = blend(fromD, toD, v);
            invalidate();
        });
        colorAnim.start();
    }

    // ── Draw dispatcher ───────────────────────────────────────────────────────
    @Override
    protected void onDraw(Canvas canvas) {
        float cx = getWidth()  / 2f;
        float cy = getHeight() / 2f;
        float r  = Math.min(cx, cy) * 0.86f;
        float t  = time / 1000f; // normalised continuous

        switch (state) {
            case LISTENING: drawListening(canvas, cx, cy, r, t); break;
            case THINKING:  drawThinking (canvas, cx, cy, r, t); break;
            case SPEAKING:  drawSpeaking (canvas, cx, cy, r, t); break;
            case WAKE:      drawWake     (canvas, cx, cy, r, t); break;
            default:        drawIdle     (canvas, cx, cy, r, t); break;
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // IDLE — blue rotating dashed rings + tick marks + steady glow
    // ════════════════════════════════════════════════════════════════════════
    private void drawIdle(Canvas canvas, float cx, float cy, float r, float t) {
        float pulse = (float)(0.5 + 0.5 * Math.sin(t * Math.PI * 2 * 0.8));

        // Outer dashed rotating ring 1
        canvas.save();
        canvas.rotate(t * 360f * 0.25f, cx, cy);
        pDash.setColor(alpha(currentAccent, 55));
        pDash.setPathEffect(new DashPathEffect(new float[]{10f, 14f}, 0));
        canvas.drawCircle(cx, cy, r * 1.18f, pDash);
        canvas.restore();

        // Outer dashed rotating ring 2 (opposite direction)
        canvas.save();
        canvas.rotate(-t * 360f * 0.15f, cx, cy);
        pDash.setColor(alpha(currentAccent, 35));
        pDash.setPathEffect(new DashPathEffect(new float[]{5f, 20f}, 0));
        canvas.drawCircle(cx, cy, r * 1.02f, pDash);
        canvas.restore();
        pDash.setPathEffect(null);

        // Tick marks on outer ring
        for (int i = 0; i < 24; i++) {
            float angle = (float)(i / 24.0 * Math.PI * 2);
            float len   = i % 6 == 0 ? r * 0.10f : r * 0.05f;
            float innerR = r * 0.88f;
            pTick.setColor(alpha(currentAccent, i % 6 == 0 ? 110 : 45));
            pTick.setStrokeWidth(i % 6 == 0 ? 1.5f : 1f);
            canvas.drawLine(
                cx + (float) Math.cos(angle) * innerR,
                cy + (float) Math.sin(angle) * innerR,
                cx + (float) Math.cos(angle) * (innerR + len),
                cy + (float) Math.sin(angle) * (innerR + len),
                pTick
            );
        }

        // Concentric rings
        pStroke.setStrokeWidth(1.8f);
        pStroke.setColor(alpha(currentAccent, 180));
        canvas.drawCircle(cx, cy, r * 0.88f, pStroke);
        pStroke.setColor(alpha(currentAccent, 80));
        canvas.drawCircle(cx, cy, r * 0.65f, pStroke);
        pStroke.setColor(alpha(currentAccent, 95));
        canvas.drawCircle(cx, cy, r * 0.40f, pStroke);

        // Core glow
        drawGlow(canvas, cx, cy, r * 0.40f, currentAccent, (int)(80 + 40 * pulse));

        // Core fill
        pFill.setColor(currentCore);
        canvas.drawCircle(cx, cy, r * 0.35f, pFill);

        // Center dot
        pFill.setColor(alpha(0xFFFFFFFF, (int)(190 + 65 * pulse)));
        canvas.drawCircle(cx, cy, r * 0.07f, pFill);
    }

    // ════════════════════════════════════════════════════════════════════════
    // LISTENING — GOLD + audio bar burst around circle
    // ════════════════════════════════════════════════════════════════════════
    private void drawListening(Canvas canvas, float cx, float cy, float r, float t) {
        final int GOLD = 0xFFC9A84C;

        // Expanding pulse rings
        for (int i = 0; i < 3; i++) {
            float phase = ((t * 2f + i * 0.33f) % 1f);
            float rr = r * (1.05f + phase * 0.75f);
            pStroke.setColor(alpha(GOLD, (int)((1f - phase) * 145)));
            pStroke.setStrokeWidth(1.5f);
            canvas.drawCircle(cx, cy, rr, pStroke);
        }

        // Audio bars around circle (fake, animated)
        int numBars = 64;
        for (int i = 0; i < numBars; i++) {
            float angle = (float)(i / (double) numBars * Math.PI * 2 - Math.PI / 2);
            double val = 0.15 + 0.4 * Math.abs(
                Math.sin(t * Math.PI * 2 * 4 + i * 0.25)
                * Math.cos(t * Math.PI * 2 * 2 + i * 0.1)
            );
            float innerR = r * 1.08f;
            float outerR = innerR + (float)(val * r * 0.75f);
            pBar.setColor(alpha(0xFFC9A84C, Math.min(255, (int)(85 + val * 170))));
            canvas.drawLine(
                cx + (float) Math.cos(angle) * innerR,
                cy + (float) Math.sin(angle) * innerR,
                cx + (float) Math.cos(angle) * outerR,
                cy + (float) Math.sin(angle) * outerR,
                pBar
            );
        }

        // Rings — gold
        pStroke.setStrokeWidth(2f);
        pStroke.setColor(alpha(GOLD, 220));
        canvas.drawCircle(cx, cy, r * 0.88f, pStroke);
        pStroke.setColor(alpha(GOLD, 120));
        canvas.drawCircle(cx, cy, r * 0.65f, pStroke);
        pStroke.setColor(alpha(GOLD, 155));
        canvas.drawCircle(cx, cy, r * 0.42f, pStroke);

        drawGlow(canvas, cx, cy, r * 0.42f, GOLD, 190);

        pFill.setColor(currentCore);
        canvas.drawCircle(cx, cy, r * 0.36f, pFill);

        pFill.setColor(0xFFFFE980);
        canvas.drawCircle(cx, cy, r * 0.08f, pFill);
    }

    // ════════════════════════════════════════════════════════════════════════
    // THINKING — PURPLE + 3 spinning rings + 6 orbiting dots
    // ════════════════════════════════════════════════════════════════════════
    private void drawThinking(Canvas canvas, float cx, float cy, float r, float t) {
        final int PURPLE = 0xFF8B5CF6;
        final int LIGHT  = 0xFFA78BFA;

        // 3 spinning dashed rings at different speeds
        float[] speeds  = { 1.2f, -0.7f,  0.4f };
        float[] radii   = { 1.50f, 1.30f, 1.10f };
        float[][] dashes= { {8,10}, {5,15}, {3,20} };
        for (int i = 0; i < 3; i++) {
            canvas.save();
            canvas.rotate(t * 360f * speeds[i], cx, cy);
            pDash.setColor(alpha(PURPLE, 110));
            pDash.setStrokeWidth(1.2f);
            pDash.setPathEffect(new DashPathEffect(
                new float[]{ dashes[i][0], dashes[i][1] }, 0));
            canvas.drawCircle(cx, cy, r * radii[i], pDash);
            canvas.restore();
        }
        pDash.setPathEffect(null);

        // 6 orbiting dots
        for (int i = 0; i < 6; i++) {
            float angle = (float)(i / 6.0 * Math.PI * 2 + t * Math.PI * 2 * 2.5);
            float dx = cx + (float) Math.cos(angle) * r * 1.15f;
            float dy = cy + (float) Math.sin(angle) * r * 1.15f;
            double a2 = 0.5 + 0.5 * Math.sin(t * Math.PI * 2 * 4 + i);
            pFill.setColor(alpha(LIGHT, (int)(120 + 135 * a2)));
            canvas.drawCircle(dx, dy, 4.5f, pFill);
        }

        // Rings
        pStroke.setStrokeWidth(2f);
        pStroke.setColor(alpha(PURPLE, 200));
        canvas.drawCircle(cx, cy, r * 0.88f, pStroke);
        pStroke.setColor(alpha(PURPLE, 105));
        canvas.drawCircle(cx, cy, r * 0.65f, pStroke);
        pStroke.setColor(alpha(PURPLE, 130));
        canvas.drawCircle(cx, cy, r * 0.42f, pStroke);

        drawGlow(canvas, cx, cy, r * 0.40f, PURPLE, 165);

        pFill.setColor(currentCore);
        canvas.drawCircle(cx, cy, r * 0.35f, pFill);

        // Spinning inner cross
        canvas.save();
        canvas.translate(cx, cy);
        canvas.rotate(t * 360f * 3f);
        pStroke.setColor(alpha(0xFFD4C8FF, 110));
        pStroke.setStrokeWidth(1f);
        canvas.drawLine(-r * 0.25f, 0, r * 0.25f, 0, pStroke);
        canvas.drawLine(0, -r * 0.25f, 0, r * 0.25f, pStroke);
        canvas.restore();
    }

    // ════════════════════════════════════════════════════════════════════════
    // SPEAKING — GREEN + outward ripple waves + waveform bars
    // ════════════════════════════════════════════════════════════════════════
    private void drawSpeaking(Canvas canvas, float cx, float cy, float r, float t) {
        final int GREEN      = 0xFF16A34A;
        final int GREEN_LITE = 0xFF4ADE80;

        // Outward ripple rings
        for (int i = 0; i < 4; i++) {
            float phase = ((t * 1.8f + i * 0.25f) % 1f);
            float rr = r * (1.05f + phase * 1.05f);
            pStroke.setColor(alpha(GREEN, (int)((1f - phase) * 110)));
            pStroke.setStrokeWidth(1.5f);
            canvas.drawCircle(cx, cy, rr, pStroke);
        }

        // Waveform bars (animated to speech rhythm)
        int numBars = 48;
        for (int i = 0; i < numBars; i++) {
            float angle = (float)(i / (double) numBars * Math.PI * 2 - Math.PI / 2);
            double val = 0.2 + 0.55 * Math.abs(
                Math.sin(t * Math.PI * 2 * 9 + i * 0.5)
                * Math.cos(t * Math.PI * 2 * 3.5 + i * 0.2)
            );
            float innerR = r * 1.07f;
            float outerR = innerR + (float)(val * r * 0.68f);
            pBar.setColor(alpha(GREEN_LITE, Math.min(255, (int)(85 + val * 165))));
            canvas.drawLine(
                cx + (float) Math.cos(angle) * innerR,
                cy + (float) Math.sin(angle) * innerR,
                cx + (float) Math.cos(angle) * outerR,
                cy + (float) Math.sin(angle) * outerR,
                pBar
            );
        }

        // Rings
        pStroke.setStrokeWidth(2f);
        pStroke.setColor(alpha(GREEN, 220));
        canvas.drawCircle(cx, cy, r * 0.88f, pStroke);
        pStroke.setColor(alpha(GREEN, 110));
        canvas.drawCircle(cx, cy, r * 0.65f, pStroke);
        pStroke.setColor(alpha(GREEN, 150));
        canvas.drawCircle(cx, cy, r * 0.42f, pStroke);

        drawGlow(canvas, cx, cy, r * 0.42f, GREEN, 175);

        pFill.setColor(currentCore);
        canvas.drawCircle(cx, cy, r * 0.35f, pFill);

        pFill.setColor(0xFFA7F3D0);
        canvas.drawCircle(cx, cy, r * 0.08f, pFill);
    }

    // ════════════════════════════════════════════════════════════════════════
    // WAKE — dim slow pulse
    // ════════════════════════════════════════════════════════════════════════
    private void drawWake(Canvas canvas, float cx, float cy, float r, float t) {
        float pulse = (float)(0.2 + 0.1 * Math.sin(t * Math.PI * 2 * 0.4));
        pStroke.setStrokeWidth(1.5f);
        pStroke.setColor(alpha(currentAccent, (int)(pulse * 255)));
        canvas.drawCircle(cx, cy, r * 0.88f, pStroke);
        canvas.drawCircle(cx, cy, r * 0.65f, pStroke);
        drawGlow(canvas, cx, cy, r * 0.30f, currentAccent, (int)(pulse * 0.4f * 255));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private void drawGlow(Canvas canvas, float cx, float cy, float r, int color, int alpha) {
        int red = Color.red(color), g = Color.green(color), b = Color.blue(color);
        int c0 = Color.argb(Math.min(255, alpha), red, g, b);
        int c1 = Color.argb(0, red, g, b);
        RadialGradient rg = new RadialGradient(cx, cy, r, c0, c1, Shader.TileMode.CLAMP);
        pGlow.setShader(rg);
        canvas.drawCircle(cx, cy, r, pGlow);
        pGlow.setShader(null);
    }

    private int alpha(int color, int a) {
        return (color & 0x00FFFFFF) | (Math.min(255, Math.max(0, a)) << 24);
    }

    private int blend(int from, int to, float t) {
        int fa=(from>>24)&0xFF, fr=(from>>16)&0xFF, fg=(from>>8)&0xFF, fb=from&0xFF;
        int ta=(to  >>24)&0xFF, tr=(to  >>16)&0xFF, tg=(to  >>8)&0xFF, tb=to  &0xFF;
        return Color.argb(
            (int)(fa+(ta-fa)*t),(int)(fr+(tr-fr)*t),
            (int)(fg+(tg-fg)*t),(int)(fb+(tb-fb)*t));
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (timeAnim  != null) { timeAnim.cancel();  timeAnim  = null; }
        if (colorAnim != null) { colorAnim.cancel(); colorAnim = null; }
    }
}
