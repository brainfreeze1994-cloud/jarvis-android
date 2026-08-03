package com.jarvis.ai;

import android.content.Context;
import android.service.notification.StatusBarNotification;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Smart Notification Summary — reads all pending notifications and summarises them.
 * "What did I miss?" / "Read my notifications" / "Summarise notifications"
 * Works with NotificationService (NotificationListenerService).
 */
public class NotificationSummary {

    public static boolean isSummaryCommand(String text) {
        String lower = text.toLowerCase(Locale.US);
        return lower.contains("what did i miss") || lower.contains("my notifications") ||
               lower.contains("read notifications") || lower.contains("notification summary") ||
               lower.contains("summarise notifications") || lower.contains("summarize notifications") ||
               lower.contains("what notifications") || lower.contains("any messages") ||
               lower.contains("missed notifications") || lower.contains("pending notifications") ||
               lower.contains("check notifications") || lower.contains("unread notifications");
    }

    public static String summarise(Context ctx) {
        // Get notifications via NotificationService static buffer
        List<NotificationService.NotifEntry> notifs = NotificationService.getRecent(20);

        if (notifs == null || notifs.isEmpty())
            return "[EMOTION:excited] No pending notifications, sir. You're all caught up.";

        // Group by app
        Map<String, List<String>> byApp = new HashMap<>();
        for (NotificationService.NotifEntry n : notifs) {
            if (!byApp.containsKey(n.app)) byApp.put(n.app, new ArrayList<>());
            byApp.get(n.app).add(n.text);
        }

        StringBuilder sb = new StringBuilder("[EMOTION:neutral] **Notification Summary, sir:**\n\n");
        int total = 0;
        for (Map.Entry<String, List<String>> entry : byApp.entrySet()) {
            String app = entry.getKey();
            List<String> msgs = entry.getValue();
            sb.append("**").append(app).append("** (").append(msgs.size()).append(")\n");
            // Show up to 2 per app
            for (int i = 0; i < Math.min(2, msgs.size()); i++) {
                String msg = msgs.get(i);
                if (msg.length() > 80) msg = msg.substring(0, 77) + "…";
                sb.append("  • ").append(msg).append("\n");
            }
            if (msgs.size() > 2) sb.append("  _…and ").append(msgs.size() - 2).append(" more_\n");
            sb.append("\n");
            total += msgs.size();
        }
        sb.append("**Total: ").append(total).append(" notification(s)** from **")
          .append(byApp.size()).append(" app(s)**.");
        return sb.toString().trim();
    }

    /** Quick spoken summary for TTS */
    public static String getSpokenSummary(Context ctx) {
        List<NotificationService.NotifEntry> notifs = NotificationService.getRecent(20);
        if (notifs == null || notifs.isEmpty())
            return "No notifications, sir.";

        Map<String, Integer> counts = new HashMap<>();
        for (NotificationService.NotifEntry n : notifs)
            counts.merge(n.app, 1, Integer::sum);

        StringBuilder sb = new StringBuilder("You have ");
        sb.append(notifs.size()).append(" notification");
        if (notifs.size() != 1) sb.append("s");
        sb.append(" from ").append(counts.size()).append(" app");
        if (counts.size() != 1) sb.append("s");
        sb.append(": ");
        int i = 0;
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            sb.append(e.getValue()).append(" from ").append(e.getKey());
            if (++i < counts.size()) sb.append(", ");
        }
        sb.append(". Sir.");
        return sb.toString();
    }
}
