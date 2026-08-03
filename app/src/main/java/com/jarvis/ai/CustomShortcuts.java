package com.jarvis.ai;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Voice shortcuts: "When I say X, do Y"
 * Stored as trigger → action pairs.
 */
public class CustomShortcuts {

    private static final String PREFS = "henry_shortcuts";
    private static final String KEY   = "shortcuts";
    private static final Gson   GSON  = new Gson();

    public static class Shortcut {
        public String trigger;  // what user says
        public String action;   // what HENRY does / says back
        public Shortcut(String trigger, String action) {
            this.trigger = trigger; this.action = action;
        }
    }

    // ── Detect "teach me" command ─────────────────────────────────────────────
    /**
     * Returns [trigger, action] if text is a shortcut definition, else null.
     * Patterns:
     *   "when I say X do Y"
     *   "if I say X then Y"
     *   "shortcut X = Y"
     *   "teach: X → Y"
     */
    public static String[] parseDefinition(String text) {
        String t = text.trim();
        java.util.regex.Matcher m;

        m = java.util.regex.Pattern.compile(
            "(?:when|if)\\s+i\\s+say\\s+[\"']?(.+?)[\"']?\\s+(?:do|then|:)\\s+(.+)",
            java.util.regex.Pattern.CASE_INSENSITIVE).matcher(t);
        if (m.find()) return new String[]{ m.group(1).trim(), m.group(2).trim() };

        m = java.util.regex.Pattern.compile(
            "(?:shortcut|teach)\\s*[:\"]?\\s*(.+?)\\s*[=→>]+\\s*(.+)",
            java.util.regex.Pattern.CASE_INSENSITIVE).matcher(t);
        if (m.find()) return new String[]{ m.group(1).trim(), m.group(2).trim() };

        return null;
    }

    // ── Match against stored shortcuts ────────────────────────────────────────
    /**
     * Returns the action string if userText matches any trigger, else null.
     */
    public static String match(Context ctx, String userText) {
        String t = userText.toLowerCase(Locale.US).trim();
        List<Shortcut> list = load(ctx);
        for (Shortcut s : list) {
            if (s.trigger != null && t.contains(s.trigger.toLowerCase(Locale.US))) {
                return s.action;
            }
        }
        return null;
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────
    public static String add(Context ctx, String trigger, String action) {
        List<Shortcut> list = load(ctx);
        // Update if trigger already exists
        for (Shortcut s : list) {
            if (s.trigger.equalsIgnoreCase(trigger)) {
                s.action = action; save(ctx, list);
                return "[EMOTION:proud] Updated shortcut, sir. \"" + trigger + "\" → " + action;
            }
        }
        list.add(new Shortcut(trigger.toLowerCase(Locale.US), action));
        save(ctx, list);
        return "[EMOTION:excited] Got it, sir. When you say **\"" + trigger + "\"** I'll " + action + ".";
    }

    public static String listAll(Context ctx) {
        List<Shortcut> list = load(ctx);
        if (list.isEmpty()) return "[EMOTION:warm] No shortcuts yet, sir. Teach me one with \"When I say X, do Y\".";
        StringBuilder sb = new StringBuilder("[EMOTION:neutral] **Your shortcuts, sir:**\n\n");
        for (int i = 0; i < list.size(); i++)
            sb.append("**").append(i+1).append(".** \"").append(list.get(i).trigger)
              .append("\" → ").append(list.get(i).action).append("\n");
        return sb.toString().trim();
    }

    public static String delete(Context ctx, String trigger) {
        List<Shortcut> list = load(ctx);
        boolean removed = list.removeIf(s -> s.trigger.equalsIgnoreCase(trigger));
        if (removed) { save(ctx, list); return "[EMOTION:neutral] Shortcut removed, sir."; }
        return "[EMOTION:neutral] Shortcut not found, sir.";
    }

    public static String clearAll(Context ctx) {
        prefs(ctx).edit().remove(KEY).apply();
        return "[EMOTION:neutral] All shortcuts cleared, sir.";
    }

    private static List<Shortcut> load(Context ctx) {
        String json = prefs(ctx).getString(KEY, null);
        if (json == null) return new ArrayList<>();
        try {
            Type type = new TypeToken<List<Shortcut>>(){}.getType();
            List<Shortcut> list = GSON.fromJson(json, type);
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) { return new ArrayList<>(); }
    }

    private static void save(Context ctx, List<Shortcut> list) {
        prefs(ctx).edit().putString(KEY, GSON.toJson(list)).apply();
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
