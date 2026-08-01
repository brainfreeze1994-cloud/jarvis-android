package com.jarvis.ai;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.view.animation.AnimationSet;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Save crash info so next launch shows it
        Thread.setDefaultUncaughtExceptionHandler((t, ex) -> {
            try {
                String trace = android.util.Log.getStackTraceString(ex);
                getSharedPreferences("jarvis_prefs", MODE_PRIVATE).edit()
                    .putString("last_crash", ex.getClass().getSimpleName() + ": " + ex.getMessage() + "\n\n" + trace)
                    .apply();
            } catch (Exception ignored) {}
            android.os.Process.killProcess(android.os.Process.myPid());
        });

        // Full screen immersive
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        );

        setContentView(R.layout.activity_splash);

        // Show crash from previous launch if any
        String lastCrash = getSharedPreferences("jarvis_prefs", MODE_PRIVATE).getString("last_crash", null);
        if (lastCrash != null) {
            getSharedPreferences("jarvis_prefs", MODE_PRIVATE).edit().remove("last_crash").apply();
            new android.app.AlertDialog.Builder(this)
                .setTitle("Crash Report (share with developer)")
                .setMessage(lastCrash)
                .setPositiveButton("OK", null)
                .show();
        }

        TextView titleView = findViewById(R.id.splash_title);
        TextView subtitleView = findViewById(R.id.splash_subtitle);
        View orbView = findViewById(R.id.splash_orb);

        ScaleAnimation scaleAnim = new ScaleAnimation(
            0.3f, 1.0f, 0.3f, 1.0f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        );
        scaleAnim.setDuration(800);
        scaleAnim.setFillAfter(true);

        AlphaAnimation fadeAnim = new AlphaAnimation(0f, 1f);
        fadeAnim.setDuration(800);
        fadeAnim.setFillAfter(true);

        AnimationSet orbSet = new AnimationSet(true);
        orbSet.addAnimation(scaleAnim);
        orbSet.addAnimation(fadeAnim);
        orbView.startAnimation(orbSet);

        new Handler().postDelayed(() -> {
            AlphaAnimation titleFade = new AlphaAnimation(0f, 1f);
            titleFade.setDuration(600);
            titleFade.setFillAfter(true);
            titleView.startAnimation(titleFade);
            titleView.setVisibility(View.VISIBLE);
        }, 400);

        new Handler().postDelayed(() -> {
            AlphaAnimation subFade = new AlphaAnimation(0f, 1f);
            subFade.setDuration(600);
            subFade.setFillAfter(true);
            subtitleView.startAnimation(subFade);
            subtitleView.setVisibility(View.VISIBLE);
        }, 700);

        new Handler().postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }, 2500);
    }
}
