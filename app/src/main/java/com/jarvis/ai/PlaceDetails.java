package com.jarvis.ai;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

/**
 * Place Details — fetches Wikipedia summary + coordinates for any place or landmark.
 * "Tell me about Burj Khalifa"
 * "What is the Eiffel Tower?"
 * "Info on Dubai Mall"
 */
public class PlaceDetails {

    public interface Callback {
        void onResult(String summary, double lat, double lon, String name);
        void onError(String reason);
    }

    public static boolean isPlaceQuery(String text) {
        String lower = text.toLowerCase(Locale.US);
        return (lower.startsWith("tell me about") || lower.startsWith("what is") ||
                lower.startsWith("info on") || lower.startsWith("info about") ||
                lower.startsWith("details about") || lower.startsWith("where is") ||
                lower.startsWith("show me") || lower.contains("landmark") ||
                lower.contains("famous place") || lower.contains("tourist")) &&
               !lower.contains("weather") && !lower.contains("price") &&
               !lower.contains("time") && !lower.contains("how");
    }

    public static String parseQuery(String text) {
        return text
            .replaceAll("(?i)^(tell me about|what is|what's|info on|info about|details about|where is|show me)\\s+", "")
            .replaceAll("(?i)\\?$", "").trim();
    }

    public static void fetch(String query, Callback cb) {
        new Thread(() -> {
            try {
                // 1. Wikipedia search
                String enc = java.net.URLEncoder.encode(query, "UTF-8");
                String wikiSearch = "https://en.wikipedia.org/api/rest_v1/page/summary/" + enc;
                JSONObject wiki = httpGet(wikiSearch);

                String summary = null, title = query;
                if (wiki != null && wiki.has("extract")) {
                    summary = wiki.optString("extract", null);
                    title   = wiki.optString("title", query);
                    // Trim to 3 sentences max
                    if (summary != null && summary.length() > 600) {
                        String[] sentences = summary.split("\\. ");
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < Math.min(3, sentences.length); i++)
                            sb.append(sentences[i]).append(". ");
                        summary = sb.toString().trim();
                    }
                }

                // 2. Geocode via Nominatim
                String enc2 = java.net.URLEncoder.encode(query, "UTF-8");
                String nominatim = "https://nominatim.openstreetmap.org/search?q=" + enc2 +
                    "&format=json&limit=1";
                JSONArray geo = httpGetArray(nominatim);
                double lat = 0, lon = 0;
                if (geo != null && geo.length() > 0) {
                    lat = geo.getJSONObject(0).getDouble("lat");
                    lon = geo.getJSONObject(0).getDouble("lon");
                }

                if (summary == null && lat == 0) {
                    cb.onError("[EMOTION:neutral] Couldn't find information about **" + query + "**, sir.");
                    return;
                }

                String result = summary != null ? summary : "Located at " +
                    String.format(Locale.US, "%.4f°N, %.4f°E", lat, lon);

                cb.onResult("[EMOTION:excited] **" + title + "**\n\n" + result, lat, lon, title);

            } catch (Exception e) {
                cb.onError("[EMOTION:concerned] Lookup failed: " + e.getMessage());
            }
        }).start();
    }

    private static JSONObject httpGet(String rawUrl) {
        try {
            URL url = new URL(rawUrl);
            HttpURLConnection c = (HttpURLConnection) url.openConnection();
            c.setConnectTimeout(8000); c.setReadTimeout(10000);
            c.setRequestProperty("User-Agent", "HENRY-AI/1.0"); c.connect();
            if (c.getResponseCode() != 200) return null;
            InputStream is = c.getInputStream();
            StringBuilder sb = new StringBuilder();
            byte[] buf = new byte[32768]; int r;
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
