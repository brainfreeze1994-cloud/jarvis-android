package com.jarvis.ai;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;

public class SplashActivity extends AppCompatActivity {

    private static final String CRASH_FILE = "henry_crash.txt";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

        if (crashFile.exists()) {
            String crashMsg = readCrashFile(crashFile);
            crashFile.delete();
            new AlertDialog.Builder(this)
                .setTitle("HENRY Crash Report — share with developer")
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

            TextView titleView    = findViewById(R.id.splash_title);
            TextView subtitleView = findViewById(R.id.splash_subtitle);
            View     orbView      = findViewById(R.id.splash_orb);

            ScaleAnimation scaleAnim = new ScaleAnimation(
                0.3f, 1.0f, 0.3f, 1.0f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
            scaleAnim.setDuration(800);
            scaleAnim.setFillAfter(true);

            AlphaAnimation fadeAnim = new AlphaAnimation(0f, 1f);
            fadeAnim.setDuration(800);
            fadeAnim.setFillAfter(true);

            AnimationSet orbSet = new AnimationSet(true);
            orbSet.addAnimation(scaleAnim);
            orbSet.addAnimation(fadeAnim);
            if (orbView != null) orbView.startAnimation(orbSet);

            new Handler().postDelayed(() -> {
                if (titleView != null) {
                    AlphaAnimation tf = new AlphaAnimation(0f, 1f);
                    tf.setDuration(600); tf.setFillAfter(true);
                    titleView.startAnimation(tf);
                    titleView.setVisibility(View.VISIBLE);
                }
            }, 400);

            new Handler().postDelayed(() -> {
                if (subtitleView != null) {
                    AlphaAnimation sf = new AlphaAnimation(0f, 1f);
                    sf.setDuration(600); sf.setFillAfter(true);
                    subtitleView.startAnimation(sf);
                    subtitleView.setVisibility(View.VISIBLE);
                }
            }, 700);

            new Handler().postDelayed(() -> {
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }, 2500);

        } catch (Throwable t) {
            String msg = t.getClass().getSimpleName() + ": " + t.getMessage()
                       + "\n\n" + android.util.Log.getStackTraceString(t);
            try {
                new AlertDialog.Builder(this)
                    .setTitle("HENRY Splash Error")
                    .setMessage(msg)
                    .setPositiveButton("OK", null)
                    .show();
            } catch (Exception ignored) {}
        }
    }

    private String readCrashFile(File f) {
        try {
            FileInputStream fis = new FileInputStream(f);
            byte[] data = new byte[(int) f.length()];
            fis.read(data);
            fis.close();
            return new String(data, "UTF-8");
        } catch (Exception e) {
            return "Could not read crash file: " + e.getMessage();
        }
    }
}
