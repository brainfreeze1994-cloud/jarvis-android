package com.jarvis.ai;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;

/**
 * Renders a stylized 3D shaded polygonal asteroid rock with dynamic lighting and facets,
 * matching NASA Eyes on Asteroids UI.
 */
public class AsteroidRockView extends View {

    private Paint facetPaint1, facetPaint2, facetPaint3, facetPaint4, outlinePaint;
    private float rotationAngle = 0f;

    public AsteroidRockView(Context context) {
        super(context);
        init();
    }

    public AsteroidRockView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AsteroidRockView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        facetPaint1 = new Paint(Paint.ANTI_ALIAS_FLAG);
        facetPaint1.setColor(0xFFD4CEBA); // Highlight top facet

        facetPaint2 = new Paint(Paint.ANTI_ALIAS_FLAG);
        facetPaint2.setColor(0xFFB5AC98); // Mid facet

        facetPaint3 = new Paint(Paint.ANTI_ALIAS_FLAG);
        facetPaint3.setColor(0xFF8E8674); // Shadow facet

        facetPaint4 = new Paint(Paint.ANTI_ALIAS_FLAG);
        facetPaint4.setColor(0xFF6E6756); // Deep shadow facet

        outlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        outlinePaint.setStyle(Paint.Style.STROKE);
        outlinePaint.setStrokeWidth(2f);
        outlinePaint.setColor(0x44FFFFFF);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        float cx = w / 2f;
        float cy = h / 2f;
        float r = Math.min(w, h) * 0.44f;

        canvas.save();
        canvas.translate(cx, cy);

        // Draw multiple shaded polygonal facets for realistic rocky appearance
        // Facet 1 (Top left highlight)
        Path p1 = new Path();
        p1.moveTo(-0.25f * r, -0.92f * r);
        p1.lineTo(0.35f * r, -0.85f * r);
        p1.lineTo(0.15f * r, -0.25f * r);
        p1.lineTo(-0.45f * r, -0.35f * r);
        p1.close();
        canvas.drawPath(p1, facetPaint1);

        // Facet 2 (Top right highlight)
        Path p2 = new Path();
        p2.moveTo(0.35f * r, -0.85f * r);
        p2.lineTo(0.88f * r, -0.40f * r);
        p2.lineTo(0.55f * r, 0.10f * r);
        p2.lineTo(0.15f * r, -0.25f * r);
        p2.close();
        canvas.drawPath(p2, facetPaint2);

        // Facet 3 (Far left midtone)
        Path p3 = new Path();
        p3.moveTo(-0.25f * r, -0.92f * r);
        p3.lineTo(-0.85f * r, -0.35f * r);
        p3.lineTo(-0.75f * r, 0.35f * r);
        p3.lineTo(-0.45f * r, -0.35f * r);
        p3.close();
        canvas.drawPath(p3, facetPaint2);

        // Facet 4 (Center face)
        Path p4 = new Path();
        p4.moveTo(-0.45f * r, -0.35f * r);
        p4.lineTo(0.15f * r, -0.25f * r);
        p4.lineTo(0.55f * r, 0.10f * r);
        p4.lineTo(0.20f * r, 0.55f * r);
        p4.lineTo(-0.35f * r, 0.45f * r);
        p4.close();
        canvas.drawPath(p4, facetPaint2);

        // Facet 5 (Bottom shadow)
        Path p5 = new Path();
        p5.moveTo(-0.75f * r, 0.35f * r);
        p5.lineTo(-0.35f * r, 0.45f * r);
        p5.lineTo(0.05f * r, 0.90f * r);
        p5.lineTo(-0.55f * r, 0.85f * r);
        p5.close();
        canvas.drawPath(p5, facetPaint3);

        // Facet 6 (Bottom right deep shadow)
        Path p6 = new Path();
        p6.moveTo(0.55f * r, 0.10f * r);
        p6.lineTo(0.85f * r, 0.45f * r);
        p6.lineTo(0.45f * r, 0.85f * r);
        p6.lineTo(0.20f * r, 0.55f * r);
        p6.close();
        canvas.drawPath(p6, facetPaint4);

        // Facet 7 (Bottom center shadow)
        Path p7 = new Path();
        p7.moveTo(-0.35f * r, 0.45f * r);
        p7.lineTo(0.20f * r, 0.55f * r);
        p7.lineTo(0.45f * r, 0.85f * r);
        p7.lineTo(0.05f * r, 0.90f * r);
        p7.close();
        canvas.drawPath(p7, facetPaint3);

        // Outer contour
        Path contour = new Path();
        contour.moveTo(-0.25f * r, -0.92f * r);
        contour.lineTo(0.35f * r, -0.85f * r);
        contour.lineTo(0.88f * r, -0.40f * r);
        contour.lineTo(0.85f * r, 0.45f * r);
        contour.lineTo(0.45f * r, 0.85f * r);
        contour.lineTo(0.05f * r, 0.90f * r);
        contour.lineTo(-0.55f * r, 0.85f * r);
        contour.lineTo(-0.75f * r, 0.35f * r);
        contour.lineTo(-0.85f * r, -0.35f * r);
        contour.close();
        canvas.drawPath(contour, outlinePaint);

        canvas.restore();
    }
}
