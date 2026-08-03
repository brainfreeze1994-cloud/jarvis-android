package com.jarvis.ai;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.camera2.CameraManager;
import android.os.BatteryManager;
import android.os.Build;
import android.provider.Settings;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DeviceCommands {

    private static boolean flashOn      = false;
    private static String  flashCamId   = null;

    // ── Flashlight ────────────────────────────────────────────────────────────
    public static String toggleFlashlight(Context ctx) {
        return setFlashlight(ctx, !flashOn);
    }

    public static String setFlashlight(Context ctx, boolean on) {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
                return "Flashlight not supported on this Android version, sir.";
            CameraManager cm = (CameraManager) ctx.getSystemService(Context.CAMERA_SERVICE);
            if (cm == null) return "Camera service unavailable, sir.";
            if (flashCamId == null) {
                String[] ids = cm.getCameraIdList();
                if (ids.length == 0) return "No camera found, sir.";
                flashCamId = ids[0];
            }
            cm.setTorchMode(flashCamId, on);
            flashOn = on;
            return on ? "Flashlight on, sir." : "Flashlight off, sir.";
        } catch (Exception e) {
            return "Couldn't access flashlight, sir.";
        }
    }

    // ── Screen brightness ─────────────────────────────────────────────────────
    public static String setBrightness(AppCompatActivity activity, int pct) {
        try {
            float level = Math.max(0.01f, Math.min(1f, pct / 100f));
            WindowManager.LayoutParams lp = activity.getWindow().getAttributes();
            lp.screenBrightness = level;
            activity.getWindow().setAttributes(lp);
            return "Brightness set to " + pct + "%, sir.";
        } catch (Exception e) {
            return "Couldn't adjust brightness, sir.";
        }
    }

    // ── Do Not Disturb ────────────────────────────────────────────────────────
    public static String setDoNotDisturb(Context ctx, boolean enable) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return "Do Not Disturb control requires Android 6+, sir.";
        try {
            NotificationManager nm =
                (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return "Notification service unavailable, sir.";
            if (!nm.isNotificationPolicyAccessGranted()) {
                Intent i = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(i);
                return "Please grant Do Not Disturb access, sir. Opening settings now.";
            }
            nm.setInterruptionFilter(enable
                ? NotificationManager.INTERRUPTION_FILTER_NONE
                : NotificationManager.INTERRUPTION_FILTER_ALL);
            return enable ? "Do Not Disturb enabled, sir." : "Do Not Disturb disabled, sir.";
        } catch (Exception e) {
            return "Couldn't change DND setting, sir.";
        }
    }

    // ── Battery ───────────────────────────────────────────────────────────────
    public static String getBatteryInfo(Context ctx) {
        try {
            Intent bi = ctx.registerReceiver(null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (bi == null) return "Battery information unavailable, sir.";
            int level  = bi.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale  = bi.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            int status = bi.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            int pct    = scale > 0 ? level * 100 / scale : -1;
            boolean charging = status == BatteryManager.BATTERY_STATUS_CHARGING
                            || status == BatteryManager.BATTERY_STATUS_FULL;
            boolean full     = status == BatteryManager.BATTERY_STATUS_FULL;
            String statusStr = full ? "fully charged" : charging ? "charging" : "on battery";
            String emoji     = pct > 80 ? "🔋" : pct > 40 ? "🔋" : "🪫";
            return emoji + " Battery at **" + pct + "%** — " + statusStr + ", sir.";
        } catch (Exception e) {
            return "Battery information unavailable, sir.";
        }
    }

    // ── Date / Time ───────────────────────────────────────────────────────────
    public static String getDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, MMMM d yyyy — h:mm a", Locale.US);
        return "🕐 " + sdf.format(new Date()) + ", sir.";
    }
}
