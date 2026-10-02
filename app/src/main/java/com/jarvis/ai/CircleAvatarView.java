package com.jarvis.ai;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

/**
 * Circular avatar image, cropped via BitmapShader (reliable across all API levels, unlike
 * relying on clipToOutline + background shape tricks). Used for HENRY's consistent AI-generated
 * portrait (see HenryAvatarManager) in the chat avatar slot.
 */
public class CircleAvatarView extends View {
    private Bitmap bitmap;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    public CircleAvatarView(Context context) { super(context); }
    public CircleAvatarView(Context context, AttributeSet attrs) { super(context, attrs); }
    public CircleAvatarView(Context context, AttributeSet attrs, int defStyle) { super(context, attrs, defStyle); }

    public void setImageBitmap(Bitmap bmp) {
        this.bitmap = bmp;
        if (bmp != null) {
            paint.setShader(new BitmapShader(bmp, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
            updateShaderMatrix();
        } else {
            paint.setShader(null);
        }
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        rect.set(0, 0, w, h);
        updateShaderMatrix();
    }

    /** Centers+crops the bitmap to fill the view (like centerCrop), applied whenever either
     *  the bitmap or the view's size becomes known/changes — setImageBitmap is typically called
     *  after RecyclerView has already laid the view out, so onSizeChanged alone isn't enough. */
    private void updateShaderMatrix() {
        if (bitmap == null || paint.getShader() == null) return;
        int w = getWidth(), h = getHeight();
        if (w == 0 || h == 0) return;
        float scale = Math.max(w / (float) bitmap.getWidth(), h / (float) bitmap.getHeight());
        android.graphics.Matrix m = new android.graphics.Matrix();
        m.setScale(scale, scale);
        m.postTranslate((w - bitmap.getWidth() * scale) / 2f, (h - bitmap.getHeight() * scale) / 2f);
        ((BitmapShader) paint.getShader()).setLocalMatrix(m);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (bitmap == null) return;
        float radius = Math.min(getWidth(), getHeight()) / 2f;
        canvas.drawCircle(getWidth() / 2f, getHeight() / 2f, radius, paint);
    }
}
