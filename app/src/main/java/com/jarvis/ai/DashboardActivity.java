package com.jarvis.ai;

import android.content.SharedPreferences;
import android.graphics.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.util.*;

/**
 * DashboardActivity — HENRY Stats & Activity Dashboard
 * Shows usage stats, conversation count, memory facts count,
 * activity chart, daily briefing, system status.
 */
public class DashboardActivity extends AppCompatActivity {

    private static final String PREFS = "henry_dashboard";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(0xFF020C1B);
        scroll.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 48, 24, 48);

        // Title
        root.addView(makeTitle("◈ H·E·N·R·Y DASHBOARD"));
        root.addView(makeDivider());

        // Stats cards row
        LinearLayout statsRow = new LinearLayout(this);
        statsRow.setOrientation(LinearLayout.HORIZONTAL);
        statsRow.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        int totalMessages = prefs.getInt("total_messages", 0);
        int todayMessages = prefs.getInt("today_messages", 0);
        int imagesGenerated = prefs.getInt("images_generated", 0);
        int voiceCommands  = prefs.getInt("voice_commands", 0);
        List<String> facts = SmartMemory.getFacts(this);

        statsRow.addView(makeStatCard("💬", String.valueOf(totalMessages), "Total Chats", 0xFF00D4FF));
        statsRow.addView(makeStatCard("🗓", String.valueOf(todayMessages), "Today", 0xFF00FF99));
        statsRow.addView(makeStatCard("💾", String.valueOf(facts.size()), "Memories", 0xFFCC88FF));
        statsRow.addView(makeStatCard("🖼", String.valueOf(imagesGenerated), "Images", 0xFFFF9944));
        root.addView(statsRow);

        root.addView(makeSpacer(16));

        // System Status
        root.addView(makeTitle("◈ SYSTEM STATUS"));
        root.addView(makeStatusRow("AI Backend", "ONLINE", 0xFF00FF99));
        root.addView(makeStatusRow("Voice Engine", "ACTIVE", 0xFF00FF99));
        root.addView(makeStatusRow("Memory Banks", facts.isEmpty() ? "EMPTY" : facts.size() + " FACTS", 0xFF00D4FF));
        root.addView(makeStatusRow("Brain Modules", "9 LOADED", 0xFF00D4FF));
        root.addView(makeStatusRow("Security", BiometricLock.isLockEnabled(this) ? "LOCKED" : "UNLOCKED",
            BiometricLock.isLockEnabled(this) ? 0xFF00FF99 : 0xFFFF9944));
        root.addView(makeStatusRow("Stealth Mode", BiometricLock.isStealthMode(this) ? "ON" : "OFF",
            BiometricLock.isStealthMode(this) ? 0xFFFF4444 : 0xFF2a6a8a));
        root.addView(makeDivider());

        // Activity bar chart
        root.addView(makeTitle("◈ WEEKLY ACTIVITY"));
        root.addView(makeActivityChart(prefs));
        root.addView(makeDivider());

        // Memory facts preview
        root.addView(makeTitle("◈ MEMORY SNAPSHOT"));
        if (facts.isEmpty()) {
            root.addView(makeBody("No memories yet. Converse with HENRY to build your profile."));
        } else {
            for (int i = 0; i < Math.min(5, facts.size()); i++) {
                root.addView(makeBody("◈ " + facts.get(i)));
            }
            if (facts.size() > 5) {
                root.addView(makeBody("… +" + (facts.size() - 5) + " more memories stored"));
            }
        }
        root.addView(makeDivider());

        // Security toggle
        root.addView(makeTitle("◈ SECURITY"));
        root.addView(makeToggleRow("Biometric Lock", BiometricLock.isLockEnabled(this), enabled -> {
            BiometricLock.setLockEnabled(this, enabled);
            Toast.makeText(this, enabled ? "Biometric lock enabled, sir." : "Lock disabled.", Toast.LENGTH_SHORT).show();
        }));
        root.addView(makeToggleRow("Stealth Mode", BiometricLock.isStealthMode(this), enabled -> {
            BiometricLock.setStealthMode(this, enabled);
            Toast.makeText(this, enabled ? "Stealth mode ON — HENRY hidden from recents." : "Stealth mode off.", Toast.LENGTH_SHORT).show();
        }));
        root.addView(makeDivider());

        // Close button
        Button close = new Button(this);
        close.setText("← BACK TO HENRY");
        close.setBackgroundColor(0x2200D4FF);
        close.setTextColor(0xFF00D4FF);
        close.setTextSize(13f);
        close.setPadding(0, 20, 0, 20);
        close.setOnClickListener(v -> finish());
        root.addView(close);

        scroll.addView(root);
        setContentView(scroll);
    }

    // ── Stat increment helpers (call from MainActivity) ──────────────────────
    public static void incrementMessages(android.content.Context ctx) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE);
        String today = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        String lastDay = p.getString("last_day", "");
        int todayCount = today.equals(lastDay) ? p.getInt("today_messages", 0) + 1 : 1;
        int weekDay = new java.util.Calendar.Builder().build().get(java.util.Calendar.DAY_OF_WEEK) - 1;
        String key = "day_" + weekDay;
        p.edit()
            .putInt("total_messages", p.getInt("total_messages", 0) + 1)
            .putInt("today_messages", todayCount)
            .putString("last_day", today)
            .putInt(key, p.getInt(key, 0) + 1)
            .apply();
    }

    public static void incrementImages(android.content.Context ctx) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE);
        p.edit().putInt("images_generated", p.getInt("images_generated", 0) + 1).apply();
    }

    public static void incrementVoice(android.content.Context ctx) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE);
        p.edit().putInt("voice_commands", p.getInt("voice_commands", 0) + 1).apply();
    }

    // ── UI Helpers ────────────────────────────────────────────────────────────
    private TextView makeTitle(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(0xFF00D4FF);
        tv.setTextSize(11f);
        tv.setLetterSpacing(0.15f);
        tv.setPadding(0, 20, 0, 8);
        return tv;
    }

    private TextView makeBody(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(0xFF2a6a8a);
        tv.setTextSize(12f);
        tv.setPadding(0, 4, 0, 4);
        return tv;
    }

    private View makeDivider() {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1));
        v.setBackgroundColor(0xFF081830);
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) v.getLayoutParams();
        lp.setMargins(0, 12, 0, 4);
        v.setLayoutParams(lp);
        return v;
    }

    private View makeSpacer(int dp) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp));
        return v;
    }

    private LinearLayout makeStatCard(String icon, String value, String label, int color) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(4, 0, 4, 0);
        card.setLayoutParams(lp);
        card.setBackgroundColor(0xFF060f1e);
        card.setPadding(8, 16, 8, 16);

        TextView ico = new TextView(this); ico.setText(icon); ico.setTextSize(22f); ico.setGravity(Gravity.CENTER);
        TextView val = new TextView(this); val.setText(value); val.setTextSize(20f); val.setTextColor(color); val.setGravity(Gravity.CENTER);
        TextView lbl = new TextView(this); lbl.setText(label); lbl.setTextSize(9f); lbl.setTextColor(0xFF2a6a8a); lbl.setGravity(Gravity.CENTER);

        card.addView(ico); card.addView(val); card.addView(lbl);
        return card;
    }

    private LinearLayout makeStatusRow(String label, String value, int color) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 6, 0, 6);

        TextView lbl = new TextView(this); lbl.setText(label); lbl.setTextColor(0xFF2a6a8a);
        lbl.setTextSize(12f); lbl.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView val = new TextView(this); val.setText(value); val.setTextColor(color);
        val.setTextSize(12f); val.setGravity(Gravity.END);
        val.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        row.addView(lbl); row.addView(val);
        return row;
    }

    private View makeActivityChart(SharedPreferences prefs) {
        String[] days = {"Sun","Mon","Tue","Wed","Thu","Fri","Sat"};
        int[] counts = new int[7];
        int max = 1;
        for (int i = 0; i < 7; i++) {
            counts[i] = prefs.getInt("day_" + i, 0);
            if (counts[i] > max) max = counts[i];
        }

        LinearLayout chart = new LinearLayout(this);
        chart.setOrientation(LinearLayout.HORIZONTAL);
        chart.setGravity(Gravity.BOTTOM);
        chart.setPadding(0, 16, 0, 0);
        chart.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 120));

        final int fMax = max;
        for (int i = 0; i < 7; i++) {
            LinearLayout col = new LinearLayout(this);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
            lp.setMargins(4, 0, 4, 0);
            col.setLayoutParams(lp);

            View bar = new View(this);
            int barH = counts[i] == 0 ? 4 : (int)(80f * counts[i] / fMax);
            LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, barH);
            bar.setLayoutParams(blp);
            bar.setBackgroundColor(0xFF00D4FF);

            TextView dayLbl = new TextView(this);
            dayLbl.setText(days[i]); dayLbl.setTextSize(8f);
            dayLbl.setTextColor(0xFF2a6a8a); dayLbl.setGravity(Gravity.CENTER);

            col.addView(bar); col.addView(dayLbl);
            chart.addView(col);
        }
        return chart;
    }

    interface ToggleListener { void onToggle(boolean enabled); }

    private LinearLayout makeToggleRow(String label, boolean current, ToggleListener listener) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 8, 0, 8);

        TextView lbl = new TextView(this); lbl.setText(label); lbl.setTextColor(0xFF4a9ab8);
        lbl.setTextSize(13f); lbl.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        Switch sw = new Switch(this);
        sw.setChecked(current);
        sw.setOnCheckedChangeListener((v, checked) -> listener.onToggle(checked));

        row.addView(lbl); row.addView(sw);
        return row;
    }
}
