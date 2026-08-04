package com.jarvis.ai;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class SplashActivity extends AppCompatActivity {

    private static final String CRASH_FILE = "henry_crash.txt";
    private final Handler handler = new Handler(Looper.getMainLooper());

    private static final String[] BOOT_LINES = {
        "INITIALIZING NEURAL CORE...",
        "LOADING LANGUAGE MODULES...",
        "CALIBRATING VOICE ENGINE...",
        "CONNECTING TO HENRY SERVERS...",
        "RUNNING SELF-DIAGNOSTICS...",
        "LOADING MEMORY BANKS...",
        "ALL SYSTEMS NOMINAL.",
        "WELCOME BACK, SIR."
    };

    // Progress milestones for each boot line (0–100)
    private static final int[] BOOT_PROGRESS = { 5, 20, 38, 55, 68, 80, 92, 100 };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Install crash handler
        final File crashFile = new File(getFilesDir(), CRASH_FILE);
        Thread.setDefaultUncaughtExceptionHandler((thread, ex) -> {
            try {
                String msg = ex.getClass().getSimpleName() + ": " + ex.getMessage()
                           + "\n\n" + android.util.Log.getStackTraceString(ex);
                FileOutputStream fos = new FileOutputStream(crashFile, false);
                fos.write(msg.getBytes("UTF-8"));
                fos.flush();
                fos.getFD().sync();
                fos.close();
            } catch (Exception ignored) {}
            android.os.Process.killProcess(android.os.Process.myPid());
        });

        // Check for crash from previous run
        if (crashFile.exists()) {
            String crashMsg = readCrashFile(crashFile);
            crashFile.delete();
            new AlertDialog.Builder(this)
                .setTitle("HENRY Crash Report")
                .setMessage(crashMsg)
                .setPositiveButton("OK", (d, w) -> launchSplash())
                .setCancelable(false)
                .show();
            return;
        }

        launchSplash();
    }

    private void launchSplash() {
        try {
            getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

            setContentView(R.layout.activity_splash);

            View     orbView         = findViewById(R.id.splash_orb);
            TextView titleView       = findViewById(R.id.splash_title);
            TextView subtitleView    = findViewById(R.id.splash_subtitle);
            TextView statusView      = findViewById(R.id.splash_status);
            View     progressContainer = findViewById(R.id.splash_progress_container);
            View     progressFill    = findViewById(R.id.splash_progress_fill);
            TextView percentView     = findViewById(R.id.splash_percent);

            // Step 1 — Orb pulses in (scale + fade)
            ScaleAnimation scaleAnim = new ScaleAnimation(
                0.2f, 1.0f, 0.2f, 1.0f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
            scaleAnim.setDuration(700);
            AlphaAnimation fadeIn = new AlphaAnimation(0f, 1f);
            fadeIn.setDuration(700);
            AnimationSet orbAnim = new AnimationSet(true);
            orbAnim.addAnimation(scaleAnim);
            orbAnim.addAnimation(fadeIn);
            orbAnim.setFillAfter(true);
            orbView.startAnimation(orbAnim);

            // Step 2 — Title fades in after orb
            handler.postDelayed(() -> {
                if (titleView != null) {
                    titleView.setVisibility(View.VISIBLE);
                    AlphaAnimation a = new AlphaAnimation(0f, 1f);
                    a.setDuration(500);
                    a.setFillAfter(true);
                    titleView.startAnimation(a);
                }
            }, 750);

            // Step 3 — Subtitle fades in
            handler.postDelayed(() -> {
                if (subtitleView != null) {
                    subtitleView.setVisibility(View.VISIBLE);
                    AlphaAnimation a = new AlphaAnimation(0f, 1f);
                    a.setDuration(400);
                    a.setFillAfter(true);
                    subtitleView.startAnimation(a);
                }
            }, 1100);

            // Step 4 — Show progress + status, start boot sequence
            handler.postDelayed(() -> {
                if (statusView != null) statusView.setVisibility(View.VISIBLE);
                if (progressContainer != null) progressContainer.setVisibility(View.VISIBLE);
                if (percentView != null) percentView.setVisibility(View.VISIBLE);
                runBootSequence(statusView, progressFill, percentView, 0);
            }, 1400);

        } catch (Exception e) {
            new AlertDialog.Builder(this)
                .setTitle("Launch Error")
                .setMessage(e.getMessage())
                .setPositiveButton("OK", (d, w) -> finish())
                .show();
        }
    }

    private void runBootSequence(TextView statusView, View progressFill, TextView percentView, int index) {
        if (index >= BOOT_LINES.length) {
            // All done — launch MainActivity
            handler.postDelayed(this::goToMain, 400);
            return;
        }

        // Update status text
        if (statusView != null) statusView.setText(BOOT_LINES[index]);

        // Animate progress fill
        int targetPercent = BOOT_PROGRESS[index];
        animateProgress(progressFill, percentView, targetPercent);

        // Schedule next line (interval varies: faster in middle, pause at end)
        int delay = (index == BOOT_LINES.length - 1) ? 700
                  : (index < 2)                       ? 320
                  : 260;
        handler.postDelayed(() -> runBootSequence(statusView, progressFill, percentView, index + 1), delay);
    }

    private void animateProgress(View fill, TextView pct, int targetPct) {
        if (fill == null) return;
        fill.post(() -> {
            int parentWidth = ((View) fill.getParent()).getWidth();
            int targetPx = (int) (parentWidth * targetPct / 100f);
            android.view.ViewGroup.LayoutParams lp = fill.getLayoutParams();
            lp.width = targetPx;
            fill.setLayoutParams(lp);
            if (pct != null) pct.setText(targetPct + "%");
        });
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    private String readCrashFile(File f) {
        try {
            FileInputStream fis = new FileInputStream(f);
            byte[] data = new byte[(int) f.length()];
            fis.read(data);
            fis.close();
            return new String(data, "UTF-8");
        } catch (Exception e) {
            return "Could not read crash log.";
        }
    }
}
