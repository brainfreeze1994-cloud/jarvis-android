package com.jarvis.ai;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VoiceNotes {

    private static final String PREFS_NAME = "henry_notes";
    private static final String KEY_NOTES  = "notes_list";
    private static final Gson   GSON       = new Gson();

    public static class Note {
        public String text;
        public String timestamp;
        public Note(String text, String timestamp) {
            this.text = text;
            this.timestamp = timestamp;
        }
    }

    // ── Detect save command ───────────────────────────────────────────────────
    /** Returns content to save, or null if not a save command. */
    public static String parseSaveCommand(String text) {
        String t = text.trim();
        // "save note: …", "note down …", "remember that …", "note: …"
        Matcher m = Pattern.compile(
            "(?:save\\s+(?:a\\s+)?note[:\\-]?|note\\s+down[:\\-]?|remember\\s+(?:that\\s+)?[:\\-]?|note[:\\-])\\s*(.+)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(t);
        if (m.find()) return m.group(1).trim();
        return null;
    }

    // ── Detect recall command ─────────────────────────────────────────────────
    public static boolean isRecallCommand(String text) {
        String t = text.toLowerCase(Locale.US);
        return t.contains("my notes") || t.contains("show notes")
            || t.contains("list notes") || t.contains("read my notes")
            || t.contains("what did i note") || t.contains("what have i saved");
    }

    // ── Detect delete command ─────────────────────────────────────────────────
    public static boolean isDeleteCommand(String text) {
        String t = text.toLowerCase(Locale.US);
        return (t.contains("delete") || t.contains("clear") || t.contains("erase"))
            && (t.contains("note") || t.contains("notes"));
    }

    // ── Save ──────────────────────────────────────────────────────────────────
    public static String save(Context ctx, String content) {
        List<Note> notes = load(ctx);
        String ts = new SimpleDateFormat("MMM d, h:mm a", Locale.US).format(new Date());
        notes.add(new Note(content, ts));
        persist(ctx, notes);
        return "[EMOTION:proud] Noted, sir. Saved at " + ts + ".";
    }

    // ── Read all ──────────────────────────────────────────────────────────────
    public static String readAll(Context ctx) {
        List<Note> notes = load(ctx);
        if (notes.isEmpty()) return "[EMOTION:warm] No notes saved yet, sir.";
        StringBuilder sb = new StringBuilder("**Your notes, sir:**\n\n");
        for (int i = 0; i < notes.size(); i++) {
            Note n = notes.get(i);
            sb.append("**").append(i + 1).append(".** ").append(n.text)
              .append("\n   _").append(n.timestamp).append("_\n\n");
        }
        return "[EMOTION:neutral] " + sb.toString().trim();
    }

    // ── Delete all ────────────────────────────────────────────────────────────
    public static String deleteAll(Context ctx) {
        prefs(ctx).edit().remove(KEY_NOTES).apply();
        return "[EMOTION:neutral] All notes cleared, sir.";
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private static List<Note> load(Context ctx) {
        String json = prefs(ctx).getString(KEY_NOTES, null);
        if (json == null) return new ArrayList<>();
        try {
            Type type = new TypeToken<List<Note>>(){}.getType();
            List<Note> list = GSON.fromJson(json, type);
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) { return new ArrayList<>(); }
    }

    private static void persist(Context ctx, List<Note> notes) {
        prefs(ctx).edit().putString(KEY_NOTES, GSON.toJson(notes)).apply();
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
