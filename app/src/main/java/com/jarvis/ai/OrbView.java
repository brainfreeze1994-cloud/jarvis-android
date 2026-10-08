package com.jarvis.ai;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
    // ════════════════════════════════════════════════════════════════════════
    // HENRY'S PORTRAIT — replaces the plain center-fill circle in every state below.
    // Landmark fractions (of the bundled henry_portrait.png, a 430x430 head-and-shoulders
    // crop) were measured once via face/eye/mouth detection on the source image — they are
    // fixed because the portrait itself is fixed. If the portrait image is ever replaced,
    // these four constants need re-measuring against the new image.
    // ════════════════════════════════════════════════════════════════════════
    private Bitmap portraitBitmap;
    private final Paint pPortrait = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private long lastBlinkStart = 0;
    private long nextBlinkAt = 1500;

    private static final float EYE1_CX = 0.5547f, EYE1_CY = 0.3384f;
    private static final float EYE2_CX = 0.4291f, EYE2_CY = 0.3547f;
    private static final float EYE_W = 0.085f, EYE_H = 0.050f;
    private static final float MOUTH_X = 0.4256f, MOUTH_Y = 0.4326f, MOUTH_W = 0.1791f, MOUTH_H = 0.0884f;
    private static final int BLINK_DURATION_MS = 140;

    private void ensurePortraitLoaded() {
        if (portraitBitmap == null) {
            try {
                portraitBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.henry_portrait);
            } catch (Throwable ignored) {}
        }
    }

    /** Draws HENRY's portrait in place of the old plain-color core fill, with a subtle
     *  always-on breathing pulse, a periodic blink, and — only while SPEAKING — a mouth-wobble
     *  effect built from the photo's own mouth pixels rather than a drawn-on shape, since the
     *  latter tends to look mismatched against a photo rather than alive. */
    private void drawPortraitCore(Canvas canvas, float cx, float cy, float r, float t, OrbState state) {
        ensurePortraitLoaded();
        if (portraitBitmap == null) {
            pFill.setColor(currentCore);
            canvas.drawCircle(cx, cy, r, pFill);
            return;
        }
        int bw = portraitBitmap.getWidth(), bh = portraitBitmap.getHeight();
        // Wall-clock based (not the shared 't' animator) so breathing/mouth-wobble frequency
        // is exact regardless of how the pre-existing ring/dash animations use 't'.
        float tSeconds = System.currentTimeMillis() / 1000f;
        float breathe = 1f + 0.015f * (float) Math.sin(tSeconds * Math.PI * 2 * 0.25);

        canvas.save();
        canvas.scale(breathe, breathe, cx, cy);

        // Circular-cropped base portrait, centered to fill the core radius.
        float left = cx - r, top = cy - r, scale = (2 * r) / Math.min(bw, bh);
        canvas.save();
        android.graphics.Path clip = new android.graphics.Path();
        clip.addCircle(cx, cy, r, android.graphics.Path.Direction.CW);
        canvas.clipPath(clip);
        android.graphics.RectF dst = new android.graphics.RectF(
                cx - bw * scale / 2f, cy - bh * scale / 2f, cx + bw * scale / 2f, cy + bh * scale / 2f);
        canvas.drawBitmap(portraitBitmap, null, dst, pPortrait);
        canvas.restore();

        // Mouth wobble while speaking — redraws the real mouth pixels, vertically squashed
        // and stretched on a sine wave, instead of overlaying an artificial mouth shape.
        if (state == OrbState.SPEAKING) {
            float mouthWobble = 0.7f + 0.55f * (float) Math.abs(Math.sin(tSeconds * Math.PI * 2 * 3.2));
            drawFacialRegionScaled(canvas, MOUTH_X, MOUTH_Y, MOUTH_W, MOUTH_H, bw, bh, left, top, scale, 1f, mouthWobble);
        }

        // Blink — periodic, both eyes, using the real eye pixels squashed thin.
        long now = System.currentTimeMillis();
        if (now > nextBlinkAt) {
            lastBlinkStart = now;
            nextBlinkAt = now + 2800 + (long) (Math.random() * 3200);
        }
        long sinceBlink = now - lastBlinkStart;
        if (sinceBlink >= 0 && sinceBlink < BLINK_DURATION_MS) {
            float phase = sinceBlink / (float) BLINK_DURATION_MS; // 0..1 across the blink
            float closeAmount = 1f - (float) Math.sin(phase * Math.PI); // 1 -> ~0 -> 1
            float eyeScaleY = Math.max(0.08f, closeAmount);
            drawFacialRegionScaled(canvas, EYE1_CX - EYE_W / 2, EYE1_CY - EYE_H / 2, EYE_W, EYE_H, bw, bh, left, top, scale, 1f, eyeScaleY);
            drawFacialRegionScaled(canvas, EYE2_CX - EYE_W / 2, EYE2_CY - EYE_H / 2, EYE_W, EYE_H, bw, bh, left, top, scale, 1f, eyeScaleY);
        }

        canvas.restore();
    }

    /** Redraws one rectangular region of the source bitmap (given as fractions of bitmap size)
     *  at its same display position, but scaled vertically/horizontally around its own center —
     *  the technique behind both the mouth wobble and the blink, reusing real photo pixels
     *  instead of drawing a synthetic shape on top. */
    private void drawFacialRegionScaled(Canvas canvas, float fx, float fy, float fw, float fh,
                                         int bw, int bh, float dstLeft, float dstTop, float scale,
                                         float scaleX, float scaleY) {
        android.graphics.Rect src = new android.graphics.Rect(
                (int) (fx * bw), (int) (fy * bh), (int) ((fx + fw) * bw), (int) ((fy + fh) * bh));
        float cx2 = dstLeft + (src.left + src.right) / 2f * scale;
        float cy2 = dstTop + (src.top + src.bottom) / 2f * scale;
        float halfW = (src.right - src.left) / 2f * scale * scaleX;
        float halfH = (src.bottom - src.top) / 2f * scale * scaleY;
        android.graphics.RectF dst = new android.graphics.RectF(cx2 - halfW, cy2 - halfH, cx2 + halfW, cy2 + halfH);
        canvas.drawBitmap(portraitBitmap, src, dst, pPortrait);
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

        drawPortraitCore(canvas, cx, cy, r * 0.35f, t, state);
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

        drawPortraitCore(canvas, cx, cy, r * 0.36f, t, state);
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

        drawPortraitCore(canvas, cx, cy, r * 0.35f, t, state);

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

        drawPortraitCore(canvas, cx, cy, r * 0.35f, t, state);
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

        int savedAlpha = pPortrait.getAlpha();
        pPortrait.setAlpha((int) (120 + pulse * 135));
        drawPortraitCore(canvas, cx, cy, r * 0.35f, t, state);
        pPortrait.setAlpha(savedAlpha);
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
