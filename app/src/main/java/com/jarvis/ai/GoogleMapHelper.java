package com.jarvis.ai;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

/**
 * GoogleMapHelper — Dedicated Google Maps integration.
 * Directly launches the official Google Maps app or web application with search,
 * location, directions, and satellite view. No static screenshots or third-party tiles.
 */
public class GoogleMapHelper {

    private static final String GOOGLE_MAPS_PKG = "com.google.android.apps.maps";

    /**
     * Open Google Maps for a specific search query (e.g. "restaurants near me", "Dubai Mall", "Tokyo").
     */
    public static void openGoogleMaps(Context context, String query) {
        if (context == null) return;
        Uri uri;
        if (query != null && !query.trim().isEmpty()) {
            uri = Uri.parse("geo:0,0?q=" + Uri.encode(query.trim()));
        } else {
            uri = Uri.parse("geo:0,0?q=my+location");
        }

        Intent mapIntent = new Intent(Intent.ACTION_VIEW, uri);
        mapIntent.setPackage(GOOGLE_MAPS_PKG);

        try {
            if (mapIntent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(mapIntent);
                return;
            }
        } catch (Exception ignored) {}

        // Fallback to Google Maps web
        openGoogleMapsWeb(context, query);
    }

    /**
     * Open Google Maps coordinates with optional label.
     */
    public static void openGoogleMapsCoords(Context context, double lat, double lon, String label) {
        if (context == null) return;
        Uri uri;
        if (label != null && !label.trim().isEmpty()) {
            uri = Uri.parse("geo:" + lat + "," + lon + "?q=" + lat + "," + lon + "(" + Uri.encode(label.trim()) + ")");
        } else {
            uri = Uri.parse("geo:" + lat + "," + lon + "?q=" + lat + "," + lon);
        }

        Intent mapIntent = new Intent(Intent.ACTION_VIEW, uri);
        mapIntent.setPackage(GOOGLE_MAPS_PKG);

        try {
            if (mapIntent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(mapIntent);
                return;
            }
        } catch (Exception ignored) {}

        // Fallback to web
        String webUrl = "https://www.google.com/maps/search/?api=1&query=" + lat + "," + lon;
        try {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(webUrl));
            context.startActivity(browserIntent);
        } catch (Exception e) {
            Intent inApp = new Intent(context, MapActivity.class);
            inApp.putExtra(MapActivity.EXTRA_LAT, lat);
            inApp.putExtra(MapActivity.EXTRA_LON, lon);
            if (label != null) inApp.putExtra(MapActivity.EXTRA_LABEL, label);
            context.startActivity(inApp);
        }
    }

    /**
     * Open interactive Google Maps Web experience.
     */
    public static void openGoogleMapsWeb(Context context, String query) {
        String webUrl;
        if (query != null && !query.trim().isEmpty()) {
            webUrl = "https://www.google.com/maps/search/?api=1&query=" + Uri.encode(query.trim());
        } else {
            webUrl = "https://www.google.com/maps";
        }

        try {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(webUrl));
            context.startActivity(browserIntent);
        } catch (Exception e) {
            // Fallback to MapActivity
            Intent inApp = new Intent(context, MapActivity.class);
            if (query != null && !query.trim().isEmpty()) {
                inApp.putExtra(MapActivity.EXTRA_QUERY, query);
            }
            context.startActivity(inApp);
        }
    }
}
