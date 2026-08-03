package com.jarvis.ai;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import java.util.ArrayList;
import java.util.List;

public class Stopwatch {

    public interface TickCallback { void onTick(long elapsedMs); }

    private static long      startTimeMs  = -1;
    private static long      pausedMs     = 0;   // accumulated paused time
    private static boolean   running      = false;
    private static final List<Long> laps  = new ArrayList<>();

    private static Handler  handler   = new Handler(Looper.getMainLooper());
    private static Runnable ticker    = null;
    private static TickCallback tickCb = null;

    public static boolean isRunning() { return running; }
    public static boolean isActive()  { return startTimeMs >= 0; }

    public static String start(TickCallback cb) {
        if (running) return "[EMOTION:amused] Stopwatch is already running, sir.";
        tickCb = cb;
        if (startTimeMs < 0) {
            startTimeMs = SystemClock.elapsedRealtime();
            pausedMs    = 0;
            laps.clear();
        } else {
            // resume
            startTimeMs = SystemClock.elapsedRealtime() - pausedMs;
        }
        running = true;
        startTicker();
        return "[EMOTION:excited] Stopwatch started, sir.";
    }

    public static String stop() {
        if (!running) return "[EMOTION:neutral] Stopwatch is not running, sir.";
        running  = false;
        pausedMs = elapsed();
        stopTicker();
        return "[EMOTION:neutral] Stopwatch paused at **" + format(pausedMs) + "**, sir.";
    }

    public static String reset() {
        running     = false;
        startTimeMs = -1;
        pausedMs    = 0;
        laps.clear();
        stopTicker();
        return "[EMOTION:neutral] Stopwatch reset, sir.";
    }

    public static String lap() {
        if (!running) return "[EMOTION:neutral] Stopwatch is not running, sir.";
        long e = elapsed();
        laps.add(e);
        return "[EMOTION:excited] Lap " + laps.size() + " — **" + format(e) + "**, sir.";
    }

    public static String status() {
        if (startTimeMs < 0) return "[EMOTION:neutral] Stopwatch not started, sir.";
        long e = running ? elapsed() : pausedMs;
        String state = running ? "running" : "paused";
        StringBuilder sb = new StringBuilder();
        sb.append("[EMOTION:neutral] Stopwatch ").append(state)
          .append(" — **").append(format(e)).append("**");
        if (!laps.isEmpty()) {
            sb.append("\n\nLaps:");
            for (int i = 0; i < laps.size(); i++)
                sb.append("\n  Lap ").append(i + 1).append(": **").append(format(laps.get(i))).append("**");
        }
        sb.append(", sir.");
        return sb.toString();
    }

    private static long elapsed() {
        if (!running) return pausedMs;
        return SystemClock.elapsedRealtime() - startTimeMs;
    }

    private static void startTicker() {
        stopTicker();
        ticker = new Runnable() {
            @Override public void run() {
                if (!running) return;
                if (tickCb != null) tickCb.onTick(elapsed());
                handler.postDelayed(this, 100);
            }
        };
        handler.post(ticker);
    }

    private static void stopTicker() {
        if (ticker != null) { handler.removeCallbacks(ticker); ticker = null; }
    }

    public static String format(long ms) {
        long totalSec = ms / 1000;
        long min = totalSec / 60;
        long sec = totalSec % 60;
        long hun = (ms % 1000) / 10;
        return String.format(java.util.Locale.US, "%02d:%02d.%02d", min, sec, hun);
    }
}
