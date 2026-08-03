package com.jarvis.ai;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Build;
import android.view.KeyEvent;

import java.util.Locale;

/**
 * Music control — handles media keys (play/pause/next/prev) and volume,
 * and opens common music apps by voice command.
 */
public class MusicControl {

    private static final String[][] MUSIC_APPS = {
        {"spotify",       "com.spotify.music",              "Spotify"},
        {"youtube music", "com.google.android.apps.youtube.music", "YouTube Music"},
        {"youtube",       "com.google.android.youtube",     "YouTube"},
        {"apple music",   "com.apple.android.music",        "Apple Music"},
        {"deezer",        "com.deezer.android",             "Deezer"},
        {"soundcloud",    "com.soundcloud.android",         "SoundCloud"},
        {"amazon music",  "com.amazon.mp3",                 "Amazon Music"},
        {"tidal",         "com.aspiro.tidal",               "TIDAL"},
    };

    public static boolean isMusicCommand(String text) {
        String lower = text.toLowerCase(Locale.US);
        return lower.contains("play music") || lower.contains("pause music") ||
               lower.contains("stop music") || lower.contains("next song") ||
               lower.contains("previous song") || lower.contains("skip song") ||
               lower.contains("next track") || lower.contains("previous track") ||
               lower.contains("volume up") || lower.contains("volume down") ||
               lower.contains("mute music") || lower.contains("play spotify") ||
               lower.contains("open spotify") || lower.contains("play youtube") ||
               lower.contains("music louder") || lower.contains("music softer") ||
               lower.contains("music quieter") || lower.contains("resume music") ||
               lower.contains("open music");
    }

    public static String handle(Context ctx, String text) {
        String lower = text.toLowerCase(Locale.US);
        AudioManager am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);

        // ── Open specific app ────────────────────────────────────────────────
        for (String[] app : MUSIC_APPS) {
            if (lower.contains(app[0])) {
                if (appInstalled(ctx, app[1])) {
                    ctx.startActivity(ctx.getPackageManager().getLaunchIntentForPackage(app[1]));
                    return "[EMOTION:excited] Opening " + app[2] + " for you, sir.";
                } else {
                    // Open Play Store
                    Intent store = new Intent(Intent.ACTION_VIEW,
                        android.net.Uri.parse("https://play.google.com/store/apps/details?id=" + app[1]));
                    store.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    ctx.startActivity(store);
                    return "[EMOTION:neutral] " + app[2] + " isn't installed, sir. Opening Play Store.";
                }
            }
        }

        // ── Playback controls ─────────────────────────────────────────────────
        if (lower.contains("play music") || lower.contains("resume music") || lower.contains("unpause")) {
            sendMediaKey(ctx, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
            return "[EMOTION:excited] Playing music, sir.";
        }
        if (lower.contains("pause") || lower.contains("stop music")) {
            sendMediaKey(ctx, KeyEvent.KEYCODE_MEDIA_PAUSE);
            return "[EMOTION:neutral] Music paused, sir.";
        }
        if (lower.contains("next") || lower.contains("skip")) {
            sendMediaKey(ctx, KeyEvent.KEYCODE_MEDIA_NEXT);
            return "[EMOTION:excited] Skipping to next track, sir.";
        }
        if (lower.contains("previous") || lower.contains("back")) {
            sendMediaKey(ctx, KeyEvent.KEYCODE_MEDIA_PREVIOUS);
            return "[EMOTION:neutral] Going back a track, sir.";
        }

        // ── Volume ────────────────────────────────────────────────────────────
        if (lower.contains("volume up") || lower.contains("louder")) {
            if (am != null) am.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
            return "[EMOTION:neutral] Turning it up, sir.";
        }
        if (lower.contains("volume down") || lower.contains("softer") || lower.contains("quieter")) {
            if (am != null) am.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
            return "[EMOTION:neutral] Lowering the volume, sir.";
        }
        if (lower.contains("mute music")) {
            if (am != null) am.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_MUTE, 0);
            return "[EMOTION:neutral] Music muted, sir.";
        }

        // ── Generic open music ────────────────────────────────────────────────
        if (lower.contains("open music")) {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_APP_MUSIC);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                ctx.startActivity(intent);
                return "[EMOTION:excited] Opening music player, sir.";
            } catch (Exception e) {
                sendMediaKey(ctx, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
                return "[EMOTION:neutral] Toggling music playback, sir.";
            }
        }

        // ── Fallback: toggle play/pause ───────────────────────────────────────
        sendMediaKey(ctx, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
        return "[EMOTION:excited] Toggling music, sir.";
    }

    private static void sendMediaKey(Context ctx, int keyCode) {
        AudioManager am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
        if (am == null) return;
        am.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyCode));
        am.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,   keyCode));
    }

    private static boolean appInstalled(Context ctx, String pkg) {
        try {
            ctx.getPackageManager().getPackageInfo(pkg, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) { return false; }
    }
}
