package com.jarvis.ai;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReminderManager {

    private static final String PREFS = "henry_reminders";
    private static final String KEY   = "reminder_list";
    private static final Gson   GSON  = new Gson();

    public static class Reminder {
        public int id; public String label; public long triggerMs;
    }

    public static String trySchedule(Context ctx, String text) {
        String lower = text.toLowerCase(Locale.US);
        if (!lower.contains("remind") && !lower.contains("alarm")
            && !lower.contains("alert") && !lower.contains("wake me")) return null;
        long triggerMs = parseTime(lower);
        if (triggerMs == 0) return null;
        String label = extractLabel(text);
        int id = (int)(triggerMs % 90000) + 1;
        schedule(ctx, id, label, triggerMs);
        persist(ctx, id, label, triggerMs);
        String when = new SimpleDateFormat("EEE, MMM d 'at' h:mm a", Locale.US)
            .format(new Date(triggerMs));
        return "[EMOTION:proud]\nReminder set, sir. I'll remind you to **\"" + label
            + "\"** on **" + when + "**. Consider it handled.";
    }

    public static String listReminders(Context ctx) {
        List<Reminder> list = load(ctx);
        long now = System.currentTimeMillis();
        List<Reminder> upcoming = new ArrayList<>();
        for (Reminder r : list) if (r.triggerMs > now) upcoming.add(r);
        if (upcoming.isEmpty()) return null;
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM d 'at' h:mm a", Locale.US);
        StringBuilder sb = new StringBuilder("[EMOTION:warm]\nYour upcoming reminders, sir:\n\n");
        for (int i = 0; i < upcoming.size(); i++) {
            sb.append("**").append(i + 1).append(".** ")
              .append(upcoming.get(i).label).append(" — ")
              .append(sdf.format(new Date(upcoming.get(i).triggerMs))).append("\n");
        }
        return sb.toString().trim();
    }

    private static long parseTime(String lower) {
        Matcher rel = Pattern.compile("in\\s+(\\d+)\\s*(minute|min|hour|hr|second|sec)",
            Pattern.CASE_INSENSITIVE).matcher(lower);
        if (rel.find()) {
            int n = Integer.parseInt(rel.group(1));
            String u = rel.group(2).toLowerCase();
            long ms = u.startsWith("hour") || u.equals("hr") ? n * 3600_000L
                    : u.startsWith("sec") ? n * 1000L : n * 60_000L;
            return System.currentTimeMillis() + ms;
        }
        Matcher abs = Pattern.compile(
            "at\\s+(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?",
            Pattern.CASE_INSENSITIVE).matcher(lower);
        if (abs.find()) {
            int h = Integer.parseInt(abs.group(1));
            int m = abs.group(2) != null ? Integer.parseInt(abs.group(2)) : 0;
            String ap = abs.group(3);
            if (ap != null) {
                if (ap.equalsIgnoreCase("pm") && h < 12) h += 12;
                if (ap.equalsIgnoreCase("am") && h == 12) h = 0;
            }
            Calendar cal = Calendar.getInstance();
            if (lower.contains("tomorrow")) cal.add(Calendar.DAY_OF_YEAR, 1);
            cal.set(Calendar.HOUR_OF_DAY, h);
            cal.set(Calendar.MINUTE, m);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            if (!lower.contains("tomorrow") && cal.getTimeInMillis() <= System.currentTimeMillis())
                cal.add(Calendar.DAY_OF_YEAR, 1);
            return cal.getTimeInMillis();
        }
        return 0;
    }

    private static String extractLabel(String text) {
        Matcher m = Pattern.compile(
            "(?:remind me|alarm|alert|wake me).*?(?:to|for)\\s+(.+)",
            Pattern.CASE_INSENSITIVE).matcher(text);
        if (m.find()) {
            String l = m.group(1).trim()
                .replaceAll("(?i)\\s*at\\s+\\d.*", "")
                .replaceAll("(?i)\\s*in\\s+\\d.*", "").trim();
            if (!l.isEmpty()) return Character.toUpperCase(l.charAt(0)) + l.substring(1);
        }
        return "Reminder";
    }

    private static void schedule(Context ctx, int id, String label, long triggerMs) {
        Intent intent = new Intent(ctx, ReminderReceiver.class);
        intent.putExtra(ReminderReceiver.EXTRA_LABEL, label);
        intent.putExtra("notifId", id + 2000);
        PendingIntent pi = PendingIntent.getBroadcast(ctx, id, intent,
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms())
            am.set(AlarmManager.RTC_WAKEUP, triggerMs, pi);
        else
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pi);
    }

    private static void persist(Context ctx, int id, String label, long triggerMs) {
        List<Reminder> list = load(ctx);
        list.removeIf(r -> r.id == id);
        Reminder r = new Reminder(); r.id = id; r.label = label; r.triggerMs = triggerMs;
        list.add(r);
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY, GSON.toJson(list)).apply();
    }

    private static List<Reminder> load(Context ctx) {
        String json = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, "[]");
        try {
            Type t = new TypeToken<List<Reminder>>(){}.getType();
            List<Reminder> l = GSON.fromJson(json, t);
            return l != null ? l : new ArrayList<>();
        } catch (Exception e) { return new ArrayList<>(); }
    }
}
