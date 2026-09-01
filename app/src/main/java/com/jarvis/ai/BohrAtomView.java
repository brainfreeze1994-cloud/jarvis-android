package com.jarvis.ai;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Animated Bohr Model Atomic Visualizer.
 * Renders the element's nucleus with glowing proton/neutron cluster and
 * concentric electron shells (K, L, M, N, O, P, Q) with revolving glowing electrons.
 */
public class BohrAtomView extends View {

    private final Paint nucleusPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint electronPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private String symbol = "Fe";
    private int atomicNumber = 26;
    private int[] shellElectrons = new int[]{2, 8, 14, 2}; // default Iron
    private int themeColor = 0xFF00FFCC;

    private float baseAngle = 0f;
    private long lastTime = System.currentTimeMillis();

    public BohrAtomView(Context context) {
        super(context);
        init();
    }

    public BohrAtomView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(1.5f);
        ringPaint.setColor(0x3300FFCC);

        electronPaint.setStyle(Paint.Style.FILL);
        electronPaint.setColor(0xFF00FFFF);

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
        textPaint.setColor(Color.WHITE);
    }

    /**
     * Sets element configuration to render correct orbital shells.
     */
    public void setElement(String symbol, int atomicNumber, int color) {
        this.symbol = symbol;
        this.atomicNumber = atomicNumber;
        this.themeColor = color;
        this.shellElectrons = calculateShellDistribution(atomicNumber);
        invalidate();
    }

    /**
     * Standard electron shell capacities (K=2, L=8, M=18, N=32, etc.)
     */
    private int[] calculateShellDistribution(int z) {
        if (z <= 0) return new int[]{1};
        // Simplified standard shell filling approximation for Bohr visualizer
        int[] maxCap = {2, 8, 18, 32, 32, 18, 8};
        List<Integer> shells = new ArrayList<>();
        int remaining = z;
        for (int cap : maxCap) {
            if (remaining <= 0) break;
            int take = Math.min(remaining, cap);
            shells.add(take);
            remaining -= take;
        }
        int[] res = new int[shells.size()];
        for (int i = 0; i < shells.size(); i++) res[i] = shells.get(i);
        return res;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        float cx = w / 2f;
        float cy = h / 2f;
        float maxRadius = Math.min(cx, cy) - 24f;
        if (maxRadius < 30) maxRadius = 30;

        long now = System.currentTimeMillis();
        float dt = (now - lastTime) / 1000f;
        lastTime = now;
        if (dt > 0.1f) dt = 0.016f;

        baseAngle += 40f * dt;
        if (baseAngle > 360000f) baseAngle = 0f;

        // 1. Draw glowing Nucleus
        float nucleusRadius = Math.max(16f, maxRadius * 0.18f);
        glowPaint.setShader(new RadialGradient(cx, cy, nucleusRadius * 2.2f,
                new int[]{(themeColor & 0x00FFFFFF) | 0x88000000, (themeColor & 0x00FFFFFF) | 0x22000000, 0x00000000},
                new float[]{0f, 0.6f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, nucleusRadius * 2.2f, glowPaint);

        nucleusPaint.setColor(themeColor);
        nucleusPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(cx, cy, nucleusRadius, nucleusPaint);

        // Center Symbol & Z Number
        textPaint.setTextSize(nucleusRadius * 0.9f);
        textPaint.setColor(0xFF001520);
        canvas.drawText(symbol, cx, cy + (nucleusRadius * 0.32f), textPaint);

        // 2. Draw Concentric Shell Rings & Revolving Electrons
        int numShells = Math.max(1, shellElectrons.length);
        float shellStep = (maxRadius - nucleusRadius - 10f) / numShells;

        char[] shellLabels = {'K', 'L', 'M', 'N', 'O', 'P', 'Q'};

        for (int i = 0; i < numShells; i++) {
            float r = nucleusRadius + 14f + (i + 1) * shellStep;
            int count = shellElectrons[i];

            // Shell Ring
            ringPaint.setColor((themeColor & 0x00FFFFFF) | 0x3A000000);
            canvas.drawCircle(cx, cy, r, ringPaint);

            // Shell Label (K, L, M...)
            char label = (i < shellLabels.length) ? shellLabels[i] : (char)('K' + i);
            textPaint.setColor(0x88AADDFF);
            textPaint.setTextSize(18f);
            canvas.drawText(label + ":" + count + "e⁻", cx + r + 2f, cy - 4f, textPaint);

            // Electrons on this shell
            float shellSpeed = (i % 2 == 0 ? 1f : -0.8f) * (1.2f / (i + 1));
            float shellAngleOffset = baseAngle * shellSpeed;

            float angleStep = 360f / Math.max(1, count);
            for (int e = 0; e < count; e++) {
                double rad = Math.toRadians(shellAngleOffset + e * angleStep);
                float ex = cx + (float)(r * Math.cos(rad));
                float ey = cy + (float)(r * Math.sin(rad));

                // Glowing electron dot
                glowPaint.setShader(new RadialGradient(ex, ey, 8f,
                        new int[]{0xFFFFFFFF, 0xFF00FFCC, 0x0000FFCC},
                        new float[]{0f, 0.4f, 1f}, Shader.TileMode.CLAMP));
                canvas.drawCircle(ex, ey, 8f, glowPaint);

                electronPaint.setColor(0xFFFFFFFF);
                canvas.drawCircle(ex, ey, 3.5f, electronPaint);
            }
        }

        postInvalidateOnAnimation();
    }
}
