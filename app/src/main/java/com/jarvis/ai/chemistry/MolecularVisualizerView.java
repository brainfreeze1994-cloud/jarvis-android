package com.jarvis.ai.chemistry;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Interactive Chemical Structure & Molecular Visualizer View.
 * Supports:
 *   1. 2D Structural Chemical Formula Projection
 *   2. Real-Time Interactive 3D Ball-and-Stick (Rotate, Pitch/Yaw, Zoom, Pan, Tap Atom)
 *   3. Scientifically Accurate Lewis Dot Structure
 *   4. Molecular Polarity & Dipole Vector Visualization (δ⁺, δ⁻, Bond Dipoles, Net μ Debye)
 */
public class MolecularVisualizerView extends View {

    public static final int MODE_2D       = 0;
    public static final int MODE_3D       = 1;
    public static final int MODE_LEWIS    = 2;
    public static final int MODE_POLARITY = 3;

    public enum RenderMode {
        MODE_2D,
        MODE_3D,
        MODE_LEWIS,
        MODE_POLARITY
    }

    private int currentMode = MODE_3D;
    private MolecularStructureData currentMolecule = null;
    private Atom3D selectedAtom = null;

    // View Options
    private boolean showLabels = true;
    private boolean autoRotate = true;

    // 3D Camera & Transform State
    private float rotX = 18.0f; // Pitch (deg)
    private float rotY = 32.0f; // Yaw (deg)
    private float scaleFactor = 1.0f;
    private float panX = 0.0f;
    private float panY = 0.0f;

    // Touch & Gesture Tracking
    private float lastTouchX, lastTouchY;
    private boolean isDragging = false;
    private ScaleGestureDetector scaleDetector;
    private GestureDetector gestureDetector;

    // Auto-rotation timer
    private final Handler animHandler = new Handler(Looper.getMainLooper());
    private final Runnable autoRotateRunnable = new Runnable() {
        @Override
        public void run() {
            if (autoRotate && currentMode == MODE_3D) {
                rotY = (rotY + 0.65f) % 360f;
                invalidate();
            }
            animHandler.postDelayed(this, 16);
        }
    };

    // Rendering Paints
    private Paint gridPaint;
    private Paint hudTextPaint;
    private Paint atomPaint;
    private Paint bondPaint;
    private Paint bondDoublePaint;
    private Paint textPaint;
    private Paint labelBgPaint;
    private Paint lonePairPaint;
    private Paint dipolePaint;
    private Paint dipoleArrowPaint;
    private Paint selectionGlowPaint;

    // Listener
    public interface OnAtomSelectedListener {
        void onAtomSelected(Atom3D atom, MolecularStructureData molecule);
    }
    private OnAtomSelectedListener atomSelectedListener;

    public MolecularVisualizerView(Context context) {
        super(context);
        init(context);
    }

    public MolecularVisualizerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public MolecularVisualizerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(0x1800E5FF);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(1.5f);

        hudTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        hudTextPaint.setColor(0xFF00E5FF);
        hudTextPaint.setTextSize(spToPx(10));
        hudTextPaint.setTypeface(Typeface.MONOSPACE);

        atomPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        atomPaint.setStyle(Paint.Style.FILL);

        bondPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bondPaint.setStyle(Paint.Style.STROKE);
        bondPaint.setStrokeCap(Paint.Cap.ROUND);
        bondPaint.setColor(0xFF88AABB);

        bondDoublePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bondDoublePaint.setStyle(Paint.Style.STROKE);
        bondDoublePaint.setStrokeCap(Paint.Cap.ROUND);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);

        labelBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelBgPaint.setStyle(Paint.Style.FILL);
        labelBgPaint.setColor(0xCC05101E);

        lonePairPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        lonePairPaint.setStyle(Paint.Style.FILL);
        lonePairPaint.setColor(0xFFFFD54F); // Golden electron dots

        dipolePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dipolePaint.setStyle(Paint.Style.STROKE);
        dipolePaint.setStrokeWidth(dpToPx(2.5f));
        dipolePaint.setColor(0xFF00FFCC);

        dipoleArrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dipoleArrowPaint.setStyle(Paint.Style.FILL);
        dipoleArrowPaint.setColor(0xFF00FFCC);

        selectionGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        selectionGlowPaint.setStyle(Paint.Style.STROKE);
        selectionGlowPaint.setStrokeWidth(dpToPx(2.5f));
        selectionGlowPaint.setColor(0xFF00FFCC);

        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                scaleFactor *= detector.getScaleFactor();
                scaleFactor = Math.max(0.45f, Math.min(scaleFactor, 3.2f));
                invalidate();
                return true;
            }
        });

        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                return handleAtomTap(e.getX(), e.getY());
            }
        });

        animHandler.post(autoRotateRunnable);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        animHandler.removeCallbacks(autoRotateRunnable);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUBLIC API CONTROLS
    // ─────────────────────────────────────────────────────────────────────────

    public void setMolecularData(MolecularStructureData data) {
        this.currentMolecule = data;
        this.selectedAtom = null;
        invalidate();
    }

    public MolecularStructureData getMolecularData() {
        return currentMolecule;
    }

    public void setMode(int mode) {
        this.currentMode = mode;
        invalidate();
    }

    public void setRenderMode(RenderMode mode) {
        if (mode == null) return;
        switch (mode) {
            case MODE_2D: setMode(MODE_2D); break;
            case MODE_3D: setMode(MODE_3D); break;
            case MODE_LEWIS: setMode(MODE_LEWIS); break;
            case MODE_POLARITY: setMode(MODE_POLARITY); break;
        }
    }

    public RenderMode getRenderMode() {
        switch (currentMode) {
            case MODE_2D: return RenderMode.MODE_2D;
            case MODE_LEWIS: return RenderMode.MODE_LEWIS;
            case MODE_POLARITY: return RenderMode.MODE_POLARITY;
            case MODE_3D:
            default: return RenderMode.MODE_3D;
        }
    }

    public int getMode() {
        return currentMode;
    }

    public void setAutoRotate(boolean enabled) {
        this.autoRotate = enabled;
        if (!autoRotate) {
            animHandler.removeCallbacks(autoRotateRunnable);
        } else {
            animHandler.removeCallbacks(autoRotateRunnable);
            animHandler.post(autoRotateRunnable);
        }
        invalidate();
    }

    public boolean isAutoRotate() {
        return autoRotate;
    }

    public void toggleLabels() {
        this.showLabels = !this.showLabels;
        invalidate();
    }

    public boolean isShowLabels() {
        return showLabels;
    }

    public void resetCamera() {
        ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
        final float startRotX = rotX;
        final float startRotY = rotY;
        final float startScale = scaleFactor;
        final float startPanX = panX;
        final float startPanY = panY;

        anim.setDuration(400);
        anim.addUpdateListener(animation -> {
            float frac = animation.getAnimatedFraction();
            rotX = startRotX + (18f - startRotX) * frac;
            rotY = startRotY + (32f - startRotY) * frac;
            scaleFactor = startScale + (1.0f - startScale) * frac;
            panX = startPanX + (0f - startPanX) * frac;
            panY = startPanY + (0f - startPanY) * frac;
            invalidate();
        });
        anim.start();
    }

    public void setOnAtomSelectedListener(OnAtomSelectedListener listener) {
        this.atomSelectedListener = listener;
    }

    /**
     * Exports the rendered visual structure as a high-resolution Bitmap.
     */
    public Bitmap exportStructureBitmap() {
        int w = getWidth() > 0 ? getWidth() : 900;
        int h = getHeight() > 0 ? getHeight() : 700;
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        draw(canvas);
        return bitmap;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TOUCH GESTURE HANDLING
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);

        if (scaleDetector.isInProgress()) {
            isDragging = false;
            return true;
        }

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchX = event.getX();
                lastTouchY = event.getY();
                isDragging = true;
                break;

            case MotionEvent.ACTION_MOVE:
                if (isDragging && event.getPointerCount() == 1) {
                    float dx = event.getX() - lastTouchX;
                    float dy = event.getY() - lastTouchY;

                    if (currentMode == MODE_3D) {
                        rotY += dx * 0.5f;
                        rotX -= dy * 0.5f;
                        // Limit pitch to prevent flipping
                        rotX = Math.max(-85f, Math.min(85f, rotX));
                    } else {
                        // 2D/Lewis panning
                        panX += dx;
                        panY += dy;
                    }

                    lastTouchX = event.getX();
                    lastTouchY = event.getY();
                    invalidate();
                } else if (event.getPointerCount() == 2) {
                    // Two-finger pan in 3D
                    float dx = event.getX() - lastTouchX;
                    float dy = event.getY() - lastTouchY;
                    panX += dx * 0.6f;
                    panY += dy * 0.6f;
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

    private boolean handleAtomTap(float touchX, float touchY) {
        if (currentMolecule == null || currentMolecule.atoms.isEmpty()) return false;

        float cX = getWidth() / 2f + panX;
        float cY = getHeight() / 2f + panY;
        float scale = Math.min(getWidth(), getHeight()) * 0.22f * scaleFactor;

        Atom3D hitAtom = null;
        float closestDistSq = Float.MAX_VALUE;

        if (currentMode == MODE_3D) {
            float radX = (float) Math.toRadians(rotX);
            float radY = (float) Math.toRadians(rotY);
            float cosX = (float) Math.cos(radX);
            float sinX = (float) Math.sin(radX);
            float cosY = (float) Math.cos(radY);
            float sinY = (float) Math.sin(radY);

            for (Atom3D a : currentMolecule.atoms) {
                // Apply rotation
                float x1 = a.x * cosY + a.z * sinY;
                float z1 = -a.x * sinY + a.z * cosY;
                float y2 = a.y * cosX - z1 * sinX;

                float screenX = cX + x1 * scale;
                float screenY = cY - y2 * scale; // Invert y for screen space

                float atomR = Math.max(dpToPx(18), (a.radiusPm / 100f) * scale * 0.28f);
                float distSq = (touchX - screenX) * (touchX - screenX) + (touchY - screenY) * (touchY - screenY);

                if (distSq <= (atomR * 1.5f) * (atomR * 1.5f) && distSq < closestDistSq) {
                    closestDistSq = distSq;
                    hitAtom = a;
                }
            }
        } else {
            // 2D / Lewis / Polarity mode
            for (Atom3D a : currentMolecule.atoms) {
                float screenX = cX + a.x2d * scale;
                float screenY = cY + a.y2d * scale;
                float atomR = dpToPx(24);
                float distSq = (touchX - screenX) * (touchX - screenX) + (touchY - screenY) * (touchY - screenY);

                if (distSq <= (atomR * 1.5f) * (atomR * 1.5f) && distSq < closestDistSq) {
                    closestDistSq = distSq;
                    hitAtom = a;
                }
            }
        }

        if (hitAtom != null) {
            selectedAtom = hitAtom;
            if (atomSelectedListener != null) {
                atomSelectedListener.onAtomSelected(hitAtom, currentMolecule);
            }
            invalidate();
            return true;
        }
        return false;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DRAW ROUTINE
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        // 1. Futuristic Dark Navy Background
        canvas.drawColor(0xFF050E1A);

        // 2. Holographic HUD Coordinate Grid & Concentric Rings
        drawHudGrid(canvas, w, h);

        if (currentMolecule == null) {
            drawAwaitingPrompt(canvas, w, h);
            return;
        }

        // 3. Render Mode Specific Geometry
        switch (currentMode) {
            case MODE_2D:
                draw2DStructure(canvas, w, h);
                break;
            case MODE_3D:
                draw3DMolecule(canvas, w, h);
                break;
            case MODE_LEWIS:
                drawLewisStructure(canvas, w, h);
                break;
            case MODE_POLARITY:
                drawPolarityVisualization(canvas, w, h);
                break;
        }

        // 4. Mode HUD Badge & Metadata Overlays
        drawHudOverlays(canvas, w, h);
    }

    private void drawHudGrid(Canvas canvas, int w, int h) {
        float cX = w / 2f + panX;
        float cY = h / 2f + panY;

        // Concentric target circles
        float maxR = Math.min(w, h) * 0.45f;
        canvas.drawCircle(cX, cY, maxR * 0.33f, gridPaint);
        canvas.drawCircle(cX, cY, maxR * 0.66f, gridPaint);
        canvas.drawCircle(cX, cY, maxR, gridPaint);

        // Crosshairs
        canvas.drawLine(cX - maxR, cY, cX + maxR, cY, gridPaint);
        canvas.drawLine(cX, cY - maxR, cX, cY + maxR, gridPaint);

        // Corner HUD Reticle Brackets
        Paint reticlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        reticlePaint.setColor(0x4000E5FF);
        reticlePaint.setStyle(Paint.Style.STROKE);
        reticlePaint.setStrokeWidth(dpToPx(1.5f));

        float m = dpToPx(10);
        float len = dpToPx(16);
        // Top-left
        canvas.drawLine(m, m, m + len, m, reticlePaint);
        canvas.drawLine(m, m, m, m + len, reticlePaint);
        // Top-right
        canvas.drawLine(w - m, m, w - m - len, m, reticlePaint);
        canvas.drawLine(w - m, m, w - m, m + len, reticlePaint);
        // Bottom-left
        canvas.drawLine(m, h - m, m + len, h - m, reticlePaint);
        canvas.drawLine(m, h - m, m, h - m - len, reticlePaint);
        // Bottom-right
        canvas.drawLine(w - m, h - m, w - m - len, h - m, reticlePaint);
        canvas.drawLine(w - m, h - m, w - m, h - m - len, reticlePaint);
    }

    private void drawAwaitingPrompt(Canvas canvas, int w, int h) {
        textPaint.setColor(0xFF5588AA);
        textPaint.setTextSize(spToPx(13));
        canvas.drawText("Awaiting Reactants in Reaction Vessel", w / 2f, h / 2f - dpToPx(10), textPaint);
        textPaint.setTextSize(spToPx(10));
        canvas.drawText("Select elements above to synthesize a verified molecular structure", w / 2f, h / 2f + dpToPx(12), textPaint);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MODE 1: 2D STRUCTURE VIEW
    // ─────────────────────────────────────────────────────────────────────────

    private void draw2DStructure(Canvas canvas, int w, int h) {
        float cX = w / 2f + panX;
        float cY = h / 2f + panY;
        float scale = Math.min(w, h) * 0.22f * scaleFactor;

        // 1. Draw Bonds in 2D
        for (Bond3D bond : currentMolecule.bonds) {
            if (bond.fromAtomIndex >= currentMolecule.atoms.size() ||
                bond.toAtomIndex >= currentMolecule.atoms.size()) continue;

            Atom3D a1 = currentMolecule.atoms.get(bond.fromAtomIndex);
            Atom3D a2 = currentMolecule.atoms.get(bond.toAtomIndex);

            float x1 = cX + a1.x2d * scale;
            float y1 = cY + a1.y2d * scale;
            float x2 = cX + a2.x2d * scale;
            float y2 = cY + a2.y2d * scale;

            draw2DBondLine(canvas, x1, y1, x2, y2, bond.bondOrder);
        }

        // 2. Draw Atoms in 2D
        for (Atom3D atom : currentMolecule.atoms) {
            float ax = cX + atom.x2d * scale;
            float ay = cY + atom.y2d * scale;
            float radius = dpToPx(18);

            // Circular backing
            atomPaint.setColor(atom.color);
            canvas.drawCircle(ax, ay, radius, atomPaint);

            // Stroke outline
            Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
            border.setStyle(Paint.Style.STROKE);
            border.setStrokeWidth(dpToPx(1.5f));
            border.setColor(0x88FFFFFF);
            canvas.drawCircle(ax, ay, radius, border);

            // Selection indicator
            if (atom == selectedAtom) {
                canvas.drawCircle(ax, ay, radius + dpToPx(5), selectionGlowPaint);
            }

            // Chemical Symbol
            textPaint.setColor(isColorDark(atom.color) ? 0xFFFFFFFF : 0xFF101010);
            textPaint.setTextSize(spToPx(13));
            canvas.drawText(atom.symbol, ax, ay + spToPx(4.5f), textPaint);

            // Formal charge superscript
            if (atom.formalCharge != 0) {
                String fcStr = atom.formalCharge > 0 ? "+" + atom.formalCharge : String.valueOf(atom.formalCharge);
                textPaint.setTextSize(spToPx(9));
                textPaint.setColor(0xFFFFD54F);
                canvas.drawText(fcStr, ax + radius * 0.75f, ay - radius * 0.4f, textPaint);
            }
        }
    }

    private void draw2DBondLine(Canvas canvas, float x1, float y1, float x2, float y2, int order) {
        bondPaint.setColor(0xFF708FA8);
        bondPaint.setStrokeWidth(dpToPx(3.5f));

        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float) Math.hypot(dx, dy);
        if (len == 0) return;

        // Normal perpendicular vector for double/triple bonds
        float nx = -dy / len;
        float ny = dx / len;
        float offset = dpToPx(4.0f);

        if (order == 1) {
            canvas.drawLine(x1, y1, x2, y2, bondPaint);
        } else if (order == 2) {
            canvas.drawLine(x1 + nx * offset, y1 + ny * offset, x2 + nx * offset, y2 + ny * offset, bondPaint);
            canvas.drawLine(x1 - nx * offset, y1 - ny * offset, x2 - nx * offset, y2 - ny * offset, bondPaint);
        } else if (order == 3) {
            canvas.drawLine(x1, y1, x2, y2, bondPaint);
            canvas.drawLine(x1 + nx * offset * 1.5f, y1 + ny * offset * 1.5f, x2 + nx * offset * 1.5f, y2 + ny * offset * 1.5f, bondPaint);
            canvas.drawLine(x1 - nx * offset * 1.5f, y1 - ny * offset * 1.5f, x2 - nx * offset * 1.5f, y2 - ny * offset * 1.5f, bondPaint);
        } else {
            // Ionic coordination dotted bond
            Paint dash = new Paint(bondPaint);
            dash.setPathEffect(new DashPathEffect(new float[]{dpToPx(4), dpToPx(4)}, 0));
            dash.setColor(0x8800E5FF);
            canvas.drawLine(x1, y1, x2, y2, dash);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MODE 2: INTERACTIVE 3D BALL-AND-STICK MOLECULE
    // ─────────────────────────────────────────────────────────────────────────

    private static class ProjectedAtom {
        Atom3D atom;
        float screenX, screenY, depthZ;
        float screenRadius;
    }

    private static class ProjectedBond {
        Bond3D bond;
        ProjectedAtom p1, p2;
        float depthZ;
    }

    private void draw3DMolecule(Canvas canvas, int w, int h) {
        float cX = w / 2f + panX;
        float cY = h / 2f + panY;
        float scale = Math.min(w, h) * 0.22f * scaleFactor;

        float radX = (float) Math.toRadians(rotX);
        float radY = (float) Math.toRadians(rotY);
        float cosX = (float) Math.cos(radX);
        float sinX = (float) Math.sin(radX);
        float cosY = (float) Math.cos(radY);
        float sinY = (float) Math.sin(radY);

        List<ProjectedAtom> pAtoms = new ArrayList<>();
        for (Atom3D a : currentMolecule.atoms) {
            // Rotate around Y (yaw)
            float x1 = a.x * cosY + a.z * sinY;
            float z1 = -a.x * sinY + a.z * cosY;

            // Rotate around X (pitch)
            float y2 = a.y * cosX - z1 * sinX;
            float z2 = a.y * sinX + z1 * cosX;

            ProjectedAtom pa = new ProjectedAtom();
            pa.atom = a;
            pa.screenX = cX + x1 * scale;
            pa.screenY = cY - y2 * scale; // Invert y for screen
            pa.depthZ = z2;

            // Scale radius based on physical picometer atomic radius and perspective
            float baseR = (a.radiusPm / 100f) * scale * 0.26f;
            baseR = Math.max(dpToPx(13), Math.min(baseR, dpToPx(38)));
            pa.screenRadius = baseR;

            pAtoms.add(pa);
        }

        List<ProjectedBond> pBonds = new ArrayList<>();
        for (Bond3D b : currentMolecule.bonds) {
            if (b.fromAtomIndex >= pAtoms.size() || b.toAtomIndex >= pAtoms.size()) continue;
            ProjectedBond pb = new ProjectedBond();
            pb.bond = b;
            pb.p1 = pAtoms.get(b.fromAtomIndex);
            pb.p2 = pAtoms.get(b.toAtomIndex);
            pb.depthZ = (pb.p1.depthZ + pb.p2.depthZ) / 2f;
            pBonds.add(pb);
        }

        // Painter's Algorithm: Sort items from back (lowest depthZ) to front (highest depthZ)
        List<Object> renderQueue = new ArrayList<>();
        renderQueue.addAll(pBonds);
        renderQueue.addAll(pAtoms);

        Collections.sort(renderQueue, (o1, o2) -> {
            float z1 = (o1 instanceof ProjectedAtom) ? ((ProjectedAtom) o1).depthZ : ((ProjectedBond) o1).depthZ;
            float z2 = (o2 instanceof ProjectedAtom) ? ((ProjectedAtom) o2).depthZ : ((ProjectedBond) o2).depthZ;
            return Float.compare(z1, z2);
        });

        // Render sorted elements
        for (Object obj : renderQueue) {
            if (obj instanceof ProjectedBond) {
                draw3DBond(canvas, (ProjectedBond) obj);
            } else if (obj instanceof ProjectedAtom) {
                draw3DAtom(canvas, (ProjectedAtom) obj);
            }
        }
    }

    private void draw3DBond(Canvas canvas, ProjectedBond pb) {
        float x1 = pb.p1.screenX;
        float y1 = pb.p1.screenY;
        float x2 = pb.p2.screenX;
        float y2 = pb.p2.screenY;

        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float) Math.hypot(dx, dy);
        if (len == 0) return;

        float nx = -dy / len;
        float ny = dx / len;
        float offset = dpToPx(3.5f);

        // Realistic bond cylinder with 2-color gradient between from/to atoms
        LinearGradient grad = new LinearGradient(x1, y1, x2, y2,
                pb.p1.atom.color, pb.p2.atom.color, Shader.TileMode.CLAMP);

        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setShader(grad);
        p.setStrokeCap(Paint.Cap.ROUND);

        if (pb.bond.bondOrder == 1) {
            p.setStrokeWidth(dpToPx(5.5f));
            canvas.drawLine(x1, y1, x2, y2, p);
        } else if (pb.bond.bondOrder == 2) {
            p.setStrokeWidth(dpToPx(3.5f));
            canvas.drawLine(x1 + nx * offset, y1 + ny * offset, x2 + nx * offset, y2 + ny * offset, p);
            canvas.drawLine(x1 - nx * offset, y1 - ny * offset, x2 - nx * offset, y2 - ny * offset, p);
        } else if (pb.bond.bondOrder == 3) {
            p.setStrokeWidth(dpToPx(3.0f));
            canvas.drawLine(x1, y1, x2, y2, p);
            canvas.drawLine(x1 + nx * offset * 1.5f, y1 + ny * offset * 1.5f, x2 + nx * offset * 1.5f, y2 + ny * offset * 1.5f, p);
            canvas.drawLine(x1 - nx * offset * 1.5f, y1 - ny * offset * 1.5f, x2 - nx * offset * 1.5f, y2 - ny * offset * 1.5f, p);
        } else {
            // Ionic electrostatic coordination
            p.setStrokeWidth(dpToPx(2.5f));
            p.setPathEffect(new DashPathEffect(new float[]{dpToPx(5), dpToPx(4)}, 0));
            canvas.drawLine(x1, y1, x2, y2, p);
        }
    }

    private void draw3DAtom(Canvas canvas, ProjectedAtom pa) {
        float x = pa.screenX;
        float y = pa.screenY;
        float r = pa.screenRadius;

        // Radial gradient for 3D sphere specular illumination (light source at top-left)
        int baseColor = pa.atom.color;
        int highlightColor = lightenColor(baseColor, 0.45f);
        int shadowColor = darkenColor(baseColor, 0.4f);

        RadialGradient sphereShader = new RadialGradient(
                x - r * 0.35f, y - r * 0.35f, r * 1.25f,
                new int[]{highlightColor, baseColor, shadowColor},
                new float[]{0.0f, 0.55f, 1.0f},
                Shader.TileMode.CLAMP);

        atomPaint.setShader(sphereShader);
        canvas.drawCircle(x, y, r, atomPaint);
        atomPaint.setShader(null);

        // Selection highlight ring
        if (pa.atom == selectedAtom) {
            canvas.drawCircle(x, y, r + dpToPx(6), selectionGlowPaint);
        }

        // Atom Symbol and Labels
        if (showLabels) {
            textPaint.setColor(isColorDark(baseColor) ? 0xFFFFFFFF : 0xFF101010);
            textPaint.setTextSize(Math.max(spToPx(9), r * 0.65f));
            canvas.drawText(pa.atom.symbol, x, y + textPaint.getTextSize() * 0.35f, textPaint);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MODE 3: LEWIS STRUCTURE
    // ─────────────────────────────────────────────────────────────────────────

    private void drawLewisStructure(Canvas canvas, int w, int h) {
        float cX = w / 2f + panX;
        float cY = h / 2f + panY;
        float scale = Math.min(w, h) * 0.22f * scaleFactor;

        // 1. Draw Covalent Bond Lines (Dash or Pairs)
        for (Bond3D bond : currentMolecule.bonds) {
            if (bond.fromAtomIndex >= currentMolecule.atoms.size() ||
                bond.toAtomIndex >= currentMolecule.atoms.size()) continue;

            Atom3D a1 = currentMolecule.atoms.get(bond.fromAtomIndex);
            Atom3D a2 = currentMolecule.atoms.get(bond.toAtomIndex);

            float x1 = cX + a1.x2d * scale;
            float y1 = cY + a1.y2d * scale;
            float x2 = cX + a2.x2d * scale;
            float y2 = cY + a2.y2d * scale;

            draw2DBondLine(canvas, x1, y1, x2, y2, bond.bondOrder);
        }

        // 2. Draw Atom Symbols & Lone Pairs
        for (int i = 0; i < currentMolecule.atoms.size(); i++) {
            Atom3D atom = currentMolecule.atoms.get(i);
            float ax = cX + atom.x2d * scale;
            float ay = cY + atom.y2d * scale;

            // Background pill for contrast
            float pillR = dpToPx(16);
            Paint pillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            pillPaint.setColor(0xE60A1828);
            canvas.drawCircle(ax, ay, pillR, pillPaint);

            // Selection indicator
            if (atom == selectedAtom) {
                canvas.drawCircle(ax, ay, pillR + dpToPx(4), selectionGlowPaint);
            }

            // Chemical Symbol
            textPaint.setColor(atom.color == 0xFFFFFFFF ? 0xFF00FFCC : atom.color);
            textPaint.setTextSize(spToPx(14));
            canvas.drawText(atom.symbol, ax, ay + spToPx(5), textPaint);

            // Formal Charge badge (e.g. [+], [-])
            if (atom.formalCharge != 0) {
                String chargeStr = atom.formalCharge > 0 ? "+" : "−";
                if (Math.abs(atom.formalCharge) > 1) {
                    chargeStr = (atom.formalCharge > 0 ? "+" : "−") + Math.abs(atom.formalCharge);
                }
                textPaint.setTextSize(spToPx(10));
                textPaint.setColor(0xFFFFD54F);
                canvas.drawText(chargeStr, ax + pillR + dpToPx(3), ay - pillR * 0.5f, textPaint);
            }
        }

        // 3. Draw Lone Pair Electron Dots
        float dotR = dpToPx(2.4f);
        float dotSeparation = dpToPx(4.0f);
        float distFromCenter = dpToPx(21f);

        for (LonePair3D lp : currentMolecule.lonePairs) {
            if (lp.atomIndex >= currentMolecule.atoms.size()) continue;
            Atom3D atom = currentMolecule.atoms.get(lp.atomIndex);
            float ax = cX + atom.x2d * scale;
            float ay = cY + atom.y2d * scale;

            double angleRad = Math.toRadians(lp.angleDeg2D);
            float midX = ax + (float) Math.cos(angleRad) * distFromCenter;
            float midY = ay + (float) Math.sin(angleRad) * distFromCenter;

            // Perpendicular vector for the pair of dots
            float perpX = -(float) Math.sin(angleRad) * dotSeparation;
            float perpY = (float) Math.cos(angleRad) * dotSeparation;

            // Draw paired electron dots
            canvas.drawCircle(midX + perpX, midY + perpY, dotR, lonePairPaint);
            canvas.drawCircle(midX - perpX, midY - perpY, dotR, lonePairPaint);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MODE 4: POLARITY & DIPOLE VISUALIZATION
    // ─────────────────────────────────────────────────────────────────────────

    private void drawPolarityVisualization(Canvas canvas, int w, int h) {
        // Draw 2D structural base
        draw2DStructure(canvas, w, h);

        float cX = w / 2f + panX;
        float cY = h / 2f + panY;
        float scale = Math.min(w, h) * 0.22f * scaleFactor;

        // 1. Partial Charge Badges (δ⁺, δ⁻) on Atoms
        Paint chargePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        chargePaint.setTextSize(spToPx(11));
        chargePaint.setTypeface(Typeface.DEFAULT_BOLD);

        for (Atom3D atom : currentMolecule.atoms) {
            float ax = cX + atom.x2d * scale;
            float ay = cY + atom.y2d * scale;
            float offset = dpToPx(24);

            if (atom.partialCharge > 0.05f) {
                chargePaint.setColor(0xFF00E5FF); // δ⁺ Cyan
                canvas.drawText("δ⁺", ax + offset * 0.7f, ay - offset * 0.6f, chargePaint);
            } else if (atom.partialCharge < -0.05f) {
                chargePaint.setColor(0xFFFF5252); // δ⁻ Red/Coral
                canvas.drawText("δ⁻", ax + offset * 0.7f, ay - offset * 0.6f, chargePaint);
            }
        }

        // 2. Bond Dipole Moment Arrows (from δ⁺ to δ⁻ with crossed tail)
        for (Bond3D bond : currentMolecule.bonds) {
            if (bond.fromAtomIndex >= currentMolecule.atoms.size() ||
                bond.toAtomIndex >= currentMolecule.atoms.size()) continue;

            Atom3D a1 = currentMolecule.atoms.get(bond.fromAtomIndex);
            Atom3D a2 = currentMolecule.atoms.get(bond.toAtomIndex);

            // Determine positive and negative ends
            Atom3D pos = a1.partialCharge > a2.partialCharge ? a1 : a2;
            Atom3D neg = a1.partialCharge > a2.partialCharge ? a2 : a1;

            if (Math.abs(pos.partialCharge - neg.partialCharge) >= 0.15f) {
                float x1 = cX + pos.x2d * scale;
                float y1 = cY + pos.y2d * scale;
                float x2 = cX + neg.x2d * scale;
                float y2 = cY + neg.y2d * scale;

                drawCrossedDipoleArrow(canvas, x1, y1, x2, y2, 0x8800FFCC);
            }
        }

        // 3. Prominent Net Molecular Dipole Vector
        if (currentMolecule.isPolar && currentMolecule.dipoleDebye > 0.05f) {
            float vecLen = dpToPx(70);
            float startX = cX - currentMolecule.netDipoleX * (vecLen * 0.5f);
            float startY = cY + currentMolecule.netDipoleY * (vecLen * 0.5f); // inverting screen y
            float endX = cX + currentMolecule.netDipoleX * (vecLen * 0.5f);
            float endY = cY - currentMolecule.netDipoleY * (vecLen * 0.5f);

            drawMajorNetDipoleArrow(canvas, startX, startY, endX, endY, currentMolecule.dipoleDebye);
        } else {
            // Symmetric non-polar announcement banner
            Paint nonPolarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            nonPolarPaint.setColor(0xFF88AABB);
            nonPolarPaint.setTextSize(spToPx(11));
            nonPolarPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("⚡ SYMMETRICAL NON-POLAR (Net Dipole μ = 0.00 D)", w / 2f, h - dpToPx(28), nonPolarPaint);
        }
    }

    private void drawCrossedDipoleArrow(Canvas canvas, float x1, float y1, float x2, float y2, int color) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float) Math.hypot(dx, dy);
        if (len < dpToPx(20)) return;

        // Offset parallel from bond center
        float nx = -dy / len;
        float ny = dx / len;
        float sideOffset = dpToPx(14);

        float sx = x1 + nx * sideOffset + (dx / len) * dpToPx(12);
        float sy = y1 + ny * sideOffset + (dy / len) * dpToPx(12);
        float ex = x2 + nx * sideOffset - (dx / len) * dpToPx(12);
        float ey = y2 + ny * sideOffset - (dy / len) * dpToPx(12);

        Paint arrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arrowPaint.setColor(color);
        arrowPaint.setStyle(Paint.Style.STROKE);
        arrowPaint.setStrokeWidth(dpToPx(2.0f));

        // Shaft
        canvas.drawLine(sx, sy, ex, ey, arrowPaint);

        // Crossed tail at positive end (+)
        float tailCross = dpToPx(5);
        canvas.drawLine(sx + nx * tailCross, sy + ny * tailCross, sx - nx * tailCross, sy - ny * tailCross, arrowPaint);

        // Arrow head pointing to negative end
        float headLen = dpToPx(6);
        float headAngle = (float) Math.toRadians(25);
        float arrowAngle = (float) Math.atan2(ey - sy, ex - sx);

        Path head = new Path();
        head.moveTo(ex, ey);
        head.lineTo(ex - (float) Math.cos(arrowAngle - headAngle) * headLen,
                    ey - (float) Math.sin(arrowAngle - headAngle) * headLen);
        head.lineTo(ex - (float) Math.cos(arrowAngle + headAngle) * headLen,
                    ey - (float) Math.sin(arrowAngle + headAngle) * headLen);
        head.close();

        Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        fill.setColor(color);
        fill.setStyle(Paint.Style.FILL);
        canvas.drawPath(head, fill);
    }

    private void drawMajorNetDipoleArrow(Canvas canvas, float sx, float sy, float ex, float ey, float debye) {
        dipolePaint.setColor(0xFFFFD54F);
        dipolePaint.setStrokeWidth(dpToPx(3.5f));

        // Shaft
        canvas.drawLine(sx, sy, ex, ey, dipolePaint);

        // Crossed + tail
        float dx = ex - sx;
        float dy = ey - sy;
        float len = (float) Math.hypot(dx, dy);
        if (len == 0) return;
        float nx = -dy / len;
        float ny = dx / len;

        float crossLen = dpToPx(8);
        canvas.drawLine(sx + nx * crossLen, sy + ny * crossLen, sx - nx * crossLen, sy - ny * crossLen, dipolePaint);

        // Glowing arrow head
        float headLen = dpToPx(14);
        float headAngle = (float) Math.toRadians(28);
        float arrowAngle = (float) Math.atan2(dy, dx);

        Path head = new Path();
        head.moveTo(ex, ey);
        head.lineTo(ex - (float) Math.cos(arrowAngle - headAngle) * headLen,
                    ey - (float) Math.sin(arrowAngle - headAngle) * headLen);
        head.lineTo(ex - (float) Math.cos(arrowAngle + headAngle) * headLen,
                    ey - (float) Math.sin(arrowAngle + headAngle) * headLen);
        head.close();

        dipoleArrowPaint.setColor(0xFFFFD54F);
        canvas.drawPath(head, dipoleArrowPaint);

        // Net Dipole Label
        Paint netLabel = new Paint(Paint.ANTI_ALIAS_FLAG);
        netLabel.setColor(0xFFFFD54F);
        netLabel.setTextSize(spToPx(11));
        netLabel.setTypeface(Typeface.DEFAULT_BOLD);
        netLabel.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("NET DIPOLE: μ = " + debye + " D", ex, ey - dpToPx(12), netLabel);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HUD LABELS & CORNER TELEMETRY OVERLAYS
    // ─────────────────────────────────────────────────────────────────────────

    private void drawHudOverlays(Canvas canvas, int w, int h) {
        if (currentMolecule == null) return;

        // Top-left: Molecule Name & Formula
        hudTextPaint.setColor(0xFF00E5FF);
        hudTextPaint.setTextSize(spToPx(11));
        hudTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
        canvas.drawText(currentMolecule.formula + "  •  " + currentMolecule.chemicalName, dpToPx(16), dpToPx(24), hudTextPaint);

        hudTextPaint.setColor(0xFF88AABB);
        hudTextPaint.setTextSize(spToPx(9.5f));
        hudTextPaint.setTypeface(Typeface.MONOSPACE);
        canvas.drawText("GEOMETRY: " + currentMolecule.molecularGeometry, dpToPx(16), dpToPx(38), hudTextPaint);

        // Top-right: Current View Mode Badge
        String modeName = "3D BALL & STICK";
        if (currentMode == MODE_2D) modeName = "2D STRUCTURE";
        else if (currentMode == MODE_LEWIS) modeName = "LEWIS ELECTRON DOT";
        else if (currentMode == MODE_POLARITY) modeName = "POLARITY & DIPOLE";

        Paint badgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        badgePaint.setColor(0xFF00FFCC);
        badgePaint.setTextSize(spToPx(9.5f));
        badgePaint.setTypeface(Typeface.DEFAULT_BOLD);
        float badgeWidth = badgePaint.measureText(modeName);

        RectF badgeRect = new RectF(w - badgeWidth - dpToPx(24), dpToPx(12), w - dpToPx(12), dpToPx(32));
        Paint badgeBg = new Paint(Paint.ANTI_ALIAS_FLAG);
        badgeBg.setColor(0x2800FFCC);
        canvas.drawRoundRect(badgeRect, dpToPx(4), dpToPx(4), badgeBg);

        Paint badgeBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
        badgeBorder.setStyle(Paint.Style.STROKE);
        badgeBorder.setColor(0x8800FFCC);
        badgeBorder.setStrokeWidth(dpToPx(1));
        canvas.drawRoundRect(badgeRect, dpToPx(4), dpToPx(4), badgeBorder);

        canvas.drawText(modeName, badgeRect.left + dpToPx(6), badgeRect.centerY() + spToPx(3.5f), badgePaint);

        // Bottom-left: Tap atom hint
        hudTextPaint.setColor(0x886088B0);
        hudTextPaint.setTextSize(spToPx(8.5f));
        canvas.drawText("TOUCH: DRAG TO ROTATE • PINCH TO ZOOM • TAP ATOM FOR INTEL", dpToPx(16), h - dpToPx(12), hudTextPaint);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // COLOR HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private static boolean isColorDark(int color) {
        double darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
        return darkness >= 0.45;
    }

    private static int lightenColor(int color, float fraction) {
        int r = Math.min(255, (int) (Color.red(color) + (255 - Color.red(color)) * fraction));
        int g = Math.min(255, (int) (Color.green(color) + (255 - Color.green(color)) * fraction));
        int b = Math.min(255, (int) (Color.blue(color) + (255 - Color.blue(color)) * fraction));
        return Color.rgb(r, g, b);
    }

    private static int darkenColor(int color, float fraction) {
        int r = Math.max(0, (int) (Color.red(color) * (1 - fraction)));
        int g = Math.max(0, (int) (Color.green(color) * (1 - fraction)));
        int b = Math.max(0, (int) (Color.blue(color) * (1 - fraction)));
        return Color.rgb(r, g, b);
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    private float spToPx(float sp) {
        return sp * getResources().getDisplayMetrics().scaledDensity;
    }
}
