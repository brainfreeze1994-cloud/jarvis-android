package com.jarvis.ai;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Protocols — custom, named, multi-step automations. Tony Stark's own
 * terminology ("Protocol: House Party Protocol"), and functionally the
 * single biggest lever for making HENRY feel like an assistant that DOES
 * things instead of a bunch of separate tools you have to ask for one at
 * a time.
 *
 * Deliberately reuses the existing command pipeline rather than calling
 * action classes directly — a protocol step is just plain text ("turn on
 * do not disturb", "what's the weather") that gets replayed through
 * askJarvis() exactly as if you'd typed it. This means a protocol can use
 * ANY command HENRY already understands, with zero new wiring needed per
 * feature, and it automatically stays in sync as new commands get added.
 *
 * "Create protocol Movie Night: dim brightness, silent mode, open Netflix"
 * "Run Movie Night" / "Protocol: Movie Night" / just saying "Movie Night"
 * "My protocols" / "List protocols"
 * "Delete protocol Movie Night"
 */
public class ProtocolManager {

    private static final String PREFS    = "protocol_prefs";
    private static final String KEY_DATA = "protocols_json";

    public static class Protocol {
        String name;
        List<String> steps;

        Protocol(String name, List<String> steps) {
            this.name = name;
            this.steps = steps;
        }

        Protocol(JSONObject j) throws Exception {
            name = j.getString("name");
            steps = new ArrayList<>();
            JSONArray arr = j.getJSONArray("steps");
            for (int i = 0; i < arr.length(); i++) steps.add(arr.getString(i));
        }

        JSONObject toJson() throws Exception {
            JSONArray arr = new JSONArray();
            for (String s : steps) arr.put(s);
            return new JSONObject().put("name", name).put("steps", arr);
        }
    }

    // ── Load / Save ───────────────────────────────────────────────────────────
    private static List<Protocol> load(Context ctx) {
        List<Protocol> list = new ArrayList<>();
        String json = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_DATA, "[]");
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) list.add(new Protocol(arr.getJSONObject(i)));
        } catch (Exception ignored) {}
        return list;
    }

    private static void save(Context ctx, List<Protocol> protocols) {
        try {
            JSONArray arr = new JSONArray();
            for (Protocol p : protocols) arr.put(p.toJson());
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
               .putString(KEY_DATA, arr.toString()).apply();
        } catch (Exception ignored) {}
    }

    // ── Detection ─────────────────────────────────────────────────────────────
    public static boolean isCreateCommand(String text) {
        return java.util.regex.Pattern.compile("^(create|make|new|set up)\\s+protocol\\b", java.util.regex.Pattern.CASE_INSENSITIVE)
            .matcher(text.trim()).find();
    }

    public static boolean isListCommand(String text) {
        String t = text.toLowerCase(Locale.US);
        return t.contains("my protocols") || t.contains("list protocols") || t.equals("protocols");
    }

    public static boolean isDeleteCommand(String text) {
        return java.util.regex.Pattern.compile("^delete\\s+protocol\\b", java.util.regex.Pattern.CASE_INSENSITIVE)
            .matcher(text.trim()).find();
    }

    /** Returns the matched protocol name if this text is meant to run one, else null. */
    public static String matchRunCommand(Context ctx, String text) {
        String t = text.trim();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(
            "^(?:run|start|activate|protocol:?)\\s+(.+)$", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(t);
        String candidate = m.find() ? m.group(1).trim() : t.trim();

        for (Protocol p : load(ctx)) {
            if (p.name.equalsIgnoreCase(candidate) || p.name.equalsIgnoreCase(t)) return p.name;
        }
        return null;
    }

    // ── Create ────────────────────────────────────────────────────────────────
    /**
     * Parses "create protocol Movie Night: dim brightness, silent mode, open Netflix"
     * into a name + ordered step list. Returns a result message.
     */
    public static String create(Context ctx, String userText) {
        // Strip the leading "create/make/new/set up protocol" prefix
        String rest = userText.trim().replaceFirst(
            "(?i)^(create|make|new|set up)\\s+protocol\\s*", "").trim();

        int colon = rest.indexOf(':');
        if (colon < 0) {
            return "[EMOTION:neutral] I need a name and steps, sir — try \"create protocol Movie Night: dim brightness, silent mode, open Netflix\".";
        }
        String name = rest.substring(0, colon).trim();
        String stepsRaw = rest.substring(colon + 1).trim();
        if (name.isEmpty() || stepsRaw.isEmpty()) {
            return "[EMOTION:neutral] I need both a name and at least one step, sir.";
        }

        List<String> steps = new ArrayList<>();
        for (String s : stepsRaw.split(",")) {
            String trimmed = s.trim();
            if (!trimmed.isEmpty()) steps.add(trimmed);
        }
        if (steps.isEmpty()) {
            return "[EMOTION:neutral] Didn't catch any steps there, sir — separate them with commas.";
        }

        List<Protocol> protocols = load(ctx);
        // Overwrite if a protocol with this name already exists
        protocols.removeIf(p -> p.name.equalsIgnoreCase(name));
        protocols.add(new Protocol(name, steps));
        save(ctx, protocols);

        StringBuilder sb = new StringBuilder("[EMOTION:proud] **Protocol \"" + name + "\" saved, sir.**\n\n" + steps.size() + " step" + (steps.size() != 1 ? "s" : "") + ":\n");
        for (int i = 0; i < steps.size(); i++) sb.append(i + 1).append(". ").append(steps.get(i)).append("\n");
        sb.append("\nSay \"run ").append(name).append("\" whenever you want it.");
        return sb.toString();
    }

    // ── List / Delete ────────────────────────────────────────────────────────
    public static String list(Context ctx) {
        List<Protocol> protocols = load(ctx);
        if (protocols.isEmpty()) {
            return "[EMOTION:neutral] No protocols saved yet, sir. Try \"create protocol Morning: check weather, my tasks, daily briefing\".";
        }
        StringBuilder sb = new StringBuilder("[EMOTION:neutral] **Your Protocols, sir:**\n\n");
        for (Protocol p : protocols) {
            sb.append("🔹 **").append(p.name).append("** — ").append(p.steps.size()).append(" step").append(p.steps.size() != 1 ? "s" : "").append("\n");
        }
        return sb.toString();
    }

    public static String delete(Context ctx, String userText) {
        String name = userText.trim().replaceFirst("(?i)^delete\\s+protocol\\s*", "").trim();
        List<Protocol> protocols = load(ctx);
        boolean removed = protocols.removeIf(p -> p.name.equalsIgnoreCase(name));
        if (removed) {
            save(ctx, protocols);
            return "[EMOTION:neutral] Protocol \"" + name + "\" deleted, sir.";
        }
        return "[EMOTION:neutral] Couldn't find a protocol called \"" + name + "\", sir.";
    }

    public static List<String> getSteps(Context ctx, String name) {
        for (Protocol p : load(ctx)) {
            if (p.name.equalsIgnoreCase(name)) return p.steps;
        }
        return new ArrayList<>();
    }
}
