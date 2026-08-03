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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Habit Tracker — track daily habits with streaks.
 * Commands: "add habit gym", "mark gym done", "my streaks", "habit report"
 */
public class HabitTracker {

    private static final String PREFS    = "habit_prefs";
    private static final String KEY_DATA = "habits_json";
    private static final String DATE_FMT = "yyyyMMdd";

    // ── Data model ────────────────────────────────────────────────────────────
    static class Habit {
        String name;
        int streak;
        int best;
        String lastDone; // yyyyMMdd
        int totalDays;

        Habit(String name) {
            this.name = name; this.streak = 0; this.best = 0;
            this.lastDone = ""; this.totalDays = 0;
        }

        Habit(JSONObject j) throws Exception {
            name     = j.getString("name");
            streak   = j.optInt("streak", 0);
            best     = j.optInt("best", 0);
            lastDone = j.optString("lastDone", "");
            totalDays= j.optInt("totalDays", 0);
        }

        JSONObject toJson() throws Exception {
            return new JSONObject()
                .put("name", name).put("streak", streak)
                .put("best", best).put("lastDone", lastDone)
                .put("totalDays", totalDays);
        }
    }

    // ── Load / Save ───────────────────────────────────────────────────────────
    private static List<Habit> load(Context ctx) {
        List<Habit> list = new ArrayList<>();
        String json = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_DATA, "[]");
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) list.add(new Habit(arr.getJSONObject(i)));
        } catch (Exception ignored) {}
        return list;
    }

    private static void save(Context ctx, List<Habit> habits) {
        try {
            JSONArray arr = new JSONArray();
            for (Habit h : habits) arr.put(h.toJson());
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
               .putString(KEY_DATA, arr.toString()).apply();
        } catch (Exception ignored) {}
    }

    private static String today() {
        return new SimpleDateFormat(DATE_FMT, Locale.US).format(new Date());
    }

    private static String yesterday() {
        long ts = System.currentTimeMillis() - 86400000L;
        return new SimpleDateFormat(DATE_FMT, Locale.US).format(new Date(ts));
    }

    // ── Commands ──────────────────────────────────────────────────────────────
    public static boolean isHabitCommand(String text) {
        String lower = text.toLowerCase(Locale.US);
        return lower.contains("habit") || lower.contains("streak") ||
               lower.startsWith("mark ") || lower.contains("mark it done") ||
               lower.contains("i did ") || lower.contains("completed ") ||
               lower.contains("check in") || lower.contains("my habits");
    }

    public static String handle(Context ctx, String text) {
        String lower = text.toLowerCase(Locale.US);
        List<Habit> habits = load(ctx);

        // Add habit
        if (lower.startsWith("add habit") || lower.contains("new habit") || lower.contains("track habit")) {
            String name = text.replaceAll("(?i)(add|new|track)\\s+habit\\s*:?\\s*", "").trim();
            if (name.isEmpty()) return "[EMOTION:neutral] What habit would you like to track, sir?";
            for (Habit h : habits)
                if (h.name.equalsIgnoreCase(name))
                    return "[EMOTION:neutral] Already tracking **" + h.name + "**, sir.";
            habits.add(new Habit(name));
            save(ctx, habits);
            return "[EMOTION:excited] Added **" + name + "** to your habits, sir. Let's build that streak!";
        }

        // Delete habit
        if (lower.startsWith("delete habit") || lower.startsWith("remove habit")) {
            String name = text.replaceAll("(?i)(delete|remove)\\s+habit\\s*:?\\s*", "").trim();
            boolean removed = habits.removeIf(h -> h.name.equalsIgnoreCase(name));
            save(ctx, habits);
            return removed ? "[EMOTION:neutral] Removed **" + name + "** from habits, sir."
                           : "[EMOTION:neutral] Couldn't find habit **" + name + "**, sir.";
        }

        // Mark done — "mark gym done" / "i did gym" / "completed meditation"
        Matcher markM = Pattern.compile(
            "(?:mark\\s+(.+?)\\s+done|i\\s+did\\s+(.+)|completed?\\s+(.+)|(.+?)\\s+done)",
            Pattern.CASE_INSENSITIVE).matcher(text);
        if (markM.find()) {
            String habitName = (markM.group(1) != null ? markM.group(1) :
                                markM.group(2) != null ? markM.group(2) :
                                markM.group(3) != null ? markM.group(3) :
                                markM.group(4)).trim()
                               .replaceAll("(?i)\\s*(is|was|has been)\\s*done", "").trim();
            Habit found = null;
            for (Habit h : habits)
                if (h.name.toLowerCase().contains(habitName.toLowerCase()) ||
                    habitName.toLowerCase().contains(h.name.toLowerCase())) { found = h; break; }

            if (found == null)
                return "[EMOTION:neutral] I don't see **" + habitName + "** in your habits, sir. Say 'add habit " + habitName + "' to start tracking.";

            String today = today();
            if (today.equals(found.lastDone))
                return "[EMOTION:warm] Already marked **" + found.name + "** done today, sir. Streak: " + found.streak + " days!";

            // Update streak
            if (yesterday().equals(found.lastDone)) found.streak++;
            else found.streak = 1;
            found.lastDone  = today;
            found.totalDays++;
            if (found.streak > found.best) found.best = found.streak;
            save(ctx, habits);

            String fire = found.streak >= 30 ? " 🔥 30 day milestone!" :
                          found.streak >= 7  ? " 🔥 One week streak!" :
                          found.streak >= 3  ? " Keep it up!" : "";
            return String.format(Locale.US,
                "[EMOTION:%s] **%s** marked done, sir! Current streak: **%d days** | Best: **%d days**%s",
                found.streak >= 7 ? "proud" : "excited",
                found.name, found.streak, found.best, fire);
        }

        // Show all habits / streaks
        if (lower.contains("my habit") || lower.contains("streak") || lower.contains("habit report")) {
            if (habits.isEmpty())
                return "[EMOTION:neutral] No habits tracked yet, sir. Say 'add habit [name]' to start.";
            StringBuilder sb = new StringBuilder("[EMOTION:warm] **Your Habit Streaks, sir:**\n\n");
            String today = today();
            for (Habit h : habits) {
                boolean doneToday = today.equals(h.lastDone);
                String check = doneToday ? "✅" : "⬜";
                sb.append(String.format(Locale.US,
                    "%s **%s** — %d day streak (best: %d) | Total: %d days\n",
                    check, h.name, h.streak, h.best, h.totalDays));
            }
            return sb.toString().trim();
        }

        return null;
    }
}
