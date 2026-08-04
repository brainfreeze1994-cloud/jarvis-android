package com.jarvis.ai;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Smart Clipboard — copy HENRY replies, maintain history of last 10 copies.
 * "Copy that" / "Copy last reply" / "Clipboard history" / "Copy [text]"
 */
public class ClipboardManager2 {

    private static final String PREFS    = "clipboard_prefs";
    private static final String KEY_DATA = "clip_history";
    private static final int    MAX      = 10;

    public static boolean isCopyCommand(String text) {
        String lower = text.toLowerCase(Locale.US);
        return lower.equals("copy that") || lower.equals("copy it") ||
               lower.startsWith("copy last") || lower.startsWith("copy the last") ||
               lower.contains("copy reply") || lower.contains("copy response") ||
               lower.contains("clipboard history") || lower.contains("my clipboard") ||
               lower.contains("what did i copy") || lower.equals("copy");
    }

    /** Copy text to system clipboard and save to history */
    public static String copy(Context ctx, String text, String label) {
        if (text == null || text.trim().isEmpty())
            return "[EMOTION:neutral] Nothing to copy, sir.";

        ClipboardManager cm = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText(label != null ? label : "HENRY", text));
        }
        addToHistory(ctx, text);
        // Truncate for display
        String preview = text.length() > 60 ? text.substring(0, 57) + "…" : text;
        return "[EMOTION:warm] Copied to clipboard, sir: \"" + preview + "\"";
    }

    /** Get clipboard history as formatted string */
    public static String getHistory(Context ctx) {
        List<String> history = load(ctx);
        if (history.isEmpty())
            return "[EMOTION:neutral] Clipboard history is empty, sir.";
        StringBuilder sb = new StringBuilder("[EMOTION:neutral] **Clipboard History, sir:**\n\n");
        for (int i = history.size() - 1; i >= 0; i--) {
            String item = history.get(i);
            String preview = item.length() > 80 ? item.substring(0, 77) + "…" : item;
            sb.append(i + 1 == history.size() ? "**Latest:** " : (history.size() - i) + ". ")
              .append(preview).append("\n\n");
        }
        return sb.toString().trim();
    }

    private static void addToHistory(Context ctx, String text) {
        List<String> list = load(ctx);
        list.add(text);
        if (list.size() > MAX) list = list.subList(list.size() - MAX, list.size());
        save(ctx, list);
    }

    private static List<String> load(Context ctx) {
        List<String> list = new ArrayList<>();
        String json = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_DATA, "[]");
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) list.add(arr.getString(i));
        } catch (Exception ignored) {}
        return list;
    }

    private static void save(Context ctx, List<String> list) {
        try {
            JSONArray arr = new JSONArray();
            for (String s : list) arr.put(s);
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
               .putString(KEY_DATA, arr.toString()).apply();
        } catch (Exception ignored) {}
    }
}
