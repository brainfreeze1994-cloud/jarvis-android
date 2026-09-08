package com.jarvis.ai;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

/**
 * HenryLottieDialog
 *
 * Interactive showcase viewer for the high-performance HenryLottieAnimationView component.
 * Allows testing and previewing presets with speed controls and real-time playback toggling.
 */
public class HenryLottieDialog {

    public static void show(Context context) {
        if (context == null) return;
        try {
            Dialog dialog = new Dialog(context);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.dialog_lottie_showcase);
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.getWindow().setLayout(
                    (int) (context.getResources().getDisplayMetrics().widthPixels * 0.92),
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                );
            }

            HenryLottieAnimationView lottieView = dialog.findViewById(R.id.lottie_showcase_view);
            TextView tvTitle = dialog.findViewById(R.id.tv_current_animation_name);
            View btnClose = dialog.findViewById(R.id.tv_dialog_close);

            Button btnPulse  = dialog.findViewById(R.id.btn_preset_pulse);
            Button btnDots   = dialog.findViewById(R.id.btn_preset_dots);
            Button btnWave   = dialog.findViewById(R.id.btn_preset_wave);
            Button btnLoader = dialog.findViewById(R.id.btn_preset_loader);

            Button btnTogglePlay = dialog.findViewById(R.id.btn_toggle_play);
            Button btnSpeedHalf  = dialog.findViewById(R.id.btn_speed_half);
            Button btnSpeed1x    = dialog.findViewById(R.id.btn_speed_1x);
            Button btnSpeed2x    = dialog.findViewById(R.id.btn_speed_2x);

            // Initial animation
            if (lottieView != null) {
                lottieView.playNeuralPulse();
            }

            btnClose.setOnClickListener(v -> dialog.dismiss());

            final int COLOR_ACTIVE = 0xFF00D4FF;
            final int COLOR_INACTIVE = 0xFF1A7799;

            Runnable resetPresetTabs = () -> {
                btnPulse.setTextColor(COLOR_INACTIVE);
                btnDots.setTextColor(COLOR_INACTIVE);
                btnWave.setTextColor(COLOR_INACTIVE);
                btnLoader.setTextColor(COLOR_INACTIVE);
            };

            btnPulse.setOnClickListener(v -> {
                resetPresetTabs.run();
                btnPulse.setTextColor(COLOR_ACTIVE);
                tvTitle.setText("PRESET: NEURAL PULSE (CONCENTRIC RINGS)");
                if (lottieView != null) lottieView.playNeuralPulse();
                if (btnTogglePlay != null) btnTogglePlay.setText("⏸ PAUSE");
            });

            btnDots.setOnClickListener(v -> {
                resetPresetTabs.run();
                btnDots.setTextColor(COLOR_ACTIVE);
                tvTitle.setText("PRESET: TYPING BOUNCE (3-DOT CYBER)");
                if (lottieView != null) lottieView.playThinkingDots();
                if (btnTogglePlay != null) btnTogglePlay.setText("⏸ PAUSE");
            });

            btnWave.setOnClickListener(v -> {
                resetPresetTabs.run();
                btnWave.setTextColor(COLOR_ACTIVE);
                tvTitle.setText("PRESET: VOICE WAVEFORM (5-BAND EQUALIZER)");
                if (lottieView != null) lottieView.playVoiceWave();
                if (btnTogglePlay != null) btnTogglePlay.setText("⏸ PAUSE");
            });

            btnLoader.setOnClickListener(v -> {
                resetPresetTabs.run();
                btnLoader.setTextColor(COLOR_ACTIVE);
                tvTitle.setText("PRESET: CYBER LOADER (ORBITAL ACCELERATOR)");
                if (lottieView != null) lottieView.playCyberLoader();
                if (btnTogglePlay != null) btnTogglePlay.setText("⏸ PAUSE");
            });

            btnTogglePlay.setOnClickListener(v -> {
                if (lottieView == null) return;
                if (lottieView.isAnimating()) {
                    lottieView.pauseAnimation();
                    btnTogglePlay.setText("▶ PLAY");
                } else {
                    lottieView.resumeAnimation();
                    btnTogglePlay.setText("⏸ PAUSE");
                }
            });

            btnSpeedHalf.setOnClickListener(v -> {
                if (lottieView != null) lottieView.setDynamicSpeed(0.5f);
                btnSpeedHalf.setTextColor(COLOR_ACTIVE);
                btnSpeed1x.setTextColor(COLOR_INACTIVE);
                btnSpeed2x.setTextColor(COLOR_INACTIVE);
            });

            btnSpeed1x.setOnClickListener(v -> {
                if (lottieView != null) lottieView.setDynamicSpeed(1.0f);
                btnSpeedHalf.setTextColor(COLOR_INACTIVE);
                btnSpeed1x.setTextColor(COLOR_ACTIVE);
                btnSpeed2x.setTextColor(COLOR_INACTIVE);
            });

            btnSpeed2x.setOnClickListener(v -> {
                if (lottieView != null) lottieView.setDynamicSpeed(2.0f);
                btnSpeedHalf.setTextColor(COLOR_INACTIVE);
                btnSpeed1x.setTextColor(COLOR_INACTIVE);
                btnSpeed2x.setTextColor(COLOR_ACTIVE);
            });

            dialog.show();
        } catch (Exception e) {
            android.util.Log.e("HenryLottieDialog", "Failed to show Lottie showcase dialog", e);
        }
    }
}
