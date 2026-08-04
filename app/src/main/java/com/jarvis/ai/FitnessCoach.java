package com.jarvis.ai;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Fitness Coach — AI workout plans + accelerometer rep counter.
 * "Start workout", "Count my reps", "Give me a chest workout", "How many reps did I do?"
 */
public class FitnessCoach implements SensorEventListener {

    private static final String PREFS       = "fitness_coach_prefs";
    private static final String KEY_REPS    = "total_reps_today";
    private static final String KEY_DATE    = "rep_date";
    private static final String KEY_WORKOUT = "last_workout";
    private static final float  REP_THRESHOLD = 3.5f;
    private static final long   DEBOUNCE_MS   = 600;

    private final Context ctx;
    private final SensorManager sm;
    private final Sensor accel;
    private boolean counting = false;
    private int     sessionReps = 0;
    private float   lastMag = 0;
    private long    lastRepTime = 0;
    private boolean peakDetected = false;

    private static FitnessCoach instance;

    public interface Callback {
        void onResult(String reply);
        void onError(String reason);
    }

    private FitnessCoach(Context ctx) {
        this.ctx = ctx.getApplicationContext();
        sm = (SensorManager) this.ctx.getSystemService(Context.SENSOR_SERVICE);
        accel = sm != null ? sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) : null;
    }

    public static FitnessCoach getInstance(Context ctx) {
        if (instance == null) instance = new FitnessCoach(ctx);
        return instance;
    }

    public static boolean isFitnessCommand(String text) {
        String lower = text.toLowerCase(Locale.US);
        return lower.contains("workout") || lower.contains("exercise") ||
               lower.contains("count reps") || lower.contains("count my reps") ||
               lower.contains("start counting") || lower.contains("stop counting") ||
               lower.contains("how many reps") || lower.contains("rep count") ||
               lower.contains("fitness plan") || lower.contains("training plan") ||
               lower.contains("gym routine") || lower.contains("push up") ||
               lower.contains("pushup") || lower.contains("sit up") ||
               lower.contains("squat plan") || lower.contains("cardio plan") ||
               lower.contains("chest workout") || lower.contains("leg workout") ||
               lower.contains("arm workout") || lower.contains("back workout") ||
               lower.contains("abs workout") || lower.contains("full body") ||
               lower.contains("calories burned") || lower.contains("reset reps");
    }

    public String handle(String userText, UserProfile profile, Callback cb) {
        String lower = userText.toLowerCase(Locale.US);

        if (lower.contains("start counting") || lower.contains("count reps") || lower.contains("count my reps")) {
            return startCounting();
        }
        if (lower.contains("stop counting") || (lower.contains("stop") && counting)) {
            return stopCounting();
        }
        if (lower.contains("how many reps") || lower.contains("rep count")) {
            return getRepCount();
        }
        if (lower.contains("reset reps")) {
            return resetReps();
        }
        // AI workout plan
        generateWorkoutPlan(userText, profile, cb);
        return null; // async
    }

    private String startCounting() {
        if (accel == null) return "[EMOTION:concerned] No accelerometer, sir. Rep counting needs hardware sensor.";
        sessionReps = 0;
        counting = true;
        sm.registerListener(this, accel, SensorManager.SENSOR_DELAY_GAME);
        return "[EMOTION:excited] Rep counter started! I'm tracking your movement. Say 'stop counting' when done, sir.";
    }

    private String stopCounting() {
        counting = false;
        if (sm != null) sm.unregisterListener(this);
        saveDailyReps(sessionReps);
        int total = getTodayTotal();
        return String.format(Locale.US,
            "[EMOTION:proud] Great work, sir! **Session: %d reps** | **Today's total: %d reps**\n\nKeep it up!",
            sessionReps, total);
    }

    private String getRepCount() {
        if (counting) return "[EMOTION:excited] Currently at **" + sessionReps + " reps** and counting, sir!";
        int total = getTodayTotal();
        return total == 0
            ? "[EMOTION:neutral] No reps counted yet today. Say 'start counting reps' to begin, sir."
            : "[EMOTION:proud] You've done **" + total + " reps** today, sir. Impressive!";
    }

    private String resetReps() {
        sessionReps = 0;
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
           .putInt(KEY_REPS, 0).apply();
        return "[EMOTION:neutral] Rep count reset, sir. Ready for a fresh session.";
    }

    private void generateWorkoutPlan(String query, UserProfile profile, Callback cb) {
        new Thread(() -> {
            try {
                String name = (profile != null && !profile.name.isEmpty()) ? profile.name : "sir";
                String prompt = "You are H.E.N.R.Y, an expert personal fitness coach for " + name + ". " +
                    "Create a practical workout plan based on: \"" + query + "\". " +
                    "Format:\n**🏋️ Workout Plan:**\n**Warm-up (5 min):** ...\n" +
                    "**Main Sets:**\n• Exercise 1: X sets × Y reps\n• Exercise 2: X sets × Y reps\n...\n" +
                    "**Cool-down (5 min):** ...\n**💡 Tips:** ...\n" +
                    "Keep it practical, safe, and motivating.";

                JSONArray msgs = new JSONArray();
                JSONObject msg = new JSONObject();
                msg.put("role", "user"); msg.put("content", query);
                msgs.put(msg);
                JSONObject body = new JSONObject();
                body.put("messages", msgs);
                body.put("responseMode", "detailed");
                body.put("systemOverride", prompt);

                URL url = new URL("https://jarvis-ai-seven-dun.vercel.app/api/jarvis");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST"); conn.setDoOutput(true);
                conn.setConnectTimeout(20000); conn.setReadTimeout(30000);
                conn.setRequestProperty("Content-Type", "application/json");
                try (OutputStream os = conn.getOutputStream()) { os.write(body.toString().getBytes("UTF-8")); }
                InputStream is = conn.getInputStream();
                StringBuilder sb = new StringBuilder();
                byte[] buf = new byte[8192]; int r;
                while ((r = is.read(buf)) != -1) sb.append(new String(buf, 0, r, "UTF-8"));
                is.close();
                JSONObject j = new JSONObject(sb.toString());
                String reply = j.optString("reply", "").replaceAll("\\[EMOTION:\\w+\\]\\s*", "").trim();
                // Save last workout
                ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                   .putString(KEY_WORKOUT, reply).apply();
                cb.onResult("[EMOTION:excited] " + reply);
            } catch (Exception e) {
                cb.onError("[EMOTION:concerned] Workout plan error, sir: " + e.getMessage());
            }
        }).start();
    }

    private void saveDailyReps(int reps) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String today = new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date());
        String saved = p.getString(KEY_DATE, "");
        int existing = today.equals(saved) ? p.getInt(KEY_REPS, 0) : 0;
        p.edit().putInt(KEY_REPS, existing + reps).putString(KEY_DATE, today).apply();
    }

    private int getTodayTotal() {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String today = new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date());
        return today.equals(p.getString(KEY_DATE, "")) ? p.getInt(KEY_REPS, 0) : 0;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!counting) return;
        float x = event.values[0], y = event.values[1], z = event.values[2];
        float mag = (float) Math.sqrt(x*x + y*y + z*z);
        float delta = Math.abs(mag - lastMag);
        long now = System.currentTimeMillis();
        if (delta > REP_THRESHOLD && !peakDetected) {
            peakDetected = true;
        } else if (delta < 1.0f && peakDetected && (now - lastRepTime) > DEBOUNCE_MS) {
            sessionReps++;
            lastRepTime = now;
            peakDetected = false;
        }
        lastMag = mag;
    }

    @Override public void onAccuracyChanged(Sensor s, int a) {}

    public boolean isCounting() { return counting; }
    public int getSessionReps() { return sessionReps; }
}
