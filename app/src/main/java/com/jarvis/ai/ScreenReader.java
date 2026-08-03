package com.jarvis.ai;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Screen Reader — reads visible text on the screen using AccessibilityService.
 * The service collects node text; MainActivity retrieves it via a static buffer.
 */
public class ScreenReader extends AccessibilityService {

    private static volatile String lastScreenText = null;
    private static volatile boolean capturing     = false;

    // ── Static helpers (called from MainActivity) ─────────────────────────────
    public static boolean isEnabled(Context ctx) {
        String flat = Settings.Secure.getString(
            ctx.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        return flat != null && flat.contains(ctx.getPackageName() + "/" + ScreenReader.class.getName());
    }

    public static void requestEnable(Context ctx) {
        Intent i = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(i);
    }

    public static boolean isScreenReaderCommand(String text) {
        String lower = text.toLowerCase(Locale.US);
        return lower.contains("read screen") || lower.contains("read the screen") ||
               lower.contains("what's on screen") || lower.contains("what is on screen") ||
               lower.contains("read this screen") || lower.contains("what does the screen say") ||
               lower.contains("screen text") || lower.contains("read visible text");
    }

    /** Ask the service to capture; returns captured text or null if not ready. */
    public static String captureNow() {
        capturing = true;
        // The service will fill lastScreenText on next event or directly
        // We trigger via a broadcast; here we return the last cached value
        String result = lastScreenText;
        lastScreenText = null;
        capturing = false;
        return result;
    }

    public static void storeScreenText(String text) {
        lastScreenText = text;
    }

    // ── AccessibilityService lifecycle ────────────────────────────────────────
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Capture screen text on window state change
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root != null) {
            String text = extractText(root, 0);
            if (!text.trim().isEmpty()) lastScreenText = text.trim();
            root.recycle();
        }
    }

    @Override public void onInterrupt() {}

    private String extractText(AccessibilityNodeInfo node, int depth) {
        if (node == null || depth > 10) return "";
        StringBuilder sb = new StringBuilder();
        CharSequence text = node.getText();
        CharSequence desc = node.getContentDescription();
        if (text != null && text.length() > 0) sb.append(text).append(" ");
        else if (desc != null && desc.length() > 0) sb.append(desc).append(" ");
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            sb.append(extractText(child, depth + 1));
            if (child != null) child.recycle();
        }
        return sb.toString();
    }
}
