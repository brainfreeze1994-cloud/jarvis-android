package com.jarvis.ai;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.animation.ValueAnimator;
import android.view.animation.AccelerateDecelerateInterpolator;

import java.util.Locale;

/**
 * Breathing Exercise — animated guided breathing with orb.
 * Box breathing (4-4-4-4), 4-7-8, deep breathing.
 * Shows animated blue orb that expands/contracts.
 * "Breathing exercise" / "Help me relax" / "Box breathing" / "Calm me down"
 */
public class BreathingExercise {

    public static boolean isBreathingCommand(String text) {
        String lower = text.toLowerCase(Locale.US);
        return lower.contains("breath") || lower.contains("relax") ||
               lower.contains("calm") || lower.contains("meditat") ||
               lower.contains("stress relief") || lower.contains("box breathing") ||
               lower.contains("inhale") || lower.contains("help me relax") ||
               lower.contains("anxiety") && lower.contains("help") ||
               lower.contains("panic") && lower.contains("help");
    }

    public enum Mode {
        BOX("Box Breathing", new int[]{4,4,4,4},
            new String[]{"Inhale…","Hold…","Exhale…","Hold…"},
            "Inhale 4s → Hold 4s → Exhale 4s → Hold 4s. Repeat."),
        FOUR_SEVEN_EIGHT("4-7-8 Breathing", new int[]{4,7,8,0},
            new String[]{"Inhale…","Hold…","Exhale…",""},
            "Inhale 4s → Hold 7s → Exhale 8s. Deeply calming."),
        DEEP("Deep Breathing", new int[]{5,0,5,0},
            new String[]{"Inhale…","","Exhale…",""},
            "Slow, deep breath in and out. Simple and effective.");

        final String name;
        final int[]    seconds;
        final String[] phases;
        final String   description;
        Mode(String name, int[] seconds, String[] phases, String desc) {
            this.name = name; this.seconds = seconds;
            this.phases = phases; this.description = desc;
        }
    }

    private static class BreathOrb extends View {
        private final Paint paintRing  = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint paintFill  = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint paintOuter = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float scale = 0.5f;

        BreathOrb(Context ctx) {
            super(ctx);
            paintRing.setStyle(Paint.Style.STROKE);
            paintRing.setStrokeWidth(3f);
            paintRing.setColor(0xFF00BEFF);
            paintFill.setStyle(Paint.Style.FILL);
            paintFill.setColor(0xFF021828);
            paintOuter.setStyle(Paint.Style.STROKE);
            paintOuter.setStrokeWidth(1.5f);
            paintOuter.setColor(0xFF004466);
        }

        void setScale(float s) { this.scale = s; invalidate(); }

        @Override protected void onDraw(Canvas canvas) {
            float cx = getWidth() / 2f, cy = getHeight() / 2f;
            float maxR = Math.min(cx, cy) * 0.9f;
            float r = maxR * scale;
            // Outer static ring
            paintOuter.setAlpha(120);
            canvas.drawCircle(cx, cy, maxR, paintOuter);
            // Middle static ring
            paintOuter.setAlpha(60);
            canvas.drawCircle(cx, cy, maxR * 0.65f, paintOuter);
            // Animated fill
            paintFill.setColor(0xFF021828);
            canvas.drawCircle(cx, cy, r, paintFill);
            // Animated ring
            paintRing.setAlpha((int)(100 + 155 * scale));
            canvas.drawCircle(cx, cy, r, paintRing);
        }
    }

    public static void show(Context ctx, Mode mode, TextToSpeechCallback ttsCallback) {
        Dialog dialog = new Dialog(ctx, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);
        if (dialog.getWindow() != null)
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(0xFF020C1B);
        root.setPadding(32, 48, 32, 48);

        // Title
        TextView tvTitle = new TextView(ctx);
        tvTitle.setText(mode.name);
        tvTitle.setTextColor(0xFF00BEFF);
        tvTitle.setTextSize(20f);
        tvTitle.setGravity(Gravity.CENTER);
        tvTitle.setLetterSpacing(0.2f);
        root.addView(tvTitle);

        // Description
        TextView tvDesc = new TextView(ctx);
        tvDesc.setText(mode.description);
        tvDesc.setTextColor(0xFF004466);
        tvDesc.setTextSize(11f);
        tvDesc.setGravity(Gravity.CENTER);
        tvDesc.setPadding(0, 8, 0, 24);
        root.addView(tvDesc);

        // Orb
        BreathOrb orb = new BreathOrb(ctx);
        LinearLayout.LayoutParams orbLp = new LinearLayout.LayoutParams(280, 280);
        orb.setLayoutParams(orbLp);
        root.addView(orb);

        // Phase label
        TextView tvPhase = new TextView(ctx);
        tvPhase.setText("Get ready…");
        tvPhase.setTextColor(0xFF80DFFF);
        tvPhase.setTextSize(28f);
        tvPhase.setGravity(Gravity.CENTER);
        tvPhase.setPadding(0, 24, 0, 0);
        root.addView(tvPhase);

        // Countdown
        TextView tvCount = new TextView(ctx);
        tvCount.setTextColor(0xFF004466);
        tvCount.setTextSize(18f);
        tvCount.setGravity(Gravity.CENTER);
        tvCount.setPadding(0, 8, 0, 0);
        root.addView(tvCount);

        // Cycle counter
        TextView tvCycles = new TextView(ctx);
        tvCycles.setTextColor(0xFF003355);
        tvCycles.setTextSize(12f);
        tvCycles.setGravity(Gravity.CENTER);
        tvCycles.setPadding(0, 16, 0, 0);
        tvCycles.setText("Cycles: 0");
        root.addView(tvCycles);

        // Stop button
        TextView btnStop = new TextView(ctx);
        btnStop.setText("◆ STOP");
        btnStop.setTextColor(0xFF00BEFF);
        btnStop.setTextSize(14f);
        btnStop.setGravity(Gravity.CENTER);
        btnStop.setPadding(32, 20, 32, 20);
        btnStop.setBackgroundColor(0xFF041828);
        LinearLayout.LayoutParams stopLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        stopLp.topMargin = 32;
        btnStop.setLayoutParams(stopLp);
        btnStop.setOnClickListener(v -> dialog.dismiss());
        root.addView(btnStop);

        dialog.setContentView(root);
        dialog.show();

        // Animation engine
        Handler handler = new Handler(Looper.getMainLooper());
        final boolean[] running = {true};
        final int[] cycleCount = {0};

        dialog.setOnDismissListener(d -> running[0] = false);

        // Delay 1.5s before starting
        handler.postDelayed(() -> runCycle(ctx, handler, running, orb, tvPhase, tvCount, tvCycles,
            mode, cycleCount, ttsCallback), 1500);
    }

    private static void runCycle(Context ctx, Handler handler, boolean[] running,
                                  BreathOrb orb, TextView tvPhase, TextView tvCount,
                                  TextView tvCycles, Mode mode, int[] cycleCount,
                                  TextToSpeechCallback ttsCallback) {
        if (!running[0]) return;
        cycleCount[0]++;
        tvCycles.setText("Cycles: " + cycleCount[0]);
        runPhase(ctx, handler, running, orb, tvPhase, tvCount, tvCycles,
            mode, cycleCount, ttsCallback, 0, 0);
    }

    private static void runPhase(Context ctx, Handler handler, boolean[] running,
                                  BreathOrb orb, TextView tvPhase, TextView tvCount,
                                  TextView tvCycles, Mode mode, int[] cycleCount,
                                  TextToSpeechCallback ttsCallback,
                                  int phaseIdx, int elapsed) {
        if (!running[0]) return;
        if (phaseIdx >= mode.seconds.length) {
            runCycle(ctx, handler, running, orb, tvPhase, tvCount,
                tvCycles, mode, cycleCount, ttsCallback);
            return;
        }
        int duration = mode.seconds[phaseIdx];
        if (duration == 0) {
            runPhase(ctx, handler, running, orb, tvPhase, tvCount,
                tvCycles, mode, cycleCount, ttsCallback, phaseIdx + 1, 0);
            return;
        }

        String phaseName = mode.phases[phaseIdx];
        tvPhase.setText(phaseName);
        if (ttsCallback != null && elapsed == 0) ttsCallback.speak(phaseName);

        // Animate orb
        boolean expanding = phaseIdx == 0;
        float fromScale = expanding ? 0.35f : (phaseIdx == 2 ? 0.95f : orb.scale);
        float toScale   = phaseIdx == 0 ? 0.95f : (phaseIdx == 2 ? 0.35f : orb.scale);
        ValueAnimator anim = ValueAnimator.ofFloat(fromScale, toScale);
        anim.setDuration(duration * 1000L);
        anim.setInterpolator(new AccelerateDecelerateInterpolator());
        anim.addUpdateListener(a -> {
            if (running[0]) orb.setScale((float) a.getAnimatedValue());
        });
        anim.start();

        // Countdown
        final int[] secondsLeft = {duration};
        Runnable tick = new Runnable() {
            @Override public void run() {
                if (!running[0]) return;
                tvCount.setText(String.valueOf(secondsLeft[0]));
                secondsLeft[0]--;
                if (secondsLeft[0] >= 0) handler.postDelayed(this, 1000);
                else {
                    anim.cancel();
                    runPhase(ctx, handler, running, orb, tvPhase, tvCount,
                        tvCycles, mode, cycleCount, ttsCallback, phaseIdx + 1, 0);
                }
            }
        };
        handler.post(tick);
    }

    public interface TextToSpeechCallback { void speak(String text); }
}
