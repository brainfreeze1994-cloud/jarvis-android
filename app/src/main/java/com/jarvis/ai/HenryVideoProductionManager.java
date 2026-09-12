package com.jarvis.ai;

import android.content.Context;
import java.io.File;

/**
 * Coordinates the fully embedded, free Android video renderer.
 * No Python server, cloud provider, API key, FFmpeg install, or Veo/Gemini dependency is used.
 */
public final class HenryVideoProductionManager {
    public interface Callback {
        void onStatus(String status, int completedScenes, int totalScenes);
        void onSuccess(File finalVideo, int completedScenes);
        void onError(String error);
    }

    private HenryVideoProductionManager() {}

    /**
     * Script-driven video: duration is auto-scaled from the script's word count (15s–15min),
     * and each sentence becomes its own on-screen scene with fade + slow zoom, Lottie-style.
     */
    public static void generateFromScript(Context context, String script,
                                           String aspectRatio, String resolution, Callback callback) {
        generateFromScript(context, script, 0, aspectRatio, resolution, callback);
    }

    /**
     * Same as {@link #generateFromScript(Context, String, String, String, Callback)}, but takes
     * an explicit target duration in seconds (e.g. parsed from "make a 30 second video") — pass
     * 0 to fall back to the word-count estimate instead.
     */
    public static void generateFromScript(Context context, String script, int explicitDurationSeconds,
                                           String aspectRatio, String resolution, Callback callback) {
        new Thread(() -> {
            try {
                callback.onStatus("EMBEDDED VIDEO ENGINE: parsing script into scenes…", 0, 1);
                File out = new File(context.getCacheDir(), "HENRY_SCRIPT_" + System.currentTimeMillis() + ".mp4");
                HenryEmbeddedVideoEngine.Config config = HenryEmbeddedVideoEngine.Config.fromScript(
                        script, explicitDurationSeconds, aspectRatio, resolution);
                HenryEmbeddedVideoEngine.render(context, config, (status, percent) -> {
                    callback.onStatus(status, percent, 100);
                }, out);
                if (!out.exists() || out.length() == 0) {
                    throw new IllegalStateException("Embedded renderer produced an empty MP4.");
                }
                callback.onSuccess(out, 1);
            } catch (Throwable e) {
                callback.onError(e.getMessage() != null ? e.getMessage() : "Embedded video production failed.");
            }
        }, "henry-script-video-production").start();
    }
}
