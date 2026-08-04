package com.jarvis.ai;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Proactive Suggestions — HENRY suggests things before you ask.
 * Time-aware, context-aware nudges based on usage patterns + time of day.
 */
public class ProactiveSuggestions {

    private static final String PREFS         = "proactive_prefs";
    private static final String KEY_LAST_DATE = "last_suggestion_date";
    private static final String KEY_USAGE     = "usage_counts";

    /** Returns a proactive suggestion based on time + context, or null */
    public static String getSuggestion(Context ctx, UserProfile profile, List<HistoryItem> history) {
        try {
            Calendar cal = Calendar.getInstance();
            int hour   = cal.get(Calendar.HOUR_OF_DAY);
            int minute = cal.get(Calendar.MINUTE);
            int dow    = cal.get(Calendar.DAY_OF_WEEK); // 1=Sun
            String today = new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date());

            // Only one suggestion per 2 hours
            SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            String lastSuggTime = p.getString("last_sugg_time", "");
            String nowHour = today + "_" + hour;
            if (nowHour.equals(lastSuggTime)) return null;

            String name = (profile != null && !profile.nickname.isEmpty()) ? profile.nickname : "sir";
            String suggestion = null;

            // Morning nudge (7-9am)
            if (hour >= 7 && hour < 9) {
                String lastMorning = p.getString("last_morning", "");
                if (!today.equals(lastMorning)) {
                    p.edit().putString("last_morning", today).apply();
                    String[] morning = {
                        "Good morning, " + name + "! Shall I read your daily briefing?",
                        "Morning, " + name + "! Your day is ready — say 'daily digest' for a full briefing.",
                        "Rise and shine, " + name + "! Want today's news, weather, and reminders?"
                    };
                    suggestion = morning[hour % morning.length];
                }
            }
            // Lunch nudge (12-1pm)
            else if (hour == 12 || hour == 13) {
                if (!p.getString("last_lunch", "").equals(today)) {
                    p.edit().putString("last_lunch", today).apply();
                    suggestion = "Lunchtime, " + name + "! Want a meal suggestion or recipe idea?";
                }
            }
            // Evening wind-down (8-10pm)
            else if (hour >= 20 && hour < 22) {
                if (!p.getString("last_evening", "").equals(today)) {
                    p.edit().putString("last_evening", today).apply();
                    String[] evening = {
                        "Evening, " + name + "! Want me to set a reminder or summarise your day?",
                        "Winding down, " + name + "? I can set your alarm, or we could review today's tasks.",
                    };
                    suggestion = evening[hour % evening.length];
                }
            }
            // Weekend activity (Sat/Sun, 10am-2pm)
            else if ((dow == Calendar.SATURDAY || dow == Calendar.SUNDAY) && hour >= 10 && hour < 14) {
                if (!p.getString("last_weekend", "").equals(today)) {
                    p.edit().putString("last_weekend", today).apply();
                    suggestion = "It's the weekend, " + name + "! Want nearby places to visit, a workout plan, or recipe ideas?";
                }
            }
            // Motivational nudge (3pm slump)
            else if (hour == 15 && minute < 30) {
                if (!p.getString("last_3pm", "").equals(today)) {
                    p.edit().putString("last_3pm", today).apply();
                    suggestion = "3pm energy dip, " + name + "? I can suggest a breathing exercise or quick workout to recharge.";
                }
            }

            // Check birthday reminders
            String bdayAlert = BirthdayTracker.checkTodayBirthdays(ctx);
            if (bdayAlert != null && !p.getString("last_bday_check", "").equals(today)) {
                p.edit().putString("last_bday_check", today).apply();
                return bdayAlert; // Birthday takes priority
            }

            if (suggestion != null) {
                p.edit().putString("last_sugg_time", nowHour).apply();
                return "[EMOTION:warm] " + suggestion;
            }
            return null;
        } catch (Exception e) { return null; }
    }

    /** Track what commands are used most — for future smarter suggestions */
    public static void trackUsage(Context ctx, String command) {
        try {
            SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            JSONObject counts;
            try { counts = new JSONObject(p.getString(KEY_USAGE, "{}")); }
            catch (Exception e) { counts = new JSONObject(); }
            // Bucket the command into a category
            String cat = categorize(command);
            counts.put(cat, counts.optInt(cat, 0) + 1);
            p.edit().putString(KEY_USAGE, counts.toString()).apply();
        } catch (Exception ignored) {}
    }

    private static String categorize(String text) {
        String lower = text.toLowerCase(Locale.US);
        if (lower.contains("weather")) return "weather";
        if (lower.contains("news")) return "news";
        if (lower.contains("reminder")) return "reminders";
        if (lower.contains("workout") || lower.contains("exercise")) return "fitness";
        if (lower.contains("music")) return "music";
        if (lower.contains("recipe") || lower.contains("meal")) return "food";
        if (lower.contains("navigate") || lower.contains("map")) return "navigation";
        return "general";
    }
}
