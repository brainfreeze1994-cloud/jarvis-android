package com.jarvis.ai;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Locale;

/**
 * Voice Shortcuts Widget — home screen widget with 4 customisable voice command buttons.
 * Tap any button → HENRY executes that command directly.
 */
public class VoiceShortcutWidget extends AppWidgetProvider {

    public static final String ACTION_BTN1 = "com.jarvis.ai.WIDGET_BTN1";
    public static final String ACTION_BTN2 = "com.jarvis.ai.WIDGET_BTN2";
    public static final String ACTION_BTN3 = "com.jarvis.ai.WIDGET_BTN3";
    public static final String ACTION_BTN4 = "com.jarvis.ai.WIDGET_BTN4";
    public static final String ACTION_MIC  = "com.jarvis.ai.WIDGET_MIC";

    private static final String PREFS      = "voice_widget_prefs";
    private static final String KEY_CMDS   = "widget_commands";

    // Default shortcut commands
    private static final String[] DEFAULT_LABELS   = {"📰 News",    "☀️ Weather", "📅 Reminder", "🎵 Music"};
    private static final String[] DEFAULT_COMMANDS = {"read news", "weather today", "show reminders", "play music"};

    @Override
    public void onUpdate(Context ctx, AppWidgetManager mgr, int[] ids) {
        for (int id : ids) updateWidget(ctx, mgr, id);
    }

    public static void updateWidget(Context ctx, AppWidgetManager mgr, int widgetId) {
        RemoteViews views = new RemoteViews(ctx.getPackageName(), R.layout.widget_voice_shortcuts);

        String[] labels   = getLabels(ctx);
        String[] commands = getCommands(ctx);

        // Set button labels
        try { views.setTextViewText(R.id.wb_btn1, labels[0]); } catch (Exception ignored) {}
        try { views.setTextViewText(R.id.wb_btn2, labels[1]); } catch (Exception ignored) {}
        try { views.setTextViewText(R.id.wb_btn3, labels[2]); } catch (Exception ignored) {}
        try { views.setTextViewText(R.id.wb_btn4, labels[3]); } catch (Exception ignored) {}

        // Set click listeners
        views.setOnClickPendingIntent(R.id.wb_btn1, buildIntent(ctx, ACTION_BTN1, commands[0]));
        views.setOnClickPendingIntent(R.id.wb_btn2, buildIntent(ctx, ACTION_BTN2, commands[1]));
        views.setOnClickPendingIntent(R.id.wb_btn3, buildIntent(ctx, ACTION_BTN3, commands[2]));
        views.setOnClickPendingIntent(R.id.wb_btn4, buildIntent(ctx, ACTION_BTN4, commands[3]));
        views.setOnClickPendingIntent(R.id.wb_mic,  buildIntent(ctx, ACTION_MIC,  ""));

        mgr.updateAppWidget(widgetId, views);
    }

    private static PendingIntent buildIntent(Context ctx, String action, String command) {
        Intent i = new Intent(ctx, MainActivity.class);
        i.setAction(action);
        i.putExtra("widget_command", command);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(ctx, action.hashCode(), i,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    public static String[] getLabels(Context ctx) {
        try {
            JSONArray arr = loadShortcuts(ctx);
            String[] result = DEFAULT_LABELS.clone();
            for (int i = 0; i < Math.min(arr.length(), 4); i++) {
                String label = arr.getJSONObject(i).optString("label", DEFAULT_LABELS[i]);
                result[i] = label;
            }
            return result;
        } catch (Exception e) { return DEFAULT_LABELS.clone(); }
    }

    public static String[] getCommands(Context ctx) {
        try {
            JSONArray arr = loadShortcuts(ctx);
            String[] result = DEFAULT_COMMANDS.clone();
            for (int i = 0; i < Math.min(arr.length(), 4); i++) {
                String cmd = arr.getJSONObject(i).optString("command", DEFAULT_COMMANDS[i]);
                result[i] = cmd;
            }
            return result;
        } catch (Exception e) { return DEFAULT_COMMANDS.clone(); }
    }

    public static String setShortcut(Context ctx, int slot, String label, String command) {
        if (slot < 1 || slot > 4) return "[EMOTION:neutral] Slot must be 1-4, sir.";
        try {
            JSONArray arr = loadShortcuts(ctx);
            // Ensure 4 entries
            while (arr.length() < 4) {
                JSONObject d = new JSONObject();
                int idx = arr.length();
                d.put("label", DEFAULT_LABELS[idx]);
                d.put("command", DEFAULT_COMMANDS[idx]);
                arr.put(d);
            }
            arr.getJSONObject(slot - 1).put("label", label).put("command", command);
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
               .putString(KEY_CMDS, arr.toString()).apply();
            // Refresh widget
            AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
            android.content.ComponentName comp = new android.content.ComponentName(ctx, VoiceShortcutWidget.class);
            int[] ids = mgr.getAppWidgetIds(comp);
            for (int id : ids) updateWidget(ctx, mgr, id);
            return "[EMOTION:excited] Widget button " + slot + " set to **\"" + label + "\"**, sir!";
        } catch (Exception e) {
            return "[EMOTION:concerned] Couldn't update widget, sir: " + e.getMessage();
        }
    }

    public static boolean isWidgetCommand(String text) {
        String lower = text.toLowerCase(Locale.US);
        return lower.contains("widget button") || lower.contains("set widget") ||
               lower.contains("shortcut button") || lower.contains("widget shortcut") ||
               lower.contains("customize widget") || lower.contains("customise widget") ||
               lower.contains("change button");
    }

    public static String handleWidgetSetup(Context ctx, String text) {
        // "Set widget button 2 to weather"
        String lower = text.toLowerCase(Locale.US);
        int slot = -1;
        for (int i = 1; i <= 4; i++) {
            if (lower.contains("button " + i) || lower.contains("slot " + i) ||
                lower.contains(ordinal(i))) { slot = i; break; }
        }
        if (slot == -1) return "[EMOTION:neutral] Which button (1-4) should I update, sir?";
        // Extract label/command
        String cmd = lower.replaceAll("(?i)set.*button.*\\d+|widget|shortcut|slot|to|button|customize|customise", "").trim();
        if (cmd.isEmpty()) return "[EMOTION:neutral] What command should that button run, sir?";
        String label = cmd.length() > 12 ? cmd.substring(0, 12) + "…" : cmd;
        return setShortcut(ctx, slot, "⚡ " + capitalize(label), cmd);
    }

    private static JSONArray loadShortcuts(Context ctx) {
        try { return new JSONArray(ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_CMDS, "[]")); }
        catch (Exception e) { return new JSONArray(); }
    }

    private static String ordinal(int n) {
        String[] o = {"first","second","third","fourth"};
        return n >= 1 && n <= 4 ? o[n-1] : "";
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
