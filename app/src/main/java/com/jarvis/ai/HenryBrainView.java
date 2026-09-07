package com.jarvis.ai;

import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * HenryBrainView — standalone custom View for the HENRY Brain Map canvas.
 * Renders a stylized top-down brain with 8 glowing, tappable regions.
 */
public class HenryBrainView extends View {

    public interface OnRegionClickListener {
        void onClick(String regionId);
    }

    private OnRegionClickListener listener;
    private final Paint paint     = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // {id, label, cx%, cy%, rx%, ry%, ARGB color}
    private static final Object[][] REGIONS = {
        {"mental_imagery",       "Mental\nImagery",      0.50f, 0.18f, 0.18f, 0.10f, 0xFF00D4FF},
        {"neural_plasticity",    "Neural\nPlasticity",   0.25f, 0.30f, 0.14f, 0.10f, 0xFF00FF99},
        {"default_mode",         "Default\nMode",        0.75f, 0.30f, 0.14f, 0.10f, 0xFFCC88FF},
        {"sensory_substitution", "Sensory\nSub.",        0.20f, 0.50f, 0.13f, 0.09f, 0xFFFF9944},
        {"memory",               "Memory\nBanks",        0.80f, 0.50f, 0.13f, 0.09f, 0xFFFFDD00},
        {"google_docs",          "Google\nDocs",         0.30f, 0.70f, 0.12f, 0.09f, 0xFF4CAF50},
        {"google_sheets",        "Google\nSheets",       0.50f, 0.77f, 0.12f, 0.09f, 0xFF0AB56E},
        {"google_slides",        "Google\nSlides",       0.70f, 0.70f, 0.12f, 0.09f, 0xFFFF7043},
    };

    private String hoveredRegion = null;

    public HenryBrainView(Context context) {
        super(context); init();
    }
    public HenryBrainView(Context context, AttributeSet attrs) {
        super(context, attrs); init();
    }
    public HenryBrainView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle); init();
    }

    private void init() {
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
    }

    public void setOnRegionClickListener(OnRegionClickListener l) {
        this.listener = l;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth(), h = getHeight();
        canvas.drawColor(0xFF020C1B);

        // Brain body — large oval
        float cx = w * 0.5f, cy = h * 0.46f;
        float rx = w * 0.42f, ry = h * 0.40f;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFF0A1E30);
        canvas.drawOval(new RectF(cx - rx, cy - ry, cx + rx, cy + ry), paint);

        // Centre split line
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(0xFF00D4FF);
        paint.setStrokeWidth(1.5f);
        canvas.drawLine(cx, cy - ry + 12, cx, cy + ry - 12, paint);

        // Brain outline glow
        glowPaint.set(paint);
        glowPaint.setMaskFilter(new BlurMaskFilter(14f, BlurMaskFilter.Blur.OUTER));
        glowPaint.setColor(0x4400D4FF);
        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeWidth(4f);
        canvas.drawOval(new RectF(cx - rx, cy - ry, cx + rx, cy + ry), glowPaint);

        // Sulci (decorative wrinkle lines)
        drawSulci(canvas, cx, cy, rx, ry);

        // Draw each region
        for (Object[] r : REGIONS) {
            String id    = (String) r[0];
            String label = (String) r[1];
            float  rcx   = w * (float) r[2];
            float  rcy   = h * (float) r[3];
            float  rrx   = w * (float) r[4];
            float  rry   = h * (float) r[5];
            int    color = (int)   r[6];
            boolean hovered = id.equals(hoveredRegion);

            // Glow halo
            glowPaint.setColor(color & 0x55FFFFFF);
            glowPaint.setStyle(Paint.Style.FILL);
            glowPaint.setMaskFilter(new BlurMaskFilter(hovered ? 24f : 14f, BlurMaskFilter.Blur.NORMAL));
            canvas.drawOval(new RectF(rcx - rrx - 8, rcy - rry - 8, rcx + rrx + 8, rcy + rry + 8), glowPaint);

            // Filled ellipse
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(hovered ? (color & 0xFFFFFF) | 0xFF000000 : (color & 0xFFFFFF) | 0xBB000000);
            paint.setMaskFilter(null);
            canvas.drawOval(new RectF(rcx - rrx, rcy - rry, rcx + rrx, rcy + rry), paint);

            // Border
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(hovered ? color : (color & 0xFFFFFF) | 0x99000000);
            paint.setStrokeWidth(hovered ? 2.5f : 1.5f);
            canvas.drawOval(new RectF(rcx - rrx, rcy - rry, rcx + rrx, rcy + rry), paint);

            // Label text
            textPaint.setColor(hovered ? color : 0xCCFFFFFF);
            float ts = h * 0.022f;
            textPaint.setTextSize(ts);
            String[] lines = label.split("\n");
            float lineH  = ts * 1.25f;
            float startY = rcy - (lines.length - 1) * lineH / 2f + ts / 3f;
            for (int li = 0; li < lines.length; li++) {
                canvas.drawText(lines[li], rcx, startY + li * lineH, textPaint);
            }
        }

        // Title
        paint.setMaskFilter(null);
        textPaint.setTextSize(h * 0.032f);
        textPaint.setColor(0xFF00D4FF);
        canvas.drawText("◈  H E N R Y  B R A I N  M A P  ◈", w / 2f, h * 0.06f, textPaint);
        textPaint.setTextSize(h * 0.016f);
        textPaint.setColor(0xFF2A6A8A);
        canvas.drawText("TAP A REGION TO ACTIVATE", w / 2f, h * 0.10f, textPaint);
    }

    private void drawSulci(Canvas canvas, float cx, float cy, float rx, float ry) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(0xFF0D2A3A);
        paint.setStrokeWidth(1.2f);
        paint.setMaskFilter(null);
        Path p = new Path();
        p.moveTo(cx - rx * 0.6f, cy - ry * 0.2f);
        p.cubicTo(cx - rx * 0.3f, cy - ry * 0.5f, cx + rx * 0.1f, cy - ry * 0.3f, cx + rx * 0.5f, cy - ry * 0.1f);
        canvas.drawPath(p, paint);
        p.reset();
        p.moveTo(cx - rx * 0.5f, cy + ry * 0.1f);
        p.cubicTo(cx - rx * 0.1f, cy + ry * 0.4f, cx + rx * 0.2f, cy + ry * 0.2f, cx + rx * 0.6f, cy + ry * 0.3f);
        canvas.drawPath(p, paint);
        p.reset();
        p.moveTo(cx - rx * 0.3f, cy - ry * 0.6f);
        p.cubicTo(cx, cy - ry * 0.4f, cx + rx * 0.2f, cy - ry * 0.65f, cx + rx * 0.4f, cy - ry * 0.4f);
        canvas.drawPath(p, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        int w = getWidth(), h = getHeight();
        float ex = e.getX(), ey = e.getY();
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                hoveredRegion = findRegion(ex, ey, w, h);
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
                String hit = findRegion(ex, ey, w, h);
                hoveredRegion = null;
                invalidate();
                if (hit != null && listener != null) listener.onClick(hit);
                return true;
        }
        return super.onTouchEvent(e);
    }

    private String findRegion(float x, float y, int w, int h) {
        for (Object[] r : REGIONS) {
            float rcx = w * (float) r[2];
            float rcy = h * (float) r[3];
            float rrx = w * (float) r[4] * 1.25f; // slightly bigger hit area
            float rry = h * (float) r[5] * 1.25f;
            float dx  = (x - rcx) / rrx;
            float dy  = (y - rcy) / rry;
            if (dx * dx + dy * dy <= 1.0f) return (String) r[0];
        }
        return null;
    }
}
