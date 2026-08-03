package com.jarvis.ai;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Fitness Tracker — daily step counter using hardware step detector/counter sensor.
 * Falls back to accelerometer-based step detection if step sensor unavailable.
 */
public class FitnessTracker implements SensorEventListener {

    private static final String PREFS       = "fitness_prefs";
    private static final String KEY_STEPS   = "steps_today";
    private static final String KEY_GOAL    = "step_goal";
    private static final String KEY_DATE    = "step_date";
    private static final String KEY_OFFSET  = "step_offset"; // sensor offset for reset

    private final Context       ctx;
    private final SensorManager sm;
    private Sensor              stepSensor;
    private boolean             usesStepCounter; // true = TYPE_STEP_COUNTER, false = TYPE_STEP_DETECTOR

    // Accel fallback
    private float   lastX, lastY, lastZ;
    private boolean accelInit = false;
    private static final float ACCEL_THRESHOLD = 3.0f;
    private long    lastStepTime = 0;

    public interface StepCallback { void onStep(int totalToday); }
    private StepCallback stepCallback;

    public FitnessTracker(Context ctx) {
        this.ctx = ctx.getApplicationContext();
        sm = (SensorManager) this.ctx.getSystemService(Context.SENSOR_SERVICE);
        resetIfNewDay();
    }

    public void setStepCallback(StepCallback cb) { this.stepCallback = cb; }

    public void start() {
        if (sm == null) return;
        Sensor counter  = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        Sensor detector = sm.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        if (counter != null) {
            stepSensor = counter; usesStepCounter = true;
            sm.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
        } else if (detector != null) {
            stepSensor = detector; usesStepCounter = false;
            sm.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            // Accelerometer fallback
            Sensor accel = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (accel != null) sm.registerListener(this, accel, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    public void stop() {
        if (sm != null) sm.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            long offset = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                             .getLong(KEY_OFFSET, -1);
            if (offset == -1) {
                offset = (long) event.values[0];
                ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                   .putLong(KEY_OFFSET, offset).apply();
            }
            int today = (int)(event.values[0] - offset);
            saveSteps(today);
        } else if (event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
            incrementSteps();
        } else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            handleAccel(event.values[0], event.values[1], event.values[2]);
        }
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private void handleAccel(float x, float y, float z) {
        if (!accelInit) { lastX = x; lastY = y; lastZ = z; accelInit = true; return; }
        float delta = Math.abs(x - lastX) + Math.abs(y - lastY) + Math.abs(z - lastZ);
        if (delta > ACCEL_THRESHOLD) {
            long now = System.currentTimeMillis();
            if (now - lastStepTime > 400) { lastStepTime = now; incrementSteps(); }
        }
        lastX = x; lastY = y; lastZ = z;
    }

    private void incrementSteps() {
        int current = getStepsToday();
        saveSteps(current + 1);
    }

    private void saveSteps(int steps) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
           .putInt(KEY_STEPS, steps).apply();
        if (stepCallback != null) stepCallback.onStep(steps);
    }

    public int getStepsToday() {
        resetIfNewDay();
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_STEPS, 0);
    }

    public int getDailyGoal() {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_GOAL, 10000);
    }

    public void setDailyGoal(Context c, int goal) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(KEY_GOAL, goal).apply();
    }

    private void resetIfNewDay() {
        String today = new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date());
        String saved = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                          .getString(KEY_DATE, "");
        if (!today.equals(saved)) {
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
               .putInt(KEY_STEPS, 0)
               .putString(KEY_DATE, today)
               .putLong(KEY_OFFSET, -1)
               .apply();
        }
    }

    public static String getStatusReport(Context ctx) {
        android.content.SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int steps = p.getInt(KEY_STEPS, 0);
        int goal  = p.getInt(KEY_GOAL, 10000);
        float pct = (float) steps / goal * 100f;
        int bars  = (int)(pct / 10);
        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < 10; i++) bar.append(i < bars ? "█" : "░");
        bar.append("]");
        String emotion = pct >= 100 ? "proud" : pct >= 50 ? "excited" : "neutral";
        String encouragement = pct >= 100 ? " Goal smashed, sir. Outstanding." :
                               pct >= 75  ? " Almost there, sir. Keep going." :
                               pct >= 50  ? " Halfway there, sir." :
                               pct >= 25  ? " Good start, sir. Keep moving." :
                               " Every step counts, sir.";
        double km = steps * 0.762 / 1000.0;
        int cal = (int)(steps * 0.04);
        return String.format(Locale.US,
            "[EMOTION:%s] Today's steps: **%,d / %,d** %s %.0f%%\n" +
            "Distance: ~**%.2f km** | Calories: ~**%d kcal**%s",
            emotion, steps, goal, bar.toString(), pct, km, cal, encouragement);
    }

    public static boolean isStepsCommand(String text) {
        String lower = text.toLowerCase(Locale.US);
        return lower.contains("step") || lower.contains("walk") || lower.contains("fitness") ||
               lower.contains("how much did i walk") || lower.contains("did i walk") ||
               lower.contains("calories burned") || lower.contains("daily goal") ||
               lower.contains("set my goal") || lower.contains("set step goal");
    }
}
