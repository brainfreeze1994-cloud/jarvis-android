package com.jarvis.ai;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Locale;

/**
 * RelationshipBrain — HENRY remembers every person you mention.
 * "My boss Sarah said..." → Boss = Sarah
 * "My wife loves Italian" → Stored for future reference
 * "Draft an email to Sarah" → HENRY knows Sarah is your boss, adjusts tone
 */
public class RelationshipBrain {

    private static final String PREFS    = "henry_relationships";
    private static final String KEY_REL  = "relationships";
    private static final int    MAX_REL  = 40;

    public static class Person {
        public String name;
        public String relationship; // boss, wife, friend, etc.
        public String notes;        // extra details learned over time
        public String lastMentioned;
    }

    // ── Auto-extract from user message ────────────────────────────────────
    public static void learnFromMessage(Context ctx, String msg) {
        if (msg == null || msg.length() < 5) return;
        String lower = msg.toLowerCase(Locale.US);

        // "My boss/wife/friend [Name]..."
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(
            "(?:my|the)\\s+(boss|wife|husband|girlfriend|boyfriend|partner|friend|brother|sister|mother|father|mum|dad|son|daughter|colleague|manager|team|assistant|doctor|lawyer|neighbour|landlord)\\s+(?:is\\s+)?([A-Z][a-z]{1,20})",
            java.util.regex.Pattern.CASE_INSENSITIVE
        ).matcher(msg);

        while (m.find()) {
            String rel  = m.group(1).toLowerCase(Locale.US);
            String name = m.group(2);
            savePerson(ctx, name, rel, "");
        }

        // "email/call/text/message [Name]" — likely a known contact
        java.util.regex.Matcher m2 = java.util.regex.Pattern.compile(
            "(?:email|call|text|message|WhatsApp|contact)\\s+([A-Z][a-z]{1,20})",
            java.util.regex.Pattern.CASE_INSENSITIVE
        ).matcher(msg);
        while (m2.find()) {
            String name = m2.group(1);
            // Only save if not a common verb
            if (!name.toLowerCase(Locale.US).matches("me|him|her|them|you|us|back|again|now|soon")) {
                savePerson(ctx, name, "contact", "");
            }
        }
    }

    // ── Save a person ─────────────────────────────────────────────────────
    public static void savePerson(Context ctx, String name, String relationship, String notes) {
        try {
            JSONArray arr = load(ctx);
            // Update if exists
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                if (o.getString("name").equalsIgnoreCase(name)) {
                    if (relationship != null && !relationship.isEmpty() && !relationship.equals("contact"))
                        o.put("relationship", relationship);
                    if (notes != null && !notes.isEmpty())
                        o.put("notes", o.optString("notes","") + "; " + notes);
                    o.put("lastMentioned", today());
                    save(ctx, arr);
                    return;
                }
            }
            // New person
            JSONObject o = new JSONObject();
            o.put("name", name);
            o.put("relationship", relationship);
            o.put("notes", notes != null ? notes : "");
            o.put("lastMentioned", today());
            arr.put(o);
            if (arr.length() > MAX_REL) arr.remove(0);
            save(ctx, arr);
        } catch (Exception ignored) {}
    }

    // ── Build context string for API ──────────────────────────────────────
    public static String buildContext(Context ctx) {
        try {
            JSONArray arr = load(ctx);
            if (arr.length() == 0) return "";
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                String name = o.optString("name","");
                String rel  = o.optString("relationship","contact");
                String notes= o.optString("notes","");
                sb.append(name).append(" = user's ").append(rel);
                if (!notes.isEmpty()) sb.append(" (").append(notes).append(")");
                sb.append("; ");
            }
            return sb.toString().trim();
        } catch (Exception e) { return ""; }
    }

    // ── Get context around a specific name ────────────────────────────────
    public static String lookupPerson(Context ctx, String query) {
        try {
            JSONArray arr = load(ctx);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                if (o.optString("name","").equalsIgnoreCase(query)) {
                    return o.optString("name") + " is user's " + o.optString("relationship","contact")
                        + (o.optString("notes","").isEmpty() ? "" : ". " + o.optString("notes"));
                }
            }
        } catch (Exception e) {}
        return null;
    }

    // ── List all people ───────────────────────────────────────────────────
    public static String getSummary(Context ctx) {
        try {
            JSONArray arr = load(ctx);
            if (arr.length() == 0)
                return "[EMOTION:neutral] I haven't learned about the people in your life yet, sir. Mention someone and I'll remember them.";
            StringBuilder sb = new StringBuilder("[EMOTION:warm]\n**People HENRY knows about:**\n\n");
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                sb.append("• **").append(o.optString("name")).append("** — ").append(o.optString("relationship","contact"));
                if (!o.optString("notes","").isEmpty()) sb.append(" — ").append(o.optString("notes"));
                sb.append("\n");
            }
            return sb.toString();
        } catch (Exception e) { return "[EMOTION:neutral] Memory error, sir."; }
    }

    public static void forgetPerson(Context ctx, String name) {
        try {
            JSONArray arr = load(ctx), updated = new JSONArray();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                if (!o.optString("name","").equalsIgnoreCase(name)) updated.put(o);
            }
            save(ctx, updated);
        } catch (Exception ignored) {}
    }

    public static void clearAll(Context ctx) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_REL).apply();
    }

    private static JSONArray load(Context ctx) {
        try { return new JSONArray(ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_REL,"[]")); }
        catch (Exception e) { return new JSONArray(); }
    }
    private static void save(Context ctx, JSONArray arr) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_REL, arr.toString()).apply();
    }
    private static String today() {
        return new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new java.util.Date());
    }
}
