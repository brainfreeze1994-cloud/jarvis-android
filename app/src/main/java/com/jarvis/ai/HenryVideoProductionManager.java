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

    public static void generate(Context context, String topic, int targetMinutes,
                                String aspectRatio, String resolution, Callback callback) {
        final int minutes = Math.max(1, Math.min(30, targetMinutes));
        new Thread(() -> {
            try {
                callback.onStatus("EMBEDDED VIDEO ENGINE: preparing local renderer…", 0, 1);
                File out = new File(context.getCacheDir(), "HENRY_EMBEDDED_" + System.currentTimeMillis() + ".mp4");
                HenryEmbeddedVideoEngine.Config config = HenryEmbeddedVideoEngine.Config.from(
                        topic, minutes * 60, aspectRatio, resolution);
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
        }, "henry-embedded-video-production").start();
    }
}
