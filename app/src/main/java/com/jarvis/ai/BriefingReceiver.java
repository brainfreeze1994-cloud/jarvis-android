package com.jarvis.ai;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import java.util.Calendar;

/**
 * Fires the Daily Briefing automatically each morning without being asked —
 * that's the actual point of it. A briefing you have to remember to request
 * is just another command; one that shows up on its own is the difference
 * this feature is supposed to make.
 */
public class BriefingReceiver extends BroadcastReceiver {

    private static final String ACTION_TICK = "com.jarvis.ai.BRIEFING_TICK";
    private static final String CHANNEL_ID  = "henry_briefing";
    private static final int    NOTIF_ID    = 9200;
    private static final String PREFS       = "jarvis_prefs";
    private static final String KEY_ENABLED = "briefing_enabled";
    private static final String KEY_HOUR    = "briefing_hour";
    private static final String KEY_MINUTE  = "briefing_minute";

    @Override
    public void onReceive(Context ctx, Intent intent) {
        String action = intent.getAction();
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            if (p.getBoolean(KEY_ENABLED, true)) scheduleNext(ctx);
            return;
        }

        if (!p.getBoolean(KEY_ENABLED, true)) return;

        DailyBriefing.generate(ctx, briefing -> {
            String clean = briefing.replaceAll("\\[EMOTION:\\w+\\]\\s*", "").trim();
            notify(ctx, clean);
            scheduleNext(ctx); // re-arm for tomorrow
        });
    }

    private void notify(Context ctx, String text) {
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                CHANNEL_ID, "H.E.N.R.Y Daily Briefing", NotificationManager.IMPORTANCE_DEFAULT);
            ch.setDescription("Your automatic morning briefing");
            nm.createNotificationChannel(ch);
        }
        Intent open = new Intent(ctx, MainActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi = PendingIntent.getActivity(ctx, 0, open,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification n = new NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("◈ Your Briefing, Sir")
            .setContentText(text)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build();
        nm.notify(NOTIF_ID, n);
    }

    // ── Scheduling ───────────────────────────────────────────────────────────
    /** Schedules the next briefing at the configured hour/minute (default 7:30am). */
    public static void scheduleNext(Context ctx) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int hour   = p.getInt(KEY_HOUR, 7);
        int minute = p.getInt(KEY_MINUTE, 30);

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);
        if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1); // already passed today — schedule for tomorrow
        }

        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        Intent i = new Intent(ctx, BriefingReceiver.class);
        i.setAction(ACTION_TICK);
        PendingIntent pi = PendingIntent.getBroadcast(ctx, 0, i,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(),
            AlarmManager.INTERVAL_DAY, pi);
    }

    public static void cancel(Context ctx) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        Intent i = new Intent(ctx, BriefingReceiver.class);
        i.setAction(ACTION_TICK);
        PendingIntent pi = PendingIntent.getBroadcast(ctx, 0, i,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        am.cancel(pi);
    }

    public static void setEnabled(Context ctx, boolean enabled) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
           .putBoolean(KEY_ENABLED, enabled).apply();
        if (enabled) scheduleNext(ctx); else cancel(ctx);
    }
}
