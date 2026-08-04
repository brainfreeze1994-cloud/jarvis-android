package com.jarvis.ai;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Smart Memory — HENRY learns user preferences across sessions.
 * Extracts and stores: likes, dislikes, habits, preferences, facts about user.
 * "What do you know about me?", "Forget that I like X", "Remember I prefer Y"
 */
public class SmartMemory {

    private static final String PREFS        = "smart_memory_prefs";
    private static final String KEY_FACTS    = "user_facts";
    private static final String KEY_PREFS    = "user_preferences";
    private static final String KEY_PATTERNS = "usage_patterns";
    private static final int    MAX_FACTS    = 50;

    public static boolean isMemoryCommand(String text) {
        String lower = text.toLowerCase(Locale.US);
        return lower.contains("what do you know about me") ||
               lower.contains("what have you learned") ||
               lower.contains("forget that") || lower.contains("remember that") ||
               lower.contains("remember i ") || lower.contains("i told you") ||
               lower.contains("my memory") || lower.contains("clear memory") ||
               lower.contains("what do you remember");
    }

    public static String handle(Context ctx, String text) {
        String lower = text.toLowerCase(Locale.US);
        if (lower.contains("what do you know") || lower.contains("what have you learned") ||
            lower.contains("what do you remember") || lower.contains("my memory")) {
            return getMemorySummary(ctx);
        }
        if (lower.contains("clear memory") || lower.contains("forget everything")) {
            return clearMemory(ctx);
        }
        if (lower.contains("forget that") || lower.contains("remove that")) {
            String what = text.replaceAll("(?i)forget that|remove that", "").trim();
            return forgetFact(ctx, what);
        }
        if (lower.contains("remember that") || lower.contains("remember i ") || lower.contains("note that")) {
            String fact = text.replaceAll("(?i)^(remember that|remember|note that)\\s*", "").trim();
            return manuallyRemember(ctx, fact);
        }
        return "[EMOTION:neutral] What would you like me to remember or forget, sir?";
    }

    /** Auto-learn from conversation — call after every user message */
    public static void learnFromMessage(Context ctx, String userText) {
        String lower = userText.toLowerCase(Locale.US);
        List<String> facts = new ArrayList<>();

        // Detect preferences
        if (lower.contains("i like ") || lower.contains("i love ") || lower.contains("i enjoy ")) {
            String fact = extractAfter(lower, new String[]{"i like ", "i love ", "i enjoy "});
            if (!fact.isEmpty()) facts.add("Likes: " + capitalize(fact));
        }
        if (lower.contains("i don't like") || lower.contains("i hate ") || lower.contains("i dislike")) {
            String fact = extractAfter(lower, new String[]{"i don't like ", "i hate ", "i dislike "});
            if (!fact.isEmpty()) facts.add("Dislikes: " + capitalize(fact));
        }
        if (lower.contains("i prefer ") || lower.contains("i usually ") || lower.contains("i always ")) {
            String fact = extractAfter(lower, new String[]{"i prefer ", "i usually ", "i always "});
            if (!fact.isEmpty()) facts.add("Habit: " + capitalize(fact));
        }
        if (lower.contains("i work at ") || lower.contains("i work in ") || lower.contains("my job is")) {
            String fact = extractAfter(lower, new String[]{"i work at ", "i work in ", "my job is "});
            if (!fact.isEmpty()) facts.add("Works at: " + capitalize(fact));
        }
        if (lower.contains("i live in ") || lower.contains("i'm from ") || lower.contains("i am from ")) {
            String fact = extractAfter(lower, new String[]{"i live in ", "i'm from ", "i am from "});
            if (!fact.isEmpty()) facts.add("Location: " + capitalize(fact));
        }
        if (lower.contains("my name is ") || lower.contains("call me ")) {
            String fact = extractAfter(lower, new String[]{"my name is ", "call me "});
            if (!fact.isEmpty()) facts.add("Name: " + capitalize(fact));
        }

        for (String f : facts) saveFact(ctx, f);
    }

    public static String manuallyRemember(Context ctx, String fact) {
        if (fact.isEmpty()) return "[EMOTION:neutral] What should I remember, sir?";
        saveFact(ctx, "Note: " + fact);
        return "[EMOTION:warm] Got it, sir. I'll remember that: **" + fact + "**";
    }

    private static String getMemorySummary(Context ctx) {
        try {
            JSONArray facts = loadFacts(ctx);
            if (facts.length() == 0)
                return "[EMOTION:neutral] I haven't learned anything about you yet, sir. Tell me about yourself — I'll remember.";
            StringBuilder sb = new StringBuilder("[EMOTION:warm] **🧠 What I know about you, sir:**\n\n");
            for (int i = 0; i < facts.length(); i++) {
                JSONObject o = facts.getJSONObject(i);
                sb.append("• ").append(o.getString("fact")).append("\n");
            }
            return sb.toString();
        } catch (Exception e) { return "[EMOTION:neutral] Memory unavailable, sir."; }
    }

    private static String clearMemory(Context ctx) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
           .remove(KEY_FACTS).apply();
        return "[EMOTION:neutral] Memory cleared, sir. Fresh start.";
    }

    private static String forgetFact(Context ctx, String what) {
        try {
            JSONArray facts = loadFacts(ctx);
            JSONArray updated = new JSONArray();
            boolean removed = false;
            for (int i = 0; i < facts.length(); i++) {
                JSONObject o = facts.getJSONObject(i);
                if (!o.getString("fact").toLowerCase(Locale.US).contains(what.toLowerCase(Locale.US))) {
                    updated.put(o);
                } else { removed = true; }
            }
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
               .putString(KEY_FACTS, updated.toString()).apply();
            return removed ? "[EMOTION:neutral] Forgotten, sir." : "[EMOTION:neutral] I didn't have that in memory, sir.";
        } catch (Exception e) { return "[EMOTION:neutral] Couldn't update memory, sir."; }
    }

    private static void saveFact(Context ctx, String fact) {
        try {
            JSONArray facts = loadFacts(ctx);
            // Check for duplicate
            for (int i = 0; i < facts.length(); i++) {
                if (facts.getJSONObject(i).getString("fact").equalsIgnoreCase(fact)) return;
            }
            JSONObject entry = new JSONObject();
            entry.put("fact", fact);
            entry.put("date", new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date()));
            facts.put(entry);
            // Keep last MAX_FACTS
            while (facts.length() > MAX_FACTS) facts.remove(0);
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
               .putString(KEY_FACTS, facts.toString()).apply();
        } catch (Exception ignored) {}
    }

    /** Build memory context string to inject into AI prompts */
    public static String buildMemoryContext(Context ctx) {
        try {
            JSONArray facts = loadFacts(ctx);
            if (facts.length() == 0) return "";
            StringBuilder sb = new StringBuilder("Known facts about user: ");
            for (int i = 0; i < facts.length(); i++) {
                sb.append(facts.getJSONObject(i).getString("fact"));
                if (i < facts.length() - 1) sb.append("; ");
            }
            return sb.toString();
        } catch (Exception e) { return ""; }
    }

    private static JSONArray loadFacts(Context ctx) {
        try { return new JSONArray(ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_FACTS, "[]")); }
        catch (Exception e) { return new JSONArray(); }
    }

    private static String extractAfter(String text, String[] patterns) {
        for (String p : patterns) {
            int idx = text.indexOf(p);
            if (idx >= 0) {
                String after = text.substring(idx + p.length()).trim();
                // Take first clause (before comma, period, and/or)
                after = after.split("[,\\.;]")[0].split("\\band\\b")[0].trim();
                if (after.length() > 2 && after.length() < 80) return after;
            }
        }
        return "";
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
