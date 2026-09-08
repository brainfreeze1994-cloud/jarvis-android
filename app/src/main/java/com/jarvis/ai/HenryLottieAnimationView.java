package com.jarvis.ai;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import com.airbnb.android.LottieAnimationView;
import com.airbnb.android.LottieDrawable;
import com.airbnb.android.RenderMode;

/**
 * HenryLottieAnimationView
 *
 * Lightweight, high-performance Lottie animation component for H.E.N.R.Y.
 * Provides instant on-device vector animations (neural pulses, voice waveforms,
 * thinking dots, and cybernetic loaders) with hardware acceleration and 60fps rendering.
 */
public class HenryLottieAnimationView extends LottieAnimationView {

    private static final String TAG = "HenryLottieView";

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
        // Enable hardware rendering for 60fps silky smooth animations
        try {
            setRenderMode(RenderMode.HARDWARE);
        } catch (Exception ignored) {}

        enableMergePathsForKitKatAndAbove(true);
        setRepeatCount(LottieDrawable.INFINITE);
    }

    /**
     * Play a built-in lightweight animation preset.
     */
    public void playPreset(Preset preset) {
        if (preset == null) return;
        this.currentPreset = preset;
        try {
            setAnimation(preset.getAssetPath());
            setRepeatCount(LottieDrawable.INFINITE);
            playAnimation();
        } catch (Exception e) {
            Log.e(TAG, "Failed to load Lottie asset: " + preset.getAssetPath(), e);
        }
    }

    /**
     * Play a preset by name (case-insensitive: "typing", "pulse", "voice", "loader").
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
        } else if (lower.contains("spinner") || lower.contains("track")) {
            playPreset(Preset.LOADING_SPINNER);
        } else if (lower.contains("check") || lower.contains("success") || lower.contains("done")) {
            playPreset(Preset.SUCCESS_CHECK);
        } else if (lower.contains("load") || lower.contains("cyber") || lower.contains("spin")) {
            playPreset(Preset.CYBER_LOADER);
        } else {
            playPreset(Preset.NEURAL_PULSE);
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

    /**
     * Stop and reset progress.
     */
    public void stopAndReset() {
        cancelAnimation();
        setProgress(0f);
    }

    /**
     * Set playback speed dynamically (e.g. 1.5f for faster thinking or audio pitch).
     */
    public void setDynamicSpeed(float speed) {
        setSpeed(speed);
    }

    public Preset getCurrentPreset() {
        return currentPreset;
    }
}
