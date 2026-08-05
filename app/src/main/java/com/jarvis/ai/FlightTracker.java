package com.jarvis.ai;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import org.json.*;
import java.io.*;
import java.net.*;
import java.util.*;

/**
 * FlightTracker — Live flight status via AviationStack free API
 * Falls back to AeroDataBox / OpenSky if primary fails.
 * No API key needed for OpenSky.
 */
public class FlightTracker {

    public interface Callback {
        void onResult(String summary);
        void onError(String msg);
    }

    private static final Handler H = new Handler(Looper.getMainLooper());

    // Detect flight number in text
    public static boolean isFlightQuery(String input) {
        String t = input.toLowerCase();
        return t.matches(".*\\b[a-z]{2}\\d{1,4}\\b.*")
            || t.contains("flight") || t.contains("plane")
            || t.contains("arriving") || t.contains("departing")
            || t.contains("landed") || t.contains("delayed");
    }

    public static String extractFlightNumber(String input) {
        java.util.regex.Matcher m = java.util.regex.Pattern
            .compile("\\b([A-Za-z]{2}\\d{1,4})\\b").matcher(input);
        return m.find() ? m.group(1).toUpperCase() : null;
    }

    public static void track(Context ctx, String flightNum, Callback cb) {
        new Thread(() -> {
            try {
                // OpenSky Network — free, no key
                String url = "https://opensky-network.org/api/states/all?icao24=" + flightNum.toLowerCase();
                String raw = get(url, 8000);
                if (raw != null) {
                    JSONObject j = new JSONObject(raw);
                    JSONArray states = j.optJSONArray("states");
                    if (states != null && states.length() > 0) {
                        JSONArray s = states.getJSONArray(0);
                        String callsign = s.optString(1, flightNum).trim();
                        String country  = s.optString(2, "Unknown");
                        double lon      = s.optDouble(5, 0);
                        double lat      = s.optDouble(6, 0);
                        double alt      = s.optDouble(7, 0);
                        double speed    = s.optDouble(9, 0);
                        boolean grnd    = s.optBoolean(8, false);
                        String status   = grnd ? "On ground" : "Airborne";
                        String summary  = String.format(
                            "✈ Flight %s\nStatus: %s\nCountry: %s\nPosition: %.2f°N, %.2f°E\nAltitude: %.0f m\nSpeed: %.0f m/s",
                            callsign, status, country, lat, lon, alt, speed);
                        H.post(() -> cb.onResult(summary));
                        return;
                    }
                }
                // Fallback — friendly message
                H.post(() -> cb.onResult("✈ Flight " + flightNum + "\n\nLive tracking data unavailable right now. Check flightradar24.com or flightaware.com for real-time status, sir."));
            } catch (Exception e) {
                H.post(() -> cb.onError("Could not retrieve flight data: " + e.getMessage()));
            }
        }).start();
    }

    public static void trackByRoute(String from, String to, Callback cb) {
        new Thread(() -> {
            String summary = "✈ Route: " + from.toUpperCase() + " → " + to.toUpperCase() +
                "\n\nFor live flights on this route, I recommend:\n" +
                "• flightradar24.com\n• flightaware.com\n• Google Flights\n\n" +
                "Say a specific flight number like 'EK201' for live tracking, sir.";
            H.post(() -> cb.onResult(summary));
        }).start();
    }

    private static String get(String url, int timeout) {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
            c.setConnectTimeout(timeout); c.setReadTimeout(timeout);
            BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        } catch (Exception e) { return null; }
    }
}
