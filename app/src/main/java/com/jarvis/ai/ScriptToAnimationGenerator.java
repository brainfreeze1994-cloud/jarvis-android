package com.henry.assistant.animation;
// TODO: change package to match your HENRY Android module structure.

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.airbnb.lottie.LottieCompositionFactory;
import com.airbnb.lottie.LottieDrawable;
import com.airbnb.lottie.LottieResult;
import com.airbnb.lottie.model.LottieCompositionCache;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ScriptToAnimationGenerator
 *
 * Turns a text script into a Lottie-style animation (built programmatically as
 * Lottie JSON, no After Effects needed) and renders that animation to an MP4
 * video file on-device using MediaCodec + MediaMuxer.
 *
 * Duration auto-scales with script length: 15 seconds minimum, 15 minutes (900s) maximum.
 *
 * Requires (app-level build.gradle):
 *   implementation "com.airbnb.android:lottie:6.4.0"
 *
 * Android only — uses MediaCodec/MediaMuxer, no iOS/web equivalent here.
 */
public class ScriptToAnimationGenerator {

    private static final String TAG = "ScriptToAnimation";

    // ---- Tunables ----
    public static final int FPS = 30;
    public static final int WIDTH = 1080;
    public static final int HEIGHT = 1920; // portrait, mobile-first
    public static final int MIN_DURATION_SEC = 15;
    public static final int MAX_DURATION_SEC = 15 * 60; // 900
    public static final double WORDS_PER_SECOND = 2.3;  // ~140 wpm speaking pace
    public static final double MIN_SCENE_SECONDS = 2.0;

    private static final int[] SCENE_COLORS = {
            0xFF1A1A2E, 0xFF16213E, 0xFF0F3460, 0xFF533483,
            0xFF1B262C, 0xFF3282B8, 0xFF2C3333, 0xFF212121
    };

    public interface RenderCallback {
        void onProgress(int percent);
        void onComplete(File outputFile, int totalDurationSec);
        void onError(Exception e);
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // ---------------------------------------------------------------------
    // STEP 1: Script -> Scenes -> Lottie JSON composition
    // ---------------------------------------------------------------------

    private static class Scene {
        String text;
        int startFrame;
        int endFrame;
        int colorHex;
    }

    /** Splits the script into scenes (by sentence) and assigns each a duration. */
    private List<Scene> parseScriptIntoScenes(String script, int totalFrames) {
        List<Scene> scenes = new ArrayList<>();
        String[] rawSentences = script.trim().split("(?<=[.!?])\\s+");
        List<String> sentences = new ArrayList<>();
        for (String s : rawSentences) {
            if (!s.trim().isEmpty()) sentences.add(s.trim());
        }
        if (sentences.isEmpty()) sentences.add(script.trim().isEmpty() ? "..." : script.trim());

        // Weight each scene's share of total frames by its word count (min floor per scene).
        double[] weights = new double[sentences.size()];
        double weightSum = 0;
        for (int i = 0; i < sentences.size(); i++) {
            int words = sentences.get(i).split("\\s+").length;
            weights[i] = Math.max(words, 3);
            weightSum += weights[i];
        }

        int cursor = 0;
        for (int i = 0; i < sentences.size(); i++) {
            Scene scene = new Scene();
            scene.text = sentences.get(i);
            scene.colorHex = SCENE_COLORS[i % SCENE_COLORS.length];
            scene.startFrame = cursor;
            int frames = (int) Math.round((weights[i] / weightSum) * totalFrames);
            int minFrames = (int) (MIN_SCENE_SECONDS * FPS);
            frames = Math.max(frames, Math.min(minFrames, totalFrames));
            int end = (i == sentences.size() - 1) ? totalFrames : Math.min(cursor + frames, totalFrames);
            scene.endFrame = end;
            cursor = end;
            scenes.add(scene);
            if (cursor >= totalFrames) break;
        }
        return scenes;
    }

    /** Computes total video duration in seconds from script length, clamped to [15s, 900s]. */
    public int computeDurationSeconds(String script) {
        int wordCount = script.trim().isEmpty() ? 0 : script.trim().split("\\s+").length;
        double estimated = wordCount / WORDS_PER_SECOND;
        int clamped = (int) Math.round(Math.max(MIN_DURATION_SEC, Math.min(MAX_DURATION_SEC, estimated)));
        return clamped;
    }

    /**
     * Builds a Lottie-compatible JSON composition for the given script.
     * Each scene = a colored background shape layer + a fading/sliding text layer.
     */
    public JSONObject generateComposition(String script) throws JSONException {
        int durationSec = computeDurationSeconds(script);
        int totalFrames = durationSec * FPS;

        List<Scene> scenes = parseScriptIntoScenes(script, totalFrames);

        JSONObject root = new JSONObject();
        root.put("v", "5.9.0");
        root.put("fr", FPS);
        root.put("ip", 0);
        root.put("op", totalFrames);
        root.put("w", WIDTH);
        root.put("h", HEIGHT);
        root.put("nm", "HENRY Script Animation");
        root.put("ddd", 0);

        JSONArray layers = new JSONArray();
        int layerIndex = 1;
        for (Scene scene : scenes) {
            layers.put(buildBackgroundLayer(scene, layerIndex++));
            layers.put(buildTextLayer(scene, layerIndex++));
        }
        root.put("layers", layers);
        root.put("assets", new JSONArray());
        return root;
    }

    private JSONObject buildBackgroundLayer(Scene scene, int index) throws JSONException {
        JSONObject layer = new JSONObject();
        layer.put("ddd", 0);
        layer.put("ind", index);
        layer.put("ty", 1); // solid layer
        layer.put("nm", "bg_" + index);
        layer.put("sr", 1);
        layer.put("ip", scene.startFrame);
        layer.put("op", scene.endFrame);
        layer.put("st", 0);
        layer.put("sw", WIDTH);
        layer.put("sh", HEIGHT);
        layer.put("sc", String.format("#%06X", (0xFFFFFF & scene.colorHex)));

        JSONObject ks = new JSONObject();
        ks.put("o", fixedValue(100));
        ks.put("p", staticVec(WIDTH / 2.0, HEIGHT / 2.0));
        ks.put("a", staticVec(0, 0));
        ks.put("s", fadeScaleKeyframes(scene)); // subtle Ken Burns zoom
        ks.put("r", fixedValue(0));
        layer.put("ks", ks);
        return layer;
    }

    private JSONObject buildTextLayer(Scene scene, int index) throws JSONException {
        JSONObject layer = new JSONObject();
        layer.put("ddd", 0);
        layer.put("ind", index);
        layer.put("ty", 5); // text layer
        layer.put("nm", "text_" + index);
        layer.put("sr", 1);
        layer.put("ip", scene.startFrame);
        layer.put("op", scene.endFrame);
        layer.put("st", 0);

        JSONObject textData = new JSONObject();
        JSONObject document = new JSONObject();
        document.put("t", scene.text);
        document.put("f", "Helvetica-Bold");
        document.put("s", 64);
        document.put("fc", new JSONArray().put(1).put(1).put(1));
        document.put("j", 2); // centered
        document.put("lh", 78);
        document.put("ls", 0);
        JSONObject doc0 = new JSONObject();
        doc0.put("t", 0);
        doc0.put("s", document);
        textData.put("d", new JSONObject().put("k", new JSONArray().put(doc0)));
        layer.put("t", textData);

        JSONObject ks = new JSONObject();
        ks.put("o", fadeInOutOpacity(scene)); // fade in/out per scene
        ks.put("p", staticVec(WIDTH / 2.0, HEIGHT / 2.0));
        ks.put("a", staticVec(0, 0));
        ks.put("s", fixedVec(100, 100));
        ks.put("r", fixedValue(0));
        layer.put("ks", ks);
        return layer;
    }

    // ---- Keyframe helpers ----

    private JSONObject fixedValue(double v) throws JSONException {
        return new JSONObject().put("a", 0).put("k", v);
    }

    private JSONObject staticVec(double x, double y) throws JSONException {
        return new JSONObject().put("a", 0).put("k", new JSONArray().put(x).put(y).put(0));
    }

    private JSONObject fixedVec(double x, double y) throws JSONException {
        return new JSONObject().put("a", 0).put("k", new JSONArray().put(x).put(y).put(100));
    }

    private JSONObject fadeInOutOpacity(Scene scene) throws JSONException {
        int span = Math.max(1, scene.endFrame - scene.startFrame);
        int fade = Math.min(10, span / 4);
        JSONArray keys = new JSONArray();
        keys.put(kf(scene.startFrame, 0));
        keys.put(kf(scene.startFrame + fade, 100));
        keys.put(kf(scene.endFrame - fade, 100));
        keys.put(kf(scene.endFrame, 0));
        return new JSONObject().put("a", 1).put("k", keys);
    }

    private JSONObject fadeScaleKeyframes(Scene scene) throws JSONException {
        JSONArray keys = new JSONArray();
        keys.put(kfVec(scene.startFrame, 100, 100));
        keys.put(kfVec(scene.endFrame, 108, 108)); // slow zoom-in over the scene
        return new JSONObject().put("a", 1).put("k", keys);
    }

    private JSONObject kf(int frame, double value) throws JSONException {
        JSONObject k = new JSONObject();
        k.put("t", frame);
        k.put("s", new JSONArray().put(value));
        return k;
    }

    private JSONObject kfVec(int frame, double x, double y) throws JSONException {
        JSONObject k = new JSONObject();
        k.put("t", frame);
        k.put("s", new JSONArray().put(x).put(y).put(100));
        return k;
    }

    // ---------------------------------------------------------------------
    // STEP 2: Lottie JSON -> MP4 video via MediaCodec + MediaMuxer
    // ---------------------------------------------------------------------

    /** Generates the animation and renders it straight to an MP4 file. Runs off the main thread. */
    public void generateVideoFromScript(Context context, String script, File outputFile, RenderCallback callback) {
        executor.execute(() -> {
            try {
                JSONObject composition = generateComposition(script);
                int durationSec = composition.getInt("op") / FPS;
                renderCompositionToVideo(context, composition, outputFile, durationSec, callback);
            } catch (Exception e) {
                Log.e(TAG, "Generation failed", e);
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    private void renderCompositionToVideo(Context context, JSONObject composition, File outputFile,
                                           int durationSec, RenderCallback callback) throws Exception {
        LottieResult<com.airbnb.lottie.LottieComposition> result =
                LottieCompositionFactory.fromJsonStringSync(composition.toString(), "henry_script_anim").getValue() != null
                        ? LottieCompositionFactory.fromJsonStringSync(composition.toString(), "henry_script_anim")
                        : null;

        com.airbnb.lottie.LottieComposition lottieComposition = result != null ? result.getValue() : null;
        if (lottieComposition == null) {
            throw new IllegalStateException("Failed to parse generated Lottie composition");
        }

        LottieDrawable drawable = new LottieDrawable();
        drawable.setComposition(lottieComposition);
        drawable.setBounds(0, 0, WIDTH, HEIGHT);

        int totalFrames = durationSec * FPS;
        long frameDurationUs = 1_000_000L / FPS;

        MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, WIDTH, HEIGHT);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
        format.setInteger(MediaFormat.KEY_BIT_RATE, 6_000_000);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, FPS);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2);

        MediaCodec encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        encoder.start();

        MediaMuxer muxer = new MediaMuxer(outputFile.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        int trackIndex = -1;
        boolean muxerStarted = false;

        Bitmap frameBitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(frameBitmap);
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

        for (int frame = 0; frame < totalFrames; frame++) {
            drawable.setFrame(frame % Math.max(1, lottieComposition.getDurationFrames() > 0
                    ? (int) lottieComposition.getDurationFrames() : totalFrames));
            canvas.drawColor(Color.BLACK);
            drawable.draw(canvas);

            byte[] yuv = bitmapToYuv420(frameBitmap);
            long presentationTimeUs = frame * frameDurationUs;

            int inputIndex = encoder.dequeueInputBuffer(10_000);
            if (inputIndex >= 0) {
                ByteBuffer inputBuffer = encoder.getInputBuffer(inputIndex);
                inputBuffer.clear();
                inputBuffer.put(yuv);
                encoder.queueInputBuffer(inputIndex, 0, yuv.length, presentationTimeUs, 0);
            }

            int outputIndex = encoder.dequeueOutputBuffer(bufferInfo, 10_000);
            while (outputIndex >= 0) {
                if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    // handled below via loop
                } else if (outputIndex >= 0) {
                    ByteBuffer encodedData = encoder.getOutputBuffer(outputIndex);
                    if (bufferInfo.size > 0 && muxerStarted) {
                        muxer.writeSampleData(trackIndex, encodedData, bufferInfo);
                    }
                    encoder.releaseOutputBuffer(outputIndex, false);
                }
                outputIndex = encoder.dequeueOutputBuffer(bufferInfo, 0);
            }

            if (!muxerStarted && encoder.getOutputFormat() != null) {
                trackIndex = muxer.addTrack(encoder.getOutputFormat());
                muxer.start();
                muxerStarted = true;
            }

            if (frame % FPS == 0) {
                int percent = (int) ((frame / (float) totalFrames) * 100);
                mainHandler.post(() -> callback.onProgress(percent));
            }
        }

        // Signal end of stream and drain remaining output.
        int inputIndex = encoder.dequeueInputBuffer(10_000);
        if (inputIndex >= 0) {
            encoder.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
        }
        boolean eos = false;
        while (!eos) {
            int outputIndex = encoder.dequeueOutputBuffer(bufferInfo, 10_000);
            if (outputIndex >= 0) {
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) eos = true;
                ByteBuffer encodedData = encoder.getOutputBuffer(outputIndex);
                if (bufferInfo.size > 0 && muxerStarted) {
                    muxer.writeSampleData(trackIndex, encodedData, bufferInfo);
                }
                encoder.releaseOutputBuffer(outputIndex, false);
            } else if (outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                break;
            }
        }

        encoder.stop();
        encoder.release();
        if (muxerStarted) {
            muxer.stop();
        }
        muxer.release();

        int finalDurationSec = durationSec;
        mainHandler.post(() -> callback.onComplete(outputFile, finalDurationSec));
    }

    /** Simple ARGB_8888 Bitmap -> YUV420 planar (I420) conversion for the encoder input buffer. */
    private byte[] bitmapToYuv420(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] argb = new int[width * height];
        bitmap.getPixels(argb, 0, width, 0, 0, width, height);

        byte[] yuv = new byte[width * height * 3 / 2];
        int frameSize = width * height;
        int yIndex = 0;
        int uvIndex = frameSize;

        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                int p = argb[j * width + i];
                int r = (p >> 16) & 0xff;
                int g = (p >> 8) & 0xff;
                int b = p & 0xff;

                int y = ((66 * r + 129 * g + 25 * b + 128) >> 8) + 16;
                yuv[yIndex++] = (byte) clamp(y);

                if (j % 2 == 0 && i % 2 == 0) {
                    int u = ((-38 * r - 74 * g + 112 * b + 128) >> 8) + 128;
                    int v = ((112 * r - 94 * g - 18 * b + 128) >> 8) + 128;
                    yuv[uvIndex++] = (byte) clamp(u);
                    yuv[uvIndex++] = (byte) clamp(v);
                }
            }
        }
        return yuv;
    }

    private int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    public void shutdown() {
        executor.shutdown();
    }

    // ---------------------------------------------------------------------
    // Usage:
    //
    //   ScriptToAnimationGenerator generator = new ScriptToAnimationGenerator();
    //   File out = new File(context.getExternalFilesDir(null), "henry_animation.mp4");
    //   generator.generateVideoFromScript(context, script, out, new RenderCallback() {
    //       public void onProgress(int percent) { /* update progress UI */ }
    //       public void onComplete(File outputFile, int totalDurationSec) { /* play/share file */ }
    //       public void onError(Exception e) { /* show error */ }
    //   });
    //
    // Notes:
    //  - COLOR_FormatYUV420Flexible is broadly supported, but on a few OEM encoders you may
    //    need to query MediaCodecInfo.CodecCapabilities.colorFormats and pick a supported one
    //    at runtime instead of assuming Flexible works everywhere.
    //  - For longer scripts (approaching 15 min / ~2000+ frames), consider chunked rendering
    //    with a foreground Service + notification, since this loop runs on a single background
    //    thread and a 15-minute render will take real wall-clock time on-device.
    //  - Swap SCENE_COLORS / fonts / add shape layers (buildBackgroundLayer) to match HENRY's
    //    HUD aesthetic instead of flat solids.
    // ---------------------------------------------------------------------
}
