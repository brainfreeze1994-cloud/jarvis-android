package com.jarvis.ai;

import android.app.KeyguardManager;
import android.content.Context;
import android.media.AudioManager;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;

import java.util.Locale;

/**
 * Focus Mode — DND + screen dimming + motivation message.
 * "Focus mode" / "Start focus mode" / "I need to focus" / "Stop focus mode"
 */
public class FocusMode {

    private static boolean active = false;
    private static PowerManager.WakeLock wakeLock;

    public static boolean isActive() { return active; }

    public static boolean isFocusCommand(String text) {
        String lower = text.toLowerCase(Locale.US);
        return lower.contains("focus mode") || lower.contains("start focus") ||
               lower.contains("i need to focus") || lower.contains("help me focus") ||
               lower.contains("concentration mode") || lower.contains("deep work") ||
               lower.contains("stop focus") || lower.contains("end focus") ||
               lower.contains("disable focus") || lower.contains("exit focus");
    }

    public static String activate(Context ctx, UserProfile profile) {
        active = true;

        // Enable DND
        AudioManager am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
        if (am != null) am.setRingerMode(AudioManager.RINGER_MODE_SILENT);

        // Dim brightness
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.System.canWrite(ctx)) {
                Settings.System.putInt(ctx.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS, 30);
            }
        } catch (Exception ignored) {}

        String name = (profile != null && !profile.name.isEmpty()) ? profile.name : "sir";
        String[] motivations = {
            "The successful warrior is the average man with laser-like focus. — Bruce Lee",
            "Concentrate all your thoughts upon the work in hand. — Alexander Graham Bell",
            "Where focus goes, energy flows. — Tony Robbins",
            "The secret of getting ahead is getting started. — Mark Twain",
            "Do the hard jobs first. The easy jobs will take care of themselves. — Dale Carnegie",
            "Focus is not about saying yes to things — it's about saying no to a thousand things. — Steve Jobs"
        };
        String quote = motivations[(int)(System.currentTimeMillis() / 60000) % motivations.length];

        return String.format(Locale.US,
            "[EMOTION:serious] **Focus Mode activated, sir.**\n\n" +
            "DND enabled. Notifications silenced. Screen dimmed.\n\n" +
            "_%s_\n\n" +
            "Say **'stop focus'** when done, %s.", quote, name);
    }

    public static String deactivate(Context ctx) {
        active = false;

        // Restore ringer
        AudioManager am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
        if (am != null) am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);

        // Restore brightness
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.System.canWrite(ctx)) {
                Settings.System.putInt(ctx.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS, 200);
            }
        } catch (Exception ignored) {}

        if (wakeLock != null && wakeLock.isHeld()) { wakeLock.release(); wakeLock = null; }

        return "[EMOTION:excited] **Focus Mode deactivated, sir.** Well done — back to normal settings. How did it go?";
    }
}
