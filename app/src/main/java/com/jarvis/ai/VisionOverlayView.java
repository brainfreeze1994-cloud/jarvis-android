package com.jarvis.ai;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

/**
 * VisionOverlayView — draws detection/tracking bounding boxes
 * over a camera preview or image. Standalone top-level class
 * so it can be referenced in XML layouts.
 */
public class VisionOverlayView extends View {

    public static class Box {
        public final RectF  rect;
        public final String label;
        public final float  confidence;
        public final int    color;

        public Box(RectF r, String l, float c, int col) {
            rect = r; label = l; confidence = c; color = col;
        }
    }

    private final List<Box> boxes   = new ArrayList<>();
    private final Paint     boxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint     txtPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint     bgPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int imgW = 1, imgH = 1;

    public VisionOverlayView(Context ctx) { super(ctx); }
    public VisionOverlayView(Context ctx, AttributeSet attrs) { super(ctx, attrs); }
    public VisionOverlayView(Context ctx, AttributeSet attrs, int defStyle) { super(ctx, attrs, defStyle); }

    public void setBoxes(List<Box> b) { boxes.clear(); boxes.addAll(b); invalidate(); }
    public void clearBoxes()          { boxes.clear(); invalidate(); }
    public void setImageSize(int w, int h) { imgW = w; imgH = h; }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (boxes.isEmpty()) return;

        float sw = (float) getWidth()  / Math.max(imgW, 1);
        float sh = (float) getHeight() / Math.max(imgH, 1);

        for (Box b : boxes) {
            RectF scaled = new RectF(
                b.rect.left   * sw,
                b.rect.top    * sh,
                b.rect.right  * sw,
                b.rect.bottom * sh
            );

            // Bounding box
            boxPaint.setStyle(Paint.Style.STROKE);
            boxPaint.setColor(b.color);
            boxPaint.setStrokeWidth(3f);
            canvas.drawRect(scaled, boxPaint);

            // Corner accents
            float cs = Math.min(scaled.width(), scaled.height()) * 0.12f;
            boxPaint.setStrokeWidth(5f);
            // TL
            canvas.drawLine(scaled.left, scaled.top, scaled.left + cs, scaled.top, boxPaint);
            canvas.drawLine(scaled.left, scaled.top, scaled.left, scaled.top + cs, boxPaint);
            // TR
            canvas.drawLine(scaled.right - cs, scaled.top, scaled.right, scaled.top, boxPaint);
            canvas.drawLine(scaled.right, scaled.top, scaled.right, scaled.top + cs, boxPaint);
            // BL
            canvas.drawLine(scaled.left, scaled.bottom - cs, scaled.left, scaled.bottom, boxPaint);
            canvas.drawLine(scaled.left, scaled.bottom, scaled.left + cs, scaled.bottom, boxPaint);
            // BR
            canvas.drawLine(scaled.right - cs, scaled.bottom, scaled.right, scaled.bottom, boxPaint);
            canvas.drawLine(scaled.right, scaled.bottom - cs, scaled.right, scaled.bottom, boxPaint);

            // Label background + text
            String lbl = b.label + "  " + (int)(b.confidence * 100) + "%";
            float  ts  = Math.max(getHeight() * 0.022f, 24f);
            txtPaint.setTextSize(ts);
            txtPaint.setColor(Color.WHITE);
            float tw = txtPaint.measureText(lbl);

            bgPaint.setColor((b.color & 0x00FFFFFF) | 0xCC000000);
            float labelTop = scaled.top - ts - 6;
            canvas.drawRect(scaled.left, labelTop,
                            scaled.left + tw + 12, scaled.top, bgPaint);
            // Left accent stripe
            bgPaint.setColor(b.color);
            canvas.drawRect(scaled.left, labelTop, scaled.left + 4, scaled.top, bgPaint);
            canvas.drawText(lbl, scaled.left + 8, scaled.top - 5, txtPaint);
        }
    }
}
