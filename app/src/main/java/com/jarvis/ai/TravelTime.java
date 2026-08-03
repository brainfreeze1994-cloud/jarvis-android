package com.jarvis.ai;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;

import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

/**
 * Travel Time — uses OSRM (free, no key) + Nominatim geocoding to estimate
 * driving and walking time from current location to a destination.
 * "How long to Dubai Mall?"
 * "How far is Burj Khalifa?"
 * "ETA to airport"
 */
public class TravelTime {

    public interface Callback {
        void onResult(String formatted);
        void onError(String reason);
    }

    public static boolean isTravelTimeQuery(String text) {
        String lower = text.toLowerCase(Locale.US);
        return (lower.contains("how long") || lower.contains("how far") ||
                lower.contains("eta to") || lower.contains("travel time") ||
                lower.contains("distance to") || lower.contains("how many minutes") ||
                lower.contains("time to reach") || lower.contains("how much time")) &&
               (lower.contains(" to ") || lower.contains("get to") || lower.contains("reach"));
    }

    public static String parseDestination(String text) {
        return text
            .replaceAll("(?i)(how long|how far|eta|travel time|distance|how many minutes|time to reach|how much time)\\s*(to|is|from here to)?\\s*", "")
            .replaceAll("(?i)(get to|reach)\\s*", "")
            .replaceAll("(?i)\\?", "")
            .trim();
    }

    public static void query(Context ctx, String destination, Callback cb) {
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            cb.onError("[EMOTION:concerned] Location permission needed for travel time, sir.");
            return;
        }

        LocationManager lm = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
        Location loc = null;
        try {
            loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (loc == null) loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        } catch (SecurityException ignored) {}

        final double fromLat, fromLon;
        if (loc != null) {
            fromLat = loc.getLatitude(); fromLon = loc.getLongitude();
        } else {
            fromLat = 25.2048; fromLon = 55.2708; // Dubai fallback
        }

        new Thread(() -> {
            try {
                // 1. Geocode destination via Nominatim
                String enc = java.net.URLEncoder.encode(destination, "UTF-8");
                String nominatimUrl = "https://nominatim.openstreetmap.org/search?q=" + enc +
                    "&format=json&limit=1";
                JSONArray geo = httpGetArray(nominatimUrl);
                if (geo == null || geo.length() == 0) {
                    cb.onError("[EMOTION:neutral] Couldn't find **" + destination + "**, sir. Try a more specific name.");
                    return;
                }
                JSONObject place  = geo.getJSONObject(0);
                double toLat      = place.getDouble("lat");
                double toLon      = place.getDouble("lon");
                String placeName  = place.optString("display_name", destination);
                // Shorten display name (first 2 parts)
                String[] nameParts = placeName.split(",");
                String shortName = nameParts.length >= 2 ?
                    nameParts[0].trim() + ", " + nameParts[1].trim() : placeName;

                // 2. OSRM routing — driving
                String osrmCar  = "https://router.project-osrm.org/route/v1/driving/" +
                    fromLon + "," + fromLat + ";" + toLon + "," + toLat + "?overview=false";
                // Walking
                String osrmWalk = "https://router.project-osrm.org/route/v1/foot/" +
                    fromLon + "," + fromLat + ";" + toLon + "," + toLat + "?overview=false";

                JSONObject carRoute  = httpGet(osrmCar);
                JSONObject walkRoute = httpGet(osrmWalk);

                double carSecs = 0, carMetres = 0, walkSecs = 0, walkMetres = 0;
                if (carRoute != null) {
                    JSONObject r = carRoute.getJSONArray("routes").getJSONObject(0);
                    carSecs   = r.getDouble("duration");
                    carMetres = r.getDouble("distance");
                }
                if (walkRoute != null) {
                    JSONObject r = walkRoute.getJSONArray("routes").getJSONObject(0);
                    walkSecs   = r.getDouble("duration");
                    walkMetres = r.getDouble("distance");
                }

                String carTime  = formatTime(carSecs);
                String walkTime = formatTime(walkSecs);
                double km       = carMetres / 1000.0;

                String emotion = carSecs < 600 ? "excited" : carSecs < 1800 ? "neutral" : "neutral";
                cb.onResult(String.format(Locale.US,
                    "[EMOTION:%s] **%s** is **%.1f km** away, sir.\n\n" +
                    "🚗 Driving: **%s**\n" +
                    "🚶 Walking: **%s**",
                    emotion, shortName, km, carTime, walkTime));
            } catch (Exception e) {
                cb.onError("[EMOTION:concerned] Couldn't calculate travel time: " + e.getMessage());
            }
        }).start();
    }

    private static String formatTime(double seconds) {
        if (seconds <= 0) return "N/A";
        int mins = (int)(seconds / 60);
        if (mins < 60) return mins + " min";
        int h = mins / 60, m = mins % 60;
        return m > 0 ? h + "h " + m + "min" : h + "h";
    }

    private static JSONObject httpGet(String rawUrl) {
        try {
            URL url = new URL(rawUrl);
            HttpURLConnection c = (HttpURLConnection) url.openConnection();
            c.setConnectTimeout(8000); c.setReadTimeout(12000);
            c.setRequestProperty("User-Agent", "HENRY-AI/1.0"); c.connect();
            InputStream is = c.getInputStream();
            StringBuilder sb = new StringBuilder();
            byte[] buf = new byte[16384]; int r;
            while ((r = is.read(buf)) != -1) sb.append(new String(buf, 0, r, "UTF-8"));
            is.close(); return new JSONObject(sb.toString());
        } catch (Exception e) { return null; }
    }

    private static JSONArray httpGetArray(String rawUrl) {
        try {
            URL url = new URL(rawUrl);
            HttpURLConnection c = (HttpURLConnection) url.openConnection();
            c.setConnectTimeout(8000); c.setReadTimeout(10000);
            c.setRequestProperty("User-Agent", "HENRY-AI/1.0"); c.connect();
            InputStream is = c.getInputStream();
            StringBuilder sb = new StringBuilder();
            byte[] buf = new byte[16384]; int r;
            while ((r = is.read(buf)) != -1) sb.append(new String(buf, 0, r, "UTF-8"));
            is.close(); return new JSONArray(sb.toString());
        } catch (Exception e) { return null; }
    }
}
