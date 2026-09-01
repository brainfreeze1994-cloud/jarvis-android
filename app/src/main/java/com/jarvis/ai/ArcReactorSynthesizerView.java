package com.jarvis.ai;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Tony Stark Particle Laser Synthesizer & Arc Reactor Core Canvas.
 * Renders prism lasers focusing on a heavy atomic core, plasma arcs,
 * energy containment rings, and synthesis spark bursts.
 */
public class ArcReactorSynthesizerView extends View {

    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint laserPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint corePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sparkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float rotationAngle = 0f;
    private float laserPulse = 0f;
    private boolean isSynthesizing = false;
    private float synthProgress = 0f;
    private String synthesizedElement = "BADASSIUM (NEW ELEMENT #118+)";

    private final List<Spark> sparks = new ArrayList<>();
    private final Random random = new Random();
    private long lastTime = System.currentTimeMillis();

    private static class Spark {
        float x, y, vx, vy, life, maxLife, size;
        int color;
    }

    public ArcReactorSynthesizerView(Context context) {
        super(context);
        init();
    }

    public ArcReactorSynthesizerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(2f);

        laserPaint.setStyle(Paint.Style.STROKE);
        laserPaint.setStrokeWidth(4f);

        sparkPaint.setStyle(Paint.Style.FILL);

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
        textPaint.setColor(0xFF00FFCC);
    }

    public void startSynthesis(String elementName, Runnable onComplete) {
        this.synthesizedElement = elementName;
        this.isSynthesizing = true;
        this.synthProgress = 0f;
        invalidate();
    }

    public void setSynthesisProgress(float progress) {
        this.synthProgress = progress;
        if (progress >= 1f) {
            this.isSynthesizing = false;
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        float cx = w / 2f;
        float cy = h / 2f;
        float radius = Math.min(cx, cy) - 20f;
        if (radius < 40) radius = 40;

        long now = System.currentTimeMillis();
        float dt = (now - lastTime) / 1000f;
        lastTime = now;
        if (dt > 0.1f) dt = 0.016f;

        rotationAngle += (isSynthesizing ? 90f : 25f) * dt;
        laserPulse = (float) Math.sin(now * 0.008f);

        // 1. Arc Reactor Outer Housing & Segmented Rings
        ringPaint.setColor(0x3300D4FF);
        ringPaint.setStrokeWidth(3f);
        canvas.drawCircle(cx, cy, radius, ringPaint);

        ringPaint.setColor(0x2200FFAA);
        canvas.drawCircle(cx, cy, radius * 0.8f, ringPaint);

        // Rotating Reactor Coils (10 coils like Tony's Mark VI chest core)
        int coils = 10;
        float coilRadius = radius * 0.88f;
        for (int i = 0; i < coils; i++) {
            double angle = Math.toRadians(rotationAngle + i * (360f / coils));
            float px = cx + (float)(coilRadius * Math.cos(angle));
            float py = cy + (float)(coilRadius * Math.sin(angle));

            ringPaint.setColor(isSynthesizing ? 0xFF00FFAA : 0xAA00D4FF);
            ringPaint.setStrokeWidth(5f);
            canvas.drawCircle(px, py, radius * 0.06f, ringPaint);
        }

        // 2. Focused Laser Beams converging into Core (Iron Man 2 Laser synthesis)
        int beamCount = isSynthesizing ? 6 : 4;
        for (int i = 0; i < beamCount; i++) {
            double beamAngle = Math.toRadians(i * (360f / beamCount) + (isSynthesizing ? rotationAngle * 0.5f : 0));
            float startX = cx + (float)(radius * Math.cos(beamAngle));
            float startY = cy + (float)(radius * Math.sin(beamAngle));

            laserPaint.setStrokeWidth(isSynthesizing ? (4f + laserPulse * 2f) : 2.5f);
            laserPaint.setColor(isSynthesizing ? 0xFF00FFCC : 0x8800D4FF);
            canvas.drawLine(startX, startY, cx, cy, laserPaint);

            // Inner focus glow line
            laserPaint.setStrokeWidth(1.2f);
            laserPaint.setColor(0xFFFFFFFF);
            canvas.drawLine(startX, startY, cx, cy, laserPaint);
        }

        // 3. Central Core Plasma & Fusion Glow
        float coreRadius = radius * (isSynthesizing ? (0.28f + laserPulse * 0.05f) : 0.22f);
        corePaint.setShader(new RadialGradient(cx, cy, coreRadius * 2.5f,
                new int[]{0xFFFFFFFF, isSynthesizing ? 0xFF00FF88 : 0xFF00D4FF, 0x00000000},
                new float[]{0f, 0.4f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, coreRadius * 2.5f, corePaint);

        corePaint.setShader(null);
        corePaint.setColor(0xFFFFFFFF);
        canvas.drawCircle(cx, cy, coreRadius * 0.5f, corePaint);

        // 4. Spark Particle Explosion during Synthesis
        if (isSynthesizing) {
            for (int i = 0; i < 4; i++) {
                Spark sp = new Spark();
                sp.x = cx;
                sp.y = cy;
                double spAngle = random.nextDouble() * 2 * Math.PI;
                float spSpeed = 80f + random.nextFloat() * 160f;
                sp.vx = (float)(spSpeed * Math.cos(spAngle));
                sp.vy = (float)(spSpeed * Math.sin(spAngle));
                sp.life = 0f;
                sp.maxLife = 0.3f + random.nextFloat() * 0.4f;
                sp.size = 2f + random.nextFloat() * 4f;
                sp.color = random.nextBoolean() ? 0xFF00FFCC : 0xFFFFFFFF;
                sparks.add(sp);
            }
        }

        for (int i = sparks.size() - 1; i >= 0; i--) {
            Spark sp = sparks.get(i);
            sp.life += dt;
            if (sp.life >= sp.maxLife) {
                sparks.remove(i);
                continue;
            }
            sp.x += sp.vx * dt;
            sp.y += sp.vy * dt;
            float alpha = 1f - (sp.life / sp.maxLife);
            sparkPaint.setColor((sp.color & 0x00FFFFFF) | ((int)(alpha * 255) << 24));
            canvas.drawCircle(sp.x, sp.y, sp.size * alpha, sparkPaint);
        }

        // 5. HUD Readout Overlay
        textPaint.setTextSize(22f);
        textPaint.setColor(isSynthesizing ? 0xFF00FFAA : 0xFF00E5FF);
        String status = isSynthesizing ? ("SYNTHESIZING • " + (int)(synthProgress * 100) + "%") : "CORE STATUS: STABLE • 3.8 GJ/s";
        canvas.drawText(status, cx, cy + radius * 0.65f, textPaint);

        postInvalidateOnAnimation();
    }
}
