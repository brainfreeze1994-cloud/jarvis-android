package com.jarvis.ai;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Coordinates the fully embedded, free Android video renderer.
 * No Python server, cloud provider, API key, FFmpeg install, or Veo/Gemini dependency is used
 * for the video ENCODING itself. Scene illustrations are fetched from Pollinations.ai (the same
 * free, no-API-key image service already used by {@link ImageGenerator} elsewhere in the app).
 */
public final class HenryVideoProductionManager {
    private static final int MAX_PHOTO_DIMENSION = 1280; // downscale cap to avoid OOM on large camera photos
    private static final int IMAGE_CONNECT_TIMEOUT_MS = 15000;
    private static final int IMAGE_READ_TIMEOUT_MS = 25000;

    public interface Callback {
        void onStatus(String status, int completedScenes, int totalScenes);
        void onSuccess(File finalVideo, int completedScenes);
        void onError(String error);
    }

    private HenryVideoProductionManager() {}

    /**
     * Script-driven video: duration is auto-scaled from the script's word count (15s–15min),
     * and each sentence becomes its own on-screen scene with fade + slow zoom, Lottie-style.
     * Each scene also gets a real AI-generated illustration matching its sentence — not just
     * a plain color gradient — fetched from Pollinations before rendering starts.
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
            List<Bitmap> fetchedForCleanup = new ArrayList<>();
            try {
                callback.onStatus("EMBEDDED VIDEO ENGINE: parsing script into scenes…", 0, 1);
                HenryEmbeddedVideoEngine.Config config = HenryEmbeddedVideoEngine.Config.fromScript(
                        script, explicitDurationSeconds, aspectRatio, resolution);

                // Fetch one real illustration per scene, based on that scene's own sentence.
                // A failed fetch for a given scene just leaves it on the gradient fallback —
                // one slow/failed image never aborts the whole video.
                int totalScenes = config.scenes.size();
                for (int i = 0; i < totalScenes; i++) {
                    HenryEmbeddedVideoEngine.SceneText scene = config.scenes.get(i);
                    callback.onStatus("EMBEDDED VIDEO ENGINE: generating visual " + (i + 1) + "/" + totalScenes + "…",
                            (int) (((i) * 40.0) / Math.max(1, totalScenes)), 100);
                    Bitmap illustration = fetchIllustration(scene.text, aspectRatio);
                    if (illustration != null) {
                        scene.setBackground(illustration);
                        fetchedForCleanup.add(illustration);
                    }
                }

                File out = new File(context.getCacheDir(), "HENRY_SCRIPT_" + System.currentTimeMillis() + ".mp4");
                HenryEmbeddedVideoEngine.render(context, config, (status, percent) -> {
                    // Render progress fills the remaining 40–100% of the reported range.
                    callback.onStatus(status, 40 + (int) (percent * 0.6), 100);
                }, out);
                if (!out.exists() || out.length() == 0) {
                    throw new IllegalStateException("Embedded renderer produced an empty MP4.");
                }
                callback.onSuccess(out, 1);
            } catch (Throwable e) {
                callback.onError(e.getMessage() != null ? e.getMessage() : "Embedded video production failed.");
            } finally {
                for (Bitmap b : fetchedForCleanup) { try { b.recycle(); } catch (Throwable ignored) {} }
            }
        }, "henry-script-video-production").start();
    }

    /**
     * Fetches a Pollinations illustration for one scene's sentence, downscaled for the renderer.
     * Uses the flux model (noticeably better quality than the sana default) at dimensions that
     * already match the target orientation, instead of a fixed square that then loses detail
     * being stretched/cropped to fit a 9:16 or 16:9 frame.
     */
    private static Bitmap fetchIllustration(String scenePrompt, String aspectRatio) {
        HttpURLConnection conn = null;
        try {
            boolean portrait = "9:16".equals(aspectRatio);
            String urlStr = buildSceneImageUrl(scenePrompt, portrait);
            conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setConnectTimeout(IMAGE_CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(IMAGE_READ_TIMEOUT_MS);
            conn.setRequestProperty("User-Agent", "HENRY-Android/1.0");
            conn.connect();
            if (conn.getResponseCode() != 200) return null;
            try (InputStream in = conn.getInputStream()) {
                byte[] bytes = readAll(in);
                BitmapFactory.Options bounds = new BitmapFactory.Options();
                bounds.inJustDecodeBounds = true;
                BitmapFactory.decodeByteArray(bytes, 0, bytes.length, bounds);
                int sample = 1;
                while ((bounds.outWidth / sample) > MAX_PHOTO_DIMENSION || (bounds.outHeight / sample) > MAX_PHOTO_DIMENSION) {
                    sample *= 2;
                }
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inSampleSize = sample;
                return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opts);
            }
        } catch (Throwable e) {
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    /** Pollinations request tuned for scene illustrations: flux model, orientation-matched size. */
    private static String buildSceneImageUrl(String prompt, boolean portrait) {
        try {
            String clean = prompt.replaceAll("\\s+", " ").trim();
            if (clean.length() > 160) clean = clean.substring(0, 160);
            // Only negative/suppression hints here — no forced style like "cinematic photograph".
            // Forcing a photographic style overrides any request that implies a different style
            // (cartoon, stick figure, illustration, anime, etc.), which is exactly what turned
            // "a stickman walking around" into a photorealistic hooded figure with a sword.
            // The prompt itself should be the only thing steering style; these hints only
            // suppress the model's tendency to render literal on-screen text/watermarks.
            String styled = clean + ", no text, no words, no caption, no typography, " +
                    "no watermark, no logo, no signature";
            long seed = (long) (Math.random() * 9000000) + 1000000;
            int w = portrait ? 768 : 1344;
            int h = portrait ? 1344 : 768;
            return "https://image.pollinations.ai/prompt/" + java.net.URLEncoder.encode(styled, "UTF-8") +
                    "?model=flux&seed=" + seed + "&width=" + w + "&height=" + h + "&nologo=true";
        } catch (Exception e) {
            return "https://image.pollinations.ai/prompt/scene+no+text?model=flux&width=1344&height=768&nologo=true";
        }
    }

    private static byte[] readAll(InputStream in) throws Exception {
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int n;
        while ((n = in.read(chunk)) != -1) buffer.write(chunk, 0, n);
        return buffer.toByteArray();
    }

    /**
     * Photo-slideshow video: decodes each photo URI to a downscaled bitmap, then renders one
     * scene per photo with its caption overlaid (fade + Ken Burns zoom), in the order given.
     * photoUris.size() must equal captions.size().
     */
    public static void generateFromPhotos(Context context, List<Uri> photoUris, List<String> captions,
                                           String aspectRatio, String resolution, Callback callback) {
        new Thread(() -> {
            List<Bitmap> decoded = new ArrayList<>();
            try {
                callback.onStatus("EMBEDDED VIDEO ENGINE: decoding photos…", 0, 1);
                for (Uri uri : photoUris) {
                    Bitmap bmp = decodeDownscaled(context, uri);
                    if (bmp != null) decoded.add(bmp);
                }
                if (decoded.isEmpty()) {
                    throw new IllegalStateException("No photos could be decoded for the slideshow.");
                }
                File out = new File(context.getCacheDir(), "HENRY_PHOTOS_" + System.currentTimeMillis() + ".mp4");
                HenryEmbeddedVideoEngine.Config config = HenryEmbeddedVideoEngine.Config.fromPhotos(
                        decoded, captions, aspectRatio, resolution);
                HenryEmbeddedVideoEngine.render(context, config, (status, percent) -> {
                    callback.onStatus(status, percent, 100);
                }, out);
                if (!out.exists() || out.length() == 0) {
                    throw new IllegalStateException("Embedded renderer produced an empty MP4.");
                }
                callback.onSuccess(out, 1);
            } catch (Throwable e) {
                callback.onError(e.getMessage() != null ? e.getMessage() : "Photo slideshow production failed.");
            } finally {
                for (Bitmap b : decoded) { try { b.recycle(); } catch (Throwable ignored) {} }
            }
        }, "henry-photo-video-production").start();
    }

    private static Bitmap decodeDownscaled(Context context, Uri uri) {
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (InputStream in = context.getContentResolver().openInputStream(uri)) {
                BitmapFactory.decodeStream(in, null, bounds);
            }
            int sample = 1;
            while ((bounds.outWidth / sample) > MAX_PHOTO_DIMENSION || (bounds.outHeight / sample) > MAX_PHOTO_DIMENSION) {
                sample *= 2;
            }
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = sample;
            try (InputStream in = context.getContentResolver().openInputStream(uri)) {
                return BitmapFactory.decodeStream(in, null, opts);
            }
        } catch (Throwable e) {
            return null;
        }
    }
}
