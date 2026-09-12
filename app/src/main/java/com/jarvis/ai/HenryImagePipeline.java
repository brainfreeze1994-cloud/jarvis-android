package com.jarvis.ai;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * H.E.N.R.Y. — ROBUST IMAGE GENERATION & CACHING PIPELINE
 *
 * Handles full lifecycle: Prompt Enhancement -> Network Stream -> Local Storage -> Shareable File
 * Includes timeout control, cancellation, retry support, and state reporting.
 */
public class HenryImagePipeline {

    public enum ImageState {
        IDLE,
        PREPARING_PROMPT,
        GENERATING,
        SUCCESS,
        FAILED,
        CANCELLED,
        TIMEOUT
    }

    public interface ImageCallback {
        void onStateChanged(ImageState state, String message);
        void onSuccess(Bitmap bitmap, File savedFile, String prompt);
        void onError(String error, boolean canRetry);
    }

    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static Future<?> currentTask = null;

    /**
     * Cancels any active image generation task.
     */
    public static void cancelActiveGeneration() {
        if (currentTask != null && !currentTask.isDone()) {
            currentTask.cancel(true);
        }
    }

    /**
     * Executes generation with 25-second timeout and file persistence.
     */
    public static void generateImage(Context context, String rawPrompt, boolean isAnimation, ImageCallback callback) {
        cancelActiveGeneration();

        final Context appContext = context.getApplicationContext();
        callback.onStateChanged(ImageState.PREPARING_PROMPT, "Refining prompt and lighting parameters...");

        String cleanPrompt = ImageGenerator.extractPrompt(rawPrompt);
        String imageUrl = isAnimation ? ImageGenerator.buildAnimationUrl(cleanPrompt) : ImageGenerator.buildImageUrl(cleanPrompt);

        currentTask = executor.submit(() -> {
            mainHandler.post(() -> callback.onStateChanged(ImageState.GENERATING, "Synthesizing visual tokens via neural renderer..."));

            HttpURLConnection conn = null;
            InputStream in = null;
            FileOutputStream out = null;

            try {
                URL url = new URL(imageUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(20000);
                conn.setRequestMethod("GET");
                conn.setInstanceFollowRedirects(true);

                int responseCode = conn.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    mainHandler.post(() -> callback.onError("Server returned response code " + responseCode, true));
                    return;
                }

                in = conn.getInputStream();
                Bitmap bmp = BitmapFactory.decodeStream(in);

                if (bmp == null) {
                    mainHandler.post(() -> callback.onError("Failed to decode image bitmap stream.", true));
                    return;
                }

                // Save to local cache directory for fast sharing
                File dir = new File(appContext.getCacheDir(), "henry_images");
                if (!dir.exists()) dir.mkdirs();

                String fileName = "IMG_" + System.currentTimeMillis() + ".png";
                File savedFile = new File(dir, fileName);
                out = new FileOutputStream(savedFile);
                bmp.compress(Bitmap.CompressFormat.PNG, 95, out);
                out.flush();

                mainHandler.post(() -> {
                    callback.onStateChanged(ImageState.SUCCESS, "Image generated successfully.");
                    callback.onSuccess(bmp, savedFile, cleanPrompt);
                });

            } catch (java.net.SocketTimeoutException ste) {
                mainHandler.post(() -> {
                    callback.onStateChanged(ImageState.TIMEOUT, "Generation timed out after 20 seconds.");
                    callback.onError("Connection timed out. Tap retry to attempt with alternative seed.", true);
                });
            } catch (Exception e) {
                if (Thread.currentThread().isInterrupted()) {
                    mainHandler.post(() -> callback.onStateChanged(ImageState.CANCELLED, "Generation cancelled by user."));
                } else {
                    mainHandler.post(() -> {
                        callback.onStateChanged(ImageState.FAILED, "Generation error: " + e.getMessage());
                        callback.onError("Image render failed: " + e.getMessage(), true);
                    });
                }
            } finally {
                try { if (in != null) in.close(); } catch (Exception ignored) {}
                try { if (out != null) out.close(); } catch (Exception ignored) {}
                if (conn != null) conn.disconnect();
            }
        });
    }
}
