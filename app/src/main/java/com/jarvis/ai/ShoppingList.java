package com.jarvis.ai;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShoppingList {

    private static final String PREFS = "henry_shopping";
    private static final String KEY   = "items";
    private static final Gson   GSON  = new Gson();

    // ── Intent detection ──────────────────────────────────────────────────────
    public static boolean isAddCommand(String text) {
        String t = text.toLowerCase(Locale.US);
        return (t.contains("add") || t.contains("put") || t.contains("buy"))
            && (t.contains("shopping") || t.contains("grocery") || t.contains("list") || t.contains("groceries"));
    }

    public static boolean isShowCommand(String text) {
        String t = text.toLowerCase(Locale.US);
        return (t.contains("show") || t.contains("read") || t.contains("list") || t.contains("what"))
            && (t.contains("shopping") || t.contains("grocery") || t.contains("groceries"));
    }

    public static boolean isClearCommand(String text) {
        String t = text.toLowerCase(Locale.US);
        return (t.contains("clear") || t.contains("delete") || t.contains("remove") || t.contains("done"))
            && (t.contains("shopping") || t.contains("grocery") || t.contains("list"));
    }

    public static boolean isRemoveItem(String text) {
        String t = text.toLowerCase(Locale.US);
        return (t.contains("remove") || t.contains("delete") || t.contains("cross off"))
            && !t.contains("list") && !t.contains("all");
    }

    // ── Parse item from command ───────────────────────────────────────────────
    public static String parseItem(String text) {
        String t = text.trim();
        // "add milk to shopping list" → "milk"
        Matcher m = Pattern.compile(
            "(?:add|put|buy|get)\\s+(.+?)\\s+(?:to|on|in)\\s+(?:my\\s+)?(?:shopping|grocery|the)\\s*(?:list)?",
            Pattern.CASE_INSENSITIVE).matcher(t);
        if (m.find()) return m.group(1).trim();
        // "add milk" after detecting shopping context
        m = Pattern.compile("(?:add|buy|get|put)\\s+(.+)", Pattern.CASE_INSENSITIVE).matcher(t);
        if (m.find()) return m.group(1).replaceAll("(?i)\\s+to.*", "").trim();
        return null;
    }

    // ── Operations ───────────────────────────────────────────────────────────
    public static String add(Context ctx, String item) {
        if (item == null || item.isEmpty()) return "[EMOTION:neutral] What should I add, sir?";
        List<String> items = load(ctx);
        // Avoid duplicates (case-insensitive)
        for (String s : items)
            if (s.equalsIgnoreCase(item))
                return "[EMOTION:amused] " + capitalize(item) + " is already on the list, sir.";
        items.add(capitalize(item));
        save(ctx, items);
        return "[EMOTION:proud] Added **" + capitalize(item) + "** to your shopping list, sir. "
             + items.size() + " item" + (items.size() == 1 ? "" : "s") + " total.";
    }

    public static String readAll(Context ctx) {
        List<String> items = load(ctx);
        if (items.isEmpty()) return "[EMOTION:warm] Your shopping list is empty, sir.";
        StringBuilder sb = new StringBuilder("[EMOTION:neutral] **Shopping list — " + items.size() + " item");
        if (items.size() > 1) sb.append("s");
        sb.append(":**\n\n");
        for (int i = 0; i < items.size(); i++)
            sb.append("**").append(i + 1).append(".** ").append(items.get(i)).append("\n");
        return sb.toString().trim();
    }

    public static String remove(Context ctx, String item) {
        List<String> items = load(ctx);
        boolean removed = items.removeIf(s -> s.equalsIgnoreCase(item));
        if (removed) { save(ctx, items); return "[EMOTION:neutral] Removed **" + item + "**, sir."; }
        return "[EMOTION:neutral] Couldn't find **" + item + "** on the list, sir.";
    }

    public static String clearAll(Context ctx) {
        prefs(ctx).edit().remove(KEY).apply();
        return "[EMOTION:neutral] Shopping list cleared, sir.";
    }

    /** Share list via Android share sheet. */
    public static Intent shareIntent(Context ctx) {
        List<String> items = load(ctx);
        if (items.isEmpty()) return null;
        StringBuilder sb = new StringBuilder("🛒 Shopping List\n\n");
        for (int i = 0; i < items.size(); i++)
            sb.append(i + 1).append(". ").append(items.get(i)).append("\n");
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, sb.toString());
        share.putExtra(Intent.EXTRA_SUBJECT, "Shopping List from H.E.N.R.Y");
        return Intent.createChooser(share, "Share shopping list via…");
    }

    public static List<String> load(Context ctx) {
        String json = prefs(ctx).getString(KEY, null);
        if (json == null) return new ArrayList<>();
        try {
            Type t = new TypeToken<List<String>>(){}.getType();
            List<String> list = GSON.fromJson(json, t);
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) { return new ArrayList<>(); }
    }

    private static void save(Context ctx, List<String> items) {
        prefs(ctx).edit().putString(KEY, GSON.toJson(items)).apply();
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
