package com.jarvis.ai;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Stores custom suggestion chip texts in SharedPreferences.
 * 6 chips, keys chip_0 … chip_5.
 */
public class ChipPrefs {

    private static final String PREFS = "henry_chips";

    private static final String[] DEFAULTS = {
        "What's the weather in Dubai?",
        "Generate image of a warrior",
        "Write me a Python script",
        "Tell me something fascinating",
        "Latest tech news today",
        "Explain quantum computing"
    };

    public static String get(Context ctx, int index) {
        return prefs(ctx).getString("chip_" + index, DEFAULTS[index]);
    }

    public static void set(Context ctx, int index, String text) {
        prefs(ctx).edit().putString("chip_" + index, text).apply();
    }

    public static void reset(Context ctx, int index) {
        prefs(ctx).edit().remove("chip_" + index).apply();
    }

    public static String getDefault(int index) {
        return DEFAULTS[index];
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
