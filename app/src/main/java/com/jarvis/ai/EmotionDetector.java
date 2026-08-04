package com.jarvis.ai;

import java.util.Locale;

/**
 * EmotionDetector — reads HOW the user types and classifies their emotional state.
 * HENRY responds differently based on detected emotion.
 */
public class EmotionDetector {

    public enum EmotionalState {
        NORMAL, FRUSTRATED, STRESSED, SAD, EXCITED
    }

    public static EmotionalState detect(String text) {
        if (text == null || text.isEmpty()) return EmotionalState.NORMAL;

        String lower = text.toLowerCase(Locale.US);
        int words    = text.trim().split("\\s+").length;

        // Count caps ratio
        int letters = 0, caps = 0;
        for (char c : text.toCharArray()) {
            if (Character.isLetter(c)) { letters++; if (Character.isUpperCase(c)) caps++; }
        }
        float capsRatio = letters > 0 ? (float) caps / letters : 0;

        // Count exclamations
        int excl = 0;
        for (char c : text.toCharArray()) if (c == '!') excl++;

        // FRUSTRATED — mostly caps, angry words, or multiple exclamations
        if (capsRatio > 0.55f && letters > 5) return EmotionalState.FRUSTRATED;
        if (excl > 2) return EmotionalState.FRUSTRATED;
        if (containsAny(lower, "hate", "terrible", "worst", "stupid", "useless", "idiot",
                "broken", "garbage", "trash", "ridiculous", "awful", "horrible")) {
            return EmotionalState.FRUSTRATED;
        }

        // STRESSED — urgent/desperate keywords, or very short panicked messages
        if (containsAny(lower, "stressed", "overwhelmed", "can't cope", "help me",
                "urgent", "emergency", "asap", "panic", "freaking out", "losing my mind",
                "don't know what to do", "please help", "need help now")) {
            return EmotionalState.STRESSED;
        }
        if (words <= 4 && lower.endsWith("?") && containsAny(lower, "how","what","why","help","fix")) {
            return EmotionalState.STRESSED;
        }

        // SAD — sadness keywords
        if (containsAny(lower, "sad", "depressed", "lonely", "crying", "alone",
                "hopeless", "worthless", "tired of", "exhausted", "can't take it",
                "give up", "miss you", "heartbroken", "devastated")) {
            return EmotionalState.SAD;
        }

        // EXCITED — positive high-energy
        if (containsAny(lower, "amazing", "awesome", "excited", "love it", "fantastic",
                "great news", "finally", "cant wait", "can't wait", "thrilled",
                "so happy", "yay", "yes!!", "let's go", "incredible")) {
            return EmotionalState.EXCITED;
        }

        return EmotionalState.NORMAL;
    }

    public static String toApiString(EmotionalState state) {
        switch (state) {
            case FRUSTRATED: return "frustrated";
            case STRESSED:   return "stressed";
            case SAD:        return "sad";
            case EXCITED:    return "excited";
            default:         return "normal";
        }
    }

    /** Get a short display label for the orb status hint */
    public static String getOrbHint(EmotionalState state, String defaultHint) {
        switch (state) {
            case FRUSTRATED: return "I HEAR YOU, SIR. LET'S FIX THIS.";
            case STRESSED:   return "FOCUS. I'VE GOT YOU, SIR.";
            case SAD:        return "I'M HERE, SIR. ALWAYS.";
            case EXCITED:    return "EXCELLENT NEWS, SIR!";
            default:         return defaultHint;
        }
    }

    private static boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) if (text.contains(kw)) return true;
        return false;
    }
}
