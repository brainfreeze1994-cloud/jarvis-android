package com.jarvis.ai;

import android.app.Notification;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

public class NotificationService extends NotificationListenerService {

    public static final String ACTION_NOTIFY = "com.jarvis.ai.NEW_NOTIFICATION";
    public static final String EXTRA_APP     = "notif_app";
    public static final String EXTRA_TEXT    = "notif_text";

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
