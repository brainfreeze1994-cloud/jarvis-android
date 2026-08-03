package com.jarvis.ai;

import android.app.Notification;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Reads incoming notifications and broadcasts them so MainActivity can speak them.
 * Also maintains a static buffer for NotificationSummary.
 * User must grant "Notification Access" in Settings > Notifications > Special app access.
 */
public class NotificationService extends NotificationListenerService {

    public static final String ACTION_NOTIFY = "com.jarvis.ai.NEW_NOTIFICATION";
    public static final String EXTRA_APP     = "notif_app";
    public static final String EXTRA_TEXT    = "notif_text";

    // Static buffer for summary feature
    public static class NotifEntry {
        public final String app;
        public final String text;
        public NotifEntry(String app, String text) { this.app = app; this.text = text; }
    }

    private static final List<NotifEntry> recentBuffer = Collections.synchronizedList(new ArrayList<>());
    private static final int MAX_BUFFER = 50;

    public static List<NotifEntry> getRecent(int max) {
        synchronized (recentBuffer) {
            int from = Math.max(0, recentBuffer.size() - max);
            return new ArrayList<>(recentBuffer.subList(from, recentBuffer.size()));
        }
    }

    public static void clearBuffer() { recentBuffer.clear(); }

    // Skip our own notifications and system noise
    private static final String[] SKIP_PACKAGES = {
        "com.jarvis.ai", "android", "com.android.systemui",
        "com.android.launcher", "com.google.android.gms"
    };

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null) return;
        String pkg = sbn.getPackageName();
        for (String skip : SKIP_PACKAGES) if (pkg.startsWith(skip)) return;

        Notification n = sbn.getNotification();
        if (n == null) return;

        Bundle extras = n.extras;
        if (extras == null) return;

        CharSequence title = extras.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence text  = extras.getCharSequence(Notification.EXTRA_TEXT);
        if (text == null || text.toString().trim().isEmpty()) return;

        String appName = getAppName(pkg);
        String message = (title != null ? title.toString() + ": " : "") + text.toString();

        // Add to buffer
        synchronized (recentBuffer) {
            recentBuffer.add(new NotifEntry(appName, message));
            if (recentBuffer.size() > MAX_BUFFER)
                recentBuffer.remove(0);
        }

        // Broadcast for live announcements
        android.content.Intent intent = new android.content.Intent(ACTION_NOTIFY);
        intent.setPackage(getPackageName());
        intent.putExtra(EXTRA_APP, appName);
        intent.putExtra(EXTRA_TEXT, message);
        sendBroadcast(intent);
    }

    private String getAppName(String pkg) {
        try {
            PackageManager pm = getPackageManager();
            ApplicationInfo info = pm.getApplicationInfo(pkg, 0);
            return pm.getApplicationLabel(info).toString();
        } catch (Exception e) { return pkg; }
    }
}
