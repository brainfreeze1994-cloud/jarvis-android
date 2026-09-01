package com.jarvis.ai;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Interactive Orbital Map for NASA Eyes on Asteroids & Asteroid Watch.
 * Renders Sun, Earth, celestial grid, and asteroid orbit paths with touch gestures (pan/zoom/select).
 */
public class AsteroidOrbitView extends View {

    public static class AsteroidOrbital {
        public String name;
        public double semiMajorAxis; // in AU (e.g. 1.1)
        public double eccentricity;   // e.g. 0.25
        public double inclinationDeg; // e.g. 12.0
        public double periodDays;    // orbital period in days
        public double currentAnomaly; // 0 to 2*PI
        public int color;
        public float sizeMeters;
        public String closeApproachDate;
        public double missDistanceKm;
        public double velocityKmh;
        public boolean isHazardous;
        public long approachTimestampMs;

        public AsteroidOrbital(String name, double a, double e, double inc, double period,
                               int color, float sizeMeters, String date, double missKm, double velKmh, boolean haz, long timestamp) {
            this.name = name;
            this.semiMajorAxis = a;
            this.eccentricity = e;
            this.inclinationDeg = inc;
            this.periodDays = period;
            this.currentAnomaly = (name.hashCode() % 360) * Math.PI / 180.0;
            this.color = color;
            this.sizeMeters = sizeMeters;
            this.closeApproachDate = date;
            this.missDistanceKm = missKm;
            this.velocityKmh = velKmh;
            this.isHazardous = haz;
            this.approachTimestampMs = timestamp;
        }
    }

    public interface OnAsteroidSelectedListener {
        void onAsteroidSelected(int index, AsteroidOrbital asteroid);
    }

    private final List<AsteroidOrbital> asteroids = new ArrayList<>();
    private int selectedIndex = 0;
    private OnAsteroidSelectedListener listener;

    private Paint bgPaint, gridPaint, sunPaint, earthPaint, orbitPaint, asteroidPaint, labelPaint, glowPaint;
    private float offsetX = 0f, offsetY = 0f;
    private float scale = 1.0f;
    private ScaleGestureDetector scaleDetector;
    private GestureDetector gestureDetector;
    private float lastTouchX, lastTouchY;
    private boolean isDragging = false;

    // Starfield
    private final float[] starX = new float[120];
    private final float[] starY = new float[120];
    private final float[] starAlpha = new float[120];
    private final float[] starSize = new float[120];

    private long lastFrameTime;
    private boolean isSimulating = true;
    private float simSpeed = 1.0f;

    public AsteroidOrbitView(Context context) {
        super(context);
        init(context);
    }

    public AsteroidOrbitView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public AsteroidOrbitView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        bgPaint = new Paint();
        bgPaint.setColor(0xFF020C1B);

        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(0x1800D4FF);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(1.2f);
        gridPaint.setPathEffect(new DashPathEffect(new float[]{6, 6}, 0));

        sunPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        sunPaint.setColor(0xFFFFCC00);
        sunPaint.setStyle(Paint.Style.FILL);

        earthPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        earthPaint.setColor(0xFF00D4FF);
        earthPaint.setStyle(Paint.Style.FILL);

        orbitPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        orbitPaint.setStyle(Paint.Style.STROKE);
        orbitPaint.setStrokeWidth(2f);

        asteroidPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        asteroidPaint.setStyle(Paint.Style.FILL);

        labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setColor(0xFF88BBDD);
        labelPaint.setTextSize(26f);
        labelPaint.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));

        glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        // Generate stars
        Random rnd = new Random(42);
        for (int i = 0; i < 120; i++) {
            starX[i] = rnd.nextFloat();
            starY[i] = rnd.nextFloat();
            starAlpha[i] = 0.2f + rnd.nextFloat() * 0.8f;
            starSize[i] = 1.0f + rnd.nextFloat() * 2.5f;
        }

        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                scale *= detector.getScaleFactor();
                scale = Math.max(0.4f, Math.min(scale, 3.5f));
                invalidate();
                return true;
            }
        });

        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                return checkTapSelection(e.getX(), e.getY());
            }
        });

        lastFrameTime = SystemClock.uptimeMillis();
    }

    public void setAsteroids(List<AsteroidOrbital> list) {
        asteroids.clear();
        if (list != null) {
            asteroids.addAll(list);
        }
        selectedIndex = 0;
        invalidate();
    }

    public List<AsteroidOrbital> getAsteroids() {
        return asteroids;
    }

    public void setSelectedIndex(int idx) {
        if (idx >= 0 && idx < asteroids.size()) {
            selectedIndex = idx;
            invalidate();
        }
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public void setOnAsteroidSelectedListener(OnAsteroidSelectedListener l) {
        this.listener = l;
    }

    public void setSimSpeed(float speed) {
        this.simSpeed = speed;
    }

    private boolean checkTapSelection(float tapX, float tapY) {
        float cx = getWidth() / 2f + offsetX;
        float cy = getHeight() * 0.42f + offsetY;
        float auPixels = Math.min(getWidth(), getHeight()) * 0.32f * scale;

        for (int i = 0; i < asteroids.size(); i++) {
            AsteroidOrbital ast = asteroids.get(i);
            float r = (float) (ast.semiMajorAxis * (1 - ast.eccentricity * ast.eccentricity) /
                    (1 + ast.eccentricity * Math.cos(ast.currentAnomaly))) * auPixels;
            float px = cx + (float) (r * Math.cos(ast.currentAnomaly));
            float py = cy + (float) (r * Math.sin(ast.currentAnomaly) * Math.cos(Math.toRadians(ast.inclinationDeg)));

            float dx = tapX - px;
            float dy = tapY - py;
            if (dx * dx + dy * dy < 48 * 48) {
                selectedIndex = i;
                if (listener != null) {
                    listener.onAsteroidSelected(i, ast);
                }
                invalidate();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchX = event.getX();
                lastTouchY = event.getY();
                isDragging = true;
                break;
            case MotionEvent.ACTION_MOVE:
                if (!scaleDetector.isInProgress() && isDragging) {
                    float dx = event.getX() - lastTouchX;
                    float dy = event.getY() - lastTouchY;
                    offsetX += dx;
                    offsetY += dy;
                    lastTouchX = event.getX();
                    lastTouchY = event.getY();
                    invalidate();
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isDragging = false;
                break;
        }
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        // Background
        canvas.drawRect(0, 0, w, h, bgPaint);

        // Draw Starfield
        Paint starPaint = new Paint();
        starPaint.setColor(0xFFFFFFFF);
        for (int i = 0; i < starX.length; i++) {
            int alpha = (int) (starAlpha[i] * 200);
            starPaint.setAlpha(alpha);
            canvas.drawCircle(starX[i] * w, starY[i] * h, starSize[i], starPaint);
        }

        // Center origin (Earth-Sun system)
        float cx = w / 2f + offsetX;
        float cy = h * 0.42f + offsetY;
        float auPixels = Math.min(w, h) * 0.32f * scale;

        // Distance Range Rings (0.5 AU, 1.0 AU Earth orbit, 1.5 AU Mars orbit)
        gridPaint.setColor(0x1800D4FF);
        canvas.drawCircle(cx, cy, 0.5f * auPixels, gridPaint);
        canvas.drawCircle(cx, cy, 1.5f * auPixels, gridPaint);

        // Sun in center
        glowPaint.setShader(new RadialGradient(cx, cy, 38 * scale, 0x88FFCC00, 0x00FFCC00, Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, 38 * scale, glowPaint);
        glowPaint.setShader(null);
        canvas.drawCircle(cx, cy, 12 * scale, sunPaint);

        // Label Sun
        labelPaint.setTextSize(20f * scale);
        labelPaint.setColor(0xFFFFDD88);
        canvas.drawText("SUN", cx - 18 * scale, cy + 28 * scale, labelPaint);

        // Earth Orbit (1.0 AU)
        orbitPaint.setColor(0x5500D4FF);
        orbitPaint.setStrokeWidth(2f * scale);
        canvas.drawCircle(cx, cy, 1.0f * auPixels, orbitPaint);

        // Earth Anomaly (time-based)
        long now = SystemClock.uptimeMillis();
        float dt = (now - lastFrameTime) / 1000f;
        lastFrameTime = now;
        if (dt > 0.1f) dt = 0.016f;

        double earthAnomaly = (now / 30000.0 * 2 * Math.PI) % (2 * Math.PI);
        float earthX = cx + (float) (auPixels * Math.cos(earthAnomaly));
        float earthY = cy + (float) (auPixels * Math.sin(earthAnomaly));

        // Earth Glow & Body
        glowPaint.setShader(new RadialGradient(earthX, earthY, 26 * scale, 0x9900D4FF, 0x0000D4FF, Shader.TileMode.CLAMP));
        canvas.drawCircle(earthX, earthY, 26 * scale, glowPaint);
        glowPaint.setShader(null);
        canvas.drawCircle(earthX, earthY, 8 * scale, earthPaint);

        // Earth Label
        labelPaint.setTextSize(22f * scale);
        labelPaint.setColor(0xFF00D4FF);
        canvas.drawText("EARTH", earthX + 12 * scale, earthY + 6 * scale, labelPaint);

        // Draw Asteroid Orbits & Positions
        for (int i = 0; i < asteroids.size(); i++) {
            AsteroidOrbital ast = asteroids.get(i);
            boolean isSelected = (i == selectedIndex);

            // Update simulation anomaly
            if (isSimulating) {
                double speed = (2 * Math.PI / (ast.periodDays * 0.05)) * dt * simSpeed;
                ast.currentAnomaly = (ast.currentAnomaly + speed) % (2 * Math.PI);
            }

            // Draw Asteroid Orbit Ellipse
            Path orbitPath = new Path();
            int steps = 72;
            for (int s = 0; s <= steps; s++) {
                double theta = (s / (double) steps) * 2 * Math.PI;
                float r = (float) (ast.semiMajorAxis * (1 - ast.eccentricity * ast.eccentricity) /
                        (1 + ast.eccentricity * Math.cos(theta))) * auPixels;
                float ox = cx + (float) (r * Math.cos(theta));
                float oy = cy + (float) (r * Math.sin(theta) * Math.cos(Math.toRadians(ast.inclinationDeg)));
                if (s == 0) orbitPath.moveTo(ox, oy);
                else orbitPath.lineTo(ox, oy);
            }

            orbitPaint.setColor(isSelected ? 0xFF00FF99 : 0x44FFFFFF);
            orbitPaint.setStrokeWidth(isSelected ? 2.5f * scale : 1.2f * scale);
            if (isSelected) {
                orbitPaint.setPathEffect(null);
            } else {
                orbitPaint.setPathEffect(new DashPathEffect(new float[]{8, 8}, 0));
            }
            canvas.drawPath(orbitPath, orbitPaint);
            orbitPaint.setPathEffect(null);

            // Asteroid current position
            float astR = (float) (ast.semiMajorAxis * (1 - ast.eccentricity * ast.eccentricity) /
                    (1 + ast.eccentricity * Math.cos(ast.currentAnomaly))) * auPixels;
            float ax = cx + (float) (astR * Math.cos(ast.currentAnomaly));
            float ay = cy + (float) (astR * Math.sin(ast.currentAnomaly) * Math.cos(Math.toRadians(ast.inclinationDeg)));

            // Draw connecting line to Earth if selected
            if (isSelected) {
                Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                linePaint.setColor(0x8800FF99);
                linePaint.setStrokeWidth(2f);
                linePaint.setPathEffect(new DashPathEffect(new float[]{10, 8}, 0));
                canvas.drawLine(earthX, earthY, ax, ay, linePaint);

                // Highlight Reticle around selected asteroid
                glowPaint.setShader(new RadialGradient(ax, ay, 32 * scale, 0x8800FF99, 0x0000FF99, Shader.TileMode.CLAMP));
                canvas.drawCircle(ax, ay, 32 * scale, glowPaint);
                glowPaint.setShader(null);

                Paint reticlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                reticlePaint.setColor(0xFF00FF99);
                reticlePaint.setStyle(Paint.Style.STROKE);
                reticlePaint.setStrokeWidth(2.5f);
                canvas.drawCircle(ax, ay, 18 * scale, reticlePaint);

                // Draw hexagon node
                drawHexagon(canvas, ax, ay, 10 * scale, reticlePaint);
            }

            // Asteroid Body
            asteroidPaint.setColor(ast.isHazardous ? 0xFFFF4444 : (isSelected ? 0xFF00FF99 : 0xFFE0E0E0));
            canvas.drawCircle(ax, ay, (isSelected ? 7f : 5f) * scale, asteroidPaint);

            // Asteroid Label
            labelPaint.setTextSize((isSelected ? 24f : 19f) * scale);
            labelPaint.setColor(isSelected ? 0xFF00FF99 : 0xFFAAAAAA);
            canvas.drawText(ast.name, ax + 14 * scale, ay - 6 * scale, labelPaint);
        }

        // Keep animated
        postInvalidateOnAnimation();
    }

    private void drawHexagon(Canvas canvas, float cx, float cy, float radius, Paint paint) {
        Path hex = new Path();
        for (int i = 0; i < 6; i++) {
            double angle = Math.toRadians(60 * i - 30);
            float hx = cx + (float) (radius * Math.cos(angle));
            float hy = cy + (float) (radius * Math.sin(angle));
            if (i == 0) hex.moveTo(hx, hy);
            else hex.lineTo(hx, hy);
        }
        hex.close();
        canvas.drawPath(hex, paint);
    }
}
