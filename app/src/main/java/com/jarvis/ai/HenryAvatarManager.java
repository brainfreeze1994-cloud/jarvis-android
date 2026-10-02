package com.jarvis.ai;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

/**
 * HENRY's visual identity: a single, consistent AI-generated portrait (good-looking, athletic
 * build, visible arm tattoo), fetched once with a FIXED seed and cached permanently to disk —
 * every subsequent load is the exact same face, not a new random image. Used as the avatar in
 * the chat UI (ChatAdapter) in place of the plain "HNR" text circle.
 *
 * Deliberately a static portrait rather than an attempt at precise lip-sync: overlaying a drawn
 * mouth shape on an AI-generated photo tends to look mismatched (lighting/texture don't line up)
 * rather than convincingly alive. The "HENRY is speaking" cue is instead a glow ring around the
 * avatar (see ChatAdapter's speaking-position handling), which reads clearly without that risk.
 */
public final class HenryAvatarManager {
    private static final String CACHE_FILENAME = "henry_avatar_portrait.png";
    private static final int FIXED_SEED = 824601; // never change — this is what makes the face consistent
    private static final int SIZE = 512;
    private static final int CONNECT_TIMEOUT_MS = 15000;
    private static final int READ_TIMEOUT_MS = 45000;

    private static final String AVATAR_PROMPT =
            "handsome athletic filipino man in his early 30s, short dark hair, confident warm " +
            "smile, wearing a fitted charcoal grey crew neck shirt, detailed tattoo sleeve " +
            "visible on one arm, professional studio portrait, soft lighting, looking directly " +
            "at camera, upper body shot, no text or watermark";

    private static volatile Bitmap memoryCache;

    public interface AvatarCallback {
        void onAvatarReady(Bitmap avatar);
    }

    private HenryAvatarManager() {}

    /** Returns the cached avatar immediately if already loaded in memory, else null. Never blocks. */
    public static Bitmap peekCached() {
        return memoryCache;
    }

    /**
     * Loads HENRY's portrait: memory cache -> disk cache -> network (first run only). Callback
     * always runs on the calling thread's Handler via the caller — this method itself does the
     * network/disk work on a background thread and posts nothing, so callers should hop back to
     * the main thread inside their callback if updating UI.
     */
    public static void loadAvatar(Context context, AvatarCallback callback) {
        if (memoryCache != null) {
            callback.onAvatarReady(memoryCache);
            return;
        }
        new Thread(() -> {
            Bitmap result = loadFromDisk(context);
            if (result == null) {
                result = fetchFromNetwork();
                if (result != null) saveToDisk(context, result);
            }
            if (result != null) memoryCache = result;
            callback.onAvatarReady(result);
        }, "henry-avatar-load").start();
    }

    private static Bitmap loadFromDisk(Context context) {
        try {
            File f = new File(context.getFilesDir(), CACHE_FILENAME);
            if (!f.exists()) return null;
            return BitmapFactory.decodeFile(f.getAbsolutePath());
        } catch (Throwable e) {
            return null;
        }
    }

    private static void saveToDisk(Context context, Bitmap bmp) {
        try (FileOutputStream out = new FileOutputStream(new File(context.getFilesDir(), CACHE_FILENAME))) {
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (Throwable ignored) {}
    }

    private static Bitmap fetchFromNetwork() {
        for (int attempt = 0; attempt < 2; attempt++) {
            HttpURLConnection conn = null;
            try {
                String url = "https://image.pollinations.ai/prompt/" +
                        URLEncoder.encode(AVATAR_PROMPT, "UTF-8") +
                        "?model=flux&seed=" + FIXED_SEED + "&width=" + SIZE + "&height=" + SIZE + "&nologo=true";
                conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
                conn.setReadTimeout(READ_TIMEOUT_MS);
                conn.setRequestProperty("User-Agent", "HENRY-Android/1.0");
                conn.connect();
                if (conn.getResponseCode() != 200) continue;
                try (InputStream in = conn.getInputStream()) {
                    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                    byte[] chunk = new byte[8192];
                    int n;
                    while ((n = in.read(chunk)) != -1) buffer.write(chunk, 0, n);
                    byte[] bytes = buffer.toByteArray();
                    Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    if (bmp != null) return bmp;
                }
            } catch (Throwable e) {
                // try again on next loop iteration, or give up after both attempts
            } finally {
                if (conn != null) conn.disconnect();
            }
        }
        return null;
    }
}
