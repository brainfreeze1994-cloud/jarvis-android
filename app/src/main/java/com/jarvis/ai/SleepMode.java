package com.jarvis.ai;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.provider.Settings;

import androidx.core.app.NotificationCompat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sleep Mode — goodnight ritual:
 *   1. Enables DND / silent mode
 *   2. Dims screen brightness
 *   3. Returns a farewell message
 *   4. Optionally schedules a good morning reminder
 */
public class SleepMode {

    private static final String CHANNEL_ID = "henry_morning";
    private static final int    NOTIF_ID   = 8001;
    private static final String PREFS      = "sleep_prefs";
    private static final String KEY_WAKE   = "wake_hour";
    private static final String KEY_WAKE_M = "wake_min";

    public static boolean isSleepCommand(String text) {
        String lower = text.toLowerCase(Locale.US);
        return lower.contains("goodnight") || lower.contains("good night") ||
               lower.contains("sleep mode") || lower.contains("i'm going to sleep") ||
               lower.contains("i'm going to bed") || lower.contains("going to bed") ||
               lower.contains("turning in") || lower.contains("night henry") ||
               lower.contains("bedtime") || lower.contains("night mode");
    }

    public static boolean isWakeCommand(String text) {
        String lower = text.toLowerCase(Locale.US);
        return lower.contains("good morning") || lower.contains("morning henry") ||
               lower.contains("wake up") || lower.contains("i'm awake") ||
               lower.contains("disable sleep") || lower.contains("turn off sleep");
    }

    /**
     * Activates sleep mode. Returns a farewell message.
     * @param wakeHour  -1 if no alarm requested
     */
    public static String activate(Context ctx, int wakeHour, int wakeMin) {
        // 1. Silent mode / DND
        AudioManager am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
        if (am != null) am.setRingerMode(AudioManager.RINGER_MODE_SILENT);

        // 2. Dim brightness (requires WRITE_SETTINGS)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.System.canWrite(ctx)) {
                Settings.System.putInt(ctx.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS, 10);
            }
        } catch (Exception ignored) {}

        // 3. Schedule morning reminder if requested
        String alarmMsg = "";
        if (wakeHour >= 0) {
            scheduleMorningAlarm(ctx, wakeHour, wakeMin);
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
               .putInt(KEY_WAKE, wakeHour).putInt(KEY_WAKE_M, wakeMin).apply();
            alarmMsg = String.format(Locale.US,
                " I'll wake you at **%d:%02d**.", wakeHour, wakeMin);
        }

        return "[EMOTION:warm] Goodnight, sir. Sweet dreams." + alarmMsg +
               " Systems entering low-power mode. I'll be here when you need me.";
    }

    public static String deactivate(Context ctx) {
        // Restore ringer
        AudioManager am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
        if (am != null) am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
        // Restore brightness
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.System.canWrite(ctx)) {
                Settings.System.putInt(ctx.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS, 200);
            }
        } catch (Exception ignored) {}
        return "[EMOTION:excited] Good morning, sir. Rested and ready. Systems nominal. How can I assist?";
    }

    /** Parse wake time from text like "goodnight, wake me at 7" or "sleep mode, 6:30" */
    public static int[] parseWakeTime(String text) {
        String lower = text.toLowerCase(Locale.US);
        // "wake me at 7:30" or "at 7" or "7am"
        Matcher m = Pattern.compile("(?:wake(?:\\s+me)?\\s+(?:up\\s+)?at|at)\\s+(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?")
            .matcher(lower);
        if (m.find()) {
            int h = Integer.parseInt(m.group(1));
            int min = m.group(2) != null ? Integer.parseInt(m.group(2)) : 0;
            if ("pm".equals(m.group(3)) && h < 12) h += 12;
            if ("am".equals(m.group(3)) && h == 12) h = 0;
            return new int[]{h, min};
        }
        // "7 o'clock" "wake me at 6"
        Matcher m2 = Pattern.compile("(\\d{1,2})\\s*(?:o'?clock|am|pm)").matcher(lower);
        if (m2.find()) {
            int h = Integer.parseInt(m2.group(1));
            String ampm = m2.group().replaceAll("[^a-z]", "");
            if (ampm.contains("pm") && h < 12) h += 12;
            return new int[]{h, 0};
        }
        return null; // no wake time specified
    }

    private static void scheduleMorningAlarm(Context ctx, int hour, int min) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, min);
        cal.set(Calendar.SECOND, 0);
        if (cal.getTimeInMillis() <= System.currentTimeMillis())
            cal.add(Calendar.DAY_OF_YEAR, 1);

        Intent notifIntent = new Intent(ctx, MorningAlarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(ctx, NOTIF_ID, notifIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
            else
                am.setExact(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
        } catch (Exception ignored) {}
    }
}
