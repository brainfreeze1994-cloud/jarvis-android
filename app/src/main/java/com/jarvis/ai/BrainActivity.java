package com.jarvis.ai;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

/**
 * BrainActivity — Interactive HENRY Brain Map
 * Renders a stylized top-down brain with 8 clickable regions.
 * Tapping a region launches the corresponding HENRY brain module.
 */
public class BrainActivity extends AppCompatActivity {

    public static final int REQUEST_CODE = 4001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_brain);

        TextView tvTitle = findViewById(R.id.brain_title);
        if (tvTitle != null) tvTitle.setText("H.E.N.R.Y BRAIN");

        BrainView brainView = findViewById(R.id.brain_view);
        if (brainView != null) {
            brainView.setOnRegionClickListener(region -> {
                switch (region) {
                    case "mental_imagery":
                        startActivity(new Intent(this, MentalImageryActivity.class));
                        break;
                    case "sensory_substitution":
                        startActivity(new Intent(this, SensorySubstitutionActivity.class));
                        break;
                    case "neural_plasticity":
                        startActivity(new Intent(this, NeuralPlasticityActivity.class));
                        break;
                    case "default_mode":
                        startActivity(new Intent(this, DefaultModeNetworkActivity.class));
                        break;
                    case "google_docs":
                        Toast.makeText(this, "Say: Create a Google Doc", Toast.LENGTH_SHORT).show();
                        break;
                    case "google_sheets":
                        Toast.makeText(this, "Say: Create a Google Sheet", Toast.LENGTH_SHORT).show();
                        break;
                    case "google_slides":
                        Toast.makeText(this, "Say: Create a Google Slides", Toast.LENGTH_SHORT).show();
                        break;
                    case "memory":
                        startActivity(new Intent(this, SmartMemoryActivity.class));
                        break;
                }
            });
        }

        TextView tvBack = findViewById(R.id.brain_back);
        if (tvBack != null) tvBack.setOnClickListener(v -> finish());
    }

    // ── Inner canvas view ─────────────────────────────────────────────────────
    public static class BrainView extends View {

        public interface OnRegionClickListener {
            void onClick(String regionId);
        }

        private OnRegionClickListener listener;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        // Region data: {id, label, cx%, cy%, rx%, ry%, color}
        private static final Object[][] REGIONS = {
            {"mental_imagery",      "Mental\nImagery",       0.50f, 0.18f, 0.18f, 0.10f, 0xFF00D4FF},
            {"neural_plasticity",   "Neural\nPlasticity",    0.25f, 0.28f, 0.14f, 0.10f, 0xFF00FF99},
            {"default_mode",        "Default\nMode",         0.75f, 0.28f, 0.14f, 0.10f, 0xFFCC88FF},
            {"sensory_substitution","Sensory\nSub.",         0.20f, 0.48f, 0.13f, 0.09f, 0xFFFF9944},
            {"memory",              "Memory\nBanks",         0.80f, 0.48f, 0.13f, 0.09f, 0xFFFFDD00},
            {"google_docs",         "Google\nDocs",          0.30f, 0.68f, 0.12f, 0.09f, 0xFF4CAF50},
            {"google_sheets",       "Google\nSheets",        0.50f, 0.75f, 0.12f, 0.09f, 0xFF0AB56E},
            {"google_slides",       "Google\nSlides",        0.70f, 0.68f, 0.12f, 0.09f, 0xFFFF7043},
        };

        private float touchX, touchY;
        private String hoveredRegion = null;

        public BrainView(android.content.Context context) { super(context); init(); }
        public BrainView(android.content.Context context, android.util.AttributeSet attrs) { super(context, attrs); init(); }

        private void init() {
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        }

        public void setOnRegionClickListener(OnRegionClickListener l) { this.listener = l; }

        @Override
        protected void onDraw(Canvas canvas) {
            int w = getWidth(), h = getHeight();
            canvas.drawColor(0xFF020C1B);

            // Draw brain outline — two large overlapping ovals
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xFF0A1E30);
            float cx = w * 0.5f, cy = h * 0.45f;
            float rx = w * 0.42f, ry = h * 0.40f;
            canvas.drawOval(new RectF(cx - rx, cy - ry, cx + rx, cy + ry), paint);

            // Brain split line
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(0xFF00D4FF);
            paint.setStrokeWidth(1.5f);
            canvas.drawLine(cx, cy - ry + 10, cx, cy + ry - 10, paint);

            // Brain outline glow
            glowPaint.set(paint);
            glowPaint.setMaskFilter(new android.graphics.BlurMaskFilter(12f, android.graphics.BlurMaskFilter.Blur.OUTER));
            glowPaint.setColor(0x4400D4FF);
            glowPaint.setStyle(Paint.Style.STROKE);
            glowPaint.setStrokeWidth(4f);
            canvas.drawOval(new RectF(cx - rx, cy - ry, cx + rx, cy + ry), glowPaint);

            // Draw sulci (brain wrinkle lines)
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1f);
            paint.setColor(0xFF0D2A40);
            drawBrainSulci(canvas, cx, cy, rx, ry);

            // Draw each region
            for (Object[] r : REGIONS) {
                String id    = (String) r[0];
                String label = (String) r[1];
                float rcx    = w * (float) r[2];
                float rcy    = h * (float) r[3];
                float rrx    = w * (float) r[4];
                float rry    = h * (float) r[5];
                int   color  = (int) r[6];

                boolean hovered = id.equals(hoveredRegion);

                // Glow halo
                glowPaint.setColor(color & 0x55FFFFFF);
                glowPaint.setStyle(Paint.Style.FILL);
                glowPaint.setMaskFilter(new android.graphics.BlurMaskFilter(hovered ? 22f : 14f, android.graphics.BlurMaskFilter.Blur.NORMAL));
                canvas.drawOval(new RectF(rcx - rrx - 8, rcy - rry - 8, rcx + rrx + 8, rcy + rry + 8), glowPaint);

                // Filled ellipse
                paint.setStyle(Paint.Style.FILL);
                paint.setColor((color & 0xFFFFFF) | (hovered ? 0xFF000000 : 0xCC000000));
                canvas.drawOval(new RectF(rcx - rrx, rcy - rry, rcx + rrx, rcy + rry), paint);

                // Border
                paint.setStyle(Paint.Style.STROKE);
                paint.setColor(hovered ? color : (color & 0xFFFFFF) | 0x88000000);
                paint.setStrokeWidth(hovered ? 2.5f : 1.5f);
                canvas.drawOval(new RectF(rcx - rrx, rcy - rry, rcx + rrx, rcy + rry), paint);

                // Label
                textPaint.setColor(hovered ? color : 0xCCFFFFFF);
                float textSizePx = h * 0.022f;
                textPaint.setTextSize(textSizePx);
                String[] lines = label.split("\n");
                float lineH = textSizePx * 1.25f;
                float startY = rcy - (lines.length - 1) * lineH / 2f + textSizePx / 3f;
                for (int li = 0; li < lines.length; li++) {
                    canvas.drawText(lines[li], rcx, startY + li * lineH, textPaint);
                }
            }

            // Title
            textPaint.setTextSize(h * 0.032f);
            textPaint.setColor(0xFF00D4FF);
            canvas.drawText("◈ HENRY BRAIN MAP", w / 2f, h * 0.06f, textPaint);
            textPaint.setTextSize(h * 0.016f);
            textPaint.setColor(0xFF2A6A8A);
            canvas.drawText("TAP A REGION TO ACTIVATE", w / 2f, h * 0.10f, textPaint);
        }

        private void drawBrainSulci(Canvas canvas, float cx, float cy, float rx, float ry) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(0xFF112233);
            paint.setStrokeWidth(1.2f);
            // A few curved path "wrinkles" for visual richness
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
            p.cubicTo(cx, cy - ry * 0.4f, cx + rx * 0.2f, cy - ry * 0.6f, cx + rx * 0.4f, cy - ry * 0.4f);
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
                    if (hit != null && listener != null) {
                        listener.onClick(hit);
                    }
                    return true;
            }
            return super.onTouchEvent(e);
        }

        private String findRegion(float x, float y, int w, int h) {
            for (Object[] r : REGIONS) {
                float rcx = w * (float) r[2];
                float rcy = h * (float) r[3];
                float rrx = w * (float) r[4] * 1.2f; // slightly larger hit area
                float rry = h * (float) r[5] * 1.2f;
                float dx  = (x - rcx) / rrx;
                float dy  = (y - rcy) / rry;
                if (dx * dx + dy * dy <= 1.0f) return (String) r[0];
            }
            return null;
        }
    }
}
