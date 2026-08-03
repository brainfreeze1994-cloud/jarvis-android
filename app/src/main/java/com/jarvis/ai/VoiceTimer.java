package com.jarvis.ai;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.CountDownTimer;

import androidx.core.app.NotificationCompat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * In-process countdown timer (survives config changes but not process kill).
 * For sleep-proof alarms use ReminderManager instead.
 */
public class VoiceTimer {

    public interface Callback {
        void onTick(long secondsLeft);
        void onFinish(String label);
    }

    private static CountDownTimer activeTimer = null;
    private static String         activeLabel = null;
    private static long           endTimeMs   = -1;

    private static final String CHANNEL_ID = "henry_timer";

    // ── Parse ─────────────────────────────────────────────────────────────────
    /** Returns total seconds if userText looks like a timer command, else -1. */
    public static long[] parse(String text) {
        String t = text.toLowerCase(java.util.Locale.US);
        if (!t.contains("timer") && !t.contains("count") && !t.contains("minute")
            && !t.contains("second") && !t.contains("hour")) return null;
        // Look for Nh Nm Ns patterns
        long totalSecs = 0;
        Matcher hm = Pattern.compile("(\\d+)\\s*h(?:our)?s?").matcher(t);
        if (hm.find()) totalSecs += Long.parseLong(hm.group(1)) * 3600;
        Matcher mm = Pattern.compile("(\\d+)\\s*m(?:in(?:ute)?s?)?").matcher(t);
        if (mm.find()) totalSecs += Long.parseLong(mm.group(1)) * 60;
        Matcher sm = Pattern.compile("(\\d+)\\s*s(?:ec(?:ond)?s?)?").matcher(t);
        if (sm.find()) totalSecs += Long.parseLong(sm.group(1));
        if (totalSecs <= 0) return null;
        // Extract optional label after "called"/"named"/"label"
        String label = "timer";
        Matcher lm = Pattern.compile("(?:called|named|label(?:led)?)\\s+[\"']?([\\w\\s]+)[\"']?").matcher(t);
        if (lm.find()) label = lm.group(1).trim();
        return new long[]{ totalSecs, 0 }; // [0]=seconds, use label separately
    }

    /** Full start — parses duration + optional label from command text. */
    public static String startFromText(Context ctx, String text, Callback cb) {
        String t = text.toLowerCase(java.util.Locale.US);
        if (!t.contains("timer") && !t.contains("count")) {
            if (!t.matches(".*\\d+\\s*(hour|minute|min|second|sec).*")) return null;
        }
        long totalSecs = 0;
        Matcher hm = Pattern.compile("(\\d+)\\s*h(?:our)?s?").matcher(t);
        if (hm.find()) totalSecs += Long.parseLong(hm.group(1)) * 3600;
        Matcher mm = Pattern.compile("(\\d+)\\s*m(?:in(?:ute)?s?)?").matcher(t);
        if (mm.find()) totalSecs += Long.parseLong(mm.group(1)) * 60;
        Matcher sm = Pattern.compile("(\\d+)\\s*s(?:ec(?:ond)?s?)?").matcher(t);
        if (sm.find()) totalSecs += Long.parseLong(sm.group(1));
        if (totalSecs <= 0) return null;

        String label = "Timer";
        Matcher lm = Pattern.compile("(?:called|named|for)\\s+[\"']?([a-z][a-z0-9\\s]{1,30})[\"']?").matcher(t);
        if (lm.find()) label = capitalize(lm.group(1).trim());

        return start(ctx, totalSecs, label, cb);
    }

    public static String start(Context ctx, long totalSecs, String label, Callback cb) {
        cancel();
        activeLabel = label;
        endTimeMs   = System.currentTimeMillis() + totalSecs * 1000;

        String friendly = friendlyDuration(totalSecs);
        createChannel(ctx);

        activeTimer = new CountDownTimer(totalSecs * 1000, 1000) {
            @Override public void onTick(long msLeft) {
                long secsLeft = msLeft / 1000;
                if (cb != null) cb.onTick(secsLeft);
            }
            @Override public void onFinish() {
                activeTimer = null;
                endTimeMs   = -1;
                if (cb != null) cb.onFinish(label);
                fireNotification(ctx, label);
            }
        }.start();

        return "[EMOTION:excited] " + label + " set for **" + friendly + "**, sir. I'll let you know.";
    }

    public static String cancel() {
        if (activeTimer != null) {
            activeTimer.cancel();
            activeTimer = null;
            endTimeMs   = -1;
            String old = activeLabel;
            activeLabel = null;
            return old != null ? "[EMOTION:neutral] " + old + " cancelled, sir." : null;
        }
        return null;
    }

    public static String status() {
        if (activeTimer == null || endTimeMs < 0) return null;
        long secsLeft = (endTimeMs - System.currentTimeMillis()) / 1000;
        if (secsLeft < 0) return null;
        return "[EMOTION:neutral] " + (activeLabel != null ? activeLabel : "Timer") +
               " — **" + friendlyDuration(secsLeft) + "** remaining, sir.";
    }

    public static boolean isActive() { return activeTimer != null; }

    // ── Helpers ───────────────────────────────────────────────────────────────
    public static String friendlyDuration(long secs) {
        long h = secs / 3600, m = (secs % 3600) / 60, s = secs % 60;
        StringBuilder sb = new StringBuilder();
        if (h > 0) sb.append(h).append(h == 1 ? " hour " : " hours ");
        if (m > 0) sb.append(m).append(m == 1 ? " minute " : " minutes ");
        if (s > 0 && h == 0) sb.append(s).append(s == 1 ? " second" : " seconds");
        return sb.toString().trim();
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static void createChannel(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                CHANNEL_ID, "H.E.N.R.Y Timer",
                NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("Timer done alerts");
            NotificationManager nm =
                (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    private static void fireNotification(Context ctx, String label) {
        createChannel(ctx);
        Intent tap = new Intent(ctx, MainActivity.class);
        tap.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int piFlags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
            ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            : PendingIntent.FLAG_UPDATE_CURRENT;
        PendingIntent pi = PendingIntent.getActivity(ctx, 9001, tap, piFlags);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("⏱ " + label + " Done")
            .setContentText("Your timer has finished, sir.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi);
        NotificationManager nm =
            (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(9001, builder.build());
    }
}
