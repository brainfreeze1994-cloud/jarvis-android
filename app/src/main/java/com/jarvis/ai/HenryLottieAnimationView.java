package com.jarvis.ai;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;

import java.lang.reflect.Method;

/**
 * HenryLottieAnimationView
 *
 * Resilient, high-performance Lottie animation component for H.E.N.R.Y.
 * Provides instant vector animations (loading spinners, success checkmarks,
 * neural pulses, voice waveforms, typing dots, and cybernetic loaders).
 *
 * Decoupled from hard compile-time Lottie dependencies to guarantee 100% build
 * resilience across all environments, with hardware-accelerated Canvas fallback
 * and dynamic reflection for com.airbnb.android.LottieAnimationView when present.
 */
public class HenryLottieAnimationView extends FrameLayout {

    private static final String TAG = "HenryLottieView";
    public static final int INFINITE = -1;

    public enum Preset {
        TYPING_DOTS("lottie/typing_dots.json"),
        NEURAL_PULSE("lottie/neural_pulse.json"),
        VOICE_WAVE("lottie/voice_wave.json"),
        CYBER_LOADER("lottie/cyber_loader.json"),
        LOADING_SPINNER("lottie/loading_spinner.json"),
        SUCCESS_CHECK("lottie/success_check.json");

        private final String assetPath;
        Preset(String assetPath) {
            this.assetPath = assetPath;
        }
        public String getAssetPath() {
            return assetPath;
        }
    }

    private Preset currentPreset = null;
    private View lottieDelegate = null;
    private CanvasFallbackView canvasFallback = null;
    private boolean isLottieAvailable = false;
    private float playbackSpeed = 1.0f;
    private boolean isCurrentlyAnimating = false;

    public HenryLottieAnimationView(Context context) {
        super(context);
        init(context, null);
    }

    public HenryLottieAnimationView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public HenryLottieAnimationView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        // Try initializing reflection-based Lottie delegate
        try {
            Class<?> lottieClass = Class.forName("com.airbnb.android.LottieAnimationView");
            Object lottieObj = lottieClass.getConstructor(Context.class, AttributeSet.class)
                    .newInstance(context, attrs);
            if (lottieObj instanceof View) {
                lottieDelegate = (View) lottieObj;
                LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
                lp.gravity = Gravity.CENTER;
                addView(lottieDelegate, lp);

                // Configure Lottie settings via reflection
                try {
                    Method repeatMethod = lottieClass.getMethod("setRepeatCount", int.class);
                    repeatMethod.invoke(lottieDelegate, INFINITE);
                } catch (Exception ignored) {}

                try {
                    Method mergeMethod = lottieClass.getMethod("enableMergePathsForKitKatAndAbove", boolean.class);
                    mergeMethod.invoke(lottieDelegate, true);
                } catch (Exception ignored) {}

                isLottieAvailable = true;
            }
        } catch (Throwable t) {
            // Lottie library not on classpath or failed to load
            isLottieAvailable = false;
            lottieDelegate = null;
        }

        // Always initialize canvas fallback view
        canvasFallback = new CanvasFallbackView(context);
        LayoutParams canvasLp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        canvasLp.gravity = Gravity.CENTER;
        addView(canvasFallback, canvasLp);

        if (isLottieAvailable && lottieDelegate != null) {
            canvasFallback.setVisibility(View.GONE);
            lottieDelegate.setVisibility(View.VISIBLE);
        } else {
            canvasFallback.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Play a built-in lightweight animation preset.
     */
    public void playPreset(Preset preset) {
        if (preset == null) return;
        this.currentPreset = preset;
        this.isCurrentlyAnimating = true;

        boolean lottieSuccess = false;
        if (isLottieAvailable && lottieDelegate != null) {
            try {
                Class<?> cls = lottieDelegate.getClass();
                Method setAnimMethod = cls.getMethod("setAnimation", String.class);
                setAnimMethod.invoke(lottieDelegate, preset.getAssetPath());

                if (preset == Preset.SUCCESS_CHECK) {
                    Method repeatMethod = cls.getMethod("setRepeatCount", int.class);
                    repeatMethod.invoke(lottieDelegate, 0);
                } else {
                    Method repeatMethod = cls.getMethod("setRepeatCount", int.class);
                    repeatMethod.invoke(lottieDelegate, INFINITE);
                }

                Method playMethod = cls.getMethod("playAnimation");
                playMethod.invoke(lottieDelegate);

                lottieDelegate.setVisibility(View.VISIBLE);
                if (canvasFallback != null) canvasFallback.setVisibility(View.GONE);
                lottieSuccess = true;
            } catch (Throwable e) {
                Log.w(TAG, "Lottie delegate failed, falling back to native Canvas: " + e.getMessage());
                lottieSuccess = false;
            }
        }

        if (!lottieSuccess) {
            if (lottieDelegate != null) lottieDelegate.setVisibility(View.GONE);
            if (canvasFallback != null) {
                canvasFallback.setVisibility(View.VISIBLE);
                canvasFallback.setPreset(preset, playbackSpeed);
                canvasFallback.start();
            }
        }
    }

    /**
     * Play a preset by name (case-insensitive: "typing", "pulse", "voice", "loader", "spinner", "success").
     */
    public void playPreset(String name) {
        if (name == null) return;
        String lower = name.toLowerCase().trim();
        if (lower.contains("type") || lower.contains("dot") || lower.contains("think")) {
            playPreset(Preset.TYPING_DOTS);
        } else if (lower.contains("voice") || lower.contains("wave") || lower.contains("speak") || lower.contains("audio")) {
            playPreset(Preset.VOICE_WAVE);
        } else if (lower.contains("pulse") || lower.contains("radar") || lower.contains("neural")) {
            playPreset(Preset.NEURAL_PULSE);
        } else if (lower.contains("spinner") || lower.contains("spin")) {
            playPreset(Preset.LOADING_SPINNER);
        } else if (lower.contains("check") || lower.contains("success") || lower.contains("done") || lower.contains("ok")) {
            playPreset(Preset.SUCCESS_CHECK);
        } else if (lower.contains("load") || lower.contains("cyber")) {
            playPreset(Preset.CYBER_LOADER);
        } else {
            playPreset(Preset.LOADING_SPINNER);
        }
    }

    public void playThinkingDots() {
        playPreset(Preset.TYPING_DOTS);
    }

    public void playNeuralPulse() {
        playPreset(Preset.NEURAL_PULSE);
    }

    public void playVoiceWave() {
        playPreset(Preset.VOICE_WAVE);
    }

    public void playCyberLoader() {
        playPreset(Preset.CYBER_LOADER);
    }

    public void playLoadingSpinner() {
        playPreset(Preset.LOADING_SPINNER);
    }

    public void playSuccessCheck() {
        playPreset(Preset.SUCCESS_CHECK);
    }

    public void setAnimation(String assetPath) {
        if (assetPath == null) return;
        String lower = assetPath.toLowerCase();
        if (lower.contains("typing") || lower.contains("dots")) {
            playPreset(Preset.TYPING_DOTS);
        } else if (lower.contains("voice") || lower.contains("wave")) {
            playPreset(Preset.VOICE_WAVE);
        } else if (lower.contains("pulse") || lower.contains("neural")) {
            playPreset(Preset.NEURAL_PULSE);
        } else if (lower.contains("check") || lower.contains("success")) {
            playPreset(Preset.SUCCESS_CHECK);
        } else if (lower.contains("loader") || lower.contains("cyber")) {
            playPreset(Preset.CYBER_LOADER);
        } else {
            playPreset(Preset.LOADING_SPINNER);
        }
    }

    public void playAnimation() {
        if (currentPreset != null) {
            playPreset(currentPreset);
        } else {
            playLoadingSpinner();
        }
    }

    public void cancelAnimation() {
        stopAndReset();
    }

    public void setProgress(float progress) {
        if (lottieDelegate != null && isLottieAvailable) {
            try {
                Method m = lottieDelegate.getClass().getMethod("setProgress", float.class);
                m.invoke(lottieDelegate, progress);
            } catch (Exception ignored) {}
        }
        if (canvasFallback != null) {
            canvasFallback.setProgress(progress);
        }
    }

    public void setRepeatCount(int count) {
        if (lottieDelegate != null && isLottieAvailable) {
            try {
                Method m = lottieDelegate.getClass().getMethod("setRepeatCount", int.class);
                m.invoke(lottieDelegate, count);
            } catch (Exception ignored) {}
        }
    }

    public void pauseAnimation() {
        isCurrentlyAnimating = false;
        if (lottieDelegate != null && isLottieAvailable) {
            try {
                Method m = lottieDelegate.getClass().getMethod("pauseAnimation");
                m.invoke(lottieDelegate);
            } catch (Exception ignored) {}
        }
        if (canvasFallback != null) {
            canvasFallback.pause();
        }
    }

    public void resumeAnimation() {
        isCurrentlyAnimating = true;
        if (lottieDelegate != null && isLottieAvailable) {
            try {
                Method m = lottieDelegate.getClass().getMethod("resumeAnimation");
                m.invoke(lottieDelegate);
            } catch (Exception ignored) {}
        }
        if (canvasFallback != null) {
            canvasFallback.resume();
        }
    }

    public boolean isAnimating() {
        if (lottieDelegate != null && isLottieAvailable) {
            try {
                Method m = lottieDelegate.getClass().getMethod("isAnimating");
                Object res = m.invoke(lottieDelegate);
                if (res instanceof Boolean) return (Boolean) res;
            } catch (Exception ignored) {}
        }
        return isCurrentlyAnimating;
    }

    /**
     * Stop and reset progress.
     */
    public void stopAndReset() {
        isCurrentlyAnimating = false;
        if (lottieDelegate != null && isLottieAvailable) {
            try {
                Method cancelM = lottieDelegate.getClass().getMethod("cancelAnimation");
                cancelM.invoke(lottieDelegate);
                Method setProgM = lottieDelegate.getClass().getMethod("setProgress", float.class);
                setProgM.invoke(lottieDelegate, 0f);
            } catch (Exception ignored) {}
        }
        if (canvasFallback != null) {
            canvasFallback.stop();
        }
    }

    /**
     * Set playback speed dynamically (e.g. 1.5f for faster thinking or audio pitch).
     */
    public void setDynamicSpeed(float speed) {
        this.playbackSpeed = speed;
        if (lottieDelegate != null && isLottieAvailable) {
            try {
                Method m = lottieDelegate.getClass().getMethod("setSpeed", float.class);
                m.invoke(lottieDelegate, speed);
            } catch (Exception ignored) {}
        }
        if (canvasFallback != null) {
            canvasFallback.setSpeed(speed);
        }
    }

    public void setSpeed(float speed) {
        setDynamicSpeed(speed);
    }

    public Preset getCurrentPreset() {
        return currentPreset;
    }

    // ── Native Hardware-Accelerated 60fps Canvas Fallback View ───────────────
    private static class CanvasFallbackView extends View {
        private Preset preset = Preset.LOADING_SPINNER;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF arcBounds = new RectF();
        private final Path checkPath = new Path();

        private ValueAnimator animator;
        private float animatedFraction = 0f;
        private float speed = 1.0f;
        private boolean isRunning = false;

        public CanvasFallbackView(Context context) {
            super(context);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
            trackPaint.setStyle(Paint.Style.STROKE);
            glowPaint.setStyle(Paint.Style.STROKE);
        }

        public void setPreset(Preset preset, float speed) {
            this.preset = preset;
            this.speed = speed;
        }

        public void setSpeed(float speed) {
            this.speed = speed;
            if (animator != null && animator.isRunning()) {
                long duration = (long) (1200 / Math.max(0.2f, speed));
                animator.setDuration(duration);
            }
        }

        public void setProgress(float progress) {
            this.animatedFraction = progress;
            invalidate();
        }

        public void start() {
            stop();
            isRunning = true;
            long duration = (long) (1200 / Math.max(0.2f, speed));
            if (preset == Preset.SUCCESS_CHECK) {
                duration = 900;
            }
            animator = ValueAnimator.ofFloat(0f, 1f);
            animator.setDuration(duration);
            animator.setInterpolator(new LinearInterpolator());
            if (preset != Preset.SUCCESS_CHECK) {
                animator.setRepeatCount(ValueAnimator.INFINITE);
            } else {
                animator.setRepeatCount(0);
            }
            animator.addUpdateListener(animation -> {
                animatedFraction = (float) animation.getAnimatedValue();
                invalidate();
            });
            animator.start();
        }

        public void stop() {
            isRunning = false;
            if (animator != null) {
                animator.cancel();
                animator = null;
            }
            animatedFraction = 0f;
            invalidate();
        }

        public void pause() {
            if (animator != null) {
                animator.pause();
            }
            isRunning = false;
        }

        public void resume() {
            if (animator != null) {
                animator.resume();
                isRunning = true;
            } else {
                start();
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) return;

            float cx = w / 2f;
            float cy = h / 2f;
            float size = Math.min(w, h);

            if (preset == Preset.LOADING_SPINNER) {
                drawLoadingSpinner(canvas, cx, cy, size);
            } else if (preset == Preset.SUCCESS_CHECK) {
                drawSuccessCheck(canvas, cx, cy, size);
            } else if (preset == Preset.TYPING_DOTS) {
                drawTypingDots(canvas, cx, cy, size);
            } else if (preset == Preset.VOICE_WAVE) {
                drawVoiceWave(canvas, cx, cy, size);
            } else if (preset == Preset.CYBER_LOADER) {
                drawCyberLoader(canvas, cx, cy, size);
            } else {
                drawNeuralPulse(canvas, cx, cy, size);
            }
        }

        private void drawLoadingSpinner(Canvas canvas, float cx, float cy, float size) {
            float radius = size * 0.38f;
            arcBounds.set(cx - radius, cy - radius, cx + radius, cy + radius);

            // Track
            trackPaint.setColor(0x3300E5FF);
            trackPaint.setStrokeWidth(size * 0.07f);
            canvas.drawCircle(cx, cy, radius, trackPaint);

            // Spinning Arc
            float startAngle = (animatedFraction * 360f * 2f) % 360f;
            float sweepAngle = 75f + (float) Math.sin(animatedFraction * Math.PI * 2) * 45f;
            paint.setColor(0xFF00E5FF);
            paint.setStrokeWidth(size * 0.08f);
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawArc(arcBounds, startAngle, sweepAngle, false, paint);
        }

        private void drawSuccessCheck(Canvas canvas, float cx, float cy, float size) {
            float radius = size * 0.38f;

            // Success Circle
            paint.setColor(0xFF00E676);
            paint.setStrokeWidth(size * 0.07f);
            paint.setStyle(Paint.Style.STROKE);

            float circleFraction = Math.min(1f, animatedFraction * 1.5f);
            canvas.drawArc(new RectF(cx - radius, cy - radius, cx + radius, cy + radius),
                    -90f, 360f * circleFraction, false, paint);

            // Checkmark
            if (animatedFraction > 0.3f) {
                float checkFraction = Math.min(1f, (animatedFraction - 0.3f) / 0.7f);
                paint.setStrokeWidth(size * 0.08f);
                checkPath.reset();
                float x1 = cx - radius * 0.45f;
                float y1 = cy + radius * 0.05f;
                float x2 = cx - radius * 0.1f;
                float y2 = cy + radius * 0.42f;
                float x3 = cx + radius * 0.5f;
                float y3 = cy - radius * 0.35f;

                checkPath.moveTo(x1, y1);
                if (checkFraction < 0.45f) {
                    float f = checkFraction / 0.45f;
                    checkPath.lineTo(x1 + (x2 - x1) * f, y1 + (y2 - y1) * f);
                } else {
                    checkPath.lineTo(x2, y2);
                    float f = (checkFraction - 0.45f) / 0.55f;
                    checkPath.lineTo(x2 + (x3 - x2) * f, y2 + (y3 - y2) * f);
                }
                canvas.drawPath(checkPath, paint);
            }
        }

        private void drawTypingDots(Canvas canvas, float cx, float cy, float size) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xFF00E5FF);
            float dotRadius = size * 0.09f;
            float spacing = size * 0.28f;

            for (int i = 0; i < 3; i++) {
                float phase = (animatedFraction + i * 0.25f) % 1f;
                float bounce = (float) Math.sin(phase * Math.PI) * (size * 0.14f);
                float x = cx + (i - 1) * spacing;
                float y = cy - Math.max(0, bounce);
                int alpha = (int) (120 + Math.max(0, bounce / (size * 0.14f)) * 135);
                paint.setAlpha(alpha);
                canvas.drawCircle(x, y, dotRadius, paint);
            }
        }

        private void drawVoiceWave(Canvas canvas, float cx, float cy, float size) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xFF00E5FF);
            float barWidth = size * 0.08f;
            float spacing = size * 0.15f;
            int barCount = 5;
            float totalW = barCount * spacing;

            for (int i = 0; i < barCount; i++) {
                float phase = (animatedFraction * 2f + i * 0.35f) % 1f;
                float barHeight = size * 0.15f + (float) Math.abs(Math.sin(phase * Math.PI)) * (size * 0.5f);
                float x = cx - totalW / 2f + i * spacing + spacing / 2f;
                paint.setAlpha(180 + (i % 2) * 75);
                canvas.drawRoundRect(x - barWidth / 2f, cy - barHeight / 2f,
                        x + barWidth / 2f, cy + barHeight / 2f, barWidth / 2f, barWidth / 2f, paint);
            }
        }

        private void drawCyberLoader(Canvas canvas, float cx, float cy, float size) {
            float r1 = size * 0.38f;
            float r2 = size * 0.26f;
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(size * 0.06f);

            // Outer ring
            paint.setColor(0xFF00E5FF);
            arcBounds.set(cx - r1, cy - r1, cx + r1, cy + r1);
            canvas.drawArc(arcBounds, animatedFraction * 360f, 120f, false, paint);
            canvas.drawArc(arcBounds, animatedFraction * 360f + 180f, 90f, false, paint);

            // Inner counter-rotating ring
            paint.setColor(0xFF00A2FF);
            paint.setStrokeWidth(size * 0.04f);
            arcBounds.set(cx - r2, cy - r2, cx + r2, cy + r2);
            canvas.drawArc(arcBounds, -animatedFraction * 360f * 1.5f, 150f, false, paint);
        }

        private void drawNeuralPulse(Canvas canvas, float cx, float cy, float size) {
            paint.setStyle(Paint.Style.STROKE);
            float maxR = size * 0.44f;

            for (int i = 0; i < 3; i++) {
                float progress = (animatedFraction + i * 0.33f) % 1f;
                float r = progress * maxR;
                int alpha = (int) ((1f - progress) * 220);
                paint.setColor(0xFF00E5FF);
                paint.setAlpha(alpha);
                paint.setStrokeWidth(size * 0.04f * (1f - progress * 0.5f));
                canvas.drawCircle(cx, cy, r, paint);
            }

            // Glowing center core
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xFF00E5FF);
            paint.setAlpha(240);
            canvas.drawCircle(cx, cy, size * 0.08f, paint);
        }
    }
}
