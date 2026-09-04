package com.jarvis.ai;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * High-tech H.E.N.R.Y Facial Recognition & Biometric Scanner Animated View.
 * Renders holographic scanning brackets, dynamic 3D face mesh, laser sweep,
 * rotating radar telemetry, and biometric node triangulation.
 */
public class FacialBiometricScanView extends View {

    public static final int STATE_IDLE = 0;
    public static final int STATE_SCANNING = 1;
    public static final int STATE_SUCCESS = 2;
    public static final int STATE_ERROR = 3;

    public static final int MODE_FACE = 0;
    public static final int MODE_FINGERPRINT = 1;

    // Colors
    private static final int COLOR_PRIMARY_CYAN = 0xFF00D4FF;
    private static final int COLOR_NEON_TEAL    = 0xFF00FFCC;
    private static final int COLOR_SUCCESS_GREEN= 0xFF00E676;
    private static final int COLOR_ERROR_RED    = 0xFFFF3366;
    private static final int COLOR_GRID_CYAN    = 0x3300D4FF;

    private int currentState = STATE_SCANNING;
    private int currentMode = MODE_FACE;

    // Paints
    private Paint paintBracket;
    private Paint paintLaser;
    private Paint paintLaserGlow;
    private Paint paintRadar;
    private Paint paintMeshLine;
    private Paint paintMeshNode;
    private Paint paintText;
    private Paint paintCircle;

    // Animators
    private ValueAnimator laserAnimator;
    private ValueAnimator radarAnimator;
    private ValueAnimator pulseAnimator;
    private ValueAnimator shakeAnimator;

    private float laserY = 0f;
    private float radarAngle = 0f;
    private float pulseRadius = 0f;
    private float shakeOffset = 0f;

    // Face mesh landmarks (normalized 0..1 relative to target rect)
    private final List<PointF> facePoints = new ArrayList<>();
    private final List<int[]> faceConnections = new ArrayList<>();

    public FacialBiometricScanView(Context context) {
        super(context);
        init();
    }

    public FacialBiometricScanView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FacialBiometricScanView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paintBracket = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintBracket.setStyle(Paint.Style.STROKE);
        paintBracket.setStrokeWidth(5f);
        paintBracket.setStrokeCap(Paint.Cap.ROUND);
        paintBracket.setColor(COLOR_PRIMARY_CYAN);

        paintLaser = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintLaser.setStyle(Paint.Style.STROKE);
        paintLaser.setStrokeWidth(4f);
        paintLaser.setColor(COLOR_NEON_TEAL);

        paintLaserGlow = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintLaserGlow.setStyle(Paint.Style.FILL);

        paintRadar = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintRadar.setStyle(Paint.Style.STROKE);
        paintRadar.setStrokeWidth(2f);
        paintRadar.setColor(0x5500D4FF);

        paintMeshLine = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintMeshLine.setStyle(Paint.Style.STROKE);
        paintMeshLine.setStrokeWidth(1.8f);
        paintMeshLine.setColor(COLOR_GRID_CYAN);

        paintMeshNode = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintMeshNode.setStyle(Paint.Style.FILL);
        paintMeshNode.setColor(COLOR_NEON_TEAL);

        paintText = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setTextSize(26f);
        paintText.setLetterSpacing(0.15f);
        paintText.setColor(COLOR_NEON_TEAL);

        paintCircle = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintCircle.setStyle(Paint.Style.STROKE);
        paintCircle.setStrokeWidth(2f);
        paintCircle.setColor(0x4000D4FF);

        initDefaultFaceMesh();
        setupAnimators();
    }

    private void initDefaultFaceMesh() {
        facePoints.clear();
        faceConnections.clear();

        // Normalized landmarks [0.0 - 1.0] inside scanner box
        // 0: Forehead top
        facePoints.add(new PointF(0.50f, 0.22f));
        // 1, 2: Left/Right Temples
        facePoints.add(new PointF(0.28f, 0.30f));
        facePoints.add(new PointF(0.72f, 0.30f));
        // 3, 4: Left/Right Eyebrows
        facePoints.add(new PointF(0.38f, 0.35f));
        facePoints.add(new PointF(0.62f, 0.35f));
        // 5, 6: Left/Right Eye centers
        facePoints.add(new PointF(0.38f, 0.42f));
        facePoints.add(new PointF(0.62f, 0.42f));
        // 7: Nose Bridge
        facePoints.add(new PointF(0.50f, 0.46f));
        // 8: Nose Tip
        facePoints.add(new PointF(0.50f, 0.56f));
        // 9, 10: Left/Right Nostrils
        facePoints.add(new PointF(0.44f, 0.58f));
        facePoints.add(new PointF(0.56f, 0.58f));
        // 11, 12: Mouth corners
        facePoints.add(new PointF(0.38f, 0.68f));
        facePoints.add(new PointF(0.62f, 0.68f));
        // 13: Upper lip center
        facePoints.add(new PointF(0.50f, 0.66f));
        // 14: Lower lip center
        facePoints.add(new PointF(0.50f, 0.72f));
        // 15, 16: Left/Right Jawline
        facePoints.add(new PointF(0.25f, 0.55f));
        facePoints.add(new PointF(0.75f, 0.55f));
        // 17, 18: Lower Jaw
        facePoints.add(new PointF(0.32f, 0.72f));
        facePoints.add(new PointF(0.68f, 0.72f));
        // 19: Chin
        facePoints.add(new PointF(0.50f, 0.82f));

        // Connect landmarks into neural mesh
        connect(0, 1); connect(0, 2); connect(1, 3); connect(2, 4);
        connect(3, 5); connect(4, 6); connect(3, 7); connect(4, 7);
        connect(5, 7); connect(6, 7); connect(7, 8); connect(8, 9);
        connect(8, 10); connect(9, 13); connect(10, 13);
        connect(11, 13); connect(12, 13); connect(11, 14); connect(12, 14);
        connect(1, 15); connect(2, 16); connect(15, 17); connect(16, 18);
        connect(17, 19); connect(18, 19); connect(14, 19);
        connect(5, 15); connect(6, 16); connect(9, 11); connect(10, 12);
    }

    private void connect(int i, int j) {
        faceConnections.add(new int[]{i, j});
    }

    private void setupAnimators() {
        // Laser sweep animator (up and down)
        laserAnimator = ValueAnimator.ofFloat(0f, 1f);
        laserAnimator.setDuration(1800);
        laserAnimator.setRepeatMode(ValueAnimator.REVERSE);
        laserAnimator.setRepeatCount(ValueAnimator.INFINITE);
        laserAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        laserAnimator.addUpdateListener(anim -> {
            laserY = (float) anim.getAnimatedValue();
            invalidate();
        });

        // Radar spin animator
        radarAnimator = ValueAnimator.ofFloat(0f, 360f);
        radarAnimator.setDuration(3200);
        radarAnimator.setRepeatCount(ValueAnimator.INFINITE);
        radarAnimator.setInterpolator(new LinearInterpolator());
        radarAnimator.addUpdateListener(anim -> {
            radarAngle = (float) anim.getAnimatedValue();
            invalidate();
        });

        // Pulse wave expanding animator
        pulseAnimator = ValueAnimator.ofFloat(0f, 1f);
        pulseAnimator.setDuration(1500);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        pulseAnimator.addUpdateListener(anim -> {
            pulseRadius = (float) anim.getAnimatedValue();
            invalidate();
        });

        laserAnimator.start();
        radarAnimator.start();
        pulseAnimator.start();
    }

    public void setScannerMode(int mode) {
        this.currentMode = mode;
        invalidate();
    }

    public void setScannerState(int state) {
        this.currentState = state;
        switch (state) {
            case STATE_SUCCESS:
                paintBracket.setColor(COLOR_SUCCESS_GREEN);
                paintLaser.setColor(COLOR_SUCCESS_GREEN);
                paintMeshNode.setColor(COLOR_SUCCESS_GREEN);
                paintText.setColor(COLOR_SUCCESS_GREEN);
                paintMeshLine.setColor(0x6600E676);
                break;
            case STATE_ERROR:
                paintBracket.setColor(COLOR_ERROR_RED);
                paintLaser.setColor(COLOR_ERROR_RED);
                paintMeshNode.setColor(COLOR_ERROR_RED);
                paintText.setColor(COLOR_ERROR_RED);
                paintMeshLine.setColor(0x66FF3366);
                triggerShake();
                break;
            case STATE_SCANNING:
            case STATE_IDLE:
            default:
                paintBracket.setColor(COLOR_PRIMARY_CYAN);
                paintLaser.setColor(COLOR_NEON_TEAL);
                paintMeshNode.setColor(COLOR_NEON_TEAL);
                paintText.setColor(COLOR_NEON_TEAL);
                paintMeshLine.setColor(COLOR_GRID_CYAN);
                break;
        }
        invalidate();
    }

    private void triggerShake() {
        if (shakeAnimator != null && shakeAnimator.isRunning()) shakeAnimator.cancel();
        shakeAnimator = ValueAnimator.ofFloat(0f, 18f, -18f, 12f, -12f, 6f, -6f, 0f);
        shakeAnimator.setDuration(450);
        shakeAnimator.addUpdateListener(anim -> {
            shakeOffset = (float) anim.getAnimatedValue();
            invalidate();
        });
        shakeAnimator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) return;

        canvas.save();
        if (shakeOffset != 0f) {
            canvas.translate(shakeOffset, 0);
        }

        float cx = width / 2f;
        float cy = height / 2f;
        float size = Math.min(width, height) * 0.85f;
        float half = size / 2f;
        RectF targetRect = new RectF(cx - half, cy - half, cx + half, cy + half);

        // 1. Draw outer radar circles & pulse rings
        drawRadarCircles(canvas, cx, cy, half);

        // 2. Draw 4 Cybernetic Corner Brackets
        drawCornerBrackets(canvas, targetRect);

        // 3. Draw biometric content based on mode (Face mesh or Fingerprint arches)
        if (currentMode == MODE_FACE) {
            drawFaceMesh(canvas, targetRect);
        } else {
            drawFingerprintMesh(canvas, targetRect);
        }

        // 4. Draw moving scanning laser and glowing fan
        if (currentState == STATE_SCANNING || currentState == STATE_IDLE) {
            drawScanningLaser(canvas, targetRect);
        } else if (currentState == STATE_SUCCESS) {
            drawSuccessOverlay(canvas, targetRect);
        } else if (currentState == STATE_ERROR) {
            drawErrorOverlay(canvas, targetRect);
        }

        canvas.restore();
    }

    private void drawRadarCircles(Canvas canvas, float cx, float cy, float maxRadius) {
        // Outer guide circle
        paintCircle.setColor(0x2200D4FF);
        canvas.drawCircle(cx, cy, maxRadius, paintCircle);
        canvas.drawCircle(cx, cy, maxRadius * 0.72f, paintCircle);

        // Rotating radar sweep arc
        if (currentState == STATE_SCANNING) {
            paintRadar.setColor(0x3300FFCC);
            canvas.save();
            canvas.rotate(radarAngle, cx, cy);
            RectF arcRect = new RectF(cx - maxRadius, cy - maxRadius, cx + maxRadius, cy + maxRadius);
            canvas.drawArc(arcRect, 0, 45, false, paintRadar);
            canvas.drawArc(arcRect, 180, 45, false, paintRadar);
            canvas.restore();

            // Expanding pulse wave
            float curPulse = maxRadius * pulseRadius;
            int alpha = (int) (120 * (1f - pulseRadius));
            paintCircle.setColor(Color.argb(alpha, 0, 212, 255));
            canvas.drawCircle(cx, cy, curPulse, paintCircle);
        }
    }

    private void drawCornerBrackets(Canvas canvas, RectF r) {
        float arm = r.width() * 0.18f;
        float pad = 4f;

        // Top-Left
        canvas.drawLine(r.left + pad, r.top + pad, r.left + pad + arm, r.top + pad, paintBracket);
        canvas.drawLine(r.left + pad, r.top + pad, r.left + pad, r.top + pad + arm, paintBracket);

        // Top-Right
        canvas.drawLine(r.right - pad, r.top + pad, r.right - pad - arm, r.top + pad, paintBracket);
        canvas.drawLine(r.right - pad, r.top + pad, r.right - pad, r.top + pad + arm, paintBracket);

        // Bottom-Left
        canvas.drawLine(r.left + pad, r.bottom - pad, r.left + pad + arm, r.bottom - pad, paintBracket);
        canvas.drawLine(r.left + pad, r.bottom - pad, r.left + pad, r.bottom - pad - arm, paintBracket);

        // Bottom-Right
        canvas.drawLine(r.right - pad, r.bottom - pad, r.right - pad - arm, r.bottom - pad, paintBracket);
        canvas.drawLine(r.right - pad, r.bottom - pad, r.right - pad, r.bottom - pad - arm, paintBracket);

        // Ticks along edges
        float mx = r.centerX();
        float my = r.centerY();
        canvas.drawLine(mx - 8f, r.top, mx + 8f, r.top, paintBracket);
        canvas.drawLine(mx - 8f, r.bottom, mx + 8f, r.bottom, paintBracket);
        canvas.drawLine(r.left, my - 8f, r.left, my + 8f, paintBracket);
        canvas.drawLine(r.right, my - 8f, r.right, my + 8f, paintBracket);
    }

    private void drawFaceMesh(Canvas canvas, RectF r) {
        float w = r.width();
        float h = r.height();

        // 1. Draw connecting mesh wireframe
        for (int[] conn : faceConnections) {
            PointF p1 = facePoints.get(conn[0]);
            PointF p2 = facePoints.get(conn[1]);
            float x1 = r.left + p1.x * w;
            float y1 = r.top + p1.y * h;
            float x2 = r.left + p2.x * w;
            float y2 = r.top + p2.y * h;
            canvas.drawLine(x1, y1, x2, y2, paintMeshLine);
        }

        // 2. Draw eye reticles with subtle iris crosshair
        PointF leftEye = facePoints.get(5);
        PointF rightEye = facePoints.get(6);
        float lx = r.left + leftEye.x * w;
        float ly = r.top + leftEye.y * h;
        float rx = r.left + rightEye.x * w;
        float ry = r.top + rightEye.y * h;

        float eyeRadius = w * 0.045f;
        canvas.drawCircle(lx, ly, eyeRadius, paintBracket);
        canvas.drawCircle(rx, ry, eyeRadius, paintBracket);
        canvas.drawLine(lx - eyeRadius - 4, ly, lx + eyeRadius + 4, ly, paintMeshLine);
        canvas.drawLine(rx - eyeRadius - 4, ry, rx + eyeRadius + 4, ry, paintMeshLine);

        // 3. Draw biometric nodes
        for (int i = 0; i < facePoints.size(); i++) {
            PointF p = facePoints.get(i);
            float px = r.left + p.x * w;
            float py = r.top + p.y * h;
            float nodeRadius = (i == 0 || i == 5 || i == 6 || i == 8 || i == 19) ? 4.5f : 3.0f;
            canvas.drawCircle(px, py, nodeRadius, paintMeshNode);
        }
    }

    private void drawFingerprintMesh(Canvas canvas, RectF r) {
        float cx = r.centerX();
        float cy = r.centerY();
        float step = r.width() * 0.065f;

        Paint fpPaint = new Paint(paintBracket);
        fpPaint.setStrokeWidth(3.5f);
        fpPaint.setColor(currentState == STATE_SUCCESS ? COLOR_SUCCESS_GREEN :
                         currentState == STATE_ERROR ? COLOR_ERROR_RED : 0xBB00D4FF);

        for (int i = 1; i <= 6; i++) {
            float radX = step * i;
            float radY = step * (i * 1.25f);
            RectF oval = new RectF(cx - radX, cy - radY, cx + radX, cy + radY);
            canvas.drawArc(oval, 180 + (i % 2 == 0 ? 15 : 0), 180 - (i % 2 == 0 ? 30 : 0), false, fpPaint);
        }
    }

    private void drawScanningLaser(Canvas canvas, RectF r) {
        float curY = r.top + (r.height() * laserY);

        // Gradient glow fan trailing the laser
        LinearGradient glowShader = new LinearGradient(
                r.centerX(), curY - 35f,
                r.centerX(), curY + 35f,
                new int[]{0x0000D4FF, 0x4400FFCC, 0x0000D4FF},
                new float[]{0f, 0.5f, 1f},
                Shader.TileMode.CLAMP
        );
        paintLaserGlow.setShader(glowShader);
        canvas.drawRect(r.left + 8f, curY - 35f, r.right - 8f, curY + 35f, paintLaserGlow);

        // Sharp neon laser bar
        canvas.drawLine(r.left + 6f, curY, r.right - 6f, curY, paintLaser);
    }

    private void drawSuccessOverlay(Canvas canvas, RectF r) {
        // Draw green verified ring
        paintBracket.setColor(COLOR_SUCCESS_GREEN);
        canvas.drawCircle(r.centerX(), r.centerY(), r.width() * 0.38f, paintBracket);

        // Draw verified checkmark
        Path checkPath = new Path();
        float cx = r.centerX();
        float cy = r.centerY();
        float s = r.width() * 0.12f;
        checkPath.moveTo(cx - s, cy);
        checkPath.lineTo(cx - (s * 0.3f), cy + (s * 0.7f));
        checkPath.lineTo(cx + s, cy - (s * 0.7f));

        Paint checkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        checkPaint.setStyle(Paint.Style.STROKE);
        checkPaint.setStrokeWidth(6.5f);
        checkPaint.setStrokeCap(Paint.Cap.ROUND);
        checkPaint.setStrokeJoin(Paint.Join.ROUND);
        checkPaint.setColor(COLOR_SUCCESS_GREEN);
        canvas.drawPath(checkPath, checkPaint);
    }

    private void drawErrorOverlay(Canvas canvas, RectF r) {
        // Draw red exclamation or X
        float cx = r.centerX();
        float cy = r.centerY();
        float s = r.width() * 0.14f;

        Paint xPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        xPaint.setStyle(Paint.Style.STROKE);
        xPaint.setStrokeWidth(6.5f);
        xPaint.setStrokeCap(Paint.Cap.ROUND);
        xPaint.setColor(COLOR_ERROR_RED);

        canvas.drawLine(cx - s, cy - s, cx + s, cy + s, xPaint);
        canvas.drawLine(cx + s, cy - s, cx - s, cy + s, xPaint);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (laserAnimator != null) laserAnimator.cancel();
        if (radarAnimator != null) radarAnimator.cancel();
        if (pulseAnimator != null) pulseAnimator.cancel();
        if (shakeAnimator != null) shakeAnimator.cancel();
    }
}
