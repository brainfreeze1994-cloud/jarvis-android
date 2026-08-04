package com.jarvis.ai;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Sleep Tracker — accelerometer-based movement detection overnight.
 * Measures restlessness → sleep quality score in morning.
 * "Start sleep tracking" / "How did I sleep?" / "Sleep report"
 */
public class SleepTracker implements SensorEventListener {

    private static final String PREFS        = "sleep_tracker_prefs";
    private static final String KEY_START    = "sleep_start";
    private static final String KEY_MOVES    = "sleep_moves";
    private static final String KEY_SCORE    = "sleep_score";
    private static final String KEY_LAST_RPT = "sleep_last_report";
    private static final String KEY_ACTIVE   = "sleep_active";

    private final Context ctx;
    private final SensorManager sm;
    private Sensor accel;

    private float lastX, lastY, lastZ;
    private boolean initialized = false;
    private static final float MOVE_THRESHOLD = 1.5f;
    private static final long  DEBOUNCE_MS    = 3000;
    private long lastMoveTime = 0;

    private static SleepTracker instance;

    private SleepTracker(Context ctx) {
        this.ctx = ctx.getApplicationContext();
        sm = (SensorManager) this.ctx.getSystemService(Context.SENSOR_SERVICE);
        accel = sm != null ? sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) : null;
    }

    public static SleepTracker getInstance(Context ctx) {
        if (instance == null) instance = new SleepTracker(ctx);
        return instance;
    }

    public static boolean isSleepCommand(String text) {
        String lower = text.toLowerCase(Locale.US);
        return lower.contains("sleep track") || lower.contains("track my sleep") ||
               lower.contains("start sleep") || lower.contains("sleep monitor") ||
               lower.contains("how did i sleep") || lower.contains("sleep report") ||
               lower.contains("sleep quality") || lower.contains("sleep score") ||
               lower.contains("stop sleep track") || lower.contains("end sleep track");
    }

    public String start() {
        if (accel == null)
            return "[EMOTION:neutral] No accelerometer found, sir. Sleep tracking requires hardware sensor.";
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        p.edit().putLong(KEY_START, System.currentTimeMillis())
                .putInt(KEY_MOVES, 0)
                .putBoolean(KEY_ACTIVE, true).apply();
        initialized = false;
        sm.registerListener(this, accel, SensorManager.SENSOR_DELAY_NORMAL);
        String time = new SimpleDateFormat("h:mm a", Locale.US).format(new Date());
        return "[EMOTION:warm] Sleep tracking started at " + time + ", sir. " +
               "Place your phone face-down on the bed. Say 'sleep report' in the morning. Sweet dreams.";
    }

    public String stop() {
        sm.unregisterListener(this);
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
           .putBoolean(KEY_ACTIVE, false).apply();
        return generateReport();
    }

    public String generateReport() {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long startTime = p.getLong(KEY_START, 0);
        int moves      = p.getInt(KEY_MOVES, 0);
        if (startTime == 0)
            return "[EMOTION:neutral] No sleep session recorded yet, sir. Say 'start sleep tracking' before bed.";

        long now       = System.currentTimeMillis();
        long durationMs = now - startTime;
        long hours     = durationMs / 3_600_000;
        long minutes   = (durationMs % 3_600_000) / 60_000;

        // Quality score: based on movement count relative to sleep duration
        double movesPerHour = hours > 0 ? (double) moves / hours : moves;
        int score;
        String quality, emoji, advice, emotion;

        if (movesPerHour < 5) {
            score = 95; quality = "Excellent"; emoji = "🌟"; emotion = "proud";
            advice = "Deep, restful sleep. Your body fully recovered, sir.";
        } else if (movesPerHour < 15) {
            score = 80; quality = "Good"; emoji = "✅"; emotion = "excited";
            advice = "Solid sleep with some light movement. Well rested, sir.";
        } else if (movesPerHour < 30) {
            score = 65; quality = "Fair"; emoji = "⚠️"; emotion = "neutral";
            advice = "Moderate restlessness detected. Consider sleep hygiene improvements, sir.";
        } else if (movesPerHour < 50) {
            score = 45; quality = "Poor"; emoji = "😴"; emotion = "concerned";
            advice = "Significant movement overnight. You may feel tired today, sir.";
        } else {
            score = 25; quality = "Very Poor"; emoji = "😩"; emotion = "concerned";
            advice = "Very restless night, sir. Try reducing screen time and caffeine before bed.";
        }

        // Save score
        p.edit().putInt(KEY_SCORE, score)
                .putString(KEY_LAST_RPT, new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date()))
                .apply();

        return String.format(Locale.US,
            "[EMOTION:%s] %s **Sleep Report, sir:**\n\n" +
            "🕐 Duration: **%dh %dm**\n" +
            "📊 Quality: **%s** (%d/100)\n" +
            "🔄 Movements detected: **%d** (%.1f/hr)\n\n" +
            "_%s_",
            emotion, emoji, hours, minutes, quality, score, moves, movesPerHour, advice);
    }

    public boolean isActive() {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ACTIVE, false);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!initialized) {
            lastX = event.values[0]; lastY = event.values[1]; lastZ = event.values[2];
            initialized = true; return;
        }
        float dx = Math.abs(event.values[0] - lastX);
        float dy = Math.abs(event.values[1] - lastY);
        float dz = Math.abs(event.values[2] - lastZ);
        if (dx + dy + dz > MOVE_THRESHOLD) {
            long now = System.currentTimeMillis();
            if (now - lastMoveTime > DEBOUNCE_MS) {
                lastMoveTime = now;
                SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
                p.edit().putInt(KEY_MOVES, p.getInt(KEY_MOVES, 0) + 1).apply();
            }
        }
        lastX = event.values[0]; lastY = event.values[1]; lastZ = event.values[2];
    }

    @Override public void onAccuracyChanged(Sensor s, int a) {}
}
