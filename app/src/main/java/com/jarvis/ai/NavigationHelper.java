package com.jarvis.ai;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Navigation — opens Google Maps or Waze with turn-by-turn directions.
 * "Navigate to Dubai Mall"
 * "Take me to Burj Khalifa"
 * "Directions to [place]"
 * "Open Waze to [place]"
 */
public class NavigationHelper {

    public static boolean isNavCommand(String text) {
        String lower = text.toLowerCase(Locale.US);
        return lower.startsWith("navigate to") || lower.startsWith("take me to") ||
               lower.startsWith("directions to") || lower.startsWith("go to") ||
               lower.startsWith("open maps") || lower.startsWith("open waze") ||
               lower.startsWith("route to") || lower.startsWith("get me to") ||
               lower.contains("how do i get to") || lower.contains("show me the way to") ||
               lower.contains("drive to") || lower.contains("walk to");
    }

    public static String parseDestination(String text) {
        return text
            .replaceAll("(?i)^(navigate|take me|directions|go|route|get me|drive|walk)\\s+to\\s+", "")
            .replaceAll("(?i)^(open\\s+maps?|open\\s+waze)\\s*(to\\s+)?", "")
            .replaceAll("(?i)^(how do i get to|show me the way to)\\s+", "")
            .trim();
    }

    public static String navigate(Context ctx, String destination, boolean preferWaze) {
        if (destination.isEmpty())
            return "Where would you like to go, sir?";

        String encodedDest = Uri.encode(destination);

        // Try Waze first if preferred and installed
        if (preferWaze && isAppInstalled(ctx, "com.waze")) {
            String wazeUrl = "waze://?q=" + encodedDest + "&navigate=yes";
            Intent waze = new Intent(Intent.ACTION_VIEW, Uri.parse(wazeUrl));
            waze.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(waze);
            return "Waze is guiding you to **" + destination + "**, sir.";
        }

        // Google Maps navigation
        String mapsUrl = "google.navigation:q=" + encodedDest + "&mode=d";
        Intent maps = new Intent(Intent.ACTION_VIEW, Uri.parse(mapsUrl));
        maps.setPackage("com.google.android.apps.maps");
        maps.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if (maps.resolveActivity(ctx.getPackageManager()) != null) {
            ctx.startActivity(maps);
            return "Google Maps is navigating to **" + destination + "**, sir.";
        }

        // Fallback: GoogleMapHelper navigation or in-app map
        GoogleMapHelper.openGoogleMapsDirections(ctx, destination);
        return "Opening directions to **" + destination + "**, sir.";
    }

    // Open maps for search (not navigation)
    public static String openMap(Context ctx, String query) {
        String encodedQ = Uri.encode(query);
        Intent maps = new Intent(Intent.ACTION_VIEW,
            Uri.parse("geo:0,0?q=" + encodedQ));
        maps.setPackage("com.google.android.apps.maps");
        maps.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (maps.resolveActivity(ctx.getPackageManager()) != null) {
            ctx.startActivity(maps);
            return "Opening map for **" + query + "**, sir.";
        }
        // Fallback to in-app Google Maps activity
        GoogleMapHelper.openInAppMap(ctx, query);
        return "Opening interactive map for **" + query + "**, sir.";
    }

    private static boolean isAppInstalled(Context ctx, String pkg) {
        try { ctx.getPackageManager().getPackageInfo(pkg, 0); return true; }
        catch (PackageManager.NameNotFoundException e) { return false; }
    }
}
