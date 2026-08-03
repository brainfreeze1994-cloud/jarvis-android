package com.jarvis.ai;

import android.content.Context;
import android.os.CountDownTimer;
import android.os.Vibrator;

import java.util.Locale;

/**
 * Pomodoro Timer — 25 min work / 5 min break cycles.
 * Voice: "start pomodoro", "pomodoro status", "skip break", "stop pomodoro"
 */
public class PomodoroTimer {

    public interface Callback {
        void onTick(String label, long secondsLeft);
        void onPhaseComplete(String phase, int sessionCount);
        void onStopped();
    }

    public enum Phase { NONE, WORK, BREAK, LONG_BREAK }

    private static PomodoroTimer instance;
    private CountDownTimer timer;
    private Phase  currentPhase = Phase.NONE;
    private int    sessionsDone = 0;
    private long   secondsLeft  = 0;
    private Callback callback;
    private final Context ctx;

    private static final int WORK_MIN       = 25;
    private static final int BREAK_MIN      = 5;
    private static final int LONG_BREAK_MIN = 15;
    private static final int SESSIONS_FOR_LONG_BREAK = 4;

    private PomodoroTimer(Context ctx) {
        this.ctx = ctx.getApplicationContext();
    }

    public static PomodoroTimer getInstance(Context ctx) {
        if (instance == null) instance = new PomodoroTimer(ctx);
        return instance;
    }

    public static boolean isActive() {
        return instance != null && instance.currentPhase != Phase.NONE;
    }

    public static boolean isPomodoroCommand(String text) {
        String lower = text.toLowerCase(Locale.US);
        return lower.contains("pomodoro") ||
               lower.contains("focus timer") ||
               lower.contains("focus session") ||
               lower.contains("work session") ||
               (lower.contains("start") && lower.contains("focus")) ||
               (lower.contains("skip") && lower.contains("break"));
    }

    public String handle(String text) {
        String lower = text.toLowerCase(Locale.US);

        if (lower.contains("stop") || lower.contains("cancel") || lower.contains("end")) {
            stop(); return "[EMOTION:neutral] Pomodoro stopped, sir. Sessions completed: " + sessionsDone + ".";
        }
        if (lower.contains("status") || lower.contains("how long")) {
            return getStatus();
        }
        if (lower.contains("skip")) {
            skipToNext(); return "[EMOTION:neutral] Skipping to next phase, sir.";
        }
        if (lower.contains("pause")) {
            pauseTimer(); return "[EMOTION:neutral] Pomodoro paused, sir.";
        }

        // Start
        if (currentPhase != Phase.NONE) {
            return "[EMOTION:neutral] Pomodoro already running, sir. " + getStatus();
        }
        startWork();
        return String.format(Locale.US,
            "[EMOTION:excited] Pomodoro started, sir! **%d minutes** of focus. " +
            "I'll alert you when it's break time. Get to work!", WORK_MIN);
    }

    private void startWork() {
        currentPhase = Phase.WORK;
        start(WORK_MIN * 60L);
    }

    private void startBreak() {
        boolean longBreak = (sessionsDone % SESSIONS_FOR_LONG_BREAK == 0);
        currentPhase = longBreak ? Phase.LONG_BREAK : Phase.BREAK;
        start((longBreak ? LONG_BREAK_MIN : BREAK_MIN) * 60L);
    }

    private void start(long seconds) {
        if (timer != null) timer.cancel();
        secondsLeft = seconds;
        timer = new CountDownTimer(seconds * 1000, 1000) {
            @Override public void onTick(long ms) {
                secondsLeft = ms / 1000;
                if (callback != null)
                    callback.onTick(currentPhase == Phase.WORK ? "FOCUS" : "BREAK", secondsLeft);
            }
            @Override public void onFinish() {
                vibrate();
                if (currentPhase == Phase.WORK) {
                    sessionsDone++;
                    Phase done = Phase.WORK;
                    if (callback != null) callback.onPhaseComplete("WORK", sessionsDone);
                    startBreak();
                } else {
                    if (callback != null) callback.onPhaseComplete("BREAK", sessionsDone);
                    startWork();
                }
            }
        }.start();
    }

    private void vibrate() {
        try {
            Vibrator v = (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null) v.vibrate(new long[]{0, 500, 200, 500, 200, 500}, -1);
        } catch (Exception ignored) {}
    }

    public void stop() {
        if (timer != null) { timer.cancel(); timer = null; }
        currentPhase = Phase.NONE; secondsLeft = 0;
        if (callback != null) callback.onStopped();
    }

    private void pauseTimer() {
        if (timer != null) timer.cancel();
    }

    private void skipToNext() {
        if (timer != null) timer.cancel();
        if (currentPhase == Phase.WORK) { sessionsDone++; startBreak(); }
        else startWork();
    }

    public String getStatus() {
        if (currentPhase == Phase.NONE) return "[EMOTION:neutral] No active Pomodoro, sir.";
        long m = secondsLeft / 60, s = secondsLeft % 60;
        String phase = currentPhase == Phase.WORK ? "Focus session" :
                       currentPhase == Phase.LONG_BREAK ? "Long break" : "Short break";
        return String.format(Locale.US,
            "[EMOTION:neutral] **%s** — %d:%02d remaining | Sessions done: **%d**",
            phase, m, s, sessionsDone);
    }

    public void setCallback(Callback cb) { this.callback = cb; }
    public Phase getCurrentPhase() { return currentPhase; }
    public int getSessionsDone() { return sessionsDone; }
}
