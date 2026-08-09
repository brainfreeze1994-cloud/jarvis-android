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
 * Daily Briefing — pulls real data from SleepTracker, TaskManager, HabitTracker
 * and WeatherForecast, then hands it to HENRY to synthesize into one short,
 * natural spoken briefing instead of a bullet-point dump. This is the "chief
 * of staff" behavior — it connects dots across your own data instead of
 * just answering isolated questions.
 *
 * Trigger: "morning briefing" / "daily briefing" / "brief me" / "what's my day look like"
 */
public class DailyBriefing {

    public interface Callback { void onReady(String briefing); }

    public static boolean isBriefingCommand(String text) {
        String t = text.toLowerCase(Locale.US);
        return t.contains("morning briefing") || t.contains("daily briefing") ||
               t.contains("brief me") || t.contains("my briefing") ||
               t.contains("what's my day look like") || t.contains("how's my day looking") ||
               t.contains("give me a rundown");
    }

    public static void generate(Context ctx, Callback cb) {
        StringBuilder facts = new StringBuilder();
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());

        appendSleepFacts(ctx, facts, today);
        appendTaskFacts(ctx, facts, today);
        appendHabitFacts(ctx, facts);

        // Weather is the only async piece — fetch it, then synthesize with everything.
        WeatherForecast.fetch(ctx, false, new WeatherForecast.Callback() {
            @Override public void onResult(String formatted) {
                facts.append("Weather: ").append(stripMarkdown(formatted)).append("\n");
                synthesize(ctx, facts.toString(), cb);
            }
            @Override public void onError(String reason) {
                synthesize(ctx, facts.toString(), cb);
            }
        });
    }

    // ── Data gathering ───────────────────────────────────────────────────────
    private static void appendSleepFacts(Context ctx, StringBuilder facts, String today) {
        SharedPreferences p = ctx.getSharedPreferences("sleep_tracker_prefs", Context.MODE_PRIVATE);
        int score = p.getInt("sleep_score", -1);
        String lastReportDate = p.getString("sleep_last_report", "");
        if (score >= 0 && today.equals(lastReportDate)) {
            facts.append("Sleep score last night: ").append(score).append("/100\n");
        }
    }

    private static void appendTaskFacts(Context ctx, StringBuilder facts, String today) {
        String json = ctx.getSharedPreferences("task_prefs", Context.MODE_PRIVATE).getString("tasks_json", "[]");
        try {
            JSONArray arr = new JSONArray(json);
            int total = 0, overdue = 0, dueToday = 0;
            List<String> overdueTitles = new ArrayList<>();
            List<String> todayTitles = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject t = arr.getJSONObject(i);
                if (t.optBoolean("done", false)) continue;
                total++;
                String due = t.optString("dueDate", "");
                if (due.isEmpty()) continue;
                if (due.compareTo(today) < 0) { overdue++; overdueTitles.add(t.optString("title")); }
                else if (due.equals(today)) { dueToday++; todayTitles.add(t.optString("title")); }
            }
            if (total > 0) {
                facts.append(total).append(" open task").append(total != 1 ? "s" : "");
                if (overdue > 0) facts.append(" — ").append(overdue).append(" OVERDUE: ").append(String.join(", ", overdueTitles));
                if (dueToday > 0) facts.append(" — ").append(dueToday).append(" due today: ").append(String.join(", ", todayTitles));
                facts.append("\n");
            }
        } catch (Exception ignored) {}
    }

    private static void appendHabitFacts(Context ctx, StringBuilder facts) {
        String json = ctx.getSharedPreferences("habit_prefs", Context.MODE_PRIVATE).getString("habits_json", "[]");
        try {
            JSONArray arr = new JSONArray(json);
            String todayCompact = new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date());
            List<String> atRisk = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject h = arr.getJSONObject(i);
                int streak = h.optInt("streak", 0);
                String lastDone = h.optString("lastDone", "");
                if (streak > 0 && !todayCompact.equals(lastDone)) {
                    atRisk.add(h.optString("name") + " (" + streak + "-day streak)");
                }
            }
            if (!atRisk.isEmpty()) {
                facts.append("Habit streaks not yet done today (at risk): ").append(String.join(", ", atRisk)).append("\n");
            }
        } catch (Exception ignored) {}
    }

    private static String stripMarkdown(String s) {
        return s == null ? "" : s.replaceAll("\\*\\*|__|#", "").trim();
    }

    // ── Synthesis ────────────────────────────────────────────────────────────
    private static void synthesize(Context ctx, String facts, Callback cb) {
        if (facts.trim().isEmpty()) {
            cb.onReady("[EMOTION:neutral] Not much to report this morning, sir — no tasks, no habit streaks at risk, and no sleep data logged. Clean slate.");
            return;
        }
        List<HistoryItem> tempHistory = new ArrayList<>();
        tempHistory.add(new HistoryItem("user",
            "Give me my daily briefing. Here's today's raw data:\n\n" + facts +
            "\nSynthesize this into a short, natural SPOKEN briefing — 3 to 5 sentences, like a chief of staff talking, " +
            "not a bulleted list read aloud. Connect things that relate (e.g. low sleep score + overdue tasks is worth " +
            "noting together). Skip any category with no data above — don't say \"no data for X\", just omit it. " +
            "Start your reply with [EMOTION:tag]."));
        JarvisApi.ask(tempHistory, null, "concise", new JarvisApi.Callback() {
            @Override public void onSuccess(String reply, String imageUrl, List<String> followUps) {
                cb.onReady(reply);
            }
            @Override public void onError(String error) {
                cb.onReady("[EMOTION:neutral] Good morning, sir. I gathered your data but couldn't put together the full briefing just now — worth asking again in a moment.");
            }
        });
    }
}
