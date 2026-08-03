package com.jarvis.ai;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BatteryGuardian {

    private static final String PREFS      = "battery_guardian";
    private static final String KEY_THRESH = "threshold";
    private static final String KEY_ALERTED = "alerted";
    private static final String CHANNEL_ID  = "henry_battery";

    public interface AlertCallback {
        void onAlert(int level, int threshold);
    }

    // ── Read threshold ────────────────────────────────────────────────────────
    public static int getThreshold(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_THRESH, 20);
    }

    public static void setThreshold(Context ctx, int pct) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
           .putInt(KEY_THRESH, pct)
           .putBoolean(KEY_ALERTED, false)
           .apply();
    }

    // ── Live battery data ─────────────────────────────────────────────────────
    public static int getBatteryLevel(Context ctx) {
        Intent battery = ctx.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (battery == null) return -1;
        int level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        return (int) ((level / (float) scale) * 100);
    }

    public static boolean isCharging(Context ctx) {
        Intent battery = ctx.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (battery == null) return false;
        int status = battery.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
               status == BatteryManager.BATTERY_STATUS_FULL;
    }

    public static String getStatus(Context ctx) {
        int level    = getBatteryLevel(ctx);
        boolean chg  = isCharging(ctx);
        int threshold = getThreshold(ctx);
        String emotion = level <= 15 ? "concerned" : level <= threshold ? "concerned" : "neutral";
        String chgStr  = chg ? "charging ⚡" : "not charging";
        String alertStr = (!chg && level > threshold)
            ? " Alert set at " + threshold + "%." : "";
        return "[EMOTION:" + emotion + "] Battery at **" + level + "%**, " + chgStr + "." + alertStr + " Sir.";
    }

    // ── Periodic check — call this every ~2 minutes ───────────────────────────
    public static void check(Context ctx, AlertCallback cb) {
        int level     = getBatteryLevel(ctx);
        int threshold = getThreshold(ctx);
        boolean chg   = isCharging(ctx);
        boolean alerted = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ALERTED, false);

        if (!chg && level <= threshold && !alerted) {
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
               .putBoolean(KEY_ALERTED, true).apply();
            sendNotification(ctx, level);
            if (cb != null) cb.onAlert(level, threshold);
        }
        // Reset alert flag once charging begins
        if (chg && alerted) {
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
               .putBoolean(KEY_ALERTED, false).apply();
        }
    }

    private static void sendNotification(Context ctx, int level) {
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                CHANNEL_ID, "HENRY Battery Alerts", NotificationManager.IMPORTANCE_HIGH);
            nm.createNotificationChannel(ch);
        }
        NotificationCompat.Builder b = new NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("H.E.N.R.Y — Low Battery")
            .setContentText("Battery at " + level + "%, sir. Please plug in.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true);
        nm.notify(7001, b.build());
    }

    // ── Parse threshold-setting commands ─────────────────────────────────────
    public static String parseThresholdCommand(String text) {
        String lower = text.toLowerCase(Locale.US);
        boolean isThresholdCmd = (lower.contains("battery") || lower.contains("charge")) &&
            (lower.contains("alert") || lower.contains("warn") ||
             lower.contains("notify") || lower.contains("remind") || lower.contains("below"));
        if (!isThresholdCmd) return null;
        Matcher m = Pattern.compile("(\\d+)\\s*%?").matcher(lower);
        if (m.find()) return m.group(1);
        return null;
    }
}
