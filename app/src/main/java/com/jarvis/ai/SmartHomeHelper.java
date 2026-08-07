package com.jarvis.ai;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import java.io.*;
import java.net.*;
import java.util.*;

/**
 * SmartHomeHelper — Control smart home devices via voice
 * Supports: Google Home, Alexa, SmartThings, Home Assistant, Philips Hue
 * Direct HTTP control for local Home Assistant instances
 */
public class SmartHomeHelper {

    public interface Callback {
        void onResult(String result);
        void onError(String msg);
    }

    private static final Handler H = new Handler(Looper.getMainLooper());

    // Plain contains() lets short keywords like "ac" match inside unrelated
    // words ("active", "back", "attack") — this checks for the keyword as a
    // standalone word only. Same fix as applied to LivePrices.java earlier.
    private static boolean containsWord(String haystack, String word) {
        return java.util.regex.Pattern.compile(
            "\\b" + java.util.regex.Pattern.quote(word) + "\\b")
            .matcher(haystack).find();
    }

    public static boolean isSmartHomeQuery(String input) {
        String t = input.toLowerCase();
        // "ac" bare-matched "active" ("active storms"), sending storm queries
        // here instead of the storm tracker. Bail out on storm/weather words.
        if (t.contains("storm") || t.contains("typhoon") || t.contains("hurricane") ||
            t.contains("cyclone")) return false;
        return t.contains("light") || t.contains("lamp") || containsWord(t, "ac") || t.contains("air con")
            || t.contains("thermostat") || t.contains("temperature") || t.contains("smart home")
            || t.contains("alexa") || t.contains("google home") || t.contains("hue")
            || t.contains("turn on") || t.contains("turn off") || containsWord(t, "dim")
            || t.contains("brightness") && (t.contains("room") || t.contains("light"))
            || t.contains("lock") && t.contains("door") || t.contains("unlock")
            || containsWord(t, "fan") || t.contains("curtain") || t.contains("blinds")
            || t.contains("plug") || t.contains("socket") || t.contains("switch");
    }

    public static String parseCommand(String input) {
        String t = input.toLowerCase();
        if (t.contains("turn on") || t.contains("switch on") || t.contains("on the")) return "on";
        if (t.contains("turn off") || t.contains("switch off") || t.contains("off the")) return "off";
        if (t.contains("dim") || t.contains("lower")) return "dim";
        if (t.contains("brighten") || t.contains("brighter")) return "bright";
        if (t.contains("lock")) return "lock";
        if (t.contains("unlock")) return "unlock";
        return "toggle";
    }

    public static String parseDevice(String input) {
        String t = input.toLowerCase();
        if (t.contains("bedroom light") || t.contains("bedroom lamp")) return "bedroom_light";
        if (t.contains("living room light") || t.contains("lounge")) return "living_room_light";
        if (t.contains("kitchen light")) return "kitchen_light";
        if (t.contains("bathroom light")) return "bathroom_light";
        if (t.contains("all lights") || t.contains("every light")) return "all_lights";
        if (t.contains("ac") || t.contains("air con") || t.contains("aircon")) return "ac";
        if (t.contains("fan")) return "fan";
        if (t.contains("tv") || t.contains("television")) return "tv";
        if (t.contains("front door") || t.contains("main door")) return "front_door";
        if (t.contains("garage")) return "garage";
        return "unknown_device";
    }

    /** Try local Home Assistant REST API */
    public static void controlHomeAssistant(String haUrl, String haToken,
                                             String domain, String service,
                                             String entityId, Callback cb) {
        new Thread(() -> {
            try {
                String url = haUrl.replaceAll("/$", "") + "/api/services/" + domain + "/" + service;
                String body = "{\"entity_id\":\"" + entityId + "\"}";
                HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
                c.setRequestMethod("POST");
                c.setRequestProperty("Authorization", "Bearer " + haToken);
                c.setRequestProperty("Content-Type", "application/json");
                c.setDoOutput(true);
                c.setConnectTimeout(5000);
                c.getOutputStream().write(body.getBytes());
                int code = c.getResponseCode();
                if (code == 200 || code == 201) {
                    H.post(() -> cb.onResult("✓ " + entityId + " " + service + " command sent, sir."));
                } else {
                    H.post(() -> cb.onError("Home Assistant returned: " + code));
                }
            } catch (Exception e) {
                H.post(() -> cb.onError("Could not reach Home Assistant: " + e.getMessage()));
            }
        }).start();
    }

    /** Open Google Home app */
    public static void openGoogleHome(Context ctx, Callback cb) {
        Intent i = ctx.getPackageManager().getLaunchIntentForPackage("com.google.android.apps.chromecast.app");
        if (i != null) { ctx.startActivity(i); cb.onResult("Opening Google Home, sir."); }
        else {
            ctx.startActivity(new Intent(Intent.ACTION_VIEW,
                Uri.parse("market://details?id=com.google.android.apps.chromecast.app"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            cb.onResult("Google Home not installed. Opening Play Store to install it.");
        }
    }

    /** Open Amazon Alexa app */
    public static void openAlexa(Context ctx, Callback cb) {
        Intent i = ctx.getPackageManager().getLaunchIntentForPackage("com.amazon.dee.app");
        if (i != null) { ctx.startActivity(i); cb.onResult("Opening Amazon Alexa, sir."); }
        else cb.onResult("Alexa app not installed, sir. Install it from the Play Store.");
    }

    /** Smart reply for unconnected state */
    public static String getSmartHomeSetupGuide(String device, String command) {
        return "🏠 Smart Home — " + command.toUpperCase() + " " + device.replace("_", " ").toUpperCase() + "\n\n" +
            "To control your smart devices via HENRY, you can:\n\n" +
            "1️⃣ **Google Home** — tap 🏠 to open, or say 'Open Google Home'\n" +
            "2️⃣ **Amazon Alexa** — say 'Open Alexa'\n" +
            "3️⃣ **Home Assistant** — connect your local HA server in HENRY settings\n" +
            "4️⃣ **Philips Hue** — use the Hue app or connect via Home Assistant\n\n" +
            "Once connected, HENRY can control lights, AC, locks, fans, and more directly, sir.";
    }
}
