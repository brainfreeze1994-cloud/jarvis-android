package com.jarvis.ai;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Mood Tracker — logs daily mood entries and detects patterns.
 * "I feel great", "mood: happy", "I'm stressed", "my mood report"
 */
public class MoodTracker {

    private static final String PREFS    = "mood_prefs";
    private static final String KEY_DATA = "mood_json";
    private static final String DATE_FMT = "yyyy-MM-dd HH:mm";

    // Mood detection keywords
    private static final Map<String, String[]> MOOD_MAP = new HashMap<String, String[]>() {{
        put("happy",     new String[]{"happy","glad","joy","great","amazing","wonderful","excited","fantastic","awesome"});
        put("sad",       new String[]{"sad","unhappy","down","depressed","upset","miserable","low","blue"});
        put("angry",     new String[]{"angry","mad","frustrated","annoyed","furious","irritated","rage","pissed"});
        put("anxious",   new String[]{"anxious","stressed","nervous","worried","overwhelmed","panic","tense","uneasy"});
        put("tired",     new String[]{"tired","exhausted","sleepy","drained","fatigued","worn out","burnt out"});
        put("calm",      new String[]{"calm","peaceful","relaxed","serene","content","zen","chill","at peace"});
        put("motivated", new String[]{"motivated","inspired","driven","pumped","determined","energetic","ready","focused"});
        put("grateful",  new String[]{"grateful","thankful","blessed","appreciative","lucky"});
        put("lonely",    new String[]{"lonely","alone","isolated","disconnected","empty"});
        put("confused",  new String[]{"confused","lost","unsure","uncertain","overwhelmed","unclear"});
    }};

    static class MoodEntry {
        String mood;
        String note;
        String timestamp;

        MoodEntry(String mood, String note, String timestamp) {
            this.mood = mood; this.note = note; this.timestamp = timestamp;
        }
        MoodEntry(JSONObject j) throws Exception {
            mood = j.getString("mood"); note = j.optString("note", "");
            timestamp = j.optString("timestamp", "");
        }
        JSONObject toJson() throws Exception {
            return new JSONObject().put("mood", mood).put("note", note).put("timestamp", timestamp);
        }
    }

    // ── Load / Save ───────────────────────────────────────────────────────────
    private static List<MoodEntry> load(Context ctx) {
        List<MoodEntry> list = new ArrayList<>();
        String json = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_DATA, "[]");
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) list.add(new MoodEntry(arr.getJSONObject(i)));
        } catch (Exception ignored) {}
        return list;
    }

    private static void save(Context ctx, List<MoodEntry> entries) {
        try {
            JSONArray arr = new JSONArray();
            for (MoodEntry e : entries) arr.put(e.toJson());
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
               .putString(KEY_DATA, arr.toString()).apply();
        } catch (Exception ignored) {}
    }

    // ── Detection ─────────────────────────────────────────────────────────────
    public static boolean isMoodCommand(String text) {
        String lower = text.toLowerCase(Locale.US);
        return lower.startsWith("i feel") || lower.startsWith("feeling ") ||
               lower.startsWith("mood:") || lower.startsWith("my mood") ||
               lower.contains("mood report") || lower.contains("mood history") ||
               lower.contains("how have i been feeling") || lower.contains("mood tracker") ||
               lower.startsWith("i am feeling") || lower.startsWith("i'm feeling") ||
               isMoodKeyword(lower);
    }

    private static boolean isMoodKeyword(String lower) {
        // "I'm stressed", "I'm happy today", etc.
        for (String[] words : MOOD_MAP.values())
            for (String w : words)
                if (lower.startsWith("i'm " + w) || lower.startsWith("i am " + w) ||
                    lower.startsWith("feeling " + w) || lower.startsWith("i feel " + w))
                    return true;
        return false;
    }

    private static String detectMood(String lower) {
        for (Map.Entry<String, String[]> entry : MOOD_MAP.entrySet())
            for (String kw : entry.getValue())
                if (lower.contains(kw)) return entry.getKey();
        return null;
    }

    // ── Handle ────────────────────────────────────────────────────────────────
    public static String handle(Context ctx, String text) {
        String lower = text.toLowerCase(Locale.US);
        List<MoodEntry> entries = load(ctx);

        // Show report
        if (lower.contains("mood report") || lower.contains("mood history") ||
            lower.contains("how have i been feeling") || lower.contains("my mood")) {
            return buildReport(entries);
        }

        // Log mood
        String mood = detectMood(lower);
        if (mood == null) return null;

        String note = text.replaceAll("(?i)(i('m| am|'ve been)? (feeling|feel)|feeling|mood:?)\\s*", "").trim();
        String ts   = new SimpleDateFormat(DATE_FMT, Locale.US).format(new Date());
        entries.add(new MoodEntry(mood, note, ts));
        // Keep last 90 days (approx 3 entries/day max = 270)
        if (entries.size() > 300) entries = entries.subList(entries.size() - 300, entries.size());
        save(ctx, entries);

        return buildResponse(mood, entries);
    }

    private static String buildResponse(String mood, List<MoodEntry> all) {
        // Count recent occurrences
        int recentCount = 0;
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        for (int i = all.size() - 1; i >= Math.max(0, all.size() - 7); i--)
            if (all.get(i).mood.equals(mood)) recentCount++;

        Map<String, String[]> responses = new HashMap<String, String[]>() {{
            put("happy",     new String[]{"warm",    "Loving that energy, sir. Happiness looks brilliant on you.",
                                                      "You've been feeling happy a lot lately. Keep doing whatever you're doing, sir."});
            put("sad",       new String[]{"concerned","I'm sorry to hear that, sir. Want to talk about it? I'm right here.",
                                                      "You've had a rough patch lately, sir. Remember — this is temporary. I've got you."});
            put("angry",     new String[]{"concerned","Understood, sir. Take a breath. Whatever it is — you'll handle it.",
                                                      "Seems like you've been frustrated a lot lately. Want me to help find a solution?"});
            put("anxious",   new String[]{"concerned","That sounds tough, sir. Deep breath. You've handled everything thrown at you so far.",
                                                      "You've been anxious lately. Let's tackle what's worrying you — one thing at a time."});
            put("tired",     new String[]{"warm",    "Rest well, sir. Even the best machines need downtime.",
                                                      "You've mentioned being tired several times, sir. Make sure you're sleeping enough."});
            put("calm",      new String[]{"excited",  "Brilliant. Calm mind, sharp thinking. This is your best state, sir.",
                                                      "You've been consistently calm. That's a superpower, sir."});
            put("motivated", new String[]{"proud",   "That's the spirit, sir. Let's make it count.",
                                                      "You've been on a roll lately. Channel that energy!"});
            put("grateful",  new String[]{"warm",    "Gratitude is powerful, sir. That mindset will take you far.",
                                                      "You've been grateful often lately. That's a beautiful trait, sir."});
            put("lonely",    new String[]{"concerned","You're never truly alone, sir. I'm always here. Talk to me.",
                                                      "I've noticed you've felt lonely a few times. Reach out to someone you care about, sir."});
            put("confused",  new String[]{"neutral",  "Clarity will come, sir. Let's break it down together.",
                                                      "You've been uncertain lately. Sometimes confusion is just growth in disguise."});
        }};

        String[] resp = responses.getOrDefault(mood, new String[]{"neutral",
            "Noted, sir. I'm keeping track of how you feel.", "Pattern noted, sir."});

        String emotion  = resp[0];
        String reply    = recentCount >= 3 ? resp[2] : resp[1];
        String moodIcon = moodIcon(mood);

        return "[EMOTION:" + emotion + "] " + moodIcon + " Logged: **" + capitalize(mood) + "**. " + reply;
    }

    private static String buildReport(List<MoodEntry> entries) {
        if (entries.isEmpty())
            return "[EMOTION:neutral] No mood entries yet, sir. Tell me how you're feeling anytime.";

        Map<String, Integer> counts = new HashMap<>();
        for (MoodEntry e : entries) counts.merge(e.mood, 1, Integer::sum);

        // Sort by count
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(counts.entrySet());
        sorted.sort((a, b) -> b.getValue() - a.getValue());

        StringBuilder sb = new StringBuilder("[EMOTION:warm] **Your Mood Report, sir:**\n\n");
        sb.append("Total entries: **").append(entries.size()).append("**\n\n");
        sb.append("**Mood breakdown:**\n");
        for (Map.Entry<String, Integer> e : sorted) {
            double pct = (double) e.getValue() / entries.size() * 100;
            sb.append(String.format(Locale.US, "%s **%s** — %d entries (%.0f%%)\n",
                moodIcon(e.getKey()), capitalize(e.getKey()), e.getValue(), pct));
        }

        // Last 5 entries
        sb.append("\n**Recent entries:**\n");
        int from = Math.max(0, entries.size() - 5);
        for (int i = from; i < entries.size(); i++) {
            MoodEntry e = entries.get(i);
            sb.append(String.format(Locale.US, "%s %s — %s\n",
                moodIcon(e.mood), e.timestamp, capitalize(e.mood)));
        }
        return sb.toString().trim();
    }

    private static String moodIcon(String mood) {
        switch (mood) {
            case "happy":     return "😊";
            case "sad":       return "😢";
            case "angry":     return "😠";
            case "anxious":   return "😰";
            case "tired":     return "😴";
            case "calm":      return "😌";
            case "motivated": return "💪";
            case "grateful":  return "🙏";
            case "lonely":    return "😔";
            case "confused":  return "😕";
            default:          return "💭";
        }
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
