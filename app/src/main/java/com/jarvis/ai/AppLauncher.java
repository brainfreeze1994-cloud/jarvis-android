package com.jarvis.ai;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AppLauncher {

    // app keyword → package name
    private static final Map<String, String> APP_MAP = new LinkedHashMap<>();
    static {
        APP_MAP.put("whatsapp",    "com.whatsapp");
        APP_MAP.put("youtube",     "com.google.android.youtube");
        APP_MAP.put("spotify",     "com.spotify.music");
        APP_MAP.put("instagram",   "com.instagram.android");
        APP_MAP.put("facebook",    "com.facebook.katana");
        APP_MAP.put("twitter",     "com.twitter.android");
        APP_MAP.put("x",           "com.twitter.android");
        APP_MAP.put("tiktok",      "com.zhiliaoapp.musically");
        APP_MAP.put("telegram",    "org.telegram.messenger");
        APP_MAP.put("gmail",       "com.google.android.gm");
        APP_MAP.put("maps",        "com.google.android.apps.maps");
        APP_MAP.put("google maps", "com.google.android.apps.maps");
        APP_MAP.put("camera",      "com.android.camera2");
        APP_MAP.put("photos",      "com.google.android.apps.photos");
        APP_MAP.put("gallery",     "com.google.android.apps.photos");
        APP_MAP.put("settings",    "com.android.settings");
        APP_MAP.put("clock",       "com.google.android.deskclock");
        APP_MAP.put("calculator",  "com.google.android.calculator");
        APP_MAP.put("calendar",    "com.google.android.calendar");
        APP_MAP.put("chrome",      "com.android.chrome");
        APP_MAP.put("netflix",     "com.netflix.mediaclient");
        APP_MAP.put("uber",        "com.ubercab");
        APP_MAP.put("linkedin",    "com.linkedin.android");
        APP_MAP.put("snapchat",    "com.snapchat.android");
        APP_MAP.put("discord",     "com.discord");
        APP_MAP.put("zoom",        "us.zoom.videomeetings");
        APP_MAP.put("teams",       "com.microsoft.teams");
        APP_MAP.put("outlook",     "com.microsoft.office.outlook");
        APP_MAP.put("excel",       "com.microsoft.office.excel");
        APP_MAP.put("word",        "com.microsoft.office.word");
        APP_MAP.put("play store",  "com.android.vending");
        APP_MAP.put("files",       "com.google.android.apps.nbu.files");
        APP_MAP.put("phone",       "com.android.dialer");
        APP_MAP.put("messages",    "com.google.android.apps.messaging");
        APP_MAP.put("contacts",    "com.android.contacts");
        APP_MAP.put("notes",       "com.google.android.keep");
        APP_MAP.put("keep",        "com.google.android.keep");
        APP_MAP.put("drive",       "com.google.android.apps.docs");
        APP_MAP.put("translate",   "com.google.android.apps.translate");
        APP_MAP.put("amazon",      "com.amazon.mShop.android.shopping");
        APP_MAP.put("shazam",      "com.shazam.android");
        APP_MAP.put("reddit",      "com.reddit.frontpage");
    }

    /**
     * Returns true if this looks like an "open/launch/start app" command.
     */
    public static boolean isLaunchCommand(String text) {
        String t = text.toLowerCase();
        return (t.startsWith("open ") || t.startsWith("launch ") || t.startsWith("start ")
                || t.startsWith("run ") || t.contains("open the ") || t.contains("open app"));
    }

    /**
     * Returns "[EMOTION:neutral] Opened X, sir." on success,
     * "[EMOTION:concerned] …" on failure, or null if no app matched.
     */
    public static String launch(Context ctx, String text) {
        String t = text.toLowerCase()
            .replace("open the ", "").replace("open ", "")
            .replace("launch the ", "").replace("launch ", "")
            .replace("start the ", "").replace("start ", "")
            .replace("run ", "").trim();

        // Try longest match first (e.g. "google maps" before "maps")
        String pkg = null;
        String matchedName = null;
        for (Map.Entry<String, String> e : APP_MAP.entrySet()) {
            if (t.contains(e.getKey())) {
                if (matchedName == null || e.getKey().length() > matchedName.length()) {
                    matchedName = e.getKey();
                    pkg = e.getValue();
                }
            }
        }

        if (pkg == null) return null;

        PackageManager pm = ctx.getPackageManager();
        Intent launch = pm.getLaunchIntentForPackage(pkg);
        if (launch == null) {
            // App not installed — open Play Store
            Intent store = new Intent(Intent.ACTION_VIEW,
                Uri.parse("market://details?id=" + pkg));
            store.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(store);
            return "[EMOTION:concerned] " + capitalize(matchedName) +
                   " is not installed, sir. Opening Play Store.";
        }
        launch.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(launch);
        return "[EMOTION:neutral] Opening " + capitalize(matchedName) + ", sir.";
    }

    /**
     * Open web search in browser.
     */
    public static String webSearch(Context ctx, String query) {
        String url = "https://www.google.com/search?q=" + Uri.encode(query);
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(i);
        return "[EMOTION:neutral] Searching for **" + query + "**, sir.";
    }

    /**
     * Send WhatsApp message to a contact number or contact name.
     * contactTarget: phone number or name (caller resolves name → number first)
     */
    public static String whatsappMessage(Context ctx, String number, String message) {
        try {
            String url = "https://api.whatsapp.com/send?phone=" +
                         number.replaceAll("[^0-9+]", "") +
                         "&text=" + Uri.encode(message);
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            i.setPackage("com.whatsapp");
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
            return "[EMOTION:warm] Opening WhatsApp to send your message, sir.";
        } catch (Exception e) {
            return "[EMOTION:concerned] WhatsApp is not installed, sir.";
        }
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        String[] words = s.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String w : words) sb.append(Character.toUpperCase(w.charAt(0)))
                                  .append(w.substring(1)).append(" ");
        return sb.toString().trim();
    }
}
