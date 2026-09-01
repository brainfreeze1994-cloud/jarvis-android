package com.jarvis.ai;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Holographic Workshop Background matching Tony Stark's Iron Man 2 workshop holographic displays.
 * Renders glowing vertical laser beams, dynamic perspective floor grid, scanning HUD particles,
 * and ambient cyan/teal holographic nodes.
 */
public class HolographicWorkshopBackgroundView extends View {

    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint beamPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint particlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint scanlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint vignettePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final List<HoloParticle> particles = new ArrayList<>();
    private final List<HoloBeam> beams = new ArrayList<>();
    private final Random random = new Random();

    private float scanlineY = 0f;
    private long lastTime = System.currentTimeMillis();

    private static class HoloParticle {
        float x, y, size, speedY, alpha;
        int color;
    }

    private static class HoloBeam {
        float x, width, alpha, speedAlpha;
        boolean increasing;
    }

    public HolographicWorkshopBackgroundView(Context context) {
        super(context);
        init();
    }

    public HolographicWorkshopBackgroundView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        gridPaint.setColor(0x1800E5FF);
        gridPaint.setStrokeWidth(1.2f);
        gridPaint.setStyle(Paint.Style.STROKE);

        beamPaint.setStyle(Paint.Style.FILL);
        particlePaint.setStyle(Paint.Style.FILL);
        scanlinePaint.setStyle(Paint.Style.STROKE);
        scanlinePaint.setStrokeWidth(2f);
        scanlinePaint.setColor(0x2200FFAA);

        // Spawn particles
        for (int i = 0; i < 35; i++) {
            HoloParticle p = new HoloParticle();
            p.x = random.nextFloat();
            p.y = random.nextFloat();
            p.size = 2f + random.nextFloat() * 4f;
            p.speedY = 0.03f + random.nextFloat() * 0.07f;
            p.alpha = 0.2f + random.nextFloat() * 0.6f;
            p.color = (random.nextBoolean()) ? 0xFF00FFCC : 0xFF00D4FF;
            particles.add(p);
        }

        // Spawn holographic vertical light beams (like in Iron Man 2 workshop)
        for (int i = 0; i < 6; i++) {
            HoloBeam b = new HoloBeam();
            b.x = 0.08f + i * 0.16f + (random.nextFloat() * 0.04f - 0.02f);
            b.width = 15f + random.nextFloat() * 25f;
            b.alpha = 0.04f + random.nextFloat() * 0.08f;
            b.speedAlpha = 0.005f + random.nextFloat() * 0.01f;
            b.increasing = random.nextBoolean();
            beams.add(b);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        long now = System.currentTimeMillis();
        float dt = (now - lastTime) / 1000f;
        lastTime = now;
        if (dt > 0.1f) dt = 0.016f;

        // Base gradient: Deep space navy to Stark workshop dark cyan
        vignettePaint.setShader(new LinearGradient(0, 0, 0, h,
                new int[]{0xFF040A12, 0xFF061424, 0xFF03080F},
                new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h, vignettePaint);

        // 1. Draw Vertical Holographic Projector Light Beams (Iron Man 2 style)
        for (HoloBeam b : beams) {
            if (b.increasing) {
                b.alpha += b.speedAlpha * dt * 20;
                if (b.alpha >= 0.14f) b.increasing = false;
            } else {
                b.alpha -= b.speedAlpha * dt * 20;
                if (b.alpha <= 0.02f) b.increasing = true;
            }

            float beamX = b.x * w;
            int beamColor = Color.argb((int)(b.alpha * 255), 0, 230, 255);
            beamPaint.setShader(new LinearGradient(beamX, 0, beamX, h,
                    new int[]{0x0000E5FF, beamColor, 0x0000E5FF},
                    new float[]{0f, 0.45f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawRect(beamX - b.width / 2f, 0, beamX + b.width / 2f, h, beamPaint);
        }

        // 2. Draw Holographic Isometric/Perspective Grid (Floor & Floating planes)
        float gridSpacing = 48f;
        for (float x = 0; x < w; x += gridSpacing) {
            canvas.drawLine(x, 0, x, h, gridPaint);
        }
        for (float y = 0; y < h; y += gridSpacing) {
            canvas.drawLine(0, y, w, y, gridPaint);
        }

        // 3. Floating Holographic Data Particles
        for (HoloParticle p : particles) {
            p.y -= p.speedY * dt;
            if (p.y < 0) {
                p.y = 1f;
                p.x = random.nextFloat();
            }

            int alphaInt = (int)(p.alpha * 255 * (0.8f + 0.2f * (float)Math.sin(now * 0.003f + p.x * 10)));
            particlePaint.setColor((p.color & 0x00FFFFFF) | (alphaInt << 24));
            canvas.drawCircle(p.x * w, p.y * h, p.size, particlePaint);

            // Subtle connecting tether lines for cluster effect
            if (p.size > 4f) {
                gridPaint.setColor(0x1200FFAA);
                canvas.drawLine(p.x * w, p.y * h, p.x * w + 20, p.y * h - 20, gridPaint);
            }
        }

        // 4. Moving HUD Scanning Line
        scanlineY += h * 0.15f * dt;
        if (scanlineY > h) scanlineY = 0f;
        scanlinePaint.setShader(new LinearGradient(0, scanlineY, w, scanlineY,
                new int[]{0x0000FFCC, 0x4400FFCC, 0x0000FFCC},
                new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawLine(0, scanlineY, w, scanlineY, scanlinePaint);

        // Continuous redraw for smooth 60fps holographic animation
        postInvalidateOnAnimation();
    }
}
