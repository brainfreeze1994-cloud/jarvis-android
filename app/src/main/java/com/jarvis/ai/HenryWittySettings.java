package com.jarvis.ai;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Persisted witty-engine preferences (intensity + personality), threaded end-to-end:
 * chat command -> SharedPreferences -> JarvisApi request body -> api/jarvis.js ->
 * witty_engine.js -> actual response tone. See api/witty_engine.js for the full
 * authoritative specification of what each intensity/personality value means.
 */
public final class HenryWittySettings {
    private static final String PREFS = "henry_witty_settings";
    private static final String KEY_INTENSITY = "witty_intensity";
    private static final String KEY_PERSONALITY = "witty_personality";

    public static final String DEFAULT_INTENSITY = "NORMAL";
    public static final String DEFAULT_PERSONALITY = "PLAYFUL";

    public static final String[] VALID_INTENSITIES = {
            "OFF", "LIGHT", "NORMAL", "HIGH", "SAVAGE", "BARDAGULAN", "BRUTAL"
    };
    public static final String[] VALID_PERSONALITIES = {
            "CLEVER", "PLAYFUL", "SARCASTIC", "DRY", "CHAOTIC", "CAMP", "SAVAGE",
            "DEADPAN", "ABSURD", "BAKLA", "BARDAGULAN", "FRIENDLY_TEASING"
    };

    private HenryWittySettings() {}

    public static String getIntensity(Context context) {
        if (context == null) return DEFAULT_INTENSITY;
        return prefs(context).getString(KEY_INTENSITY, DEFAULT_INTENSITY);
    }

    public static String getPersonality(Context context) {
        if (context == null) return DEFAULT_PERSONALITY;
        return prefs(context).getString(KEY_PERSONALITY, DEFAULT_PERSONALITY);
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static boolean setIntensity(Context context, String value) {
        String upper = value == null ? "" : value.trim().toUpperCase(Locale.US);
        for (String v : VALID_INTENSITIES) {
            if (v.equals(upper)) {
                prefs(context).edit().putString(KEY_INTENSITY, upper).apply();
                return true;
            }
        }
        return false;
    }

    private static boolean setPersonality(Context context, String value) {
        String upper = value == null ? "" : value.trim().toUpperCase(Locale.US);
        for (String v : VALID_PERSONALITIES) {
            if (v.equals(upper)) {
                prefs(context).edit().putString(KEY_PERSONALITY, upper).apply();
                return true;
            }
        }
        return false;
    }

    /**
     * Parses a chat command like "set witty intensity to SAVAGE" or "witty style CAMP".
     * Returns a confirmation/error message if the text was a witty-settings command,
     * or null if it wasn't one at all (so the caller can fall through to normal handling).
     */
    public static String tryHandleCommand(Context context, String rawText) {
        if (rawText == null || context == null) return null;
        String t = rawText.trim();

        Matcher mi = Pattern.compile("(?i)\\bwitty\\s*(?:intensity|level)\\s*(?:to|=|:)?\\s*([a-z_]+)\\b").matcher(t);
        if (mi.find()) {
            String val = mi.group(1);
            if (setIntensity(context, val)) {
                return "🎚️ Witty intensity set to **" + val.toUpperCase(Locale.US) + "**.";
            }
            return "That's not a valid witty intensity. Choose one of: " + String.join(", ", VALID_INTENSITIES) + ".";
        }

        Matcher mp = Pattern.compile("(?i)\\bwitty\\s*(?:personality|style)\\s*(?:to|=|:)?\\s*([a-z_]+)\\b").matcher(t);
        if (mp.find()) {
            String val = mp.group(1);
            if (setPersonality(context, val)) {
                return "🎭 Witty personality set to **" + val.toUpperCase(Locale.US) + "**.";
            }
            return "That's not a valid witty personality. Choose one of: " + String.join(", ", VALID_PERSONALITIES) + ".";
        }

        return null;
    }
}
