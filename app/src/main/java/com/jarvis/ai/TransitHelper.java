package com.jarvis.ai;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import java.util.Locale;

/**
 * TransitHelper — detects transit/navigation queries worldwide and provides
 * deep-links to Google Maps (transit mode), Uber, and Careem.
 * Works for any city, any country.
 */
public class TransitHelper {

    // ─── Detection ────────────────────────────────────────────────────────────

    public static boolean isTransitQuery(String text) {
        if (text == null) return false;
        String t = text.toLowerCase(Locale.US);

        // Must have a travel-action keyword
        boolean hasAction = t.matches(
            ".*\\b(how (do i|can i|to) get|direction|route|navigate|navigation|" +
            "metro|subway|tube|mrt|lrt|bts|bus|taxi|cab|uber|careem|grab|lyft|" +
            "tram|train|ferry|transit|transport|commute|" +
            "from .+ to|go to|travel to|take me to|get from|get to|" +
            "nearest station|closest station|nearest stop|how far|" +
            "how long to get|what bus|which bus|which train|which metro|" +
            "drop me|drive me|ride to|way to get)\\b.*"
        );

        // Must have at least one location indicator (place noun, preposition+place, or street)
        boolean hasLocation = t.matches(
            ".*\\b(airport|station|terminal|mall|street|road|avenue|blvd|" +
            "city|town|village|district|downtown|uptown|suburb|" +
            "university|hospital|school|hotel|office|park|beach|port|" +
            "from\\s+\\w|to\\s+\\w|at\\s+\\w|near\\s+\\w|in\\s+\\w)\\b.*"
        );

        // Accept if action + location, OR if there's explicit from→to pattern anywhere
        boolean hasFromTo = t.matches(".*\\bfrom\\b.+\\bto\\b.*");

        return hasAction && (hasLocation || hasFromTo);
    }

    // ─── Route Extraction ─────────────────────────────────────────────────────

    /**
     * Try to extract [origin, destination] from query.
     * Returns null if no route pattern is found.
     */
    public static String[] extractRoute(String text) {
        if (text == null) return null;

        // "from X to Y"
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(
            "from\\s+(.+?)\\s+to\\s+(.+?)(?:\\?|$|\\.|,)",
            java.util.regex.Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (m.find()) return new String[]{ m.group(1).trim(), m.group(2).trim() };

        // "how do I get from X to Y"
        m = java.util.regex.Pattern.compile(
            "get\\s+from\\s+(.+?)\\s+to\\s+(.+?)(?:\\?|$|\\.|,)",
            java.util.regex.Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (m.find()) return new String[]{ m.group(1).trim(), m.group(2).trim() };

        // "go to / take me to / navigate to / drive me to X"
        m = java.util.regex.Pattern.compile(
            "(?:go to|take me to|get to|travel to|navigate to|drive me to|drop me (at|to)|ride to)\\s+(.+?)(?:\\?|$|\\.|,)",
            java.util.regex.Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (m.find()) {
            String dest = m.group(m.groupCount()).trim();
            return new String[]{ "", dest };
        }

        return null;
    }

    // ─── Google Maps Transit ──────────────────────────────────────────────────

    /**
     * Open Google Maps transit directions.
     * If origin is empty/null, uses the device's current location.
     * No city suffix is appended — the query works globally.
     */
    public static void openGoogleMapsTransit(Context ctx, String origin, String destination) {
        String url;
        if (origin != null && !origin.isEmpty()) {
            url = "https://www.google.com/maps/dir/?api=1"
                + "&origin="      + Uri.encode(origin)
                + "&destination=" + Uri.encode(destination)
                + "&travelmode=transit";
        } else {
            url = "https://www.google.com/maps/dir/?api=1"
                + "&destination=" + Uri.encode(destination)
                + "&travelmode=transit";
        }
        ctx.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }

    /**
     * Open Google Maps driving directions (fallback when user asks for taxi/Uber route
     * but neither app is installed).
     */
    public static void openGoogleMapsDriving(Context ctx, String origin, String destination) {
        String url;
        if (origin != null && !origin.isEmpty()) {
            url = "https://www.google.com/maps/dir/?api=1"
                + "&origin="      + Uri.encode(origin)
                + "&destination=" + Uri.encode(destination)
                + "&travelmode=driving";
        } else {
            url = "https://www.google.com/maps/dir/?api=1"
                + "&destination=" + Uri.encode(destination)
                + "&travelmode=driving";
        }
        ctx.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }

    /** Open a generic location search in Google Maps */
    public static void openGoogleMapsSearch(Context ctx, String query) {
        String url = "https://www.google.com/maps/search/" + Uri.encode(query);
        ctx.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }

    // ─── Ride-hailing ─────────────────────────────────────────────────────────

    /** Open Uber with destination pre-filled (global app) */
    public static void openUber(Context ctx, String destination) {
        // Try Uber app deep-link first
        try {
            PackageManager pm = ctx.getPackageManager();
            pm.getPackageInfo("com.ubercab", 0);
            Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("uber://?action=setPickup&dropoff[nickname]=" + Uri.encode(destination)));
            ctx.startActivity(intent);
        } catch (Exception e) {
            // Uber not installed — open web
            ctx.startActivity(new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://m.uber.com/ul/?action=setPickup&dropoff[nickname]=" + Uri.encode(destination))));
        }
    }

    /** Open Grab with destination (SE Asia) */
    public static void openGrab(Context ctx, String destination) {
        try {
            PackageManager pm = ctx.getPackageManager();
            pm.getPackageInfo("com.grabtaxi.passenger", 0);
            Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("grab://open?screenType=BOOKING&dropOffAddress=" + Uri.encode(destination)));
            ctx.startActivity(intent);
        } catch (Exception e) {
            ctx.startActivity(new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://www.grab.com")));
        }
    }

    /** Open Careem with destination pre-filled (MENA region) */
    public static void openCareem(Context ctx, String destination) {
        try {
            PackageManager pm = ctx.getPackageManager();
            pm.getPackageInfo("com.careem.acma", 0);
            Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("careem://com.careem.acma?destination=" + Uri.encode(destination)));
            ctx.startActivity(intent);
        } catch (Exception e) {
            ctx.startActivity(new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://www.careem.com")));
        }
    }

    /** Open Lyft with destination (North America) */
    public static void openLyft(Context ctx, String destination) {
        try {
            PackageManager pm = ctx.getPackageManager();
            pm.getPackageInfo("me.lyft.android", 0);
            Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("lyft://ridetype?id=lyft&destination[address]=" + Uri.encode(destination)));
            ctx.startActivity(intent);
        } catch (Exception e) {
            ctx.startActivity(new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://www.lyft.com")));
        }
    }

    // ─── Smart Ride-hail Picker ───────────────────────────────────────────────

    /**
     * Try ride-hailing apps in order: Uber → Careem → Grab → Lyft → Google Maps driving.
     * Picks the first installed one automatically.
     */
    public static void openBestRideApp(Context ctx, String destination) {
        PackageManager pm = ctx.getPackageManager();
        String[] packages = {
            "com.ubercab",
            "com.careem.acma",
            "com.grabtaxi.passenger",
            "me.lyft.android"
        };
        for (String pkg : packages) {
            try {
                pm.getPackageInfo(pkg, 0);
                switch (pkg) {
                    case "com.ubercab":          openUber(ctx, destination);   return;
                    case "com.careem.acma":      openCareem(ctx, destination); return;
                    case "com.grabtaxi.passenger": openGrab(ctx, destination); return;
                    case "me.lyft.android":      openLyft(ctx, destination);   return;
                }
            } catch (PackageManager.NameNotFoundException ignored) {}
        }
        // No ride app installed — fall back to Google Maps driving
        openGoogleMapsDriving(ctx, null, destination);
    }
}
