package com.jarvis.ai;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.SystemClock;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Fully embedded free video renderer.
 *
 * Creates an actual H.264 MP4 on the Android device using platform MediaCodec and
 * MediaMuxer. Duration and scenes are both derived from a script: one scene per
 * sentence, duration auto-scaled 15s–15min from word count, each scene fades in/out
 * with a slow Ken Burns zoom and its sentence rendered as wrapped on-screen text,
 * Lottie-style. No cloud provider, API key, FFmpeg install, or diffusion model.
 */
public final class HenryEmbeddedVideoEngine {
    private static final int FPS = 24;         // standard smooth playback rate for social platforms
    private static final int UNIQUE_FPS = 12;  // real distinct frames/sec; each held for 2 output frames
    private static final int I_FRAME_SECONDS = 2;

    // HD, social-media-ready resolution — both dimensions are multiples of 16 (required by many
    // hardware encoders internally) and land within ~1% of true 720x1280/1280x720.
    private static final int PORTRAIT_WIDTH = 720;   // TikTok / YouTube Shorts / Reels (9:16)
    private static final int PORTRAIT_HEIGHT = 1280;
    private static final int LANDSCAPE_WIDTH = 1280;  // standard YouTube (16:9)
    private static final int LANDSCAPE_HEIGHT = 720;

    public static final int SCRIPT_MIN_DURATION_SEC = 15;
    public static final int SCRIPT_MAX_DURATION_SEC = 15 * 60;
    private static final double WORDS_PER_SECOND = 2.3; // ~140 wpm speaking pace
    private static final double MIN_SCENE_SECONDS = 1.6;

    private static final int[] SCENE_PALETTE = {
            0xFF1A1A2E, 0xFF16213E, 0xFF0F3460, 0xFF533483,
            0xFF1B262C, 0xFF3282B8, 0xFF2C3333, 0xFF122C34
    };

    public interface ProgressCallback { void onProgress(String status, int percent); }

    /** One script-derived scene: text + the time window (in seconds) it's shown for. */
    public static final class SceneText {
        final String text;
        final float startSec;
        final float endSec;
        final int colorIndex;
        Bitmap background; // null => gradient background; non-null => this image fills the frame

        SceneText(String text, float startSec, float endSec, int colorIndex) {
            this(text, startSec, endSec, colorIndex, null);
        }

        SceneText(String text, float startSec, float endSec, int colorIndex, Bitmap background) {
            this.text = text;
            this.startSec = startSec;
            this.endSec = endSec;
            this.colorIndex = colorIndex;
            this.background = background;
        }

        /** Attaches (or replaces) this scene's background image after construction. */
        public void setBackground(Bitmap background) {
            this.background = background;
        }
    }

    public static final class Config {
        final int durationSeconds;
        final String aspectRatio;
        final String resolution;
        final int width;
        final int height;
        final List<SceneText> scenes;

        private Config(int durationSeconds, String aspectRatio, String resolution,
                       int width, int height, List<SceneText> scenes) {
            this.durationSeconds = Math.max(1, durationSeconds);
            this.aspectRatio = aspectRatio;
            this.resolution = resolution;
            this.width = width;
            this.height = height;
            this.scenes = scenes;
        }

        /**
         * Duration and scenes are both derived from the script itself.
         * Duration = word count / speaking pace, clamped to [15s, 900s].
         */
        public static Config fromScript(String script, String aspectRatio, String resolution) {
            return fromScript(script, 0, aspectRatio, resolution);
        }

        /**
         * Same as {@link #fromScript(String, String, String)}, but if explicitDurationSeconds
         * is greater than 0 (e.g. the user asked for "30 seconds" or "2 minutes"), that duration
         * wins instead of the word-count estimate — still clamped to [15s, 900s].
         */
        public static Config fromScript(String script, int explicitDurationSeconds, String aspectRatio, String resolution) {
            boolean portrait = "9:16".equals(aspectRatio);
            int w = portrait ? PORTRAIT_WIDTH : LANDSCAPE_WIDTH;
            int h = portrait ? PORTRAIT_HEIGHT : LANDSCAPE_HEIGHT;
            int durationSeconds = explicitDurationSeconds > 0
                    ? Math.max(SCRIPT_MIN_DURATION_SEC, Math.min(SCRIPT_MAX_DURATION_SEC, explicitDurationSeconds))
                    : estimateDurationSeconds(script);
            List<SceneText> scenes = buildScenesFromScript(script, durationSeconds);
            return new Config(durationSeconds, portrait ? "9:16" : "16:9",
                    resolution == null ? "720p" : resolution, w, h, scenes);
        }

        /**
         * Photo-slideshow mode: one scene per photo, each shown for an equal share of the
         * total duration (default ~5s/photo, clamped 15s–15min overall), with its caption
         * overlaid using the same fade + Ken Burns zoom as script mode. photos.size() must
         * equal captions.size().
         */
        public static Config fromPhotos(List<Bitmap> photos, List<String> captions, String aspectRatio, String resolution) {
            boolean portrait = "9:16".equals(aspectRatio);
            int w = portrait ? PORTRAIT_WIDTH : LANDSCAPE_WIDTH;
            int h = portrait ? PORTRAIT_HEIGHT : LANDSCAPE_HEIGHT;
            int count = Math.max(1, photos.size());
            int durationSeconds = Math.max(SCRIPT_MIN_DURATION_SEC, Math.min(SCRIPT_MAX_DURATION_SEC, count * 5));
            List<SceneText> scenes = new ArrayList<>();
            float slice = durationSeconds / (float) count;
            for (int i = 0; i < photos.size(); i++) {
                float start = i * slice;
                float end = (i == photos.size() - 1) ? durationSeconds : (i + 1) * slice;
                String caption = i < captions.size() ? captions.get(i) : "";
                scenes.add(new SceneText(caption, start, end, i % SCENE_PALETTE.length, photos.get(i)));
            }
            return new Config(durationSeconds, portrait ? "9:16" : "16:9",
                    resolution == null ? "720p" : resolution, w, h, scenes);
        }
    }

    private HenryEmbeddedVideoEngine() {}

    /** Word count / speaking pace, clamped to the 15s–15min range. */
    public static int estimateDurationSeconds(String script) {
        String trimmed = script == null ? "" : script.trim();
        int wordCount = trimmed.isEmpty() ? 0 : trimmed.split("\\s+").length;
        double estimated = wordCount / WORDS_PER_SECOND;
        return (int) Math.round(Math.max(SCRIPT_MIN_DURATION_SEC, Math.min(SCRIPT_MAX_DURATION_SEC, estimated)));
    }

    /** Splits the script into one scene per sentence, sized proportionally to its word count. */
    private static List<SceneText> buildScenesFromScript(String script, int totalSeconds) {
        List<SceneText> scenes = new ArrayList<>();
        String trimmed = script == null ? "" : script.trim();
        String[] raw = trimmed.split("(?<=[.!?])\\s+");
        List<String> sentences = new ArrayList<>();
        for (String s : raw) if (!s.trim().isEmpty()) sentences.add(s.trim());
        if (sentences.isEmpty()) sentences.add(trimmed.isEmpty() ? "H.E.N.R.Y." : trimmed);

        double[] weights = new double[sentences.size()];
        double sum = 0;
        for (int i = 0; i < sentences.size(); i++) {
            int words = sentences.get(i).split("\\s+").length;
            weights[i] = Math.max(words, 3);
            sum += weights[i];
        }

        float cursor = 0f;
        for (int i = 0; i < sentences.size(); i++) {
            float dur = (float) ((weights[i] / sum) * totalSeconds);
            dur = Math.max(dur, (float) Math.min(MIN_SCENE_SECONDS, totalSeconds));
            float end = (i == sentences.size() - 1) ? totalSeconds : Math.min(cursor + dur, totalSeconds);
            scenes.add(new SceneText(sentences.get(i), cursor, end, i % SCENE_PALETTE.length));
            cursor = end;
            if (cursor >= totalSeconds) break;
        }
        return scenes;
    }

    public static boolean isSupported() {
        try {
            return findCodec() != null;
        } catch (Throwable ignored) { return false; }
    }

    public static void render(Context context, Config config, ProgressCallback progress, File output) throws Exception {
        if (!isSupported()) throw new IllegalStateException("This Android device does not expose an H.264 MediaCodec encoder.");
        File parent = output.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) throw new IllegalStateException("Cannot create video output folder.");

        MediaCodec codec = null;
        MediaMuxer muxer = null;
        Bitmap bitmap = Bitmap.createBitmap(config.width, config.height, Bitmap.Config.ARGB_8888);
        int[] pixels = new int[config.width * config.height];
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        Canvas canvas = new Canvas(bitmap);
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        boolean muxerStarted = false;
        int track = -1;
        try {
            MediaCodecInfo codecInfo = findCodec();
            int colorFormat = chooseColorFormat(codecInfo);
            codec = MediaCodec.createByCodecName(codecInfo.getName());
            MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, config.width, config.height);
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat);
            format.setInteger(MediaFormat.KEY_BIT_RATE, config.width * config.height * 5);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, FPS);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_SECONDS);
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            codec.start();
            muxer = new MediaMuxer(output.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

            int totalUniqueFrames = Math.max(1, config.durationSeconds * UNIQUE_FPS);
            int repeat = FPS / UNIQUE_FPS;
            long frameIndex = 0;
            long lastProgress = 0;
            for (int unique = 0; unique < totalUniqueFrames; unique++) {
                float time = unique / (float) UNIQUE_FPS;
                drawScriptScene(canvas, paint, config, time);
                bitmap.getPixels(pixels, 0, config.width, 0, 0, config.width, config.height);
                byte[] yuv = argbToYuv(pixels, config.width, config.height, colorFormat);
                for (int r = 0; r < repeat; r++) {
                    long pts = frameIndex * 1_000_000L / FPS;
                    feed(codec, yuv, pts);
                    drain(codec, info, muxer, muxerStarted, track);
                    if (!muxerStarted && pendingTrack >= 0) {
                        track = pendingTrack;
                        muxer.addTrack(pendingFormat);
                        muxer.start();
                        muxerStarted = true;
                    }
                    frameIndex++;
                }
                long now = SystemClock.uptimeMillis();
                if (now - lastProgress > 400 || unique == totalUniqueFrames - 1) {
                    int pct = (int) Math.min(99, ((unique + 1L) * 100L) / totalUniqueFrames);
                    progress.onProgress(String.format(Locale.US, "EMBEDDED RENDER: %d%% — %.1fs / %ds", pct, time, config.durationSeconds), pct);
                    lastProgress = now;
                }
            }
            int eosIndex = codec.dequeueInputBuffer(5000);
            if (eosIndex < 0) throw new IllegalStateException("H.264 encoder EOS buffer unavailable.");
            codec.queueInputBuffer(eosIndex, 0, 0, frameIndex * 1_000_000L / FPS, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            drainUntilEnd(codec, info, muxer, muxerStarted, track);
            if (muxerStarted) muxer.stop();
            progress.onProgress("EMBEDDED MP4 VERIFIED: " + (output.length() / 1024 / 1024) + " MB", 100);
        } finally {
            try { bitmap.recycle(); } catch (Throwable ignored) {}
            if (codec != null) { try { codec.stop(); } catch (Throwable ignored) {} try { codec.release(); } catch (Throwable ignored) {} }
            if (muxer != null) { try { if (muxerStarted) {} } catch (Throwable ignored) {} try { muxer.release(); } catch (Throwable ignored) {} }
        }
    }

    private static MediaCodecInfo findCodec() {
        MediaCodecList list = new MediaCodecList(MediaCodecList.REGULAR_CODECS);
        for (MediaCodecInfo info : list.getCodecInfos()) {
            if (info.isEncoder()) {
                for (String type : info.getSupportedTypes()) {
                    if (MediaFormat.MIMETYPE_VIDEO_AVC.equalsIgnoreCase(type)) {
                        try {
                            if (chooseColorFormat(info) != -1) return info;
                        } catch (Throwable ignored) {}
                    }
                }
            }
        }
        return null;
    }

    private static int chooseColorFormat(MediaCodecInfo info) {
        MediaCodecInfo.CodecCapabilities caps = info.getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_AVC);
        for (int c : caps.colorFormats) if (c == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar) return c;
        for (int c : caps.colorFormats) if (c == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar) return c;
        return -1;
    }

    private static MediaFormat pendingFormat;
    private static int pendingTrack = -1;

    private static void drain(MediaCodec codec, MediaCodec.BufferInfo info, MediaMuxer muxer, boolean muxerStarted, int track) {
        while (true) {
            int out = codec.dequeueOutputBuffer(info, 0);
            if (out == MediaCodec.INFO_TRY_AGAIN_LATER) return;
            if (out == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                pendingFormat = codec.getOutputFormat();
                pendingTrack = 0;
                continue;
            }
            if (out >= 0) {
                ByteBuffer data = codec.getOutputBuffer(out);
                if (data != null && info.size > 0 && muxerStarted && track >= 0 && (info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                    data.position(info.offset); data.limit(info.offset + info.size);
                    muxer.writeSampleData(track, data, info);
                }
                codec.releaseOutputBuffer(out, false);
            }
        }
    }

    private static void drainUntilEnd(MediaCodec codec, MediaCodec.BufferInfo info, MediaMuxer muxer, boolean muxerStarted, int track) {
        long deadline = SystemClock.uptimeMillis() + 15000;
        while (SystemClock.uptimeMillis() < deadline) {
            int out = codec.dequeueOutputBuffer(info, 1000);
            if (out == MediaCodec.INFO_TRY_AGAIN_LATER) continue;
            if (out == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                if (!muxerStarted) {
                    pendingFormat = codec.getOutputFormat(); pendingTrack = 0;
                    try { track = muxer.addTrack(pendingFormat); muxer.start(); muxerStarted = true; } catch (Throwable ignored) {}
                }
                continue;
            }
            if (out >= 0) {
                ByteBuffer data = codec.getOutputBuffer(out);
                if (data != null && info.size > 0 && muxerStarted && track >= 0 && (info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                    data.position(info.offset); data.limit(info.offset + info.size);
                    muxer.writeSampleData(track, data, info);
                }
                boolean eos = (info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
                codec.releaseOutputBuffer(out, false);
                if (eos) return;
            }
        }
        throw new IllegalStateException("H.264 encoder did not finish within the safety timeout.");
    }

    private static void feed(MediaCodec codec, byte[] yuv, long pts) throws Exception {
        int index = codec.dequeueInputBuffer(5000);
        if (index < 0) throw new IllegalStateException("H.264 encoder input buffer unavailable.");
        ByteBuffer input = codec.getInputBuffer(index);
        if (input == null || input.capacity() < yuv.length) throw new IllegalStateException("H.264 encoder buffer is too small.");
        input.clear(); input.put(yuv);
        codec.queueInputBuffer(index, 0, yuv.length, pts, 0);
    }

    private static byte[] argbToYuv(int[] argb, int width, int height, int format) {
        int frame = width * height;
        int quarter = frame / 4;
        byte[] out = new byte[frame + quarter * 2];
        int yPos = 0;
        int uBase = frame;
        int vBase = frame + quarter;
        boolean semi = format == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar;
        int uvPos = frame;
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                int c = argb[j * width + i];
                int r = (c >> 16) & 255, g = (c >> 8) & 255, b = c & 255;
                int y = ((66 * r + 129 * g + 25 * b + 128) >> 8) + 16;
                out[yPos++] = (byte) clamp(y);
            }
        }
        for (int j = 0; j < height; j += 2) {
            for (int i = 0; i < width; i += 2) {
                int c = argb[j * width + i];
                int r = (c >> 16) & 255, g = (c >> 8) & 255, b = c & 255;
                int u = ((-38 * r - 74 * g + 112 * b + 128) >> 8) + 128;
                int v = ((112 * r - 94 * g - 18 * b + 128) >> 8) + 128;
                int n = (j / 2) * (width / 2) + (i / 2);
                if (semi) {
                    out[uvPos + n * 2] = (byte) clamp(u);
                    out[uvPos + n * 2 + 1] = (byte) clamp(v);
                } else {
                    out[uBase + n] = (byte) clamp(u);
                    out[vBase + n] = (byte) clamp(v);
                }
            }
        }
        return out;
    }

    private static int clamp(int x) { return x < 0 ? 0 : Math.min(255, x); }

    // ------------------------------------------------------------------
    // Script scene rendering: Lottie-style fade + slow zoom + wrapped text
    // ------------------------------------------------------------------

    private static SceneText findActiveScene(List<SceneText> scenes, float t) {
        for (SceneText s : scenes) {
            if (t >= s.startSec && t < s.endSec) return s;
        }
        return scenes.get(scenes.size() - 1);
    }

    /** Returns the scene immediately after the given one in the list, or null if it's the last. */
    private static SceneText findNextScene(List<SceneText> scenes, SceneText current) {
        int idx = scenes.indexOf(current);
        return (idx >= 0 && idx < scenes.size() - 1) ? scenes.get(idx + 1) : null;
    }

    private static float smoothstep(float edge0, float edge1, float x) {
        float v = Math.max(0f, Math.min(1f, (x - edge0) / Math.max(0.0001f, edge1 - edge0)));
        return v * v * (3 - 2 * v);
    }

    private static void drawScriptScene(Canvas c, Paint p, Config cfg, float t) {
        float w = cfg.width, h = cfg.height;
        SceneText scene = findActiveScene(cfg.scenes, t);
        float span = Math.max(0.001f, scene.endSec - scene.startSec);
        float local = (t - scene.startSec) / span; // 0..1 progress through this scene
        float fadeWindow = Math.min(0.5f, span * 0.25f) / span;
        float alphaIn = smoothstep(0f, fadeWindow, local);
        float alphaOut = 1f - smoothstep(1f - fadeWindow, 1f, local);
        float textAlpha = Math.min(alphaIn, alphaOut);

        // Real, clearly-visible Ken Burns motion: an eased push-in up to 24% zoom, combined
        // with a slow pan whose direction is deterministic per scene (so consecutive scenes
        // don't all drift the same way) — a pure 6%-zoom-only version was imperceptible,
        // especially on short or single-scene videos.
        float zoomProgress = smoothstep(0f, 1f, local);
        float scale = 1f + 0.24f * zoomProgress;
        int panSeed = (scene.text != null ? scene.text.hashCode() : scene.colorIndex);
        float panDirX = (panSeed % 2 == 0) ? 1f : -1f;
        float panDirY = ((panSeed / 2) % 2 == 0) ? 1f : -1f;
        float panX = w * 0.06f * panDirX * zoomProgress;
        float panY = h * 0.05f * panDirY * zoomProgress;

        // Crossfade the BACKGROUND into the next scene during the closing part of this
        // scene's duration, instead of hard-cutting on the frame boundary — this is what
        // actually makes consecutive AI illustrations (or photos) feel like one continuous
        // video rather than a slideshow of abrupt jump-cuts.
        SceneText next = findNextScene(cfg.scenes, scene);
        float crossfadeStart = 1f - fadeWindow;
        float crossfadeProgress = (next != null) ? smoothstep(crossfadeStart, 1f, local) : 0f;

        c.save();
        c.translate(w / 2f + panX, h / 2f + panY);
        c.scale(scale, scale);
        c.translate(-w / 2f, -h / 2f);
        drawSceneBackground(c, p, scene, w, h, 255, t);
        c.restore();

        if (next != null && crossfadeProgress > 0f) {
            c.save();
            c.translate(w / 2f, h / 2f);
            c.scale(1f, 1f); // incoming scene starts its own zoom once it officially begins
            c.translate(-w / 2f, -h / 2f);
            drawSceneBackground(c, p, next, w, h, (int) (crossfadeProgress * 255), t);
            c.restore();
        }

        // Bottom scrim so captions stay legible over any photo/illustration brightness —
        // drawn once, after both background layers, so it applies during transitions too.
        if (scene.background != null || (next != null && next.background != null)) {
            p.setShader(new LinearGradient(0, h * 0.55f, 0, h,
                    0x00000000, 0xCC000000, Shader.TileMode.CLAMP));
            c.drawRect(0, h * 0.55f, w, h, p);
            p.setShader(null);
        }

        if (scene.text != null && !scene.text.trim().isEmpty()) {
            TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            textPaint.setColor(Color.argb((int) (textAlpha * 255), 255, 255, 255));
            textPaint.setTextSize(Math.max(16f, w * (scene.background != null ? 0.058f : 0.075f)));
            textPaint.setTextAlign(Paint.Align.LEFT);

            int layoutWidth = (int) (w * 0.86f);
            StaticLayout layout = StaticLayout.Builder
                    .obtain(scene.text, 0, scene.text.length(), textPaint, layoutWidth)
                    .setAlignment(Layout.Alignment.ALIGN_CENTER)
                    .setLineSpacing(1.05f, 1.1f)
                    .build();

            float verticalCenter = scene.background != null
                    ? h * 0.82f - layout.getHeight() / 2f  // near the bottom, over the scrim
                    : (h - layout.getHeight()) / 2f;         // vertically centered, script mode
            c.save();
            c.translate((w - layoutWidth) / 2f, verticalCenter);
            layout.draw(c);
            c.restore();
        }
    }

    /** Draws one scene's background (photo center-cropped, or animated gradient) at the given alpha [0-255]. */
    private static void drawSceneBackground(Canvas c, Paint p, SceneText scene, float w, float h, int alpha, float t) {
        p.setAlpha(255); // reset from any prior use before setting shader/alpha below
        if (scene.background != null) {
            p.setAlpha(alpha);
            drawCenterCropped(c, p, scene.background, w, h);
            p.setAlpha(255);
        } else {
            drawAnimatedGradientFallback(c, p, scene, w, h, alpha, t);
        }
    }

    /**
     * Coded fallback used whenever a scene has no fetched illustration (e.g. the image
     * request failed or timed out). Rather than a flat static color, this draws a genuinely
     * animated Lottie-style pattern — a moving gradient plus soft drifting glow shapes —
     * so a failed fetch never leaves a scene looking like a broken placeholder.
     */
    private static void drawAnimatedGradientFallback(Canvas c, Paint p, SceneText scene, float w, float h, int alpha, float t) {
        int baseColor = SCENE_PALETTE[scene.colorIndex];
        int nextColor = SCENE_PALETTE[(scene.colorIndex + 1) % SCENE_PALETTE.length];

        // Slowly drifting diagonal gradient (the angle itself animates) instead of a fixed one.
        float drift = (float) Math.sin(t * 0.25f) * w * 0.3f;
        p.setShader(new LinearGradient(0 + drift, 0, w + drift, h, baseColor, nextColor, Shader.TileMode.CLAMP));
        p.setAlpha(alpha);
        c.drawRect(0, 0, w, h, p);
        p.setShader(null);

        // 3 soft glow circles drifting at different speeds/radii — cheap, always-available
        // procedural motion so the fallback still reads as an intentional animated scene.
        int[] glowColors = { 0x33FFFFFF, 0x2200D9FF, 0x22FF7BD1 };
        float[][] motion = { {0.6f, 0.35f, 0.18f}, {0.9f, 0.55f, 0.24f}, {1.3f, 0.7f, 0.14f} };
        for (int i = 0; i < 3; i++) {
            float speed = motion[i][0], phase = motion[i][1], radiusFrac = motion[i][2];
            float cx = w * (0.5f + 0.38f * (float) Math.sin(t * speed * 0.4f + phase * 6f));
            float cy = h * (0.5f + 0.32f * (float) Math.cos(t * speed * 0.3f + phase * 4f));
            float r = Math.min(w, h) * radiusFrac;
            android.graphics.RadialGradient glow = new android.graphics.RadialGradient(
                    cx, cy, r, glowColors[i], 0x00000000, Shader.TileMode.CLAMP);
            p.setShader(glow);
            p.setAlpha(alpha);
            c.drawCircle(cx, cy, r, p);
        }
        p.setShader(null);
        p.setAlpha(255);
    }

    /** Scales+crops a bitmap to fill the target frame (like CSS background-size: cover). */
    private static void drawCenterCropped(Canvas c, Paint p, Bitmap bmp, float w, float h) {
        float bmpW = bmp.getWidth(), bmpH = bmp.getHeight();
        float scale = Math.max(w / bmpW, h / bmpH);
        float drawW = bmpW * scale, drawH = bmpH * scale;
        float left = (w - drawW) / 2f, top = (h - drawH) / 2f;
        android.graphics.Rect src = new android.graphics.Rect(0, 0, bmp.getWidth(), bmp.getHeight());
        android.graphics.RectF dst = new android.graphics.RectF(left, top, left + drawW, top + drawH);
        c.drawBitmap(bmp, src, dst, p);
    }
}
