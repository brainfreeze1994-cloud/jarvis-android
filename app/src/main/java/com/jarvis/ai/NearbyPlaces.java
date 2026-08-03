package com.jarvis.ai;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URL;
import java.net.HttpURLConnection;
import java.io.InputStream;
import java.util.Locale;

/**
 * Nearby places using Nominatim (OpenStreetMap) — 100% free, no API key.
 * Accepts voice queries like "restaurants near me", "pharmacy near me", etc.
 */
public class NearbyPlaces {

    public interface Callback {
        void onResult(String formatted);
        void onError(String reason);
    }

    // ── Category mapping ─────────────────────────────────────────────────────
    private static final String[][] CATEGORIES = {
        {"restaurant|food|eat|dining|cafe|coffee", "amenity=restaurant", "Restaurants"},
        {"pharmacy|medicine|drug|chemist",          "amenity=pharmacy",   "Pharmacies"},
        {"hospital|clinic|doctor|medical|health",   "amenity=hospital",   "Hospitals"},
        {"atm|cash|bank",                           "amenity=atm",        "ATMs / Banks"},
        {"petrol|gas|fuel|station",                 "amenity=fuel",       "Petrol Stations"},
        {"supermarket|grocery|store|shop|mall",     "shop=supermarket",   "Supermarkets"},
        {"hotel|hostel|accommodation|stay",         "tourism=hotel",      "Hotels"},
        {"school|university|college|education",     "amenity=school",     "Schools"},
        {"park|garden|recreation|playground",       "leisure=park",       "Parks"},
        {"gym|fitness|workout",                     "leisure=fitness_centre", "Gyms"},
    };

    public static boolean isNearbyQuery(String text) {
        String lower = text.toLowerCase(Locale.US);
        return (lower.contains("near me") || lower.contains("nearby") ||
                lower.contains("around me") || lower.contains("close to me") ||
                lower.contains("what's around")) &&
               (lower.contains("restaurant") || lower.contains("food") ||
                lower.contains("pharmacy") || lower.contains("hospital") ||
                lower.contains("atm") || lower.contains("petrol") ||
                lower.contains("hotel") || lower.contains("gym") ||
                lower.contains("park") || lower.contains("store") ||
                lower.contains("shop") || lower.contains("mall") ||
                lower.contains("cafe") || lower.contains("coffee") ||
                lower.contains("bank") || lower.contains("school") ||
                lower.contains("eat") || lower.contains("near"));
    }

    public static void search(Context ctx, String query, Callback cb) {
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            cb.onError("[EMOTION:concerned] I need location permission to find nearby places, sir.");
            return;
        }

        LocationManager lm = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
        Location loc = null;
        try {
            loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (loc == null) loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (loc == null) loc = lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
        } catch (SecurityException ignored) {}

        if (loc == null) {
            cb.onError("[EMOTION:concerned] Can't determine your location right now, sir. Enable GPS and try again.");
            return;
        }

        final double lat  = loc.getLatitude();
        final double lon  = loc.getLongitude();
        final String cat  = detectCategory(query);
        final String catLabel = detectCategoryLabel(query);

        new Thread(() -> {
            try {
                // Overpass API — free, no key
                String osmTag = cat;
                String overpassQuery = "[out:json][timeout:10];"
                    + "node[" + osmTag + "](around:1500," + lat + "," + lon + ");"
                    + "out 8;";
                String encoded = java.net.URLEncoder.encode(overpassQuery, "UTF-8");
                URL url = new URL("https://overpass-api.de/api/interpreter?data=" + encoded);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(15000);
                conn.setRequestProperty("User-Agent", "HENRY-AI-App/1.0");
                conn.connect();

                InputStream is = conn.getInputStream();
                byte[] buf = new byte[65536];
                int read;
                StringBuilder sb = new StringBuilder();
                while ((read = is.read(buf)) != -1) sb.append(new String(buf, 0, read, "UTF-8"));
                is.close();

                JSONObject json = new JSONObject(sb.toString());
                JSONArray elements = json.optJSONArray("elements");

                if (elements == null || elements.length() == 0) {
                    cb.onResult("[EMOTION:neutral] No " + catLabel + " found within 1.5 km, sir.");
                    return;
                }

                StringBuilder result = new StringBuilder();
                result.append("[EMOTION:excited] Here are nearby **").append(catLabel)
                      .append("** within 1.5 km, sir:\n\n");
                int shown = 0;
                for (int i = 0; i < elements.length() && shown < 6; i++) {
                    JSONObject el   = elements.getJSONObject(i);
                    JSONObject tags = el.optJSONObject("tags");
                    if (tags == null) continue;
                    String name = tags.optString("name", null);
                    if (name == null || name.trim().isEmpty()) continue;
                    double eLat = el.optDouble("lat", lat);
                    double eLon = el.optDouble("lon", lon);
                    int dist = (int) haversine(lat, lon, eLat, eLon);
                    String addr = tags.optString("addr:street", "");
                    result.append("**").append(name).append("** — ").append(dist).append(" m away");
                    if (!addr.isEmpty()) result.append(", ").append(addr);
                    result.append("\n");
                    shown++;
                }
                if (shown == 0) {
                    cb.onResult("[EMOTION:neutral] Found locations but none had names, sir. Try a broader search.");
                } else {
                    cb.onResult(result.toString().trim());
                }
            } catch (Exception e) {
                cb.onError("[EMOTION:concerned] Couldn't fetch nearby places: " + e.getMessage() + ", sir.");
            }
        }).start();
    }

    private static String detectCategory(String text) {
        String lower = text.toLowerCase(Locale.US);
        for (String[] row : CATEGORIES)
            if (lower.matches(".*(" + row[0] + ").*")) return row[1];
        return "amenity=restaurant";
    }

    private static String detectCategoryLabel(String text) {
        String lower = text.toLowerCase(Locale.US);
        for (String[] row : CATEGORIES)
            if (lower.matches(".*(" + row[0] + ").*")) return row[2];
        return "Places";
    }

    // Haversine formula — distance in metres
    private static double haversine(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLon/2) * Math.sin(dLon/2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    }
}
